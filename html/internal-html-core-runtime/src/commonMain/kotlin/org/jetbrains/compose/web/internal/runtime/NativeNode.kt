package org.jetbrains.compose.web.internal.runtime

expect abstract class NativeNode

expect fun NativeNode.insertChild(index: Int, child: NativeNode)
expect fun NativeNode.removeChildren(index: Int, count: Int)
expect fun NativeNode.moveChildren(from: Int, to: Int, count: Int)
expect fun NativeNode.clearChildren()

@ComposeWebInternalApi
open class NativeNodeWrapper(open val node: NativeNode) {
    fun insert(index: Int, wrapper: NativeNodeWrapper) = node.insertChild(index, wrapper.node)
    fun remove(index: Int, count: Int) = node.removeChildren(index, count)
    fun move(from: Int, to: Int, count: Int) = node.moveChildren(from, to, count)
}
