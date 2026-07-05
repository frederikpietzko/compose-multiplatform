/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.dom.Style
import org.jetbrains.compose.web.renderToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StyleSsrJvmTest {
    @Test
    fun styleChildCombinatorIsRenderedAsUnescapedCss() {
        val html = renderToString {
            Style {
                style(child(type("div"), type("p"))) {
                    property("color", "red")
                }
            }
        }

        // The `>` child combinator must survive verbatim inside <style> -- it is HTML "raw text",
        // so entity-escaping it (as regular element text is escaped) would produce invalid CSS.
        assertFalse(html.contains("&gt;"), "expected no HTML-escaped '>' inside <style>, got: $html")
        assertTrue(html.contains("div > p"), "expected literal CSS child combinator, got: $html")
        assertEquals(
            "<style>div > p {\n    color: red;\n}</style>",
            html
        )
    }
}
