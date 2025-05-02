package com.trainyapp.cronet.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.net.CronetProviderInstaller
import com.trainyapp.cronet.Cronet
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.BeforeClass
import org.junit.runner.RunWith
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CronetEngineTest {
    companion object {
        var client by Delegates.notNull<HttpClient>()

        @BeforeClass
        @JvmStatic
        fun setup() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            CronetProviderInstaller.installProvider(context)
            client = HttpClient(Cronet) {
                install(ContentNegotiation) {
                    json()
                }

                expectSuccess = true

                engine {
                    this.context = context
                }
            }
        }
    }

    @Test
    fun testDeleteRequest() = runTest {
        val response = client.delete("https://httpbin.schlaubi.net/delete")
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/delete", body.url)
    }

    @Test
    fun testGetRequest() = runTest {
        val response = client.get("https://httpbin.schlaubi.net/get")
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/get", body.url)
    }

    @Test
    fun testPatchRequest() = runTest {
        val response = client.patch("https://httpbin.schlaubi.net/patch")
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/patch", body.url)
    }

    @Test
    fun testPostRequest() = runTest {
        val response = client.post("https://httpbin.schlaubi.net/post")
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/post", body.url)
    }

    @Test
    fun testPutRequest() = runTest {
        val response = client.put("https://httpbin.schlaubi.net/put")
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/put", body.url)
    }

    @Test
    fun testFormDataRequest() = runTest {
        val formData = parameters {
            set("key", "value")
        }

        val response = client.put("https://httpbin.schlaubi.net/put") {
            setBody(FormDataContent(formData))
        }
        val body = response.body<Response>()

        val bodyParameters = Parameters.build {
            body.form.forEach { (key, value) -> set(key, value) }
        }

        assertEquals("https://httpbin.schlaubi.net/put", body.url)
        assertEquals(formData, bodyParameters)
    }

    @Test
    fun testFileRequest() = runTest {
        val fileContent = "abc"
        val channel = ByteReadChannel(fileContent)

        val response = client.put("https://httpbin.schlaubi.net/put") {
            setBody(channel)
        }
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/put", body.url)
        assertEquals(fileContent, body.data)
    }

    @Test
    fun testMultipartFormData() = runTest {
        val formData = formData {
            append("key", "value")
            append("file", ChannelProvider { ByteReadChannel("abc") }, Headers.build {
                append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                append(HttpHeaders.ContentDisposition, "filename=abc.txt")
            })
        }

        val response = client.put("https://httpbin.schlaubi.net/put") {
            setBody(MultiPartFormDataContent(formData))
        }
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/put", body.url)

        assertEquals(mapOf("key" to "value"), body.form)
        assertEquals(mapOf("file" to "abc"), body.files)
    }
}
