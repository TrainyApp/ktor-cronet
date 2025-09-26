package com.trainyapp.cronet

import android.util.Log
import com.trainyapp.cronet.internal.toHttpResponseData
import com.trainyapp.cronet.internal.toUploadDataProvider
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.properties.Delegates

private const val chunkLength = 8192

internal class CronetEngine(
    override val config: CronetEngineConfig,
    private val engine: CronetEngine
) : HttpClientEngineBase("Cronet") {
    private val configExecutor = config.dispatcher?.asExecutor()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val callContext = callContext()
        val executor = configExecutor ?: coroutineContext[CoroutineDispatcher]?.asExecutor()
        ?: error("Could not find a suitable dispatcher for executing the request. Please specify one using withContext()")

        return suspendCancellableCoroutine { continuation ->
            val requestTime = GMTDate()
            val request =
                engine.newUrlRequestBuilder(
                    data.url.toString(),
                    CronetCallback(continuation, requestTime, callContext),
                    executor
                )
                    .apply {
                        setHttpMethod(data.method.value)
                        data.headers.flattenForEach { key, value ->
                            addHeader(key, value)
                        }

                        if (!data.body.isEmpty()) {
                            if (HttpHeaders.ContentType !in data.headers) {
                                val contentType = data.body.contentType?.toString() ?: error("Content-Type header is required for requests with bodies")
                                addHeader(HttpHeaders.ContentType, contentType)
                            }

                            val uploadDataProvider = data.body.toUploadDataProvider(callContext)
                            setUploadDataProvider(uploadDataProvider, executor)
                        }
                    }
                    .build()
            continuation.invokeOnCancellation { request.cancel() }
            request.start()
        }
    }

    override fun close() {
        engine.shutdown()
    }

    private inner class CronetCallback(
        private val continuation: Continuation<HttpResponseData>,
        private val requestTime: GMTDate,
        private val callContext: CoroutineContext,
    ) : UrlRequest.Callback() {
        private val scope: CoroutineScope = CoroutineScope(callContext)


        private var buffer by Delegates.notNull<ByteBuffer>()

        override fun onRedirectReceived(
            request: UrlRequest,
            info: UrlResponseInfo,
            newLocationUrl: String
        ) {
            if (config.followRedirects) {
                request.followRedirect()
            } else {
                resume(body = EmptyContent, info = info)
            }
        }

        override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
            Log.d("CronetKtor", "Start reading response for: $request")
            val contentLength = info.allHeaders["Content-Length"]?.first()?.toIntOrNull()?.takeIf { it > 0 } ?: chunkLength
            Log.d("CronetKtor", "Allocating $contentLength bytes for response body of: $request")
            buffer = ByteBuffer.allocateDirect(contentLength)
            scope.launch(Dispatchers.IO) {
                request.read(buffer)
            }
        }

        override fun onReadCompleted(
            request: UrlRequest,
            info: UrlResponseInfo,
            byteBuffer: ByteBuffer
        ) {
            if (buffer.capacity() == buffer.position()) {
                Log.d("CronetKtor", "Buffer full, resizing to ${buffer.capacity() * 2}")
                val newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2).apply {
                    buffer.flip()
                    put(buffer)
                }
                buffer = newBuffer
            }
            Log.d("CronetKtor", "Reading response for: $request")
            request.read(buffer)
        }

        override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
            Log.d("CronetKtor", "Request succeeded: $request")
            buffer.limit(buffer.position())
            buffer.rewind()
            resume(body = ByteReadChannel(buffer), info = info)
        }

        override fun onFailed(
            request: UrlRequest,
            info: UrlResponseInfo?,
            error: CronetException
        ) {
            continuation.resumeWithException(CronetRequestFailedException(request, info, error))
        }

        private fun resume(body: Any, info: UrlResponseInfo) {
            val response = try {
                info.toHttpResponseData(requestTime, body, callContext)
            } catch (e: Throwable) {
                continuation.resumeWithException(e)
                return
            }
            continuation.resume(response)
        }
    }
}
