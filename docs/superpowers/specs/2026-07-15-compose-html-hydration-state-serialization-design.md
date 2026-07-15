# Compose HTML Hydration State Serialization

**Date:** 2026-07-15
**Status:** Approved design
**Goal:** Serialize explicitly registered Compose state alongside JVM-rendered HTML and restore it before the first browser hydration composition, so server and client can begin with different fallback inputs while still producing an identical hydrated DOM tree.

---

## 1. Context

Compose HTML now supports:

1. JVM server-side rendering through `renderToString`.
2. Marker-free browser hydration through `hydrateComposable`.
3. Normal client recomposition after hydration.

Current hydration reruns the composable independently in the browser. Every `remember` initializer therefore runs again, and server and browser must independently derive the same initial state. No Compose state crosses the server/client boundary.

This design adds explicit, serializer-backed state restoration. It does not serialize the Compose slot table or arbitrary `remember` values. Only state created through the new `rememberHydratableState` API participates.

---

## 2. Scope

### In scope

- Explicit page-global state keys.
- `MutableState<T>` values serialized with `kotlinx.serialization`.
- Reified serializer inference for the common call path.
- Explicit `KSerializer<T>` overload for custom serializers and generic call sites.
- A JVM result containing HTML and raw state JSON.
- A JS hydration overload accepting raw state JSON.
- Strict server/client state-key equality during initial hydration.
- Versioned, deterministic JSON payloads.
- Existing hydration failure cleanup for state-restoration failures.
- Primitive, nullable, collection, and custom-serializer tests.

### Out of scope

- Automatic serialization of arbitrary `remember` or Compose slot-table state.
- Composition-position-derived keys.
- Hierarchical key scopes.
- State transport, script elements, DOM lookup conventions, HTML escaping, CSP integration, HTTP integration, or framework integration.
- Changes to the Spring hydration example.
- State persistence beyond one server-render-to-browser-hydration handoff.
- Resumability without rerunning the composable.
- Serialization of effects, coroutine state, derived state, or event handlers.
- Multiple serialization formats or caller-configurable `Json` instances in the first version.
- Custom `SnapshotMutationPolicy` support in the first version.

---

## 3. Public API

### 3.1 Remembered hydratable state

Add common APIs under `org.jetbrains.compose.web.state`:

```kotlin
@Composable
inline fun <reified T> rememberHydratableState(
    key: String,
    noinline initial: () -> T
): MutableState<T>

@Composable
fun <T> rememberHydratableState(
    key: String,
    serializer: KSerializer<T>,
    initial: () -> T
): MutableState<T>
```

Typical use:

```kotlin
var count by rememberHydratableState("counter") { serverInitialCount }
```

The inline overload obtains `serializer<T>()` from `kotlinx.serialization`. The explicit overload supports custom, contextual, polymorphic, or non-reified generic use cases.

The name intentionally differs from Compose Runtime 1.10's existing `androidx.compose.runtime.saveable.rememberSerializable`. That API targets platform saved-state restoration and derives identity from composition position; this design targets JVM SSR-to-browser hydration and requires an explicit page-global key.

### 3.2 JVM rendering result

Add under `org.jetbrains.compose.web`:

```kotlin
data class SsrResult(
    val html: String,
    val stateJson: String
)

fun renderToStringWithState(
    content: @Composable DOMScope<DomElement>.() -> Unit
): SsrResult
```

`html` contains the same markup format as `renderToString`. `stateJson` is raw JSON. The caller decides how to transport it to the browser.

Existing `renderToString` remains source-compatible and keeps returning `String`.

### 3.3 JS hydration overload

Add under `org.jetbrains.compose.web`:

```kotlin
fun <TElement : Element> hydrateComposable(
    root: TElement,
    stateJson: String,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition
```

The overload receives state directly. It does not discover payloads in the document and does not prescribe transport.

Existing `hydrateComposable` overloads remain source-compatible.

---

## 4. Semantics

### 4.1 Registry-free behavior

When no hydration-state registry is installed, `rememberHydratableState` behaves like:

```kotlin
remember { mutableStateOf(initial()) }
```

This lets the same composable run through ordinary client rendering and existing SSR/hydration APIs. Serialization activates only through `renderToStringWithState` or the state-aware `hydrateComposable` overload.

Inputs are intentionally absent in the first API. As with plain `remember`, the initializer is used only when the remembered state first enters the composition.

### 4.2 State-aware JVM behavior

During `renderToStringWithState`:

1. Create an empty server-mode `HydrationStateRegistry`.
2. Install it through an internal `CompositionLocal` around `content`.
3. For every `rememberHydratableState` call:
   - require a nonblank page-global key;
   - reserve that key for the active remembered entry;
   - run `initial`;
   - create a structurally compared `MutableState<T>`;
   - register the serializer and a provider for the state's current value.
4. Run the existing initial composition and settling frame.
5. Serialize the DOM and current registered values before disposing the composition.
6. Return both values in `SsrResult`.

State providers are tied to the remembered entry lifecycle. If a state-bearing composable leaves the composition during SSR recomposition, its provider is removed and its key is absent from the final snapshot.

### 4.3 State-aware browser behavior

During state-aware `hydrateComposable`:

1. Parse and validate the state envelope before starting DOM hydration.
2. Create a restoration-mode `HydrationStateRegistry` from its `values` object.
3. Install the registry through the same internal `CompositionLocal`.
4. For every `rememberHydratableState` call:
   - reserve its explicit key;
   - consume the matching `JsonElement`;
   - decode it with the call-site serializer;
   - create `MutableState<T>` with the restored value;
   - do not call `initial`.
5. Let restored state drive the initial element, text, attribute, class, and style output used by positional DOM matching.
6. Before the hydration session commits, verify that every payload key was consumed exactly once.
7. Commit DOM hydration and switch the registry from restoration mode to live mode.

After hydration commits, values behave as ordinary Compose mutable state. A newly introduced `rememberHydratableState` call initializes normally because the one-time restoration phase has ended. Active keys remain unique across the rendered root.

If a restored state-bearing group later leaves and re-enters the composition, it initializes again; this feature is not a browser persistence mechanism.

### 4.4 Key rules

- Keys are unique across the whole rendered root while their entries are active.
- Blank keys fail.
- Reusable or repeated components must receive caller-defined stable prefixes, for example `"row-$id-expanded"`.
- A duplicate active key fails rather than becoming an ordered list.
- During restoration, the server and client initial compositions must declare exactly the same key set.

---

## 5. Payload format

Version 1 uses a JSON envelope:

```json
{
  "version": 1,
  "values": {
    "counter": 7,
    "user": {
      "name": "Ada"
    }
  }
}
```

Rules:

- Each value remains a `JsonElement` produced by its registered `KSerializer`.
- Keys are emitted in lexicographic order for deterministic output and stable tests.
- JSON `null` is a valid value when accepted by the serializer and is distinct from a missing key.
- `version` must equal `1`.
- `values` must be a JSON object.
- Raw JSON is returned unchanged as an application transport concern. Core does not make it safe for inline HTML or JavaScript contexts.

The versioned envelope permits future metadata or format evolution without interpreting a state key as protocol metadata.

---

## 6. Architecture and component boundaries

### 6.1 `HydrationStateRegistry`

A common internal component owns state bookkeeping only. It has no DOM, HTML, browser, HTTP, or framework dependency.

Responsibilities:

- Track server-mode active entries and current-value providers.
- Track restoration-mode unconsumed `JsonElement`s.
- Reserve and release active page-global keys.
- Decode restored values through their call-site serializers.
- Encode current values through their registered serializers.
- Verify exact payload consumption before hydration commit.
- Switch a client registry from restoration mode to live mode after initial hydration.

Remembered registry entries use Compose lifecycle callbacks so providers represent the final active composition, not every key ever encountered during transient recomposition.

### 6.2 Composition adapter

`rememberHydratableState` is the only public composable-facing adapter. It selects registry-backed or ordinary behavior and returns `MutableState<T>`. Consumers do not interact with registry instances.

### 6.3 Payload codec

A common internal codec converts between the versioned JSON envelope and registry `JsonElement` maps. It is independently testable and contains no composition logic.

### 6.4 JVM renderer wiring

Refactor the existing JVM renderer enough to share its composition lifecycle between `renderToString` and `renderToStringWithState`.

The state-aware path snapshots both DOM and registry after the existing settling frame and before composition teardown. This ordering guarantees the returned HTML and state represent the same composition point.

The ordinary path does not install a registry and preserves current behavior.

### 6.5 JS hydration wiring

The state-aware overload parses the payload and installs the restoration registry, then delegates to the existing positional hydration machinery.

Add a small internal hydration callback seam so registry consumption is verified before `HydrationDomApplierStrategy.finish()` commits initial hydration. If verification throws, existing failure handling aborts the session, removes listeners installed during the attempt, disposes the partial composition, and preserves the SSR root according to current best-effort guarantees.

The ordinary hydration path supplies a no-op callback and remains unchanged.

---

## 7. Error handling

Add public `HydrationStateException : IllegalStateException` under `org.jetbrains.compose.web.state` so applications can distinguish state-payload failures from `HydrationMismatchException` and arbitrary composable failures.

State-aware rendering or hydration fails for:

- malformed JSON;
- missing, invalid, or unsupported envelope version;
- missing or non-object `values`;
- blank state key;
- duplicate active state key;
- client key missing from the server payload;
- server payload key unconsumed by the client initial composition;
- serializer encode failure;
- serializer decode failure.

Messages include the state key and operation where available. Example:

```text
Failed to restore hydration state "counter": expected Int, found "seven"
```

A payload-present missing key never falls back to `initial`. Exactness is required during restoration.

Payload parsing happens before DOM hydration begins. Failures encountered later during composition use existing hydration cleanup. Cleanup remains best-effort rather than transactional: earlier equivalent DOM updates are not rolled back.

---

## 8. Dependencies

`html-core` gains a common `kotlinx-serialization-json` API dependency. `KSerializer` appears in the public API, and JSON support is required on both JVM and JS.

The core module does not need the Kotlin serialization compiler plugin because its implementation uses generic and built-in serializers. Downstream consumers need the plugin only when they define generated serializers for their own `@Serializable` classes.

No dependency on Compose `runtime-saveable` is added. Its registry intentionally supports duplicate providers per key and generic platform-saveable values, which conflict with this feature's explicit unique-key and JSON semantics.

---

## 9. Testing strategy

### 9.1 Common registry and codec tests

- Reified primitive serializer inference.
- Explicit serializer overload.
- Primitive, nullable, and collection values.
- Deterministic lexicographic key ordering.
- Version 1 envelope round trip.
- Blank and duplicate key rejection.
- Malformed payload, unsupported version, missing `values`, and wrong `values` type.
- Missing restored key and unconsumed restored key.
- Encode and decode errors wrapped with key context.
- Registry-free fallback behaves like ordinary remembered mutable state.

### 9.2 JVM SSR tests

Use a shared composable whose initial value can differ by platform:

```kotlin
@Composable
fun StatefulCounter(initialCount: Int) {
    var count by rememberHydratableState("counter") { initialCount }
    Button({ onClick { count++ } }) {
        Text("Count: $count")
    }
}
```

Render with server fallback `7` and assert:

- HTML is `<button>Count: 7</button>`.
- Payload contains version `1` and `"counter": 7`.
- HTML and payload capture the same final state.
- Existing `renderToString` tests remain unchanged.

### 9.3 JS hydration tests

Start with server fixture `<button>Count: 7</button>` and server state payload, but compose with browser fallback `0`.

Assert:

- browser initializer is not called;
- existing button and text-node identities are preserved;
- text remains `Count: 7` during hydration;
- click updates the same text node to `Count: 8`;
- missing, extra, duplicate, malformed, wrong-type, and unsupported-version payloads fail;
- state verification failures perform listener and partial-composition cleanup;
- a later ordinary hydration can run after failure;
- existing stateless hydration tests remain green.

---

## 10. Completion criteria

The feature is complete when automated tests demonstrate that:

- JVM SSR returns matching HTML and versioned serialized state.
- Browser hydration restores explicitly keyed values before its first DOM match.
- Server and browser fallback initializers may differ without replacing matched nodes.
- Restored state continues through normal client recomposition.
- Inferred and explicit serializers work.
- Exact key-set and payload validation failures are actionable and use existing hydration cleanup.
- Existing state-unaware SSR, rendering, and hydration APIs retain their behavior.
- Core contains no state-transport or framework-integration policy.
