/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.Composition
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.renderComposable
import org.jetbrains.compose.web.core.tests.hydration.HydrationCounter
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.DomElement
import org.jetbrains.compose.web.dom.DomText
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class HydrationJsTest {
    @Test
    fun reusesExistingElementAndTextNodes() {
        val root = document.createElement("div")
        root.innerHTML = "<div><span>Hello</span></div>"
        val div = root.firstChild
        val span = div!!.firstChild
        val text = span!!.firstChild

        val composition = hydrateComposable(root) {
            Div { Span { Text("Hello") } }
        }
        try {
            assertSame(div, root.firstChild)
            assertSame(span, root.firstChild!!.firstChild)
            assertSame(text, root.firstChild!!.firstChild!!.firstChild)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun doesNotInsertNodesWhileHydrating() {
        val root = document.createElement("div")
        root.innerHTML = "<div><span>Hello</span></div>"
        val observer = MutationObserver { _, _ -> }
        observer.observe(root, ChildListMutationOptions)

        val composition = hydrateComposable(root) {
            Div { Span { Text("Hello") } }
        }
        try {
            assertEquals(0, observer.takeRecords().size)
        } finally {
            observer.disconnect()
            composition.dispose()
        }
    }

    @Test
    fun supportsRootIdOverload() {
        val root = document.createElement("div") as HTMLElement
        root.id = "hydrate-root"
        root.innerHTML = "<div>Hello</div>"
        document.body!!.appendChild(root)
        try {
            val child = root.firstChild
            val composition = hydrateComposable("hydrate-root") { Div { Text("Hello") } }
            try {
                assertSame(child, root.firstChild)
            } finally {
                composition.dispose()
            }
        } finally {
            root.parentNode?.removeChild(root)
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun failsOnDifferentTextWithoutClearingRootOrLeakingRecomposer() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<span>Server</span>"
        val child = root.firstChild
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) { Span { Text("Client") } }
        }

        assertSame(child, root.firstChild)
        assertEquals("<span>Server</span>", root.innerHTML)
        delay(1)
        assertEquals(recomposersBefore, Recomposer.runningRecomposers.value.size)

        val normalRoot = document.createElement("div")
        val normalComposition = renderComposable(normalRoot) { Div { Text("Normal") } }
        assertEquals("<div>Normal</div>", normalRoot.innerHTML)
        normalComposition.dispose()
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun finishFailurePreservesRootAndDoesNotLeakRecomposer() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<span>Server</span><em>Extra</em>"
        val span = root.firstChild
        val extra = root.lastChild
        val html = root.innerHTML
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) { Span { Text("Server") } }
        }

        assertSame(span, root.firstChild)
        assertSame(extra, root.lastChild)
        assertEquals(html, root.innerHTML)
        delay(1)
        assertEquals(recomposersBefore, Recomposer.runningRecomposers.value.size)
    }

    @Test
    fun finishFailureDeactivatesHydrationBeforeRefDisposal() {
        val root = document.createElement("div")
        root.innerHTML = "<span>Server</span><em>Extra</em>"
        val span = root.firstChild
        val extra = root.lastChild
        val html = root.innerHTML
        val normalRoot = document.createElement("div")
        var normalComposition: Composition? = null

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Span(attrs = {
                    ref {
                        onDispose {
                            normalComposition = renderComposable(normalRoot) {
                                Div { Text("Normal") }
                            }
                        }
                    }
                }) { Text("Server") }
            }
        }

        assertEquals(
            "Hydration mismatch at root: expected end of node, found extra <em>",
            failure.message
        )
        assertSame(span, root.firstChild)
        assertSame(extra, root.lastChild)
        assertEquals(html, root.innerHTML)
        assertEquals("<div>Normal</div>", normalRoot.innerHTML)
        normalComposition!!.dispose()
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun throwingRefDisposalDoesNotReplaceFinishFailureOrLeakRecomposer() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<span>Server</span><em>Extra</em>"
        val span = root.firstChild
        val extra = root.lastChild
        val html = root.innerHTML
        val cleanupFailure = IllegalStateException("cleanup failed")
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        val failure = assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Span(attrs = {
                    ref {
                        onDispose { throw cleanupFailure }
                    }
                }) { Text("Server") }
            }
        }

        assertEquals(
            "Hydration mismatch at root: expected end of node, found extra <em>",
            failure.message
        )
        assertSame(cleanupFailure, failure.suppressedExceptions.single())
        assertSame(span, root.firstChild)
        assertSame(extra, root.lastChild)
        assertEquals(html, root.innerHTML)
        delay(1)
        assertEquals(recomposersBefore, Recomposer.runningRecomposers.value.size)
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun userComposableFailurePreservesRootAndAllowsNormalRendering() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<span>Server</span>"
        val span = root.firstChild
        val html = root.innerHTML
        val contentFailure = IllegalStateException("content failed")
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        val failure = assertFailsWith<IllegalStateException> {
            hydrateComposable(root) {
                Span { Text("Server") }
                throw contentFailure
            }
        }

        assertSame(contentFailure, failure)
        assertSame(span, root.firstChild)
        assertEquals(html, root.innerHTML)
        delay(1)
        assertEquals(recomposersBefore, Recomposer.runningRecomposers.value.size)

        val normalRoot = document.createElement("div")
        val normalComposition = renderComposable(normalRoot) { Div { Text("Normal") } }
        assertEquals("<div>Normal</div>", normalRoot.innerHTML)
        normalComposition.dispose()
    }

    @Test
    fun hydratesEventThenUsesNormalRecomposition() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<div class=\"idle\" style=\"color:red;\"><button>Count: 0</button></div>"
        val div = root.firstChild as HTMLElement
        val button = div.firstChild as HTMLElement
        val text = button.firstChild as DomText

        val composition = hydrateComposable(root) { HydrationCounter() }
        try {
            button.dispatchEvent(MouseEvent("click"))
            delay(100)

            assertSame(div, root.firstChild)
            assertSame(button, div.firstChild)
            assertSame(text, button.firstChild)
            assertEquals("Count: 1", text.data)
            assertEquals("active", div.className)
            assertEquals("green", div.style.color)
            assertEquals("<span class=\"result\">Updated</span>", (div.lastChild as Element).outerHTML)

            button.dispatchEvent(MouseEvent("click"))
            delay(100)

            assertSame(button, div.firstChild)
            assertSame(text, button.firstChild)
            assertEquals("Count: 2", text.data)
            assertEquals(1, div.childNodes.length)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun removesListenersWhenLaterMismatchAborts() {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 0</button><span>Server</span>"
        var clicks = 0
        val button = root.firstChild as HTMLElement

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) {
                Button({ onClick { clicks++ } }) { Text("Count: 0") }
                Span { Text("Client") }
            }
        }

        button.dispatchEvent(MouseEvent("click"))
        assertEquals(0, clicks)
        assertSame(button, root.firstChild)
    }

    @Test
    fun reportsMissingAndExtraChildren() {
        val missing = document.createElement("div")
        assertContains(
            assertFailsWith<HydrationMismatchException> {
                hydrateComposable(missing) { Span {} }
            }.message.orEmpty(),
            "found no node"
        )

        val extra = document.createElement("div")
        extra.innerHTML = "<span></span><p></p>"
        assertContains(
            assertFailsWith<HydrationMismatchException> {
                hydrateComposable(extra) { Span {} }
            }.message.orEmpty(),
            "found extra <p>"
        )
    }

    @Test
    fun rejectsSvgNamespaceAndClearsSessionAfterFailure() {
        val svgRoot = document.createElement("div")
        svgRoot.innerHTML = "<svg></svg>"
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(svgRoot) { TagElement<DomElement>("svg", null, null) }
        }

        val validRoot = document.createElement("div")
        validRoot.innerHTML = "<span>ok</span>"
        val composition = hydrateComposable(validRoot) { Span { Text("ok") } }
        try {
            assertEquals("<span>ok</span>", validRoot.innerHTML)
        } finally {
            composition.dispose()
        }
    }
}

private object ChildListMutationOptions : MutationObserverInit {
    override var childList: Boolean? = true
    override var subtree: Boolean? = true
}
