package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.core.tests.parity.SampleTree
import org.jetbrains.compose.web.testutils.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Task 4.1: cross-target parity test, JS half.
 *
 * Compiles and renders the exact same `SampleTree` composable from commonTest (see
 * `commonTest/kotlin/ParityComposables.kt`) into a real browser DOM via the existing
 * `html-test-utils` harness (`runTest`/`composition`), and asserts on the resulting
 * `innerHTML`. The jvmTest half (`ParityJvmTest`) renders the same composable via
 * `renderToString` and asserts on the same structure, proving the shared composable API
 * behaves identically on both targets.
 */
class ParityJsTest {
    @Test
    fun sampleTreeRenders() = runTest {
        composition {
            SampleTree()
        }

        assertEquals(
            "<div class=\"container parity\" style=\"color: red;\" id=\"root\">" +
                "<span class=\"label\">Hello</span>" +
                "<div class=\"body\"><p>World</p></div>" +
                "<span class=\"footer\">Footer</span>" +
                "</div>",
            root.innerHTML
        )
    }
}
