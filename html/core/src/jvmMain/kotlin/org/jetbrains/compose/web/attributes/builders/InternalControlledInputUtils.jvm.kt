/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes.builders

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.DomHtmlElement
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.HTMLInputElement
import org.jetbrains.compose.web.dom.HTMLTextAreaElement

// SSR has no live DOM and no cross-recomposition element identity to key a WeakMap on, so these
// are inert on JVM -- an accepted v1 limitation for controlled-input SSR (see the common expect
// file for the full rationale).

internal actual fun restoreControlledInputState(inputElement: HTMLInputElement) {
    // inert: SSR
}

internal actual fun restoreControlledTextAreaState(element: HTMLTextAreaElement) {
    // inert: SSR
}

internal actual fun <V : Any> saveControlledInputState(element: DomHtmlElement, value: V) {
    // inert: SSR
}

@Composable
internal actual fun ElementScope<HTMLInputElement>.DisposeRadioGroupEffect() {
    // inert: SSR
}
