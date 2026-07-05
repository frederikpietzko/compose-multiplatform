/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.NativeFocusEvent
import androidx.compose.web.events.SyntheticEvent

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.FocusEvent -> NativeFocusEvent.
class SyntheticFocusEvent internal constructor(
    nativeEvent: NativeFocusEvent,
) : SyntheticEvent<NativeEventTarget>(nativeEvent) {

    val relatedTarget: NativeEventTarget? = nativeEvent.relatedTarget
}
