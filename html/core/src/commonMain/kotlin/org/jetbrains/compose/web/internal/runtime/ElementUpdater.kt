/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import org.jetbrains.compose.web.css.StyleHolder
import org.jetbrains.compose.web.dom.DomElement

// The expect/actual application seam for element updates (task 2.3). Replaces the former
// jsMain-only `DomElementWrapper` (org.jetbrains.compose.web.dom.Base.kt) so that
// TagElement/ComposeDomNode can move to commonMain and operate on a plain NativeNodeWrapper.
//
// JS actuals (ElementUpdater.js.kt) reproduce DomElementWrapper's logic byte-for-byte -- including
// updateAttrs's "skip style/class" rule, updateStyleDeclarations's HTMLElement/SVGElement-only
// guard, and updateClasses's classList usage. The one behavioral subtlety: DomElementWrapper used
// to diff old-vs-new event listeners via per-wrapper instance state (`currentListeners`); since
// `applyEventListeners` is stateless (just the new list), the JS actual stashes the previous list
// as a dynamic property on the node itself to preserve the exact same diffing behavior.
//
// JVM actuals (ElementUpdater.jvm.kt) write into JvmElement's plain attributes/styles/classes maps
// (SSR has no live DOM); applyProps/applyEventListeners are inert there (no live element
// properties or event dispatch to update in a server-rendered tree).

expect fun NativeNode.applyAttrs(attrs: Map<String, String>)

expect fun NativeNode.applyClasses(classes: List<String>)

expect fun NativeNode.applyStyle(style: StyleHolder)

expect fun NativeNode.applyProps(updates: List<Pair<(DomElement, Any) -> Unit, Any>>)

@ComposeWebInternalApi
expect fun NativeNode.applyEventListeners(listeners: List<NamedEventListener>)
