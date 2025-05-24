package com.trainyapp.cronet.internal

import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.util.date.*
import org.chromium.net.UrlResponseInfo
import kotlin.coroutines.CoroutineContext

internal fun UrlResponseInfo.toHttpResponseData(
    requestTime: GMTDate,
    body: Any = EmptyContent,
    callContext: CoroutineContext
): HttpResponseData {
    return HttpResponseData(
        HttpStatusCode(httpStatusCode, httpStatusText),
        requestTime,
        allHeaders.toHeaders(),
        negotiatedProtocol.toHttpProtocolVersion(),
        body, callContext
    )
}

private fun String.toHttpProtocolVersion() = when(this) {
    "h1", "http/1.0" -> HttpProtocolVersion.HTTP_1_0
    "http/1.1" -> HttpProtocolVersion.HTTP_1_1
    "h2",  -> HttpProtocolVersion.HTTP_2_0
    "h3", "quic" -> HttpProtocolVersion.QUIC
    "quic/1+spdy/3" -> HttpProtocolVersion.SPDY_3
    else -> HttpProtocolVersion.HTTP_1_0
}

private fun Map<String, List<String>>.toHeaders() = HeadersImpl(dropCompressionHeaders())

// We remove the content encoding headers, as content encoding is already handled by cronet
private fun Map<String, List<String>>.dropCompressionHeaders(): Map<String, List<String>> {
    return if (containsKey(HttpHeaders.ContentEncoding)) {
        filter { (key) -> key == HttpHeaders.ContentEncoding || key == HttpHeaders.ContentLength }
    } else {
        this
    }
}
