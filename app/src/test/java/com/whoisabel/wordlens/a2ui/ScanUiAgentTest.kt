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
        val components = components(listOf(listOf("a", "Total: 1.00 EUR"), listOf("01.02.2026", "+49 151 2345 6789", "a@b.io", "www.b.io")))
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

    private fun List<JsonObject>.byId(id: String) = single { it["id"]!!.jsonPrimitive.content == id }
    private fun List<JsonObject>.hasId(id: String) = any { it["id"]!!.jsonPrimitive.content == id }

    @Test
    fun receiptGetsABigTotal() {
        val components = components(listOf(listOf("INVOICE 2026-001"), listOf("Total: 42.50 EUR")))
        val amount = components.byId("totalAmount")
        assertEquals("h2", amount["variant"]!!.jsonPrimitive.content)
        assertEquals("42.50 EUR", amount["text"]!!.jsonPrimitive.content)
        assertEquals("Looks like a receipt", components.byId("kind")["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun businessCardGetsOpenUrlButtons() {
        val components = components(listOf(listOf("Alex Morgan", "+49 151 2345 6789", "alex@wordlens.app")))
        fun urlOf(id: String) = components.byId(id)["action"]!!.jsonObject["functionCall"]!!.jsonObject
            .also { assertEquals("openUrl", it["call"]!!.jsonPrimitive.content) }["args"]!!.jsonObject["url"]!!.jsonPrimitive.content
        assertEquals("tel:+4915123456789", urlOf("phone0"))
        assertEquals("mailto:alex@wordlens.app", urlOf("email0"))
        assertTrue(!components.hasId("total"))
    }

    @Test
    fun plainTextHasNoInsightsCard() {
        val components = components(listOf(listOf("The quick brown fox")))
        assertTrue(!components.hasId("insights"))
        assertEquals(listOf("header", "tabs"), components.byId("root")["children"]!!.jsonArray.map { it.jsonPrimitive.content })
    }
}
