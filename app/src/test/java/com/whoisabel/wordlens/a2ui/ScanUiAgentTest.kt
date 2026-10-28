package com.whoisabel.wordlens.a2ui

import com.whoisabel.wordlens.Blocks
import com.whoisabel.wordlens.a2ui.agent.COPY_EVENT
import com.whoisabel.wordlens.a2ui.agent.buildA2uiMessages
import com.whoisabel.wordlens.a2ui.agent.copyText
import com.whoisabel.wordlens.a2ui.engine.A2uiSession
import com.whoisabel.wordlens.a2ui.engine.ComponentRegistry

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanUiAgentTest {
    private fun components(blocks: Blocks): List<JsonObject> {
        val msgs = buildA2uiMessages("s", blocks).map { Json.parseToJsonElement(it).jsonObject }
        assertEquals(listOf("createSurface", "updateComponents", "updateDataModel"), msgs.map { (it.keys - "version").single() })
        assertEquals("v0.9", msgs[0]["version"]!!.jsonPrimitive.content)
        return msgs[1]["updateComponents"]!!.jsonObject["components"]!!.jsonArray.map { it.jsonObject }
    }

    private fun List<JsonObject>.count(type: String) = count { it["component"]!!.jsonPrimitive.content == type }

    @Test
    fun uiShapeFollowsRecognizedText() {
        val small = components(listOf(listOf("a")))
        val big = components(listOf(listOf("a", "b"), listOf("c"), listOf("d", "e", "f")))
        assertEquals(1, small.count("Card")); assertEquals(1, small.count("TextField"))
        assertEquals(3, big.count("Card")); assertEquals(6, big.count("TextField"))
    }

    @Test
    fun fieldsBindToDistinctDataPathsHoldingTheText() {
        val msgs = buildA2uiMessages("s", listOf(listOf("a", "b"), listOf("c"))).map { Json.parseToJsonElement(it).jsonObject }
        val paths = components(listOf(listOf("a", "b"), listOf("c"))).filter { it["component"]!!.jsonPrimitive.content == "TextField" }
            .map { it["value"]!!.jsonObject["path"]!!.jsonPrimitive.content }
        assertEquals(listOf("/lines/0", "/lines/1", "/lines/2"), paths)
        val lines = msgs[2]["updateDataModel"]!!.jsonObject["value"]!!.jsonObject["lines"]!!.jsonArray
        assertEquals(listOf("a", "b", "c"), lines.map { it.jsonPrimitive.content })
    }
}
