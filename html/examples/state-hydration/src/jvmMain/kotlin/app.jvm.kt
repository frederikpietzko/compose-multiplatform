/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example

actual fun reload() {
    // Will never do anything, makes no sense on JVM
}

actual fun initialTodos(): List<Todo> {
    val todoService = ApplicationContextHolder.applicationContext.getBean(TodoService::class.java)
    return todoService.getTodos()
}