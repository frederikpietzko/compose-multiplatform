# Compose HTML on the JVM — Server-Side Rendering

**Date:** 2026-07-04
**Status:** Design / proposal
**Goal:** Let the same Compose HTML composables that today run only on Kotlin/JS
also render to an HTML **string** on the JVM (server-side rendering), so a
JVM backend (Ktor, Spring, etc.) can emit initial markup.

---

## 1. Problem statement

Compose HTML (`html/core`, `html/internal-html-core-runtime`, `html/svg`) is
**Kotlin/JS-only**. The real sources live under `src/jsMain`; the `jvm()`
target declared in `html/core/build.gradle.kts` is an empty stub with no
sources (present only for KMP metadata).

We want to author a composable tree once and render it to an HTML string on the
JVM. Interactivity (hydration) is **out of scope** for this design — JVM output
is one-shot static markup. Client interactivity continues to come from the
existing JS runtime if/when the page is later hydrated (a separate future
effort).

### Why it's not a small gap

- **27 of 65 `core` files import `org.w3c.dom.*` directly.** Those types
  (`Element`, `HTMLElement`, `HTMLInputElement`, `org.w3c.dom.events.Event`,
  `EventTarget`, `CSSStyleDeclaration`, …) are Kotlin/JS **external
  declarations**. They do not exist on the JVM.
- The **public API is generic over them**: `TagElement<TElement : Element>`,
  `AttrsScope<out TElement : Element>`, event listeners typed on
  `org.w3c.dom.events.*`, element factories returning `HTMLDivElement` etc.
- **FQN collision trap:** the JDK ships an `org.w3c.dom` (java.xml module) whose
  `Element`/`Node` are *different declarations* with a *different member set*
  than Kotlin/JS's. It has no `org.w3c.dom.html.*` or `org.w3c.dom.css.*`, no
  `.style` / `.classList` / `addEventListener` on `Element`. So "just depend on
  `org.w3c.dom` on the JVM" does **not** let the existing JS-typed source
  compile there, and its `Transformer`/`LSSerializer` output follows XML rules
  (`<br/>`, XML escaping, raw-text handling) that are subtly wrong for HTML.
  Rejected as a false shortcut — see §6.

### What already works in our favor

- `compose.runtime` (Recomposer, Composition, `AbstractApplier`) is
  multiplatform and runs on the JVM unchanged. Reconciliation is not
  browser-specific.
- `AttrsScope` **already accumulates into plain data** — `attributesMap:
  Map<String,String>`, `classes: MutableList<String>`, a `StyleScopeBuilder`.
  The attribute/class/style path is already backend-agnostic; only a few points
  touch the native node (see §3).

---

## 2. Chosen approach

**Refactor `html/core` (and its runtime + svg) into a true KMP module:** move
the JS sources into `commonMain` behind an `expect`/`actual` node abstraction,
keep JS backed by the browser DOM (minimal churn), and add a `jvmMain` backend
that builds a **lightweight plain-Kotlin mutable node model** and serializes it
to HTML with **kotlinx.html**.

Target: **full element / attribute / CSS parity** — every existing composable
compiles for JVM. Events and native-node side effects are **inert** on the JVM
(SSR has no events); they still emit their attribute/markup contribution where
one exists.

### Approaches considered

| # | Approach | Verdict |
|---|----------|---------|
| A | **KMP refactor + expect/actual node abstraction; JS→browser DOM, JVM→own node model + kotlinx.html serialize** | ✅ **Chosen.** Real write-once sharing, keeps live JS applier, correct HTML output, dependency-light. |
| B | Typealias `org.w3c.dom.*` to hand-written JVM shim classes | ❌ Reimplements a huge slice of the DOM/HTML/CSS spec as dead stubs; most never do anything for SSR. |
| C | Separate JVM-only markup DSL, no shared source | ❌ Two APIs diverge; defeats "same composables render both places." |
| D | "kotlinx.html everywhere in common, drop w3c" | ❌ kotlinx.html's *common* surface is one-shot string streaming, not a mutable indexed tree — can't back the live JS applier without re-serializing every frame (regresses interactivity). kotlinx.html stays as the JVM *serializer*, not the shared node model. |
| E | Depend on `org.w3c.dom` (JDK or Xerces) on the JVM | ❌ FQN collision + missing `html`/`css` hierarchy + XML-not-HTML serialization. See §6. |

---

## 3. The six coupling points (and their JVM resolution)

Everything JS-specific reduces to these:

1. **Element type surface** — `Element`, `HTMLElement`, and ~60 `HTMLxxxElement`
   subtypes used as generic type params / factory return types.
   → Introduce a common element-type hierarchy (see §4.1). JS actualizes to the
   browser types; JVM actualizes to node-model classes.

2. **The applier** (`DomApplier` / `DomNodeWrapper`) — generic
   insert/remove/move over a retained node.
   → Abstract the node wrapper. JS wraps a browser `Node`; JVM wraps a
   `JvmNode`. Insert/remove/move logic is identical in shape.

3. **Element factory** — `ElementBuilder` calls `document.createElement(tag)`.
   → `expect`/`actual` factory. JVM builds a `JvmElement(tagName)`.

4. **`AttrsScope` application** — attrs/classes/style are already plain data;
   only `updateAttrs`/`updateClasses`/`updateStyleDeclarations` write to a
   native node.
   → Common interface with a `applyTo(node)` per backend. JVM writes into the
   node model's maps.

5. **`prop` and `ref`** — `prop((E, V) -> Unit, V)` runs a lambda against the
   *native* element (e.g. `element.value = ...`); `ref` hands the user the
   native `TElement`.
   → On JVM these are **inert** (the "element" is a `JvmElement`, has no
   `.value`). Known semantic gap: controlled inputs that set `value`/`checked`
   via `prop` won't reflect in SSR markup unless the component also sets the
   corresponding attribute. See §7 (Open questions / risks).

6. **Events + `renderComposable`** — synthetic events wrap
   `org.w3c.dom.events.*`; `renderComposable` uses `kotlinx.browser.document`
   and a JS microtask dispatcher/clock.
   → Synthetic-event types abstracted in common (native-event field is
   platform-specific; JVM has none). JVM gets `renderToString(content)` that
   composes once with a `BroadcastFrameClock`, awaits recomposer idle, disposes,
   and serializes. Listeners register but never fire.

---

## 4. Architecture

### 4.1 Module / source-set layout

```
html/core
  src/commonMain   <- almost all current jsMain sources move here
  src/jsMain       <- actuals: browser-DOM backend (thin, ~ current applier)
  src/jvmMain      <- actuals: node-model backend + kotlinx.html serializer
  src/jsTest       <- existing tests, unchanged behavior
  src/jvmTest       <- new: renderToString assertions

html/internal-html-core-runtime  -> same split (applier + render entrypoints)
html/svg                         -> same split
```

Build: `html/core/build.gradle.kts` gains real `commonMain`/`jvmMain` source
sets; `jvmMain` depends on `org.jetbrains.kotlinx:kotlinx-html` (JVM). The
existing `jvmMain` `compose.desktop.currentOs` dependency is dropped (it was
never used by real code).

### 4.2 Common node abstraction

A minimal expect surface — **not** the full w3c hierarchy. Sketch:

```kotlin
// commonMain
expect abstract class DomElement            // maps: JS -> org.w3c.dom.Element,   JVM -> JvmElement
expect abstract class DomHtmlElement : DomElement  // JS -> HTMLElement, JVM -> JvmElement
// ...the ~60 HTMLxxxElement expect types, each an alias/subtype
```

Realistically the ~60 typed subclasses become common `expect` typealiases or
empty marker subtypes so `Div(): DomHtmlDivElement` etc. still type-check.
Most carry no members we use on the JVM — they exist only to preserve the
public generic signatures. This is the bulk of the "full parity" cost and is
largely mechanical.

### 4.3 JVM backend

```kotlin
// jvmMain
sealed class JvmNode
class JvmElement(val tagName: String) : JvmNode() {
    val attributes = LinkedHashMap<String, String>()
    val styles     = LinkedHashMap<String, String>()   // inline style props
    val classes    = mutableListOf<String>()
    val children   = mutableListOf<JvmNode>()
}
class JvmTextNode(var text: String) : JvmNode()

// applier operates on JvmNode: insert(index)/remove/move on children list
// serializer: walk tree -> kotlinx.html TagConsumer (correct HTML rules)
fun renderToString(content: @Composable DOMScope<JvmElement>.() -> Unit): String
```

`renderToString` correctness rules delegated to kotlinx.html: void elements
(`<br>`, `<img>`), attribute escaping, entity encoding, raw-text `<script>` /
`<style>`.

### 4.4 Data flow (JVM)

```
renderToString(content)
  -> create root JvmElement
  -> Recomposer(BroadcastFrameClock) + Composition(JvmApplier(root))
  -> setContent { content(scope) }   // builds/mutates JvmNode tree
  -> advance clock once, await idle  // let effects/state settle
  -> dispose composition
  -> serialize root.children via kotlinx.html -> String
```

---

## 5. Testing strategy

- **`jvmTest`**: `renderToString { Div({ classes("x"); style { color(Color.red) } }) { Text("hi") } }`
  → assert exact string `"""<div class="x" style="color:red;">hi</div>"""`.
- Cover: nested trees, attrs, classes, inline style, void elements, text
  escaping, conditional composition (recomposition settling before serialize),
  SVG subset.
- **Parity guard**: a shared set of composables compiled in *both* `jsTest` and
  `jvmTest` to prove the common API surface builds on both targets.
- Existing `jsTest` suite must stay green (no behavior change on JS).

---

## 6. Why not `org.w3c.dom` on the JVM (detail)

- **JDK `org.w3c.dom`** (java.xml): has mutable `Node` ops
  (`insertBefore`/`appendChild`/`removeChild`/`childNodes`) that map to the
  applier, **but** no `org.w3c.dom.html.*`, no `org.w3c.dom.css.*`, no
  `.style`/`.classList`/`addEventListener` on `Element`. Its `Element` is a
  *different declaration* than Kotlin/JS's identical-FQN type, so common code
  referencing browser-only members fails to compile on JVM. Serialization is XML
  (`Transformer`/`LSSerializer`) → wrong HTML.
- **Xerces `xml-apis`**: does ship `org.w3c.dom.html.*` / `css.*`, but DOM Level
  1/2 Java signatures differ from Kotlin/JS, and it's an ancient
  endorsed-override-prone dependency.
- The one thing JDK dom gives free (a mutable tree) is ~50 lines to hand-roll,
  and hand-rolling yields correct HTML serialization. Net: not easier, lower
  quality.

---

## 7. Open questions / risks

1. **`prop`-driven values (controlled inputs).** Components that set
   `value`/`checked`/`innerText` via `prop` won't appear in SSR markup because
   `prop` targets a native node. **Decision needed:** (a) accept as a
   documented limitation for v1, or (b) add a JVM-aware bridge so common
   props map to attributes. Recommend (a) for v1.
2. **`ref` blocks** hand out the native element; on JVM the "element" is a
   `JvmElement`. `ref` effects that touch browser APIs won't compile in common
   or will no-op. Likely need a common-safe `ref` signature (element typed as
   the common element type) — may ripple into user code. **Needs validation.**
3. **CSS `StyleSheet` / media / keyframes** (`CSSStyleSheet` usage in
   `Elements.kt` `Style {}`): stylesheet-injection is browser-oriented. For SSR,
   emit collected rules into a `<style>` block. Scope of the CSS OM surface to
   port needs a pass.
4. **Migration blast radius.** Moving jsMain→commonMain must not break the
   published JS artifact (API, ABI, `@ComposeWebInternalApi` opt-ins). Requires
   binary-compatibility validation.
5. **SVG module** parity — same refactor, smaller surface.
6. **wasm** target: not in scope, but the common abstraction should not
   foreclose a future wasm actual.

---

## 8. Out of scope

- Client-side **hydration** (JS re-attaching to server markup).
- Actual on-screen JVM rendering (Skia/Compose Desktop).
- wasm target.
- Event dispatch on the JVM.
- **Typed CSS DSL on the JVM (deferred — follow-up task).** As of the 2.x
  migration, the typed CSS property DSL stays Kotlin/JS-only: `css/CSSUnits.kt`
  (`5.px`, `em`, …), `Color`, `CSSKeywords`, `CSSEnums`, `CSSOperations`,
  `CSSVariableValueAs`, all 19 `css/properties/*` helpers (`width()`, `color()`,
  `padding()`, `flex*`, `grid*`, `transform()`, `transition()`, …), the
  `StyleSheet` base-class authoring idiom, `CSSMediaRule`, and `CSSKeyframeRule`.
  Porting these needs value-class rework (they are JS `external interface` +
  `unsafeCast` zero-cost wrappers). **What DOES work on JVM:**
  `StyleScope.property(name, value)` / `.variable(...)` raw string/number escape
  hatch, `AttrsScope.style { ... }`, the full selector DSL, and `Style { }`
  serialized into a `<style>` tag. Tracked as a follow-up: "Port typed CSS DSL to
  commonMain for JVM SSR."

---

## 9. Rough phasing (for the later plan)

1. Stand up KMP skeleton: split source sets, empty `jvmMain`, build green on JS.
2. Introduce common node abstraction + move applier/runtime to common with
   `expect`/`actual`; JS actuals = current behavior. JS tests green.
3. Move the typed element/attr/event/CSS surface to common (the big mechanical
   lift); JS tests green throughout.
4. Implement JVM backend: `JvmNode`, `JvmApplier`, `renderToString`,
   kotlinx.html serializer.
5. `jvmTest` suite + parity guard.
6. Resolve `prop`/`ref` decisions (§7).
