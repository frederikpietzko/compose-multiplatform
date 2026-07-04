package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.JvmElement
import org.jetbrains.compose.web.internal.runtime.JvmTextNode
import org.jetbrains.compose.web.internal.runtime.NativeNode

actual fun createNativeElement(tagName: String): NativeNode = JvmElement(tagName)

actual fun createNativeText(text: String): NativeNode = JvmTextNode(text)
