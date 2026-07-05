package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.JvmTextNode
import org.jetbrains.compose.web.internal.runtime.NativeNode

// Must return a real DomElement (not a bare JvmElement): Base.kt's TagElement does
// `elementBuilder.create() as TElement` where TElement is some DomElement subtype (e.g.
// HTMLDivElement). That cast compiles to a checkcast against TElement's erasure, which is
// DomElement -- so the created node must actually be a DomElement instance or every element
// composable (Div, Span, P, ...) throws ClassCastException the moment a real composition tries
// to insert it (surfaced by task 3.3's renderToString, the first end-to-end JVM composition).
// At *that* call site the concrete tagName-specific subtype (e.g. HTMLDivElement) isn't needed:
// generic erasure means the checkcast only ever verifies against the DomElement bound.
//
// However, some composables (Input, TextArea, ...) later read `scopeElement` from a context
// where the element-scope's type parameter has already been substituted with a concrete
// subtype (e.g. `ElementScope<HTMLInputElement>`), not a still-generic type parameter. There,
// Kotlin's compiler *does* emit a checkcast to the concrete subtype (HTMLInputElement etc.),
// because the substitution is known statically at that call site. If the underlying node were
// still just a bare `object : DomElement(tagName) {}`, that checkcast would throw
// ClassCastException (task 4.1b): e.g. `renderToString { Input(type = InputType.Text) {} }`
// crashes inside `restoreControlledInputState(inputElement = scopeElement)`.
//
// The fix: construct the tagName-specific subtype here, so every `HTMLXxxElement` checkcast
// downstream (present or future) succeeds. The subtypes are abstract browser-DOM shells with no
// members on JVM (see DomElementTypes.jvm.kt), so an anonymous instance is just as inert as the
// generic fallback -- this only widens the created object's `is`/cast surface.
actual fun createNativeElement(tagName: String): NativeNode = when (tagName) {
    "a" -> object : HTMLAnchorElement(tagName) {}
    "area" -> object : HTMLAreaElement(tagName) {}
    "audio" -> object : HTMLAudioElement(tagName) {}
    "br" -> object : HTMLBRElement(tagName) {}
    "button" -> object : HTMLButtonElement(tagName) {}
    "canvas" -> object : HTMLCanvasElement(tagName) {}
    "datalist" -> object : HTMLDataListElement(tagName) {}
    "dl" -> object : HTMLDListElement(tagName) {}
    "div" -> object : HTMLDivElement(tagName) {}
    "embed" -> object : HTMLEmbedElement(tagName) {}
    "fieldset" -> object : HTMLFieldSetElement(tagName) {}
    "form" -> object : HTMLFormElement(tagName) {}
    "hr" -> object : HTMLHRElement(tagName) {}
    "h1", "h2", "h3", "h4", "h5", "h6" -> object : HTMLHeadingElement(tagName) {}
    "iframe" -> object : HTMLIFrameElement(tagName) {}
    "img" -> object : HTMLImageElement(tagName) {}
    "input" -> object : HTMLInputElement(tagName) {}
    "li" -> object : HTMLLIElement(tagName) {}
    "label" -> object : HTMLLabelElement(tagName) {}
    "legend" -> object : HTMLLegendElement(tagName) {}
    "map" -> object : HTMLMapElement(tagName) {}
    "meter" -> object : HTMLMeterElement(tagName) {}
    "ol" -> object : HTMLOListElement(tagName) {}
    "object" -> object : HTMLObjectElement(tagName) {}
    "optgroup" -> object : HTMLOptGroupElement(tagName) {}
    "option" -> object : HTMLOptionElement(tagName) {}
    "output" -> object : HTMLOutputElement(tagName) {}
    "p" -> object : HTMLParagraphElement(tagName) {}
    "param" -> object : HTMLParamElement(tagName) {}
    "picture" -> object : HTMLPictureElement(tagName) {}
    "pre" -> object : HTMLPreElement(tagName) {}
    "progress" -> object : HTMLProgressElement(tagName) {}
    "select" -> object : HTMLSelectElement(tagName) {}
    "source" -> object : HTMLSourceElement(tagName) {}
    "span" -> object : HTMLSpanElement(tagName) {}
    "style" -> object : HTMLStyleElement(tagName) {}
    "caption" -> object : HTMLTableCaptionElement(tagName) {}
    "th", "td" -> object : HTMLTableCellElement(tagName) {}
    "col", "colgroup" -> object : HTMLTableColElement(tagName) {}
    "table" -> object : HTMLTableElement(tagName) {}
    "tr" -> object : HTMLTableRowElement(tagName) {}
    "thead", "tbody", "tfoot" -> object : HTMLTableSectionElement(tagName) {}
    "textarea" -> object : HTMLTextAreaElement(tagName) {}
    "track" -> object : HTMLTrackElement(tagName) {}
    "ul" -> object : HTMLUListElement(tagName) {}
    "video" -> object : HTMLVideoElement(tagName) {}
    else -> object : DomHtmlElement(tagName) {}
}

actual fun createNativeText(text: String): NativeNode = JvmTextNode(text)
