package com.whoisabel.wordlens.a2ui.model

import androidx.a2ui.model.protocol.A2uiComponentPayload

/**
 * Typed view of an A2UI component. The engine hands us untyped payloads
 * (`type = "Text", properties = {"text": …}`); [from] converts each one once so renderers
 * work with real fields instead of string keys.
 *
 * Dynamic values (text, labels, field values) stay raw as `Any?`: either a literal or a
 * `{"path": "/…"}` binding. Renderers resolve them through the engine at draw time.
 */
sealed interface A2uiComponent {
    val id: String

    data class Column(override val id: String, val children: List<String>) : A2uiComponent
    data class Card(override val id: String, val child: String) : A2uiComponent
    data class Text(override val id: String, val text: Any?, val variant: String) : A2uiComponent
    data class TextField(override val id: String, val label: Any?, val value: Any?) : A2uiComponent {
        /** The data model path this field writes to, if its value is bound. */
        val boundPath: String? get() = (value as? Map<*, *>)?.get("path") as? String
    }
    data class Button(override val id: String, val child: String, val action: Map<String, Any?>) : A2uiComponent

    /** A type that is not in our catalog; the engine normally rejects these before we see them. */
    data class Unknown(override val id: String, val type: String) : A2uiComponent

    companion object {
        fun from(payload: A2uiComponentPayload): A2uiComponent {
            val p = payload.properties
            val id = payload.id
            return when (payload.type) {
                "Column" -> Column(id, p.ids("children"))
                "Card" -> Card(id, p["child"] as String)
                "Text" -> Text(id, p["text"], p["variant"] as? String ?: "body")
                "TextField" -> TextField(id, p["label"], p["value"])
                "Button" -> Button(id, p["child"] as String, p.map("action"))
                else -> Unknown(id, payload.type)
            }
        }

        private fun Map<String, Any?>.ids(key: String) = (this[key] as? List<*>).orEmpty().filterIsInstance<String>()

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any?>.map(key: String) = this[key] as? Map<String, Any?> ?: emptyMap()
    }
}
