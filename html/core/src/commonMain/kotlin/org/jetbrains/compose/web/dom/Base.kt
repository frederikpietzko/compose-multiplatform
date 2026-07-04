package org.jetbrains.compose.web.dom

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.NativeNodeWrapper
import org.jetbrains.compose.web.internal.runtime.applyAttrs
import org.jetbrains.compose.web.internal.runtime.applyClasses
import org.jetbrains.compose.web.internal.runtime.applyEventListeners
import org.jetbrains.compose.web.internal.runtime.applyProps
import org.jetbrains.compose.web.internal.runtime.applyStyle

// Moved from jsMain to commonMain (task 2.3). DomElementWrapper (the former private per-node
// update-logic wrapper) is gone -- its update methods are now the expect functions in
// ElementUpdater.kt, called through a plain NativeNodeWrapper. See ElementUpdater.kt for why this
// is behavior-preserving on JS (byte-for-byte lift of DomElementWrapper's bodies) and what JVM
// does instead (write into JvmElement's maps / inert).

@Composable
@ExplicitGroupsComposable
private inline fun <TScope, T> ComposeDomNode(
    crossinline factory: () -> T,
    elementScope: TScope,
    attrsSkippableUpdate: @Composable SkippableUpdater<T>.() -> Unit,
    content: (@Composable TScope.() -> Unit)
) {
    currentComposer.startNode()
    if (currentComposer.inserting) {
        currentComposer.createNode {
            factory()
        }
    } else {
        currentComposer.useNode()
    }

    attrsSkippableUpdate.invoke(SkippableUpdater(currentComposer))

    currentComposer.startReplaceableGroup(0x7ab4aae9)
    content.invoke(elementScope)
    currentComposer.endReplaceableGroup()
    currentComposer.endNode()
}

@OptIn(ComposeWebInternalApi::class)
@Composable
fun <TElement : DomElement> TagElement(
    elementBuilder: ElementBuilder<TElement>,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    val scope = remember { ElementScopeImpl<TElement>() }
    var refEffect: (DisposableEffectScope.(TElement) -> DisposableEffectResult)? = null

    ComposeDomNode<ElementScope<TElement>, NativeNodeWrapper>(
        factory = {
            // elementBuilder.create() returns the common NativeNode type (an unchecked cast on
            // JVM; on JS it's already the exact browser type constructed for this tag).
            @Suppress("UNCHECKED_CAST")
            val node = elementBuilder.create() as TElement
            scope.element = node
            NativeNodeWrapper(node)
        },
        attrsSkippableUpdate = {
            val attrsScope = AttrsScopeBuilder<TElement>()
            applyAttrs?.invoke(attrsScope)

            refEffect = attrsScope.refEffect

            update {
                set(attrsScope.classes) { classes -> node.applyClasses(classes) }
                set(attrsScope.styleScope) { style -> node.applyStyle(style) }
                set(attrsScope.collect()) { attrs -> node.applyAttrs(attrs) }
                set(attrsScope.eventsListenerScopeBuilder.collectListeners()) { listeners ->
                    node.applyEventListeners(listeners)
                }
                set(attrsScope.propertyUpdates) { updates -> node.applyProps(updates) }
            }
        },
        elementScope = scope,
        content = {
            content?.invoke(this)
        }
    )

    refEffect?.let { effect ->
        DisposableEffect(null) {
            effect.invoke(this, scope.element)
        }
    }
}

/**
 * @param tagName - the name of the tag that needs to be created.
 * It's best to use constant values for [tagName].
 * If variable [tagName] needed, consider wrapping TagElement calls into an if...else:
 *
 * ```
 *      if (useDiv) TagElement("div",...) else TagElement("span", ...)
 * ```
 */
@Composable
fun <TElement : DomElement> TagElement(
    tagName: String,
    applyAttrs: (AttrsScope<TElement>.() -> Unit)?,
    content: (@Composable ElementScope<TElement>.() -> Unit)?
) {
    key(tagName) {
        TagElement(
            elementBuilder = ElementBuilder.createBuilder(tagName),
            applyAttrs = applyAttrs,
            content = content
        )
    }
}
