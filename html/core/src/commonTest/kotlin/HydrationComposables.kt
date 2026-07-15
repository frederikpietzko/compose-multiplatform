/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests.hydration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.state.rememberHydratableState

@Composable
fun HydrationCounter() {
    var count by remember { mutableStateOf(0) }
    Div({
        classes(if (count == 0) "idle" else "active")
        style { property("color", if (count == 0) "red" else "green") }
    }) {
        Button({ onClick { count++ } }) {
            Text("Count: $count")
        }
        if (count % 2 == 1) {
            Span({ classes("result") }) { Text("Updated") }
        }
    }
}

@Composable
fun HydratableCounter(
    initialCount: Int,
    onInitialize: () -> Unit = {}
) {
    var count by rememberHydratableState("counter") {
        onInitialize()
        initialCount
    }
    Button(attrs = { onClick { count++ } }) {
        Text("Count: $count")
    }
}
