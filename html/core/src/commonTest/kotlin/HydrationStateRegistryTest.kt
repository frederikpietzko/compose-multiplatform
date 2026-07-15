/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class HydrationStateRegistryTest {
    @Test
    fun savingRegistrySnapshotsCurrentValuesAndReleasesForgottenEntries() {
        val registry = HydrationStateRegistry.saving()
        val entry = registry.createEntry("counter", Int.serializer()) { 1 }
        entry.onRemembered()
        entry.state.value = 7

        assertEquals(
            mapOf("counter" to JsonPrimitive(7)),
            registry.snapshotValues()
        )

        entry.onForgotten()
        assertEquals(emptyMap(), registry.snapshotValues())
    }

    @Test
    fun rejectsBlankAndDuplicateActiveKeys() {
        val registry = HydrationStateRegistry.saving()
        assertContains(
            assertFailsWith<HydrationStateException> {
                registry.createEntry("  ", Int.serializer()) { 1 }
            }.message.orEmpty(),
            "must not be blank"
        )

        val first = registry.createEntry("same", Int.serializer()) { 1 }
        val second = registry.createEntry("same", Int.serializer()) { 2 }
        first.onRemembered()
        assertContains(
            assertFailsWith<HydrationStateException> {
                second.onRemembered()
            }.message.orEmpty(),
            "Duplicate hydration state key \"same\""
        )
    }

    @Test
    fun restoresValueWithoutCallingInitializer() {
        val registry = HydrationStateRegistry.restoring(
            mapOf("counter" to JsonPrimitive(7))
        )
        var initializerCalled = false
        val entry = registry.createEntry("counter", Int.serializer()) {
            initializerCalled = true
            0
        }
        entry.onRemembered()

        assertEquals(7, entry.state.value)
        assertFalse(initializerCalled)
        registry.verifyRestorationComplete()
        registry.commitRestoration()
    }

    @Test
    fun distinguishesJsonNullFromMissingKey() {
        val registry = HydrationStateRegistry.restoring(
            mapOf("label" to JsonNull)
        )
        val entry = registry.createEntry("label", String.serializer().nullable) {
            "fallback"
        }
        entry.onRemembered()

        assertEquals(null, entry.state.value)
        registry.verifyRestorationComplete()
    }

    @Test
    fun rejectsMissingAndUnconsumedRestoredKeys() {
        var initializerCalled = false
        val missing = HydrationStateRegistry.restoring(emptyMap())
        assertContains(
            assertFailsWith<HydrationStateException> {
                missing.createEntry("counter", Int.serializer()) {
                    initializerCalled = true
                    0
                }
            }.message.orEmpty(),
            "Missing hydration state for key \"counter\""
        )
        assertFalse(initializerCalled)

        val extra = HydrationStateRegistry.restoring(
            mapOf("unused" to JsonPrimitive(true))
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                extra.verifyRestorationComplete()
            }.message.orEmpty(),
            "Unconsumed hydration state keys: unused"
        )
    }

    @Test
    fun initializesNewEntriesAfterRestorationCommits() {
        val registry = HydrationStateRegistry.restoring(
            mapOf("counter" to JsonPrimitive(7))
        )
        val restored = registry.createEntry("counter", Int.serializer()) { 0 }
        restored.onRemembered()
        registry.verifyRestorationComplete()
        registry.commitRestoration()
        restored.onForgotten()

        val later = registry.createEntry("later", Int.serializer()) { 9 }
        later.onRemembered()
        assertEquals(9, later.state.value)
    }

    @Test
    fun wrapsSerializerFailuresWithKeyAndOperation() {
        val saving = HydrationStateRegistry.saving()
        val savingEntry = saving.createEntry("broken", ThrowingIntSerializer) { 1 }
        savingEntry.onRemembered()
        assertContains(
            assertFailsWith<HydrationStateException> {
                saving.snapshotValues()
            }.message.orEmpty(),
            "Failed to serialize hydration state \"broken\""
        )

        val restoring = HydrationStateRegistry.restoring(
            mapOf("broken" to JsonPrimitive(1))
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                restoring.createEntry("broken", ThrowingIntSerializer) { 0 }
            }.message.orEmpty(),
            "Failed to restore hydration state \"broken\""
        )
    }
}

private object ThrowingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ThrowingInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        error("encode failed")
    }

    override fun deserialize(decoder: Decoder): Int {
        error("decode failed")
    }
}
