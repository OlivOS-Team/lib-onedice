package onedice.data

import java.util.*

class ResRecursive(val resDouble:Double=0.0,
                   val resDetail:String="",
                   val resNumberDetail: String = "",
                   val resDoubleMin:Double = 0.0,
                   val resDoubleMax:Double = 0.0
){
    companion object{
        fun getExtremum(a: ResRecursive,
                        b: ResRecursive,
                        operation:(a:Double,b:Double)->Double,
                        resultCallback:(max:Double,min:Double)->Unit){
            val arrayList = ArrayList<Double>()
            operation(a.resDoubleMax,b.resDoubleMax).apply {
                if(!this.isNaN()){
                    arrayList.add(this)
                }
            }
            operation(a.resDoubleMin,b.resDoubleMax).apply {
                if(!this.isNaN()){
                    arrayList.add(this)
                }
            }
            operation(a.resDoubleMin,b.resDoubleMin).apply {
                if(!this.isNaN()){
                    arrayList.add(this)
                }
            }
            operation(a.resDoubleMax,b.resDoubleMin).apply {
                if(!this.isNaN()){
                    arrayList.add(this)
                }
            }
            resultCallback(arrayList.maxOrNull()?: Double.NaN,arrayList.minOrNull()?: Double.NaN)
        }
    }
    fun clone(): ResRecursive {
        return ResRecursive(resDouble = resDouble,resDetail = resDetail,resDoubleMin = resDoubleMin,resDoubleMax = resDoubleMax)
    }
}