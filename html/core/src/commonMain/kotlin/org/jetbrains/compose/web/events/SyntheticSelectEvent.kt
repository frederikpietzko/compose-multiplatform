/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeEvent
import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.readInputValueOrNull

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.Event/EventTarget -> Native*.
//
// SelectionInfoDetails used to be a jsMain-only `external interface`, unsafe-cast directly from
// the event's target (input/textarea elements have native selectionStart/selectionEnd
// properties). `external` isn't available outside JS/Native source sets, so it's now a plain data
// holder; the (still JS-only) SelectEventListener in attributes/SyntheticEventListener.kt reads
// the two fields dynamically off the target and constructs one of these to pass in.
class SyntheticSelectEvent<Element : NativeEventTarget> internal constructor(
    nativeEvent: NativeEvent,
    selectionInfoDetails: SelectionInfoDetails
) : SyntheticEvent<Element>(nativeEvent) {

    val selectionStart: Int = selectionInfoDetails.selectionStart
    val selectionEnd: Int = selectionInfoDetails.selectionEnd


    fun selection(): String {
        return nativeEvent.target.readInputValueOrNull()?.substring(
            selectionStart, selectionEnd
        ) ?: ""
    }
}

internal class SelectionInfoDetails(
    val selectionStart: Int,
    val selectionEnd: Int,
)
