package com.whoisabel.wordlens.a2ui.agent

import com.whoisabel.wordlens.a2ui.engine.WordLensCatalog
import com.whoisabel.wordlens.scan.Blocks
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/*
 * Plays the role of the A2UI *agent*: in a real app an LLM or server would send these messages.
 * Here they are generated locally from the ML Kit result.
 *
 * Generated layout (ids in quotes):
 *
 *   "root" Column
 *    ├ "header" Row ─ Icon "search", Text h3 "4 lines in 2 blocks"
 *    └ "tabs" Tabs
 *       ├ Edit    → "editTab" Column
 *       │            ├ "card0" Card → Column ─ Row(Icon, caption) · Divider · TextField per line
 *       │            ├ "card1" …
 *       │            └ "copy" Button → Row ─ Icon "share", Text "Copy all"
 *       └ Preview → "previewCard" Card → Column ─ one Text per line
 *
 * Data model: {"lines": ["INVOICE 2026-001", …]}. Every TextField and preview Text binds to /lines/<n>.
 */

const val COPY_EVENT = "copy_all"

/** Joins the resolved event context (keys are zero-padded line numbers) back into text. */
fun copyText(context: Map<String, Any?>): String = context.toSortedMap().values.joinToString("\n")

fun buildA2uiMessages(surfaceId: String, blocks: Blocks): List<String> {
    val lines = blocks.flatten()
    val components = buildList {
        addAll(header(blocks))
        add(component("tabs", "Tabs", "tabs" to tabsOf("Edit" to "editTab", "Preview" to "previewCard")))
        addAll(editTab(blocks))
        addAll(previewTab(lines.size))
        add(component("root", "Column", "children" to ids(listOf("header", "tabs"))))
    }
    return listOf(
        createSurface(surfaceId, WordLensCatalog.ID),
        updateComponents(surfaceId, components),
        updateDataModel(surfaceId, "/", obj("lines" to ids(lines))),
    )
}

private fun header(blocks: Blocks) = listOf(
    component("header", "Row", "align" to str("center"), "children" to ids(listOf("headerIcon", "summary"))),
    component("headerIcon", "Icon", "name" to str("search")),
    component("summary", "Text", "variant" to str("h3"), "text" to str("${blocks.sumOf { it.size }} lines in ${blocks.size} blocks")),
)

/** One Card per ML Kit block, each with a TextField per line, then the Copy all button. */
private fun editTab(blocks: Blocks): List<JsonObject> = buildList {
    var lineIndex = 0
    blocks.forEachIndexed { b, blockLines ->
        val fieldIds = blockLines.indices.map { l ->
            val id = "field${lineIndex}"
            add(component(id, "TextField", "label" to str("Line ${l + 1}"), "value" to bound("/lines/${lineIndex++}")))
            id
        }
        add(component("card$b", "Card", "child" to str("block$b")))
        add(component("block$b", "Column", "children" to ids(listOf("blockHeader$b", "divider$b") + fieldIds)))
        add(component("blockHeader$b", "Row", "children" to ids(listOf("blockIcon$b", "blockTitle$b"))))
        add(component("blockIcon$b", "Icon", "name" to str("edit")))
        add(component("blockTitle$b", "Text", "variant" to str("caption"), "text" to str("Block ${b + 1} · ${blockLines.size} lines")))
        add(component("divider$b", "Divider"))
    }
    addAll(copyButton(lineCount = lineIndex))
    add(component("editTab", "Column", "children" to ids(blocks.indices.map { "card$it" } + "copy")))
}

/**
 * The button's event context binds every line, so when it is pressed the engine resolves the
 * *edited* values and sends them in the outbound event.
 */
private fun copyButton(lineCount: Int): List<JsonObject> {
    val context = JsonObject(List(lineCount) { "%04d".format(it) to bound("/lines/$it") }.toMap())
    return listOf(
        component("copy", "Button", "child" to str("copyContent"), "action" to obj("event" to obj("name" to str(COPY_EVENT), "context" to context))),
        component("copyContent", "Row", "justify" to str("center"), "children" to ids(listOf("copyIcon", "copyLabel"))),
        component("copyIcon", "Icon", "name" to str("share")),
        component("copyLabel", "Text", "text" to str("Copy all")),
    )
}

/** Read-only view of the same data model paths; it updates live while the user edits. */
private fun previewTab(lineCount: Int): List<JsonObject> {
    val textIds = List(lineCount) { "preview$it" }
    return textIds.mapIndexed { n, id -> component(id, "Text", "text" to bound("/lines/$n")) } + listOf(
        component("previewCard", "Card", "child" to str("previewColumn")),
        component("previewColumn", "Column", "children" to ids(textIds)),
    )
}

private fun tabsOf(vararg tabs: Pair<String, String>) =
    JsonArray(tabs.map { (title, child) -> obj("title" to str(title), "child" to str(child)) })
