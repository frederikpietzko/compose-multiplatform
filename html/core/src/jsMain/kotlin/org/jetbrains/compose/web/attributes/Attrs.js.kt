/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.attributes.builders.saveControlledInputState
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLTextAreaElement

// Bodies moved verbatim from the pre-split jsMain Attrs.kt (task 2.5). See the commonMain Attrs.kt
// for the rationale.

internal actual val setInputValue: (HTMLInputElement, String) -> Unit = { e, v ->
    if (v != e.value) {
        e.value = v
    }
    saveControlledInputState(e, v)
}

internal actual val setTextAreaDefaultValue: (HTMLTextAreaElement, String) -> Unit = { e, v ->
    e.innerText = v
}

internal actual val setCheckedValue: (HTMLInputElement, Boolean) -> Unit = { e, v ->
    e.checked = v
    saveControlledInputState(e, v)
}

// AutoComplete is a JS `external interface` zero-cost branding type, so these 3 overloads stay
// jsMain-only (see task-2.5 report) even though the rest of Attrs.kt is now common.

fun AttrsScope<HTMLInputElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsScope<HTMLSelectElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())

fun AttrsScope<HTMLTextAreaElement>.autoComplete(value: AutoComplete) =
    attr("autocomplete", value.unsafeCast<String>())
