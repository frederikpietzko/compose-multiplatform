/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.dom.DomElement

// SSR has no live DOM: attrs/classes/styles are written into JvmElement's plain maps so they can
// later be serialized to HTML; applyProps/applyEventListeners are inert (no live element
// properties or event dispatch exist in a server-rendered tree) -- documented limitation.

actual fun NativeNode.applyAttrs(attrs: Map<String, String>) {
    val el = this as? JvmElement ?: return
    el.attributes.keys.retainAll(setOf("style", "class"))
    el.attributes.putAll(attrs)
}

actual fun NativeNode.applyClasses(classes: List<String>) {
    (this as? JvmElement)?.let { it.classes.clear(); it.classes.addAll(classes) }
}

@OptIn(ComposeWebInternalApi::class)
actual fun NativeNode.applyStyle(style: StyleHolder) {
    val el = this as? JvmElement ?: return
    el.styles.clear()
    style.properties.forEach { (name, value, _) -> el.styles[name] = value.toString() }
    style.variables.forEach { (name, value) -> el.styles[name] = value.toString() }
}

actual fun NativeNode.applyProps(updates: List<Pair<(DomElement, Any) -> Unit, Any>>) { /* inert: SSR */ }

@ComposeWebInternalApi
actual fun NativeNode.applyEventListeners(listeners: List<NamedEventListener>) { /* inert: SSR */ }
