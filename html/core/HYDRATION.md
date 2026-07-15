# Compose HTML hydration PoC

Compose HTML can reuse HTML rendered by `renderToString` on the JVM as the initial browser DOM.
Hydration attaches event listeners and Compose state to those existing nodes, then continues with
normal client-side recomposition.

This is an experimental proof of concept for deterministic HTML trees. It is not yet a general
production hydration or resumability system.

## Usage

Define the page UI in code available to both JVM and JS targets:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text

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

Render it on the JVM and place the result inside a stable root element:

```kotlin
import org.jetbrains.compose.web.renderToString

val markup = renderToString { Counter() }
val page = """
    <!doctype html>
    <html>
      <body>
        <div id="root">$markup</div>
        <script src="/app.js"></script>
      </body>
    </html>
""".trimIndent()
```

After that markup is present in the browser, run the same composable with the same initial inputs:

```kotlin
import org.jetbrains.compose.web.hydrateComposable

val composition = hydrateComposable("root") {
    Counter()
}
```

The existing `<div>`, `<button>`, and text node are retained. Clicking the button updates the
existing text node through normal Compose recomposition.

`hydrateComposable` returns a live `Composition`. Dispose it when the mounted UI is no longer
needed:

```kotlin
composition.dispose()
```

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
matching. Server and browser initial compositions must register exactly the same keys: blank or
active duplicate keys, missing payload keys, and unconsumed payload keys fail hydration. Payload
discovery, HTML embedding, escaping, CSP handling, and HTTP transport are outside Compose HTML's
state API.

## API

The JS target provides three overloads in `org.jetbrains.compose.web`:

```kotlin
fun <TElement : Element> hydrateComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition

fun <TElement : Element> hydrateComposable(
    root: TElement,
    stateJson: String,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition

fun hydrateComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition
```

The element overloads are useful when the caller already has a DOM reference or needs to provide a
custom frame clock. The state-aware overload accepts raw JSON from `renderToStringWithState`. The
ID overload follows `renderComposable(rootElementId, ...)`: a missing ID fails rather than creating
a root. There is no body-specific hydration overload in the PoC.

## Required server/client flow

Hydration requires this sequence:

1. JVM renders a composable with `renderToString`.
2. Server places that markup inside a browser root element.
3. Browser parses the HTML before the JS entry point runs.
4. JS calls `hydrateComposable` on the root with the same composable and initial inputs.
5. Initial composition claims existing DOM children by parent and position.
6. After matching succeeds, later recompositions use normal DOM insert, remove, move, and update
   behavior.

In this state-unaware flow, no Compose state is transferred from JVM to JS. `remember` initializes
independently in the browser. Application data needed to reproduce the initial tree must therefore
be supplied to both targets by the application. The state-aware APIs described above can instead
transfer values registered explicitly with `rememberHydratableState`.

## What works

Within the supported deterministic HTML subset, hydration supports:

- Reusing existing HTML element and text node identities.
- Nested ordinary HTML elements.
- Attributes, classes, inline styles, DOM properties, and `ref` effects through the existing JS
  update path.
- Event listeners such as `onClick`.
- Synchronous initial `remember` state.
- Text, attribute, class, and style updates after hydration.
- Conditional insertion and removal after hydration.
- Normal keyed move behavior after hydration.
- Explicit `Composition.dispose()` lifecycle.

Hydration does not insert, replace, remove, or move matched nodes during the initial composition.
Attribute and property updaters may still write client values to those nodes.

## Matching rules

The PoC uses no hydration IDs, data attributes, or comment markers. It matches every Compose node
to the next DOM child under the current parent.

An element matches only when:

- The existing node is an element.
- It uses the HTML namespace.
- Its `localName` equals the expected lowercase tag name.

A text node matches only when its parsed `data` exactly equals the initial Compose text. Browser
entity decoding happens before comparison, so this server HTML:

```html
<span>Tom &amp; Jerry</span>
```

matches Compose text `"Tom & Jerry"`.

Every child must be consumed. Missing children and extra children are mismatches. Attributes are
not compared because browsers normalize class order, styles, boolean attributes, and DOM
properties; the normal JS updater applies client values to the claimed element instead.

## Mismatch behavior

A divergent tree throws `HydrationMismatchException` synchronously. Its message includes the DOM
path, expected node, and actual node:

```text
Hydration mismatch at root/div[0]/button[1]/text()[0]:
expected text "Count: 0", found text "Count: 1"
```

Other mismatch cases include:

```text
expected <span>, found <div>
expected <button>, found no node
expected end of node, found extra <p>
expected <svg>, found <svg> in namespace http://www.w3.org/2000/svg
```

The PoC deliberately has no fallback that clears the root and performs a client render. A fallback
would hide inconsistent server/client inputs.

On failure, hydration:

- Stops matching.
- Deactivates the hydration session before disposal effects run.
- Preserves the SSR root instead of clearing it.
- Removes event listeners attached by the failed hydration attempt.
- Disposes the partial composition and cancels its recomposer.
- Rethrows the original mismatch or composable exception, even if cleanup also throws.

Cleanup is best-effort, not transactional. Attributes, styles, text, properties, or user `ref`
side effects applied before a later mismatch are not rolled back. Treat the failed root as unusable
and reload or show a fatal error.

## Current limitations

1. **Identical synchronous initial trees are required.** Server and browser must use the same
   composable and produce the same initial tree. Explicitly restored state may let their fallback
   inputs differ; other inputs that affect the tree must still match.

2. **Only explicitly registered state is serialized.** Values created through
   `rememberHydratableState` can cross the SSR boundary when using `renderToStringWithState` and
   the state-aware `hydrateComposable` overload. Ordinary `remember`, slot-table state, page
   inputs, effects, coroutine state, and event handlers are not serialized. Keys must be nonblank,
   page-global, and identical across the server and browser initial compositions.

3. **No async initial tree changes.** `LaunchedEffect`, `produceState`, or other asynchronous work
   must not change the SSR tree before hydration finishes. Such changes can make JVM output differ
   from the browser's synchronous initial composition.

4. **Browser-normalized structures are unsupported.** For example, a browser can insert
   `<tbody>` into `<table><tr>...</tr></table>`, so the parsed DOM no longer matches the Compose
   tree positionally.

5. **SVG hydration is unsupported.** SVG nodes use a different namespace. SVG can still be rendered
   by JVM SSR, but this PoC rejects it during hydration.

6. **Raw-text elements are unsupported.** Hydration of `<script>` and `<style>` is outside the PoC.

7. **Empty or formatting text is strict.** Browsers may discard empty text nodes. Added comments or
   formatting whitespace create unexpected nodes and fail matching.

8. **The DOM must remain unchanged before hydration.** Browser extensions, scripts, or application
   code that edits the root can cause a mismatch.

9. **Only one hydration session can run at a time.** Nested or concurrent hydration calls fail.

10. **No mismatch recovery or subtree replacement.** Any mismatch aborts the whole initial
    hydration attempt.

11. **SSR-controlled input values remain limited.** Values written through JVM-inert `prop`
    operations do not appear in server markup. Hydration can apply the client property, but the
    static server HTML still follows the limitation documented in [JVM_SSR.md](JVM_SSR.md).

## Testing hydration

A browser test should verify identity, not only equivalent HTML:

```kotlin
val root = document.createElement("div")
root.innerHTML = "<div><button>Count: 0</button></div>"

val existingDiv = root.firstChild
val existingButton = existingDiv!!.firstChild
val existingText = existingButton!!.firstChild

val composition = hydrateComposable(root) {
    Counter()
}

assertSame(existingDiv, root.firstChild)
assertSame(existingButton, root.firstChild!!.firstChild)
assertSame(existingText, root.firstChild!!.firstChild!!.firstChild)

composition.dispose()
```

The repository test suite contains broader examples:

- `html/core/src/jsTest/kotlin/HydrationJsTest.kt`
- `html/core/src/commonTest/kotlin/HydrationComposables.kt`
- `html/core/src/jvmTest/kotlin/HydrationFixtureJvmTest.kt`
- `html/internal-html-core-runtime/src/jsTest/kotlin/HydrationSessionTest.kt`
- `html/internal-html-core-runtime/src/jvmTest/kotlin/DomApplierStrategyTest.kt`

For JVM rendering setup and limitations, see [JVM_SSR.md](JVM_SSR.md). Internal architecture and
scope decisions are documented in the [hydration design spec][hydration-design].

[hydration-design]: ../../docs/superpowers/specs/2026-07-13-compose-html-hydration-poc-design.md
