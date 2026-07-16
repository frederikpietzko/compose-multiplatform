/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.example

import org.jetbrains.compose.web.SsrResult
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ResponseBody

@Component
class SsrResultMessageConverter : HttpMessageConverter<SsrResult> {
    override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean {
        return false
    }

    override fun canWrite(clazz: Class<*>, mediaType: MediaType?): Boolean {
        return clazz == SsrResult::class.java
    }

    override fun getSupportedMediaTypes(): List<MediaType> {
        return listOf(MediaType.TEXT_HTML)
    }

    override fun read(
        clazz: Class<out SsrResult>,
        inputMessage: HttpInputMessage
    ): SsrResult {
        error("Cannot read SSR result")
    }

    override fun write(
        t: SsrResult,
        contentType: MediaType?,
        outputMessage: HttpOutputMessage
    ) {
        outputMessage.headers.contentType = MediaType.TEXT_HTML
        outputMessage.body.write(t.toHtmlString().toByteArray())
    }

    private fun SsrResult.toHtmlString() = buildString {
        append("<!DOCTYPE html>")
        append("<html>")
        append("<head>")
        append("<title>Counter</title>")
        append("""<script defer src="http://localhost:8081/state-hydration.js"></script>""".trimMargin())
        append("""<script id="hydrated-state" type="application/json">""")
        append(stateJson)
        append("</script>")
        append("</head>")
        append("<body>")
        append("""<div id="root">""")
        append(html)
        append("</div>")
        append("</body>")
        append("</html>")
    }
}

@Controller
@ResponseBody
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ComposeController
