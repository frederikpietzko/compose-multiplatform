/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.NativeNode
import org.jetbrains.compose.web.internal.runtime.NativeNodeWrapper

/**
 * Generic, platform-agnostic composition-wiring shared by every `renderComposable*` entry point.
 *
 * This builds a [Recomposer] driven by [monotonicFrameClock] and [dispatcher], wires up a
 * [ControlledComposition] over a [DomApplier] rooted at [root], sets [content] on it and returns
 * the resulting [Composition].
 *
 * This function is intentionally free of any platform dependency (no `kotlinx.browser`, no
 * platform-specific snapshot bootstrapping): the frame clock and dispatcher are injected by the
 * caller, so this is shared by jsMain's `renderComposable(root: Element, ...)` (which injects
 * [org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher] and a browser frame clock)
 * and, from Task 3.3 onward, jvmMain's `renderToString`. Any platform-specific bootstrapping that
 * used to happen alongside the generic wiring (e.g. JS's `GlobalSnapshotManager.ensureStarted()`)
 * remains the caller's responsibility and is NOT performed here.
 *
 * @param root - the root [NativeNode] of the platform-specific tree managed by Compose
 * @param monotonicFrameClock - the [MonotonicFrameClock] driving recomposition frame timing
 * @param dispatcher - the [CoroutineDispatcher] the [Recomposer]'s recompose-and-apply loop runs on
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
@ComposeWebInternalApi
fun <TElement : NativeNode> renderComposableRoot(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    dispatcher: CoroutineDispatcher,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    val context = monotonicFrameClock + dispatcher
    val recomposer = Recomposer(context)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = ControlledComposition(
        applier = DomApplier(NativeNodeWrapper(root)),
        parent = recomposer
    )
    val scope = object : DOMScope<TElement> {
        override val DisposableEffectScope.scopeElement: TElement
            get() = root
    }
    composition.setContent @Composable {
        content(scope)
    }
    return composition
}
