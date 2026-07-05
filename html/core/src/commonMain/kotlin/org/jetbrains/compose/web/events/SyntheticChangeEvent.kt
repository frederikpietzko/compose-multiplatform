/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeEvent
import androidx.compose.web.events.SyntheticEvent

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.Event/EventTarget -> Native*.
// Element's bound widened to unconstrained in task 2.5 -- see SyntheticEvent.kt for the rationale.
class SyntheticChangeEvent<Value, Element> internal constructor(
    val value: Value,
    nativeEvent: NativeEvent,
) : SyntheticEvent<Element>(nativeEvent)
