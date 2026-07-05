/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeDataTransfer
import androidx.compose.web.events.NativeEvent
import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.extractInputEventDetails

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.Event/EventTarget -> Native*.
//
// @param nativeEvent: NativeEvent - we don't use [NativeInputEvent] here (there's no such expect
// type), since for some cases the native event can be just a plain Event -- same reasoning as the
// original jsMain comment about org.w3c.dom.events.InputEvent.
//
// data/dataTransfer/inputType/isComposing aren't reliably present on every native event that can
// reach here, so (as in the original jsMain code) they're read dynamically -- via the
// extractInputEventDetails expect fun -- rather than declared as real NativeEvent members.
class SyntheticInputEvent<ValueType, Element : NativeEventTarget> internal constructor(
    val value: ValueType,
    nativeEvent: NativeEvent
) : SyntheticEvent<Element>(
    nativeEvent = nativeEvent
) {
    private val details = extractInputEventDetails(nativeEvent)

    val data: String? = details.data
    val dataTransfer: NativeDataTransfer? = details.dataTransfer
    val inputType: String? = details.inputType
    val isComposing: Boolean = details.isComposing
}
