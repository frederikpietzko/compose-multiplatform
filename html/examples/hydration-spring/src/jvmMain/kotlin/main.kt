/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.DomElement
import org.jetbrains.compose.web.renderToString
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@SpringBootApplication
class Application

fun main(){
    runApplication<Application>()
}

@Controller
@RequestMapping("/")
@ResponseBody
class CounterController {
    @GetMapping
    fun index() = wrapped {
        CounterApp()
    }
}

fun wrapped(content: @Composable (DOMScope<DomElement>) -> Unit) = buildString {
    append("<!DOCTYPE html>")
    append("<html>")
    append("<head>")
    append("<title>Counter</title>")
    append("""<script defer src="http://localhost:8081/hydration-spring.js"></script>""".trimMargin())
    append("</head>")
    append("<body>")
    append("""<div id="root">""")
    append(renderToString(content))
    append("</div>")
    append("</body>")
    append("</html>")
}