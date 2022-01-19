package onedice.nodes

class CalLongNode(LongData:String): CalNode(LongData, NodeType.NUMBER){
    fun getDouble():Double{
        return try{
            return nodeData.toDouble()
        }catch (e:Throwable){
            0.0
        }
    }
    fun appendLong(data:Char): CalLongNode {
        if(data.isDigit()){
            super.nodeData += data
        }
        return CalLongNode(super.nodeData)
    }
}