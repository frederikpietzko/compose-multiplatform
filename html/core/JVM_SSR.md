# JVM server-side rendering (SSR)

`:html-core` (and `:html-svg`) ship a JVM target in addition to the existing JS target.
On JVM you can render Compose HTML composables directly to an HTML string, without a browser
or a DOM — useful for server-side rendering, static site generation, or emitting markup in tests.

## Usage

```kotlin
import org.jetbrains.compose.web.renderToString
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.css.*

val html: String = renderToString {
    Div({
        classes("card")
        style {
            property("color", "red")
        }
    }) {
        Text("Hello")
        Span({ classes("title") }) {
            Text("World")
        }
    }
}
// html == """<div class="card" style="color:red;">Hello<span class="title">World</span></div>"""
```

`renderToString(content: @Composable DOMScope<DomElement>.() -> Unit): String` lives in
`org.jetbrains.compose.web`, in the `jvmMain` source set of `:html-core`. It mounts `content` in
a one-shot composition, lets the initial composition (and any effects it schedules for "next
frame", e.g. `LaunchedEffect`) settle, serializes the resulting tree to an HTML string, and tears
the composition down again — so each call is self-contained and does not leak a `Recomposer`,
even if `content` throws.

## What works on JVM

- All the built-in element composables (`Div`, `Span`, `P`, `Table`/`Tr`/`Td`, `Ul`/`Li`,
  `A`, `Button`, `Input`, `TextArea`, `Select`/`Option`, etc.) and arbitrary nesting.
- Attributes and classes (`id(...)`, `classes(...)`, `attr(...)`, and the typed attribute
  helpers).
- Inline styles via the common style API: `style { property(name, value) }` — serialized as a
  `style="..."` attribute.
- The selector DSL (`org.jetbrains.compose.web.css.selectors`, `type(...)`, `className(...)`,
  `id(...)`, `attr(...)`, combinators, etc.) and `Style { style(selector) { property(...) } }` /
  `Style(cssRules)`, which serialize into a `<style>` tag. On JVM, use the member-call form
  `style(selector) { ... }` rather than the JS-only infix/`String`-receiver sugar
  (`selector style { ... }` / `"div" style { ... }`).
- SVG elements from `:html-svg` (`Svg`, `Circle`, ...), currently behind
  `@ExperimentalComposeWebSvgApi`.
- Conditional and stateful composition (`remember`, `if`/`when` branches, etc.) — the tree is
  serialized only after the initial composition (and any effects it schedules) has settled, so
  the output reflects the fully-composed state.

## Documented limitations

1. **No events on the server.** Event listeners (`onClick`, `addEventListener`, ...) are
   registered on the composed tree exactly as on JS, but nothing ever dispatches an event during
   SSR — there is no live DOM to click, type into, or otherwise interact with. They are inert.

2. **Controlled input values are inert.** `Input`/`TextArea`'s controlled `value`/`checked`
   (set via `prop`, driven by Compose state) target a live DOM node to imperatively push state
   into; there is no such node on JVM, so controlled values are **not** reflected in the
   serialized markup. Inputs still render normally otherwise — their static `attrs` (e.g. `id`,
   `type`) show up as usual, and rendering does not crash.

3. **`ref` blocks are inert.** `ref { element -> ... }` / `scopeElement` hand callers a
   disconnected placeholder element on JVM. `ref` effects that touch browser-only APIs (measuring
   layout, focusing, calling into JS interop, etc.) are no-ops in SSR and should not be relied on
   to affect the serialized output.

4. **The typed CSS DSL is JS-only (follow-up work).** `width(5.px)`, `color(Color.red)`,
   `padding(...)`, the `StyleSheet` subclass idiom, media queries, and `@keyframes` are not
   available on JVM yet — they still live in `jsMain`. Use the raw `style { property("width",
   "5px") }` escape hatch (and the selector DSL described above) instead. See the design spec's
   "Out of scope" section for the follow-up.

5. **Hydration is out of scope.** `renderToString` produces static markup only. Making that
   markup interactive in the browser (attaching event listeners, resuming state, etc.) is a
   separate, future effort — it is not part of this JVM SSR support.
