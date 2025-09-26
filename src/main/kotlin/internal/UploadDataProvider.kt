package com.trainyapp.cronet.internal

import android.util.Log
import io.ktor.http.content.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readAtMostTo
import org.chromium.net.UploadDataProvider
import org.chromium.net.UploadDataSink
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

internal fun OutgoingContent.toUploadDataProvider(callContext: CoroutineContext): UploadDataProvider? {
    val coroutineScope = CoroutineScope(callContext)

    return when (this) {
        is OutgoingContent.ContentWrapper -> delegate().toUploadDataProvider(callContext)
        is OutgoingContent.ByteArrayContent -> ByteArrayUploadDataProvider(this)
        is OutgoingContent.ReadChannelContent -> ReadChannelUploadDataProvider(readFrom(), coroutineScope)
        is OutgoingContent.WriteChannelContent -> {
            val readChannel = CoroutineScope(callContext).writer(callContext) {
                writeTo(channel)
            }.channel

            ReadChannelUploadDataProvider(readChannel, coroutineScope)
        }

        else -> null
    }
}

private data class ByteArrayUploadDataProvider(private val content: OutgoingContent.ByteArrayContent) :
    UploadDataProvider() {
    override fun getLength(): Long = content.contentLength ?: Long.MAX_VALUE

    override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
        byteBuffer.put(content.bytes())
        uploadDataSink.onReadSucceeded(false)
    }

    override fun rewind(uploadDataSink: UploadDataSink) = uploadDataSink.onRewindSucceeded()
}

private data class ReadChannelUploadDataProvider(
    private val channel: ByteReadChannel,
    private val scope: CoroutineScope
) : UploadDataProvider() {
    override fun getLength(): Long = -1

    @OptIn(InternalAPI::class)
    override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
        scope.launch(Dispatchers.IO) {
            Log.d("CronetKtor", "Reading from channel")
            try {
                channel.readBuffer.readAtMostTo(byteBuffer)
                Log.d("CronetKtor", "Read chunk from channel; exhausted: ${channel.exhausted()}")
                uploadDataSink.onReadSucceeded(channel.exhausted())
            } catch (e: Exception) {
                uploadDataSink.onReadError(e)
            }
        }
    }

    override fun rewind(uploadDataSink: UploadDataSink) {
        uploadDataSink.onRewindError(UnsupportedOperationException("Rewind is not supported"))
    }
}
