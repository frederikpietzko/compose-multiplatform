package org.jetbrains.compose.web.internal.runtime

import org.w3c.dom.events.EventListener

@ComposeWebInternalApi
actual interface NamedEventListener : EventListener {
    actual val name: String
}
