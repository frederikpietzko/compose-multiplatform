/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

actual typealias NativeEvent = org.w3c.dom.events.Event
actual typealias NativeEventTarget = org.w3c.dom.events.EventTarget

actual typealias NativeMouseEvent = org.w3c.dom.events.MouseEvent
actual typealias NativeWheelEvent = org.w3c.dom.events.WheelEvent
actual typealias NativeDragEvent = org.w3c.dom.DragEvent
actual typealias NativeKeyboardEvent = org.w3c.dom.events.KeyboardEvent
actual typealias NativeFocusEvent = org.w3c.dom.events.FocusEvent
actual typealias NativeClipboardEvent = org.w3c.dom.clipboard.ClipboardEvent
actual typealias NativeTouchEvent = org.w3c.dom.TouchEvent

actual typealias NativeTouchList = org.w3c.dom.TouchList
actual typealias NativeDataTransfer = org.w3c.dom.DataTransfer

internal actual fun NativeMouseEvent.readMovementX(): Int = (asDynamic().movementX as? Int) ?: 0
internal actual fun NativeMouseEvent.readMovementY(): Int = (asDynamic().movementY as? Int) ?: 0

internal actual fun NativeKeyboardEvent.readLocale(): String = asDynamic().locale.toString()

internal actual fun NativeEventTarget?.readInputValueOrNull(): String? =
    this?.asDynamic()?.value?.unsafeCast<String?>()

internal actual fun extractInputEventDetails(nativeEvent: NativeEvent): InputEventDetails {
    val dyn = nativeEvent.asDynamic()
    return InputEventDetails(
        data = dyn.data?.unsafeCast<String>(),
        dataTransfer = dyn.dataTransfer?.unsafeCast<NativeDataTransfer>(),
        inputType = dyn.inputType?.unsafeCast<String>(),
        isComposing = dyn.isComposing?.unsafeCast<Boolean>() ?: false,
    )
}
