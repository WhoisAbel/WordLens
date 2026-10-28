package com.whoisabel.wordlens.a2ui.agent

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Small helpers for writing A2UI v0.9 JSON by hand. */

internal fun obj(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))
internal fun str(s: String) = JsonPrimitive(s)
internal fun strs(s: List<String>) = JsonArray(s.map(::JsonPrimitive))
internal fun bound(path: String) = obj("path" to str(path))

private fun envelope(key: String, body: JsonObject): String =
    buildJsonObject { put("version", "v0.9"); put(key, body) }.toString()

fun deleteSurface(surfaceId: String) = envelope("deleteSurface", obj("surfaceId" to str(surfaceId)))

/** Every message the agent can send. */
fun createSurface(surfaceId: String, catalogId: String) =
    envelope("createSurface", obj("surfaceId" to str(surfaceId), "catalogId" to str(catalogId)))

fun updateComponents(surfaceId: String, components: List<JsonObject>) =
    envelope("updateComponents", obj("surfaceId" to str(surfaceId), "components" to JsonArray(components)))

fun updateDataModel(surfaceId: String, path: String, value: JsonElement) =
    envelope("updateDataModel", obj("surfaceId" to str(surfaceId), "path" to str(path), "value" to value))
