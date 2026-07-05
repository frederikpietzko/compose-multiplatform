/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.CHANGE
import org.jetbrains.compose.web.attributes.EventsListenerScope.Companion.INPUT
import org.jetbrains.compose.web.attributes.SyntheticEventListener
import org.jetbrains.compose.web.dom.HTMLSelectElement
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent

internal actual class SelectInputEventListener actual constructor(
    listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<String?, HTMLSelectElement>>(INPUT, listener)

internal actual class SelectChangeEventListener actual constructor(
    listener: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<String?, HTMLSelectElement>>(CHANGE, listener)
