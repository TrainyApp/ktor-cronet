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

internal fun OutgoingContent.toUploadDataProvider(coroutineScope: CoroutineScope): UploadDataProvider? = when (this) {
    is OutgoingContent.ContentWrapper -> delegate().toUploadDataProvider(coroutineScope)
    is OutgoingContent.ByteArrayContent -> ByteArrayUploadDataProvider(this)
    is OutgoingContent.ReadChannelContent -> ReadChannelUploadDataProvider(this, coroutineScope)
    is OutgoingContent.WriteChannelContent -> WriteChannelUploadDataProvider(this, coroutineScope)
    else -> null
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
    private val content: OutgoingContent.ReadChannelContent,
    private val scope: CoroutineScope
) : UploadDataProvider() {
    override fun getLength(): Long = -1

    private val channel = content.readFrom()

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

private data class WriteChannelUploadDataProvider(
    private val content: OutgoingContent.WriteChannelContent,
    private val scope: CoroutineScope
) : UploadDataProvider() {
    override fun getLength(): Long = -1

    private var delegate: ByteReadChannel? = null

    @OptIn(InternalAPI::class)
    override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
        scope.launch(Dispatchers.IO) {
            if (delegate == null) {
                val channel = ByteChannel()
                content.writeTo(channel)
                delegate = channel
            }

            try {
                delegate!!.readBuffer.readAtMostTo(byteBuffer)
                uploadDataSink.onReadSucceeded(delegate!!.exhausted())
            } catch (e: Exception) {
                uploadDataSink.onReadError(e)
            }
        }
    }

    override fun rewind(uploadDataSink: UploadDataSink) {
        uploadDataSink.onRewindError(UnsupportedOperationException("Rewind is not supported"))
    }
}
