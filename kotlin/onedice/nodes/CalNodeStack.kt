package onedice.nodes

import java.util.*

class CalNodeStack: Stack<CalNode>() {
    override fun push(item: CalNode?): CalNode {
        return super.push(item)
    }
    override fun peek(): CalNode? {
        return try {
            super.peek()
        }catch (e:Throwable){
            null
        }
    }
    fun pushList(data: ArrayList<CalNode>){
        data.forEach {
            push(it)
        }
    }
    fun popTo(but:String): ArrayList<CalNode> {
        val res= ArrayList<CalNode>()
        while(size > 0) {
            val peek = peek() ?: break
            if(peek.nodeData==but){
                break
            }
            res.add(pop())
        }
        if(size > 0) {
            pop()
        }
        return res
    }
    fun popTo(but:String, priority:Short, saveBut:Boolean): ArrayList<CalNode> {
        val res= ArrayList<CalNode>()
        while(size>0){
            val peek = peek() ?: break
            if(peek.nodeData==but){
                break
            }
            val peekPriority = peek.getPriority()
            if(peekPriority!= null) {
                if (peekPriority < priority) {
                    break
                }
            }
            res.add(pop())
        }
        if(size>0){
            if((peek() as CalNode).nodeData == but && !saveBut){
                pop()
            }
        }
        return res
    }
}