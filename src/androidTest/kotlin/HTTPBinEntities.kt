package com.trainyapp.cronet.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Response(
    val args: Map<String, String>,
    val data: String? = null,
    val form: Map<String, String> = emptyMap(),
    val headers: Map<String, String>,
    val files: Map<String, String> = emptyMap(),
    val json: JsonElement? = null,
    val origin: String,
    val url: String
)

@Serializable
data class TestObject(val testValue: String) {
    companion object {
        val DEFAULT = TestObject("default")
    }
}
