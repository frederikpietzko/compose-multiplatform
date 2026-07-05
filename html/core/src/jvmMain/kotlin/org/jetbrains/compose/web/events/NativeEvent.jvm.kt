/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

// JVM/SSR fires no events -- none of these classes are ever instantiated for real. They exist
// purely so the commonMain Synthetic*Event wrapper classes (which read straight off Native*Event
// properties) type-check on the JVM target too. Field defaults are arbitrary placeholders, never
// actually read. See NativeEvent.kt (commonMain) for the full rationale.

actual open class NativeEvent {
    actual val bubbles: Boolean = false
    actual val cancelable: Boolean = false
    actual val composed: Boolean = false
    actual val currentTarget: NativeEventTarget? = null
    actual val eventPhase: Short = 0
    actual val defaultPrevented: Boolean = false
    actual val timeStamp: Number = 0
    actual val type: String = ""
    actual val isTrusted: Boolean = false
    actual val target: NativeEventTarget? = null

    actual fun preventDefault() {}
    actual fun stopPropagation() {}
    actual fun stopImmediatePropagation() {}
    actual fun composedPath(): Array<NativeEventTarget> = emptyArray()
}

actual abstract class NativeEventTarget

actual open class NativeMouseEvent : NativeEvent() {
    actual val altKey: Boolean = false
    actual val button: Short = 0
    actual val buttons: Short = 0
    actual val clientX: Int = 0
    actual val clientY: Int = 0
    actual val ctrlKey: Boolean = false
    actual val metaKey: Boolean = false
    actual val offsetX: Double = 0.0
    actual val offsetY: Double = 0.0
    actual val pageX: Double = 0.0
    actual val pageY: Double = 0.0
    actual val region: String? = null
    actual val relatedTarget: NativeEventTarget? = null
    actual val screenX: Int = 0
    actual val screenY: Int = 0
    actual val shiftKey: Boolean = false
    actual val x: Double = 0.0
    actual val y: Double = 0.0

    actual fun getModifierState(keyArg: String): Boolean = false
}

actual open class NativeWheelEvent : NativeMouseEvent() {
    actual val deltaX: Double = 0.0
    actual val deltaY: Double = 0.0
    actual val deltaZ: Double = 0.0
    actual val deltaMode: Int = 0
}

actual open class NativeDragEvent : NativeMouseEvent() {
    actual val dataTransfer: NativeDataTransfer? = null
}

actual open class NativeKeyboardEvent : NativeEvent() {
    actual val altKey: Boolean = false
    actual val code: String = ""
    actual val ctrlKey: Boolean = false
    actual val isComposing: Boolean = false
    actual val key: String = ""
    actual val location: Int = 0
    actual val metaKey: Boolean = false
    actual val repeat: Boolean = false
    actual val shiftKey: Boolean = false

    actual fun getModifierState(keyArg: String): Boolean = false
}

actual open class NativeFocusEvent : NativeEvent() {
    actual val relatedTarget: NativeEventTarget? = null
}

actual open class NativeClipboardEvent : NativeEvent() {
    actual val clipboardData: NativeDataTransfer? = null
}

actual open class NativeTouchEvent : NativeEvent() {
    actual val altKey: Boolean = false
    actual val changedTouches: NativeTouchList = object : NativeTouchList() {}
    actual val ctrlKey: Boolean = false
    actual val metaKey: Boolean = false
    actual val shiftKey: Boolean = false
    actual val touches: NativeTouchList = object : NativeTouchList() {}
}

actual abstract class NativeTouchList

actual abstract class NativeDataTransfer {
    actual fun getData(format: String): String = ""
    actual fun setData(format: String, data: String) {}
}

internal actual fun NativeMouseEvent.readMovementX(): Int = 0
internal actual fun NativeMouseEvent.readMovementY(): Int = 0

internal actual fun NativeKeyboardEvent.readLocale(): String = ""

internal actual fun NativeEventTarget?.readInputValueOrNull(): String? = null

internal actual fun extractInputEventDetails(nativeEvent: NativeEvent): InputEventDetails =
    InputEventDetails(data = null, dataTransfer = null, inputType = null, isComposing = false)
