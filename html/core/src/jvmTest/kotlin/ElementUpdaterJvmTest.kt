/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.internal.runtime.*
import org.jetbrains.compose.web.internal.runtime.applyAttrs
import org.jetbrains.compose.web.internal.runtime.applyClasses
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementUpdaterJvmTest {
    @Test fun writesAttrsAndClasses() {
        val el = JvmElement("div")
        (el as NativeNode).applyAttrs(mapOf("id" to "main"))
        el.applyClasses(listOf("a", "b"))
        assertEquals("main", el.attributes["id"])
        assertEquals(listOf("a", "b"), el.classes)
    }
}
