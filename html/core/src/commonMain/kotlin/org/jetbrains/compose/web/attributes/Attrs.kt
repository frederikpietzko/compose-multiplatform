package org.jetbrains.compose.web.attributes

import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.events.SyntheticSubmitEvent
import kotlin.jvm.JvmName
import org.jetbrains.compose.web.dom.HTMLAnchorElement
import org.jetbrains.compose.web.dom.HTMLButtonElement
import org.jetbrains.compose.web.dom.HTMLCanvasElement
import org.jetbrains.compose.web.dom.HTMLFormElement
import org.jetbrains.compose.web.dom.HTMLImageElement
import org.jetbrains.compose.web.dom.HTMLInputElement
import org.jetbrains.compose.web.dom.HTMLLabelElement
import org.jetbrains.compose.web.dom.HTMLOptGroupElement
import org.jetbrains.compose.web.dom.HTMLOptionElement
import org.jetbrains.compose.web.dom.HTMLSelectElement
import org.jetbrains.compose.web.dom.HTMLTableCellElement
import org.jetbrains.compose.web.dom.HTMLTableColElement
import org.jetbrains.compose.web.dom.HTMLTextAreaElement

// Moved from jsMain to commonMain (task 2.5): the bulk of this file is portable attribute setter
// extension functions (String/Int/enum-typed), so it moves verbatim. Only three things stay
// platform-specific:
//  - the 3 `autoComplete(value: AutoComplete)` overloads, moved to the jsMain-only remnant file
//    Attrs.js.kt -- AutoComplete is a JS `external interface` zero-cost branding type that needs a
//    JVM boxed-holder redesign before it can move (see task-2.5 report);
//  - setInputValue/setTextAreaDefaultValue/setCheckedValue, which mutate a live DOM element and
//    persist controlled-input state -- these become expect vals below, actualized with their
//    original bodies on JS (Attrs.js.kt) and as no-op stubs on JVM (Attrs.jvm.kt), since SSR never
//    invokes AttrsScope.prop() against a live element.
//
// @JvmName annotations: on JVM, AttrsScope<TElement>'s type parameter is erased, so two extension
// functions with the same name (e.g. `fun AttrsScope<HTMLButtonElement>.disabled()` and
// `fun AttrsScope<HTMLInputElement>.disabled()`) collide at the bytecode level ("platform
// declaration clash") even though Kotlin itself treats them as distinct overloads by receiver
// type. This never surfaced while the file was jsMain-only (Kotlin/JS mangles by receiver type,
// and no JVM bytecode was ever generated for it). @JvmName gives each clashing declaration a
// distinct JVM-visible name; it has zero effect on the Kotlin-level API/call sites (source and ABI
// compatible for Kotlin consumers) and only changes the method name Java/reflection callers would
// see.

fun AttrsScope<HTMLAnchorElement>.href(value: String) =
    attr("href", value)

fun AttrsScope<HTMLAnchorElement>.target(value: ATarget = ATarget.Self) =
    attr("target", value.targetStr)

fun AttrsScope<HTMLAnchorElement>.ref(value: ARel) =
    attr("rel", value.relStr)

fun AttrsScope<HTMLAnchorElement>.ping(value: String) =
    attr("ping", value)

fun AttrsScope<HTMLAnchorElement>.ping(vararg urls: String) =
    attr("ping", urls.joinToString(" "))

fun AttrsScope<HTMLAnchorElement>.hreflang(value: String) =
    attr("hreflang", value)

fun AttrsScope<HTMLAnchorElement>.download(value: String = "") =
    attr("download", value)

/* Button attributes */

@JvmName("autoFocusButton")
fun AttrsScope<HTMLButtonElement>.autoFocus() =
    attr("autofocus", "")

@JvmName("disabledButton")
fun AttrsScope<HTMLButtonElement>.disabled() =
    attr("disabled", "")

@JvmName("formButton")
fun AttrsScope<HTMLButtonElement>.form(formId: String) =
    attr("form", formId)

@JvmName("formActionButton")
fun AttrsScope<HTMLButtonElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsScope<HTMLButtonElement>.formEncType(value: ButtonFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsScope<HTMLButtonElement>.formMethod(value: ButtonFormMethod) =
    attr("formmethod", value.methodStr)

@JvmName("formNoValidateButton")
fun AttrsScope<HTMLButtonElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsScope<HTMLButtonElement>.formTarget(value: ButtonFormTarget) =
    attr("formtarget", value.targetStr)

@JvmName("nameButton")
fun AttrsScope<HTMLButtonElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLButtonElement>.type(value: ButtonType) =
    attr("type", value.str)

@JvmName("valueButton")
fun AttrsScope<HTMLButtonElement>.value(value: String) =
    attr("value", value)

/* Form attributes */

fun AttrsScope<HTMLFormElement>.action(value: String) =
    attr("action", value)

fun AttrsScope<HTMLFormElement>.acceptCharset(value: String) =
    attr("accept-charset", value)

fun AttrsScope<HTMLFormElement>.autoComplete(value: Boolean = true) =
    attr("autocomplete", if(value) "on" else "off")

fun AttrsScope<HTMLFormElement>.encType(value: FormEncType) =
    attr("enctype", value.typeStr)

fun AttrsScope<HTMLFormElement>.method(value: FormMethod) =
    attr("method", value.methodStr)

fun AttrsScope<HTMLFormElement>.noValidate() =
    attr("novalidate", "")

fun AttrsScope<HTMLFormElement>.target(value: FormTarget) =
    attr("target", value.targetStr)

fun AttrsScope<HTMLFormElement>.onSubmit(
    listener: (SyntheticSubmitEvent) -> Unit
) {
    addEventListener(eventName = EventsListenerScope.SUBMIT, listener = listener)
}

fun AttrsScope<HTMLFormElement>.onReset(
    listener: (SyntheticSubmitEvent) -> Unit
) {
    addEventListener(eventName = EventsListenerScope.RESET, listener = listener)
}

/* Input attributes */

fun AttrsScope<HTMLInputElement>.type(value: InputType<*>) =
    attr("type", value.typeStr)

fun AttrsScope<HTMLInputElement>.accept(value: String) =
    attr("accept", value) // type: file only

@JvmName("altInput")
fun AttrsScope<HTMLInputElement>.alt(value: String) =
    attr("alt", value) // type: image only

@JvmName("autoFocusInput")
fun AttrsScope<HTMLInputElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLInputElement>.capture(value: String) =
    attr("capture", value) // type: file only

fun AttrsScope<HTMLInputElement>.dirName(value: String) =
    attr("dirname", value) // text, search

@JvmName("disabledInput")
fun AttrsScope<HTMLInputElement>.disabled() =
    attr("disabled", "")

@JvmName("formInput")
fun AttrsScope<HTMLInputElement>.form(id: String) =
    attr("form", id)

@JvmName("formActionInput")
fun AttrsScope<HTMLInputElement>.formAction(url: String) =
    attr("formaction", url)

fun AttrsScope<HTMLInputElement>.formEncType(value: InputFormEncType) =
    attr("formenctype", value.typeStr)

fun AttrsScope<HTMLInputElement>.formMethod(value: InputFormMethod) =
    attr("formmethod", value.methodStr)

@JvmName("formNoValidateInput")
fun AttrsScope<HTMLInputElement>.formNoValidate() =
    attr("formnovalidate", "")

fun AttrsScope<HTMLInputElement>.formTarget(value: InputFormTarget) =
    attr("formtarget", value.targetStr)

@JvmName("heightInput")
fun AttrsScope<HTMLInputElement>.height(value: Int) =
    attr("height", value.toString()) // image only

@JvmName("widthInput")
fun AttrsScope<HTMLInputElement>.width(value: Int) =
    attr("width", value.toString()) // image only

@JvmName("widthCanvas")
fun AttrsScope<HTMLCanvasElement>.width(value: Int) =
    attr("width", value.toString())

@JvmName("heightCanvas")
fun AttrsScope<HTMLCanvasElement>.height(value: Int) =
    attr("height", value.toString())

fun AttrsScope<HTMLInputElement>.list(dataListId: String) =
    attr("list", dataListId)

fun AttrsScope<HTMLInputElement>.max(value: String) =
    attr("max", value)

@JvmName("maxLengthInput")
fun AttrsScope<HTMLInputElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

fun AttrsScope<HTMLInputElement>.min(value: String) =
    attr("min", value)

@JvmName("minLengthInput")
fun AttrsScope<HTMLInputElement>.minLength(value: Int) =
    attr("minlength", value.toString())

@JvmName("multipleInput")
fun AttrsScope<HTMLInputElement>.multiple() =
    attr("multiple", "")

@JvmName("nameInput")
fun AttrsScope<HTMLInputElement>.name(value: String) =
    attr("name", value)

fun AttrsScope<HTMLInputElement>.pattern(value: String) =
    attr("pattern", value)

@JvmName("placeholderInput")
fun AttrsScope<HTMLInputElement>.placeholder(value: String) =
    attr("placeholder", value)

@JvmName("readOnlyInput")
fun AttrsScope<HTMLInputElement>.readOnly() =
    attr("readonly", "")

@Deprecated(
    message = "Please use `required()` without parameters. Use if..else.. if conditional behaviour required.",
    replaceWith = ReplaceWith("required()", "org.jetbrains.compose.web.attributes.required"),
    level = DeprecationLevel.WARNING
)
fun AttrsScope<HTMLInputElement>.required(value: Boolean = true) =
    attr("required", value.toString())

@JvmName("requiredInput")
fun AttrsScope<HTMLInputElement>.required() =
    attr("required", "")

@JvmName("sizeInput")
fun AttrsScope<HTMLInputElement>.size(value: Int) =
    attr("size", value.toString())

@JvmName("srcInput")
fun AttrsScope<HTMLInputElement>.src(value: String) =
    attr("src", value) // image only

fun AttrsScope<HTMLInputElement>.step(value: Number) =
    attr("step", value.toString()) // numeric types only

/* Option attributes */

@JvmName("valueOption")
fun AttrsScope<HTMLOptionElement>.value(value: String) =
    attr("value", value)

@JvmName("disabledOption")
fun AttrsScope<HTMLOptionElement>.disabled() =
    attr("disabled", "")

fun AttrsScope<HTMLOptionElement>.selected() =
    attr("selected", "")

@JvmName("labelOption")
fun AttrsScope<HTMLOptionElement>.label(value: String) =
    attr("label", value)

/* Select attributes */

fun AttrsScope<HTMLSelectElement>.autofocus() =
    attr("autofocus", "")

@JvmName("disabledSelect")
fun AttrsScope<HTMLSelectElement>.disabled() =
    attr("disabled", "")

@JvmName("formSelect")
fun AttrsScope<HTMLSelectElement>.form(formId: String) =
    attr("form", formId)

@JvmName("multipleSelect")
fun AttrsScope<HTMLSelectElement>.multiple() =
    attr("multiple", "")

@JvmName("nameSelect")
fun AttrsScope<HTMLSelectElement>.name(value: String) =
    attr("name", value)

@JvmName("requiredSelect")
fun AttrsScope<HTMLSelectElement>.required() =
    attr("required", "")

@JvmName("sizeSelect")
fun AttrsScope<HTMLSelectElement>.size(numberOfRows: Int) =
    attr("size", numberOfRows.toString())

/* OptGroup attributes */

@JvmName("labelOptGroup")
fun AttrsScope<HTMLOptGroupElement>.label(value: String) =
    attr("label", value)

@JvmName("disabledOptGroup")
fun AttrsScope<HTMLOptGroupElement>.disabled() =
    attr("disabled", "")

/* TextArea attributes */

@JvmName("autoFocusTextArea")
fun AttrsScope<HTMLTextAreaElement>.autoFocus() =
    attr("autofocus", "")

fun AttrsScope<HTMLTextAreaElement>.cols(value: Int) =
    attr("cols", value.toString())

@JvmName("disabledTextArea")
fun AttrsScope<HTMLTextAreaElement>.disabled() =
    attr("disabled", "")

@JvmName("formTextArea")
fun AttrsScope<HTMLTextAreaElement>.form(formId: String) =
    attr("form", formId)

@JvmName("maxLengthTextArea")
fun AttrsScope<HTMLTextAreaElement>.maxLength(value: Int) =
    attr("maxlength", value.toString())

@JvmName("minLengthTextArea")
fun AttrsScope<HTMLTextAreaElement>.minLength(value: Int) =
    attr("minlength", value.toString())

@JvmName("nameTextArea")
fun AttrsScope<HTMLTextAreaElement>.name(value: String) =
    attr("name", value)

@JvmName("placeholderTextArea")
fun AttrsScope<HTMLTextAreaElement>.placeholder(value: String) =
    attr("placeholder", value)

@JvmName("readOnlyTextArea")
fun AttrsScope<HTMLTextAreaElement>.readOnly() =
    attr("readonly", "")

@JvmName("requiredTextArea")
fun AttrsScope<HTMLTextAreaElement>.required() =
    attr("required", "")

fun AttrsScope<HTMLTextAreaElement>.rows(value: Int) =
    attr("rows", value.toString())

fun AttrsScope<HTMLTextAreaElement>.wrap(value: TextAreaWrap) =
    attr("wrap", value.str)

/* Img attributes */

@JvmName("srcImage")
fun AttrsScope<HTMLImageElement>.src(value: String): AttrsScope<HTMLImageElement> =
    attr("src", value)

@JvmName("altImage")
fun AttrsScope<HTMLImageElement>.alt(value: String): AttrsScope<HTMLImageElement> =
    attr("alt", value)


internal expect val setInputValue: (HTMLInputElement, String) -> Unit

internal expect val setTextAreaDefaultValue: (HTMLTextAreaElement, String) -> Unit

internal expect val setCheckedValue: (HTMLInputElement, Boolean) -> Unit

/* Img attributes */
fun AttrsScope<HTMLLabelElement>.forId(value: String): AttrsScope<HTMLLabelElement> =
    attr("for", value)

/* Table attributes */
fun AttrsScope<HTMLTableColElement>.span(value: Int): AttrsScope<HTMLTableColElement> =
    attr("span", value.toString())

fun AttrsScope<HTMLTableCellElement>.scope(value: Scope): AttrsScope<HTMLTableCellElement> =
    attr("scope", value.str)

fun AttrsScope<HTMLTableCellElement>.colspan(value: Int): AttrsScope<HTMLTableCellElement> =
    attr("colspan", value.toString())

fun AttrsScope<HTMLTableCellElement>.rowspan(value: Int): AttrsScope<HTMLTableCellElement> =
    attr("rowspan", value.toString())
