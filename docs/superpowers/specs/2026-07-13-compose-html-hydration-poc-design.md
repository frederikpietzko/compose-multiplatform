# Compose HTML Hydration PoC

**Date:** 2026-07-13
**Status:** Approved design
**Goal:** Reuse HTML rendered by Compose HTML on the JVM as the initial browser DOM, attach Compose HTML event handling and state, and continue normal client-side recomposition without replacing the server-rendered nodes.

---

## 1. Scope

The PoC proves true, marker-free hydration for a narrow deterministic HTML subset.

The same composable and identical initial inputs run on both platforms:

1. JVM calls `renderToString` and sends HTML.
2. Browser parses that HTML into DOM nodes.
3. JS calls `hydrateComposable` with the same composable and inputs.
4. Initial JS composition claims and reuses the existing DOM nodes by position.
5. Existing JS attribute, property, ref, and event update paths target the claimed nodes.
6. After initial hydration, normal Compose recomposition inserts, removes, moves, and updates DOM nodes.

PoC success requires element and text node identity to remain unchanged during initial hydration. It does not serialize Compose state or page inputs. `remember` initializes again in the browser.

### Supported golden path

- Ordinary HTML elements whose parsed DOM structure matches the SSR node tree
- Text nodes
- Attributes, classes, and inline styles
- Event listeners
- Synchronous initial `remember` state
- Text and attribute updates after hydration
- Element insertion and removal after hydration

### Out of scope

- Hydration markers or IDs
- Compose state serialization or resumability
- Page-input JSON transport
- Mismatch fallback or subtree replacement
- Browser parser normalization, including implicit `<tbody>` insertion
- SVG and namespace transitions
- Raw-text `<script>` and `<style>` hydration
- Empty text nodes discarded by the browser
- DOM changed by scripts, extensions, or users before hydration
- Nested or concurrent hydration calls
- Async effects that change the initial SSR tree
- Production-grade rollback after a mismatch

---

## 2. Public API

Add JS-only overloads beside `renderComposable`:

```kotlin
fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition

fun hydrateComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition
```

`hydrateComposable` returns the live `Composition`. Callers dispose it like a composition returned by `renderComposable`.

A missing `rootElementId` follows the existing `renderComposable(rootElementId, ...)` behavior. This PoC does not add a body-specific hydration overload.

---

## 3. Architecture

### 3.1 Hydration session

A JS-only `HydrationSession` owns matching state for one synchronous initial composition. It stores a stack of frames. Each frame contains:

- Existing DOM parent
- Index of the next unclaimed child
- Human-readable path used in mismatch errors

Its conceptual operations are:

```kotlin
claimElement(tagName: String): Element
claimText(expectedText: String): Text
enter(node: Node)
leave(node: Node)
finish()
abort()
```

`claimElement` and `claimText` consume the next child of the current frame. `enter` pushes the claimed node while Compose emits its children. `leave` verifies that the node has no unmatched children and pops its frame. `finish` performs the same completeness check for the hydration root.

An internal active-session holder lets existing JS node factory actuals find the current session. It is installed only around synchronous initial `setContent` and always cleared in `finally`. Starting nested or concurrent hydration while a session is active throws immediately.

### 3.2 Hydration-aware node factories

Existing JS factories become mode-aware:

```kotlin
actual fun createNativeElement(tagName: String): NativeNode
actual fun createNativeText(text: String): NativeNode
```

Without an active hydration session they retain current behavior and create new browser nodes. With an active session they claim and return the next matching existing DOM node.

`Text(value)` changes its initial factory call from `createNativeText("")` to `createNativeText(value)`. This lets hydration validate text before the updater writes it. Later text changes still use the existing `setTextData` updater path. On JVM this change only initializes the temporary text model with its final initial value and does not change serialized output.

### 3.3 DomApplier strategy

`DomApplier` needs to expose Compose tree traversal to the hydration session. Node factories know the requested tag or text, but without applier traversal they cannot know whether the next node belongs inside the current element or beside it.

Add an internal strategy boundary:

```kotlin
internal interface DomApplierStrategy {
    fun enter(node: NativeNode)
    fun leave(node: NativeNode)

    fun reconcileChild(
        parent: NativeNode,
        index: Int,
        child: NativeNode
    )

    fun finish()
}
```

Two implementations exist:

- `DefaultDomApplierStrategy` preserves current behavior. Traversal callbacks are no-ops and `reconcileChild` calls the existing indexed child insertion operation.
- `HydrationDomApplierStrategy` forwards traversal to `HydrationSession`. During initial hydration, `reconcileChild` verifies that a claimed child already occupies the expected position and does not reinsert it. After successful initial hydration it delegates child reconciliation to default behavior, so later recompositions work normally.

`DomApplier.down` and `DomApplier.up` invoke `enter` and `leave`. `insertBottomUp` invokes `reconcileChild`. Initial composition completion invokes `finish` before the active session closes.

Remove and move behavior remains unchanged. Initial composition is expected only to claim existing nodes; later recompositions use ordinary remove and move operations.

### 3.4 Composition wiring

`renderComposableRoot` accepts an optional `DomApplierStrategy`, defaulting to normal rendering. Existing callers remain source-compatible.

`hydrateComposable` performs this sequence:

```text
validate no session is active
create HydrationSession(root)
create HydrationDomApplierStrategy(session)
install active session
create recomposer and composition
set content synchronously
finish matching
mark strategy hydrated
clear active session
return composition
```

If matching or user composition throws, it aborts the session, clears the active holder, prevents disposal from clearing the SSR root, disposes partial composition, and rethrows.

---

## 4. Matching protocol

Matching is positional and marker-free. Every Compose node consumes exactly the next DOM child under the current matched parent.

### Element matching

An expected element matches only when:

- Existing node has `Node.ELEMENT_NODE` type
- Existing node uses the HTML namespace
- Existing node `localName` equals the requested lowercase tag name

Attributes are not part of initial matching. Browsers normalize attributes, class order, styles, boolean attributes, and DOM properties differently from serialized HTML. Existing element updater functions apply client attrs, classes, styles, properties, refs, and listeners directly to the claimed element.

### Text matching

An expected text node matches only when:

- Existing node has `Node.TEXT_NODE` type
- Existing node `data` exactly equals the initial Compose text value

Comparison uses parsed text. For example, SSR markup `Tom &amp; Jerry` matches Compose text `Tom & Jerry`.

### Completeness

Leaving each matched element verifies that all its existing children were consumed. Finishing hydration performs the same check on the root. Missing and extra nodes are both mismatches.

The PoC does not skip comments or whitespace text. Compact `renderToString` output does not add formatting whitespace, so unexpected whitespace indicates divergent or externally modified markup.

---

## 5. Mismatch handling

A mismatch throws `HydrationMismatchException`. Its message includes:

- DOM path
- Expected node kind and value or tag
- Actual node kind and value or tag
- Missing or extra-node status where relevant

Example:

```text
Hydration mismatch at root/div[0]/button[1]:
expected text "Count: 0", found text "Count: 1"
```

Other expected forms include:

```text
expected <span>, found <div>
expected <button>, found no node
expected end of <div>, found extra <p>
expected HTML element, found SVG element
```

No automatic client-render fallback runs. A fallback would hide server/client divergence and weaken the PoC.

Failure cleanup is best-effort:

1. Mark the session aborted.
2. Stop matching immediately.
3. Suppress root clearing while disposing the partial composition.
4. Remove event listeners installed on tracked claimed elements.
5. Clear active hydration state in `finally`.
6. Rethrow the original mismatch.

Equivalent attrs, classes, styles, text, or properties already applied to earlier matched nodes are not rolled back. After mismatch, the root must be treated as unusable and the application should reload or show a fatal error. Full transactional DOM rollback is out of scope.

---

## 6. Initial state and effects

No Compose state crosses the server/client boundary. Server and browser independently initialize `remember` using identical composable inputs.

The PoC supports synchronous initial composition only. JVM `renderToString` currently lets scheduled effects settle before serialization, while browser hydration closes its matching session after synchronous `setContent`. Therefore `LaunchedEffect`, `produceState`, or other async work must not alter the SSR tree for hydrated PoC pages.

After hydration succeeds, effects and state changes may trigger ordinary recomposition. Nodes introduced after the strategy switches to normal mode are created and inserted normally.

---

## 7. Test strategy

### 7.1 Golden-path counter

Use one shared composable:

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }

    Div {
        Button(attrs = { onClick { count++ } }) {
            Text("Count: $count")
        }
    }
}
```

JVM test verifies:

```html
<div><button>Count: 0</button></div>
```

JS test:

1. Assign the JVM-expected fixture to a root's `innerHTML`.
2. Save references to existing `div`, `button`, and text nodes.
3. Call `hydrateComposable(root) { Counter() }`.
4. Assert all saved node identities remain unchanged.
5. Dispatch a click.
6. Await recomposition.
7. Assert the same text node now contains `Count: 1`.

The fixture and composable live in shared test sources so JVM rendering and JS hydration cannot silently use different component definitions.

### 7.2 Additional JS tests

- Reuse nested element and text identities
- Apply and subsequently update attrs, classes, and inline styles
- Attach click listener exactly once
- Insert a conditional node after hydration
- Remove a node after hydration
- Reject wrong tag
- Reject wrong text
- Reject missing child
- Reject extra child
- Reject SVG namespace
- Preserve root nodes when hydration fails
- Clear session after exceptions
- Allow later normal `renderComposable` after failed hydration

### 7.3 Regression tests

- Existing `renderComposable` JS tests remain green
- Existing JVM `renderToString` tests remain green
- Existing shared API parity tests remain green

---

## 8. Completion criteria

PoC is complete when automated tests demonstrate that:

- One shared composable renders deterministic initial HTML on JVM.
- JS hydration reuses parsed element and text node identities.
- Initial hydration does not insert, replace, remove, or move matched nodes.
- Event listeners work on reused nodes.
- State changes trigger normal recomposition after hydration.
- New and removed post-hydration nodes use existing applier behavior.
- Divergent initial trees fail synchronously with actionable paths.
- Existing JS rendering and JVM SSR behavior remain unchanged.
