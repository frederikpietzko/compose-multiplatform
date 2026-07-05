/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.svg.*
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeWebSvgApi::class)
class SvgRenderToStringTest {
    @Test
    fun rendersSvgTreeToString() {
        // circle is not in HtmlSerializer's VOID_TAGS, so it gets a closing tag rather than being
        // self-closed -- same as every other non-void element on the JVM serializer.
        val html = renderToString {
            Svg {
                Circle(50, 60, 70)
            }
        }
        assertEquals("""<svg><circle cx="50" cy="60" r="70"></circle></svg>""", html)
    }
}
