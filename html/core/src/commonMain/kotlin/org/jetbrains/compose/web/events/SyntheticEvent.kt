/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.Event/EventTarget -> the
// Native*/NativeEventTarget expect seam declared in NativeEvent.kt. `target` is obtained via an
// unchecked cast (same as the original `unsafeCast<Element>()`, just spelled with a regular `as`
// -- an unchecked cast on an erased generic type parameter behaves the same way on every target).
//
// Element's upper bound was NativeEventTarget through task 2.4, tightening it to "the thing a
// native event's target actually is". Task 2.5 widens it back to unconstrained: SyntheticInputEvent
// /SyntheticChangeEvent/SyntheticSelectEvent are parameterized on the concrete *element* type
// (e.g. HTMLInputElement) so InputAttrsScope/TextAreaAttrsScope/SelectAttrsScope can expose typed
// onInput/onChange/onSelect callbacks -- and the commonMain DomElement/DomHtmlElement hierarchy
// (task 2.1) is intentionally a separate expect hierarchy from NativeEventTarget (JVM's DomElement
// chain is real JvmElement subclasses; NativeEventTarget is an empty marker actual class on JVM;
// a class can't extend both without a deeper redesign). The cast below is already unchecked, so
// this bound was never load-bearing for correctness -- only for expressiveness -- and widening it
// is a source-compatible relaxation.
open class SyntheticEvent<Element> internal constructor(
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
