package org.jetbrains.compose.web.internal.runtime

import org.w3c.dom.events.EventListener

@ComposeWebInternalApi
interface NamedEventListener : EventListener {
    val name: String
}
