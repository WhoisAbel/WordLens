package com.whoisabel.wordlens.a2ui

import com.whoisabel.wordlens.a2ui.agent.TextInsights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextInsightsTest {
    @Test
    fun receipt() {
        val insights = TextInsights.analyze(
            listOf("INVOICE 2026-001", "Date: 28.10.2026", "Flat white 4.20", "Croissant 3.10", "Total: 42.50 EUR", "Thank you!")
        )
        assertEquals("42.50 EUR", insights.total)
        assertEquals(listOf("28.10.2026"), insights.dates)
        assertTrue("invoice number and date are not phone numbers: ${insights.phones}", insights.phones.isEmpty())
        assertTrue(!insights.hasContact)
    }

    @Test
    fun businessCard() {
        val insights = TextInsights.analyze(
            listOf("Alex Morgan", "Product Designer", "+49 151 2345 6789", "alex@wordlens.app", "www.wordlens.app")
        )
        assertEquals(listOf("+49 151 2345 6789"), insights.phones)
        assertEquals(listOf("alex@wordlens.app"), insights.emails)
        assertEquals(listOf("www.wordlens.app"), insights.links)
        assertEquals(null, insights.total)
    }

    @Test
    fun plainText() {
        assertTrue(TextInsights.analyze(listOf("The quick brown fox", "jumps over the lazy dog.")).isEmpty)
    }
}
