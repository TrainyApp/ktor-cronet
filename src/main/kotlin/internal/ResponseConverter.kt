package com.trainyapp.cronet.internal

import io.ktor.client.request.HttpResponseData
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HeadersImpl
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
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

private fun Map<String, List<String>>.toHeaders() = HeadersImpl(this)
