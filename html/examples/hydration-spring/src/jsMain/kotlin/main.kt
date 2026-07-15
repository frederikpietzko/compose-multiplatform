import com.example.CounterApp
import kotlinx.browser.document
import org.jetbrains.compose.web.hydrateComposable

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

fun main() {
    val root = document.getElementById("root")
    requireNotNull(root) { "Root element not found" }
    hydrateComposable(root) {
        CounterApp()
    }
}