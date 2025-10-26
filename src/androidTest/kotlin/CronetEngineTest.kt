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
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.serialization.kotlinx.protobuf.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.junit.BeforeClass
import org.junit.runner.RunWith
import kotlin.properties.Delegates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("UnusedReceiverParameter")
private val ContentType.Application.JsonIO
    get() = ContentType("application", "json+io")

@RunWith(AndroidJUnit4::class)
class CronetEngineTest {
    companion object {
        var client by Delegates.notNull<HttpClient>()

        @OptIn(ExperimentalSerializationApi::class)
        @BeforeClass
        @JvmStatic
        fun setup() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            CronetProviderInstaller.installProvider(context)
            client = HttpClient(Cronet) {
                engine {
                    options {
                        addQuicHint("httpbin.schlaubi.net", 443, -1)
                    }
                }
                install(ContentNegotiation) {
                    json()
                    jsonIo(contentType = ContentType.Application.JsonIO)
                    protobuf()
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
    fun testSerialization() = runTest {
        val response = client.put("https://httpbin.schlaubi.net/put") {
            contentType(ContentType.Application.Json)
            setBody(TestObject.DEFAULT)
        }
        val body = response.body<Response>()


        assertEquals("https://httpbin.schlaubi.net/put", body.url)
        val jsonBody = assertNotNull(body.json)
        assertEquals(TestObject.DEFAULT, Json.decodeFromJsonElement(jsonBody))
    }

    @Test
    fun testIoSerialization() = runTest {
        val response = client.put("https://httpbin.schlaubi.net/put") {
            contentType(ContentType.Application.JsonIO)
            setBody(TestObject.DEFAULT)
        }
        val body = response.body<Response>()


        assertEquals("https://httpbin.schlaubi.net/put", body.url)
        val jsonBody = assertNotNull(body.json)
        assertEquals(TestObject.DEFAULT, Json.decodeFromJsonElement(jsonBody))
    }

    @Test
    fun testProtobufSerialization() = runTest {
        val response = client.put("https://httpbin.schlaubi.net/put") {
            contentType(ContentType.Application.ProtoBuf)
            setBody(TestObject.DEFAULT)
        }
        val body = response.body<Response>()

        assertEquals("https://httpbin.schlaubi.net/put", body.url)
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

    @Test
    fun testGZipRequest() = runTest {
        val response = client.get("https://httpbin.schlaubi.net/gzip")
        val body = response.bodyAsText()
        val content = Json.decodeFromString<JsonObject>(body)
        val gzipped = content["gzipped"]!!.jsonPrimitive.boolean

        assertTrue(gzipped, "Response was not gzipped")
    }
}
