package org.jetbrains.compose.web.internal.runtime

actual abstract class NativeNode {
    val children: MutableList<NativeNode> = mutableListOf()
}

class JvmRootNode : NativeNode()
class JvmElement(val tagName: String) : NativeNode() {
    val attributes = LinkedHashMap<String, String>()
    val styles = LinkedHashMap<String, String>()
    val classes = mutableListOf<String>()
}
class JvmTextNode(var text: String) : NativeNode()

actual fun NativeNode.insertChild(index: Int, child: NativeNode) {
    if (index < children.size) children.add(index, child) else children.add(child)
}
actual fun NativeNode.removeChildren(index: Int, count: Int) {
    repeat(count) { children.removeAt(index) }
}
actual fun NativeNode.moveChildren(from: Int, to: Int, count: Int) {
    if (from == to) return
    val moved = ArrayList<NativeNode>(count)
    repeat(count) { moved.add(children.removeAt(from)) }
    val insertAt = if (from > to) to else to - count
    children.addAll(insertAt.coerceIn(0, children.size), moved)
}
actual fun NativeNode.clearChildren() {
    children.clear()
}
