/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.internal.runtime.NativeNode

// Common element-type hierarchy for Compose HTML (task 2.1).
//
// JS actualizes each type to the identically-named browser type
// (org.w3c.dom.*). JVM actualizes all of them to a real (mostly empty)
// subclass chain rooted in the runtime node model `JvmElement`
// (org.jetbrains.compose.web.internal.runtime.JvmElement) -- see
// DomElementTypes.js.kt / DomElementTypes.jvm.kt.
//
// Form chosen after derisking (see task-2.1-report.md for the full
// history of rejected forms):
//   - `expect interface` doesn't work: org.w3c.dom.Element/HTMLElement/...
//     are `external abstract class`, not interfaces, so an actual
//     typealias to them fails with "the class kinds are different" (on
//     JS too, not just JVM).
//   - `expect class` + "every JVM actual is a typealias to the same
//     JvmElement" doesn't work either: when two distinct expect types
//     declare a supertype relationship (e.g. DomHtmlElement : DomElement)
//     and both JVM actuals alias to the identical class, the compiler
//     requires that class to list itself as its own supertype, which is
//     structurally impossible ("some supertypes are missing in the
//     actual declaration").
//   - What *does* work: `expect abstract/open class` with a real,
//     distinct, (mostly empty) class per type on the JVM side, each
//     genuinely extending its parent -- bottoming out at a thin
//     `abstract class DomElement(tagName: String) : JvmElement(tagName)`.
//     Real inheritance actually exists on JVM this way, so the
//     actual-declaration checker is satisfied. Modality (`abstract` vs
//     `open`) must match the *real* modality of the browser class
//     exactly -- Kotlin's checker requires an exact match, not just
//     compatibility -- so it mirrors org.w3c.dom's own declared modality
//     for each type (all of Element/HTMLElement/HTMLxxxElement are
//     `abstract`; Text is `open`).
// DomElement : NativeNode added in task 2.3 -- both actuals already satisfy it (JvmElement/Node
// chain to NativeNode), and TagElement/ComposeDomNode (task 2.3) need to pass a created TElement
// straight into a NativeNodeWrapper without an unchecked cast.
expect abstract class DomElement : NativeNode
expect abstract class DomHtmlElement : DomElement
expect open class DomText

expect abstract class HTMLAnchorElement : DomHtmlElement
expect abstract class HTMLAreaElement : DomHtmlElement
expect abstract class HTMLAudioElement : DomHtmlElement
expect abstract class HTMLBRElement : DomHtmlElement
expect abstract class HTMLButtonElement : DomHtmlElement
expect abstract class HTMLCanvasElement : DomHtmlElement
expect abstract class HTMLDataListElement : DomHtmlElement
expect abstract class HTMLDListElement : DomHtmlElement
expect abstract class HTMLDivElement : DomHtmlElement
expect abstract class HTMLEmbedElement : DomHtmlElement
expect abstract class HTMLFieldSetElement : DomHtmlElement
expect abstract class HTMLFormElement : DomHtmlElement
expect abstract class HTMLHRElement : DomHtmlElement
expect abstract class HTMLHeadingElement : DomHtmlElement
expect abstract class HTMLIFrameElement : DomHtmlElement
expect abstract class HTMLImageElement : DomHtmlElement
expect abstract class HTMLInputElement : DomHtmlElement
expect abstract class HTMLLIElement : DomHtmlElement
expect abstract class HTMLLabelElement : DomHtmlElement
expect abstract class HTMLLegendElement : DomHtmlElement
expect abstract class HTMLMapElement : DomHtmlElement
expect abstract class HTMLMeterElement : DomHtmlElement
expect abstract class HTMLOListElement : DomHtmlElement
expect abstract class HTMLObjectElement : DomHtmlElement
expect abstract class HTMLOptGroupElement : DomHtmlElement
expect abstract class HTMLOptionElement : DomHtmlElement
expect abstract class HTMLOutputElement : DomHtmlElement
expect abstract class HTMLParagraphElement : DomHtmlElement
expect abstract class HTMLParamElement : DomHtmlElement
expect abstract class HTMLPictureElement : DomHtmlElement
expect abstract class HTMLPreElement : DomHtmlElement
expect abstract class HTMLProgressElement : DomHtmlElement
expect abstract class HTMLSelectElement : DomHtmlElement
expect abstract class HTMLSourceElement : DomHtmlElement
expect abstract class HTMLSpanElement : DomHtmlElement
expect abstract class HTMLStyleElement : DomHtmlElement
expect abstract class HTMLTableCaptionElement : DomHtmlElement
expect abstract class HTMLTableCellElement : DomHtmlElement
expect abstract class HTMLTableColElement : DomHtmlElement
expect abstract class HTMLTableElement : DomHtmlElement
expect abstract class HTMLTableRowElement : DomHtmlElement
expect abstract class HTMLTableSectionElement : DomHtmlElement
expect abstract class HTMLTextAreaElement : DomHtmlElement
expect abstract class HTMLTrackElement : DomHtmlElement
expect abstract class HTMLUListElement : DomHtmlElement
expect abstract class HTMLVideoElement : DomHtmlElement
