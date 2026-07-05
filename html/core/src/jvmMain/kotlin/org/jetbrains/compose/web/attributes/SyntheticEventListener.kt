package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.CHANGE
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.SELECT
import org.jetbrains.compose.web.events.SyntheticAnimationEvent
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticClipboardEvent
import org.jetbrains.compose.web.events.SyntheticFocusEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.events.SyntheticKeyboardEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// JVM/SSR has no event dispatch -- applyEventListeners is inert (see ElementUpdater.jvm.kt), so
// there is no handleEvent concept here at all; these are trivial stand-ins that merely satisfy
// the common expect surface.
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
) : SyntheticEventListener<T>(event, listener)

internal actual class AnimationEventListener actual constructor(
    event: String,
    listener: (SyntheticAnimationEvent) -> Unit
) : SyntheticEventListener<SyntheticAnimationEvent>(event, listener)

internal actual class MouseEventListener actual constructor(
    event: String,
    listener: (SyntheticMouseEvent) -> Unit
) : SyntheticEventListener<SyntheticMouseEvent>(event, listener)

internal actual class MouseWheelEventListener actual constructor(
    event: String,
    listener: (SyntheticWheelEvent) -> Unit
) : SyntheticEventListener<SyntheticWheelEvent>(event, listener)

internal actual class KeyboardEventListener actual constructor(
    event: String,
    listener: (SyntheticKeyboardEvent) -> Unit
) : SyntheticEventListener<SyntheticKeyboardEvent>(event, listener)

internal actual class FocusEventListener actual constructor(
    event: String,
    listener: (SyntheticFocusEvent) -> Unit
) : SyntheticEventListener<SyntheticFocusEvent>(event, listener)

internal actual class TouchEventListener actual constructor(
    event: String,
    listener: (SyntheticTouchEvent) -> Unit
) : SyntheticEventListener<SyntheticTouchEvent>(event, listener)

internal actual class DragEventListener actual constructor(
    event: String,
    listener: (SyntheticDragEvent) -> Unit
) : SyntheticEventListener<SyntheticDragEvent>(event, listener)

internal actual class ClipboardEventListener actual constructor(
    event: String,
    listener: (SyntheticClipboardEvent) -> Unit
) : SyntheticEventListener<SyntheticClipboardEvent>(event, listener)

internal actual class InputEventListener<InputValueType, Target> actual constructor(
    eventName: String,
    actual val inputType: InputType<InputValueType>,
    listener: (SyntheticInputEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<InputValueType, Target>>(eventName, listener)

internal actual class ChangeEventListener<InputValueType, Target> actual constructor(
    actual val inputType: InputType<InputValueType>,
    listener: (SyntheticChangeEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<InputValueType, Target>>(CHANGE, listener)

internal actual class SelectEventListener<Target> actual constructor(
    listener: (SyntheticSelectEvent<Target>) -> Unit
) : SyntheticEventListener<SyntheticSelectEvent<Target>>(SELECT, listener)
