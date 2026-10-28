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
        assertEquals(A2uiComponent.Unknown("x", "Video"), A2uiComponent.from(A2uiComponentPayload("x", "Video", emptyMap())))
    }
}
