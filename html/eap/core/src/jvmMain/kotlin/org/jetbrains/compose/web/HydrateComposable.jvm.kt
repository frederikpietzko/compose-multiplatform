/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.ParentNode
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.ElementScope

private fun notOnJVM(): Nothing = error("Should not be called on JVM")

internal actual fun reportHydrationMismatch(mismatch: HydrationMismatchException) {
    notOnJVM()
}

actual val browserDocument: ParentNode
    get() = notOnJVM()

actual fun <T> hydrateRoot(
    deserializeState: (String) -> T,
    monotonicFrameClock: MonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit,
    within: ParentNode,
    hydrationId: String?,
    content: @Composable (ElementScope<HTMLDivElement>.(T) -> Unit)
): Composition {
    notOnJVM()
}

internal actual fun <TElement : Element> hydrateComposableWithMode(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit,
    validationMode: HtmlValidationMode,
    content: @Composable (DOMScope<TElement>.() -> Unit)
): Composition {
    notOnJVM()
}

internal actual fun <TElement : Element> hydrateOnce(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    validationMode: HtmlValidationMode,
    content: @Composable (DOMScope<TElement>.() -> Unit)
): Composition {
    notOnJVM()
}

actual fun <T> rehydrateRoot(
    html: String,
    deserializeState: (String) -> T,
    monotonicFrameClock: MonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit,
    within: ParentNode,
    hydrationId: String?,
    content: @Composable (ElementScope<HTMLDivElement>.(T) -> Unit)
): Composition {
    notOnJVM()
}