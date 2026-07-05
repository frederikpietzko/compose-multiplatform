/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

// Native-event seam for task 2.4: the Synthetic*Event wrapper classes (moved to commonMain here)
// used to read directly off org.w3c.dom.events.* types. Every Native* type below stands in for one
// of those browser types.
//
// JS actualizes each type via `actual typealias` straight to the real org.w3c.dom type -- this
// works because every property/function declared on the expect side is copied verbatim (same
// name+signature) from the real browser class, so the compiler's actual-typealias member check is
// satisfied structurally, and because modality (open/abstract) is copied exactly from the browser
// class too (Kotlin's actual-checker requires an exact modality match, same reasoning as
// DomElementTypes.kt).
//
// JVM actualizes each type with a real (mostly field-default / never-really-used) class, chained
// with genuine inheritance mirroring the browser hierarchy -- same technique as
// DomElementTypes.jvm.kt, needed because two distinct expect types both aliasing to literally the
// same JVM class breaks the supertype checker. JVM/SSR fires no events, so these classes are never
// instantiated for real; the field defaults exist purely so the class is a valid, complete actual.
//
// A few browser fields aren't present in Kotlin's org.w3c.dom bindings at all (MouseEvent has no
// movementX/movementY, KeyboardEvent has no locale) -- the original jsMain code read those via
// `asDynamic()`. Those stay out of the Native*Event member surface and are instead read through
// small `internal expect fun` helpers declared at the bottom of this file, actualized per-platform.

expect open class NativeEvent {
    val bubbles: Boolean
    val cancelable: Boolean
    val composed: Boolean
    val currentTarget: NativeEventTarget?
    val eventPhase: Short
    val defaultPrevented: Boolean
    val timeStamp: Number
    val type: String
    val isTrusted: Boolean
    val target: NativeEventTarget?

    fun preventDefault()
    fun stopPropagation()
    fun stopImmediatePropagation()
    fun composedPath(): Array<NativeEventTarget>
}

expect abstract class NativeEventTarget

expect open class NativeMouseEvent : NativeEvent {
    val altKey: Boolean
    val button: Short
    val buttons: Short
    val clientX: Int
    val clientY: Int
    val ctrlKey: Boolean
    val metaKey: Boolean
    val offsetX: Double
    val offsetY: Double
    val pageX: Double
    val pageY: Double
    val region: String?
    val relatedTarget: NativeEventTarget?
    val screenX: Int
    val screenY: Int
    val shiftKey: Boolean
    val x: Double
    val y: Double

    fun getModifierState(keyArg: String): Boolean
}

expect open class NativeWheelEvent : NativeMouseEvent {
    val deltaX: Double
    val deltaY: Double
    val deltaZ: Double
    val deltaMode: Int
}

expect open class NativeDragEvent : NativeMouseEvent {
    val dataTransfer: NativeDataTransfer?
}

expect open class NativeKeyboardEvent : NativeEvent {
    val altKey: Boolean
    val code: String
    val ctrlKey: Boolean
    val isComposing: Boolean
    val key: String
    val location: Int
    val metaKey: Boolean
    val repeat: Boolean
    val shiftKey: Boolean

    fun getModifierState(keyArg: String): Boolean
}

expect open class NativeFocusEvent : NativeEvent {
    val relatedTarget: NativeEventTarget?
}

expect open class NativeClipboardEvent : NativeEvent {
    val clipboardData: NativeDataTransfer?
}

expect open class NativeTouchEvent : NativeEvent {
    val altKey: Boolean
    val changedTouches: NativeTouchList
    val ctrlKey: Boolean
    val metaKey: Boolean
    val shiftKey: Boolean
    val touches: NativeTouchList
}

expect abstract class NativeTouchList

expect abstract class NativeDataTransfer {
    fun getData(format: String): String
    fun setData(format: String, data: String)
}

// --- Dynamic-only browser fields that aren't part of Kotlin's org.w3c.dom bindings ---

// https://youtrack.jetbrains.com/issue/CMP-5304 -- movementX/movementY are undefined in
// SafariMobile MouseEvent, and aren't declared on org.w3c.dom.events.MouseEvent at all, hence the
// dynamic read (kept in the JS actual) instead of a real member on NativeMouseEvent.
internal expect fun NativeMouseEvent.readMovementX(): Int
internal expect fun NativeMouseEvent.readMovementY(): Int

// KeyboardEvent.locale isn't part of the standard (or the Kotlin binding); read dynamically.
internal expect fun NativeKeyboardEvent.readLocale(): String

// event.target's current text-selection value, read dynamically (target is typically an
// HTMLInputElement/HTMLTextAreaElement, but SyntheticSelectEvent only needs `.value`).
internal expect fun NativeEventTarget?.readInputValueOrNull(): String?

// SyntheticInputEvent reads a handful of fields off the raw native event that either aren't
// present on org.w3c.dom.events.Event, or (per the original comment) may not be present depending
// on which concrete event actually fired -- both read dynamically in the JS actual.
internal class InputEventDetails(
    val data: String?,
    val dataTransfer: NativeDataTransfer?,
    val inputType: String?,
    val isComposing: Boolean,
)

internal expect fun extractInputEventDetails(nativeEvent: NativeEvent): InputEventDetails
