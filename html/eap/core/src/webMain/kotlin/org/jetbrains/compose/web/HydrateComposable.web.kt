package org.jetbrains.compose.web

import androidx.compose.runtime.*
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.HTMLDivElement
import kotlinx.browser.dom.ParentNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher


/**
 * Hydrates one [HydrationRoot] in a server-rendered document.
 *
 * Pass the same [hydrationId] used by [HydrationRoot] to select a named root and its state element.
 * A null [hydrationId] selects the unnamed pair. The selected root and state element must each be
 * unique within [within], which defaults to the browser document. Pass a narrower [within] to
 * limit the search to one subtree.
 *
 * Run this after both elements have been parsed. Place the bootstrap script after them, defer an
 * external classic script or use a module script.
 *
 * [deserializeState] must match the serializer used on the server. The embedded state is public,
 * user-editable input. Use a safely configured deserializer, and validate and authorize any values
 * sent back to a server. Treat the deserialized state as an immutable snapshot.
 *
 * Hydration adopts the root's server-rendered children and leaves the state element in the document.
 * The state element records the server's validation mode. In fast mode, hydration retains initial
 * server values until a Compose update.
 *
 * A blank [hydrationId], a missing or duplicate pair, malformed protocol elements, or invalid
 * serialized state throw [HydrationStateException] before composition starts.
 *
 * @throws HydrationStateException if [hydrationId] is blank, the selected pair is invalid or
 * deserialization fails.
 */
actual fun <T> hydrateRoot(
    deserializeState: (String) -> T,
    monotonicFrameClock: MonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit,
    within: ParentNode,
    hydrationId: String?,
    content: @Composable ElementScope<HTMLDivElement>.(T) -> Unit,
): Composition {
    val protocol = findHydrationProtocol(within, hydrationId)
    val initialState = try {
        deserializeState(protocol.serializedState)
    } catch (failure: Throwable) {
        invalidHydrationState(
            "Failed to deserialize the Compose hydration state",
            failure,
        )
    }
    val scope = ElementScopeImpl<HTMLDivElement>().apply {
        element = protocol.root
    }

    return hydrateComposableWithMode(
        root = protocol.root,
        monotonicFrameClock = monotonicFrameClock,
        onHydrationMismatch = onHydrationMismatch,
        validationMode = protocol.validationMode,
    ) {
        content(scope, initialState)
    }
}

internal actual fun <TElement : Element> hydrateComposableWithMode(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit,
    validationMode: HtmlValidationMode,
    content: @Composable DOMScope<TElement>.() -> Unit,
): Composition = try {
    hydrateOnce(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        validationMode = validationMode,
        content = content,
    )
} catch (mismatch: HydrationMismatchException) {
    onHydrationMismatch(mismatch)
    root.clearChildren()
    renderComposable(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        content = content,
    )
}

internal actual fun <TElement : Element> hydrateOnce(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    validationMode: HtmlValidationMode,
    content: @Composable DOMScope<TElement>.() -> Unit,
): Composition {
    GlobalSnapshotManager.ensureStarted()

    val coroutineContext = monotonicFrameClock + JsMicrotasksDispatcher()
    val recomposer = Recomposer(coroutineContext)
    CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val applier = HydrationDomApplier(DomNodeWrapper(root), validationMode)
    val composition = ControlledComposition(
        applier = applier,
        parent = recomposer,
    )
    val htmlContext = hydratingComposeHtmlContext(applier)
    val scope = object : DOMScope<TElement> {
        override val DisposableEffectScope.scopeElement: TElement
            get() = root
    }

    try {
        composition.setContent {
            CompositionLocalProvider(
                LocalComposeHtmlContext provides htmlContext,
            ) {
                content(scope)
            }
        }
        // Initial DOM changes normally finish hydration from Applier.onEndChanges(), before
        // remember observers and effects run. An empty composition can produce no change batch.
        if (applier.isHydrating) {
            applier.finishHydration()
        }
        return object : Composition by composition {
            override fun dispose() {
                try {
                    composition.dispose()
                } finally {
                    recomposer.cancel()
                }
            }
        }
    } catch (throwable: Throwable) {
        throwable.suppressCleanupFailure(applier::abortHydration)
        throwable.suppressCleanupFailure(composition::dispose)
        throwable.suppressCleanupFailure(recomposer::close)
        throw throwable
    }
}

private inline fun Throwable.suppressCleanupFailure(cleanup: () -> Unit) {
    try {
        cleanup()
    } catch (cleanupFailure: Throwable) {
        addSuppressed(cleanupFailure)
    }
}


actual val browserDocument: ParentNode
    get() = org.jetbrains.compose.web.internal.runtime.browserDocument

actual fun <T> rehydrateRoot(
    html: String,
    deserializeState: (String) -> T,
    monotonicFrameClock: MonotonicFrameClock,
    onHydrationMismatch: (HydrationMismatchException) -> Unit,
    within: ParentNode,
    hydrationId: String?,
    content: @Composable (ElementScope<HTMLDivElement>.(T) -> Unit)
): Composition {
    val protocol = findHydrationProtocol(within, hydrationId)
    val initialState = try {
        deserializeState(protocol.serializedState)
    } catch (failure: Throwable) {
        invalidHydrationState(
            "Failed to deserialize the Compose hydration state",
            failure,
        )
    }
    protocol.root.replaceWith(html)
    val scope = ElementScopeImpl<HTMLDivElement>().apply {
        element = protocol.root
    }

    return hydrateComposableWithMode(
        root = protocol.root,
        monotonicFrameClock = monotonicFrameClock,
        onHydrationMismatch = onHydrationMismatch,
        validationMode = protocol.validationMode,
    ) {
        content(scope, initialState)
    }
}