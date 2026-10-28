package com.whoisabel.wordlens

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun obj(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))
private fun str(s: String) = JsonPrimitive(s)
private fun strs(s: List<String>) = JsonArray(s.map(::JsonPrimitive))
private fun bound(path: String) = obj("path" to str(path))

private fun envelope(key: String, body: JsonObject): String =
    buildJsonObject { put("version", "v0.9"); put(key, body) }.toString()

fun deleteSurface(surfaceId: String) = envelope("deleteSurface", obj("surfaceId" to str(surfaceId)))

/**
 * ML Kit result -> A2UI v0.9 messages (createSurface, updateComponents, updateDataModel).
 * Layout: a summary Text, then one Card per text block holding one TextField per line.
 * Every field is bound to /lines/<n>, so the UI shape is driven entirely by the recognized text.
 */
fun buildA2uiMessages(surfaceId: String, blocks: Blocks): List<String> {
    val components = mutableListOf<JsonObject>()
    val cardIds = mutableListOf<String>()
    var line = 0
    blocks.forEachIndexed { b, lines ->
        val fieldIds = lines.map { "field${line++}" }
        lines.indices.forEach { l ->
            val n = line - lines.size + l
            components += obj(
                "id" to str(fieldIds[l]), "component" to str("TextField"),
                "label" to str("Block ${b + 1} · line ${l + 1}"), "value" to bound("/lines/$n"),
            )
        }
        components += obj("id" to str("col$b"), "component" to str("Column"), "children" to strs(fieldIds))
        components += obj("id" to str("card$b"), "component" to str("Card"), "child" to str("col$b"))
        cardIds += "card$b"
    }
    val total = blocks.sumOf { it.size }
    components += obj(
        "id" to str("summary"), "component" to str("Text"), "variant" to str("title"),
        "text" to str("$total lines in ${blocks.size} blocks"),
    )
    components += obj("id" to str("root"), "component" to str("Column"), "children" to strs(listOf("summary") + cardIds))

    return listOf(
        envelope("createSurface", obj("surfaceId" to str(surfaceId), "catalogId" to str(CATALOG_ID))),
        envelope("updateComponents", obj("surfaceId" to str(surfaceId), "components" to JsonArray(components))),
        envelope(
            "updateDataModel",
            obj("surfaceId" to str(surfaceId), "path" to str("/"), "value" to obj("lines" to strs(blocks.flatten()))),
        ),
    )
}
