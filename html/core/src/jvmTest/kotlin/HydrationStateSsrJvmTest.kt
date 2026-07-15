/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.compose.web.core.tests.hydration.HydratableCounter
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.renderToStringWithState
import org.jetbrains.compose.web.state.HydrationStateException
import org.jetbrains.compose.web.state.rememberHydratableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class HydrationStateSsrJvmTest {
    @Test
    fun returnsMatchingHtmlAndStateJson() {
        val result = renderToStringWithState {
            HydratableCounter(initialCount = 7)
        }

        assertEquals("<button>Count: 7</button>", result.html)
        assertEquals(
            """{"version":1,"values":{"counter":7}}""",
            result.stateJson
        )
    }

    @Test
    fun capturesFinalStateAndActiveProvidersAfterSettling() {
        val result = renderToStringWithState {
            var count by rememberHydratableState("counter") { 0 }
            SideEffect {
                if (count == 0) count = 1
            }
            Div { Text("Count: $count") }
        }

        assertEquals("<div>Count: 1</div>", result.html)
        assertEquals(
            """{"version":1,"values":{"counter":1}}""",
            result.stateJson
        )
    }

    @Test
    fun ordinaryRendererUsesRegistryFreeFallback() {
        var initializerCalls = 0
        val html = renderToString {
            HydratableCounter(initialCount = 3) {
                initializerCalls++
            }
        }

        assertEquals("<button>Count: 3</button>", html)
        assertEquals(1, initializerCalls)
    }

    @Test
    fun supportsInferredNullableAndCollectionSerializers() {
        val result = renderToStringWithState {
            val items by rememberHydratableState("items") { listOf(2, 1) }
            val label by rememberHydratableState<String?>("label") { null }
            Text("${items.joinToString()}:${label ?: "none"}")
        }

        assertEquals("2, 1:none", result.html)
        assertEquals(
            """{"version":1,"values":{"items":[2,1],"label":null}}""",
            result.stateJson
        )
    }

    @Test
    fun supportsExplicitSerializerOverload() {
        val result = renderToStringWithState {
            val count by rememberHydratableState(
                key = "counter",
                serializer = Int.serializer()
            ) { 4 }
            Text("$count")
        }

        assertEquals("4", result.html)
        assertEquals(
            """{"version":1,"values":{"counter":4}}""",
            result.stateJson
        )
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun encodeFailureSurvivesThrowingDisposalAndCleansUpRecomposer() {
        val recomposersBefore = Recomposer.runningRecomposers.value.size
        val disposalFailure = IllegalStateException("dispose failed")

        val failure = assertFailsWith<HydrationStateException> {
            renderToStringWithState {
                rememberHydratableState(
                    key = "broken",
                    serializer = FailingEncodeSerializer
                ) { 1 }
                DisposableEffect(Unit) {
                    onDispose { throw disposalFailure }
                }
                Text("content")
            }
        }

        assertEquals("Failed to serialize hydration state \"broken\"", failure.message)
        assertSame(disposalFailure, failure.suppressedExceptions.single())
        assertEquals(
            recomposersBefore,
            Recomposer.runningRecomposers.value.size
        )
    }
}

private object FailingEncodeSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FailingEncode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        error("encode failed")
    }

    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}
