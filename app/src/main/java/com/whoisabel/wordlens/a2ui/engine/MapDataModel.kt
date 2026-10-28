package com.whoisabel.wordlens.a2ui.engine

import androidx.a2ui.engine.platform.A2uiCoreDataModel
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.compose.runtime.mutableStateMapOf

/**
 * The surface's data model: values that components bind to with `{"path": "/lines/0"}`.
 * Backed by Compose state, so any write (from the agent or from a TextField) recomposes readers.
 */
// ponytail: flat key map, no ancestor reads (e.g. get("/lines") returns null); fine for leaf bindings only.
class MapDataModel : A2uiCoreDataModel {
    private val values = mutableStateMapOf<String, Any?>()

    override fun update(path: A2uiDataPath, value: Any?) = put(path.normalizedPath, value)

    override fun get(path: A2uiDataPath): Any? = values[path.normalizedPath]

    override fun close() = values.clear()

    // Nested objects and lists are flattened into one entry per leaf: {"lines": ["a"]} -> "/lines/0" = "a".
    private fun put(key: String, value: Any?) {
        when (value) {
            is Map<*, *> -> value.forEach { (k, v) -> put("$key/$k", v) }
            is List<*> -> value.forEachIndexed { i, v -> put("$key/$i", v) }
            else -> values[key] = value
        }
    }
}
