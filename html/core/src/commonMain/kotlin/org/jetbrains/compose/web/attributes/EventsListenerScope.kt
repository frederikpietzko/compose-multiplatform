package org.jetbrains.compose.web.attributes

// if you change API for this interface, note that there is a link to this file in this tutorial: https://github.com/JetBrains/compose-multiplatform/blob/master/tutorials/HTML/Events_Handling/README.md#other-event-handlers

import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.events.SyntheticAnimationEvent
import org.jetbrains.compose.web.events.SyntheticClipboardEvent
import org.jetbrains.compose.web.events.SyntheticFocusEvent
import org.jetbrains.compose.web.events.SyntheticKeyboardEvent
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// Split out of the former jsMain-only EventsListenerScope.kt (task 2.3) so AttrsScope (which
// extends this interface) could move to commonMain first, deferring the full rich interface --
// ~40 onClick/onMouseDown/etc. methods (plus addEventListener) tied to the
// org.jetbrains.compose.web.events package of SyntheticXxxEvent wrapper classes -- to this task
// (2.4), once those wrapper classes (and the native-event expect/actual seam they need) were
// themselves moved to commonMain.
//
// Follow-up fix (post-2.4 review): with the Synthetic*/Native* seam now fully in commonMain, none
// of this interface's members reference any platform-specific type any more, so there's no reason
// left for EventsListenerScope itself to be an expect/actual declaration -- only
// `registerEventListener` is genuinely abstract (per-implementer), and everything else can be a
// plain default-bodied member of an ordinary common interface, exactly like it was pre-task-2.4
// (back when this whole interface lived only in jsMain). This also restores the public API of
// EventsListenerScopeBuilder (and its deprecated alias EventsListenerBuilder), a public,
// non-AttrsScope implementer of this interface which had temporarily lost the onXxx/addEventListener
// surface when task 2.4 moved it onto AttrsScope instead (AttrsScope inherits it right back now,
// so the duplicate copies that lived directly on AttrsScope were removed -- see AttrsScope.kt).

/**
 * [EventsListenerScope] is used most often not directly but via [AttrsScope].
 * Its purpose is to add events to the element. For all most frequently used events there
 * exist dedicated method. In case you need to support event that doesn't have such method,
 * use [addEventListener].
 */
interface EventsListenerScope {
    @ComposeWebInternalApi
    fun registerEventListener(listener: NamedEventListener)

    /* Mouse Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onContextMenu(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(CONTEXTMENU, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onClick(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(CLICK, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDoubleClick(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(DBLCLICK, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseDown(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSEDOWN, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseUp(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSEUP, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseEnter(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSEENTER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseLeave(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSELEAVE, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseMove(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSEMOVE, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseOut(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSEOUT, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseOver(listener: (SyntheticMouseEvent) -> Unit) {
        registerEventListener(MouseEventListener(MOUSEOVER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onWheel(listener: (SyntheticWheelEvent) -> Unit) {
        registerEventListener(MouseWheelEventListener(WHEEL, listener))
    }

    /* Drag Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onDrag(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DRAG, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDrop(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DROP, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragStart(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DRAGSTART, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragEnd(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DRAGEND, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragOver(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DRAGOVER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragEnter(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DRAGENTER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragLeave(listener: (SyntheticDragEvent) -> Unit) {
        registerEventListener(DragEventListener(DRAGLEAVE, listener))
    }

    /* Clipboard Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onCopy(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(COPY, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onCut(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(CUT, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onPaste(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(PASTE, listener))
    }

    /* Keyboard Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onKeyDown(listener: (SyntheticKeyboardEvent) -> Unit) {
        registerEventListener(KeyboardEventListener(KEYDOWN, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onKeyUp(listener: (SyntheticKeyboardEvent) -> Unit) {
        registerEventListener(KeyboardEventListener(KEYUP, listener))
    }

    /* Focus Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onFocus(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(FOCUS, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onBlur(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(BLUR, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onFocusIn(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(FOCUSIN, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onFocusOut(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(FOCUSOUT, listener))
    }

    /* Touch Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchCancel(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHCANCEL, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchMove(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHMOVE, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchEnd(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHEND, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchStart(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(TOUCHSTART, listener))
    }

    /* Animation Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onAnimationEnd(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(ANIMATIONEND, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onAnimationIteration(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(ANIMATIONITERATION, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onAnimationStart(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(ANIMATIONSTART, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onScroll(listener: (SyntheticEvent<NativeEventTarget>) -> Unit) {
        registerEventListener(GenericEventListener(SCROLL, listener))
    }

    /**
     * [addEventListener] used for adding arbitrary events to the element. It resembles the standard DOM addEventListener method
     * @param eventName - the name of the event
     * @param options - as of now this param is always equal to Options.DEFAULT
     * @listener - event handler
     */
    // The generic <T : SyntheticEvent<*>> overload below (needed by e.g. InputAttrsScope.onInvalid,
    // which passes a listener typed to a specific element subtype)
    // can't be a second member of this interface: on JVM, a member fun and this generic member fun
    // erase to the identical raw-SyntheticEvent signature within the same interface, a genuine
    // "platform declaration clash" that can't be resolved with @JvmName (not allowed on
    // open/interface members). So the generic overload is kept as a top-level extension function
    // instead (see below) -- it compiles into a different JVM class (the file's Kt class) than this
    // interface member, so there's no clash.
    @OptIn(ComposeWebInternalApi::class)
    fun addEventListener(
        eventName: String,
        listener: (SyntheticEvent<NativeEventTarget>) -> Unit
    ) {
        registerEventListener(GenericEventListener(eventName, listener))
    }

    companion object {
        val COPY: String = "copy"
        val CUT: String = "cut"
        val PASTE: String = "paste"
        val CONTEXTMENU: String = "contextmenu"

        val CLICK: String = "click"
        val DBLCLICK: String = "dblclick"
        val FOCUS: String = "focus"
        val BLUR: String = "blur"
        val FOCUSIN: String = "focusin"
        val FOCUSOUT: String = "focusout"

        val KEYDOWN: String = "keydown"
        val KEYUP: String = "keyup"
        val MOUSEDOWN: String = "mousedown"
        val MOUSEUP: String = "mouseup"
        val MOUSEENTER: String = "mouseenter"
        val MOUSELEAVE: String = "mouseleave"
        val MOUSEMOVE: String = "mousemove"
        val MOUSEOUT: String = "mouseout"
        val MOUSEOVER: String = "mouseover"
        val WHEEL: String = "wheel"
        val SCROLL: String = "scroll"
        val SELECT: String = "select"

        val TOUCHCANCEL: String = "touchcancel"
        val TOUCHEND: String = "touchend"
        val TOUCHMOVE: String = "touchmove"
        val TOUCHSTART: String = "touchstart"

        val ANIMATIONCANCEL: String = "animationcancel" // firefox and safari only
        val ANIMATIONEND: String = "animationend"
        val ANIMATIONITERATION: String = "animationiteration"
        val ANIMATIONSTART: String = "animationstart"

        val BEFOREINPUT: String = "beforeinput"
        val INPUT: String = "input"
        val CHANGE: String = "change"
        val INVALID: String = "invalid"

        val DRAG: String = "drag"
        val DROP: String = "drop"
        val DRAGSTART: String = "dragstart"
        val DRAGEND: String = "dragend"
        val DRAGOVER: String = "dragover"
        val DRAGENTER: String = "dragenter"
        val DRAGLEAVE: String = "dragleave"

        val SUBMIT: String = "submit"
        val RESET: String = "reset"
    }
}

/**
 * [addEventListener] used for adding arbitrary events to the element, for cases where the listener
 * needs to be typed to a specific [SyntheticEvent] subtype (e.g. one tied to a specific element
 * type, as in `InputAttrsScope.onInvalid`). See [EventsListenerScope.addEventListener] for the
 * simpler, non-generic overload that lives directly on [EventsListenerScope] as a member.
 *
 * This one stays a top-level extension function (rather than a second EventsListenerScope member)
 * because a member fun and this generic member fun would erase to the identical raw-SyntheticEvent
 * JVM signature within the same interface -- a genuine "platform declaration clash" that can't be
 * resolved with `@JvmName` (not allowed on open/interface members). As a top-level extension
 * function it compiles into a different JVM class than the EventsListenerScope interface member, so
 * no clash occurs.
 *
 * T's Element bound is unconstrained (not NativeEventTarget): call sites like
 * `InputAttrsScope.onInvalid` parameterize SyntheticEvent on the commonMain HTMLInputElement
 * DomHtmlElement subtype, a separate expect hierarchy from NativeEventTarget -- see
 * SyntheticEvent.kt for the full rationale.
 */
@OptIn(ComposeWebInternalApi::class)
fun <T : SyntheticEvent<*>> EventsListenerScope.addEventListener(
    eventName: String,
    listener: (T) -> Unit
) {
    registerEventListener(GenericEventListener(eventName, listener))
}

@Deprecated(
    message = "Renamed to EventsListenerScopeBuilder",
    replaceWith = ReplaceWith("EventsListenerScopeBuilder", "org.jetbrains.compose.web.attributes.EventsListenerScopeBuilder")
)
typealias EventsListenerBuilder = EventsListenerScopeBuilder

@OptIn(ComposeWebInternalApi::class)
open class EventsListenerScopeBuilder : EventsListenerScope {
    private val listeners: MutableList<NamedEventListener> = mutableListOf()

    override fun registerEventListener(listener: NamedEventListener) {
        listeners.add(listener)
    }

    internal fun copyListenersFrom(from: EventsListenerScopeBuilder) {
        listeners.addAll(from.listeners)
    }

    internal fun collectListeners(): List<NamedEventListener> = listeners
}
