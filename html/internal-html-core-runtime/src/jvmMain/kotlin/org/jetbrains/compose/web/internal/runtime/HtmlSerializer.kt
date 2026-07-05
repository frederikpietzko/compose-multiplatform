/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

/**
 * Serializes a [NativeNode] tree (as produced on the JVM target, see `NativeNode.jvm.kt`)
 * into an HTML string.
 *
 * This is a direct, hand-written serializer rather than a wrapper around a third-party HTML
 * builder library: walking the [NativeNode] tree ourselves is simple, fully under our control,
 * and easy to keep correct -- including HTML quirks like `<style>`/`<script>` being "raw text"
 * elements whose content must never be entity-escaped.
 */
@ComposeWebInternalApi
fun serializeToHtml(root: NativeNode): String {
    val sb = StringBuilder()
    root.children.forEach { appendNode(it, sb, rawText = false) }
    return sb.toString()
}

private fun appendNode(node: NativeNode, sb: StringBuilder, rawText: Boolean) {
    when (node) {
        is JvmTextNode -> sb.append(if (rawText) node.text else escapeText(node.text))
        is JvmElement -> appendElement(node, sb)
        else -> node.children.forEach { appendNode(it, sb, rawText) }
    }
}

private fun appendElement(element: JvmElement, sb: StringBuilder) {
    val tagName = element.tagName

    sb.append('<').append(tagName)

    if (element.classes.isNotEmpty()) {
        appendAttribute(sb, "class", element.classes.joinToString(" "))
    }
    if (element.styles.isNotEmpty()) {
        val styleValue = element.styles.entries.joinToString("") { (name, value) -> "$name:$value;" }
        appendAttribute(sb, "style", styleValue)
    }
    for ((name, value) in element.attributes) {
        appendAttribute(sb, name, value)
    }

    if (tagName in VOID_TAGS) {
        sb.append('>')
        return
    }

    sb.append('>')
    // `<style>` and `<script>` are HTML "raw text" elements: browsers do not entity-decode their
    // content, so text children must be emitted verbatim rather than HTML-escaped (escaping would
    // corrupt e.g. CSS child combinators like `div > p` into `div &gt; p`).
    val rawText = tagName.lowercase() in RAW_TEXT_TAGS
    element.children.forEach { appendNode(it, sb, rawText) }
    sb.append("</").append(tagName).append('>')
}

private fun appendAttribute(sb: StringBuilder, name: String, value: String) {
    sb.append(' ').append(name).append("=\"").append(escapeAttributeValue(value)).append('"')
}

private fun escapeText(text: String): String = buildString(text.length) {
    for (c in text) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            else -> append(c)
        }
    }
}

private fun escapeAttributeValue(value: String): String = buildString(value.length) {
    for (c in value) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(c)
        }
    }
}

private val VOID_TAGS = setOf(
    "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"
)

private val RAW_TEXT_TAGS = setOf("style", "script")
