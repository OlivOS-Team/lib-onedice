package onedice.nodes

import onedice.data.CalOperationDefault
import onedice.Roll
import java.util.*

class CalOperationNode(val operationData:Char, private val customDefault: HashMap<Char, CalOperationDefault>?=null): CalNode(operationData.toLowerCase().toString(),
    NodeType.OPERATION
){
    val vals = HashMap<Char,Double?>()
    val valsDefault = HashMap<Char,Double>()
    var valLeftDefault:Double? = null
    var valRightDefault:Double? = null
    var valStarterLeftDefault:Double? = null
    val valpriority:Short? = null
    init{
        initOperation()
    }
    fun initOperation(){
        if(inOperation()){
            getPriority()
        }
        when(operationData){
            '-'->{
                valStarterLeftDefault = 0.0
            }
            'd'->{
                valLeftDefault = 1.0
                valRightDefault = 100.0
                vals['k'] = null
                vals['q'] = null
                vals['p'] = null
                vals['b'] = null
                vals['a'] = null
                valsDefault['p'] = 1.0
                valsDefault['b'] = 1.0
            }
            'a'->{
                vals['k'] = 8.0
                vals['m'] = 10.0
            }
            'c'->{
                vals['m'] = 10.0
            }
            'b'->{
                valLeftDefault = 1.0
                valRightDefault = 1.0
            }
            'p'->{
                valLeftDefault = 1.0
                valRightDefault = 1.0
            }
            'f'->{
                valLeftDefault = 4.0
                valRightDefault = 3.0
            }
        }
        customDefault?.let { customDefault->
            if(customDefault.containsKey(operationData)){
                val defaults = customDefault[operationData] as CalOperationDefault
                if(defaults.leftD!=null){
                    valLeftDefault = defaults.leftD
                }
                if(defaults.rightD!=null){
                    valRightDefault = defaults.rightD
                }
                if(defaults.sub!=null){
                    val sub = defaults.sub
                    for(valThis in vals){
                        val key = valThis.key
                        if(sub.containsKey(key)){
                            vals[key] = sub[key] as Double
                        }
                    }
                }
                if(defaults.subD!=null){
                    val sub = defaults.subD
                    for(valThis in vals){
                        val key = valThis.key
                        if(sub.containsKey(key)){
                            valsDefault[key] = sub[key] as Double
                        }
                    }
                }
            }
        }
    }
    override fun getPriority(): Short? {
        return Roll.dictOperationPriority[nodeData[0]]?:valpriority
    }
    override fun inOperation(): Boolean {
        return Roll.dictOperationPriority.containsKey(nodeData[0])
    }
}