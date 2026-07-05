/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.dom.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenderToStringTest {
    @Test fun rendersSimpleTree() {
        val html = renderToString {
            Div({ classes("greeting") }) {
                Text("Hello")
                Span { Text("!") }
            }
        }
        assertEquals("""<div class="greeting">Hello<span>!</span></div>""", html)
    }

    @Test fun rendersConditionalStateAfterSettle() {
        val html = renderToString {
            var shown = androidx.compose.runtime.remember { true }
            if (shown) P { Text("visible") }
        }
        assertEquals("<p>visible</p>", html)
    }

    @Test fun rendersTagsNotCoveredByOtherTests() {
        val tableHtml = renderToString {
            Table {
                Tr {
                    Td { Text("cell") }
                }
            }
        }
        assertEquals("<table><tr><td>cell</td></tr></table>", tableHtml)

        val buttonHtml = renderToString {
            Button { Text("click") }
        }
        assertEquals("<button>click</button>", buttonHtml)

        val listHtml = renderToString {
            Ul {
                Li { Text("one") }
                Li { Text("two") }
            }
        }
        assertEquals("<ul><li>one</li><li>two</li></ul>", listHtml)

        val linkHtml = renderToString {
            A(href = "https://example.com") { Text("link") }
        }
        assertEquals("""<a href="https://example.com">link</a>""", linkHtml)
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test fun doesNotLeakRecomposerOnHappyPath() {
        val before = Recomposer.runningRecomposers.value.size

        renderToString {
            Div { Text("no leak here") }
        }

        val after = Recomposer.runningRecomposers.value.size
        assertEquals(before, after)
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test fun doesNotLeakRecomposerWhenContentThrows() {
        val before = Recomposer.runningRecomposers.value.size

        assertFailsWith<RuntimeException> {
            renderToString {
                throw RuntimeException("boom")
            }
        }

        val after = Recomposer.runningRecomposers.value.size
        assertEquals(before, after)
    }
}
