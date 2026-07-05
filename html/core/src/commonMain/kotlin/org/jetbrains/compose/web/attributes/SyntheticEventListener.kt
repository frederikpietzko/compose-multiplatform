/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes

import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.events.SyntheticAnimationEvent
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticClipboardEvent
import org.jetbrains.compose.web.events.SyntheticFocusEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.events.SyntheticKeyboardEvent
import org.jetbrains.compose.web.events.SyntheticSelectEvent
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// Moved from jsMain to commonMain (task 2.4).
//
// These concrete listener glue classes each need a real per-platform actual (not a plain common
// class) because they implement NamedEventListener, whose JS actual additionally extends
// org.w3c.dom.events.EventListener and thus requires a handleEvent(event: Event) override -- a
// JS-only-typed method that cannot be written in common code. So:
//  - the JS actual for each class overrides handleEvent, constructing its SyntheticXxxEvent from
//    the raw native event exactly as before task 2.4 (unsafeCast'ing the raw event to the precise
//    org.w3c.dom event subtype);
//  - the JVM actual bodies are empty/trivial, since applyEventListeners never actually dispatches
//    listeners on JVM/SSR (see ElementUpdater.jvm.kt) -- handleEvent doesn't even exist as a
//    concept there.
@OptIn(ComposeWebInternalApi::class)
expect abstract class SyntheticEventListener<T : SyntheticEvent<*>> internal constructor(
    event: String,
    listener: (T) -> Unit
) : NamedEventListener {
    val event: String
    val listener: (T) -> Unit
    override val name: String
}

// Used directly by the generic onScroll/addEventListener helpers in EventsListenerScope.kt, where
// there's no more specific SyntheticXxxEvent subtype to dispatch on.
internal expect class GenericEventListener<T : SyntheticEvent<*>>(
    event: String,
    listener: (T) -> Unit
) : SyntheticEventListener<T>

internal expect class AnimationEventListener(
    event: String,
    listener: (SyntheticAnimationEvent) -> Unit
) : SyntheticEventListener<SyntheticAnimationEvent>

internal expect class MouseEventListener(
    event: String,
    listener: (SyntheticMouseEvent) -> Unit
) : SyntheticEventListener<SyntheticMouseEvent>

internal expect class MouseWheelEventListener(
    event: String,
    listener: (SyntheticWheelEvent) -> Unit
) : SyntheticEventListener<SyntheticWheelEvent>

internal expect class KeyboardEventListener(
    event: String,
    listener: (SyntheticKeyboardEvent) -> Unit
) : SyntheticEventListener<SyntheticKeyboardEvent>

internal expect class FocusEventListener(
    event: String,
    listener: (SyntheticFocusEvent) -> Unit
) : SyntheticEventListener<SyntheticFocusEvent>

internal expect class TouchEventListener(
    event: String,
    listener: (SyntheticTouchEvent) -> Unit
) : SyntheticEventListener<SyntheticTouchEvent>

internal expect class DragEventListener(
    event: String,
    listener: (SyntheticDragEvent) -> Unit
) : SyntheticEventListener<SyntheticDragEvent>

internal expect class ClipboardEventListener(
    event: String,
    listener: (SyntheticClipboardEvent) -> Unit
) : SyntheticEventListener<SyntheticClipboardEvent>

// Moved from jsMain to commonMain (task 2.5), needed by InputAttrsScope/TextAreaAttrsScope which
// are also moving to commonMain. Target is unconstrained (not the JS-only EventTarget, and not
// NativeEventTarget either): call sites parameterize it on the commonMain HTMLInputElement/
// HTMLTextAreaElement DomHtmlElement subtypes, which are a separate expect hierarchy from
// NativeEventTarget -- see SyntheticEvent.kt for the full rationale. The `eventName` default that
// InputEventListener had in jsMain was never relied on (every call site passes it explicitly) so
// it's dropped here.
internal expect class InputEventListener<InputValueType, Target>(
    eventName: String,
    inputType: InputType<InputValueType>,
    listener: (SyntheticInputEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<InputValueType, Target>> {
    val inputType: InputType<InputValueType>
}

internal expect class ChangeEventListener<InputValueType, Target>(
    inputType: InputType<InputValueType>,
    listener: (SyntheticChangeEvent<InputValueType, Target>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<InputValueType, Target>> {
    val inputType: InputType<InputValueType>
}

internal expect class SelectEventListener<Target>(
    listener: (SyntheticSelectEvent<Target>) -> Unit
) : SyntheticEventListener<SyntheticSelectEvent<Target>>
