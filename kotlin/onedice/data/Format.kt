package onedice.data

object Format {
    fun build(str:String,vararg data:Any?):String{
        val strData = Array(data.size){
            if(data[it]==null){
                "null"
            }else {
                val to = data[it].toString()
                if (to.endsWith(".0")) {
                    to.substring(0, to.length - 2)
                } else {
                    to
                }
            }
        }
        var d=0
        var build = str
        while(build.indexOf("{replace}")!=-1){
            build = build.replaceFirst(oldValue = "{replace}",newValue = strData[d++],ignoreCase = false)
        }
        return build
    }
}