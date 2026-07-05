package org.jetbrains.compose.web.attributes

// if you change API for this interface, note that there is a link to this file in this tutorial: https://github.com/JetBrains/compose-multiplatform/blob/master/tutorials/HTML/Events_Handling/README.md#other-event-handlers

import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// Split out of the former jsMain-only EventsListenerScope.kt (task 2.3) so AttrsScope (which
// extends this interface) could move to commonMain first, deferring the full rich interface --
// ~40 onClick/onMouseDown/etc. methods (plus addEventListener) tied to the
// org.jetbrains.compose.web.events package of SyntheticXxxEvent wrapper classes -- to this task
// (2.4), once those wrapper classes (and the native-event expect/actual seam they need) were
// themselves moved to commonMain.
//
// Note on shape: the onXxx methods can't live as *default-bodied members* of this expect
// interface -- Kotlin rejects that ("Expected declaration cannot have a body"), unlike expect
// *class* members which may add extra actual-only members but may not carry a body themselves
// either. They also can't be common top-level extension functions on EventsListenerScope: real
// call sites (e.g. `Button({ onClick { ... } })`, see core/src/jsTest/kotlin/elements/EventTests.kt)
// invoke onClick/onMouseDown/etc. via AttrsScope's implicit receiver with no explicit import of
// this package, which only resolves if these are genuine interface members (as they always were
// pre-task-2.4). So the onXxx methods (plus addEventListener) are declared as default-bodied
// MEMBERS of AttrsScope instead (see AttrsScope.kt) -- the only real implementer of
// EventsListenerScope in this codebase (confirmed via grep for EventsListenerScope(Builder)
// usages) and itself a plain, non-expect interface where default bodies are fully legal. Only the
// abstract `registerEventListener` and the (name-only) companion constants remain here; they need
// real actual counterparts per platform, with the JS/JVM actual constants keeping the exact same
// values as before (see EventsListenerScope.js.kt / EventsListenerScope.jvm.kt).

/**
 * [EventsListenerScope] is used most often not directly but via [AttrsScope].
 * Its purpose is to add events to the element. For all most frequently used events there
 * exist dedicated method. In case you need to support event that doesn't have such method,
 * use `addEventListener` (declared on [AttrsScope]).
 */
expect interface EventsListenerScope {
    @ComposeWebInternalApi
    fun registerEventListener(listener: NamedEventListener)

    companion object {
        val COPY: String
        val CUT: String
        val PASTE: String
        val CONTEXTMENU: String

        val CLICK: String
        val DBLCLICK: String
        val FOCUS: String
        val BLUR: String
        val FOCUSIN: String
        val FOCUSOUT: String

        val KEYDOWN: String
        val KEYUP: String
        val MOUSEDOWN: String
        val MOUSEUP: String
        val MOUSEENTER: String
        val MOUSELEAVE: String
        val MOUSEMOVE: String
        val MOUSEOUT: String
        val MOUSEOVER: String
        val WHEEL: String
        val SCROLL: String
        val SELECT: String

        val TOUCHCANCEL: String
        val TOUCHEND: String
        val TOUCHMOVE: String
        val TOUCHSTART: String

        val ANIMATIONCANCEL: String // firefox and safari only
        val ANIMATIONEND: String
        val ANIMATIONITERATION: String
        val ANIMATIONSTART: String

        val BEFOREINPUT: String
        val INPUT: String
        val CHANGE: String
        val INVALID: String

        val DRAG: String
        val DROP: String
        val DRAGSTART: String
        val DRAGEND: String
        val DRAGOVER: String
        val DRAGENTER: String
        val DRAGLEAVE: String

        val SUBMIT: String
        val RESET: String
    }
}

/**
 * [addEventListener] used for adding arbitrary events to the element, for cases where the listener
 * needs to be typed to a specific [SyntheticEvent] subtype (e.g. one tied to a specific element
 * type, as in `InputAttrsScope.onInvalid`). See [AttrsScope.addEventListener] for the simpler,
 * non-generic overload that lives directly on [AttrsScope] as a member.
 *
 * This one stays a top-level extension function (rather than a second AttrsScope member) because a
 * member fun and this generic member fun would erase to the identical raw-SyntheticEvent JVM
 * signature within the same interface -- a genuine "platform declaration clash" that can't be
 * resolved with `@JvmName` (not allowed on open/interface members). As a top-level extension
 * function it compiles into a different JVM class than the AttrsScope interface member, so no
 * clash occurs.
 */
@OptIn(ComposeWebInternalApi::class)
fun <T : SyntheticEvent<out NativeEventTarget>> EventsListenerScope.addEventListener(
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
