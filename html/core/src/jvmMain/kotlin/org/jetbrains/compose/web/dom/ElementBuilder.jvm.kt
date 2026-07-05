package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.JvmTextNode
import org.jetbrains.compose.web.internal.runtime.NativeNode

// Must return a real DomElement (not a bare JvmElement): Base.kt's TagElement does
// `elementBuilder.create() as TElement` where TElement is some DomElement subtype (e.g.
// HTMLDivElement). That cast compiles to a checkcast against TElement's erasure, which is
// DomElement -- so the created node must actually be a DomElement instance or every element
// composable (Div, Span, P, ...) throws ClassCastException the moment a real composition tries
// to insert it (surfaced by task 3.3's renderToString, the first end-to-end JVM composition).
// The concrete tagName-specific subtype (e.g. HTMLDivElement) isn't needed here: erasure means
// the checkcast only ever verifies against the DomElement bound, never the specific subtype.
actual fun createNativeElement(tagName: String): NativeNode = object : DomElement(tagName) {}

actual fun createNativeText(text: String): NativeNode = JvmTextNode(text)
