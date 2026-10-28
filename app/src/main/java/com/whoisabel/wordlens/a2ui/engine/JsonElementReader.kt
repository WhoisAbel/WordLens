package com.whoisabel.wordlens.a2ui.engine

import androidx.a2ui.model.processor.A2uiJsonReader
import androidx.a2ui.model.processor.A2uiJsonToken
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** [A2uiJsonReader] over a pre-tokenized kotlinx JSON tree (the engine ships no reader implementation). */
internal class JsonElementReader(json: String) : A2uiJsonReader {
    private class Tok(val type: A2uiJsonToken, val text: String = "")

    private val toks = ArrayList<Tok>().also { flatten(Json.parseToJsonElement(json), it) }
    private var i = 0

    private fun flatten(e: JsonElement, out: MutableList<Tok>) {
        when (e) {
            is JsonObject -> {
                out += Tok(A2uiJsonToken.BEGIN_OBJECT)
                e.forEach { (k, v) -> out += Tok(A2uiJsonToken.NAME, k); flatten(v, out) }
                out += Tok(A2uiJsonToken.END_OBJECT)
            }
            is JsonArray -> {
                out += Tok(A2uiJsonToken.BEGIN_ARRAY)
                e.forEach { flatten(it, out) }
                out += Tok(A2uiJsonToken.END_ARRAY)
            }
            JsonNull -> out += Tok(A2uiJsonToken.NULL)
            is JsonPrimitive -> out += when {
                e.isString -> Tok(A2uiJsonToken.STRING, e.content)
                e.content == "true" || e.content == "false" -> Tok(A2uiJsonToken.BOOLEAN, e.content)
                else -> Tok(A2uiJsonToken.NUMBER, e.content)
            }
        }
    }

    override fun peek() = toks.getOrNull(i)?.type ?: A2uiJsonToken.END_DOCUMENT
    private fun next(expected: A2uiJsonToken): String {
        val t = toks[i]
        check(t.type == expected) { "Expected $expected but was ${t.type}" }
        i++
        return t.text
    }

    override fun beginObject() { next(A2uiJsonToken.BEGIN_OBJECT) }
    override fun endObject() { next(A2uiJsonToken.END_OBJECT) }
    override fun beginArray() { next(A2uiJsonToken.BEGIN_ARRAY) }
    override fun endArray() { next(A2uiJsonToken.END_ARRAY) }
    override fun hasNext() = peek().let { it != A2uiJsonToken.END_OBJECT && it != A2uiJsonToken.END_ARRAY && it != A2uiJsonToken.END_DOCUMENT }
    override fun nextName() = next(A2uiJsonToken.NAME)
    override fun nextString() = next(A2uiJsonToken.STRING)
    override fun nextBoolean() = next(A2uiJsonToken.BOOLEAN).toBoolean()
    override fun nextDouble() = next(A2uiJsonToken.NUMBER).toDouble()
    override fun nextInt() = next(A2uiJsonToken.NUMBER).toInt()
    override fun nextLong() = next(A2uiJsonToken.NUMBER).toLong()
    override fun nextNull() { next(A2uiJsonToken.NULL) }
    override fun skipValue() {
        var depth = 0
        do {
            when (toks[i++].type) {
                A2uiJsonToken.BEGIN_OBJECT, A2uiJsonToken.BEGIN_ARRAY -> depth++
                A2uiJsonToken.END_OBJECT, A2uiJsonToken.END_ARRAY -> depth--
                else -> {}
            }
        } while (depth > 0)
    }
    override fun close() {}
}
