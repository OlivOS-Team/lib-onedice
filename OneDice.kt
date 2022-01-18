import java.util.*
import kotlin.math.pow
/*
   ____  _   ____________  ________________
  / __ \/ | / / ____/ __ \/  _/ ____/ ____/
 / / / /  |/ / __/ / / / // // /   / __/
/ /_/ / /|  / /___/ /_/ // // /___/ /___
\____/_/ |_/_____/_____/___/\____/_____/
@File      :   OneDice.kt
@Author    :   赵怡然
@Contact   :   trpgbot.com
@License   :   AGPL
@Copyright :   (C) 2020-2021, OlivOS-Team
@Desc      :   由onedice.py重写的kotlin实现（尚未润色代码）
*
* */
class OneDice {
    companion object{
        val dictOperationPriority = HashMap<Char,Long?>().apply {
            this['(']=null
            this[')']=null
            this['+']=1
            this['-']=1
            this['*']=2
            this['x']=2
            this['X']=2
            this['/']=2
            this['^']=3
            this['d']=4
            this['D']=4
            this['a']=4
            this['A']=4
            this['c']=4
            this['C']=4
            this['f']=4
            this['F']=4
            this['b']=4
            this['B']=4
            this['p']=4
            this['P']=4

        }
        val listOperationSub = arrayOf('m',
            'M',
            'k',
            'K',
            'q',
            'Q',
            'b',
            'B',
            'p',
            'P')
        @JvmStatic
        fun main(s:Array<String>){
            val str_para_list = arrayOf(
                "10d10-7a5k7*10+6",
                "10d10",
                "1d(7a5k7)a(1d4)",
                "(1d100)^(7a5k7)",
                "(1d100)^(1d20)",
                "7a5k7"
            )
            for(str_para in str_para_list){
                val rd_para = RD(str_para)
                println(rd_para.originData)
                rd_para.roll()
                println("----------------")
                if(rd_para.resError != null){
                    println(rd_para.resError)
                }else{
                    println(rd_para.resLong)
                    println(rd_para.resLongMax)
                    println(rd_para.resLongMin)
                    println(rd_para.resLongMaxType)
                    println(rd_para.resLongMinType)
                    println(rd_para.resDetail)
                }
                println("================")
            }
        }
    }
    class CalOperationDefault(
        val leftD:Long?=null,
        val rightD:Long?=null,
        val sub:HashMap<Char,Long>?=null,
        val subD:HashMap<Char,Long>?=null
    )
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
        fun pushList(data:ArrayList<CalNode>){
            data.forEach {
                push(it)
            }
        }
        fun popTo(but:String):ArrayList<CalNode>{
            val res=ArrayList<CalNode>()
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
        fun popTo(but:String, priority:Long, saveBut:Boolean):ArrayList<CalNode>{
            val res=ArrayList<CalNode>()
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
    interface ICalNode{
        fun getPriority():Long?
        fun inOperation():Boolean
    }
    open class CalNode(var nodeData:String, val type:NodeType):ICalNode{
        enum class NodeType{
            Long,OPERATION,MAX
        }

        override fun toString(): String {
            return String.format("<calNode '%s' %s>",nodeData, type)
        }
        fun isLong():Boolean{
            return type == NodeType.Long
        }
        fun isOperation():Boolean{
            return type == NodeType.OPERATION
        }
        override fun getPriority(): Long? {
            return null
        }
        override fun inOperation(): Boolean {
            return false
        }
    }
    class CalLongNode(LongData:String): CalNode(LongData,NodeType.Long){
        var num:Long=0
        fun getLong():Long{
            return try{
                return nodeData.toLong()
            }catch (e:Throwable){
                0
            }
        }
        fun appendLong(data:Char):CalLongNode{
            if(data.isDigit()){
                super.nodeData += data
            }
            return CalLongNode(super.nodeData)
        }
    }
    class CalOperationNode(val operationData:Char, private val customDefault:HashMap<Char,CalOperationDefault>?=null): CalNode(operationData.toLowerCase().toString(),NodeType.OPERATION){
        val vals = HashMap<Char,Long?>()
        val valsDefault = HashMap<Char,Long>()
        var valLeftDefault:Long? = null
        var valRightDefault:Long? = null
        var valStarterLeftDefault:Long? = null
        val valEnderRightDefault:Any?=null
        val valpriority:Long? = null
        val dictOperationPriority = OneDice.dictOperationPriority
        init{
            initOperation()
        }
        fun initOperation(){
            if(inOperation()){
                getPriority()
            }
            when(operationData){
                '-'->{
                    valStarterLeftDefault = 0
                }
                'd'->{
                    valLeftDefault = 1
                    valRightDefault = 100
                    vals['k'] = null
                    vals['q'] = null
                    vals['p'] = null
                    vals['b'] = null
                    valsDefault['p'] = 1
                    valsDefault['b'] = 1
                }
                'a'->{
                    vals['k'] = 8
                    vals['m'] = 10
                }
                'c'->{
                    vals['m'] = 10
                }
                'b'->{
                    valLeftDefault = 1
                    valRightDefault = 1
                }
                'p'->{
                    valLeftDefault = 1
                    valRightDefault = 1
                }
                'f'->{
                    valLeftDefault = 4
                    valRightDefault = 3
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
                        for(val_this in vals){
                            val key = val_this.key
                            if(sub.containsKey(key)){
                                vals[key] = sub[key] as Long
                            }
                        }
                    }
                    if(defaults.subD!=null){
                        val sub = defaults.subD
                        for(val_this in vals){
                            val key = val_this.key
                            if(sub.containsKey(key)){
                                valsDefault[key] = sub[key] as Long
                            }
                        }
                    }
                }
            }
        }
        override fun getPriority(): Long? {
            return OneDice.dictOperationPriority[nodeData[0]]?:valpriority
        }
        override fun inOperation(): Boolean {
            return OneDice.dictOperationPriority.containsKey(nodeData[0])
        }
    }
    class RD(initData:String, val customDefault:HashMap<Char,CalOperationDefault>?=null){
        val originData = initData.lowercase()
        var calTree = CalNodeStack()
        var resLong:Long? = null
        var resLongMin:Long? = null
        var resLongMax:Long? = null
        var resLongMinType:ResExtremeType?=null
        var resLongMaxType:ResExtremeType?=null
        var resDetail:String=""
        var resError:ResErrorType?=null
        val dictOperationPriority = OneDice.dictOperationPriority
        enum class ResErrorType{
            UNKNOWN_GENERATE_FATAL,
            UNKNOWN_COMPLETE_FATAL,
            INPUT_RAW_INVALID,
            INPUT_CHILD_PARA_INVALID,
            INPUT_NODE_OPERATION_INVALID,
            NODE_OPERATION_INVALID,
            NODE_STACK_EMPTY,
            NODE_LEFT_VAL_INVALID,
            NODE_RIGHT_VAL_INVALID,
            NODE_SUB_VAL_INVALID,
            NODE_EXTREME_VAL_INVALID,
        }
        enum class ResExtremeType{
            INT_LIMITED,
            INT_POSITIVE_INFINITE,
            INT_NEGATIVE_INFINITE,
        }
        class ResRecursive(var resLong:Long=0,var resDetail:String=""){
            var resLongMin:Long = 0
            var resLongMax:Long = 0
            var resLongMinType = ResExtremeType.INT_LIMITED
            var resLongMaxType = ResExtremeType.INT_LIMITED
        }
        fun getPriority(nodeData: Char): Long? {
            return OneDice.dictOperationPriority[nodeData]
        }
        fun inOperation(nodeData: Char): Boolean {
            return OneDice.dictOperationPriority.containsKey(nodeData)
        }
        fun roll(){
            try{
                __getCalTree()
            }catch (e:Throwable){
                if(resError==null){
                    resError = ResErrorType.UNKNOWN_GENERATE_FATAL
                }
            }
            if(resError!=null){
                return
            }
            try{
                val resRecursiveObj = __calculate()
                if(resRecursiveObj!=null) {
                    resLong = resRecursiveObj.resLong
                    resLongMin = resRecursiveObj.resLongMin
                    resLongMax = resRecursiveObj.resLongMax
                    resLongMinType = resRecursiveObj.resLongMinType
                    resLongMaxType = resRecursiveObj.resLongMaxType
                    resDetail = resRecursiveObj.resDetail
                    return
                }
            }catch (e:Throwable){
                e.printStackTrace()
            }
            if(resError==null){
                resError = ResErrorType.UNKNOWN_GENERATE_FATAL
            }
        }
        private fun random(nMin:Long,nMax:Long):Long{
            return (nMin..nMax).random()
        }
        private fun random(nMin:Int,nMax:Int):Int{
            return (nMin..nMax).random()
        }
        private fun __calculate(rootPriority:Long = 0, forkSideRight:Boolean = true, rootData:Char?=null): ResRecursive? {
            val resNoneTemplate = ResRecursive()
            if(calTree.size <= 0) {
                resError = ResErrorType.NODE_STACK_EMPTY
                return resNoneTemplate
            }else{
                val tmp_node_this:CalNode
                var tmp_node_this_output = 0L
                var tmp_node_this_output_Max = 0L
                var tmp_node_this_output_Min = 0L
                var tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                var tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                var tmp_node_this_output_str = ""
                val calTreePeek = calTree.peek() as CalNode
                if(calTreePeek is CalLongNode){
                    calTree.pop()
                    tmp_node_this = calTreePeek
                    tmp_node_this_output = tmp_node_this.getLong()
                    tmp_node_this_output_Max = tmp_node_this.getLong()
                    tmp_node_this_output_Min = tmp_node_this.getLong()
                    tmp_node_this_output_str = tmp_node_this_output.toString()
                }
                else if(calTreePeek.isOperation()){
                    calTree.pop()
                    tmp_node_this = calTreePeek as CalOperationNode
                    val tmp_priority_this = tmp_node_this.getPriority()?:0
                    val tmp_main_val_right_obj = __calculate(tmp_priority_this, true, tmp_node_this.nodeData[0])
                    if(tmp_main_val_right_obj==null || resError != null) {
                        return resNoneTemplate
                    }
                    val tmp_main_val_left_obj = __calculate(tmp_priority_this, false, tmp_node_this.nodeData[0])
                    if(tmp_main_val_left_obj==null || resError != null) {
                        return resNoneTemplate
                    }
                    val tmp_main_val_right = ArrayList<Any>().apply {
                        add(tmp_main_val_right_obj.resLong)
                        add(tmp_main_val_right_obj.resDetail)
                    }
                    val tmp_main_val_left = ArrayList<Any>().apply {
                        add(tmp_main_val_left_obj.resLong)
                        add(tmp_main_val_left_obj.resDetail)
                    }
                    when(tmp_node_this.nodeData){
                        "+"->{
                            tmp_node_this_output = tmp_main_val_left[0] as Long + tmp_main_val_right[0] as Long
                            if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED && tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMax + tmp_main_val_right_obj.resLongMax
                            }else if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE || tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                            }else{
                                resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }
                            if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED&& tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                tmp_node_this_output_Min = tmp_main_val_left_obj.resLongMin + tmp_main_val_right_obj.resLongMin
                            }else if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE|| tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else{
                                resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }
                            tmp_node_this_output_str = StringBuilder().append(tmp_main_val_left[1]).append('+').append(tmp_main_val_right[1]).toString()
                            if(tmp_priority_this < rootPriority) {
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }
                        }
                        "-"->{
                            tmp_node_this_output = tmp_main_val_left[0] as Long - tmp_main_val_right[0] as Long
                            if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED && tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMax - tmp_main_val_right_obj.resLongMin
                            }else if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE || tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                            }else{
                                resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }
                            if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED && tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                tmp_node_this_output_Min = tmp_main_val_left_obj.resLongMin - tmp_main_val_right_obj.resLongMax
                            }else if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE||tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else{
                                resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }
                            tmp_node_this_output_str = StringBuilder().append(tmp_main_val_left[1]).append('-').append(tmp_main_val_right[1]).toString()
                            if(tmp_priority_this < rootPriority) {
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }
                        }
                        "*","x"->{
                            tmp_node_this_output = tmp_main_val_left[0] as Long * tmp_main_val_right[0] as Long
                            val tmp_node_this_output_ExtremumType_1: ResExtremeType
                            val tmp_node_this_output_ExtremumType_2: ResExtremeType
                            val tmp_node_this_output_ExtremumType_3: ResExtremeType
                            val tmp_node_this_output_ExtremumType_4: ResExtremeType
                            var tmp_node_this_output_Extremum_1 = 0L
                            var tmp_node_this_output_Extremum_2 = 0L
                            var tmp_node_this_output_Extremum_3 = 0L
                            var tmp_node_this_output_Extremum_4 = 0L
                            //##############################################
                            if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                    tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_POSITIVE_INFINITE
                                }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                    if(tmp_main_val_right_obj.resLongMax > 0){
                                        tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else if(tmp_main_val_right_obj.resLongMax < 0){
                                        tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_1 = 0
                                    }
                                }else{
                                    resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                                if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                    tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                    if((tmp_main_val_right_obj.resLongMax) > 0){
                                        tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else if((tmp_main_val_right_obj.resLongMax) < 0){
                                        tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_2 = 0
                                    }
                                }else{
                                    resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                            }else if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                    if(tmp_main_val_left_obj.resLongMin > 0){
                                        tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else if(tmp_main_val_left_obj.resLongMin < 0){
                                        tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_1 = 0
                                    }
                                }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                    tmp_node_this_output_ExtremumType_1 = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Extremum_1 = tmp_main_val_left_obj.resLongMax * tmp_main_val_right_obj.resLongMax
                                }else{
                                    resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                                if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                    if((tmp_main_val_left_obj.resLongMin) > 0){
                                        tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else if((tmp_main_val_left_obj.resLongMin) < 0){
                                        tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_2 = 0
                                    }
                                }else if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                    tmp_node_this_output_ExtremumType_2 = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Extremum_2 = tmp_main_val_left_obj.resLongMax * tmp_main_val_right_obj.resLongMin
                                }else{
                                    resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                            }else{
                                resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }

                            //##############################################
                            if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                    tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                    if((tmp_main_val_right_obj.resLongMax) > 0){
                                        tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else if((tmp_main_val_right_obj.resLongMax) < 0){
                                        tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_3 = 0
                                    }
                                }else{
                                    resError= ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                                if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                    tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_POSITIVE_INFINITE
                                }else if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                    if(tmp_main_val_right_obj.resLongMin > 0){
                                        tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else if(tmp_main_val_right_obj.resLongMin < 0){
                                        tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_4 = 0
                                    }
                                }else{
                                    resError= ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                            }else if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                    if(tmp_main_val_left_obj.resLongMin > 0){
                                        tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else if(tmp_main_val_left_obj.resLongMin < 0){
                                        tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_3 = 0
                                    }
                                }
                                else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                    tmp_node_this_output_ExtremumType_3 = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Extremum_3 = tmp_main_val_left_obj.resLongMin * tmp_main_val_right_obj.resLongMax
                                }
                                else{
                                    resError= ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                                //##############################################
                                if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                    if((tmp_main_val_left_obj.resLongMin) > 0){
                                        tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else if((tmp_main_val_left_obj.resLongMin) < 0){
                                        tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_POSITIVE_INFINITE
                                    }else{
                                        tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Extremum_4 = 0
                                    }
                                }
                                else if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                    tmp_node_this_output_ExtremumType_4 = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Extremum_4 = tmp_main_val_left_obj.resLongMin * tmp_main_val_right_obj.resLongMin
                                }
                                else{
                                    resError= ResErrorType.NODE_EXTREME_VAL_INVALID
                                    return resNoneTemplate
                                }
                            }else{
                                resError= ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }
                            //##############################################
                            if(
                                tmp_node_this_output_ExtremumType_1 == ResExtremeType.INT_POSITIVE_INFINITE||
                                tmp_node_this_output_ExtremumType_2 == ResExtremeType.INT_POSITIVE_INFINITE||
                                tmp_node_this_output_ExtremumType_3 == ResExtremeType.INT_POSITIVE_INFINITE||
                                tmp_node_this_output_ExtremumType_4 == ResExtremeType.INT_POSITIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                            }
                            else{
                                var flag_is_INT_LIMITED = false
                                var tmp_Extremum:Long = 0
                                if(tmp_node_this_output_ExtremumType_1 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_1) > tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_1
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(tmp_node_this_output_ExtremumType_2 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_2) > tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_2
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(tmp_node_this_output_ExtremumType_3 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_3) > tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_3
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(tmp_node_this_output_ExtremumType_4 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_4) > tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_4
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(flag_is_INT_LIMITED){
                                    tmp_node_this_output_Max = tmp_Extremum
                                }
                            }
                            if(tmp_node_this_output_ExtremumType_1 == ResExtremeType.INT_NEGATIVE_INFINITE||
                                tmp_node_this_output_ExtremumType_2 == ResExtremeType.INT_NEGATIVE_INFINITE||
                                tmp_node_this_output_ExtremumType_3 == ResExtremeType.INT_NEGATIVE_INFINITE||
                                tmp_node_this_output_ExtremumType_4 == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }
                            else{
                                var flag_is_INT_LIMITED = false
                                var tmp_Extremum:Long = 0
                                if(tmp_node_this_output_ExtremumType_1 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_1) < tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_1
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(tmp_node_this_output_ExtremumType_2 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_2) < tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_2
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(tmp_node_this_output_ExtremumType_3 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_3) < tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_3
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(tmp_node_this_output_ExtremumType_4 == ResExtremeType.INT_LIMITED){
                                    if(!flag_is_INT_LIMITED || (tmp_node_this_output_Extremum_4) < tmp_Extremum){
                                        tmp_Extremum = tmp_node_this_output_Extremum_4
                                    }
                                    flag_is_INT_LIMITED = true
                                }
                                if(flag_is_INT_LIMITED){
                                    tmp_node_this_output_Min = tmp_Extremum
                                }
                            }
                            tmp_node_this_output_str = StringBuilder().append(tmp_main_val_left[1]).append('*').append(tmp_main_val_right[1]).toString()
                            if(tmp_priority_this < rootPriority) {
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            } else if(forkSideRight && tmp_priority_this == rootPriority) {
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }
                        }
                        "/"->{
                            if(tmp_main_val_right[0] == 0){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            tmp_node_this_output = tmp_main_val_left[0] as Long / tmp_main_val_right[0] as Long
                            //##############################################
                            if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&
                            tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&
                            tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                if(tmp_main_val_right_obj.resLongMin > 0){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = Math.min(
                                        tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMin,
                                        0
                                    )
                                }else if(tmp_main_val_right_obj.resLongMin == 0L&&tmp_main_val_left_obj.resLongMin >= 0L){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }
                                else{
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&
                            tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                if(tmp_main_val_right_obj.resLongMin > 0L){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = Math.max(
                                        tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMin,
                                        0L
                                    )
                                }else if(tmp_main_val_right_obj.resLongMin == 0L&&tmp_main_val_left_obj.resLongMax <= 0L){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                }
                            }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&
                            tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                if(tmp_main_val_right_obj.resLongMin > 0){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = Math.max(
                                        tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMin,
                                        0
                                    )
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = Math.min(
                                        tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMin,
                                        0
                                    )
                                }
                                if(tmp_main_val_right_obj.resLongMin == 0L){
                                    if(tmp_main_val_left_obj.resLongMax <= 0L){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    }
                                    if(tmp_main_val_left_obj.resLongMin >= 0L){
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                            tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE&&
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE&&
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                if(tmp_main_val_right_obj.resLongMax < 0L){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = Math.max(
                                        tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMax,
                                        0
                                    )
                                }else if(tmp_main_val_right_obj.resLongMax == 0L &&tmp_main_val_left_obj.resLongMin >= 0L){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                }
                            }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                            tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE&&
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                if(tmp_main_val_right_obj.resLongMax < 0L){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = Math.min(
                                        tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMax,
                                        0
                                    )
                                }else if(tmp_main_val_right_obj.resLongMax == 0L&&
                                    tmp_main_val_left_obj.resLongMax <= 0L){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }
                                else{
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(
                            tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE&&
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                if(tmp_main_val_right_obj.resLongMax < 0){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = Math.max(
                                        tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMax,
                                        0
                                    )
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = Math.min(
                                        tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMax,
                                        0
                                    )
                                }
                                if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_left_obj.resLongMax <= 0){
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }
                                    if(tmp_main_val_left_obj.resLongMin >= 0){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                    }
                                    else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    }
                                }
                                else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED&&
                            tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                if(tmp_main_val_right_obj.resLongMin > 0){
                                    if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = Math.max(
                                            tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMin,
                                            tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMax
                                        )
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    }
                                    if(tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = Math.min(
                                            tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMin,
                                            tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMax
                                        )
                                    }else{
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }
                                }else if(tmp_main_val_right_obj.resLongMin == 0L){
                                    if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                        if(tmp_main_val_left_obj.resLongMax > 0){
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                        }else{
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMax
                                        }
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    }
                                    if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                        if(tmp_main_val_left_obj.resLongMin < 0){
                                            tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                        }else{
                                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Min = tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMax
                                        }
                                    }else{
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax < 0){
                                    if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = Math.max(
                                            tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMax,
                                            tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMin
                                        )
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    }
                                    if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = Math.min(
                                            tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMax,
                                            tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMin
                                        )
                                    }else{
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED){
                                        if(tmp_main_val_left_obj.resLongMin < 0){
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                        }else{
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMin / tmp_main_val_right_obj.resLongMin
                                        }
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    }
                                    if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED){
                                        if(tmp_main_val_left_obj.resLongMax > 0){
                                            tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                        }else{
                                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Min = tmp_main_val_left_obj.resLongMax / tmp_main_val_right_obj.resLongMin
                                        }
                                    }else{
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else{
                                resError = ResErrorType.NODE_EXTREME_VAL_INVALID
                                return resNoneTemplate
                            }
                            //##############################################
                            tmp_node_this_output_str = StringBuilder().append(tmp_main_val_left[1]).append('/').append(tmp_main_val_right[1]).toString()
                            if(tmp_priority_this < rootPriority){
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }else if(forkSideRight && tmp_priority_this == rootPriority){
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }
                        }
                        "^"->{
                            if(tmp_main_val_left[0] == 0 && tmp_main_val_right[0] == 0){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) >= 10000){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_right[0] as Long) >= 10000){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            tmp_node_this_output = (tmp_main_val_left[0] as Long).toDouble().pow((tmp_main_val_right[0] as Long).toDouble()).toLong()
                            if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                if(tmp_main_val_left_obj.resLongMin < -1){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(tmp_main_val_left_obj.resLongMin == -1L){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(tmp_main_val_left_obj.resLongMin == 0L){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else{
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 1
                                }
                            } else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                if(tmp_main_val_left_obj.resLongMax < -1){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == -1L,
                                        tmp_main_val_left_obj.resLongMin < -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == -1L,
                                        tmp_main_val_left_obj.resLongMin == -1L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 0L,
                                        tmp_main_val_left_obj.resLongMin < -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 0L,
                                        tmp_main_val_left_obj.resLongMin == -1L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 0L,
                                        tmp_main_val_left_obj.resLongMin == 0L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 1L,
                                        tmp_main_val_left_obj.resLongMin < -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 1L,
                                        tmp_main_val_left_obj.resLongMin == -1L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 1L,
                                        tmp_main_val_left_obj.resLongMin == 0L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 1L,
                                        tmp_main_val_left_obj.resLongMin == 1L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 1
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax > 1,
                                        tmp_main_val_left_obj.resLongMin < -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax > 1,
                                        tmp_main_val_left_obj.resLongMin == -1L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                if(tmp_main_val_right_obj.resLongMax <= 0){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                if(tmp_main_val_right_obj.resLongMax < 0){
                                    if(tmp_main_val_left_obj.resLongMax < -1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_left_obj.resLongMax < -1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                if(tmp_main_val_right_obj.resLongMax < 0){
                                    if(tmp_main_val_left_obj.resLongMin > 1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(tmp_main_val_left_obj.resLongMin >= 0){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_left_obj.resLongMin >= 0){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE
                                ))){
                                if(tmp_main_val_right_obj.resLongMax < 0){
                                    if(tmp_main_val_left_obj.resLongMax < -1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(tmp_main_val_left_obj.resLongMax == -1L){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 0L,
                                            tmp_main_val_left_obj.resLongMin <= -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 0L,
                                            tmp_main_val_left_obj.resLongMin == 0L
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 1L,
                                            tmp_main_val_left_obj.resLongMin <= -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 1L,
                                            tmp_main_val_left_obj.resLongMin > -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax > 1,
                                            tmp_main_val_left_obj.resLongMin <= -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_left_obj.resLongMax < -1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(tmp_main_val_left_obj.resLongMax == -1L){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 0L,
                                            tmp_main_val_left_obj.resLongMin <= -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 0L,
                                            tmp_main_val_left_obj.resLongMin == 0L
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 1L,
                                            tmp_main_val_left_obj.resLongMin <= -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax == 1L,
                                            tmp_main_val_left_obj.resLongMin > -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(boolByListAnd(arrayOf(
                                            tmp_main_val_left_obj.resLongMax > 1,
                                            tmp_main_val_left_obj.resLongMin <= -1
                                        ))){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                if(tmp_main_val_left_obj.resLongMin > 0){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else if(tmp_main_val_left_obj.resLongMin == -1L){
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else{
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                if(tmp_main_val_left_obj.resLongMax < -1){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(tmp_main_val_left_obj.resLongMin < -1){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax <= 0,
                                        tmp_main_val_left_obj.resLongMin == -1L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 0L,
                                        tmp_main_val_left_obj.resLongMin == 0L
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 1L,
                                        tmp_main_val_left_obj.resLongMin <= -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax == 1L,
                                        tmp_main_val_left_obj.resLongMin > -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 1
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax > 1,
                                        tmp_main_val_left_obj.resLongMin <= -1
                                    ))){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                if(tmp_main_val_right_obj.resLongMax < 0){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = -1
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 1
                                    }
                                }else{
                                    if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else{
                                        if(tmp_main_val_right_obj.resLongMax % 2 == 0L){
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Min = 0
                                        }else{
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                            tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                        }
                                    }
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                if(tmp_main_val_right_obj.resLongMax < -1){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }else if(tmp_main_val_right_obj.resLongMax == -1L){
                                    if(tmp_main_val_left_obj.resLongMax < -1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                        if(tmp_main_val_left_obj.resLongMax < -1){
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Max = 1
                                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Min = 0
                                        }else{
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Max = 1
                                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Min = -1
                                        }
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 1
                                    }
                                }else{
                                    if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                        if(tmp_main_val_right_obj.resLongMax == 1L){
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Max = Math.max(
                                                tmp_main_val_left_obj.resLongMax.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong(),
                                                0L
                                            )
                                        }else{
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                        }
                                        tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                    }else{
                                        if(tmp_main_val_right_obj.resLongMax % 2 == 0L){
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Min = 0
                                        }else{
                                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                            tmp_node_this_output_Max = Math.max(
                                                tmp_main_val_left_obj.resLongMax.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong(),
                                                0L
                                            )
                                            tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                                        }
                                    }
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                if(tmp_main_val_right_obj.resLongMax < -1){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = 0
                                    tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Min = 0
                                }
                                if(tmp_main_val_right_obj.resLongMax == -1L){
                                    if(tmp_main_val_left_obj.resLongMin > 1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 0
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else if(tmp_main_val_left_obj.resLongMin > -1){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }
                                }else if(tmp_main_val_right_obj.resLongMax == 0L){
                                    if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = -1
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Max = 1
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        tmp_node_this_output_Min = 1
                                    }
                                }else{
                                    if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        val tmp_min_val_list_1 = ArrayList<Long>()
                                        if(tmp_main_val_left_obj.resLongMin <= 1){
                                            tmp_min_val_list_1.add(1)
                                        }
                                        if(tmp_main_val_left_obj.resLongMin <= 0){
                                            tmp_min_val_list_1.add(0)
                                        }
                                        if(tmp_main_val_left_obj.resLongMin <= -1){
                                            tmp_min_val_list_1.add(-1)
                                        }
                                        tmp_min_val_list_1.add(
                                            tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong()
                                        )
                                        tmp_min_val_list_1.add(
                                            tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMin.toInt()).toLong()
                                        )
                                        if(tmp_main_val_right_obj.resLongMax > 1){
                                            tmp_min_val_list_1.add(
                                                tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt() - 1).toLong()
                                            )
                                        }
                                        tmp_node_this_output_Min = tmp_min_val_list_1.minOrNull()?:0
                                    }else{
                                        tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                        tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                        val tmp_min_val_list_1 = ArrayList<Long>()
                                        if(tmp_main_val_left_obj.resLongMin <= 1){
                                            tmp_min_val_list_1.add(1)
                                        }
                                        if(tmp_main_val_left_obj.resLongMin <= 0){
                                            tmp_min_val_list_1.add(0)
                                        }
                                        if(tmp_main_val_left_obj.resLongMin <= -1){
                                            tmp_min_val_list_1.add(-1)
                                        }
                                        tmp_min_val_list_1.add(
                                            tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMin.toInt()).toLong()
                                        )
                                        tmp_node_this_output_Min = tmp_min_val_list_1.minOrNull()?:0
                                    }
                                }
                            }else if(boolByListAnd(arrayOf(
                                    tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_left_obj.resLongMinType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_LIMITED,
                                    tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_LIMITED
                                ))){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                tmp_node_this_output_Max = 0
                                tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                tmp_node_this_output_Min = 0
                                val tmp_max_val_list_1 = ArrayList<Long>()
                                val tmp_min_val_list_1 = ArrayList<Long>()
                                tmp_max_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMax.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong()
                                )
                                tmp_min_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMax.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong()
                                )
                                tmp_max_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMax.toDouble().pow(tmp_main_val_right_obj.resLongMin.toInt()).toLong()
                                )
                                tmp_min_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMax.toDouble().pow(tmp_main_val_right_obj.resLongMin.toInt()).toLong()
                                )
                                tmp_max_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong()
                                )
                                tmp_min_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMax.toInt()).toLong()
                                )
                                tmp_max_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMin.toInt()).toLong()
                                )
                                tmp_min_val_list_1.add(
                                    tmp_main_val_left_obj.resLongMin.toDouble().pow(tmp_main_val_right_obj.resLongMin.toInt()).toLong()
                                )
                                if(tmp_main_val_right_obj.resLongMin != tmp_main_val_right_obj.resLongMax){
                                    tmp_max_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMax.toDouble().pow((tmp_main_val_right_obj.resLongMax.toInt() - 1)).toLong()
                                    )
                                    tmp_min_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMax.toDouble().pow((tmp_main_val_right_obj.resLongMax.toInt() - 1)).toLong()
                                    )
                                    tmp_max_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMax.toDouble().pow((tmp_main_val_right_obj.resLongMin.toInt() + 1)).toLong()
                                    )
                                    tmp_min_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMax.toDouble().pow((tmp_main_val_right_obj.resLongMin.toInt() + 1)).toLong()
                                    )
                                    tmp_max_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMin.toDouble().pow((tmp_main_val_right_obj.resLongMax.toInt() - 1)).toLong()
                                    )
                                    tmp_min_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMin.toDouble().pow((tmp_main_val_right_obj.resLongMax.toInt() - 1)).toLong()
                                    )
                                    tmp_max_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMin.toDouble().pow((tmp_main_val_right_obj.resLongMin.toInt() + 1)).toLong()
                                    )
                                    tmp_min_val_list_1.add(
                                        tmp_main_val_left_obj.resLongMin.toDouble().pow((tmp_main_val_right_obj.resLongMin.toInt() + 1)).toLong()
                                    )
                                }
                                if(boolByListAnd(arrayOf(
                                        tmp_main_val_right_obj.resLongMax >= 0,
                                        tmp_main_val_right_obj.resLongMin <= 0,
                                        boolByListOr(arrayOf(
                                            tmp_main_val_left_obj.resLongMax != 0L,
                                            tmp_main_val_left_obj.resLongMin != 0L
                                        ))))){
                                    tmp_max_val_list_1.add(1)
                                    tmp_min_val_list_1.add(1)
                                }
                                if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax >= -1,
                                        tmp_main_val_left_obj.resLongMin <= -1,
                                        boolByListOr(arrayOf(
                                            tmp_main_val_right_obj.resLongMax != 0L,
                                            tmp_main_val_right_obj.resLongMin != 0L
                                        ))))){
                                    tmp_max_val_list_1.add(-1)
                                    tmp_min_val_list_1.add(-1)
                                }
                                if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax >= 0,
                                        tmp_main_val_left_obj.resLongMin <= 0,
                                        boolByListOr(arrayOf(
                                            tmp_main_val_right_obj.resLongMax != 0L,
                                            tmp_main_val_right_obj.resLongMin != 0L
                                        ))))){
                                    tmp_max_val_list_1.add(0)
                                    tmp_min_val_list_1.add(0)
                                }
                                if(boolByListAnd(arrayOf(
                                        tmp_main_val_left_obj.resLongMax >= 1,
                                        tmp_main_val_left_obj.resLongMin <= 1,
                                        boolByListOr(arrayOf(
                                            tmp_main_val_right_obj.resLongMax != 0L,
                                            tmp_main_val_right_obj.resLongMin != 0L
                                        ))))){
                                    tmp_max_val_list_1.add(1)
                                    tmp_min_val_list_1.add(1)
                                }
                                tmp_node_this_output_Max = tmp_max_val_list_1.maxOrNull()?:0
                                tmp_node_this_output_Min = tmp_min_val_list_1.minOrNull()?:0
                            }
                            tmp_node_this_output_str = StringBuilder().append(tmp_main_val_left[1]).append('^').append(tmp_main_val_right[1]).toString()
                            if(tmp_priority_this < rootPriority){
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }else if(forkSideRight && tmp_priority_this == rootPriority){
                                tmp_node_this_output_str = "($tmp_node_this_output_str)"
                            }
                        }
                        "d"->{
                            if((tmp_main_val_right[0] as Long) <= 0 || (tmp_main_val_right[0] as Long) >= 10000){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) <= 0 || (tmp_main_val_left[0] as Long) >= 10000){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            var tmp_range_list = range(0, tmp_main_val_left[0] as Long)
                            tmp_node_this_output = 0
                            var tmp_node_this_output_this:Long
                            val tmp_node_this_output_list = ArrayList<Long>()
                            val tmp_node_this_output_list_2 = ArrayList<Long>()
                            tmp_node_this_output_str = ""
                            var tmp_node_this_output_str_1 = ""
                            var tmp_node_this_output_str_2 = ""
                            var tmp_node_this_output_str_3 = ""
                            if(tmp_node_this.vals['k'] != null && tmp_node_this.vals['q'] != null){
                                resError = ResErrorType.NODE_SUB_VAL_INVALID
                                return resNoneTemplate
                            }
                            if(tmp_node_this.vals['b'] != null && tmp_node_this.vals['p'] != null){
                                resError = ResErrorType.NODE_SUB_VAL_INVALID
                                return resNoneTemplate
                            }
                            if(tmp_node_this.vals['k'] != null || tmp_node_this.vals['q'] != null){
                                if(tmp_node_this.vals['b'] != null || tmp_node_this.vals['p'] != null){
                                    resError = ResErrorType.NODE_SUB_VAL_INVALID
                                    return resNoneTemplate
                                }
                            }

                            if(tmp_node_this.vals['k'] != null || tmp_node_this.vals['q'] != null){
                                for(tmp_it_this in tmp_range_list){
                                    tmp_node_this_output_this = random(1L, tmp_main_val_right[0] as Long)
                                    tmp_node_this_output_list.add(tmp_node_this_output_this)
                                }
                                if(tmp_node_this.vals['k'] != null){
                                    if((tmp_node_this.vals['k'] as Long) > tmp_node_this_output_list.size){
                                        resError = ResErrorType.NODE_SUB_VAL_INVALID
                                        return resNoneTemplate
                                    }
                                    tmp_node_this_output_list.sortWith(Collections.reverseOrder())//tmp_node_this_output_list.sort(reverse = True)
                                    tmp_range_list = range(0, tmp_node_this.vals['k'] as Long)
                                    for(tmp_it_this in tmp_range_list){
                                        val tmp_it_this_2 = tmp_it_this.toInt()
                                        tmp_node_this_output += tmp_node_this_output_list[tmp_it_this_2]
                                        tmp_node_this_output_list_2.add(tmp_node_this_output_list[tmp_it_this_2])
                                    }
                                }else if(tmp_node_this.vals['q'] != null){
                                    if((tmp_node_this.vals['q'] as Long) > tmp_node_this_output_list.size){
                                        resError = ResErrorType.NODE_SUB_VAL_INVALID
                                        return resNoneTemplate
                                    }
                                    tmp_node_this_output_list.sortWith(Collections.reverseOrder())//tmp_node_this_output_list.sort(reverse = False)
                                    tmp_range_list = range(0, tmp_node_this.vals['q'] as Long)
                                    for(tmp_it_this in tmp_range_list){
                                        val tmp_it_this_2 = tmp_it_this.toInt()
                                        tmp_node_this_output += tmp_node_this_output_list[tmp_it_this_2]
                                        tmp_node_this_output_list_2.add(tmp_node_this_output_list[tmp_it_this_2])
                                    }
                                }
                            }
                            else if(!(tmp_node_this.vals['b'] == null && tmp_node_this.vals['p'] == null)){
                                if(tmp_node_this.vals['b'] != null){
                                    if((tmp_node_this.vals['b'] as Long) <= 0 || (tmp_node_this.vals['b'] as Long) * (tmp_main_val_left[0] as Long) >= 10000){
                                        resError = ResErrorType.NODE_SUB_VAL_INVALID
                                        return resNoneTemplate
                                    }
                                    var flag_begin = true
                                    for(tmp_it_this in tmp_range_list){
                                        val tmp_rd_this = RD(String.format("1b%d",tmp_node_this.vals['b']))
                                        tmp_rd_this.roll()
                                        if(tmp_rd_this.resError != null){
                                            resError = tmp_rd_this.resError
                                            return null
                                        }else{
                                            tmp_node_this_output_this = tmp_rd_this.resLong as Long
                                            tmp_node_this_output += tmp_node_this_output_this
                                            tmp_node_this_output_list.add(tmp_node_this_output_this)
                                            if(flag_begin){
                                                flag_begin = false
                                            }else{
                                                tmp_node_this_output_str_1 += ','
                                                tmp_node_this_output_str_2 += '+'
                                            }
                                            tmp_node_this_output_str_1 += tmp_rd_this.resDetail
                                            tmp_node_this_output_str_2 += tmp_node_this_output_this.toString()
                                        }
                                    }
                                }else if(tmp_node_this.vals['p'] != null){
                                    if((tmp_node_this.vals['p'] as Long) <= 0 || (tmp_node_this.vals['p'] as Long) * (tmp_main_val_left[0] as Long) >= 10000){
                                        resError = ResErrorType.NODE_SUB_VAL_INVALID
                                        return resNoneTemplate
                                    }
                                    var flag_begin = true
                                    for(tmp_it_this in tmp_range_list){
                                        val tmp_rd_this = RD(String.format("1p%d",tmp_node_this.vals['p']))//RD('1p%d' % (tmp_node_this.vals['p'], ))
                                        tmp_rd_this.roll()
                                        if(tmp_rd_this.resError != null){
                                            resError = tmp_rd_this.resError
                                            return null
                                        }else{
                                            tmp_node_this_output_this = tmp_rd_this.resLong as Long
                                            tmp_node_this_output += tmp_node_this_output_this
                                            tmp_node_this_output_list.add(tmp_node_this_output_this)
                                            if(flag_begin){
                                                flag_begin = false
                                            }else{
                                                tmp_node_this_output_str_1 += ','
                                                tmp_node_this_output_str_2 += '+'
                                            }
                                            tmp_node_this_output_str_1 += tmp_rd_this.resDetail
                                            tmp_node_this_output_str_2 += tmp_node_this_output_this.toString()
                                        }
                                    }
                                }
                            }
                            else{
                                for(tmp_it_this in tmp_range_list){
                                    tmp_node_this_output_this = random(1, tmp_main_val_right[0] as Long)
                                    tmp_node_this_output_list.add(tmp_node_this_output_this)
                                }
                                for(tmp_node_this_output_this2 in tmp_node_this_output_list){
                                    tmp_node_this_output += tmp_node_this_output_this2
                                    tmp_node_this_output_list_2.add(tmp_node_this_output_this2)
                                }
                            }

                            if(tmp_node_this.vals['b']==null && tmp_node_this.vals['p'] == null){
                                var flag_begin = true
                                for(tmp_node_this_output_list_this in tmp_node_this_output_list){
                                    if(flag_begin){
                                        flag_begin = false
                                    }else{
                                        tmp_node_this_output_str_1 += ','
                                    }
                                    tmp_node_this_output_str_1 += tmp_node_this_output_list_this.toString()
                                }
                                flag_begin = true
                                for(tmp_node_this_output_list_this in tmp_node_this_output_list_2){
                                    if(flag_begin){
                                        flag_begin = false
                                    }else{
                                        tmp_node_this_output_str_2 += '+'
                                    }
                                    tmp_node_this_output_str_2 += tmp_node_this_output_list_this.toString()
                                }
                            }
                            if(tmp_node_this.vals['k'] == null && tmp_node_this.vals['q'] == null && tmp_node_this.vals['b'] == null && tmp_node_this.vals['p'] == null){
                                if(tmp_node_this_output_list_2.size == 1){
                                    if(rootPriority != 0L){
                                        tmp_node_this_output_str = String.format("{%s}(%d)",tmp_node_this_output_str_2, tmp_node_this_output)//'{%s}(%d)' % (tmp_node_this_output_str_2, tmp_node_this_output)
                                    }else{
                                        tmp_node_this_output_str = ""
                                    }
                                }else{
                                    tmp_node_this_output_str = String.format("{%s}(%d)",tmp_node_this_output_str_2, tmp_node_this_output)//'{%s}(%d)' % (tmp_node_this_output_str_2, tmp_node_this_output)
                                }
                            }
                            else{
                                tmp_node_this_output_str = String.format("{%s}[%s](%d)",tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)//'{%s}[%s](%d)' % (tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)
                            }

                            if(tmp_node_this.vals['b'] != null || tmp_node_this.vals['p'] != null){
                                tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMax * tmp_main_val_right_obj.resLongMax
                                tmp_node_this_output_Min = Math.max(tmp_main_val_left_obj.resLongMin * 1, 1)
                            }else if(tmp_node_this.vals['k'] != null){
                                tmp_node_this_output_Max = (tmp_node_this.vals['k'] as Long) * (tmp_main_val_right_obj.resLongMax as Long)
                                tmp_node_this_output_Min = (tmp_node_this.vals['k'] as Long) * 1
                            }else if(tmp_node_this.vals['q'] != null){
                                tmp_node_this_output_Max = (tmp_node_this.vals['q'] as Long) * (tmp_main_val_right_obj.resLongMax as Long)
                                tmp_node_this_output_Min = (tmp_node_this.vals['q'] as Long) * 1
                            }else{
                                tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMax * tmp_main_val_right_obj.resLongMax
                                tmp_node_this_output_Min = Math.max(tmp_main_val_left_obj.resLongMin * 1, 1)
                            }
                            if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_Max = 0
                            }
                            if(tmp_main_val_right_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_Max = 0
                            }
                        }
                        "a"->{
                            if((tmp_main_val_right[0] as Long) <= 1L || (tmp_main_val_right[0] as Long) >= 1000L){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) <= 0L || (tmp_main_val_left[0] as Long) >= 1000L){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_node_this.vals['m'] as Long) <= 0L || (tmp_node_this.vals['m'] as Long) >= 1000L){
                                resError = ResErrorType.NODE_SUB_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_node_this.vals['k'] as Long) <= 0 || (tmp_node_this.vals['k'] as Long) >= 1000L){
                                resError = ResErrorType.NODE_SUB_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_node_this.vals['m'] as Long) >= (tmp_node_this.vals['k'] as Long)){
                                if((tmp_node_this.vals['m'] as Long) >= tmp_main_val_right_obj.resLongMin || tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                }else{
                                    tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                    tmp_node_this_output_Max = tmp_main_val_left_obj.resLongMax * (tmp_node_this.vals['m'] as Long)
                                }
                            }else{
                                tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                tmp_node_this_output_Max = 0
                            }
                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                            tmp_node_this_output_Min = 0
                            var flag_add_roll_not_empty = true
                            val tmp_add_roll_first = tmp_main_val_left[0] as Long
                            val tmp_add_roll_threshold = tmp_main_val_right[0] as Long
                            var tmp_add_roll_count = tmp_add_roll_first
                            val tmp_add_roll_m = tmp_node_this.vals['m'] as Long
                            val tmp_add_roll_k = tmp_node_this.vals['k'] as Long
                            var tmp_node_this_output_this:Long
                            var tmp_node_this_output_list = ArrayList<Long>()
                            val tmp_node_this_output_list_list = ArrayList<ArrayList<Long>>()
                            tmp_node_this_output = 0
                            var tmp_node_this_output_1_this:Long
                            val tmp_node_this_output_1_list = ArrayList<Long>()
                            tmp_node_this_output_str = ""
                            while(flag_add_roll_not_empty){
                                val tmp_range_list = range(0, tmp_add_roll_count)
                                tmp_add_roll_count = 0
                                tmp_node_this_output_list = ArrayList<Long>()
                                tmp_node_this_output_1_this = 0
                                for(tmp_it_this in tmp_range_list){
                                    tmp_node_this_output_this = random(1L, tmp_add_roll_m)
                                    tmp_node_this_output_list.add(tmp_node_this_output_this)
                                    if(tmp_node_this_output_this >= tmp_add_roll_k){
                                        tmp_node_this_output += 1
                                        tmp_node_this_output_1_this += 1
                                    }
                                    if(tmp_node_this_output_this >= tmp_add_roll_threshold){
                                        tmp_add_roll_count += 1
                                    }
                                }
                                tmp_node_this_output_1_list.add(tmp_node_this_output_1_this)
                                tmp_node_this_output_list_list.add(tmp_node_this_output_list)
                                if(tmp_add_roll_count == 0L){
                                    flag_add_roll_not_empty = false
                                }
                            }
                            var flag_begin = true
                            for(tmp_node_this_output_list_this in tmp_node_this_output_list_list){
                                if(flag_begin){
                                    flag_begin = false
                                }else{
                                    tmp_node_this_output_str += ','
                                }
                                tmp_node_this_output_str += '{'
                                var flag_begin_2 = true
                                for(tmp_node_this_output_this2 in tmp_node_this_output_list_this){
                                    if(flag_begin_2){
                                        flag_begin_2 = false
                                    }else{
                                        tmp_node_this_output_str += ','
                                    }
                                    var tmp_node_this_output_str_this = tmp_node_this_output_this2.toString()
                                    if(tmp_node_this_output_this2 >= tmp_add_roll_k){
                                        tmp_node_this_output_str_this =
                                            "[$tmp_node_this_output_str_this]"
                                    }
                                    if(tmp_node_this_output_this2 >= tmp_add_roll_threshold){
                                        tmp_node_this_output_str_this =
                                            "<$tmp_node_this_output_str_this>"
                                    }
                                    tmp_node_this_output_str += tmp_node_this_output_str_this
                                }
                                tmp_node_this_output_str += '}'
                            }
                            val tmp_node_this_output_str_1 = tmp_node_this_output_str
                            var tmp_node_this_output_str_2 = ""
                            flag_begin = true
                            for(tmp_node_this_output_1_this2 in tmp_node_this_output_1_list){
                                if(flag_begin){
                                    flag_begin = false
                                }else{
                                    tmp_node_this_output_str_2 += '+'
                                }
                                tmp_node_this_output_str_2 += tmp_node_this_output_1_this2.toString()
                            }
                            if(tmp_node_this_output_1_list.size == 1){
                                tmp_node_this_output_str = String.format("%s(%d)",tmp_node_this_output_str_1, tmp_node_this_output)//'%s(%d)' % (tmp_node_this_output_str_1, tmp_node_this_output)
                            }else{
                                tmp_node_this_output_str = String.format("{%s}[%s](%d)",tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)//'{%s}[%s](%d)' % (tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)
                            }
                        }
                        "c"->{
                            if((tmp_main_val_right[0] as Long) <= 1L || (tmp_main_val_right[0] as Long) >= 1000L){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) <= 0L || (tmp_main_val_left[0] as Long) >= 1000L){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_node_this.vals['m'] as Long) <= 0L || (tmp_node_this.vals['m'] as Long) >= 1000L){
                                resError = ResErrorType.NODE_SUB_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_node_this.vals['m'] as Long) > tmp_main_val_right_obj.resLongMin || tmp_main_val_right_obj.resLongMinType == ResExtremeType.INT_NEGATIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                            }else{
                                tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                tmp_node_this_output_Max = (tmp_main_val_left_obj.resLongMax as Long) * (tmp_node_this.vals['m'] as Long)
                            }
                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                            tmp_node_this_output_Min = 0
                            var flag_add_roll_not_empty = true
                            val tmp_add_roll_first = tmp_main_val_left[0] as Long
                            val tmp_add_roll_threshold = tmp_main_val_right[0] as Long
                            var tmp_add_roll_count = tmp_add_roll_first
                            val tmp_add_roll_m = tmp_node_this.vals['m'] as Long
                            var tmp_node_this_output_this:Long
                            var tmp_node_this_output_list = ArrayList<Long>()
                            val tmp_node_this_output_list_list = ArrayList<ArrayList<Long>>()
                            tmp_node_this_output = 0
                            var tmp_node_this_output_1 = 0L
                            var tmp_node_this_output_2 = 0L
                            tmp_node_this_output_str = ""
                            var tmp_node_this_output_this_max =0L
                            while(flag_add_roll_not_empty){
                                val tmp_range_list = range(0L, tmp_add_roll_count)
                                tmp_add_roll_count = 0L
                                tmp_node_this_output_list = ArrayList<Long>()
                                tmp_node_this_output_this_max = 0L
                                for(tmp_it_this in tmp_range_list){
                                    tmp_node_this_output_this = random(1L, tmp_add_roll_m)
                                    tmp_node_this_output_list.add(tmp_node_this_output_this)
                                    if(tmp_node_this_output_this >= tmp_add_roll_threshold){
                                        tmp_add_roll_count += 1
                                    }
                                    if(tmp_node_this_output_this_max < tmp_node_this_output_this){
                                        tmp_node_this_output_this_max = tmp_node_this_output_this
                                    }
                                }
                                if(tmp_add_roll_count > 0){
                                    tmp_node_this_output += tmp_add_roll_m
                                    tmp_node_this_output_1 += 1
                                }else{
                                    tmp_node_this_output += tmp_node_this_output_this_max
                                    tmp_node_this_output_2 += tmp_node_this_output_this_max
                                }
                                tmp_node_this_output_list_list.add(tmp_node_this_output_list)
                                if(tmp_add_roll_count == 0L){
                                    flag_add_roll_not_empty = false
                                }
                            }
                            var flag_begin = true
                            var tmp_it_count = 0
                            val tmp_it_count_max = tmp_node_this_output_list_list.size - 1
                            for(tmp_node_this_output_list_this in tmp_node_this_output_list_list){
                                if(flag_begin){
                                    flag_begin = false
                                }else{
                                    tmp_node_this_output_str += ','
                                }
                                tmp_node_this_output_str += '{'
                                var flag_begin_2 = true
                                for(tmp_node_this_output_this2 in tmp_node_this_output_list_this){
                                    if(flag_begin_2){
                                        flag_begin_2 = false
                                    }else{
                                        tmp_node_this_output_str += ','
                                    }
                                    var tmp_node_this_output_str_this = tmp_node_this_output_this2.toString()
                                    if(tmp_node_this_output_this2 >= tmp_add_roll_threshold){
                                        tmp_node_this_output_str_this =
                                            "<$tmp_node_this_output_str_this>"
                                    }
                                    if(tmp_it_count == tmp_it_count_max && tmp_node_this_output_this_max == tmp_node_this_output_this2){
                                        tmp_node_this_output_str_this =
                                            "[$tmp_node_this_output_str_this]"
                                    }
                                    tmp_node_this_output_str += tmp_node_this_output_str_this
                                }
                                tmp_node_this_output_str += '}'
                                tmp_it_count += 1
                            }
                            val tmp_node_this_output_str_1 = tmp_node_this_output_str
                            val tmp_node_this_output_str_2 = String.format("%d*%d+%d",tmp_add_roll_m, tmp_node_this_output_1, tmp_node_this_output_2)//'%d*%d+%d' % (tmp_add_roll_m, tmp_node_this_output_1, tmp_node_this_output_2)
                            tmp_node_this_output_str = String.format("{%s}[%s](%d)",tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)//'{%s}[%s](%d)' % (tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)
                        }
                        "b"->{
                            if((tmp_main_val_right[0] as Long) >= 10000L){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) >= 10000L){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                            tmp_node_this_output_Max = 100
                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                            tmp_node_this_output_Min = 1
                            tmp_node_this_output = 0
                            var tmp_node_this_output_1 = 0L
                            val tmp_node_this_output_2 = 0
                            var tmp_node_this_output_2_mark = 10L
                            var tmp_node_this_output_this = 0L
                            val tmp_node_this_output_list = ArrayList<Long>()
                            val tmp_node_this_output_list_2 = ArrayList<Long>()
                            tmp_node_this_output_str = ""
                            var tmp_node_this_output_str_1 = ""
                            var tmp_node_this_output_str_2 = ""
                            tmp_node_this_output_this = random(1L, 100L)
                            tmp_node_this_output_1 = tmp_node_this_output_this
                            val tmp_range_list = range(0L, tmp_main_val_right[0] as Long)
                            for(tmp_it_this in tmp_range_list){
                                tmp_node_this_output_this = random(1L, 10L) - 1
                                tmp_node_this_output_list_2.add(tmp_node_this_output_this)
                                if(tmp_node_this_output_2_mark > tmp_node_this_output_this){
                                    tmp_node_this_output_2_mark = tmp_node_this_output_this
                                }
                            }
                            var tmp_node_this_output_1_1 = tmp_node_this_output_1 / 10
                            var tmp_node_this_output_1_2 = tmp_node_this_output_1 % 10
                            if(tmp_node_this_output_1_2 == 0L){
                                tmp_node_this_output_1_2 = 10
                                tmp_node_this_output_1_1 -= 1
                            }
                            if(tmp_node_this_output_1_1 > tmp_node_this_output_2_mark){
                                tmp_node_this_output = tmp_node_this_output_1_2 + tmp_node_this_output_2_mark * 10
                            }else{
                                tmp_node_this_output = tmp_node_this_output_1
                            }
                            tmp_node_this_output_str_1 = "1D100=$tmp_node_this_output_1"
                            tmp_node_this_output_str_2 = "bonus:["
                            var flag_begin = true
                            for(tmp_node_this_output_list_2_this in tmp_node_this_output_list_2){
                                if(flag_begin){
                                    flag_begin = false
                                }else{
                                    tmp_node_this_output_str_2 += ','
                                }
                                var tmp_node_this_output_list_2_this_str = tmp_node_this_output_list_2_this.toString()
                                if(tmp_node_this_output_list_2.size > 1 && tmp_node_this_output_2_mark == tmp_node_this_output_list_2_this){
                                    tmp_node_this_output_list_2_this_str = '[' + tmp_node_this_output_list_2_this_str + ']'
                                }
                                tmp_node_this_output_str_2 += tmp_node_this_output_list_2_this_str
                            }
                            tmp_node_this_output_str_2 += ']'
                            tmp_node_this_output_str = String.format("{%s %s}(%s)",tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output.toString())//'{%s %s}(%s)' % (tmp_node_this_output_str_1, tmp_node_this_output_str_2, str(tmp_node_this_output))
                        }
                        "p"->{
                            if((tmp_main_val_right[0] as Long) >= 10000L){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) >= 10000L){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                            tmp_node_this_output_Max = 100
                            tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                            tmp_node_this_output_Min = 1
                            tmp_node_this_output = 0
                            var tmp_node_this_output_1 = 0L
                            val tmp_node_this_output_2 = 0L
                            var tmp_node_this_output_2_mark = 0L
                            var tmp_node_this_output_this = 0
                            val tmp_node_this_output_list = ArrayList<Long>()
                            val tmp_node_this_output_list_2 = ArrayList<Long>()
                            tmp_node_this_output_str = ""
                            var tmp_node_this_output_str_1 = ""
                            var tmp_node_this_output_str_2 = ""
                            tmp_node_this_output_this = random(1, 100)
                            tmp_node_this_output_1 = tmp_node_this_output_this.toLong()
                            val tmp_range_list = range(0L, tmp_main_val_right[0] as Long)
                            for (tmp_it_this in tmp_range_list){
                                tmp_node_this_output_this = random(1, 10) - 1
                                tmp_node_this_output_list_2.add(tmp_node_this_output_this.toLong())
                                if(tmp_node_this_output_2_mark < tmp_node_this_output_this){
                                    tmp_node_this_output_2_mark = tmp_node_this_output_this.toLong()
                                }
                            }
                            var tmp_node_this_output_1_1 = tmp_node_this_output_1 / 10L
                            var tmp_node_this_output_1_2 = tmp_node_this_output_1 % 10L
                            if(tmp_node_this_output_1_2 == 0L){
                                tmp_node_this_output_1_2 = 10
                                tmp_node_this_output_1_1 -= 1L
                            }
                            if(tmp_node_this_output_1_1 < tmp_node_this_output_2_mark){
                                tmp_node_this_output = tmp_node_this_output_1_2 + tmp_node_this_output_2_mark * 10
                            }else{
                                tmp_node_this_output = tmp_node_this_output_1
                            }
                            tmp_node_this_output_str_1 = "1D100=" + tmp_node_this_output_1
                            tmp_node_this_output_str_2 = "punish:["
                            var flag_begin = true
                            for(tmp_node_this_output_list_2_this in tmp_node_this_output_list_2){
                                if(flag_begin){
                                    flag_begin = false
                                }else{
                                    tmp_node_this_output_str_2 += ','
                                }
                                var tmp_node_this_output_list_2_this_str = tmp_node_this_output_list_2_this.toString()
                                if(tmp_node_this_output_list_2.size > 1 && tmp_node_this_output_2_mark == tmp_node_this_output_list_2_this){
                                    tmp_node_this_output_list_2_this_str = '[' + tmp_node_this_output_list_2_this_str + ']'
                                }
                                tmp_node_this_output_str_2 += tmp_node_this_output_list_2_this_str
                            }
                            tmp_node_this_output_str_2 += ']'
                            tmp_node_this_output_str = String.format("{%s %s}(%d)",tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)
                        }
                        "f"->{
                            if((tmp_main_val_right[0] as Long) <= 1L || (tmp_main_val_right[0] as Long) >= 10000L){
                                resError = ResErrorType.NODE_RIGHT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if((tmp_main_val_left[0] as Long) <= 0L || (tmp_main_val_left[0] as Long) >= 10000L){
                                resError = ResErrorType.NODE_LEFT_VAL_INVALID
                                return resNoneTemplate
                            }
                            if(tmp_main_val_left_obj.resLongMaxType == ResExtremeType.INT_POSITIVE_INFINITE){
                                tmp_node_this_output_MaxType = ResExtremeType.INT_POSITIVE_INFINITE
                                tmp_node_this_output_MinType = ResExtremeType.INT_NEGATIVE_INFINITE
                            }else{
                                tmp_node_this_output_MaxType = ResExtremeType.INT_LIMITED
                                tmp_node_this_output_Max = tmp_main_val_left_obj.resLong * 1
                                tmp_node_this_output_MinType = ResExtremeType.INT_LIMITED
                                tmp_node_this_output_Min = tmp_main_val_left_obj.resLong * (-1)
                            }
                            val tmp_range_list = range(0L, tmp_main_val_left[0] as Long)
                            tmp_node_this_output = 0
                            val tmp_node_this_output_list = ArrayList<Int>()
                            var tmp_node_this_output_str_1 = ""
                            var tmp_node_this_output_str_2 = ""
                            val tmp_node_this_output_str_3 = ""
                            for(tmp_it_this in tmp_range_list){
                                val tmp_node_this_output_this = random(-1, 1)
                                tmp_node_this_output += tmp_node_this_output_this
                                tmp_node_this_output_list.add(tmp_node_this_output_this)
                            }
                            var flag_begin = true
                            for(tmp_node_this_output_list_this in tmp_node_this_output_list){
                                if(flag_begin){
                                    flag_begin = false
                                }else{
                                    tmp_node_this_output_str_1 += " "
                                    if(tmp_node_this_output_list_this >= 0){
                                        tmp_node_this_output_str_2 += '+'
                                    }
                                }
                                if(tmp_node_this_output_list_this < 0){
                                    tmp_node_this_output_str_1 += '-'
                                }else if(tmp_node_this_output_list_this == 0){
                                    tmp_node_this_output_str_1 += '0'
                                }else if(tmp_node_this_output_list_this > 0){
                                    tmp_node_this_output_str_1 += '+'
                                }
                                tmp_node_this_output_str_2 += tmp_node_this_output_list_this.toString()
                            }
                            tmp_node_this_output_str = String.format("{%s}[%s](%d)",tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)//'{%s}[%s](%d)' % (tmp_node_this_output_str_1, tmp_node_this_output_str_2, tmp_node_this_output)
                        }
                        else->{
                            resError = ResErrorType.NODE_OPERATION_INVALID
                            return resNoneTemplate
                        }
                    }
                }else{
                    resError = ResErrorType.NODE_OPERATION_INVALID
                    return resNoneTemplate
                }
                val resRecursiveObj = ResRecursive()
                resRecursiveObj.resLong = tmp_node_this_output
                resRecursiveObj.resLongMax = tmp_node_this_output_Max
                resRecursiveObj.resLongMin = tmp_node_this_output_Min
                resRecursiveObj.resLongMaxType = tmp_node_this_output_MaxType
                resRecursiveObj.resLongMinType = tmp_node_this_output_MinType
                resRecursiveObj.resDetail = tmp_node_this_output_str
                return resRecursiveObj
            }

        }
        fun __getCalTree(){
            val tmp_data = originData
            val tmp_res = CalNodeStack()
            val op_stack = CalNodeStack()
            val len_data = tmp_data.length
            var it_offset = 0
            var flag_old_Long = false
            var flag_left_as_Long = false
            var count_child_para = 0
            while (it_offset < len_data){
                var flag_is_op_val = false
                var tmp_offset = 1
                val tmp_data_this = tmp_data[it_offset]
                var tmp_op_peek_this = op_stack.peek()
                tmp_op_peek_this?.also { it->
                    if(it.getPriority()!= null && it is CalOperationNode){
                        if(it.vals.containsKey(tmp_data_this)){
                            flag_is_op_val = true
                        }
                    }
                }
                if(tmp_data_this.isDigit()){
                    var tmp2_data_this = CalLongNode(tmp_data_this.toString())
                    if(flag_old_Long) {
                        tmp2_data_this = (tmp_res.pop() as CalLongNode).appendLong(tmp_data_this)
                    }
                    tmp_res.push(tmp2_data_this)
                    flag_old_Long = true
                    flag_left_as_Long = true
                    tmp_offset = 1
                }else if(flag_is_op_val && op_stack.size>0){
                    tmp_op_peek_this = op_stack.peek()
                    if(tmp_op_peek_this!=null){
                        if(tmp_op_peek_this is CalOperationNode){
                            if(!flag_left_as_Long){
                                if(tmp_op_peek_this.valRightDefault != null) {
                                    tmp_res.push(CalLongNode(tmp_op_peek_this.valRightDefault.toString()))
                                    flag_left_as_Long = true
                                }else {
                                    resError = ResErrorType.INPUT_RAW_INVALID
                                    return
                                }
                            }
                            if(tmp_op_peek_this.vals.containsKey(tmp_data_this)){
                                if(it_offset < tmp_data.length - 1){
                                    if(tmp_data[it_offset+1].isDigit()){
                                        var tmp_Long_offset = 0
                                        var tmp_Long:Long? = null
                                        while(true){
                                            tmp_Long_offset++
                                            val tmp_total_offset = it_offset + tmp_Long_offset + 1
                                            val tmp_val_data_this:CharSequence
                                            if(tmp_total_offset <= tmp_data.length) {
                                                tmp_val_data_this = tmp_data.subSequence(it_offset + 1,tmp_total_offset)
                                            }else {
                                                tmp_val_data_this = ""
                                                tmp_Long_offset -= 1
                                                break
                                            }
                                            try{
                                                tmp_Long = tmp_val_data_this.toString().toLong()
                                            }catch (e:Throwable){
                                                tmp_Long_offset -= 1
                                                break
                                            }
                                        }
                                        if(tmp_Long!= null){
                                            op_stack.pop()
                                            tmp_op_peek_this.vals[tmp_data_this] = tmp_Long
                                            op_stack.push(tmp_op_peek_this)
                                        }else{
                                            resError = ResErrorType.INPUT_RAW_INVALID
                                            return
                                        }
                                        tmp_offset = 1 + tmp_Long_offset
                                    }else if(tmp_data[it_offset+1]=='('){
                                        var count_child_para_2 = 1
                                        var tmp_Long_offset = 0
                                        while(count_child_para_2 > 0){
                                            tmp_Long_offset += 1
                                            val tmp_total_offset = it_offset + tmp_Long_offset + 1
                                            if(tmp_total_offset >= tmp_data.length){
                                                tmp_Long_offset -= 1
                                                break
                                            }
                                            if(tmp_data[tmp_total_offset] == '(') {
                                                count_child_para_2 += 1
                                            }else if(tmp_data[tmp_total_offset] == ')') {
                                                count_child_para_2 -= 1
                                            }
                                        }
                                        if(count_child_para_2 == 0){
                                            val tmp_rd_child_para = RD(tmp_data.subSequence(it_offset + 1 , it_offset + 1 + tmp_Long_offset + 1).toString(), customDefault)
                                            tmp_rd_child_para.roll()
                                        }else{
                                            resError = ResErrorType.INPUT_RAW_INVALID
                                        }
                                        tmp_offset = 1 + tmp_Long_offset + 1
                                    }else if(tmp_op_peek_this.valsDefault.containsKey(tmp_data_this)){
                                        op_stack.pop()
                                        tmp_op_peek_this.vals[tmp_data_this] = tmp_op_peek_this.valsDefault[tmp_data_this] as Long
                                        op_stack.push(tmp_op_peek_this)
                                    }else{
                                        resError = ResErrorType.INPUT_RAW_INVALID
                                        return
                                    }
                                }else if(tmp_op_peek_this.valsDefault.containsKey(tmp_data_this)){
                                    op_stack.pop()
                                    tmp_op_peek_this.vals[tmp_data_this] = tmp_op_peek_this.valsDefault[tmp_data_this] as Long
                                    op_stack.push(tmp_op_peek_this)
                                }else{
                                    resError = ResErrorType.INPUT_RAW_INVALID
                                    return
                                }
                            }else{
                                resError = ResErrorType.INPUT_RAW_INVALID
                                return
                            }
                        }
                    }else{
                        resError = ResErrorType.INPUT_RAW_INVALID
                        return
                    }
                }else if(inOperation(tmp_data_this)){
                    if(getPriority(tmp_data_this)!= null){
                        tmp_op_peek_this = op_stack.peek()
                        val tmp_calOperationNode_this = CalOperationNode(tmp_data_this, customDefault)
                        if(!flag_left_as_Long){
                            if(tmp_op_peek_this==null){
                                if(tmp_calOperationNode_this.valStarterLeftDefault != null){
                                    tmp_res.push(CalLongNode(tmp_calOperationNode_this.valStarterLeftDefault.toString()))
                                    flag_left_as_Long = true
                                }else if(tmp_calOperationNode_this.valLeftDefault != null){
                                    tmp_res.push(CalLongNode(tmp_calOperationNode_this.valLeftDefault.toString()))
                                    flag_left_as_Long = true
                                }else{
                                    resError = ResErrorType.INPUT_RAW_INVALID
                                    return
                                }
                            }else if(tmp_op_peek_this is CalOperationNode){
                                if(tmp_op_peek_this.nodeData == "("){
                                    if(tmp_calOperationNode_this.valStarterLeftDefault!=null){
                                        tmp_res.push(CalLongNode(tmp_calOperationNode_this.valStarterLeftDefault.toString()))
                                        flag_left_as_Long = true
                                    }else if(tmp_calOperationNode_this.valLeftDefault != null){
                                        tmp_res.push(CalLongNode(tmp_calOperationNode_this.valLeftDefault.toString()))
                                        flag_left_as_Long = true
                                    }else{
                                        resError = ResErrorType.INPUT_RAW_INVALID
                                        return
                                    }
                                }else if(tmp_op_peek_this.getPriority() == null){
                                    if(tmp_calOperationNode_this.valLeftDefault != null) {
                                        tmp_res.push(CalLongNode(tmp_calOperationNode_this.valLeftDefault.toString()))
                                        flag_left_as_Long = true
                                    }else{
                                        resError = ResErrorType.INPUT_RAW_INVALID
                                        return
                                    }
                                }else if(tmp_op_peek_this.valRightDefault != null){
                                    tmp_res.push(CalLongNode(tmp_op_peek_this.valRightDefault.toString()))
                                    flag_left_as_Long = true
                                }else if(tmp_calOperationNode_this.valLeftDefault != null){
                                    tmp_res.push(CalLongNode(tmp_calOperationNode_this.valLeftDefault.toString()))
                                    flag_left_as_Long = true
                                }else{
                                    resError = ResErrorType.INPUT_RAW_INVALID
                                    return
                                }
                            }else{
                                resError = ResErrorType.INPUT_RAW_INVALID
                                return
                            }
                        }
                        if(tmp_op_peek_this!= null){
                            val peekPriority = tmp_op_peek_this.getPriority()
                            val priority = getPriority(tmp_data_this)
                            if(peekPriority!=null && priority!=null && priority<= peekPriority){
                                tmp_res.pushList(op_stack.popTo("(", priority, true))
                            }
                        }
                        op_stack.push(CalOperationNode(tmp_data_this, customDefault))
                        flag_old_Long = false
                        flag_left_as_Long = false
                        tmp_offset = 1
                    }else if(tmp_data_this=='('){
                        op_stack.push(CalOperationNode(tmp_data_this, customDefault))
                        count_child_para += 1
                        flag_old_Long = false
                        flag_left_as_Long = false
                        tmp_offset = 1
                    }else if(tmp_data_this==')'){
                        if(!flag_left_as_Long){
                            tmp_op_peek_this = op_stack.peek()
                            if(tmp_op_peek_this!=null){
                                if(tmp_op_peek_this is CalOperationNode && tmp_op_peek_this.valRightDefault != null){
                                    tmp_res.push(CalLongNode(tmp_op_peek_this.valRightDefault.toString()))
                                    flag_left_as_Long = true
                                }else{
                                    resError = ResErrorType.INPUT_RAW_INVALID
                                    return
                                }
                            }else{
                                resError = ResErrorType.INPUT_RAW_INVALID
                                return
                            }
                        }
                        tmp_res.pushList(op_stack.popTo("("))
                        count_child_para -= 1
                        flag_old_Long = false
                        flag_left_as_Long = true
                        tmp_offset = 1
                    }else{
                        resError = ResErrorType.INPUT_NODE_OPERATION_INVALID
                        return
                    }
                }else{
                    resError = ResErrorType.INPUT_RAW_INVALID
                    return
                }
                if(count_child_para < 0) {
                    resError = ResErrorType.INPUT_CHILD_PARA_INVALID
                    return
                }
                it_offset += tmp_offset
            }
            if(!flag_left_as_Long){
                val tmp_op_peek_this = op_stack.peek() as CalOperationNode
                if(tmp_op_peek_this.valRightDefault != null){
                    tmp_res.push(CalLongNode(tmp_op_peek_this.valRightDefault.toString()))
                    flag_left_as_Long = true
                }else{
                    resError = ResErrorType.INPUT_RAW_INVALID
                    return
                }
            }
            while(op_stack.size > 0) {
                tmp_res.pushList(op_stack.popTo("("))
            }
            if(count_child_para != 0) {
                resError = ResErrorType.INPUT_CHILD_PARA_INVALID
            }
            calTree = tmp_res
            return
        }
        private fun boolByListAnd(data: Array<Boolean>):Boolean{
            for(data_this in data) {
                if(!data_this) {
                    return false
                }
            }
            return true
        }
        private fun boolByListOr(data: Array<Boolean>):Boolean{
            for(data_this in data) {
                if(data_this) {
                    return true
                }
            }
            return false
        }
        private fun range(a:Long,b:Long):LongRange{
            return LongRange(a,b-1)
        }
    }
    
}
