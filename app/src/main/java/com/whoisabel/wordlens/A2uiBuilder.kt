package com.whoisabel.wordlens

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun obj(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))
private fun str(s: String) = JsonPrimitive(s)
private fun strs(vararg s: String) = JsonArray(s.map(::JsonPrimitive))

private fun envelope(key: String, body: JsonObject): String =
    buildJsonObject { put("version", "v0.9"); put(key, body) }.toString()

fun deleteSurface(surfaceId: String) = envelope("deleteSurface", obj("surfaceId" to str(surfaceId)))

/** Messages that show all recognized text in a single bound Text component. */
fun buildA2uiMessages(surfaceId: String, blocks: Blocks): List<String> = listOf(
    envelope("createSurface", obj("surfaceId" to str(surfaceId), "catalogId" to str(CATALOG_ID))),
    envelope(
        "updateComponents",
        obj(
            "surfaceId" to str(surfaceId),
            "components" to JsonArray(
                listOf(
                    obj("id" to str("root"), "component" to str("Column"), "children" to strs("text")),
                    obj("id" to str("text"), "component" to str("Text"), "text" to obj("path" to str("/text"))),
                )
            ),
        )
    ),
    envelope(
        "updateDataModel",
        obj("surfaceId" to str(surfaceId), "path" to str("/"), "value" to obj("text" to str(blocks.joinToString("\n\n") { it.joinToString("\n") }))),
    ),
)
