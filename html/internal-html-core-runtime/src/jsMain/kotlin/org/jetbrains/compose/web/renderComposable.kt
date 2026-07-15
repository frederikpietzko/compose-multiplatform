/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.HydrationDomApplierStrategy
import org.jetbrains.compose.web.internal.runtime.HydrationSession
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher
import org.jetbrains.compose.web.internal.runtime.deactivateHydrationSession
import org.jetbrains.compose.web.internal.runtime.withHydrationSession
import org.w3c.dom.Element
import org.w3c.dom.HTMLBodyElement
import org.w3c.dom.get

/**
 * Use this method to mount the composition at the certain [root]
 *
 * @param root - the [Element] that will be the root of the DOM tree managed by Compose
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> renderComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()

    return renderComposableRoot(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        dispatcher = JsMicrotasksDispatcher(),
        content = content
    )
}

/**
 * Use this method to mount the composition at the element with id - [rootElementId].
 *
 * @param rootElementId - the id of the [Element] that will be the root of the DOM tree managed
 * by Compose
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
@Suppress("UNCHECKED_CAST")
fun renderComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition = renderComposable(
    root = document.getElementById(rootElementId)!!,
    content = content
)

/**
 * Hydrates server-rendered DOM under [root] without replacing matched nodes.
 *
 * Server and client content must deterministically produce identical element and text structure.
 * Matching is positional and marker-free. DOM shapes changed by HTML parser normalization are not
 * supported by this proof-of-concept and produce [HydrationMismatchException]. Caller owns returned
 * [Composition] and must dispose it when no longer needed.
 *
 * @param root the server-rendered [Element] whose children are hydrated
 * @param monotonicFrameClock the clock driving recomposition frame timing
 * @param content Composable content matching server-rendered DOM exactly
 * @return live [Composition] owned by caller
 * @throws HydrationMismatchException when existing DOM does not match [content]
 */
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition = hydrateComposableWithPreCommit(
    root = root,
    monotonicFrameClock = monotonicFrameClock,
    beforeHydrationCommit = {},
    content = content
)

/**
 * Hydrates server-rendered DOM, invoking [beforeHydrationCommit] after initial composition is
 * applied but before DOM completeness is checked and hydration is committed. Exceptions from the
 * callback use normal hydration abort cleanup.
 */
@ComposeWebInternalApi
fun <TElement : Element> hydrateComposableWithPreCommit(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    beforeHydrationCommit: () -> Unit,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()
    val session = HydrationSession(root)
    val strategy = HydrationDomApplierStrategy(session)

    return withHydrationSession(session) {
        renderComposableRoot(
            root = root,
            monotonicFrameClock = monotonicFrameClock,
            dispatcher = JsMicrotasksDispatcher(),
            strategy = strategy,
            onInitialCompositionApplied = {
                beforeHydrationCommit()
                strategy.finish()
            },
            onCompositionFailed = {
                try {
                    session.abort()
                } finally {
                    deactivateHydrationSession(session)
                }
            },
            content = content
        )
    }
}

/**
 * Hydrates server-rendered DOM under element identified by [rootElementId].
 *
 * Matching is deterministic, positional, and marker-free; see [hydrateComposable] for constraints.
 * Caller owns returned [Composition] and must dispose it when no longer needed.
 *
 * @throws HydrationMismatchException when existing DOM does not match [content]
 */
fun hydrateComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition = hydrateComposable(
    root = document.getElementById(rootElementId)!!,
    content = content
)

/**
 * Use this method to mount the composition at the [HTMLBodyElement] of the current document
 *
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
fun renderComposableInBody(
    content: @Composable DOMScope<HTMLBodyElement>.() -> Unit
): Composition = renderComposable(
    root = document.getElementsByTagName("body")[0] as HTMLBodyElement,
    content = content
)
