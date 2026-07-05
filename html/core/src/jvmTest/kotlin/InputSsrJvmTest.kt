/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.TextArea
import org.jetbrains.compose.web.renderToString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Task 4.1b regression test.
 *
 * On JVM, `Input`, `TextArea` and `Select` read `scopeElement` from within an
 * `ElementScope<HTMLXxxElement>` whose type parameter is already substituted with a concrete
 * subtype at the call site (e.g. `restoreControlledInputState(inputElement = scopeElement)` in
 * `Elements.kt`). That substitution makes the compiler emit a real checkcast to the concrete
 * subtype, which used to throw `ClassCastException` because `createNativeElement` (JVM-only,
 * `ElementBuilder.jvm.kt`) constructed every element as a bare `object : DomElement(tagName) {}`,
 * regardless of tag. `createNativeElement` now maps each tag name to its matching `DomElement`
 * subtype (e.g. `input` -> `HTMLInputElement`), so these checkcasts succeed and `renderToString`
 * no longer crashes for these composables.
 *
 * Controlled `value`/`checked` are documented as inert on JVM SSR (no live DOM to reflect state
 * into) -- these tests only assert that rendering doesn't crash and that the static attributes
 * passed through `attrs` show up in the serialized markup.
 */
class InputSsrJvmTest {
    @Test
    fun rendersInputWithoutCrashing() {
        val html = renderToString {
            Input(type = InputType.Text, attrs = { id("n") })
        }
        // input is a void tag: no closing tag, no children. `type` is applied before the
        // user's `attrs` block runs (see Input in Elements.kt), so it precedes `id` in the
        // (insertion-ordered) attribute map.
        assertEquals("""<input type="text" id="n">""", html)
    }

    @Test
    fun rendersTextAreaWithoutCrashing() {
        val html = renderToString {
            TextArea(attrs = { id("ta") })
        }
        assertEquals("""<textarea id="ta"></textarea>""", html)
    }

    @Test
    fun rendersSelectWithoutCrashing() {
        val html = renderToString {
            Select(attrs = { id("s") }) {
                Option("1") { }
            }
        }
        assertEquals("""<select id="s"><option value="1"></option></select>""", html)
    }
}
