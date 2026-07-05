/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.dom.DomElement
import org.jetbrains.compose.web.dom.DomText
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.ElementCSSInlineStyle
import org.w3c.dom.svg.SVGElement

// Bodies lifted verbatim from the former (private) DomElementWrapper in dom/Base.kt -- see that
// file's git history for the pre-task-2.3 version. `this` is statically NativeNode (= Node on
// JS), so it's cast to Element first, same as DomElementWrapper's constructor parameter used to
// statically require an Element.

actual fun NativeNode.applyAttrs(attrs: Map<String, String>) {
    val node = this as Element
    node.getAttributeNames().forEach { name ->
        when (name) {
            "style", "class" -> {
                // skip style and class here, they're managed in corresponding methods
            }
            else -> node.removeAttribute(name)
        }
    }

    attrs.forEach {
        node.setAttribute(it.key, it.value)
    }
}

actual fun NativeNode.applyClasses(classes: List<String>) {
    val node = this as Element
    node.removeAttribute("class")
    if (classes.isNotEmpty()) {
        node.classList.add(*classes.toTypedArray())
    }
}

@OptIn(ComposeWebInternalApi::class)
actual fun NativeNode.applyStyle(style: StyleHolder) {
    val node = this as Element
    when (node) {
        is HTMLElement, is SVGElement -> {
            node.removeAttribute("style")

            val inlineStyle = node.unsafeCast<ElementCSSInlineStyle>().style

            style.properties.forEach { (name, value, important) ->
                inlineStyle.setProperty(name, value.toString(), if (important) "important" else "")
            }

            style.variables.forEach { (name, value) ->
                inlineStyle.setProperty(name, value.toString())
            }
        }
    }
}

actual fun NativeNode.applyProps(updates: List<Pair<(DomElement, Any) -> Unit, Any>>) {
    val node = this as DomElement
    updates.forEach { (applicator, item) ->
        applicator(node, item)
    }
}

// DomElementWrapper used to diff old-vs-new listeners via its own per-instance `currentListeners`
// field. applyEventListeners is stateless (brief's signature only takes the new list), so the
// previous list is stashed as a dynamic property directly on the node to reproduce the exact same
// diffing behavior (remove stale listeners, then add the new ones) with no wrapper subclass.
private const val PREVIOUS_LISTENERS_KEY = "__composeWebPreviousEventListeners"

@ComposeWebInternalApi
actual fun NativeNode.applyEventListeners(listeners: List<NamedEventListener>) {
    val dynNode = this.asDynamic()
    val previousListeners = dynNode[PREVIOUS_LISTENERS_KEY].unsafeCast<List<NamedEventListener>?>() ?: emptyList()

    previousListeners.forEach {
        removeEventListener(it.name, it)
    }

    dynNode[PREVIOUS_LISTENERS_KEY] = listeners

    listeners.forEach {
        addEventListener(it.name, it)
    }
}

actual fun DomText.setTextData(value: String) {
    this.data = value
}
