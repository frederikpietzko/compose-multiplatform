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
import org.jetbrains.compose.web.core.tests.hydration.HydratableCounter
import org.jetbrains.compose.web.dom.DomText
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.state.HydrationStateException
import org.jetbrains.compose.web.state.rememberHydratableState
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class HydrationStateJsTest {
    @Test
    fun restoresBeforeMatchingThenUsesNormalRecomposition() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild as HTMLElement
        val text = button.firstChild as DomText
        var initializerCalled = false

        val composition = hydrateComposable(root, CounterState) {
            HydratableCounter(initialCount = 0) {
                initializerCalled = true
            }
        }
        try {
            assertFalse(initializerCalled)
            assertSame(button, root.firstChild)
            assertSame(text, button.firstChild)
            assertEquals("Count: 7", text.data)

            button.dispatchEvent(MouseEvent("click"))
            delay(100)

            assertSame(button, root.firstChild)
            assertSame(text, button.firstChild)
            assertEquals("Count: 8", text.data)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun missingStateDoesNotRunInitializerOrChangeRoot() {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild
        var initializerCalled = false

        val failure = assertFailsWith<HydrationStateException> {
            hydrateComposable(root, EmptyState) {
                HydratableCounter(initialCount = 0) {
                    initializerCalled = true
                }
            }
        }

        assertContains(failure.message.orEmpty(), "Missing hydration state for key \"counter\"")
        assertFalse(initializerCalled)
        assertSame(button, root.firstChild)
        assertEquals("<button>Count: 7</button>", root.innerHTML)
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun unconsumedStateAbortsListenersPreservesRootAndAllowsRetry() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild as HTMLElement
        val text = button.firstChild as DomText
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        val failure = assertFailsWith<HydrationStateException> {
            hydrateComposable(root, StateWithExtraKey) {
                HydratableCounter(initialCount = 0)
            }
        }

        assertContains(failure.message.orEmpty(), "Unconsumed hydration state keys: unused")
        assertSame(button, root.firstChild)
        assertSame(text, button.firstChild)
        assertEquals("<button>Count: 7</button>", root.innerHTML)

        button.dispatchEvent(MouseEvent("click"))
        delay(1)
        assertEquals("Count: 7", text.data)
        assertEquals(recomposersBefore, Recomposer.runningRecomposers.value.size)

        val retry = hydrateComposable(root, CounterState) {
            HydratableCounter(initialCount = 0)
        }
        retry.dispose()
    }

    @Test
    fun malformedPayloadFailsBeforeHydrationStarts() {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild

        assertFailsWith<HydrationStateException> {
            hydrateComposable(root, "not-json") {
                HydratableCounter(initialCount = 0)
            }
        }

        assertSame(button, root.firstChild)
        assertEquals("<button>Count: 7</button>", root.innerHTML)
    }

    @Test
    fun rejectsDuplicateClientKeyAndWrongSerializedType() {
        val duplicateRoot = document.createElement("div")
        val duplicate = assertFailsWith<HydrationStateException> {
            hydrateComposable(
                duplicateRoot,
                """{"version":1,"values":{"same":1}}"""
            ) {
                rememberHydratableState("same") { 0 }
                rememberHydratableState("same") { 0 }
            }
        }
        assertContains(duplicate.message.orEmpty(), "Duplicate hydration state key \"same\"")

        val wrongTypeRoot = document.createElement("div")
        wrongTypeRoot.innerHTML = "<button>Count: 7</button>"
        val wrongType = assertFailsWith<HydrationStateException> {
            hydrateComposable(
                wrongTypeRoot,
                """{"version":1,"values":{"counter":"seven"}}"""
            ) {
                HydratableCounter(initialCount = 0)
            }
        }
        assertContains(wrongType.message.orEmpty(), "Failed to restore hydration state \"counter\"")
    }
}

private const val CounterState =
    """{"version":1,"values":{"counter":7}}"""
private const val EmptyState =
    """{"version":1,"values":{}}"""
private const val StateWithExtraKey =
    """{"version":1,"values":{"counter":7,"unused":true}}"""
