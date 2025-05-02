package com.trainyapp.cronet

import android.content.Context
import com.trainyapp.cronet.internal.ContextProvider
import io.ktor.client.engine.*
import org.chromium.net.impl.JavaCronetProvider
import org.chromium.net.CronetEngine as ChromiumCronet

/**
 * Configuration for the [Cronet] engine.
 *
 * @property context the [Context] to use cronet in (if none is supplied a content will be used instead)
 * @property builder optional options to apply to the underlying [ChromiumCronet]
 * @property followRedirects whether the client should follow redirects or not
 */
public class CronetEngineConfig : HttpClientEngineConfig() {
    public var context: Context? = null
    public var builder: (ChromiumCronet.Builder.() -> Unit)? = null
    public var followRedirects: Boolean = true

    /**
     * Applies additional options to the underlying [ChromiumCronet].
     */
    public fun options(builder: ChromiumCronet.Builder.() -> Unit) {
        this.builder = builder
    }

    internal fun build(): CronetEngine {
        val context = context ?: ContextProvider.ANDROID_CONTEXT ?: error("Please specify the context property")

        val options = try {
            ChromiumCronet.Builder(context)
        } catch (_: Exception) {
            try {
                JavaCronetProvider(context).createBuilder()
            } catch (_: ClassNotFoundException) {
                error(
                    "Cronet is not supported on this device and fallback provider is not available. " +
                        "Please add https://mvnrepository.com/artifact/org.chromium.net/cronet-fallback/ as a dependency"
                )
            }
        }
        builder?.invoke(options)

        return CronetEngine(this, options.build())
    }
}
