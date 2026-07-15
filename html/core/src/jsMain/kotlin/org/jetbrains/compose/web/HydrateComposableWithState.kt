/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.state.HydrationStatePayloadCodec
import org.jetbrains.compose.web.state.HydrationStateRegistry
import org.jetbrains.compose.web.state.LocalHydrationStateRegistry
import org.w3c.dom.Element

/**
 * Hydrates [root] after restoring explicitly registered state from raw [stateJson].
 * Payload transport and HTML embedding are caller responsibilities.
 */
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> hydrateComposable(
    root: TElement,
    stateJson: String,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    val registry = HydrationStateRegistry.restoring(
        HydrationStatePayloadCodec.decode(stateJson)
    )
    val composition = hydrateComposableWithPreCommit(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        beforeHydrationCommit = registry::verifyRestorationComplete,
        content = {
            val hydrationScope = this
            CompositionLocalProvider(
                LocalHydrationStateRegistry provides registry
            ) {
                content(hydrationScope)
            }
        }
    )
    registry.commitRestoration()
    return composition
}
