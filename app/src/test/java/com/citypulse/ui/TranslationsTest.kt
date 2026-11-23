package com.citypulse.ui

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Cada texto en español tiene su versión en inglés, con los mismos valores (%1$s, %2$d…). */
class TranslationsTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.exists() }

    private data class Entry(val text: String, val translatable: Boolean)

    private fun read(folder: String): Map<String, Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$folder/strings.xml"))
        val out = mutableMapOf<String, Entry>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            out[e.getAttribute("name")] = Entry(e.textContent, e.getAttribute("translatable") != "false")
        }
        val plurals = doc.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val e = plurals.item(i) as Element
            val other = (0 until e.getElementsByTagName("item").length).map { e.getElementsByTagName("item").item(it) as Element }
                .first { it.getAttribute("quantity") == "other" }
            out["plurals:" + e.getAttribute("name")] = Entry(other.textContent, true)
        }
        return out
    }

    private fun placeholders(text: String) = Regex("""%(?:\d\$)?[sd]""").findAll(text).map { it.value }.toSortedSet()

    @Test fun `todo texto en espanol tiene su traduccion al ingles`() {
        val spanish = read("values")
        val english = read("values-en")
        val missing = spanish.filter { it.value.translatable && it.key !in english }.keys
        assertTrue("Faltan en inglés: $missing", missing.isEmpty())
        val extra = english.keys - spanish.keys
        assertTrue("Sobran en inglés: $extra", extra.isEmpty())
    }

    @Test fun `las traducciones llevan los mismos valores`() {
        val spanish = read("values")
        read("values-en").forEach { (name, entry) ->
            assertEquals("Valores distintos en $name", placeholders(spanish.getValue(name).text), placeholders(entry.text))
        }
    }
}
