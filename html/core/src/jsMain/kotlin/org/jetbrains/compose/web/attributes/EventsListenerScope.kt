package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// The full rich onClick/onMouseDown/etc. surface moved to commonMain (task 2.4) as top-level
// extension functions (expect interfaces can't have default-bodied members), so this actual only
// needs to supply registerEventListener and the real event-name constant values.
actual interface EventsListenerScope {
    @ComposeWebInternalApi
    actual fun registerEventListener(listener: NamedEventListener)

    actual companion object {
        actual val COPY: String = "copy"
        actual val CUT: String = "cut"
        actual val PASTE: String = "paste"
        actual val CONTEXTMENU: String = "contextmenu"

        actual val CLICK: String = "click"
        actual val DBLCLICK: String = "dblclick"
        actual val FOCUS: String = "focus"
        actual val BLUR: String = "blur"
        actual val FOCUSIN: String = "focusin"
        actual val FOCUSOUT: String = "focusout"

        actual val KEYDOWN: String = "keydown"
        actual val KEYUP: String = "keyup"
        actual val MOUSEDOWN: String = "mousedown"
        actual val MOUSEUP: String = "mouseup"
        actual val MOUSEENTER: String = "mouseenter"
        actual val MOUSELEAVE: String = "mouseleave"
        actual val MOUSEMOVE: String = "mousemove"
        actual val MOUSEOUT: String = "mouseout"
        actual val MOUSEOVER: String = "mouseover"
        actual val WHEEL: String = "wheel"
        actual val SCROLL: String = "scroll"
        actual val SELECT: String = "select"

        actual val TOUCHCANCEL: String = "touchcancel"
        actual val TOUCHEND: String = "touchend"
        actual val TOUCHMOVE: String = "touchmove"
        actual val TOUCHSTART: String = "touchstart"

        actual val ANIMATIONCANCEL: String = "animationcancel" // firefox and safari only
        actual val ANIMATIONEND: String = "animationend"
        actual val ANIMATIONITERATION: String = "animationiteration"
        actual val ANIMATIONSTART: String = "animationstart"

        actual val BEFOREINPUT: String = "beforeinput"
        actual val INPUT: String = "input"
        actual val CHANGE: String = "change"
        actual val INVALID: String = "invalid"

        actual val DRAG: String = "drag"
        actual val DROP: String = "drop"
        actual val DRAGSTART: String = "dragstart"
        actual val DRAGEND: String = "dragend"
        actual val DRAGOVER: String = "dragover"
        actual val DRAGENTER: String = "dragenter"
        actual val DRAGLEAVE: String = "dragleave"

        actual val SUBMIT: String = "submit"
        actual val RESET: String = "reset"
    }
}
