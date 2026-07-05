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

// expect/actual seam (task 2.5): the original jsMain-only logic here persists a "controlled input"
// value across recompositions in a JS WeakMap keyed by the live DOM element, then re-applies it
// (element.value = / element.checked =) when the element gets recreated. JS keeps that logic
// verbatim (see InternalControlledInputUtils.js.kt, including the relocated JsWeakMap external
// interface). SSR has no live DOM and no cross-recomposition element identity to key a WeakMap on,
// so the JVM actual is inert -- an accepted v1 limitation for controlled-input SSR (see the SSR
// design doc).

internal expect fun restoreControlledInputState(inputElement: HTMLInputElement)

internal expect fun restoreControlledTextAreaState(element: HTMLTextAreaElement)

internal expect fun <V : Any> saveControlledInputState(element: DomHtmlElement, value: V)

@Composable
internal expect fun ElementScope<HTMLInputElement>.DisposeRadioGroupEffect()
