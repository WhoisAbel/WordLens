package com.whoisabel.wordlens

import androidx.a2ui.engine.catalog.A2uiCoreCatalog
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinition
import androidx.a2ui.engine.catalog.A2uiCoreComponentDefinitionCollection
import androidx.a2ui.engine.platform.A2uiCoreComponentRegistry
import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.engine.processor.A2uiCoreMessageProcessor
import androidx.a2ui.model.catalog.A2uiFunctionCollection
import androidx.a2ui.model.processor.A2uiJsonMessageParser
import androidx.a2ui.model.processor.A2uiJsonReader
import androidx.a2ui.model.processor.A2uiJsonToken
import androidx.a2ui.model.processor.processInput
import androidx.a2ui.model.protocol.A2uiComponentPayload
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiActionSchema
import androidx.a2ui.model.schema.commontypes.A2uiChildListSchema
import androidx.a2ui.model.schema.commontypes.A2uiComponentIdSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val CATALOG_ID = "wordlens:catalog/v1"

private class Component(
    override val name: String,
    override val description: String,
    props: Map<String, A2uiSchema>,
    required: Set<String> = emptySet(),
) : A2uiCoreComponentDefinition {
    override val propertySchema: A2uiSchema = A2uiObjectSchema(props, required)
}

/** The five components this sample renders (a subset of the A2UI basic catalog's vocabulary). */
object WordLensCatalog : A2uiCoreCatalog {
    override val id = CATALOG_ID
    override val componentDefinitions = A2uiCoreComponentDefinitionCollection(
        listOf(
            Component("Column", "Vertical layout", mapOf("children" to A2uiChildListSchema.DEFAULT_INSTANCE), setOf("children")),
            Component("Card", "Elevated container", mapOf("child" to A2uiComponentIdSchema.DEFAULT_INSTANCE), setOf("child")),
            Component("Text", "Static or bound text", mapOf("text" to A2uiDynamicStringSchema.DEFAULT_INSTANCE, "variant" to A2uiStringSchema()), setOf("text")),
            Component("TextField", "Editable text bound to the data model", mapOf("label" to A2uiDynamicStringSchema.DEFAULT_INSTANCE, "value" to A2uiDynamicStringSchema.DEFAULT_INSTANCE), setOf("value")),
            Component("Button", "Fires an action", mapOf("child" to A2uiComponentIdSchema.DEFAULT_INSTANCE, "action" to A2uiActionSchema.DEFAULT_INSTANCE), setOf("child", "action")),
        )
    )
    override val functions = A2uiFunctionCollection()
}

/** Data model backed by Compose state, keyed by normalized JSON pointer. Nested maps/lists are flattened. */
// ponytail: flat key map, no ancestor reads (e.g. get("/lines") returns null); fine for leaf bindings only.
class MapDataModel : A2uiCoreDataModel {
    private val values = mutableStateMapOf<String, Any?>()

    override fun update(path: A2uiDataPath, value: Any?) = put(path.normalizedPath, value)

    override fun get(path: A2uiDataPath): Any? = values[path.normalizedPath]

    override fun close() = values.clear()

    private fun put(key: String, value: Any?) {
        when (value) {
            is Map<*, *> -> value.forEach { (k, v) -> put("$key/$k", v) }
            is List<*> -> value.forEachIndexed { i, v -> put("$key/$i", v) }
            else -> values[key] = value
        }
    }
}

/** Keeps the latest payload per component id; Compose reads it to render. */
class ComponentRegistry : A2uiCoreComponentRegistry {
    val components = mutableStateMapOf<String, A2uiComponentPayload>()

    override fun update(components: List<A2uiComponentPayload>) = components.forEach { this.components[it.id] = it }

    override fun reportError(id: String, exception: A2uiException) {}

    override fun close() = components.clear()
}

/** Owns the official message processor and the JSON parser that feeds it. */
class A2uiSession {
    val processor = A2uiCoreMessageProcessor(listOf(WordLensCatalog), ::MapDataModel, ::ComponentRegistry)
    private val parser = A2uiJsonMessageParser { JsonElementReader(it) }

    /** Must run for the processor to consume messages; call from a LaunchedEffect. */
    suspend fun run() = processor.collectMessages()

    fun send(json: String) = processor.processInput(parser, json)
}

/** [A2uiJsonReader] over a pre-tokenized kotlinx JSON tree (the engine ships no reader implementation). */
private class JsonElementReader(json: String) : A2uiJsonReader {
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
