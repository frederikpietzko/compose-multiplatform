# Compose HTML JVM SSR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the existing Compose HTML composables render to an HTML string on the JVM (server-side rendering), while keeping the current Kotlin/JS runtime working unchanged.

**Architecture:** Refactor `:html-core`, `:internal-html-core-runtime`, and `:html-svg` from JS-only into true KMP modules. Move the JS sources into `commonMain` behind an `expect`/`actual` node abstraction. JS `actual`s stay backed by the browser DOM (near-zero behavior change). A new `jvmMain` backend builds a plain-Kotlin mutable `JvmNode` tree and serializes it to HTML with kotlinx.html via a new `renderToString` entry point. Events and native-node side effects are inert on the JVM.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose runtime (multiplatform), kotlinx.html (JVM serializer), Gradle. Build tool: `html/gradlew`.

## Global Constraints

- Compose core version: `1.10.1` (`html/gradle.properties`, `compose.version`). Do not change.
- Coroutines: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC` (from the inline version catalog in `html/settings.gradle.kts`). Do not change.
- kotlinx.html JVM dependency to add: `org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0`. Add as a catalog entry `kotlinx-html`.
- **All work happens inside the `html/` Gradle build**, which has its own `gradlew`, `settings.gradle.kts`, and `buildSrc`. Always run Gradle from `html/`.
- **Hard invariant: the JS artifact's public API and behavior must not change.** `:html-core:jsTest` and `:internal-html-core-runtime:jsTest` must stay green after every task.
- Preserve `@ComposeWebInternalApi` / `ComposeWebExperimentalTestsApi` opt-in annotations on moved declarations.
- Package names stay identical (`org.jetbrains.compose.web.*`) — only source-set location changes.
- Hydration, on-screen JVM rendering, wasm, and JVM event dispatch are OUT OF SCOPE.

### Standard commands (used throughout)

Run all from `/Users/frederik.pietzko/Workspace/compose-multiplatform/html`:

- Compile common metadata: `./gradlew :html-core:compileKotlinMetadata`
- Compile JS: `./gradlew :html-core:compileKotlinJs`
- Compile JVM: `./gradlew :html-core:compileKotlinJvm`
- JS tests: `./gradlew :html-core:jsTest`
- JVM tests: `./gradlew :html-core:jvmTest`
- Runtime module JS tests: `./gradlew :internal-html-core-runtime:jsTest`

---

## File Structure

Target layout after this plan (per module — shown for `:html-core`; `:internal-html-core-runtime` and `:html-svg` follow the same split):

```
html/core/src/
  commonMain/kotlin/...   <- bulk of former jsMain sources (backend-agnostic API)
  jsMain/kotlin/...       <- JS `actual`s: browser-DOM backend (thin)
  jvmMain/kotlin/...      <- JVM `actual`s: JvmNode model + kotlinx.html serializer
  jsTest/kotlin/...       <- unchanged
  jvmTest/kotlin/...      <- NEW: renderToString + parity tests
```

Key new files:
- `internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/NativeNode.kt` — `expect` node abstraction + common applier.
- `internal-html-core-runtime/src/jsMain/.../NativeNode.js.kt` — JS `actual`s (browser DOM).
- `internal-html-core-runtime/src/jvmMain/.../NativeNode.jvm.kt` — JVM `actual`s (`JvmNode`).
- `internal-html-core-runtime/src/jvmMain/.../HtmlSerializer.kt` — `JvmNode` → HTML string via kotlinx.html.
- `internal-html-core-runtime/src/commonMain/.../renderToString.kt` — JVM-facing entry (expect/actual; JS actual throws/unsupported or omitted).

---

## Phase 0 — KMP skeleton (build green on JS, empty JVM)

### Task 0.1: Add kotlinx.html to the version catalog

**Files:**
- Modify: `html/settings.gradle.kts` (the `versionCatalogs { create("libs") { ... } }` block, currently near line 64)

**Interfaces:**
- Produces: catalog accessor `libs.kotlinx.html` resolving to `org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0`.

- [ ] **Step 1: Add the library line**

In `html/settings.gradle.kts`, inside `versionCatalogs { create("libs") { ... } }`, add below the existing `kotlinx-coroutines-core` line:

```kotlin
            library("kotlinx-html", "org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
```

- [ ] **Step 2: Verify the catalog resolves**

Run: `./gradlew :html-core:help`
Expected: BUILD SUCCESSFUL (no "Invalid catalog definition" error).

- [ ] **Step 3: Commit**

```bash
git add html/settings.gradle.kts
git commit -m "build(html): add kotlinx-html to version catalog"
```

---

### Task 0.2: Split `:internal-html-core-runtime` into common/js source sets (no logic change)

This module is smallest (6 files) — do it first to prove the KMP split mechanic before touching `core`.

**Files:**
- Modify: `html/internal-html-core-runtime/build.gradle.kts`
- Move (git mv): everything under `src/jsMain/kotlin` that is backend-agnostic → `src/commonMain/kotlin` (decided per file in later tasks). For THIS task, only add the `jvm()` target + empty `commonMain`/`jvmMain`; leave all sources in `jsMain`.

**Interfaces:**
- Produces: module compiles for `metadata`, `js`, and `jvm` targets; `jvmMain` empty.

- [ ] **Step 1: Read current build file**

Run: `cat html/internal-html-core-runtime/build.gradle.kts`

- [ ] **Step 2: Add jvm target + common/jvm source sets**

Edit `html/internal-html-core-runtime/build.gradle.kts` so the `kotlin { }` block declares:

```kotlin
kotlin {
    jvm()
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jsMain by getting {
            // existing js-only deps stay here
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.html)
            }
        }
    }
}
```

Preserve any existing `optIn`/languageSettings blocks currently in the file (copy them into the matching source set).

- [ ] **Step 3: Verify JS still compiles and tests pass**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJs :internal-html-core-runtime:jsTest`
Expected: BUILD SUCCESSFUL, tests green.

- [ ] **Step 4: Verify JVM target compiles (empty)**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJvm`
Expected: BUILD SUCCESSFUL (nothing to compile in jvmMain yet).

- [ ] **Step 5: Commit**

```bash
git add html/internal-html-core-runtime/build.gradle.kts
git commit -m "build(html): add jvm target + common source set to runtime module"
```

---

### Task 0.3: Same skeleton split for `:html-core` and `:html-svg`

**Files:**
- Modify: `html/core/build.gradle.kts`
- Modify: `html/svg/build.gradle.kts`

**Interfaces:**
- Produces: both modules declare `jvm()` + `commonMain`/`jvmMain`; JVM builds empty; JS unchanged.

- [ ] **Step 1: Edit `html/core/build.gradle.kts`**

Replace the `sourceSets { }` body so `commonMain` holds the runtime deps and `jvmMain` depends on the runtime module + kotlinx.html. Remove the unused `compose.desktop.currentOs` dependency. Result:

```kotlin
kotlin {
    jvm()
    js(IR) {
        browser() {
            testTask {
                useKarma { standardConf() }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jsMain by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
            dependencies {
                api(project(":internal-html-core-runtime"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(project(":internal-html-core-runtime"))
                implementation(libs.kotlinx.html)
            }
        }
        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test-js"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
```

- [ ] **Step 2: Apply the equivalent edit to `html/svg/build.gradle.kts`**

Mirror the same target/source-set structure. `svg` `commonMain` deps: `implementation(compose.runtime)`; `jsMain`/`jvmMain` each `api(project(":html-core"))`.

- [ ] **Step 3: Verify JS compiles + tests green for both**

Run: `./gradlew :html-core:compileKotlinJs :html-core:jsTest :html-svg:compileKotlinJs`
Expected: BUILD SUCCESSFUL, tests green.

- [ ] **Step 4: Verify JVM targets compile empty**

Run: `./gradlew :html-core:compileKotlinJvm :html-svg:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add html/core/build.gradle.kts html/svg/build.gradle.kts
git commit -m "build(html): add jvm target + common source sets to core and svg"
```

---

## Phase 1 — Node abstraction + applier to common

### Task 1.1: Define the common `NativeNode` abstraction

**Files:**
- Create: `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/NativeNode.kt`

**Interfaces:**
- Produces:
  - `expect abstract class NativeNode` — the retained per-element node the applier mutates.
  - `expect fun NativeNode.insertChild(index: Int, child: NativeNode)`
  - `expect fun NativeNode.removeChildren(index: Int, count: Int)`
  - `expect fun NativeNode.moveChildren(from: Int, to: Int, count: Int)`
  - `expect fun NativeNode.clearChildren()`
  - `@ComposeWebInternalApi open class NativeNodeWrapper(open val node: NativeNode)` with `insert`/`remove`/`move` delegating to the expect functions (this replaces the body of the current `DomNodeWrapper`).

- [ ] **Step 1: Write the common abstraction**

```kotlin
package org.jetbrains.compose.web.internal.runtime

expect abstract class NativeNode

expect fun NativeNode.insertChild(index: Int, child: NativeNode)
expect fun NativeNode.removeChildren(index: Int, count: Int)
expect fun NativeNode.moveChildren(from: Int, to: Int, count: Int)
expect fun NativeNode.clearChildren()

@ComposeWebInternalApi
open class NativeNodeWrapper(open val node: NativeNode) {
    fun insert(index: Int, wrapper: NativeNodeWrapper) = node.insertChild(index, wrapper.node)
    fun remove(index: Int, count: Int) = node.removeChildren(index, count)
    fun move(from: Int, to: Int, count: Int) = node.moveChildren(from, to, count)
}
```

- [ ] **Step 2: Compile (expect with no actual should fail)**

Run: `./gradlew :internal-html-core-runtime:compileKotlinMetadata`
Expected: FAIL — "expect ... has no actual declaration" for JS/JVM. Confirms the expect is seen. (Proceed to add actuals next; do not commit yet.)

---

### Task 1.2: JS `actual` for `NativeNode` (browser DOM)

**Files:**
- Create: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/NativeNode.js.kt`

**Interfaces:**
- Consumes: expect decls from Task 1.1.
- Produces: `actual typealias NativeNode = org.w3c.dom.Node` and actual body functions implementing insert/remove/move/clear using the exact logic currently in `DomNodeWrapper` (Base of former `DomApplier.kt`).

- [ ] **Step 1: Write the JS actuals**

```kotlin
package org.jetbrains.compose.web.internal.runtime

import kotlinx.dom.clear
import org.w3c.dom.Node
import org.w3c.dom.get

actual typealias NativeNode = Node

actual fun NativeNode.insertChild(index: Int, child: NativeNode) {
    val length = childNodes.length
    if (index < length) {
        insertBefore(child, childNodes[index]!!)
    } else {
        appendChild(child)
    }
}

actual fun NativeNode.removeChildren(index: Int, count: Int) {
    repeat(count) { removeChild(childNodes[index]!!) }
}

actual fun NativeNode.moveChildren(from: Int, to: Int, count: Int) {
    if (from == to) return
    for (i in 0 until count) {
        val fromIndex = if (from > to) from + i else from
        val toIndex = if (from > to) to + i else to + count - 2
        val child = removeChild(childNodes[fromIndex]!!)
        insertBefore(child, childNodes[toIndex]!!)
    }
}

actual fun NativeNode.clearChildren() {
    clear()
}
```

- [ ] **Step 2: Verify JS metadata + JS compile**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJs`
Expected: BUILD SUCCESSFUL.

---

### Task 1.3: JVM `actual` for `NativeNode` (JvmNode model)

**Files:**
- Create: `html/internal-html-core-runtime/src/jvmMain/kotlin/org/jetbrains/compose/web/internal/runtime/NativeNode.jvm.kt`

**Interfaces:**
- Consumes: expect decls from Task 1.1.
- Produces:
  - `sealed class JvmNode` (the actual for `NativeNode`).
  - `class JvmElement(val tagName: String) : JvmNode()` with `attributes: LinkedHashMap<String,String>`, `styles: LinkedHashMap<String,String>`, `classes: MutableList<String>`, `children: MutableList<JvmNode>`.
  - `class JvmTextNode(var text: String) : JvmNode()` with `children` empty.
  - `class JvmRootNode : JvmNode()` with `children`.

- [ ] **Step 1: Write the failing test first**

Create `html/internal-html-core-runtime/src/jvmTest/kotlin/NativeNodeJvmTest.kt`:

```kotlin
import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeNodeJvmTest {
    @Test
    fun insertsAndMovesChildren() {
        val root: NativeNode = JvmRootNode()
        val a = JvmElement("a"); val b = JvmElement("b"); val c = JvmElement("c")
        root.insertChild(0, a)
        root.insertChild(1, b)
        root.insertChild(2, c)
        root.moveChildren(0, 2, 1) // move a after b
        assertEquals(listOf("b", "a", "c"), (root as JvmRootNode).children.map { (it as JvmElement).tagName })
        root.removeChildren(0, 1)
        assertEquals(listOf("a", "c"), root.children.map { (it as JvmElement).tagName })
    }
}
```

- [ ] **Step 2: Run test to verify it fails to compile**

Run: `./gradlew :internal-html-core-runtime:jvmTest`
Expected: FAIL — `JvmRootNode`/`JvmElement` unresolved.

- [ ] **Step 3: Write the JVM actuals**

```kotlin
package org.jetbrains.compose.web.internal.runtime

actual sealed class NativeNode {
    val children: MutableList<NativeNode> = mutableListOf()
}

class JvmRootNode : NativeNode()
class JvmElement(val tagName: String) : NativeNode() {
    val attributes = LinkedHashMap<String, String>()
    val styles = LinkedHashMap<String, String>()
    val classes = mutableListOf<String>()
}
class JvmTextNode(var text: String) : NativeNode()

actual fun NativeNode.insertChild(index: Int, child: NativeNode) {
    if (index < children.size) children.add(index, child) else children.add(child)
}
actual fun NativeNode.removeChildren(index: Int, count: Int) {
    repeat(count) { children.removeAt(index) }
}
actual fun NativeNode.moveChildren(from: Int, to: Int, count: Int) {
    if (from == to) return
    val moved = ArrayList<NativeNode>(count)
    repeat(count) { moved.add(children.removeAt(from)) }
    val insertAt = if (from > to) to else to - count
    children.addAll(insertAt.coerceIn(0, children.size), moved)
}
actual fun NativeNode.clearChildren() {
    children.clear()
}
```

Note: the test in Step 1 references `JvmRootNode().children` — since `children` now lives on `NativeNode`, adjust the cast in the test to `(root as JvmRootNode)` still works (property inherited). If the compiler complains about the `move` semantics, align the expected order with this list-based implementation and re-run.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :internal-html-core-runtime:jvmTest`
Expected: PASS.

- [ ] **Step 5: Verify JS still green (metadata + js)**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJs :internal-html-core-runtime:jsTest`
Expected: BUILD SUCCESSFUL, green.

- [ ] **Step 6: Commit**

```bash
git add html/internal-html-core-runtime/src
git commit -m "feat(html): common NativeNode abstraction with js and jvm actuals"
```

---

### Task 1.4: Move the applier to common

**Files:**
- Modify/Move: `html/internal-html-core-runtime/src/jsMain/.../DomApplier.kt` — split. The generic `DomApplier` + `NamedEventListener` go to `commonMain`; JS-DOM-specific `DomNodeWrapper` body already replaced by `NativeNodeWrapper` (Task 1.1).
- Create: `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/DomApplier.kt`
- Delete: the moved declarations from the jsMain file.

**Interfaces:**
- Consumes: `NativeNodeWrapper` (Task 1.1).
- Produces: `@ComposeWebInternalApi class DomApplier(root: NativeNodeWrapper) : AbstractApplier<NativeNodeWrapper>(root)` in common; `NamedEventListener` becomes an `expect`-free common interface if it no longer references `org.w3c.dom.events.EventListener`.

- [ ] **Step 1: Create the common applier**

```kotlin
package org.jetbrains.compose.web.internal.runtime

import androidx.compose.runtime.AbstractApplier

@ComposeWebInternalApi
class DomApplier(
    root: NativeNodeWrapper
) : AbstractApplier<NativeNodeWrapper>(root) {
    override fun insertTopDown(index: Int, instance: NativeNodeWrapper) { /* bottom-up */ }
    override fun insertBottomUp(index: Int, instance: NativeNodeWrapper) = current.insert(index, instance)
    override fun remove(index: Int, count: Int) = current.remove(index, count)
    override fun move(from: Int, to: Int, count: Int) = current.move(from, to, count)
    override fun onClear() = root.node.clearChildren()
}
```

- [ ] **Step 2: Handle `NamedEventListener`**

`NamedEventListener` currently extends `org.w3c.dom.events.EventListener`. Make it common by dropping the JS supertype and keeping only `val name: String` plus an `expect`-backed invoke, OR keep it JS-only if only JS uses it. Determine usage:

Run: `cd html && grep -rn "NamedEventListener" internal-html-core-runtime core`
Then: if referenced only by JS event wiring, leave `NamedEventListener` in `jsMain`. If referenced by common code, move a minimal common interface `interface NamedEventListener { val name: String }` to `commonMain` and let JS extend it with `EventListener`.

- [ ] **Step 3: Delete the old declarations from the jsMain `DomApplier.kt`** (the `DomApplier` class and `DomNodeWrapper`; keep JS-only leftovers).

- [ ] **Step 4: Verify JS compiles + tests green**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJs :internal-html-core-runtime:jsTest`
Expected: BUILD SUCCESSFUL, green.

- [ ] **Step 5: Verify JVM compiles**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJvm`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add html/internal-html-core-runtime/src
git commit -m "refactor(html): move DomApplier to common over NativeNode"
```

---

## Phase 2 — Lift the API surface to common

> This is the largest, most mechanical phase. The transformation rule below is applied file-by-file. Each task = one cohesive file/group, each with a compile check on **all three** targets (metadata, js, jvm) and JS tests green.

### The transformation rule (apply to every moved file)

1. `git mv` the file from `src/jsMain/kotlin/...` to `src/commonMain/kotlin/...` (same package path).
2. Replace every `org.w3c.dom.*` element/type reference with the common element type from Task 2.1 (`DomElement` and its subtypes).
3. Any body that calls a **browser-only member** (`.style`, `.classList`, `.setAttribute`, `addEventListener`, `document.*`, `.value`) must move behind an `expect`/`actual` seam: declare an `expect` function in common, put the current browser code as the JS `actual`, and a JVM `actual` that writes into `JvmElement`'s maps (or is inert for events/`prop`/`ref`).
4. Compile all three targets + run JS tests. Fix, then commit.

### Task 2.1: Common element type hierarchy

**Files:**
- Create: `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/dom/DomElementTypes.kt`
- Create: `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/dom/DomElementTypes.js.kt`
- Create: `html/core/src/jvmMain/kotlin/org/jetbrains/compose/web/dom/DomElementTypes.jvm.kt`

**Interfaces:**
- Produces: `expect` element types. Common name → JS actual (browser type) → JVM actual (`JvmElement` alias). Full set enumerated below.

The complete set to declare (base + all subtypes referenced across `core`):

```
DomElement           -> js: org.w3c.dom.Element        jvm: JvmElement
DomHtmlElement       -> js: org.w3c.dom.HTMLElement    jvm: JvmElement
DomText              -> js: org.w3c.dom.Text           jvm: JvmTextNode
```
Plus each of these (JS actual = the identically-named `org.w3c.dom.HTMLxxxElement`; JVM actual = `typealias ... = JvmElement`):
`HTMLAnchorElement, HTMLAreaElement, HTMLAudioElement, HTMLBRElement, HTMLButtonElement, HTMLCanvasElement, HTMLDataListElement, HTMLDListElement, HTMLDivElement, HTMLEmbedElement, HTMLFieldSetElement, HTMLFormElement, HTMLHRElement, HTMLHeadingElement, HTMLIFrameElement, HTMLImageElement, HTMLInputElement, HTMLLIElement, HTMLLabelElement, HTMLLegendElement, HTMLMapElement, HTMLMeterElement, HTMLOListElement, HTMLObjectElement, HTMLOptGroupElement, HTMLOptionElement, HTMLOutputElement, HTMLParagraphElement, HTMLParamElement, HTMLPictureElement, HTMLPreElement, HTMLProgressElement, HTMLSelectElement, HTMLSourceElement, HTMLSpanElement, HTMLStyleElement, HTMLTableCaptionElement, HTMLTableCellElement, HTMLTableColElement, HTMLTableElement, HTMLTableRowElement, HTMLTableSectionElement, HTMLTextAreaElement, HTMLTrackElement, HTMLUListElement, HTMLVideoElement`.

- [ ] **Step 1: Write the common expect declarations** (representative — repeat for the full list above)

```kotlin
package org.jetbrains.compose.web.dom

expect interface DomElement
expect interface DomHtmlElement : DomElement
expect interface HTMLDivElement : DomHtmlElement
expect interface HTMLInputElement : DomHtmlElement
// ... one line per type in the list above
```

- [ ] **Step 2: JS actuals** (typealias to browser types)

```kotlin
package org.jetbrains.compose.web.dom
actual typealias DomElement = org.w3c.dom.Element
actual typealias DomHtmlElement = org.w3c.dom.HTMLElement
actual typealias HTMLDivElement = org.w3c.dom.HTMLDivElement
actual typealias HTMLInputElement = org.w3c.dom.HTMLInputElement
// ... one per type
```

- [ ] **Step 3: JVM actuals** (all map to JvmElement)

```kotlin
package org.jetbrains.compose.web.dom
import org.jetbrains.compose.web.internal.runtime.JvmElement
actual typealias DomElement = JvmElement
actual typealias DomHtmlElement = JvmElement
actual typealias HTMLDivElement = JvmElement
actual typealias HTMLInputElement = JvmElement
// ... one per type — all = JvmElement
```

Note: because multiple common `expect interface`s map to the same JVM `JvmElement`, prefer `expect class` with `typealias` actuals on both sides (JS to distinct browser classes, JVM all to `JvmElement`). If the compiler rejects multiple typealiases to one class across distinct expects, fall back to declaring the JVM types as empty `class HTMLDivElement : JvmElementBase()` wrappers created by the factory. Validate which form compiles in Step 4 and keep that form.

- [ ] **Step 4: Compile all three targets**

Run: `./gradlew :html-core:compileKotlinMetadata :html-core:compileKotlinJs :html-core:compileKotlinJvm`
Expected: BUILD SUCCESSFUL. If Step 3's note applies, switch to the wrapper-class form and re-run.

- [ ] **Step 5: Commit**

```bash
git add html/core/src
git commit -m "feat(html): common DOM element type hierarchy with js/jvm actuals"
```

---

### Task 2.2: Element factory (`ElementBuilder`) to common with expect `createElement`

**Files:**
- Modify/Move: `html/core/src/jsMain/.../dom/Elements.kt` — extract `ElementBuilder` + `ElementBuilderImplementation` factory.
- Create: `html/core/src/commonMain/.../dom/ElementBuilder.kt`
- Create: `html/core/src/jsMain/.../dom/ElementBuilder.js.kt`
- Create: `html/core/src/jvmMain/.../dom/ElementBuilder.jvm.kt`

**Interfaces:**
- Consumes: common element types (Task 2.1), `NativeNode`/`JvmElement`.
- Produces:
  - `interface ElementBuilder<TElement : DomElement> { fun create(): NativeNode }` in common.
  - `expect fun createNativeElement(tagName: String): NativeNode` — JS: `document.createElement(tagName)`; JVM: `JvmElement(tagName)`.
  - `expect fun createNativeText(text: String): NativeNode` — JS: `document.createTextNode`; JVM: `JvmTextNode`.

- [ ] **Step 1: Common factory**

```kotlin
package org.jetbrains.compose.web.dom
import org.jetbrains.compose.web.internal.runtime.NativeNode

expect fun createNativeElement(tagName: String): NativeNode
expect fun createNativeText(text: String): NativeNode

interface ElementBuilder<TElement : DomElement> {
    fun create(): NativeNode
    companion object { /* createBuilder(tagName) moved here, using createNativeElement */ }
}
```

- [ ] **Step 2: JS actual** — `document.createElement(tagName)` / `document.createTextNode(text)` (lift from current `ElementBuilderImplementation`).

- [ ] **Step 3: JVM actual** — `JvmElement(tagName)` / `JvmTextNode(text)`.

- [ ] **Step 4: Compile all three targets + JS tests**

Run: `./gradlew :html-core:compileKotlinMetadata :html-core:compileKotlinJs :html-core:compileKotlinJvm :html-core:jsTest`
Expected: BUILD SUCCESSFUL, JS tests green.

- [ ] **Step 5: Commit**

```bash
git add html/core/src
git commit -m "refactor(html): element factory to common with platform createElement"
```

---

### Task 2.3: `AttrsScope` application seam (attrs/classes/style/prop/ref/events)

**Files:**
- Move: `html/core/src/jsMain/.../attributes/AttrsScope.kt` → `commonMain` (it is already plain-data; only `prop`'s `HTMLElement` bound + `ref`'s `TElement` need the common types).
- Move: the `DomElementWrapper` update logic (from former `dom/Base.kt`) behind an expect seam.
- Create: `html/core/src/commonMain/.../runtime/ElementUpdater.kt` (expect functions).
- Create: JS + JVM actuals.

**Interfaces:**
- Consumes: `JvmElement`, common element types.
- Produces expect functions (JS = current browser code; JVM = write into `JvmElement` maps / inert):
  - `expect fun NativeNode.applyAttrs(attrs: Map<String,String>)` — JVM: put into `JvmElement.attributes`.
  - `expect fun NativeNode.applyClasses(classes: List<String>)` — JVM: replace `JvmElement.classes`.
  - `expect fun NativeNode.applyStyle(style: StyleHolder)` — JVM: put into `JvmElement.styles`.
  - `expect fun NativeNode.applyProps(updates: List<Pair<(DomElement, Any) -> Unit, Any>>)` — JVM: **inert** (documented limitation).
  - `expect fun NativeNode.applyEventListeners(listeners: List<NamedEventListener>)` — JVM: **inert**.

- [ ] **Step 1: Write a failing JVM test for attr/class/style application**

Create `html/core/src/jvmTest/kotlin/ElementUpdaterJvmTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run — verify fail**

Run: `./gradlew :html-core:jvmTest`
Expected: FAIL — `applyAttrs` unresolved.

- [ ] **Step 3: Common expect + JS/JVM actuals** (JS actuals lift the exact bodies from `DomElementWrapper.updateAttrs/updateClasses/updateStyleDeclarations/updateProperties/updateEventListeners`; JVM as described in Interfaces)

JVM actual example:

```kotlin
actual fun NativeNode.applyAttrs(attrs: Map<String, String>) {
    val el = this as? JvmElement ?: return
    el.attributes.keys.retainAll(setOf("style", "class"))
    el.attributes.putAll(attrs)
}
actual fun NativeNode.applyClasses(classes: List<String>) {
    (this as? JvmElement)?.let { it.classes.clear(); it.classes.addAll(classes) }
}
actual fun NativeNode.applyStyle(style: StyleHolder) {
    val el = this as? JvmElement ?: return
    el.styles.clear()
    style.properties.forEach { (name, value, _) -> el.styles[name] = value.toString() }
    style.variables.forEach { (name, value) -> el.styles[name] = value.toString() }
}
actual fun NativeNode.applyProps(updates: List<Pair<(DomElement, Any) -> Unit, Any>>) { /* inert: SSR */ }
actual fun NativeNode.applyEventListeners(listeners: List<NamedEventListener>) { /* inert: SSR */ }
```

- [ ] **Step 4: Rewire `TagElement` / former `Base.kt`** to call the expect functions instead of `DomElementWrapper` methods, and move `TagElement` + `ComposeDomNode` to common. Keep the `update { set(...) }` structure, pointing at the expect functions.

- [ ] **Step 5: Run JVM test + JS tests**

Run: `./gradlew :html-core:jvmTest :html-core:jsTest :html-core:compileKotlinMetadata`
Expected: JVM test PASS, JS tests green, metadata compiles.

- [ ] **Step 6: Commit**

```bash
git add html/core/src
git commit -m "refactor(html): attrs/class/style application behind platform seam"
```

---

### Task 2.4: Synthetic events + `EventsListenerScope` to common

**Files:**
- Move: `html/core/src/jsMain/.../attributes/EventsListenerScope.kt`, `SyntheticEventListener.kt`, and `androidx/compose/web/events/*` synthetic-event types → common, abstracting the native-event field.

**Interfaces:**
- Produces: synthetic-event types whose `nativeEvent` is `expect` (JS = `org.w3c.dom.events.Event` subtype; JVM = a stub type never instantiated server-side). `EventsListenerScope` interface + all `onXxx` methods in common.

- [ ] **Step 1: Enumerate the native-event references**

Run: `cd html && grep -rhoE "org\.w3c\.dom\.[A-Za-z.]*Event[A-Za-z]*" core/src/jsMain | sort -u`
Record every native event type; each needs a common `expect`/actual pair or JS-only isolation.

- [ ] **Step 2: Introduce `expect class NativeEvent` (+ subtypes as needed)**; JS actual = the browser event; JVM actual = empty class. Synthetic wrappers reference `NativeEvent`.

- [ ] **Step 3: Move `EventsListenerScope` + synthetic wrappers to common**, referencing `NativeEvent`/common element types.

- [ ] **Step 4: Compile all three targets + JS tests**

Run: `./gradlew :html-core:compileKotlinMetadata :html-core:compileKotlinJs :html-core:compileKotlinJvm :html-core:jsTest`
Expected: BUILD SUCCESSFUL, JS green.

- [ ] **Step 5: Commit**

```bash
git add html/core/src
git commit -m "refactor(html): synthetic events to common with platform native-event"
```

---

### Task 2.5: Move CSS surface + remaining `dom/`, `attributes/`, `css/` files to common

**Files:**
- Move (batched, applying the transformation rule): all remaining files under `core/src/jsMain/kotlin/org/jetbrains/compose/web/{dom,attributes,css}` that do not reference browser-only runtime APIs. Files that inject stylesheets into the browser (`CSSStyleSheet` usage in `Style {}`) get an `expect`/`actual` seam: JS injects; JVM collects rules into a `<style>` element in the tree.

**Interfaces:**
- Consumes: everything above.
- Produces: the full public composable + CSS DSL API in `commonMain`. Only genuinely browser-bound helpers (e.g. `BrowserAPI.kt`, `window`/`document` access) remain in `jsMain` behind expect seams.

- [ ] **Step 1: List remaining jsMain files still importing org.w3c**

Run: `cd html/core/src/jsMain/kotlin && grep -rln "org.w3c\|kotlinx.browser" .`

- [ ] **Step 2: For each, apply the transformation rule** (move to common; browser-only members behind expect/actual). Do them in small commits (one directory or a few files each), compiling all three targets + JS tests after each.

- [ ] **Step 3: After the batch, verify no unexpected jsMain leftovers**

Run: `cd html/core/src/jsMain/kotlin && grep -rln "org.w3c\|kotlinx.browser" . || echo "clean"`
Expected: only intentional JS-only files remain (event wiring, browser API bridges).

- [ ] **Step 4: Full compile + JS tests**

Run: `./gradlew :html-core:compileKotlinMetadata :html-core:compileKotlinJs :html-core:compileKotlinJvm :html-core:jsTest`
Expected: BUILD SUCCESSFUL, JS green.

- [ ] **Step 5: Commit** (per sub-batch)

```bash
git add html/core/src
git commit -m "refactor(html): move remaining dom/attributes/css API to common"
```

---

## Phase 3 — JVM render entry point + serializer

### Task 3.1: `renderComposable` to common; JS retains browser clock

**Files:**
- Move: `html/internal-html-core-runtime/src/jsMain/.../renderComposable.kt` — split the generic composition wiring to common; keep `document`-based overloads in jsMain.
- Modify: `GlobalSnapshotManager`, `JsMicrotasksDispatcher` — keep JS-only; add a JVM frame-clock seam.

**Interfaces:**
- Produces: `fun <T> renderComposableRoot(rootWrapper: NativeNodeWrapper, clock: MonotonicFrameClock, content): Composition` in common (the generic body of current `renderComposable`). JS keeps `renderComposable(root: Element, ...)` and `renderComposableInBody`.

- [ ] **Step 1: Move the generic body to common**, parameterized on `NativeNodeWrapper` + an injected `MonotonicFrameClock` + `CoroutineDispatcher`. JS `renderComposable(Element,...)` calls it with `DomNodeWrapper(root)` + `JsMicrotasksDispatcher`.

- [ ] **Step 2: Verify JS compiles + jsTest (RenderComposableTests) green**

Run: `./gradlew :internal-html-core-runtime:compileKotlinJs :internal-html-core-runtime:jsTest`
Expected: BUILD SUCCESSFUL, green.

- [ ] **Step 3: Commit**

```bash
git add html/internal-html-core-runtime/src
git commit -m "refactor(html): generic renderComposable body to common"
```

---

### Task 3.2: JVM HTML serializer (JvmNode → String via kotlinx.html)

**Files:**
- Create: `html/internal-html-core-runtime/src/jvmMain/.../HtmlSerializer.kt`
- Test: `html/internal-html-core-runtime/src/jvmTest/kotlin/HtmlSerializerTest.kt`

**Interfaces:**
- Consumes: `JvmElement`, `JvmTextNode`, `JvmRootNode`.
- Produces: `fun serializeToHtml(root: NativeNode, prettyPrint: Boolean = false): String`.

- [ ] **Step 1: Write failing test**

```kotlin
import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlSerializerTest {
    @Test fun serializesElementWithAttrsClassesStyleAndText() {
        val root = JvmRootNode()
        val div = JvmElement("div").apply {
            classes += listOf("card", "big")
            styles["color"] = "red"
            attributes["id"] = "main"
        }
        val text = JvmTextNode("hi & <bye>")
        (div as NativeNode).children.add(text)
        (root as NativeNode).children.add(div)
        assertEquals(
            """<div class="card big" style="color:red;" id="main">hi &amp; &lt;bye&gt;</div>""",
            serializeToHtml(root)
        )
    }

    @Test fun voidElementHasNoClosingTag() {
        val root = JvmRootNode()
        (root as NativeNode).children.add(JvmElement("br"))
        assertEquals("<br>", serializeToHtml(root))
    }
}
```

- [ ] **Step 2: Run — verify fail**

Run: `./gradlew :internal-html-core-runtime:jvmTest`
Expected: FAIL — `serializeToHtml` unresolved.

- [ ] **Step 3: Implement using kotlinx.html low-level `TagConsumer`**

```kotlin
package org.jetbrains.compose.web.internal.runtime

import kotlinx.html.*
import kotlinx.html.stream.appendHTML

fun serializeToHtml(root: NativeNode, prettyPrint: Boolean = false): String {
    val sb = StringBuilder()
    val consumer = sb.appendHTML(prettyPrint = prettyPrint)
    root.children.forEach { emit(it, consumer) }
    consumer.finalize()
    return sb.toString()
}

private fun emit(node: NativeNode, consumer: TagConsumer<*>) {
    when (node) {
        is JvmTextNode -> consumer.onTagContent(node.text)
        is JvmElement -> {
            val attrs = LinkedHashMap<String, String>()
            if (node.classes.isNotEmpty()) attrs["class"] = node.classes.joinToString(" ")
            if (node.styles.isNotEmpty()) attrs["style"] = node.styles.entries.joinToString("") { "${it.key}:${it.value};" }
            attrs.putAll(node.attributes)
            val tag = object : Tag(node.tagName, consumer, "html", attrs, inlineTag = false, emptyTag = node.tagName in VOID_TAGS) {}
            consumer.onTagStart(tag)
            node.children.forEach { emit(it, consumer) }
            consumer.onTagEnd(tag)
        }
        else -> node.children.forEach { emit(it, consumer) }
    }
}

private val VOID_TAGS = setOf(
    "area","base","br","col","embed","hr","img","input","link","meta","param","source","track","wbr"
)
```

Note: kotlinx.html escapes attribute values and text content via the `TagConsumer`. Verify escaping in Step 4; if `onTagContent` does not escape, use the escaping entry point kotlinx.html provides for text (validate against the failing test's expected `&amp;`/`&lt;`). Adjust the `Tag` construction to the exact `kotlinx.html` 0.11.0 constructor signature (check `./gradlew :internal-html-core-runtime:dependencies` sources or the kotlinx.html API).

- [ ] **Step 4: Run — verify pass**

Run: `./gradlew :internal-html-core-runtime:jvmTest`
Expected: PASS (both tests). Fix escaping/void handling until exact strings match.

- [ ] **Step 5: Commit**

```bash
git add html/internal-html-core-runtime/src
git commit -m "feat(html): JvmNode to HTML string serializer via kotlinx.html"
```

---

### Task 3.3: `renderToString` public entry point (JVM)

**Files:**
- Create: `html/core/src/jvmMain/kotlin/org/jetbrains/compose/web/renderToString.kt`
- Test: `html/core/src/jvmTest/kotlin/RenderToStringTest.kt`

**Interfaces:**
- Consumes: common `renderComposableRoot`, `JvmRootNode`, `serializeToHtml`, `DOMScope`.
- Produces: `fun renderToString(content: @Composable DOMScope<DomElement>.() -> Unit): String`.

- [ ] **Step 1: Write failing test**

```kotlin
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.dom.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderToStringTest {
    @Test fun rendersSimpleTree() {
        val html = renderToString {
            Div({ classes("greeting") }) {
                Text("Hello")
                Span { Text("!") }
            }
        }
        assertEquals("""<div class="greeting">Hello<span>!</span></div>""", html)
    }

    @Test fun rendersConditionalStateAfterSettle() {
        val html = renderToString {
            var shown = androidx.compose.runtime.remember { true }
            if (shown) P { Text("visible") }
        }
        assertEquals("<p>visible</p>", html)
    }
}
```

- [ ] **Step 2: Run — verify fail**

Run: `./gradlew :html-core:jvmTest`
Expected: FAIL — `renderToString` unresolved.

- [ ] **Step 3: Implement**

```kotlin
package org.jetbrains.compose.web

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.dom.DomElement
import org.jetbrains.compose.web.internal.runtime.*

@OptIn(ComposeWebInternalApi::class)
fun renderToString(content: @Composable DOMScope<DomElement>.() -> Unit): String {
    val root = JvmRootNode()
    val clock = BroadcastFrameClock()
    val job = Job()
    val scope = CoroutineScope(clock + Dispatchers.Unconfined + job)
    val recomposer = Recomposer(scope.coroutineContext)
    val composition = ControlledComposition(DomApplier(NativeNodeWrapper(root)), recomposer)

    scope.launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }
    val domScope = object : DOMScope<DomElement> {
        override val DisposableEffectScope.scopeElement: DomElement get() = root as DomElement
    }
    composition.setContent { content(domScope) }
    clock.sendFrame(0L)                 // let effects/state settle once
    val html = serializeToHtml(root)
    composition.dispose()
    recomposer.close()
    job.cancel()
    return html
}
```

Note: `scopeElement` cast — `JvmRootNode` is not a `JvmElement`. If `DOMScope.scopeElement` must be an element type, wrap the content in a synthetic root `JvmElement("")` whose children are serialized (skip emitting the empty-tag wrapper in `serializeToHtml`). Adjust `serializeToHtml` to treat `tagName == ""` as a transparent group. Validate in Step 4.

- [ ] **Step 4: Run — verify pass**

Run: `./gradlew :html-core:jvmTest`
Expected: PASS. Iterate on the root-wrapper/settle handling until both tests pass.

- [ ] **Step 5: Commit**

```bash
git add html/core/src
git commit -m "feat(html): renderToString JVM SSR entry point"
```

---

## Phase 4 — Parity guard + svg + final validation

### Task 4.1: Cross-target parity test

**Files:**
- Create: `html/core/src/commonTest/kotlin/ParityComposables.kt` — a shared composable used by both jsTest and jvmTest to prove the API compiles on both.
- Create: `html/core/src/jvmTest/kotlin/ParityJvmTest.kt`

**Interfaces:**
- Consumes: public composable API.
- Produces: a `@Composable fun SampleTree()` in `commonTest` exercising Div/Span/P/Input/attrs/style/classes/nested/conditional.

- [ ] **Step 1: Write `SampleTree` in commonTest** using a representative slice of the API (no event assertions).

- [ ] **Step 2: jvmTest renders it and asserts a known string.**

```kotlin
class ParityJvmTest {
    @Test fun sampleTreeRenders() {
        val html = renderToString { SampleTree() }
        assert(html.contains("<div")) // plus specific structural assertions
    }
}
```

- [ ] **Step 3: Ensure commonTest also compiles for JS** (add a jsTest that references `SampleTree` in a `renderComposable` into a test container, or at minimum compiles it).

- [ ] **Step 4: Run both**

Run: `./gradlew :html-core:jsTest :html-core:jvmTest`
Expected: both green.

- [ ] **Step 5: Commit**

```bash
git add html/core/src
git commit -m "test(html): cross-target parity test for common composable API"
```

---

### Task 4.2: Apply the same refactor to `:html-svg`

**Files:**
- Move `svg/src/jsMain` sources to common per the transformation rule; add JVM actuals (SVG elements → `JvmElement`).

**Interfaces:**
- Produces: SVG composables render via `renderToString`.

- [ ] **Step 1: Enumerate svg's org.w3c references**

Run: `cd html/svg/src/jsMain/kotlin && grep -rln "org.w3c\|kotlinx.browser" .`

- [ ] **Step 2: Apply the transformation rule file-by-file**, compiling all three targets after each.

- [ ] **Step 3: Add a jvmTest** rendering a small `<svg><circle/></svg>` tree, asserting the string.

- [ ] **Step 4: Run**

Run: `./gradlew :html-svg:jsTest :html-svg:jvmTest`
Expected: green.

- [ ] **Step 5: Commit**

```bash
git add html/svg/src
git commit -m "feat(html): svg module renders to string on jvm"
```

---

### Task 4.3: Full regression + document known limitations

**Files:**
- Create: `html/core/JVM_SSR.md` — usage + documented limitations (`prop`-driven values, `ref` inert, events inert on JVM).

- [ ] **Step 1: Run the full JS + JVM suite across modules**

Run: `./gradlew :internal-html-core-runtime:jsTest :internal-html-core-runtime:jvmTest :html-core:jsTest :html-core:jvmTest :html-svg:jsTest :html-svg:jvmTest`
Expected: all green.

- [ ] **Step 2: Write `JVM_SSR.md`** covering: `renderToString` usage example; the §7 limitations from the spec (controlled-input `prop` values not reflected; `ref` blocks inert; no events server-side); hydration is future work.

- [ ] **Step 3: Commit**

```bash
git add html/core/JVM_SSR.md
git commit -m "docs(html): document jvm ssr usage and limitations"
```

---

## Self-Review notes

- **Spec coverage:** §2 approach → Phase 0–3; §3 six coupling points → element types (2.1), applier (1.1–1.4), factory (2.2), attrs application (2.3), prop/ref inert (2.3), events + renderComposable (2.4, 3.1); §4 architecture → matches; §5 testing → Phase 4; §7 risks → documented in 4.3 + inert JVM actuals in 2.3.
- **Known unknowns flagged inline** (not placeholders): exact kotlinx.html 0.11.0 `Tag` constructor (3.2 Step 3), multiple-typealias-to-one-class compile form (2.1 Step 3), `DOMScope.scopeElement` root-wrapper handling (3.3 Step 3). Each has a concrete fallback and a validation step.
- **Type consistency:** `NativeNode`, `NativeNodeWrapper`, `JvmElement`/`JvmTextNode`/`JvmRootNode`, `DomElement`, `serializeToHtml`, `renderToString`, `createNativeElement` used consistently across tasks.
