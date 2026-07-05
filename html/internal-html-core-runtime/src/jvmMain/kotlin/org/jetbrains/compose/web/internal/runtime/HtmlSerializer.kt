package org.jetbrains.compose.web.internal.runtime

/**
 * Serializes a [NativeNode] tree (as produced on the JVM target, see `NativeNode.jvm.kt`)
 * into an HTML string.
 *
 * This is a direct, hand-written serializer rather than a wrapper around kotlinx.html's
 * low-level [kotlinx.html.TagConsumer]/[kotlinx.html.Tag] API: the exact `Tag` constructor
 * signature is an implementation detail that varies across kotlinx.html versions, whereas
 * this walk over the [NativeNode] tree is simple, fully under our control, and easy to keep
 * correct.
 *
 * @param prettyPrint reserved for future formatting support (indentation/newlines between
 * tags); currently the output is always compact.
 *
 * Note: this is not annotated with [ComposeWebInternalApi] (unlike other internal-runtime
 * APIs in this package) so that call sites — including tests — don't need an explicit
 * `@OptIn`. It lives in the `internal.runtime` package, which itself signals API stability
 * expectations.
 */
fun serializeToHtml(root: NativeNode, prettyPrint: Boolean = false): String {
    val sb = StringBuilder()
    root.children.forEach { appendNode(it, sb) }
    return sb.toString()
}

private fun appendNode(node: NativeNode, sb: StringBuilder) {
    when (node) {
        is JvmTextNode -> sb.append(escapeText(node.text))
        is JvmElement -> appendElement(node, sb)
        else -> node.children.forEach { appendNode(it, sb) }
    }
}

private fun appendElement(element: JvmElement, sb: StringBuilder) {
    val tagName = element.tagName
    if (tagName.isEmpty()) {
        // Transparent node: emit only children, no wrapper tag.
        element.children.forEach { appendNode(it, sb) }
        return
    }

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
    element.children.forEach { appendNode(it, sb) }
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
