/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class HydrationStateRegistry private constructor(
    private var phase: Phase,
    restoredValues: Map<String, JsonElement>
) {
    private enum class Phase {
        Saving,
        Restoring,
        Live
    }

    private val restored = restoredValues.toMutableMap()
    private val claimedRestoredKeys = mutableSetOf<String>()
    private val activeEntries = mutableMapOf<String, HydrationStateEntry<*>>()

    companion object {
        fun saving(): HydrationStateRegistry =
            HydrationStateRegistry(Phase.Saving, emptyMap())

        fun restoring(values: Map<String, JsonElement>): HydrationStateRegistry =
            HydrationStateRegistry(Phase.Restoring, values)
    }

    fun <T> createEntry(
        key: String,
        serializer: KSerializer<T>,
        initial: () -> T
    ): HydrationStateEntry<T> {
        if (key.isBlank()) {
            throw HydrationStateException("Hydration state key must not be blank")
        }

        var restoredElement: JsonElement? = null
        val value = if (phase == Phase.Restoring) {
            if (!claimedRestoredKeys.add(key)) {
                throw duplicateKey(key)
            }
            if (!restored.containsKey(key)) {
                claimedRestoredKeys.remove(key)
                throw HydrationStateException(
                    "Missing hydration state for key \"$key\""
                )
            }
            restored.remove(key)!!.also { restoredElement = it }.let { element ->
                try {
                    Json.decodeFromJsonElement(serializer, element)
                } catch (failure: Throwable) {
                    restored[key] = element
                    claimedRestoredKeys.remove(key)
                    throw HydrationStateException(
                        "Failed to restore hydration state \"$key\"",
                        failure
                    )
                }
            }
        } else {
            initial()
        }

        return HydrationStateEntry(
            key = key,
            serializer = serializer,
            state = mutableStateOf(value),
            restoredElement = restoredElement,
            registry = this
        )
    }

    fun snapshotValues(): Map<String, JsonElement> {
        check(phase == Phase.Saving) {
            "Hydration state can only be snapshotted by a saving registry"
        }
        return activeEntries.mapValues { (_, entry) -> entry.encode() }
    }

    fun verifyRestorationComplete() {
        check(phase == Phase.Restoring) {
            "Hydration state restoration is not active"
        }
        if (restored.isNotEmpty()) {
            throw HydrationStateException(
                "Unconsumed hydration state keys: ${restored.keys.sorted().joinToString()}"
            )
        }
    }

    fun commitRestoration() {
        check(phase == Phase.Restoring) {
            "Hydration state restoration is not active"
        }
        phase = Phase.Live
        claimedRestoredKeys.clear()
    }

    fun activate(entry: HydrationStateEntry<*>) {
        val existing = activeEntries[entry.key]
        if (existing != null && existing !== entry) {
            throw duplicateKey(entry.key)
        }
        activeEntries[entry.key] = entry
    }

    fun release(entry: HydrationStateEntry<*>) {
        if (activeEntries[entry.key] === entry) {
            activeEntries.remove(entry.key)
        }
        val restoredElement = entry.restoredElement
        if (
            phase == Phase.Restoring &&
            restoredElement != null &&
            claimedRestoredKeys.remove(entry.key)
        ) {
            restored[entry.key] = restoredElement
        }
    }

    private fun duplicateKey(key: String) = HydrationStateException(
        "Duplicate hydration state key \"$key\""
    )
}

internal class HydrationStateEntry<T>(
    val key: String,
    private val serializer: KSerializer<T>,
    val state: MutableState<T>,
    val restoredElement: JsonElement?,
    private val registry: HydrationStateRegistry
) : RememberObserver {
    override fun onRemembered() {
        registry.activate(this)
    }

    override fun onForgotten() {
        registry.release(this)
    }

    override fun onAbandoned() {
        registry.release(this)
    }

    fun encode(): JsonElement = try {
        Json.encodeToJsonElement(serializer, state.value)
    } catch (failure: Throwable) {
        throw HydrationStateException(
            "Failed to serialize hydration state \"$key\"",
            failure
        )
    }
}
