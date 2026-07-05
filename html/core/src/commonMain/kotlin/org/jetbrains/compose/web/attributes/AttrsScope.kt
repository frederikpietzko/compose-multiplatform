package org.jetbrains.compose.web.attributes

import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.web.events.NativeEventTarget
import androidx.compose.web.events.SyntheticDragEvent
import androidx.compose.web.events.SyntheticEvent
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.StyleScopeBuilder
import org.jetbrains.compose.web.dom.DomElement
import org.jetbrains.compose.web.dom.DomHtmlElement
import org.jetbrains.compose.web.events.SyntheticAnimationEvent
import org.jetbrains.compose.web.events.SyntheticClipboardEvent
import org.jetbrains.compose.web.events.SyntheticFocusEvent
import org.jetbrains.compose.web.events.SyntheticKeyboardEvent
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi

// Moved from jsMain to commonMain (task 2.3): org.w3c.dom.Element -> DomElement,
// org.w3c.dom.HTMLElement -> DomHtmlElement. propertyUpdates' element type was already exactly
// the shape ElementUpdater.applyProps expects (List<Pair<(DomElement, Any) -> Unit, Any>>), so no
// further changes were needed there. Dropped the unused `setClassList` val at the bottom of the
// original file (dead code, zero call sites, confirmed via grep before this move).
//
// The onClick/onMouseDown/etc. event-handler methods (plus addEventListener) were added here as
// default-bodied interface members (task 2.4) rather than on EventsListenerScope itself, since
// EventsListenerScope is an expect interface (can't hold default-bodied members) and AttrsScope
// is its only real implementer in this codebase -- see the comment atop EventsListenerScope.kt
// for the full rationale.
private typealias SyntheticMouseEventListener = (SyntheticMouseEvent) -> Unit
private typealias SyntheticWheelEventListener = (SyntheticWheelEvent) -> Unit
private typealias SyntheticDragEventListener = (SyntheticDragEvent) -> Unit

@Deprecated(
    message = "Renamed to AttrsScopeBuilder",
    replaceWith = ReplaceWith(
        expression = "AttrsScopeBuilder<T>",
        imports = arrayOf("org.jetbrains.compose.web.attributes.AttrsScopeBuilder")
    )
)
typealias AttrsBuilder<T> = AttrsScopeBuilder<T>

/**
 * [AttrsScope] is a class that is used (as a builder context, that is as AttrsBuilder<T>.() -> Unit)
 * in all DOM-element creating API calls. It's used for adding attributes to the element created,
 * adding inline style values (via [style]) and attaching events to the element (since AttrsBuilder
 * is an [EventsListenerScope])
 *
 * In that aspect the most important method is [attr]. Setting the most frequently attributes, like [id], [tabIndex]
 * are extracted to a separate methods.
 *
 */
@HtmlAttrMarker
interface AttrsScope<out TElement : DomElement> : EventsListenerScope {
    /**
     * [style] add inline CSS-style properties to the element via [StyleScope] context
     *
     * Example:
     * ```
     * Div({
     *      style { maxWidth(5.px) }
     * })
     * ```
     *
     * `attr("style", ...)` overrides everything added in `style { }` blocks
     */
    fun style(builder: StyleScope.() -> Unit)

    /**
     * [classes] adds all values passed as params to the element's classList.
     *  This method acts cumulatively, that is, each call adds values to the classList.
     *  In the ideology of Composable functions and their recomposition one just don't need to remove classes,
     *  since if your classList is, for instance, condition-dependent, you can always just call this method conditionally.
     *
     *  `attr("class", ...)` overrides everything added using `classes(...)` calls
     */
    fun classes(classes: Collection<String>)

    /**
     * [classes] adds all values passed as params to the element's classList.
     *  This method acts cumulatively, that is, each call adds values to the classList.
     *  In the ideology of Composable functions and their recomposition one just don't need to remove classes,
     *  since if your classList is, for instance, condition-dependent, you can always just call this method conditionally.
     */
    fun classes(vararg classes: String) = classes(classes.asList())

    fun id(value: String) = attr(ID, value)
    fun hidden() = attr(HIDDEN, true.toString())
    fun title(value: String) = attr(TITLE, value)
    fun dir(value: DirType) = attr(DIR, value.dirStr)
    fun draggable(value: Draggable) = attr(DRAGGABLE, value.str)
    fun contentEditable(value: Boolean) = attr(CONTENT_EDITABLE, value.toString())
    fun lang(value: String) = attr(LANG, value)
    fun tabIndex(value: Int) = attr(TAB_INDEX, value.toString())
    fun spellCheck(value: Boolean) = attr(SPELLCHECK, value.toString())

    /**
     * see https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode
     */
    fun inputMode(value: String) = attr("inputmode", value)

    /**
     * see https://developer.mozilla.org/en-US/docs/Web/HTML/Global_attributes/inputmode
     */
    fun inputMode(value: InputMode) = attr("inputmode", value.str)

    /* Mouse Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onContextMenu(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.CONTEXTMENU, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onClick(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.CLICK, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDoubleClick(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.DBLCLICK, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseDown(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSEDOWN, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseUp(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSEUP, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseEnter(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSEENTER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseLeave(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSELEAVE, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseMove(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSEMOVE, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseOut(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSEOUT, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onMouseOver(listener: SyntheticMouseEventListener) {
        registerEventListener(MouseEventListener(EventsListenerScope.MOUSEOVER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onWheel(listener: SyntheticWheelEventListener) {
        registerEventListener(MouseWheelEventListener(EventsListenerScope.WHEEL, listener))
    }

    /* Drag Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onDrag(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DRAG, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDrop(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DROP, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragStart(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DRAGSTART, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragEnd(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DRAGEND, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragOver(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DRAGOVER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragEnter(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DRAGENTER, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onDragLeave(listener: SyntheticDragEventListener) {
        registerEventListener(DragEventListener(EventsListenerScope.DRAGLEAVE, listener))
    }

    /* Clipboard Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onCopy(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(EventsListenerScope.COPY, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onCut(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(EventsListenerScope.CUT, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onPaste(listener: (SyntheticClipboardEvent) -> Unit) {
        registerEventListener(ClipboardEventListener(EventsListenerScope.PASTE, listener))
    }

    /* Keyboard Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onKeyDown(listener: (SyntheticKeyboardEvent) -> Unit) {
        registerEventListener(KeyboardEventListener(EventsListenerScope.KEYDOWN, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onKeyUp(listener: (SyntheticKeyboardEvent) -> Unit) {
        registerEventListener(KeyboardEventListener(EventsListenerScope.KEYUP, listener))
    }

    /* Focus Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onFocus(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(EventsListenerScope.FOCUS, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onBlur(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(EventsListenerScope.BLUR, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onFocusIn(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(EventsListenerScope.FOCUSIN, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onFocusOut(listener: (SyntheticFocusEvent) -> Unit) {
        registerEventListener(FocusEventListener(EventsListenerScope.FOCUSOUT, listener))
    }

    /* Touch Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchCancel(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(EventsListenerScope.TOUCHCANCEL, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchMove(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(EventsListenerScope.TOUCHMOVE, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchEnd(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(EventsListenerScope.TOUCHEND, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onTouchStart(listener: (SyntheticTouchEvent) -> Unit) {
        registerEventListener(TouchEventListener(EventsListenerScope.TOUCHSTART, listener))
    }

    /* Animation Events */

    @OptIn(ComposeWebInternalApi::class)
    fun onAnimationEnd(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(EventsListenerScope.ANIMATIONEND, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onAnimationIteration(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(EventsListenerScope.ANIMATIONITERATION, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onAnimationStart(listener: (SyntheticAnimationEvent) -> Unit) {
        registerEventListener(AnimationEventListener(EventsListenerScope.ANIMATIONSTART, listener))
    }

    @OptIn(ComposeWebInternalApi::class)
    fun onScroll(listener: (SyntheticEvent<NativeEventTarget>) -> Unit) {
        registerEventListener(GenericEventListener(EventsListenerScope.SCROLL, listener))
    }

    /**
     * [addEventListener] used for adding arbitrary events to the element. It resembles the standard DOM addEventListener method
     * @param eventName - the name of the event
     * @param options - as of now this param is always equal to Options.DEFAULT
     * @listener - event handler
     */
    // The generic <T : SyntheticEvent<out NativeEventTarget>> overload (needed by e.g.
    // InputAttrsScope.onInvalid, which passes a listener typed to a specific element subtype)
    // can't be a second AttrsScope member here: on JVM, a member fun and this generic member fun
    // erase to the identical raw-SyntheticEvent signature within the same interface, which is a
    // genuine "platform declaration clash" (unlike the case below) and @JvmName isn't allowed on
    // open/interface members. So it's kept as a top-level extension function on
    // EventsListenerScope instead (see EventsListenerScope.kt) -- it compiles into a different JVM
    // class (the file's Kt class) than this interface member, so there's no clash, and its one
    // call site (InputAttrsScope.kt) already has `import org.jetbrains.compose.web.attributes.*`.
    @OptIn(ComposeWebInternalApi::class)
    fun addEventListener(
        eventName: String,
        listener: (SyntheticEvent<NativeEventTarget>) -> Unit
    ) {
        registerEventListener(GenericEventListener(eventName, listener))
    }

    /**
     * [ref] can be used to retrieve a reference to a html element.
     * The lambda that `ref` takes in is not Composable. It will be called only once when an element added into a composition.
     * Likewise, the lambda passed in `onDispose` will be called only once when an element leaves the composition.
     *
     * Under the hood, `ref` uses [DisposableEffect](https://developer.android.com/jetpack/compose/side-effects#disposableeffect)
     */
    fun ref(effect: DisposableEffectScope.(TElement) -> DisposableEffectResult)

    /**
     * [attr] adds arbitrary attribute to the Element.
     * If it called twice for the same attribute name, attribute value will be resolved to the last call.
     *
     * @param attr - the name of the attribute
     * @param value - the value of the attribute
     *
     * For boolean attributes cast boolean value to String and pass it as value.
     */
    fun attr(attr: String, value: String): AttrsScope<TElement>

    /**
     * [prop] allows setting values of element's properties which can't be set using [attr].
     * [update] is a lambda with two parameters: `element` and `value`. `element` is a reference to a native element.
     * Some examples of properties that can set using [prop]: `value`, `checked`, `innerText`.
     *
     * Unlike [ref], lambda passed to [prop] will be invoked every time when AttrsBuilder being called during recomposition.
     * [prop] is not supposed to be used for adding listeners, subscriptions, etc.
     * Also see [ref].
     *
     * Code Example:
     * ```
     * Input(type = InputType.Text, attrs = {
     *      // This is only an example. One doesn't need to set `value` like this, since [Input] has `value(v: String)`
     *      prop({ element: HTMLInputElement, value: String -> element.value = value }, "someTextInputValue")
     * })
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : DomHtmlElement, V> prop(update: (E, V) -> Unit, value: V)

    companion object {
        const val CLASS = "class"
        const val ID = "id"
        const val HIDDEN = "hidden"
        const val TITLE = "title"
        const val DIR = "dir"
        const val DRAGGABLE = "draggable"
        const val CONTENT_EDITABLE = "contenteditable"
        const val LANG = "lang"
        const val TAB_INDEX = "tabindex"
        const val SPELLCHECK = "spellcheck"
    }
}

open class AttrsScopeBuilder<TElement : DomElement>(
    internal val eventsListenerScopeBuilder: EventsListenerScopeBuilder = EventsListenerScopeBuilder()
) : AttrsScope<TElement>, EventsListenerScope by eventsListenerScopeBuilder {
    internal val attributesMap = mutableMapOf<String, String>()
    internal val styleScope: StyleScopeBuilder = StyleScopeBuilder()
    internal val propertyUpdates = mutableListOf<Pair<(DomElement, Any) -> Unit, Any>>()
    internal var refEffect: (DisposableEffectScope.(TElement) -> DisposableEffectResult)? = null
    internal val classes: MutableList<String> = mutableListOf()

    /**
     * [classes] adds all values passed as params to the element's classList.
     *  This method acts cumulatively, that is, each call adds values to the classList.
     *  In the ideology of Composable functions and their recomposition one just don't need to remove classes,
     *  since if your classList is, for instance, condition-dependent, you can always just call this method conditionally.
     */
    override fun classes(classes: Collection<String>) {
        this.classes.addAll(classes)
    }

    /**
     * [classes] adds all values passed as params to the element's classList.
     *  This method acts cumulatively, that is, each call adds values to the classList.
     *  In the ideology of Composable functions and their recomposition one just don't need to remove classes,
     *  since if your classList is, for instance, condition-dependent, you can always just call this method conditionally.
     */
    override fun classes(vararg classes: String) {
        this.classes.addAll(classes)
    }

    /**
     * [style] add inline CSS-style properties to the element via [StyleScope] context
     *
     * Example:
     * ```
     * Div({
     *      style { maxWidth(5.px) }
     * })
     * ```
     *
     * `attr("style", ...)` overrides everything added in `style { }` blocks
     */
    override fun style(builder: StyleScope.() -> Unit) {
        styleScope.apply(builder)
    }

    /**
     * [ref] can be used to retrieve a reference to a html element.
     * The lambda that `ref` takes in is not Composable. It will be called only once when an element added into a composition.
     * Likewise, the lambda passed in `onDispose` will be called only once when an element leaves the composition.
     *
     * Under the hood, `ref` uses [DisposableEffect](https://developer.android.com/jetpack/compose/side-effects#disposableeffect)
     */
    override fun ref(effect: DisposableEffectScope.(TElement) -> DisposableEffectResult) {
        this.refEffect = effect
    }

    /**
     * [attr] adds arbitrary attribute to the Element.
     * If it called twice for the same attribute name, attribute value will be resolved to the last call.
     *
     * @param attr - the name of the attribute
     * @param value - the value of the attribute
     *
     * For boolean attributes cast boolean value to String and pass it as value.
     */
    override fun attr(attr: String, value: String): AttrsScope<TElement> {
        attributesMap[attr] = value
        return this
    }

    /**
     * [prop] allows setting values of element's properties which can't be set by ussing [attr].
     * [update] is a lambda with two parameters: `element` and `value`. `element` is a reference to a native element.
     * Some examples of properties that can set using [prop]: `value`, `checked`, `innerText`.
     *
     * Unlike [ref], lambda passed to [prop] will be invoked every time when AttrsBuilder being called during recomposition.
     * [prop] is not supposed to be used for adding listeners, subscriptions, etc.
     * Also see [ref].
     *
     * Code Example:
     * ```
     * Input(type = InputType.Text, attrs = {
     *      // This is only an example. One doesn't need to set `value` like this, since [Input] has `value(v: String)`
     *      prop({ element: HTMLInputElement, value: String -> element.value = value }, "someTextInputValue")
     * })
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    override fun <E : DomHtmlElement, V> prop(update: (E, V) -> Unit, value: V) {
        propertyUpdates.add((update to value) as Pair<(DomElement, Any) -> Unit, Any>)
    }

    internal fun collect(): Map<String, String> {
        return attributesMap
    }

    @ComposeWebInternalApi
    internal fun copyFrom(attrsScope: AttrsScopeBuilder<TElement>) {
        refEffect = attrsScope.refEffect
        styleScope.copyFrom(attrsScope.styleScope)

        attributesMap.putAll(attrsScope.attributesMap)
        propertyUpdates.addAll(attrsScope.propertyUpdates)

        eventsListenerScopeBuilder.copyListenersFrom(attrsScope.eventsListenerScopeBuilder)
    }
}
