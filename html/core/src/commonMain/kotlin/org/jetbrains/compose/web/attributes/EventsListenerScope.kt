/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NamedEventListener

// Split out of the former jsMain-only EventsListenerScope.kt (task 2.3) so AttrsScope (which
// extends this interface) can move to commonMain. Porting the full rich interface -- ~40
// onClick/onMouseDown/etc. default methods tied to the JS-only org.jetbrains.compose.web.events
// package of SyntheticXxxEvent wrapper classes -- is out of scope for this task, since JVM/SSR has
// no event dispatch at all (applyEventListeners is inert there, see ElementUpdater.jvm.kt).
//
// So only the minimal abstract surface is expect/actual here. The JS actual keeps the full rich
// interface (all onXxx default methods + the event-name companion constants) unchanged, which is
// allowed since actual declarations may declare additional members beyond what expect requires.
// The JVM actual is a bare marker.
expect interface EventsListenerScope {
    @ComposeWebInternalApi
    fun registerEventListener(listener: NamedEventListener)
}

@Deprecated(
    message = "Renamed to EventsListenerScopeBuilder",
    replaceWith = ReplaceWith("EventsListenerScopeBuilder", "org.jetbrains.compose.web.attributes.EventsListenerScopeBuilder")
)
typealias EventsListenerBuilder = EventsListenerScopeBuilder

@OptIn(ComposeWebInternalApi::class)
open class EventsListenerScopeBuilder : EventsListenerScope {
    private val listeners: MutableList<NamedEventListener> = mutableListOf()

    override fun registerEventListener(listener: NamedEventListener) {
        listeners.add(listener)
    }

    internal fun copyListenersFrom(from: EventsListenerScopeBuilder) {
        listeners.addAll(from.listeners)
    }

    internal fun collectListeners(): List<NamedEventListener> = listeners
}
