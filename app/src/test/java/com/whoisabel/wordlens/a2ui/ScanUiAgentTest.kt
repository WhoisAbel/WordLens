package com.whoisabel.wordlens.a2ui

import com.whoisabel.wordlens.a2ui.agent.buildA2uiMessages
import com.whoisabel.wordlens.scan.Blocks
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanUiAgentTest {
    private fun messages(blocks: Blocks) = buildA2uiMessages("s", blocks).map { Json.parseToJsonElement(it).jsonObject }

    private fun components(blocks: Blocks): List<JsonObject> {
        val msgs = messages(blocks)
        assertEquals(listOf("createSurface", "updateComponents", "updateDataModel"), msgs.map { (it.keys - "version").single() })
        assertEquals("v0.9", msgs[0]["version"]!!.jsonPrimitive.content)
        return msgs[1]["updateComponents"]!!.jsonObject["components"]!!.jsonArray.map { it.jsonObject }
    }

    private fun JsonObject.type() = this["component"]!!.jsonPrimitive.content
    private fun List<JsonObject>.count(type: String) = count { it.type() == type }

    @Test
    fun uiShapeFollowsRecognizedText() {
        val small = components(listOf(listOf("a")))
        val big = components(listOf(listOf("a", "b"), listOf("c"), listOf("d", "e", "f")))
        // One Card per block, plus the Preview tab's card.
        assertEquals(2, small.count("Card")); assertEquals(1, small.count("TextField"))
        assertEquals(4, big.count("Card")); assertEquals(6, big.count("TextField"))
        assertEquals(3, big.count("Divider"))
        assertEquals(1, big.count("Tabs"))
    }

    @Test
    fun everyReferencedChildExists() {
        val components = components(listOf(listOf("a", "b"), listOf("c")))
        val ids = components.map { it["id"]!!.jsonPrimitive.content }.toSet()
        val referenced = components.flatMap { c ->
            listOfNotNull(c["child"]?.jsonPrimitive?.content) +
                c["children"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content } +
                c["tabs"]?.jsonArray.orEmpty().map { it.jsonObject["child"]!!.jsonPrimitive.content }
        }
        assertTrue("root" in ids)
        assertTrue("missing: ${referenced - ids}", ids.containsAll(referenced))
    }

    @Test
    fun fieldsAndPreviewBindToTheSameLines() {
        val blocks = listOf(listOf("a", "b"), listOf("c"))
        val components = components(blocks)
        fun pathsOf(type: String, key: String) = components.filter { it.type() == type }
            .mapNotNull { (it[key] as? JsonObject)?.get("path")?.jsonPrimitive?.content }
        val expected = listOf("/lines/0", "/lines/1", "/lines/2")
        assertEquals(expected, pathsOf("TextField", "value"))
        assertEquals(expected, pathsOf("Text", "text"))

        val lines = messages(blocks)[2]["updateDataModel"]!!.jsonObject["value"]!!.jsonObject["lines"]!!.jsonArray
        assertEquals(listOf("a", "b", "c"), lines.map { (it as JsonPrimitive).content })
    }
}
