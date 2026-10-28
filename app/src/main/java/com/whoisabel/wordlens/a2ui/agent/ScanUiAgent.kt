package com.whoisabel.wordlens.a2ui.agent

import com.whoisabel.wordlens.Blocks
import com.whoisabel.wordlens.a2ui.engine.WordLensCatalog
import kotlinx.serialization.json.JsonObject

/*
 * Plays the role of the A2UI *agent*: in a real app an LLM or server would send these messages.
 * Here they are generated locally from the ML Kit result.
 */

const val COPY_EVENT = "copy_all"

/** Joins the resolved event context (keys are zero-padded line numbers) back into text. */
fun copyText(context: Map<String, Any?>): String = context.toSortedMap().values.joinToString("\n")

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
    // The event context binds every field, so the engine resolves the *edited* values when the button is pressed.
    val context = JsonObject(List(total) { "%04d".format(it) to bound("/lines/$it") }.toMap())
    components += obj("id" to str("copyLabel"), "component" to str("Text"), "text" to str("Copy all"))
    components += obj(
        "id" to str("copy"), "component" to str("Button"), "child" to str("copyLabel"),
        "action" to obj("event" to obj("name" to str(COPY_EVENT), "context" to context)),
    )
    components += obj("id" to str("root"), "component" to str("Column"), "children" to strs(listOf("summary") + cardIds + "copy"))

    return listOf(
        createSurface(surfaceId, WordLensCatalog.ID),
        updateComponents(surfaceId, components),
        updateDataModel(surfaceId, "/", obj("lines" to strs(blocks.flatten()))),
    )
}
