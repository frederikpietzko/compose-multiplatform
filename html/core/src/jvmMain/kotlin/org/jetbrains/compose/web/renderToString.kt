/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.DomElement
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomApplier
import org.jetbrains.compose.web.internal.runtime.JvmRootNode
import org.jetbrains.compose.web.internal.runtime.NativeNodeWrapper
import org.jetbrains.compose.web.internal.runtime.serializeToHtml

/**
 * Renders [content] to an HTML string on the JVM (server-side rendering).
 *
 * This mounts a one-shot Compose composition over a [JvmRootNode] (a transparent root that
 * [serializeToHtml] unwraps to just its children, so no synthetic wrapper tag ever leaks into
 * the output), lets the initial composition settle, serializes the resulting tree, and tears
 * the composition down again before returning.
 *
 * ### Why this doesn't call [renderComposableRoot]
 *
 * [renderComposableRoot] is the shared, platform-agnostic wiring used by e.g. jsMain's
 * `renderComposable`, and conceptually this function does the same thing: build a
 * [Recomposer] from a clock + dispatcher, [ControlledComposition] over a [DomApplier], and
 * `setContent`. However, [renderComposableRoot] only returns the [androidx.compose.runtime.Composition]
 * -- it does not expose the [Recomposer] it creates internally. For a long-lived page (the JS
 * case) that's fine: the composition (and its Recomposer) are meant to live for the lifetime of
 * the page. `renderToString` is different: it is a one-shot, likely-repeatedly-called SSR entry
 * point, and a [Recomposer] that is never explicitly shut down leaks permanently -- every
 * [Recomposer] registers itself in the static, process-wide `Recomposer.runningRecomposers` set
 * while its `runRecomposeAndApplyChanges` loop is active, and only deregisters once the loop
 * observes that it's been cancelled/closed. Without a reference to the [Recomposer], there is no
 * way to trigger that deregistration, so every call would leak a Recomposer (and transitively,
 * the composed tree) for the life of the JVM process.
 *
 * To avoid that leak, `renderToString` builds the same wiring [renderComposableRoot] would build,
 * inline, so it can keep a handle on the [Recomposer] and shut it down deterministically once
 * serialization is done.
 *
 * ### Root/scope typing
 *
 * The public signature promises `content: @Composable DOMScope<DomElement>.() -> Unit`, but the
 * actual composition root is a [JvmRootNode] (chosen so [serializeToHtml] emits only real
 * elements, with no synthetic wrapper tag -- `serializeToHtml` already treats any non-
 * [org.jetbrains.compose.web.internal.runtime.JvmElement]/
 * [org.jetbrains.compose.web.internal.runtime.JvmTextNode] node, including [JvmRootNode], as
 * transparent). [JvmRootNode] is not a [DomElement] -- and, being a final class unrelated to
 * [DomElement], can't even be force-cast to one (the compiler correctly rejects `root as
 * DomElement` as a cast that can never succeed, since it would unconditionally throw
 * [ClassCastException] the moment `scopeElement` was read). So `scopeElement` here is backed by a
 * dedicated placeholder [DomElement] instead: never inserted into the tree, never serialized.
 * This is benign in practice: every built-in composable ([org.jetbrains.compose.web.dom.Div],
 * `Span`, `P`, ...) installs its own `ElementScopeImpl` around its *own* freshly created element
 * for its children (see `ElementScope.kt`), so this placeholder is only ever observed if
 * `content` reads `scopeElement` directly at the very top level (e.g. from a top-level
 * `DisposableEffect`) -- an edge case no built-in composable relies on. In that edge case, callers
 * get an inert, disconnected `DomElement` rather than a crash. This is an SSR-only, documented
 * trade-off.
 */
@OptIn(ComposeWebInternalApi::class)
fun renderToString(content: @Composable DOMScope<DomElement>.() -> Unit): String {
    val root = JvmRootNode()
    val clock = BroadcastFrameClock()
    val job = Job()
    val scope = CoroutineScope(clock + Dispatchers.Unconfined + job)
    val recomposer = Recomposer(scope.coroutineContext)

    scope.launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = ControlledComposition(
        applier = DomApplier(NativeNodeWrapper(root)),
        parent = recomposer
    )

    // See "Root/scope typing" above: root (a JvmRootNode) can't stand in for scopeElement, so this
    // is a dedicated, disconnected placeholder -- not part of the composed tree.
    val placeholderRootElement = object : DomElement("") {}
    val domScope = object : DOMScope<DomElement> {
        override val DisposableEffectScope.scopeElement: DomElement
            get() = placeholderRootElement
    }

    val html = try {
        composition.setContent { domScope.content() }

        // Initial composition (above) is applied synchronously, so the tree is already complete at
        // this point. Sending a frame additionally flushes any recomposition/effects that scheduled
        // themselves for "the next frame" (e.g. via a LaunchedEffect) during that initial pass, so
        // the serialized output reflects a fully-settled composition. BroadcastFrameClock.sendFrame
        // is a no-op if nothing is awaiting a frame, so this is safe and non-blocking either way.
        clock.sendFrame(0L)

        serializeToHtml(root)
    } finally {
        // This must run even if setContent/sendFrame/serializeToHtml throws (e.g. the caller's
        // composable throws during initial composition): otherwise the Recomposer never
        // deregisters from the static, process-wide `Recomposer.runningRecomposers` set and leaks
        // permanently for the life of the JVM process. See the "Why this doesn't call
        // renderComposableRoot" note above. Each call is idempotent, so this is safe to run
        // unconditionally.
        composition.dispose()
        recomposer.cancel()
        job.cancel()
    }

    return html
}
