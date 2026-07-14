# Compose HTML Hydration PoC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reuse deterministic JVM-rendered HTML nodes during the initial JS Compose HTML composition, attach events, and continue normal recomposition without replacing those nodes.

**Architecture:** A JS-only `HydrationSession` claims parsed DOM children by parent and index while existing element/text factories run. A `DomApplierStrategy` exposes Compose traversal and child reconciliation to that session; hydration verifies already-attached nodes during the initial composition, then permanently delegates to normal applier behavior. Mismatches throw synchronously, clear active hydration state, remove newly attached listeners, and preserve the SSR root.

**Tech Stack:** Kotlin Multiplatform, Compose Runtime `ControlledComposition`/`AbstractApplier`, Kotlin/JS browser DOM, Karma browser tests, Kotlin/JVM SSR tests.

## Global Constraints

- Hydration is JS-only; existing JVM `renderToString` output and APIs remain compatible.
- Match nodes positionally without hydration markers.
- Match HTML element namespace/tag and exact parsed text; do not compare attributes.
- Reuse element and text identities during initial hydration.
- Fail fast; no client-render fallback or subtree replacement.
- Support only deterministic synchronous initial trees; async effects that alter initial structure remain unsupported.
- SVG, raw-text elements, implicit browser nodes such as `<tbody>`, comments, and unexpected whitespace remain unsupported.
- Permit only one active, non-nested hydration session.
- After successful initial hydration, all insert/remove/move/update behavior uses existing DOM runtime behavior.

---

## File Structure

### New production files

- `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/DomApplierStrategy.kt` — common strategy seam and default behavior.
- `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/HydrationMismatchException.kt` — public JS mismatch exception.
- `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/HydrationSession.kt` — DOM cursor, mismatch diagnostics, active-session bridge, and listener rollback tracking.
- `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/HydrationDomApplierStrategy.kt` — initial reuse versus post-hydration normal reconciliation.

### Modified production files

- `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/DomApplier.kt` — delegate traversal, reconciliation, and clearing through strategy.
- `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/RenderComposableRoot.kt` — accept strategy and expose partially created composition for cleanup.
- `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/renderComposable.kt` — add public `hydrateComposable` overloads and lifecycle handling.
- `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/dom/ElementBuilder.js.kt` — claim existing nodes while hydration session is active.
- `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/dom/Elements.kt` — initialize text factories with expected text.
- `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/ElementUpdater.js.kt` — report attached listeners for mismatch cleanup.
- `html/core/JVM_SSR.md` — replace “hydration is out of scope” with PoC usage and boundaries.

### New and modified test files

- `html/internal-html-core-runtime/src/jvmTest/kotlin/DomApplierStrategyTest.kt`
- `html/internal-html-core-runtime/src/jsTest/kotlin/HydrationSessionTest.kt`
- `html/core/src/commonTest/kotlin/HydrationComposables.kt`
- `html/core/src/jvmTest/kotlin/HydrationFixtureJvmTest.kt`
- `html/core/src/jsTest/kotlin/HydrationJsTest.kt`

---

### Task 1: Add `DomApplierStrategy`

**Files:**
- Create: `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/DomApplierStrategy.kt`
- Modify: `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/internal/runtime/DomApplier.kt`
- Modify: `html/internal-html-core-runtime/src/commonMain/kotlin/org/jetbrains/compose/web/RenderComposableRoot.kt`
- Create: `html/internal-html-core-runtime/src/jvmTest/kotlin/DomApplierStrategyTest.kt`

**Interfaces:**
- Produces: `DomApplierStrategy.enter`, `leave`, `reconcileChild`, `clear`, and `finish`.
- Produces: `DefaultDomApplierStrategy` preserving current runtime behavior.
- Produces: optional `strategy` and `onCompositionCreated` arguments on `renderComposableRoot`.
- Consumes: existing `NativeNode` indexed mutation functions.

- [ ] **Step 1: Write failing strategy-delegation tests**

Create `DomApplierStrategyTest.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.jetbrains.compose.web.internal.runtime.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ComposeWebInternalApi::class)
class DomApplierStrategyTest {
    @Test
    fun delegatesTraversalReconciliationAndClear() {
        val root = JvmRootNode()
        val child = JvmElement("div")
        val calls = mutableListOf<String>()
        val strategy = object : DomApplierStrategy {
            override fun enter(node: NativeNode) { calls += "enter" }
            override fun leave(node: NativeNode) { calls += "leave" }
            override fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode) {
                assertSame(root, parent)
                calls += "reconcile:$index"
            }
            override fun clear(root: NativeNode) { calls += "clear" }
            override fun finish() { calls += "finish" }
        }
        val applier = DomApplier(NativeNodeWrapper(root), strategy)
        val wrapper = NativeNodeWrapper(child)

        applier.down(wrapper)
        applier.up()
        applier.insertBottomUp(0, wrapper)
        applier.clear()
        strategy.finish()

        assertEquals(
            listOf("enter", "leave", "reconcile:0", "clear", "finish"),
            calls
        )
    }

    @Test
    fun defaultStrategyPreservesInsertionAndClearing() {
        val root = JvmRootNode()
        val child = JvmElement("div")
        val applier = DomApplier(NativeNodeWrapper(root))

        applier.insertBottomUp(0, NativeNodeWrapper(child))
        assertSame(child, root.children.single())

        applier.clear()
        assertEquals(emptyList(), root.children)
    }
}
```

- [ ] **Step 2: Run test and verify compilation fails**

Run:

```bash
cd html
./gradlew :internal-html-core-runtime:jvmTest --tests DomApplierStrategyTest
```

Expected: FAIL because `DomApplierStrategy` and two-argument `DomApplier` do not exist.

- [ ] **Step 3: Add strategy seam and default implementation**

Create `DomApplierStrategy.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

@ComposeWebInternalApi
interface DomApplierStrategy {
    fun enter(node: NativeNode)
    fun leave(node: NativeNode)
    fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode)
    fun clear(root: NativeNode)
    fun finish()
}

@ComposeWebInternalApi
object DefaultDomApplierStrategy : DomApplierStrategy {
    override fun enter(node: NativeNode) = Unit
    override fun leave(node: NativeNode) = Unit

    override fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode) {
        parent.insertChild(index, child)
    }

    override fun clear(root: NativeNode) {
        root.clearChildren()
    }

    override fun finish() = Unit
}
```

Modify `DomApplier` to delegate while retaining `AbstractApplier` stack behavior:

```kotlin
@ComposeWebInternalApi
class DomApplier(
    root: NativeNodeWrapper,
    private val strategy: DomApplierStrategy = DefaultDomApplierStrategy
) : AbstractApplier<NativeNodeWrapper>(root) {

    override fun down(node: NativeNodeWrapper) {
        strategy.enter(node.node)
        super.down(node)
    }

    override fun up() {
        strategy.leave(current.node)
        super.up()
    }

    override fun insertTopDown(index: Int, instance: NativeNodeWrapper) = Unit

    override fun insertBottomUp(index: Int, instance: NativeNodeWrapper) {
        strategy.reconcileChild(current.node, index, instance.node)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        strategy.clear(root.node)
    }
}
```

Extend `renderComposableRoot` arguments and applier construction:

```kotlin
fun <TElement : NativeNode> renderComposableRoot(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock,
    dispatcher: CoroutineDispatcher,
    strategy: DomApplierStrategy = DefaultDomApplierStrategy,
    onCompositionCreated: (Composition) -> Unit = {},
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    val context = monotonicFrameClock + dispatcher
    val recomposer = Recomposer(context)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = ControlledComposition(
        applier = DomApplier(NativeNodeWrapper(root), strategy),
        parent = recomposer
    )
    onCompositionCreated(composition)
    val scope = object : DOMScope<TElement> {
        override val DisposableEffectScope.scopeElement: TElement
            get() = root
    }
    composition.setContent @Composable {
        content(scope)
    }
    return composition
}
```

Update KDoc with strategy and cleanup-callback parameters. Do not change existing JS call sites yet; defaults preserve them.

- [ ] **Step 4: Run focused and regression tests**

Run:

```bash
cd html
./gradlew :internal-html-core-runtime:jvmTest :core:jvmTest
```

Expected: PASS. Existing render and JVM SSR behavior unchanged.

- [ ] **Step 5: Commit**

```bash
git add html/internal-html-core-runtime/src/commonMain \
  html/internal-html-core-runtime/src/jvmTest

git commit -m "refactor(html): add DOM applier strategy"
```

---

### Task 2: Implement positional `HydrationSession`

**Files:**
- Create: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/HydrationMismatchException.kt`
- Create: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/HydrationSession.kt`
- Create: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/HydrationDomApplierStrategy.kt`
- Create: `html/internal-html-core-runtime/src/jsTest/kotlin/HydrationSessionTest.kt`

**Interfaces:**
- Consumes: `DomApplierStrategy` from Task 1.
- Produces: `HydrationMismatchException`.
- Produces: `claimHydrationElement(tagName)` and `claimHydrationText(text)` bridge functions for `:core`.
- Produces: `withHydrationSession(session) { ... }` for public API wiring.
- Produces: `HydrationDomApplierStrategy`, which reuses initially and delegates after `finish()`.

- [ ] **Step 1: Write failing matching tests**

Create browser-DOM tests covering nested matching, exact text, wrong tag, extra child, and nested-session rejection:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal.runtime

import kotlinx.browser.document
import org.jetbrains.compose.web.HydrationMismatchException
import org.w3c.dom.Element
import org.w3c.dom.Text
import kotlin.test.*

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
        assertSame((section as Element).firstChild, span)
        assertSame((span as Element).firstChild, text)
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
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
cd html
./gradlew :internal-html-core-runtime:jsBrowserTest
```

Expected: FAIL because hydration session types do not exist.

- [ ] **Step 3: Implement mismatch and cursor state**

Create `HydrationMismatchException.kt`:

```kotlin
/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

class HydrationMismatchException(message: String) : IllegalStateException(message)
```

Create `HydrationSession.kt`. Use one frame per currently entered node and record each claimed path:

```kotlin
package org.jetbrains.compose.web.internal.runtime

import org.jetbrains.compose.web.HydrationMismatchException
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.events.EventTarget

private const val HTML_NAMESPACE = "http://www.w3.org/1999/xhtml"

internal class HydrationSession(root: Node) {
    private data class Frame(
        val node: Node,
        val path: String,
        var nextChild: Int = 0
    )

    private val frames = mutableListOf(Frame(root, "root"))
    private val paths = mutableMapOf<Node, String>()
    private val listeners = mutableMapOf<EventTarget, List<NamedEventListener>>()
    var aborted: Boolean = false
        private set

    private val current: Frame get() = frames.last()

    fun claimElement(tagName: String): Element {
        val index = current.nextChild++
        val path = "${current.path}/${tagName.lowercase()}[$index]"
        val actual = current.node.childNodes.item(index)
            ?: mismatch(path, "expected <$tagName>, found no node")
        if (actual.nodeType.toInt() != 1) {
            mismatch(path, "expected <$tagName>, found ${describe(actual)}")
        }
        val element = actual as Element
        if (element.namespaceURI != HTML_NAMESPACE || element.localName != tagName.lowercase()) {
            mismatch(path, "expected <$tagName>, found ${describe(element)}")
        }
        paths[element] = path
        return element
    }

    fun claimText(expected: String): Text {
        val index = current.nextChild++
        val path = "${current.path}/text()[$index]"
        val actual = current.node.childNodes.item(index)
            ?: mismatch(path, "expected text ${expected.quote()}, found no node")
        if (actual.nodeType.toInt() != 3) {
            mismatch(path, "expected text ${expected.quote()}, found ${describe(actual)}")
        }
        val text = actual as Text
        if (text.data != expected) {
            mismatch(path, "expected text ${expected.quote()}, found text ${text.data.quote()}")
        }
        paths[text] = path
        return text
    }

    fun enter(node: Node) {
        frames += Frame(node, paths[node] ?: mismatch(current.path, "entered unclaimed node"))
    }

    fun leave(node: Node) {
        val frame = current
        if (frame.node !== node) mismatch(frame.path, "left unexpected node")
        verifyComplete(frame)
        frames.removeAt(frames.lastIndex)
    }

    fun verifyChild(parent: Node, index: Int, child: Node) {
        if (parent.childNodes.item(index) !== child) {
            mismatch(paths[child] ?: current.path, "claimed child moved before reconciliation")
        }
    }

    fun finish() {
        if (frames.size != 1) mismatch(current.path, "composition ended inside node")
        verifyComplete(frames.single())
    }

    fun recordListeners(target: EventTarget, value: List<NamedEventListener>) {
        listeners[target] = value
    }

    fun abort() {
        aborted = true
        listeners.forEach { (target, value) ->
            value.forEach { target.removeEventListener(it.name, it) }
        }
        listeners.clear()
    }

    private fun verifyComplete(frame: Frame) {
        val extra = frame.node.childNodes.item(frame.nextChild) ?: return
        mismatch(frame.path, "expected end of node, found extra ${describe(extra)}")
    }

    private fun describe(node: Node): String = when (node.nodeType.toInt()) {
        1 -> {
            val element = node as Element
            if (element.namespaceURI == HTML_NAMESPACE) "<${element.localName}>"
            else "<${element.localName}> in namespace ${element.namespaceURI}"
        }
        3 -> "text ${(node as Text).data.quote()}"
        else -> "node type ${node.nodeType}"
    }

    private fun String.quote() = buildString {
        append('"')
        this@quote.forEach { append(if (it == '\n') "\\n" else it) }
        append('"')
    }

    private fun mismatch(path: String, detail: String): Nothing {
        throw HydrationMismatchException("Hydration mismatch at $path: $detail")
    }
}

private var activeHydrationSession: HydrationSession? = null

internal fun <T> withHydrationSession(session: HydrationSession, block: () -> T): T {
    check(activeHydrationSession == null) { "A hydration session is already active" }
    activeHydrationSession = session
    return try {
        block()
    } finally {
        activeHydrationSession = null
    }
}

@ComposeWebInternalApi
fun claimHydrationElement(tagName: String): NativeNode? =
    activeHydrationSession?.claimElement(tagName)

@ComposeWebInternalApi
fun claimHydrationText(text: String): NativeNode? =
    activeHydrationSession?.claimText(text)

@ComposeWebInternalApi
fun recordHydrationEventListeners(node: NativeNode, listeners: List<NamedEventListener>) {
    activeHydrationSession?.recordListeners(node as EventTarget, listeners)
}
```

Keep `HydrationSession` and `withHydrationSession` module-internal. Cross-module bridge functions are public only because `:core` is a separate Kotlin module; guard them with existing `@ComposeWebInternalApi`.

- [ ] **Step 4: Implement hydration applier strategy**

Create `HydrationDomApplierStrategy.kt`:

```kotlin
package org.jetbrains.compose.web.internal.runtime

internal class HydrationDomApplierStrategy(
    private val session: HydrationSession
) : DomApplierStrategy {
    private var hydrating = true

    override fun enter(node: NativeNode) {
        if (hydrating) session.enter(node)
    }

    override fun leave(node: NativeNode) {
        if (hydrating) session.leave(node)
    }

    override fun reconcileChild(parent: NativeNode, index: Int, child: NativeNode) {
        if (hydrating) session.verifyChild(parent, index, child)
        else DefaultDomApplierStrategy.reconcileChild(parent, index, child)
    }

    override fun clear(root: NativeNode) {
        if (!(hydrating && session.aborted)) {
            DefaultDomApplierStrategy.clear(root)
        }
    }

    override fun finish() {
        session.finish()
        hydrating = false
    }
}
```

Add standard Apache license headers to both production files before committing.

- [ ] **Step 5: Run browser and JVM runtime tests**

Run:

```bash
cd html
./gradlew :internal-html-core-runtime:jsBrowserTest \
  :internal-html-core-runtime:jvmTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime \
  html/internal-html-core-runtime/src/jsTest/kotlin/HydrationSessionTest.kt

git commit -m "feat(html): add hydration DOM matcher"
```

---

### Task 3: Wire factories and public hydration API

**Files:**
- Modify: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/renderComposable.kt`
- Modify: `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/dom/ElementBuilder.js.kt`
- Modify: `html/core/src/commonMain/kotlin/org/jetbrains/compose/web/dom/Elements.kt:421-428`
- Create: `html/core/src/jsTest/kotlin/HydrationJsTest.kt`

**Interfaces:**
- Consumes: active-session bridge and `HydrationDomApplierStrategy` from Task 2.
- Produces: public `hydrateComposable(root, ...)` and `hydrateComposable(rootElementId, ...)`.
- Produces: identity-preserving static element/text hydration.

- [ ] **Step 1: Write failing static hydration tests**

Create initial `HydrationJsTest.kt`:

```kotlin
package org.jetbrains.compose.web.core.tests

import kotlinx.browser.document
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Text as DomText
import kotlin.test.*

class HydrationJsTest {
    @Test
    fun reusesExistingElementAndTextNodes() {
        val root = document.createElement("div")
        root.innerHTML = "<div><span>Hello</span></div>"
        val div = root.firstChild
        val span = div!!.firstChild
        val text = span!!.firstChild

        hydrateComposable(root) {
            Div { Span { Text("Hello") } }
        }

        assertSame(div, root.firstChild)
        assertSame(span, root.firstChild!!.firstChild)
        assertSame(text, root.firstChild!!.firstChild!!.firstChild)
    }

    @Test
    fun supportsRootIdOverload() {
        val root = document.createElement("div") as HTMLElement
        root.id = "hydrate-root"
        root.innerHTML = "<div>Hello</div>"
        document.body!!.appendChild(root)
        try {
            val child = root.firstChild
            hydrateComposable("hydrate-root") { Div { Text("Hello") } }
            assertSame(child, root.firstChild)
        } finally {
            root.parentNode?.removeChild(root)
        }
    }

    @Test
    fun failsOnDifferentTextWithoutClearingRoot() {
        val root = document.createElement("div")
        root.innerHTML = "<span>Server</span>"
        val child = root.firstChild

        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(root) { Span { Text("Client") } }
        }

        assertSame(child, root.firstChild)
        assertEquals("<span>Server</span>", root.innerHTML)
    }
}
```

`HydrationMismatchException` is public because callers need to distinguish deterministic server/client mismatches from exceptions thrown by composable code.

- [ ] **Step 2: Run JS core test and verify failure**

Run:

```bash
cd html
./gradlew :core:jsBrowserTest
```

Expected: FAIL because `hydrateComposable` does not exist and factories still create new nodes.

- [ ] **Step 3: Make JS node factories hydration-aware**

Modify `ElementBuilder.js.kt`:

```kotlin
import org.jetbrains.compose.web.internal.runtime.claimHydrationElement
import org.jetbrains.compose.web.internal.runtime.claimHydrationText

actual fun createNativeElement(tagName: String): NativeNode =
    claimHydrationElement(tagName) ?: document.createElement(tagName)

actual fun createNativeText(text: String): NativeNode =
    claimHydrationText(text) ?: document.createTextNode(text)
```

Modify `Text` in `Elements.kt`:

```kotlin
@OptIn(ComposeWebInternalApi::class)
@Composable
fun Text(value: String) {
    ComposeNode<NativeNodeWrapper, DomApplier>(
        factory = { NativeNodeWrapper(createNativeText(value)) },
        update = {
            set(value) { value -> (node as DomText).setTextData(value) }
        },
    )
}
```

- [ ] **Step 4: Add public hydration entry points and failure lifecycle**

In `renderComposable.kt`, add:

```kotlin
@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()
    val session = HydrationSession(root)
    val strategy = HydrationDomApplierStrategy(session)
    var partialComposition: Composition? = null

    return try {
        withHydrationSession(session) {
            renderComposableRoot(
                root = root,
                monotonicFrameClock = monotonicFrameClock,
                dispatcher = JsMicrotasksDispatcher(),
                strategy = strategy,
                onCompositionCreated = { partialComposition = it },
                content = content
            ).also { strategy.finish() }
        }
    } catch (failure: Throwable) {
        session.abort()
        partialComposition?.dispose()
        throw failure
    }
}

fun hydrateComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition = hydrateComposable(
    root = document.getElementById(rootElementId)!!,
    content = content
)
```

Use the single public exception class created in Task 2. Add KDoc documenting deterministic identical input requirement, marker-free matching, unsupported parser normalization, and returned composition ownership.

- [ ] **Step 5: Run static hydration and regression tests**

Run:

```bash
cd html
./gradlew :core:jsBrowserTest \
  :core:jvmTest \
  :internal-html-core-runtime:jsBrowserTest \
  :internal-html-core-runtime:jvmTest
```

Expected: PASS. Static hydration keeps all saved node identities.

- [ ] **Step 6: Commit**

```bash
git add html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web \
  html/core/src/jsMain/kotlin/org/jetbrains/compose/web/dom/ElementBuilder.js.kt \
  html/core/src/commonMain/kotlin/org/jetbrains/compose/web/dom/Elements.kt \
  html/core/src/jsTest/kotlin/HydrationJsTest.kt

git commit -m "feat(html): hydrate existing DOM nodes"
```

---

### Task 4: Prove events, state updates, and mismatch cleanup

**Files:**
- Create: `html/core/src/commonTest/kotlin/HydrationComposables.kt`
- Create: `html/core/src/jvmTest/kotlin/HydrationFixtureJvmTest.kt`
- Modify: `html/core/src/jsTest/kotlin/HydrationJsTest.kt`
- Modify: `html/core/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/ElementUpdater.js.kt`
- Modify: `html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/HydrationSession.kt`

**Interfaces:**
- Consumes: public hydration API and active listener tracker.
- Produces: shared `HydrationCounter` fixture.
- Proves: listener attachment, normal post-hydration recomposition, insertion/removal, and listener rollback on mismatch.

- [ ] **Step 1: Add shared composable and JVM fixture test**

Create `HydrationComposables.kt`:

```kotlin
package org.jetbrains.compose.web.core.tests.hydration

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.*

@Composable
fun HydrationCounter() {
    var count by remember { mutableStateOf(0) }
    Div({
        classes(if (count == 0) "idle" else "active")
        style { property("color", if (count == 0) "red" else "green") }
    }) {
        Button({ onClick { count++ } }) {
            Text("Count: $count")
        }
        if (count % 2 == 1) {
            Span({ classes("result") }) { Text("Updated") }
        }
    }
}
```

Create `HydrationFixtureJvmTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Add failing interactive JS tests**

Add these imports to `HydrationJsTest.kt`:

```kotlin
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.jetbrains.compose.web.core.tests.hydration.HydrationCounter
import org.w3c.dom.events.MouseEvent
```

Append these tests:

```kotlin
@Test
fun hydratesEventThenUsesNormalRecomposition() = MainScope().promise {
    val root = document.createElement("div")
    root.innerHTML = "<div class=\"idle\" style=\"color:red;\"><button>Count: 0</button></div>"
    val div = root.firstChild as HTMLElement
    val button = div.firstChild as HTMLElement
    val text = button.firstChild as DomText

    hydrateComposable(root) { HydrationCounter() }
    button.dispatchEvent(MouseEvent("click"))
    delay(100)

    assertSame(div, root.firstChild)
    assertSame(button, div.firstChild)
    assertSame(text, button.firstChild)
    assertEquals("Count: 1", text.data)
    assertEquals("active", div.className)
    assertEquals("green", div.style.color)
    assertEquals("<span class=\"result\">Updated</span>", (div.lastChild as Element).outerHTML)

    button.dispatchEvent(MouseEvent("click"))
    delay(100)

    assertSame(text, button.firstChild)
    assertEquals("Count: 2", text.data)
    assertEquals(1, div.childNodes.length)
}

@Test
fun removesListenersWhenLaterMismatchAborts() {
    val root = document.createElement("div")
    root.innerHTML = "<button>Count: 0</button><span>Server</span>"
    var clicks = 0
    val button = root.firstChild as HTMLElement

    assertFailsWith<HydrationMismatchException> {
        hydrateComposable(root) {
            Button({ onClick { clicks++ } }) { Text("Count: 0") }
            Span { Text("Client") }
        }
    }

    button.dispatchEvent(MouseEvent("click"))
    assertEquals(0, clicks)
    assertSame(button, root.firstChild)
}
```

- [ ] **Step 3: Run focused tests and confirm listener-cleanup failure**

Run:

```bash
cd html
./gradlew :core:jvmTest :core:jsBrowserTest
```

Expected: JVM fixture PASS; interactive hydration may PASS; listener rollback test FAIL because `ElementUpdater.js.kt` has not recorded installed listeners.

- [ ] **Step 4: Track listeners while hydration is active**

In `ElementUpdater.js.kt`, import `recordHydrationEventListeners` and update the end of `applyEventListeners`:

```kotlin
listeners.forEach {
    addEventListener(it.name, it)
}
recordHydrationEventListeners(this, listeners)
```

`recordHydrationEventListeners` is a no-op without active hydration. During hydration, `HydrationSession.abort()` removes exactly those listener instances from tracked targets. Keep existing `PREVIOUS_LISTENERS_KEY` behavior unchanged.

Ensure `HydrationSession.abort()` is idempotent:

```kotlin
fun abort() {
    if (aborted) return
    aborted = true
    listeners.forEach { (target, value) ->
        value.forEach { target.removeEventListener(it.name, it) }
    }
    listeners.clear()
}
```

- [ ] **Step 5: Add remaining focused mismatch tests**

Add tests to `HydrationJsTest.kt` for missing child, extra child, SVG namespace, and session reset:

```kotlin
@Test
fun reportsMissingAndExtraChildren() {
    val missing = document.createElement("div")
    assertContains(
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(missing) { Span {} }
        }.message.orEmpty(),
        "found no node"
    )

    val extra = document.createElement("div")
    extra.innerHTML = "<span></span><p></p>"
    assertContains(
        assertFailsWith<HydrationMismatchException> {
            hydrateComposable(extra) { Span {} }
        }.message.orEmpty(),
        "found extra <p>"
    )
}

@Test
fun rejectsSvgNamespaceAndClearsSessionAfterFailure() {
    val svgRoot = document.createElement("div")
    svgRoot.innerHTML = "<svg></svg>"
    assertFailsWith<HydrationMismatchException> {
        hydrateComposable(svgRoot) { TagElement("svg", null, null) }
    }

    val validRoot = document.createElement("div")
    validRoot.innerHTML = "<span>ok</span>"
    hydrateComposable(validRoot) { Span { Text("ok") } }
    assertEquals("<span>ok</span>", validRoot.innerHTML)
}
```

Use `TagElement<DomElement>("svg", null, null)` exactly so generic type inference is deterministic.

- [ ] **Step 6: Run all HTML runtime/core tests**

Run:

```bash
cd html
./gradlew :internal-html-core-runtime:allTests :core:allTests
```

Expected: PASS. Browser tests prove identity reuse, one increment per click, text/class/style updates, post-hydration insertion and removal, mismatch diagnostics, preserved root, listener cleanup, and reusable session state.

- [ ] **Step 7: Commit**

```bash
git add html/core/src/commonTest/kotlin/HydrationComposables.kt \
  html/core/src/jvmTest/kotlin/HydrationFixtureJvmTest.kt \
  html/core/src/jsTest/kotlin/HydrationJsTest.kt \
  html/core/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/ElementUpdater.js.kt \
  html/internal-html-core-runtime/src/jsMain/kotlin/org/jetbrains/compose/web/internal/runtime/HydrationSession.kt

git commit -m "test(html): prove interactive hydration"
```

---

### Task 5: Document PoC API and run final verification

**Files:**
- Modify: `html/core/JVM_SSR.md`
- Verify only: `docs/superpowers/specs/2026-07-13-compose-html-hydration-poc-design.md`

**Interfaces:**
- Consumes: completed public API and verified limitations.
- Produces: user-facing hydration example and precise PoC boundary documentation.

- [ ] **Step 1: Replace static hydration limitation with usage**

Replace limitation 5 in `html/core/JVM_SSR.md` with:

```markdown
5. **Hydration is available as an experimental PoC on JS.** Render the same composable with
   identical initial inputs on JVM and JS, then hydrate its existing browser DOM:

   ```kotlin
   // JVM
   val markup = renderToString { Counter() }

   // JS, after markup is present under #root
   val composition = hydrateComposable("root") { Counter() }
   ```

   The PoC reuses existing HTML element and text nodes, attaches event listeners, and continues
   normal recomposition. Matching is marker-free and positional. It fails fast when tags, text,
   or child counts differ.

   Current PoC limits: no state serialization, async initial tree changes, parser-normalized
   structures such as implicit `<tbody>`, SVG, raw-text elements, comments, externally modified
   DOM, nested hydration, or mismatch fallback. Inputs must produce the same synchronous initial
   tree on server and client.
```

Add imports to example if document style requires a complete snippet:

```kotlin
import org.jetbrains.compose.web.hydrateComposable
```

- [ ] **Step 2: Verify public API compilation and all tests**

Run:

```bash
cd html
./gradlew :internal-html-core-runtime:allTests \
  :core:allTests \
  :svg:allTests
```

Expected: PASS.

If local Gradle fails before configuration with `25.0.3`, rerun the exact command with `JAVA_HOME=$(/usr/libexec/java_home -v 21)`. Record both the environment failure and successful JDK 21 output. Do not treat environment startup failure as test success.

- [ ] **Step 3: Check formatting, placeholders, and unexpected changes**

Run from repository root:

```bash
git diff --check
rg -n "T[B]D|T[O]DO|F[I]XME" \
  html/internal-html-core-runtime/src \
  html/core/src \
  html/core/JVM_SSR.md
git status --short
```

Expected: `git diff --check` emits nothing; search finds no hydration placeholders; status lists only intended hydration/doc files.

- [ ] **Step 4: Confirm completion criteria manually from test names**

Run:

```bash
git diff --stat HEAD~4..HEAD
git log -4 --oneline
```

Confirm commits cover strategy seam, matcher, public hydration, interactive proof, and docs. Confirm `HydrationJsTest` asserts identity with `assertSame`, not only HTML equality.

- [ ] **Step 5: Commit documentation**

```bash
git add html/core/JVM_SSR.md

git commit -m "docs(html): document hydration PoC"
```
