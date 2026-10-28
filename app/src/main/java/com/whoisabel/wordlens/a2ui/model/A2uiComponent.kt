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

    // Layout
    data class Column(override val id: String, val children: List<String>) : A2uiComponent
    data class Row(override val id: String, val children: List<String>, val justify: String, val align: String) : A2uiComponent
    data class Card(override val id: String, val child: String) : A2uiComponent
    data class Divider(override val id: String, val vertical: Boolean) : A2uiComponent
    data class Tabs(override val id: String, val tabs: List<Tab>) : A2uiComponent {
        data class Tab(val title: Any?, val child: String)
    }

    // Content
    /** [variant] is one of the spec's h1…h5, caption, body. */
    data class Text(override val id: String, val text: Any?, val variant: String) : A2uiComponent
    /** [name] is one of the spec's icon names, e.g. "search", "edit", "share". */
    data class Icon(override val id: String, val name: String) : A2uiComponent

    // Input
    data class TextField(override val id: String, val label: Any?, val value: Any?) : A2uiComponent {
        /** The data model path this field writes to, if its value is bound. */
        val boundPath: String? get() = (value as? Map<*, *>)?.get("path") as? String
    }
    /** [variant] is "default", "primary" (main call to action) or "borderless" (link-like). */
    data class Button(override val id: String, val child: String, val action: Map<String, Any?>, val variant: String) : A2uiComponent

    /** A type that is not in our catalog; the engine normally rejects these before we see them. */
    data class Unknown(override val id: String, val type: String) : A2uiComponent

    companion object {
        fun from(payload: A2uiComponentPayload): A2uiComponent {
            val p = payload.properties
            val id = payload.id
            return when (payload.type) {
                "Column" -> Column(id, p.ids("children"))
                "Row" -> Row(id, p.ids("children"), p["justify"] as? String ?: "start", p["align"] as? String ?: "center")
                "Card" -> Card(id, p["child"] as String)
                "Divider" -> Divider(id, vertical = p["axis"] == "vertical")
                "Tabs" -> Tabs(id, (p["tabs"] as List<*>).map { tab -> (tab as Map<*, *>).let { Tabs.Tab(it["title"], it["child"] as String) } })
                "Text" -> Text(id, p["text"], p["variant"] as? String ?: "body")
                "Icon" -> Icon(id, p["name"] as String)
                "TextField" -> TextField(id, p["label"], p["value"])
                "Button" -> Button(id, p["child"] as String, p.map("action"), p["variant"] as? String ?: "default")
                else -> Unknown(id, payload.type)
            }
        }

        private fun Map<String, Any?>.ids(key: String) = (this[key] as? List<*>).orEmpty().filterIsInstance<String>()

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any?>.map(key: String) = this[key] as? Map<String, Any?> ?: emptyMap()
    }
}
