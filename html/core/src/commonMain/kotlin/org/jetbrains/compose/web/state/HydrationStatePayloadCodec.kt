/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal object HydrationStatePayloadCodec {
    private const val CurrentVersion = 1

    fun encode(values: Map<String, JsonElement>): String {
        val envelope = buildJsonObject {
            put("version", CurrentVersion)
            val sortedValues = values.entries
                .sortedBy { it.key }
                .associate { it.toPair() }
            put("values", JsonObject(sortedValues))
        }
        return Json.encodeToString(JsonObject.serializer(), envelope)
    }

    fun decode(payload: String): Map<String, JsonElement> {
        val parsed = try {
            Json.parseToJsonElement(payload)
        } catch (failure: Throwable) {
            throw HydrationStateException(
                "Failed to parse hydration state JSON",
                failure
            )
        }

        val envelope = when (parsed) {
            is JsonObject -> parsed
            is JsonPrimitive -> throw HydrationStateException(
                "Failed to parse hydration state JSON"
            )
            else -> throw HydrationStateException(
                "Hydration state payload must be a JSON object"
            )
        }
        val versionPrimitive = envelope["version"] as? JsonPrimitive
        val version = versionPrimitive
            ?.takeUnless { it.isString }
            ?.intOrNull
            ?: throw HydrationStateException(
                "Hydration state payload has a missing or invalid version"
            )
        if (version != CurrentVersion) {
            throw HydrationStateException(
                "Unsupported hydration state payload version: $version"
            )
        }

        val values = envelope["values"] as? JsonObject
            ?: throw HydrationStateException(
                "Hydration state payload must contain an object field named \"values\""
            )
        return values.toMap()
    }
}
