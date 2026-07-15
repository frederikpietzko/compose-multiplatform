/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

internal val LocalHydrationStateRegistry =
    staticCompositionLocalOf<HydrationStateRegistry?> { null }

/**
 * Remembers mutable state that participates in JVM SSR-to-browser hydration when a
 * state-aware renderer or hydrator supplies a registry.
 */
@Composable
inline fun <reified T> rememberHydratableState(
    key: String,
    noinline initial: () -> T
): MutableState<T> = rememberHydratableState(
    key = key,
    serializer = serializer<T>(),
    initial = initial
)

/**
 * Remembers mutable state using [serializer] for JVM SSR-to-browser hydration.
 * [key] must be unique across the active rendered root.
 */
@Composable
fun <T> rememberHydratableState(
    key: String,
    serializer: KSerializer<T>,
    initial: () -> T
): MutableState<T> {
    val registry = LocalHydrationStateRegistry.current
    if (registry == null) {
        return remember { mutableStateOf(initial()) }
    }

    val entry = remember(registry, key) {
        registry.createEntry(key, serializer, initial)
    }
    return entry.state
}
