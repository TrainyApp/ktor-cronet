package com.trainyapp.cronet

import kotlinx.io.IOException
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo

/**
 * Exception thrown when a [CronetException] is thrown whilst performing a request.
 *
 * @property request the [UrlRequest] causing the Exception
 * @property urlInfo the [UrlResponseInfo] of the request
 *
 * @see CronetException
 */
public class CronetRequestFailedException(
    public val request: UrlRequest,
    public val urlInfo: UrlResponseInfo?,
    cause: CronetException
) : IOException(cause)
