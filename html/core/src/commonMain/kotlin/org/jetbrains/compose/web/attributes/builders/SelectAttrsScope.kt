/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.attributes

import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.SyntheticEventListener
import org.jetbrains.compose.web.dom.HTMLSelectElement
import org.jetbrains.compose.web.events.SyntheticChangeEvent
import org.jetbrains.compose.web.events.SyntheticInputEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

@Deprecated(
    message = "Renamed to SelectAttrsScope",
    replaceWith = ReplaceWith("SelectAttrsScope", "org.jetbrains.compose.web.attributes.builders.SelectAttrsScope")
)
typealias SelectAttrsBuilder = SelectAttrsScope

class SelectAttrsScope(attrsScope: AttrsScope<HTMLSelectElement>) : AttrsScope<HTMLSelectElement> by attrsScope {

    @OptIn(ComposeWebInternalApi::class)
    fun onInput(
        listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        registerEventListener(SelectInputEventListener(listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onChange(
        listener: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
    ) {
        registerEventListener(SelectChangeEventListener(listener))
    }
}

// Moved from jsMain to commonMain (task 2.5), split into expect/actual for the same reason as
// InputEventListener/ChangeEventListener/SelectEventListener in attributes/SyntheticEventListener.kt:
// the JS actual needs to override handleEvent(event: Event), a JS-only-typed method. The
// `eventName: String = INPUT` default constructor param that SelectInputEventListener had in
// jsMain was never relied on (the only call site omits it), so it's dropped here; the JS/JVM actuals
// each pass their fixed INPUT/CHANGE event name directly to the SyntheticEventListener super
// constructor instead.
internal expect class SelectInputEventListener(
    listener: (SyntheticInputEvent<String?, HTMLSelectElement>) -> Unit
) : SyntheticEventListener<SyntheticInputEvent<String?, HTMLSelectElement>>

internal expect class SelectChangeEventListener(
    listener: (SyntheticChangeEvent<String?, HTMLSelectElement>) -> Unit
) : SyntheticEventListener<SyntheticChangeEvent<String?, HTMLSelectElement>>
