package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.JvmElement
import org.jetbrains.compose.web.internal.runtime.JvmTextNode

// DomElement can't be a plain `typealias = JvmElement`: the expect side
// must be `abstract` (org.w3c.dom.Element is abstract on JS, and Kotlin's
// actual-checker requires an exact modality match), but `JvmElement`
// itself must stay a directly-instantiable `open` class since it's used
// standalone elsewhere (e.g. NativeNodeJvmTest). So DomElement is a thin
// `abstract` real subclass of JvmElement, and every other type in this
// file chains off DomElement/DomHtmlElement (never JvmElement directly).
// A concrete leaf value is obtained the same way an abstract-but-never-
// literally-constructed browser DOM class is: via an anonymous object
// expression, e.g. `object : HTMLDivElement(tagName) {}`.
actual abstract class DomElement(tagName: String) : JvmElement(tagName)

actual abstract class DomHtmlElement(tagName: String) : DomElement(tagName)

actual typealias DomText = JvmTextNode

actual abstract class HTMLAnchorElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLAreaElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLAudioElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLBRElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLButtonElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLCanvasElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLDataListElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLDListElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLDivElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLEmbedElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLFieldSetElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLFormElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLHRElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLHeadingElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLIFrameElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLImageElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLInputElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLLIElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLLabelElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLLegendElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLMapElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLMeterElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLOListElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLObjectElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLOptGroupElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLOptionElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLOutputElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLParagraphElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLParamElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLPictureElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLPreElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLProgressElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLSelectElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLSourceElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLSpanElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLStyleElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTableCaptionElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTableCellElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTableColElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTableElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTableRowElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTableSectionElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTextAreaElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLTrackElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLUListElement(tagName: String) : DomHtmlElement(tagName)
actual abstract class HTMLVideoElement(tagName: String) : DomHtmlElement(tagName)
