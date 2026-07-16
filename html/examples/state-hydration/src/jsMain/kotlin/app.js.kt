/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example

import kotlinx.browser.window

actual fun reload() {
    window.location.reload()
}

actual fun initialTodos(): List<Todo> {
    throw IllegalStateException("This should never be called on JS")
}