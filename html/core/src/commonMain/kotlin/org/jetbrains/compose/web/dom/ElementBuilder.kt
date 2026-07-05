/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.NativeNode

/**
 * Creates the platform-native node for [tagName].
 *
 * JS: `document.createElement(tagName)`.
 * JVM: a bare [org.jetbrains.compose.web.internal.runtime.JvmElement].
 */
expect fun createNativeElement(tagName: String): NativeNode

/**
 * Creates the platform-native text node for [text].
 *
 * JS: `document.createTextNode(text)`.
 * JVM: a bare [org.jetbrains.compose.web.internal.runtime.JvmTextNode].
 */
expect fun createNativeText(text: String): NativeNode

fun interface ElementBuilder<TElement : DomElement> {
    fun create(): NativeNode

    companion object {
        // it's internal only for testing purposes
        internal val buildersCache = mutableMapOf<String, ElementBuilder<*>>()

        fun <TElement : DomElement> createBuilder(tagName: String): ElementBuilder<TElement> {
            val tagLowercase = tagName.lowercase()
            @Suppress("UNCHECKED_CAST")
            return buildersCache.getOrPut(tagLowercase) {
                ElementBuilderImplementation<TElement>(tagLowercase)
            } as ElementBuilder<TElement>
        }
    }
}

internal class ElementBuilderImplementation<TElement : DomElement>(
    private val tagName: String
) : ElementBuilder<TElement> {
    override fun create(): NativeNode = createNativeElement(tagName)
}
