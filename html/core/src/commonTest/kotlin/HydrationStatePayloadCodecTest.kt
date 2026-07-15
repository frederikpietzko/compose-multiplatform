/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HydrationStatePayloadCodecTest {
    @Test
    fun encodesVersionedPayloadWithSortedKeysAndRoundTripsValues() {
        val values = linkedMapOf(
            "z" to JsonPrimitive("last"),
            "nullable" to JsonNull,
            "items" to buildJsonArray {
                add(JsonPrimitive(2))
                add(JsonPrimitive(1))
            },
            "a" to JsonPrimitive(1)
        )

        val payload = HydrationStatePayloadCodec.encode(values)

        assertEquals(
            """{"version":1,"values":{"a":1,"items":[2,1],"nullable":null,"z":"last"}}""",
            payload
        )
        assertEquals(values, HydrationStatePayloadCodec.decode(payload))
    }

    @Test
    fun rejectsMalformedPayload() {
        val failure = assertFailsWith<HydrationStateException> {
            HydrationStatePayloadCodec.decode("not-json")
        }
        assertContains(failure.message.orEmpty(), "Failed to parse hydration state JSON")
    }

    @Test
    fun rejectsInvalidEnvelopeShapes() {
        assertContains(
            assertFailsWith<HydrationStateException> {
                HydrationStatePayloadCodec.decode("[]")
            }.message.orEmpty(),
            "must be a JSON object"
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                HydrationStatePayloadCodec.decode("""{"values":{}}""")
            }.message.orEmpty(),
            "missing or invalid version"
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                HydrationStatePayloadCodec.decode("""{"version":"1","values":{}}""")
            }.message.orEmpty(),
            "missing or invalid version"
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                HydrationStatePayloadCodec.decode("""{"version":2,"values":{}}""")
            }.message.orEmpty(),
            "Unsupported hydration state payload version: 2"
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                HydrationStatePayloadCodec.decode("""{"version":1}""")
            }.message.orEmpty(),
            "must contain an object field named \"values\""
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                HydrationStatePayloadCodec.decode("""{"version":1,"values":[]}""")
            }.message.orEmpty(),
            "must contain an object field named \"values\""
        )
    }
}
