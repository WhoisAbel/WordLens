package com.whoisabel.wordlens.a2ui.agent

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Helpers for writing A2UI v0.9 JSON by hand.
 *
 * Every message is an envelope: {"version": "v0.9", "<messageType>": { …body… }}
 * The agent sends four message types: createSurface, updateComponents, updateDataModel, deleteSurface.
 */

private const val PROTOCOL_VERSION = "v0.9"

private fun envelope(messageType: String, body: JsonObject): String =
    buildJsonObject { put("version", PROTOCOL_VERSION); put(messageType, body) }.toString()

/** Opens a new surface (a UI region) that will draw components from the given catalog. */
fun createSurface(surfaceId: String, catalogId: String) =
    envelope("createSurface", obj("surfaceId" to str(surfaceId), "catalogId" to str(catalogId)))

/** Adds or replaces components. Components form a flat list that references children by id. */
fun updateComponents(surfaceId: String, components: List<JsonObject>) =
    envelope("updateComponents", obj("surfaceId" to str(surfaceId), "components" to JsonArray(components)))

/** Writes [value] into the surface's data model at [path] (a JSON pointer such as "/" or "/lines/0"). */
fun updateDataModel(surfaceId: String, path: String, value: JsonElement) =
    envelope("updateDataModel", obj("surfaceId" to str(surfaceId), "path" to str(path), "value" to value))

fun deleteSurface(surfaceId: String) = envelope("deleteSurface", obj("surfaceId" to str(surfaceId)))

/** One component: `{"id": …, "component": type, …properties}`. */
internal fun component(id: String, type: String, vararg properties: Pair<String, JsonElement>) =
    obj("id" to str(id), "component" to str(type), *properties)

/** A data binding: the renderer reads (and for inputs, writes) the value at [path]. */
internal fun bound(path: String) = obj("path" to str(path))

internal fun obj(vararg pairs: Pair<String, JsonElement>) = JsonObject(mapOf(*pairs))
internal fun str(s: String) = JsonPrimitive(s)
internal fun ids(ids: List<String>) = JsonArray(ids.map(::JsonPrimitive))
