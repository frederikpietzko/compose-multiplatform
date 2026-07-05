/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.Event/EventTarget -> the
// Native*/NativeEventTarget expect seam declared in NativeEvent.kt. `target` is obtained via an
// unchecked cast (same as the original `unsafeCast<Element>()`, just spelled with a regular `as`
// -- an unchecked cast on an erased generic type parameter behaves the same way on every target).
open class SyntheticEvent<Element : NativeEventTarget> internal constructor(
    val nativeEvent: NativeEvent
) {
    @Suppress("UNCHECKED_CAST")
    val target: Element = nativeEvent.target as Element
    val bubbles: Boolean = nativeEvent.bubbles
    val cancelable: Boolean = nativeEvent.cancelable
    val composed: Boolean = nativeEvent.composed
    val currentTarget: NativeEventTarget? = nativeEvent.currentTarget
    val eventPhase: Short = nativeEvent.eventPhase
    val defaultPrevented: Boolean = nativeEvent.defaultPrevented
    val timestamp: Number = nativeEvent.timeStamp
    val type: String = nativeEvent.type
    val isTrusted: Boolean = nativeEvent.isTrusted

    fun preventDefault(): Unit = nativeEvent.preventDefault()
    fun stopPropagation(): Unit = nativeEvent.stopPropagation()
    fun stopImmediatePropagation(): Unit = nativeEvent.stopImmediatePropagation()
    fun composedPath(): Array<NativeEventTarget> = nativeEvent.composedPath()
}
