package com.whoisabel.wordlens.a2ui.agent

/**
 * What the agent noticed in the recognized text. [ScanUiAgent] uses it to choose components:
 * a total becomes a big amount, a phone number becomes a "call" button, and so on.
 *
 * A real agent (an LLM) would make these decisions itself; here a few regexes stand in for it.
 */
// ponytail: regex heuristics, Latin-script and common formats only; an LLM or ML Kit Entity Extraction would generalize.
data class TextInsights(
    val total: String? = null,
    val dates: List<String> = emptyList(),
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val links: List<String> = emptyList(),
) {
    val hasContact get() = phones.isNotEmpty() || emails.isNotEmpty() || links.isNotEmpty()
    val isEmpty get() = total == null && dates.isEmpty() && !hasContact

    companion object {
        fun analyze(lines: List<String>): TextInsights {
            val emails = lines.flatMap { EMAIL.findAll(it).map { m -> m.value } }
            // Remove emails first so "shop.com" inside "hi@shop.com" is not also a link.
            val withoutEmails = lines.map { EMAIL.replace(it, " ") }
            val dates = lines.flatMap { DATE.findAll(it).map { m -> m.value } }
            return TextInsights(
                total = lines.firstOrNull { TOTAL_KEYWORD.containsMatchIn(it) }?.let { AMOUNT.find(it)?.value },
                dates = dates.distinct(),
                phones = withoutEmails.flatMap { PHONE.findAll(it).map { m -> m.value.trim() } }
                    .filter { phone -> phone.count(Char::isDigit) in 8..15 && dates.none { it in phone } }
                    .distinct(),
                emails = emails.distinct(),
                links = withoutEmails.flatMap { LINK.findAll(it).map { m -> m.value.trimEnd('.', ',') } }.distinct(),
            )
        }

        private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
        private val LINK = Regex("""(?i)\b(?:https?://|www\.)\S+|\b[a-z0-9-]+\.(?:com|org|net|io|dev|app|ai|de|co\.uk)\b(?:/\S*)?""")
        private val PHONE = Regex("""(?:\+|\b)\d[\d ()/.-]{6,}\d""")
        private val AMOUNT = Regex("""(?i)[$€£]\s?\d{1,6}(?:[.,]\d{2})?|\d{1,6}[.,]\d{2}\s?(?:EUR|USD|GBP|€|\$|£)?""")
        private val TOTAL_KEYWORD = Regex("""(?i)\b(?:total|sum|amount due|balance|gesamt|summe)\b""")
        private val DATE = Regex(
            """\b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b|\b\d{4}-\d{2}-\d{2}\b|""" +
                """(?i)\b\d{1,2} (?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.? \d{4}\b"""
        )
    }
}
