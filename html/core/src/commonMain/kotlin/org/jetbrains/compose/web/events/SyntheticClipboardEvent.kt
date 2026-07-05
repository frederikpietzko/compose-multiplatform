/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeClipboardEvent
import androidx.compose.web.events.NativeDataTransfer
import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticEvent

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.clipboard.ClipboardEvent -> NativeClipboardEvent,
// org.w3c.dom.DataTransfer -> NativeDataTransfer.
class SyntheticClipboardEvent internal constructor(
    nativeEvent: NativeClipboardEvent
) : SyntheticEvent<NativeEventTarget>(nativeEvent) {

    val clipboardData: NativeDataTransfer? = nativeEvent.clipboardData

    fun getData(format: String): String? {
        return clipboardData?.getData(format)
    }

    fun setData(format: String, data: String) {
        clipboardData?.setData(format, data)
    }
}
