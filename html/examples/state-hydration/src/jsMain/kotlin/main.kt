/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package com.example

import kotlinx.browser.document
import org.jetbrains.compose.web.hydrateComposable

fun main() {
    val root = document.getElementById("root")
    requireNotNull(root) { "Root element not found" }
    val state = document.getElementById("hydrated-state")
    if (state == null) {
        hydrateComposable(root = root) {
            TodoPage()
        }
    } else {
        val jsonState = state.textContent
        requireNotNull(jsonState) { "State may not be empty if present" }
        hydrateComposable(root = root, stateJson = jsonState) {
            TodoPage()
        }
    }
}