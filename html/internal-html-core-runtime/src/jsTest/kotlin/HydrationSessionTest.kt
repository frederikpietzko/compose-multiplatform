/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import kotlinx.browser.document
import org.jetbrains.compose.web.HydrationMismatchException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class HydrationSessionTest {
    @Test
    fun claimsNestedElementsAndTextByPosition() {
        val root = document.createElement("div")
        root.innerHTML = "<section><span>Hello &amp; goodbye</span></section>"
        val session = HydrationSession(root)

        val section = session.claimElement("section")
        session.enter(section)
        val span = session.claimElement("span")
        session.enter(span)
        val text = session.claimText("Hello & goodbye")
        session.enter(text)
        session.leave(text)
        session.leave(span)
        session.leave(section)
        session.finish()

        assertSame(root.firstChild, section)
        assertSame(section.firstChild, span)
        assertSame(span.firstChild, text)
    }

    @Test
    fun reportsWrongTagWithPath() {
        val root = document.createElement("div")
        root.innerHTML = "<p>wrong</p>"
        val error = assertFailsWith<HydrationMismatchException> {
            HydrationSession(root).claimElement("span")
        }
        assertContains(error.message.orEmpty(), "root/span[0]")
        assertContains(error.message.orEmpty(), "expected <span>, found <p>")
    }

    @Test
    fun reportsExtraChildOnLeave() {
        val root = document.createElement("div")
        root.innerHTML = "<section><span></span></section>"
        val session = HydrationSession(root)
        val section = session.claimElement("section")
        session.enter(section)

        val error = assertFailsWith<HydrationMismatchException> {
            session.leave(section)
        }
        assertContains(error.message.orEmpty(), "found extra <span>")
    }

    @Test
    fun rejectsNestedSessionsAndRestoresActiveState() {
        val first = HydrationSession(document.createElement("div"))
        val second = HydrationSession(document.createElement("div"))

        withHydrationSession(first) {
            assertFailsWith<IllegalStateException> {
                withHydrationSession(second) { Unit }
            }
        }

        withHydrationSession(second) { Unit }
    }
}
