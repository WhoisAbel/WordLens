package com.whoisabel.wordlens.a2ui

import androidx.a2ui.model.protocol.A2uiComponentPayload
import com.whoisabel.wordlens.a2ui.model.A2uiComponent
import org.junit.Assert.assertEquals
import org.junit.Test

class A2uiComponentTest {
    @Test
    fun payloadsBecomeTypedModels() {
        val binding = mapOf("path" to "/lines/0")
        assertEquals(
            A2uiComponent.TextField("f", "Line 1", binding),
            A2uiComponent.from(A2uiComponentPayload("f", "TextField", mapOf("label" to "Line 1", "value" to binding))),
        )
        assertEquals("/lines/0", A2uiComponent.TextField("f", null, binding).boundPath)
        assertEquals(
            A2uiComponent.Column("c", listOf("a", "b")),
            A2uiComponent.from(A2uiComponentPayload("c", "Column", mapOf("children" to listOf("a", "b")))),
        )
        assertEquals(A2uiComponent.Text("t", "hi", "body"), A2uiComponent.from(A2uiComponentPayload("t", "Text", mapOf("text" to "hi"))))
        assertEquals(
            A2uiComponent.Tabs("tabs", listOf(A2uiComponent.Tabs.Tab("Edit", "editTab"))),
            A2uiComponent.from(A2uiComponentPayload("tabs", "Tabs", mapOf("tabs" to listOf(mapOf("title" to "Edit", "child" to "editTab"))))),
        )
        assertEquals(
            A2uiComponent.Row("r", listOf("i"), justify = "center", align = "center"),
            A2uiComponent.from(A2uiComponentPayload("r", "Row", mapOf("children" to listOf("i"), "justify" to "center"))),
        )
        assertEquals(A2uiComponent.Icon("i", "search"), A2uiComponent.from(A2uiComponentPayload("i", "Icon", mapOf("name" to "search"))))
        assertEquals(A2uiComponent.Divider("d", vertical = false), A2uiComponent.from(A2uiComponentPayload("d", "Divider", emptyMap())))
        assertEquals(A2uiComponent.Unknown("x", "Video"), A2uiComponent.from(A2uiComponentPayload("x", "Video", emptyMap())))
    }
}
