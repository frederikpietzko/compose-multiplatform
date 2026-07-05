/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeEvent
import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticEvent

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.Event -> NativeEvent.
//
// AnimationEventDetails used to be a jsMain-only `external interface`, unsafe-cast directly from
// the raw event object (the Kotlin org.w3c.dom bindings don't declare an AnimationEvent type at
// all). `external` isn't available outside JS/Native source sets, so it's now a plain data holder;
// the (still JS-only) AnimationEventListener in attributes/SyntheticEventListener.kt reads the
// three fields dynamically and constructs one of these to pass in, instead of unsafe-casting the
// whole event.
class SyntheticAnimationEvent internal constructor(
    nativeEvent: NativeEvent,
    animationEventDetails: AnimationEventDetails
) : SyntheticEvent<NativeEventTarget>(nativeEvent) {

    val animationName: String = animationEventDetails.animationName
    val elapsedTime: Number = animationEventDetails.elapsedTime
    val pseudoElement: String = animationEventDetails.pseudoElement
}

internal class AnimationEventDetails(
    val animationName: String,
    val elapsedTime: Number,
    val pseudoElement: String,
)
