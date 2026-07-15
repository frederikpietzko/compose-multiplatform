# Compose HTML Hydration State Serialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serialize explicitly keyed Compose state with JVM-rendered HTML and restore it before browser hydration's first DOM match.

**Architecture:** A common, DOM-free hydration-state registry is installed through a `CompositionLocal` only by new state-aware SSR and hydration entry points. `rememberHydratableState` registers serializer-backed mutable state with that registry; JVM SSR snapshots HTML and state after the same settling frame, while JS restores and verifies the exact key set immediately before positional hydration commits.

**Tech Stack:** Kotlin Multiplatform 2.3.20, Compose Runtime 1.10.1, kotlinx.serialization JSON 1.11.0, Kotlin/JVM, Kotlin/JS browser tests, Gradle.

## Global Constraints

- All Gradle commands run from `/Users/frederik.pietzko/Workspace/compose-multiplatform/html` using this build's `./gradlew`.
- Keep Compose Runtime at `1.10.1` and Kotlin at `2.3.20`.
- Add `org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0` as a common `api` dependency because `KSerializer<T>` appears in public API.
- Do not add `org.jetbrains.kotlin.plugin.serialization` to `:html-core`; core declares no generated `@Serializable` classes. Downstream applications apply Kotlin's matching `2.3.20` compiler plugin for their own generated serializers.
- Existing `renderToString`, `hydrateComposable`, and `renderComposable` signatures and behavior remain compatible.
- State serialization is opt-in through `rememberHydratableState`; arbitrary `remember` and slot-table state remain untouched.
- Keys are nonblank, page-global, and unique while their remembered entries are active.
- State-aware hydration requires exact server/client initial key equality.
- State JSON is raw transport data. Do not add script tags, DOM discovery, HTML escaping, CSP policy, HTTP integration, or framework conventions.
- Do not modify `html/examples/hydration-spring`.
- Preserve current positional hydration cleanup: state failures before commit abort listeners, preserve the SSR root on a best-effort basis, dispose partial composition, and cancel the recomposer.
- Every new Kotlin file uses the repository's 2020-2026 Apache license header.

---

## File Structure

### New production files

- `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateException.kt` — public state-payload exception.
- `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStatePayloadCodec.kt` — internal versioned JSON envelope codec.
- `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateRegistry.kt` — internal saving/restoring/live registry and remembered-entry lifecycle.
- `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/RememberHydratableState.kt` — public composable API and internal `CompositionLocal`.
- `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/HydrateComposableWithState.kt` — state-aware JS hydration overload.

### Modified production files

- `html/settings.gradle.kts` — serialization JSON catalog entry.
- `html/core/build.gradle.kts` — common serialization JSON API dependency.
- `html/core/src/jvmMain/kotlin/org/jetbrains/compose/web/renderToString.kt` — shared one-shot renderer, `SsrResult`, and `renderToStringWithState`.
- `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/renderComposable.kt` — internal pre-commit callback helper used by `:html-core`.

### New and modified tests

- `html/core/src/commonTest/kotlin/HydrationStatePayloadCodecTest.kt`
- `html/core/src/commonTest/kotlin/HydrationStateRegistryTest.kt`
- `html/core/src/commonTest/kotlin/HydrationComposables.kt` — add shared keyed counter without changing existing fixture.
- `html/core/src/jvmTest/kotlin/HydrationStateSsrJvmTest.kt`
- `html/core/src/jsTest/kotlin/HydrationStateJsTest.kt`

### Documentation

- `html/core/HYDRATION.md` — state-aware flow and revised limitations.
- `html/core/JVM_SSR.md` — state-aware SSR result and transport-neutral boundary.

---

### Task 1: Add serialization dependency and versioned payload codec

**Files:**
- Modify: `html/settings.gradle.kts:64-67`
- Modify: `html/core/build.gradle.kts:24-29`
- Create: `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateException.kt`
- Create: `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStatePayloadCodec.kt`
- Test: `html/core/src/commonTest/kotlin/HydrationStatePayloadCodecTest.kt`

**Interfaces:**
- Produces: `HydrationStateException(message: String, cause: Throwable? = null)`.
- Produces: `HydrationStatePayloadCodec.encode(values: Map<String, JsonElement>): String`.
- Produces: `HydrationStatePayloadCodec.decode(payload: String): Map<String, JsonElement>`.
- Payload contract: `{"version":1,"values":{...}}`, with lexicographically sorted state keys.

- [ ] **Step 1: Add kotlinx.serialization JSON to the HTML version catalog and core API**

In `html/settings.gradle.kts`, add this line below the coroutines library:

```kotlin
library(
    "kotlinx-serialization-json",
    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0"
)
```

In `html/core/build.gradle.kts`, add the API dependency inside `commonMain.dependencies`:

```kotlin
api(libs.kotlinx.serialization.json)
```

Do not add a serialization compiler plugin.

- [ ] **Step 2: Verify dependency resolution on common, JVM, and JS targets**

Run:

```bash
./gradlew :html-core:compileKotlinMetadata \
  :html-core:compileKotlinJvm \
  :html-core:compileKotlinJs
```

Expected: `BUILD SUCCESSFUL`; Gradle resolves `kotlinx-serialization-json:1.11.0` for common metadata, JVM, and JS.

- [ ] **Step 3: Write failing payload-codec tests**

Create `html/core/src/commonTest/kotlin/HydrationStatePayloadCodecTest.kt`:

```kotlin
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
```

- [ ] **Step 4: Run the focused JVM test and confirm failure**

Run:

```bash
./gradlew :html-core:jvmTest \
  --tests 'org.jetbrains.compose.web.state.HydrationStatePayloadCodecTest'
```

Expected: compilation fails because `HydrationStateException` and `HydrationStatePayloadCodec` do not exist.

- [ ] **Step 5: Implement the public exception**

Create `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateException.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

/** Thrown when serialized hydration state is invalid or cannot be encoded or restored. */
class HydrationStateException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)
```

- [ ] **Step 6: Implement deterministic envelope encoding and strict decoding**

Create `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStatePayloadCodec.kt`:

```kotlin
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
            put("values", JsonObject(values.toSortedMap()))
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

        val envelope = parsed as? JsonObject
            ?: throw HydrationStateException(
                "Hydration state payload must be a JSON object"
            )
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
```

- [ ] **Step 7: Run codec tests on JVM and JS**

Run:

```bash
./gradlew :html-core:jvmTest \
  --tests 'org.jetbrains.compose.web.state.HydrationStatePayloadCodecTest'
./gradlew :html-core:jsBrowserTest
```

Expected: JVM focused test passes; complete JS browser tests pass, including common codec tests.

- [ ] **Step 8: Commit dependency and codec**

```bash
git add html/settings.gradle.kts \
  html/core/build.gradle.kts \
  html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateException.kt \
  html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStatePayloadCodec.kt \
  html/core/src/commonTest/kotlin/HydrationStatePayloadCodecTest.kt
git commit -m "feat(html): add hydration state payload codec"
```

---

### Task 2: Implement registry lifecycle and `rememberHydratableState`

**Files:**
- Create: `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateRegistry.kt`
- Create: `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/RememberHydratableState.kt`
- Test: `html/core/src/commonTest/kotlin/HydrationStateRegistryTest.kt`

**Interfaces:**
- Consumes: `HydrationStateException` and JSON runtime from Task 1.
- Produces: `HydrationStateRegistry.saving()` and `.restoring(values)`.
- Produces: `snapshotValues`, `verifyRestorationComplete`, and `commitRestoration`.
- Produces: internal `LocalHydrationStateRegistry`.
- Produces public inferred and explicit `rememberHydratableState` overloads returning `MutableState<T>`.

- [ ] **Step 1: Write failing registry tests**

Create `html/core/src/commonTest/kotlin/HydrationStateRegistryTest.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class HydrationStateRegistryTest {
    @Test
    fun savingRegistrySnapshotsCurrentValuesAndReleasesForgottenEntries() {
        val registry = HydrationStateRegistry.saving()
        val entry = registry.createEntry("counter", Int.serializer()) { 1 }
        entry.onRemembered()
        entry.state.value = 7

        assertEquals(
            mapOf("counter" to JsonPrimitive(7)),
            registry.snapshotValues()
        )

        entry.onForgotten()
        assertEquals(emptyMap(), registry.snapshotValues())
    }

    @Test
    fun rejectsBlankAndDuplicateActiveKeys() {
        val registry = HydrationStateRegistry.saving()
        assertContains(
            assertFailsWith<HydrationStateException> {
                registry.createEntry("  ", Int.serializer()) { 1 }
            }.message.orEmpty(),
            "must not be blank"
        )

        val first = registry.createEntry("same", Int.serializer()) { 1 }
        val second = registry.createEntry("same", Int.serializer()) { 2 }
        first.onRemembered()
        assertContains(
            assertFailsWith<HydrationStateException> {
                second.onRemembered()
            }.message.orEmpty(),
            "Duplicate hydration state key \"same\""
        )
    }

    @Test
    fun restoresValueWithoutCallingInitializer() {
        val registry = HydrationStateRegistry.restoring(
            mapOf("counter" to JsonPrimitive(7))
        )
        var initializerCalled = false
        val entry = registry.createEntry("counter", Int.serializer()) {
            initializerCalled = true
            0
        }
        entry.onRemembered()

        assertEquals(7, entry.state.value)
        assertFalse(initializerCalled)
        registry.verifyRestorationComplete()
        registry.commitRestoration()
    }

    @Test
    fun distinguishesJsonNullFromMissingKey() {
        val registry = HydrationStateRegistry.restoring(
            mapOf("label" to JsonNull)
        )
        val entry = registry.createEntry("label", String.serializer().nullable) {
            "fallback"
        }
        entry.onRemembered()

        assertEquals(null, entry.state.value)
        registry.verifyRestorationComplete()
    }

    @Test
    fun rejectsMissingAndUnconsumedRestoredKeys() {
        var initializerCalled = false
        val missing = HydrationStateRegistry.restoring(emptyMap())
        assertContains(
            assertFailsWith<HydrationStateException> {
                missing.createEntry("counter", Int.serializer()) {
                    initializerCalled = true
                    0
                }
            }.message.orEmpty(),
            "Missing hydration state for key \"counter\""
        )
        assertFalse(initializerCalled)

        val extra = HydrationStateRegistry.restoring(
            mapOf("unused" to JsonPrimitive(true))
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                extra.verifyRestorationComplete()
            }.message.orEmpty(),
            "Unconsumed hydration state keys: unused"
        )
    }

    @Test
    fun initializesNewEntriesAfterRestorationCommits() {
        val registry = HydrationStateRegistry.restoring(
            mapOf("counter" to JsonPrimitive(7))
        )
        val restored = registry.createEntry("counter", Int.serializer()) { 0 }
        restored.onRemembered()
        registry.verifyRestorationComplete()
        registry.commitRestoration()
        restored.onForgotten()

        val later = registry.createEntry("later", Int.serializer()) { 9 }
        later.onRemembered()
        assertEquals(9, later.state.value)
    }

    @Test
    fun wrapsSerializerFailuresWithKeyAndOperation() {
        val saving = HydrationStateRegistry.saving()
        val savingEntry = saving.createEntry("broken", ThrowingIntSerializer) { 1 }
        savingEntry.onRemembered()
        assertContains(
            assertFailsWith<HydrationStateException> {
                saving.snapshotValues()
            }.message.orEmpty(),
            "Failed to serialize hydration state \"broken\""
        )

        val restoring = HydrationStateRegistry.restoring(
            mapOf("broken" to JsonPrimitive(1))
        )
        assertContains(
            assertFailsWith<HydrationStateException> {
                restoring.createEntry("broken", ThrowingIntSerializer) { 0 }
            }.message.orEmpty(),
            "Failed to restore hydration state \"broken\""
        )
    }
}

private object ThrowingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ThrowingInt", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        error("encode failed")
    }

    override fun deserialize(decoder: Decoder): Int {
        error("decode failed")
    }
}
```

- [ ] **Step 2: Run focused registry tests and confirm failure**

Run:

```bash
./gradlew :html-core:jvmTest \
  --tests 'org.jetbrains.compose.web.state.HydrationStateRegistryTest'
```

Expected: compilation fails because `HydrationStateRegistry` does not exist.

- [ ] **Step 3: Implement saving/restoring/live registry and remembered-entry lifecycle**

Create `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateRegistry.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class HydrationStateRegistry private constructor(
    private var phase: Phase,
    restoredValues: Map<String, JsonElement>
) {
    private enum class Phase {
        Saving,
        Restoring,
        Live
    }

    private val restored = restoredValues.toMutableMap()
    private val claimedRestoredKeys = mutableSetOf<String>()
    private val activeEntries = mutableMapOf<String, HydrationStateEntry<*>>()

    companion object {
        fun saving(): HydrationStateRegistry =
            HydrationStateRegistry(Phase.Saving, emptyMap())

        fun restoring(values: Map<String, JsonElement>): HydrationStateRegistry =
            HydrationStateRegistry(Phase.Restoring, values)
    }

    fun <T> createEntry(
        key: String,
        serializer: KSerializer<T>,
        initial: () -> T
    ): HydrationStateEntry<T> {
        if (key.isBlank()) {
            throw HydrationStateException("Hydration state key must not be blank")
        }

        var restoredElement: JsonElement? = null
        val value = if (phase == Phase.Restoring) {
            if (!claimedRestoredKeys.add(key)) {
                throw duplicateKey(key)
            }
            if (!restored.containsKey(key)) {
                claimedRestoredKeys.remove(key)
                throw HydrationStateException(
                    "Missing hydration state for key \"$key\""
                )
            }
            restored.remove(key)!!.also { restoredElement = it }.let { element ->
                try {
                    Json.decodeFromJsonElement(serializer, element)
                } catch (failure: Throwable) {
                    restored[key] = element
                    claimedRestoredKeys.remove(key)
                    throw HydrationStateException(
                        "Failed to restore hydration state \"$key\"",
                        failure
                    )
                }
            }
        } else {
            initial()
        }

        return HydrationStateEntry(
            key = key,
            serializer = serializer,
            state = mutableStateOf(value),
            restoredElement = restoredElement,
            registry = this
        )
    }

    fun snapshotValues(): Map<String, JsonElement> {
        check(phase == Phase.Saving) {
            "Hydration state can only be snapshotted by a saving registry"
        }
        return activeEntries.mapValues { (_, entry) -> entry.encode() }
    }

    fun verifyRestorationComplete() {
        check(phase == Phase.Restoring) {
            "Hydration state restoration is not active"
        }
        if (restored.isNotEmpty()) {
            throw HydrationStateException(
                "Unconsumed hydration state keys: ${restored.keys.sorted().joinToString()}"
            )
        }
    }

    fun commitRestoration() {
        check(phase == Phase.Restoring) {
            "Hydration state restoration is not active"
        }
        phase = Phase.Live
        claimedRestoredKeys.clear()
    }

    fun activate(entry: HydrationStateEntry<*>) {
        val existing = activeEntries[entry.key]
        if (existing != null && existing !== entry) {
            throw duplicateKey(entry.key)
        }
        activeEntries[entry.key] = entry
    }

    fun release(entry: HydrationStateEntry<*>) {
        if (activeEntries[entry.key] === entry) {
            activeEntries.remove(entry.key)
        }
        val restoredElement = entry.restoredElement
        if (
            phase == Phase.Restoring &&
            restoredElement != null &&
            claimedRestoredKeys.remove(entry.key)
        ) {
            restored[entry.key] = restoredElement
        }
    }

    private fun duplicateKey(key: String) = HydrationStateException(
        "Duplicate hydration state key \"$key\""
    )
}

internal class HydrationStateEntry<T>(
    val key: String,
    private val serializer: KSerializer<T>,
    val state: MutableState<T>,
    val restoredElement: JsonElement?,
    private val registry: HydrationStateRegistry
) : RememberObserver {
    override fun onRemembered() {
        registry.activate(this)
    }

    override fun onForgotten() {
        registry.release(this)
    }

    override fun onAbandoned() {
        registry.release(this)
    }

    fun encode(): JsonElement = try {
        Json.encodeToJsonElement(serializer, state.value)
    } catch (failure: Throwable) {
        throw HydrationStateException(
            "Failed to serialize hydration state \"$key\"",
            failure
        )
    }
}
```

- [ ] **Step 4: Implement public composable APIs and registry-free fallback**

Create `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/RememberHydratableState.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

internal val LocalHydrationStateRegistry =
    staticCompositionLocalOf<HydrationStateRegistry?> { null }

/**
 * Remembers mutable state that participates in JVM SSR-to-browser hydration when a
 * state-aware renderer or hydrator supplies a registry.
 */
@Composable
inline fun <reified T> rememberHydratableState(
    key: String,
    noinline initial: () -> T
): MutableState<T> = rememberHydratableState(
    key = key,
    serializer = serializer<T>(),
    initial = initial
)

/**
 * Remembers mutable state using [serializer] for JVM SSR-to-browser hydration.
 * [key] must be unique across the active rendered root.
 */
@Composable
fun <T> rememberHydratableState(
    key: String,
    serializer: KSerializer<T>,
    initial: () -> T
): MutableState<T> {
    val registry = LocalHydrationStateRegistry.current
    if (registry == null) {
        return remember { mutableStateOf(initial()) }
    }

    val entry = remember(registry, key) {
        registry.createEntry(key, serializer, initial)
    }
    return entry.state
}
```

- [ ] **Step 5: Run registry tests and common target compilation**

Run:

```bash
./gradlew :html-core:jvmTest \
  --tests 'org.jetbrains.compose.web.state.HydrationStateRegistryTest'
./gradlew :html-core:compileKotlinMetadata \
  :html-core:compileKotlinJvm \
  :html-core:compileKotlinJs
```

Expected: registry tests pass; inferred and explicit public APIs compile for metadata, JVM, and JS.

- [ ] **Step 6: Run JS common tests**

Run:

```bash
./gradlew :html-core:jsBrowserTest
```

Expected: all existing JS browser tests and new common registry tests pass.

- [ ] **Step 7: Commit registry and composable API**

```bash
git add html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/HydrationStateRegistry.kt \
  html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state/RememberHydratableState.kt \
  html/core/src/commonTest/kotlin/HydrationStateRegistryTest.kt
git commit -m "feat(html): add hydratable state registry"
```

---

### Task 3: Capture matching HTML and state during JVM SSR

**Files:**
- Modify: `html/core/src/commonTest/kotlin/HydrationComposables.kt`
- Modify: `html/core/src/jvmMain/kotlin/org/jetbrains/compose/web/renderToString.kt`
- Create: `html/core/src/jvmTest/kotlin/HydrationStateSsrJvmTest.kt`

**Interfaces:**
- Consumes: saving registry, payload codec, `LocalHydrationStateRegistry`, and `rememberHydratableState`.
- Produces: `SsrResult(html: String, stateJson: String)`.
- Produces: `renderToStringWithState(content): SsrResult`.
- Preserves: `renderToString(content): String` with no installed registry.

- [ ] **Step 1: Add a shared hydratable counter fixture**

Append this function to `html/core/src/commonTest/kotlin/HydrationComposables.kt`, leaving `HydrationCounter` unchanged:

```kotlin
import org.jetbrains.compose.web.state.rememberHydratableState

@Composable
fun HydratableCounter(
    initialCount: Int,
    onInitialize: () -> Unit = {}
) {
    var count by rememberHydratableState("counter") {
        onInitialize()
        initialCount
    }
    Button(attrs = { onClick { count++ } }) {
        Text("Count: $count")
    }
}
```

Place the new import with existing imports, not below declarations.

- [ ] **Step 2: Write failing JVM state-aware SSR tests**

Create `html/core/src/jvmTest/kotlin/HydrationStateSsrJvmTest.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.compose.web.core.tests.hydration.HydratableCounter
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.renderToStringWithState
import org.jetbrains.compose.web.state.rememberHydratableState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HydrationStateSsrJvmTest {
    @Test
    fun returnsMatchingHtmlAndStateJson() {
        val result = renderToStringWithState {
            HydratableCounter(initialCount = 7)
        }

        assertEquals("<button>Count: 7</button>", result.html)
        assertEquals(
            """{"version":1,"values":{"counter":7}}""",
            result.stateJson
        )
    }

    @Test
    fun capturesFinalStateAndActiveProvidersAfterSettling() {
        val result = renderToStringWithState {
            var count by rememberHydratableState("counter") { 0 }
            SideEffect {
                if (count == 0) count = 1
            }
            Div { Text("Count: $count") }
        }

        assertEquals("<div>Count: 1</div>", result.html)
        assertEquals(
            """{"version":1,"values":{"counter":1}}""",
            result.stateJson
        )
    }

    @Test
    fun ordinaryRendererUsesRegistryFreeFallback() {
        var initializerCalls = 0
        val html = renderToString {
            HydratableCounter(initialCount = 3) {
                initializerCalls++
            }
        }

        assertEquals("<button>Count: 3</button>", html)
        assertEquals(1, initializerCalls)
    }

    @Test
    fun supportsInferredNullableAndCollectionSerializers() {
        val result = renderToStringWithState {
            val items by rememberHydratableState("items") { listOf(2, 1) }
            val label by rememberHydratableState<String?>("label") { null }
            Text("${items.joinToString()}:${label ?: "none"}")
        }

        assertEquals("2, 1:none", result.html)
        assertEquals(
            """{"version":1,"values":{"items":[2,1],"label":null}}""",
            result.stateJson
        )
    }

    @Test
    fun supportsExplicitSerializerOverload() {
        val result = renderToStringWithState {
            val count by rememberHydratableState(
                key = "counter",
                serializer = Int.serializer()
            ) { 4 }
            Text("$count")
        }

        assertEquals("4", result.html)
        assertEquals(
            """{"version":1,"values":{"counter":4}}""",
            result.stateJson
        )
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun encodeFailureStillCleansUpRecomposer() {
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        assertFailsWith<IllegalStateException> {
            renderToStringWithState {
                rememberHydratableState(
                    key = "broken",
                    serializer = FailingEncodeSerializer
                ) { 1 }
                Text("content")
            }
        }

        assertEquals(
            recomposersBefore,
            Recomposer.runningRecomposers.value.size
        )
    }
}

private object FailingEncodeSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FailingEncode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        error("encode failed")
    }

    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}
```

- [ ] **Step 3: Run focused JVM tests and confirm failure**

Run:

```bash
./gradlew :html-core:jvmTest \
  --tests 'org.jetbrains.compose.web.core.tests.HydrationStateSsrJvmTest'
```

Expected: compilation fails because `SsrResult` and `renderToStringWithState` do not exist.

- [ ] **Step 4: Refactor the one-shot renderer without changing teardown order**

In `html/core/src/jvmMain/kotlin/org/jetbrains/compose/web/renderToString.kt`:

1. Add imports:

```kotlin
import androidx.compose.runtime.CompositionLocalProvider
import org.jetbrains.compose.web.state.HydrationStatePayloadCodec
import org.jetbrains.compose.web.state.HydrationStateRegistry
import org.jetbrains.compose.web.state.LocalHydrationStateRegistry
```

2. Add public result and state-aware entry point above the private renderer:

```kotlin
data class SsrResult(
    val html: String,
    val stateJson: String
)

fun renderToStringWithState(
    content: @Composable DOMScope<DomElement>.() -> Unit
): SsrResult {
    val registry = HydrationStateRegistry.saving()
    val rendered = renderContent(registry, content)
    return SsrResult(
        html = rendered.html,
        stateJson = checkNotNull(rendered.stateJson)
    )
}
```

3. Replace the current `renderToString` body with delegation:

```kotlin
@OptIn(ComposeWebInternalApi::class)
fun renderToString(
    content: @Composable DOMScope<DomElement>.() -> Unit
): String = renderContent(registry = null, content = content).html
```

4. Move the existing composition body into this helper. Keep the current root, clock, job, recomposer, `ControlledComposition`, placeholder root, frame, and `finally` teardown logic unchanged; only wrap content and collect state before teardown:

```kotlin
private data class RenderedContent(
    val html: String,
    val stateJson: String?
)

@OptIn(ComposeWebInternalApi::class)
private fun renderContent(
    registry: HydrationStateRegistry?,
    content: @Composable DOMScope<DomElement>.() -> Unit
): RenderedContent {
    val root = JvmRootNode()
    val clock = BroadcastFrameClock()
    val job = Job()
    val scope = CoroutineScope(clock + Dispatchers.Unconfined + job)
    val recomposer = Recomposer(scope.coroutineContext)

    scope.launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = ControlledComposition(
        applier = DomApplier(NativeNodeWrapper(root)),
        parent = recomposer
    )
    val placeholderRootElement = object : DomElement("") {}
    val domScope = object : DOMScope<DomElement> {
        override val DisposableEffectScope.scopeElement: DomElement
            get() = placeholderRootElement
    }

    return try {
        composition.setContent {
            if (registry == null) {
                domScope.content()
            } else {
                CompositionLocalProvider(
                    LocalHydrationStateRegistry provides registry
                ) {
                    domScope.content()
                }
            }
        }
        clock.sendFrame(0L)

        RenderedContent(
            html = serializeToHtml(root),
            stateJson = registry?.let {
                HydrationStatePayloadCodec.encode(it.snapshotValues())
            }
        )
    } finally {
        composition.dispose()
        recomposer.cancel()
        job.cancel()
    }
}
```

Retain and update existing KDoc so both public functions document one settling frame, same-point HTML/state capture, and deterministic teardown. Do not duplicate the old renderer body.

- [ ] **Step 5: Run focused JVM tests and existing SSR regressions**

Run:

```bash
./gradlew :html-core:jvmTest \
  --tests 'org.jetbrains.compose.web.core.tests.HydrationStateSsrJvmTest'
./gradlew :html-core:jvmTest \
  --tests 'RenderToStringTest'
```

Expected: both commands pass. Stateful result contains value `7`; ordinary renderer output and recomposer cleanup remain unchanged.

- [ ] **Step 6: Compile JS and common metadata after renderer refactor**

Run:

```bash
./gradlew :html-core:compileKotlinMetadata \
  :html-core:compileKotlinJs \
  :html-core:jsBrowserTest
```

Expected: metadata and JS compile; all browser tests pass.

- [ ] **Step 7: Commit JVM state capture**

```bash
git add html/core/src/commonTest/kotlin/HydrationComposables.kt \
  html/core/src/jvmMain/kotlin/org/jetbrains/compose/web/renderToString.kt \
  html/core/src/jvmTest/kotlin/HydrationStateSsrJvmTest.kt
git commit -m "feat(html): capture SSR hydration state"
```

---

### Task 4: Restore state before JS hydration commit

**Files:**
- Modify: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/renderComposable.kt`
- Create: `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/HydrateComposableWithState.kt`
- Test: `html/core/src/jsTest/kotlin/HydrationStateJsTest.kt`

**Interfaces:**
- Consumes: restoring registry and payload codec from Tasks 1-2.
- Produces internal runtime helper `hydrateComposableWithPreCommit(root, monotonicFrameClock, beforeHydrationCommit, content)`.
- Produces public state-aware `hydrateComposable(root, stateJson, monotonicFrameClock, content)`.
- Verification order: parse payload before hydration; consume state during initial composition; verify state keys; finish DOM matching; switch registry to live mode.

- [ ] **Step 1: Write failing golden-path and cleanup browser tests**

Create `html/core/src/jsTest/kotlin/HydrationStateJsTest.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.Recomposer
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.core.tests.hydration.HydratableCounter
import org.jetbrains.compose.web.dom.DomText
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.state.HydrationStateException
import org.jetbrains.compose.web.state.rememberHydratableState
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class HydrationStateJsTest {
    @Test
    fun restoresBeforeMatchingThenUsesNormalRecomposition() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild as HTMLElement
        val text = button.firstChild as DomText
        var initializerCalled = false

        val composition = hydrateComposable(root, CounterState) {
            HydratableCounter(initialCount = 0) {
                initializerCalled = true
            }
        }
        try {
            assertFalse(initializerCalled)
            assertSame(button, root.firstChild)
            assertSame(text, button.firstChild)
            assertEquals("Count: 7", text.data)

            button.dispatchEvent(MouseEvent("click"))
            delay(100)

            assertSame(button, root.firstChild)
            assertSame(text, button.firstChild)
            assertEquals("Count: 8", text.data)
        } finally {
            composition.dispose()
        }
    }

    @Test
    fun missingStateDoesNotRunInitializerOrChangeRoot() {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild
        var initializerCalled = false

        val failure = assertFailsWith<HydrationStateException> {
            hydrateComposable(root, EmptyState) {
                HydratableCounter(initialCount = 0) {
                    initializerCalled = true
                }
            }
        }

        assertContains(failure.message.orEmpty(), "Missing hydration state for key \"counter\"")
        assertFalse(initializerCalled)
        assertSame(button, root.firstChild)
        assertEquals("<button>Count: 7</button>", root.innerHTML)
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun unconsumedStateAbortsListenersPreservesRootAndAllowsRetry() = MainScope().promise {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild as HTMLElement
        val text = button.firstChild as DomText
        val recomposersBefore = Recomposer.runningRecomposers.value.size

        val failure = assertFailsWith<HydrationStateException> {
            hydrateComposable(root, StateWithExtraKey) {
                HydratableCounter(initialCount = 0)
            }
        }

        assertContains(failure.message.orEmpty(), "Unconsumed hydration state keys: unused")
        assertSame(button, root.firstChild)
        assertSame(text, button.firstChild)
        assertEquals("<button>Count: 7</button>", root.innerHTML)

        button.dispatchEvent(MouseEvent("click"))
        delay(1)
        assertEquals("Count: 7", text.data)
        assertEquals(recomposersBefore, Recomposer.runningRecomposers.value.size)

        val retry = hydrateComposable(root, CounterState) {
            HydratableCounter(initialCount = 0)
        }
        retry.dispose()
    }

    @Test
    fun malformedPayloadFailsBeforeHydrationStarts() {
        val root = document.createElement("div")
        root.innerHTML = "<button>Count: 7</button>"
        val button = root.firstChild

        assertFailsWith<HydrationStateException> {
            hydrateComposable(root, "not-json") {
                HydratableCounter(initialCount = 0)
            }
        }

        assertSame(button, root.firstChild)
        assertEquals("<button>Count: 7</button>", root.innerHTML)
    }

    @Test
    fun rejectsDuplicateClientKeyAndWrongSerializedType() {
        val duplicateRoot = document.createElement("div")
        val duplicate = assertFailsWith<HydrationStateException> {
            hydrateComposable(
                duplicateRoot,
                """{"version":1,"values":{"same":1}}"""
            ) {
                rememberHydratableState("same") { 0 }
                rememberHydratableState("same") { 0 }
            }
        }
        assertContains(duplicate.message.orEmpty(), "Duplicate hydration state key \"same\"")

        val wrongTypeRoot = document.createElement("div")
        wrongTypeRoot.innerHTML = "<button>Count: 7</button>"
        val wrongType = assertFailsWith<HydrationStateException> {
            hydrateComposable(
                wrongTypeRoot,
                """{"version":1,"values":{"counter":"seven"}}"""
            ) {
                HydratableCounter(initialCount = 0)
            }
        }
        assertContains(wrongType.message.orEmpty(), "Failed to restore hydration state \"counter\"")
    }
}

private const val CounterState =
    """{"version":1,"values":{"counter":7}}"""
private const val EmptyState =
    """{"version":1,"values":{}}"""
private const val StateWithExtraKey =
    """{"version":1,"values":{"counter":7,"unused":true}}"""
```

- [ ] **Step 2: Run focused browser tests and confirm failure**

Run:

```bash
./gradlew :html-core:jsBrowserTest \
  --tests 'org.jetbrains.compose.web.core.tests.HydrationStateJsTest'
```

Expected: compilation fails because no `hydrateComposable(root, stateJson, ...)` overload exists.

- [ ] **Step 3: Expose an internal pre-commit hydration helper without changing public behavior**

In `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/renderComposable.kt`:

1. Replace the current generic public `hydrateComposable(root, clock, content)` body with delegation:

```kotlin
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition = hydrateComposableWithPreCommit(
    root = root,
    monotonicFrameClock = monotonicFrameClock,
    beforeHydrationCommit = {},
    content = content
)
```

2. Add this cross-module internal helper immediately after it:

```kotlin
@ComposeWebInternalApi
fun <TElement : Element> hydrateComposableWithPreCommit(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    beforeHydrationCommit: () -> Unit,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()
    val session = HydrationSession(root)
    val strategy = HydrationDomApplierStrategy(session)

    return withHydrationSession(session) {
        renderComposableRoot(
            root = root,
            monotonicFrameClock = monotonicFrameClock,
            dispatcher = JsMicrotasksDispatcher(),
            strategy = strategy,
            onInitialCompositionApplied = {
                beforeHydrationCommit()
                strategy.finish()
            },
            onCompositionFailed = {
                try {
                    session.abort()
                } finally {
                    deactivateHydrationSession(session)
                }
            },
            content = content
        )
    }
}
```

Keep root-ID overload unchanged. Preserve KDoc on public API and add internal KDoc stating that `beforeHydrationCommit` runs after initial composition but before DOM completeness commits, and any exception uses normal hydration abort cleanup.

- [ ] **Step 4: Implement state-aware JS overload in `:html-core`**

Create `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/HydrateComposableWithState.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.state.HydrationStatePayloadCodec
import org.jetbrains.compose.web.state.HydrationStateRegistry
import org.jetbrains.compose.web.state.LocalHydrationStateRegistry
import org.w3c.dom.Element

/**
 * Hydrates [root] after restoring explicitly registered state from raw [stateJson].
 * Payload transport and HTML embedding are caller responsibilities.
 */
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> hydrateComposable(
    root: TElement,
    stateJson: String,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    val registry = HydrationStateRegistry.restoring(
        HydrationStatePayloadCodec.decode(stateJson)
    )
    val composition = hydrateComposableWithPreCommit(
        root = root,
        monotonicFrameClock = monotonicFrameClock,
        beforeHydrationCommit = registry::verifyRestorationComplete,
        content = {
            val hydrationScope = this
            CompositionLocalProvider(
                LocalHydrationStateRegistry provides registry
            ) {
                content(hydrationScope)
            }
        }
    )
    registry.commitRestoration()
    return composition
}
```

Payload decoding occurs before `hydrateComposableWithPreCommit`, so malformed envelopes cannot create a hydration session or mutate DOM. `commitRestoration` runs only after `strategy.finish()` succeeds.

- [ ] **Step 5: Run focused browser tests**

Run:

```bash
./gradlew :html-core:jsBrowserTest \
  --tests 'org.jetbrains.compose.web.core.tests.HydrationStateJsTest'
```

Expected: all five state hydration tests pass. Restored value `7` matches existing text without calling browser initializer; click updates same node to `8`; extra-key failure removes listener and leaks no recomposer.

- [ ] **Step 6: Run runtime and stateless hydration regressions**

Run:

```bash
./gradlew :internal-html-core-runtime:jsBrowserTest \
  :internal-html-core-runtime:jvmTest
./gradlew :html-core:jsBrowserTest \
  :html-core:jvmTest
```

Expected: all runtime/core tests pass. Existing stateless overloads, root-ID hydration, mismatch cleanup, listener cleanup, and JVM SSR remain green.

- [ ] **Step 7: Commit JS restoration and pre-commit seam**

```bash
git add html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/renderComposable.kt \
  html/core/src/jsMain/kotlin/org/jetbrains/compose/web/HydrateComposableWithState.kt \
  html/core/src/jsTest/kotlin/HydrationStateJsTest.kt
git commit -m "feat(html): restore state before hydration"
```

---

### Task 5: Document APIs and run full compatibility verification

**Files:**
- Modify: `html/core/HYDRATION.md`
- Modify: `html/core/JVM_SSR.md`
- Verify: all files from Tasks 1-4

**Interfaces:**
- Documents: explicit state registration, `SsrResult`, raw JSON boundary, exact-key errors, and registry-free behavior.
- Preserves: no framework or transport prescription.

- [ ] **Step 1: Add transport-neutral state usage to hydration guide**

In `html/core/HYDRATION.md`, add a section after the existing basic JVM/JS usage:

~~~~markdown
## Explicit hydration state

State created with `remember` still initializes independently on JVM and JS. For values that must
cross the SSR boundary, register mutable state with a stable page-global key:

```kotlin
import org.jetbrains.compose.web.state.rememberHydratableState

@Composable
fun Counter(initialCount: Int) {
    var count by rememberHydratableState("counter") { initialCount }
    Button({ onClick { count++ } }) {
        Text("Count: $count")
    }
}
```

The JVM state-aware renderer returns markup and raw JSON:

```kotlin
val result = renderToStringWithState {
    Counter(initialCount = 7)
}
```

Application or framework code transports `result.stateJson` to the browser. Pass the received
string directly to the state-aware overload:

```kotlin
val composition = hydrateComposable(root, receivedStateJson) {
    Counter(initialCount = 0)
}
```

The browser initializer is skipped because `counter` restores to `7` before positional DOM
matching. Payload discovery, HTML embedding, escaping, CSP handling, and HTTP transport are outside
Compose HTML's state API.
~~~~

Update current limitation 2 from “No state or data serialization” to:

```markdown
2. **Only explicitly registered state is serialized.** Values created through
   `rememberHydratableState` can cross the SSR boundary when using `renderToStringWithState` and
   the state-aware `hydrateComposable` overload. Ordinary `remember`, slot-table state, page
   inputs, effects, coroutine state, and event handlers are not serialized. Keys must be nonblank,
   page-global, and identical across the server and browser initial compositions.
```

- [ ] **Step 2: Update JVM SSR guide without adding transport policy**

In `html/core/JVM_SSR.md`, add this subsection after basic `renderToString` usage:

~~~~markdown
### Rendering explicitly registered hydration state

Use `renderToStringWithState` when selected mutable state must initialize browser hydration:

```kotlin
val result: SsrResult = renderToStringWithState {
    Counter(initialCount = 7)
}

val markup: String = result.html
val rawStateJson: String = result.stateJson
```

Register values inside the composable with `rememberHydratableState(key)`. The renderer captures
HTML and current state after the same settling frame and before composition teardown. The returned
JSON is raw transport data; application or framework code decides how to deliver it safely.

Calling ordinary `renderToString` installs no state registry. In that path,
`rememberHydratableState` behaves like ordinary remembered mutable state and nothing is exported.
~~~~

In limitation 5, replace “no state serialization” with a link to the new subsection and clarify that parser normalization, SVG, raw-text elements, externally changed DOM, nested hydration, async initial tree changes, and mismatch fallback remain unsupported.

- [ ] **Step 3: Run full HTML runtime, core, and SVG tests**

Run:

```bash
./gradlew :internal-html-core-runtime:allTests \
  :html-core:allTests \
  :html-svg:allTests
```

Expected: `BUILD SUCCESSFUL`; all JVM and JS tests pass across runtime, core, and SVG.

If Gradle fails before project configuration because local Java is unsupported, rerun the same command with:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew \
  :internal-html-core-runtime:allTests \
  :html-core:allTests \
  :html-svg:allTests
```

Expected: `BUILD SUCCESSFUL` under JDK 21. Record the initial environment failure separately; do not count it as test success.

- [ ] **Step 4: Verify compile targets, formatting, scope, and public API call sites**

Run from `html/`:

```bash
./gradlew :html-core:compileKotlinMetadata \
  :html-core:compileKotlinJvm \
  :html-core:compileKotlinJs
cd ..
git diff --check
rg -n "script|CSP|nonce|hydration-spring" \
  html/core/src/commonMain/kotlin/org/jetbrains/compose/web/state \
  html/core/src/jsMain/kotlin/org/jetbrains/compose/web/HydrateComposableWithState.kt
git status --short
```

Expected:

- all three compile tasks succeed;
- `git diff --check` emits nothing;
- transport-policy search emits nothing;
- `git status --short` lists only intended state API, tests, runtime seam, build, and documentation files;
- no file under `html/examples/hydration-spring` changed.

- [ ] **Step 5: Review spec coverage and exact contracts**

Check the final diff against `docs/superpowers/specs/2026-07-15-compose-html-hydration-state-serialization-design.md` and confirm:

- inferred and explicit serializers exist;
- registry-free fallback uses ordinary `remember` semantics;
- JSON envelope version is `1` and state keys encode in sorted order;
- JSON `null` differs from missing state;
- active duplicate, missing, extra, malformed, wrong-type, encode, and decode failures are tested;
- state verification runs before DOM hydration commit;
- registry switches live only after successful DOM finish;
- HTML and state snapshot after the same settling frame and before disposal;
- existing APIs remain present;
- no state transport or Spring integration entered scope.

- [ ] **Step 6: Commit documentation**

```bash
git add html/core/HYDRATION.md html/core/JVM_SSR.md
git commit -m "docs(html): document hydration state transfer"
```

---

## Self-Review Notes

- **Spec coverage:** Public API is implemented in Tasks 2-4; exact JSON and error model in Tasks 1-2; JVM same-point snapshot in Task 3; JS pre-commit restoration and cleanup in Task 4; transport boundary and limitations in Task 5.
- **Type consistency:** `HydrationStateRegistry`, `HydrationStateEntry`, `HydrationStatePayloadCodec`, `LocalHydrationStateRegistry`, `SsrResult`, `renderToStringWithState`, `hydrateComposableWithPreCommit`, and `rememberHydratableState` use identical names and signatures across all tasks.
- **Scope:** One cohesive feature across common state storage plus JVM/JS entry-point adapters. Framework transport and Spring example remain excluded.
- **Risk controls:** Remembered entries release providers through `RememberObserver`; payload decode precedes hydration; state verification precedes `strategy.finish`; live mode starts only after helper returns successfully; state encoding remains inside JVM renderer's existing `try/finally` cleanup.
