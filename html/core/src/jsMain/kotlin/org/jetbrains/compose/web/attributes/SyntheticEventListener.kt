package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.CHANGE
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.INPUT
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.SELECT
import org.jetbrains.compose.web.events.*
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener
import org.w3c.dom.DragEvent
import org.w3c.dom.TouchEvent
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.*

// The class stays actual-abstract (rather than providing a concrete handleEvent override here)
// because handleEvent is an obligation that comes solely from NamedEventListener's JS-only
// EventListener supertype -- it has no counterpart at all in the commonMain expect declaration.
// Kotlin's expect/actual checker rejects an actual class concretely implementing such an
// "invisible to common" abstract member at the same class that pairs with the expect declaration
// ("the modalities are different ('abstract' vs 'open')"); it's fine, though, for a *further* JS
// actual subclass (GenericEventListener, AnimationEventListener, MouseEventListener, ...) to
// concretely override it, since by then it's an ordinary override of an already-actual-declared
// member rather than a fresh implementation of a member absent from expect.
@OptIn(ComposeWebInternalApi::class)
actual abstract class SyntheticEventListener<T : SyntheticEvent<*>> internal actual constructor(
    actual val event: String,
    actual val listener: (T) -> Unit
) : NamedEventListener {
    actual override val name: String = event
}

internal actual class GenericEventListener<T : SyntheticEvent<*>> actual constructor(
    event: String,
    listener: (T) -> Unit
) : SyntheticEventListener<T>(event, listener) {
    @Suppress("UNCHECKED_CAST")
    override fun handleEvent(event: Event) {
        listener(SyntheticEvent<EventTarget>(event).unsafeCast<T>())
    }
}

internal actual class AnimationEventListener actual constructor(
    event: String,
    listener: (SyntheticAnimationEvent) -> Unit
) : SyntheticEventListener<SyntheticAnimationEvent>(
    event, listener
) {
    override fun handleEvent(event: Event) {
        listener(SyntheticAnimationEvent(event, event.unsafeCast<AnimationEventDetails>()))
    }
}

internal actual class MouseEventListener actual constructor(
    event: String,
    listener: (SyntheticMouseEvent) -> Unit
) : SyntheticEventListener<SyntheticMouseEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticMouseEvent(event.unsafeCast<MouseEvent>()))
    }
}

internal actual class MouseWheelEventListener actual constructor(
    event: String,
    listener: (SyntheticWheelEvent) -> Unit
) : SyntheticEventListener<SyntheticWheelEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticWheelEvent(event.unsafeCast<WheelEvent>()))
    }
}

internal actual class KeyboardEventListener actual constructor(
    event: String,
    listener: (SyntheticKeyboardEvent) -> Unit
) : SyntheticEventListener<SyntheticKeyboardEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticKeyboardEvent(event.unsafeCast<KeyboardEvent>()))
    }
}

internal actual class FocusEventListener actual constructor(
    event: String,
    listener: (SyntheticFocusEvent) -> Unit
) : SyntheticEventListener<SyntheticFocusEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticFocusEvent(event.unsafeCast<FocusEvent>()))
    }
}

internal actual class TouchEventListener actual constructor(
    event: String,
    listener: (SyntheticTouchEvent) -> Unit
) : SyntheticEventListener<SyntheticTouchEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticTouchEvent(event.unsafeCast<TouchEvent>()))
    }
}

internal actual class DragEventListener actual constructor(
    event: String,
    listener: (SyntheticDragEvent) -> Unit
) : SyntheticEventListener<SyntheticDragEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticDragEvent(event.unsafeCast<DragEvent>()))
    }
}

internal actual class ClipboardEventListener actual constructor(
    event: String,
    listener: (SyntheticClipboardEvent) -> Unit
) : SyntheticEventListener<SyntheticClipboardEvent>(event, listener) {
    override fun handleEvent(event: Event) {
        listener(SyntheticClipboardEvent(event.unsafeCast<ClipboardEvent>()))
    }
}

// Not part of the common EventsListenerScope surface (only used by InputAttrsScope.kt, which is
// jsMain-only / tied to the jsMain-only InputType), so these stay plain jsMain classes extending
// the now-actualized SyntheticEventListener base -- no expect/actual split needed.
internal class InputEventListener<InputValueType, Target : EventTarget>(
    eventName: String = INPUT,
    val inputType: InputType<InputValueType>,
    listener: (SyntheticInputEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<InputValueType, Target>>(
    eventName, listener
) {
    override fun handleEvent(event: Event) {
        val value = inputType.inputValue(event)
        listener(SyntheticInputEvent(value, event))
    }
}

internal class ChangeEventListener<InputValueType, Target : EventTarget>(
    val inputType: InputType<InputValueType>,
    listener: (SyntheticChangeEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<InputValueType, Target>>(
    CHANGE, listener
) {
    override fun handleEvent(event: Event) {
        val value = inputType.inputValue(event)
        listener(SyntheticChangeEvent(value, event))
    }
}

internal class SelectEventListener<Target : EventTarget>(
    listener: (SyntheticSelectEvent<Target>) -> Unit
) : SyntheticEventListener<SyntheticSelectEvent<Target>>(
    SELECT, listener
) {
    override fun handleEvent(event: Event) {
        val target = event.target.asDynamic()
        val selectionInfoDetails = SelectionInfoDetails(
            selectionStart = (target.selectionStart as? Int) ?: 0,
            selectionEnd = (target.selectionEnd as? Int) ?: 0
        )
        listener(SyntheticSelectEvent(event, selectionInfoDetails))
    }
}
