package onedice.nodes

import onedice.data.ICalNode

open class CalNode(var nodeData:String, private val type: NodeType): ICalNode {
    enum class NodeType{
        NUMBER,OPERATION//,MAX
    }
    override fun toString(): String {
        return String.format("<calNode '%s' %s>",nodeData, type)
    }
    fun isLong():Boolean{
        return type == NodeType.NUMBER
    }
    fun isOperation():Boolean{
        return type == NodeType.OPERATION
    }
    override fun getPriority(): Short? {
        return null
    }
    override fun inOperation(): Boolean {
        return false
    }
}