/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import kotlin.test.Test
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
}

private object ChildListMutationOptions : MutationObserverInit {
    override var childList: Boolean? = true
    override var subtree: Boolean? = true
}
