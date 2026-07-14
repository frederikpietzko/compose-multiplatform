/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.core.tests.hydration.HydrationCounter
import org.jetbrains.compose.web.renderToString
import kotlin.test.Test
import kotlin.test.assertEquals

class HydrationFixtureJvmTest {
    @Test
    fun counterProducesHydrationFixture() {
        assertEquals(
            "<div class=\"idle\" style=\"color:red;\"><button>Count: 0</button></div>",
            renderToString { HydrationCounter() }
        )
    }
}
