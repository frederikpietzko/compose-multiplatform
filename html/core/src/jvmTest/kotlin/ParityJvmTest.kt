/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.core.tests.parity.SampleTree
import org.jetbrains.compose.web.renderToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Task 4.1: cross-target parity test, JVM half.
 *
 * Renders the exact same `SampleTree` composable from commonTest (see
 * `commonTest/kotlin/ParityComposables.kt`) via the JVM-only `renderToString` SSR entry point,
 * and asserts on the resulting HTML string. The jsTest half (`ParityJsTest`) renders the same
 * composable into a real DOM and checks the same structure, proving the shared composable API
 * behaves identically on both targets.
 */
class ParityJvmTest {
    @Test
    fun sampleTreeRenders() {
        val html = renderToString { SampleTree() }

        assertEquals(
            "<div class=\"container parity\" style=\"color:red;\" id=\"root\">" +
                "<span class=\"label\">Hello</span>" +
                "<div class=\"body\"><p>World</p></div>" +
                "<span class=\"footer\">Footer</span>" +
                "</div>",
            html
        )
    }

    @Test
    fun sampleTreeStructuralAssertions() {
        val html = renderToString { SampleTree() }

        // Nested elements (Div nested two levels deep, Span, P)
        assertTrue(html.contains("<div"))
        assertTrue(html.contains("<span"))
        assertTrue(html.contains("<p>World</p>"))
        assertTrue(html.contains("<div class=\"body\">"))

        // Attributes
        assertTrue(html.contains("id=\"root\""))
        assertTrue(html.contains("class=\"container parity\""))

        // Inline style via the common style API
        assertTrue(html.contains("style=\"color:red;\""))

        // Text content
        assertTrue(html.contains("Hello"))
        assertTrue(html.contains("World"))

        // Conditional branch (remember + if) resolved to true
        assertTrue(html.contains("Footer"))
    }
}
