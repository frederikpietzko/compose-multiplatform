package org.jetbrains.compose.web.core.tests.parity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * A representative slice of the shared Compose HTML composable API (task 4.1), used by both
 * jvmTest (via `renderToString`) and jsTest (via `renderComposable`) to prove that the exact
 * same commonMain composable compiles AND renders consistently on both targets.
 *
 * Deliberately kept to commonMain-available API only:
 *  - nested elements: Div/Span/P (Div nested two levels deep)
 *  - attributes: `id`, `classes`
 *  - inline style via the common `style { property(...) }` API (NOT the typed CSS DSL --
 *    `color(Color.red)` / `5.px` and friends live in jsMain only and would fail to compile here)
 *  - text content via `Text`
 *  - a conditional branch driven by `remember` (no mutable state / recomposition involved,
 *    so the output is deterministic across both targets)
 *
 * `Input` is intentionally NOT exercised here: on the JVM target, `createNativeElement`
 * (ElementBuilder.jvm.kt) always creates a plain `DomElement`, never the tag-specific subtype
 * (e.g. `HTMLInputElement`). `Input`'s controlled-state `DisposableEffect` reads `scopeElement`
 * typed as `HTMLInputElement`, so that cast throws `ClassCastException` under `renderToString`
 * today -- a pre-existing SSR limitation, orthogonal to this parity test, so it's left alone here.
 *
 * No event listeners are exercised either: SSR (JVM) has no live DOM to dispatch events on, so
 * event assertions are intentionally out of scope for this parity test.
 */
@Composable
fun SampleTree() {
    val showFooter = remember { true }

    Div({
        id("root")
        classes("container", "parity")
        style {
            property("color", "red")
        }
    }) {
        Span({ classes("label") }) {
            Text("Hello")
        }
        Div({ classes("body") }) {
            P {
                Text("World")
            }
        }
        if (showFooter) {
            Span({ classes("footer") }) {
                Text("Footer")
            }
        }
    }
}
