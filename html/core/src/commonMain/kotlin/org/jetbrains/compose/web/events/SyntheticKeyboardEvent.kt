/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.events

import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.NativeKeyboardEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.readLocale

// Moved from jsMain to commonMain (task 2.4): org.w3c.dom.events.KeyboardEvent -> NativeKeyboardEvent.
class SyntheticKeyboardEvent internal constructor(
    nativeEvent: NativeKeyboardEvent
) : SyntheticEvent<NativeEventTarget>(nativeEvent) {

    private val keyboardEvent = nativeEvent

    val altKey: Boolean = nativeEvent.altKey
    val code: String = nativeEvent.code
    val ctrlKey: Boolean = nativeEvent.ctrlKey
    val isComposing: Boolean = nativeEvent.isComposing
    val key: String = nativeEvent.key
    val locale: String = nativeEvent.readLocale()
    val location: Int = nativeEvent.location
    val metaKey: Boolean = nativeEvent.metaKey
    val repeat: Boolean = nativeEvent.repeat
    val shiftKey: Boolean = nativeEvent.shiftKey

    fun getModifierState(keyArg: String): Boolean = keyboardEvent.getModifierState(keyArg)

    fun getNormalizedKey(): String = key.let {
        normalizedKeys[it] ?: it
    }
}

private val normalizedKeys = mapOf(
    "Esc" to "Escape",
    "Spacebar" to " ",
    "Left" to "ArrowLeft",
    "Up" to "ArrowUp",
    "Right" to "ArrowRight",
    "Down" to "ArrowDown",
    "Del" to "Delete",
    "Apps" to "ContextMenu",
    "Menu" to "ContextMenu",
    "Scroll" to "ScrollLock",
    "MozPrintableKey" to "Unidentified",
)
// Firefox bug for Windows key https://bugzilla.mozilla.org/show_bug.cgi?id=1232918
