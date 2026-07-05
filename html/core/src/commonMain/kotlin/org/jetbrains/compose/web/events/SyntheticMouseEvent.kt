/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.events

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.* -> the Native* expect seam declared in
// NativeEvent.kt. movementX/movementY aren't part of Kotlin's org.w3c.dom.events.MouseEvent
// binding (see https://youtrack.jetbrains.com/issue/CMP-5304), so they're still read through a
// small per-platform expect fun rather than a Native type member.

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent
 */
open class SyntheticMouseEvent internal constructor(
    nativeEvent: NativeMouseEvent
) : SyntheticEvent<NativeEventTarget>(nativeEvent) {

    private val mouseEvent = nativeEvent

    val altKey: Boolean = nativeEvent.altKey
    val button: Short = nativeEvent.button
    val buttons: Short = nativeEvent.buttons
    val clientX: Int = nativeEvent.clientX
    val clientY: Int = nativeEvent.clientY
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val metaKey: Boolean = nativeEvent.metaKey

    val movementX: Int = nativeEvent.readMovementX()
    val movementY: Int = nativeEvent.readMovementY()

    val offsetX: Double = nativeEvent.offsetX
    val offsetY: Double = nativeEvent.offsetY
    val pageX: Double = nativeEvent.pageX
    val pageY: Double = nativeEvent.pageY
    val region: String? = nativeEvent.region
    val relatedTarget: NativeEventTarget? = nativeEvent.relatedTarget
    val screenX: Int = nativeEvent.screenX
    val screenY: Int = nativeEvent.screenY
    val shiftKey: Boolean = nativeEvent.shiftKey
    val x: Double = nativeEvent.x
    val y: Double = nativeEvent.y

    fun getModifierState(keyArg: String): Boolean = mouseEvent.getModifierState(keyArg)
}


/**
 * https://developer.mozilla.org/en-US/docs/Web/API/WheelEvent
 */
class SyntheticWheelEvent(
    nativeEvent: NativeWheelEvent
) : SyntheticMouseEvent(
    nativeEvent
) {
    val deltaX: Double = nativeEvent.deltaX
    val deltaY: Double = nativeEvent.deltaY
    val deltaZ: Double = nativeEvent.deltaZ
    val deltaMode: Int = nativeEvent.deltaMode
}

/**
 * https://developer.mozilla.org/en-US/docs/Web/API/DragEvent
 */
class SyntheticDragEvent(
    nativeEvent: NativeDragEvent
) : SyntheticMouseEvent(
    nativeEvent
) {
    val dataTransfer: NativeDataTransfer? = nativeEvent.dataTransfer
}
