package org.jetbrains.compose.web.dom

import kotlinx.browser.document
import org.jetbrains.compose.web.internal.runtime.NativeNode

actual fun createNativeElement(tagName: String): NativeNode = document.createElement(tagName)

actual fun createNativeText(text: String): NativeNode = document.createTextNode(text)
