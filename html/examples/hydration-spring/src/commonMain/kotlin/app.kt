package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Text

/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@Composable
fun CounterApp() {
    var count by remember { mutableStateOf(0) }
    Button(attrs = {
        onClick { count++ }
    }) {
        Text("Clicked $count times!")
    }
}