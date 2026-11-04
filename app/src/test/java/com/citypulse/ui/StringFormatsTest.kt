package com.citypulse.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** Un formato mal escrito ("%1 min" en vez de "%1$s") cierra la app al mostrar el texto. */
class StringFormatsTest {
    @Test
    fun `los textos con argumentos tienen un formato valido`() {
        val strings = File("src/main/res/values/strings.xml").readText()
        val invalid = Regex("""%(?!%|n\b|(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[sdfxXc]\b)""")
        assertEquals(emptyList<String>(), strings.lines().filter { invalid.containsMatchIn(it) })
    }
}
