package com.trainyapp.cronet

import io.ktor.client.*
import io.ktor.client.engine.*

/**
 * An Android client engine that uses [org.chromium.net.CronetEngine] under the hood.
 * It allows you to use modern protcols like `HTTP/3`.
 *
 * ```kotlin
 * val client = HttpClient(Cronet) {
 *   engine {
 *     //this: CronetEngineConfig
 *   }
 * }
 */
public data object Cronet : HttpClientEngineFactory<CronetEngineConfig> {
    override fun create(block: CronetEngineConfig.() -> Unit): HttpClientEngine =
        CronetEngineConfig().apply(block).build()
}

@PublishedApi
internal class CronetEngineContainer : HttpClientEngineContainer {
    override val factory: HttpClientEngineFactory<*> = Cronet

    override fun toString(): String = "Cronet"
}
