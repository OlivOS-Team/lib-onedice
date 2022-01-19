package onedice

import onedice.data.CalOperationDefault
import onedice.data.Format
import onedice.data.ResRecursive
import onedice.defines.ResErrorException
import onedice.defines.ResErrorType
import onedice.function.DiceCore
import onedice.function.Mode
import onedice.nodes.CalLongNode
import onedice.nodes.CalNode
import onedice.nodes.CalNodeStack
import onedice.nodes.CalOperationNode
import java.util.*
import kotlin.math.floor
import kotlin.math.pow

/**
 * Roll
 *
 * @property customDefault 参数默认值
 * @property decimalEnable 是否使用启用浮点数运算
 * @constructor
 *
 * @param initData 骰点表达式
 */
class Roll(initData:String,
           private val customDefault: HashMap<Char, CalOperationDefault>?=null,
           private val decimalEnable:Boolean = true){
    companion object {
        val dictOperationPriority = HashMap<Char, Short?>().apply {
            this['('] = null
            this[')'] = null
            this['+'] = 1
            this['-'] = 1
            this['*'] = 2
            this['x'] = 2
            this['X'] = 2
            this['/'] = 2
            this['^'] = 3
            this['d'] = 4
            this['a'] = 4
            this['c'] = 4
            this['f'] = 4
            this['b'] = 4
            this['p'] = 4
        }
    }
    private var calTree = CalNodeStack()
    val originData:String
    init{
        val tmp = initData.lowercase()
        originData = if(tmp.startsWith("+")){
            tmp.substring(1)
        }else{
            tmp
        }
    }
    //val dictOperationPriority = Companion.dictOperationPriority
    private fun getPriority(nodeData: Char): Short? {
        return dictOperationPriority[nodeData]
    }
    private fun inOperation(nodeData: Char): Boolean {
        return dictOperationPriority.containsKey(nodeData)
    }
    fun roll(): ResRecursive {
        try{
            getCalTree()
        }catch (e:Throwable){
            throw ResErrorException(ResErrorType.UNKNOWN_GENERATE_FATAL,cause = e)
        }
        try{
            return recursiveCalculate()
        }catch (e:Throwable){
            if(e !is ResErrorException){
                throw ResErrorException(ResErrorType.UNKNOWN_COMPLETE_FATAL, cause = e)
            }
            throw e
        }
    }
    private fun recursiveCalculate(rootPriority:Short = 0, forkSideRight:Boolean = true): ResRecursive {//, rootData:Char?=null
        if(calTree.size <= 0) {
            throw ResErrorException(ResErrorType.NODE_STACK_EMPTY)
        }else{
            val tmpNodeThis: CalNode
            var tmpNodeThisOutput: Double
            var tmpNodeThisOutputMax = 0.0
            var tmpNodeThisOutputMin = 0.0
            var tmpNodeThisOutputStr:String
            var tmpNodeThisOutputNumberStr:String
            val calTreePeek = calTree.peek() as CalNode
            when {
                calTreePeek is CalLongNode -> {
                    calTree.pop()
                    tmpNodeThis = calTreePeek
                    tmpNodeThisOutput = tmpNodeThis.getDouble()
                    tmpNodeThisOutputMax = tmpNodeThis.getDouble()
                    tmpNodeThisOutputMin = tmpNodeThis.getDouble()
                    tmpNodeThisOutputStr = tmpNodeThisOutput.toString()
                    tmpNodeThisOutputNumberStr = tmpNodeThisOutput.toString()
                }
                calTreePeek.isOperation() -> {
                    calTree.pop()
                    tmpNodeThis = calTreePeek as CalOperationNode
                    val tmpPriorityThis = tmpNodeThis.getPriority()?:0
                    val tmpMainValRightObj = recursiveCalculate(tmpPriorityThis, true)//, tmpNodeThis.nodeData[0]
                    val tmpMainValLeftObj = recursiveCalculate(tmpPriorityThis, false)//, tmpNodeThis.nodeData[0]
                    val tmpMainValRight = tmpMainValRightObj.clone()
                    val tmpMainValLeft = tmpMainValLeftObj.clone()
                    when(tmpNodeThis.nodeData){
                        "+"->{
                            if(tmpMainValLeft.resDouble == Double.NEGATIVE_INFINITY && tmpMainValRight.resDouble == Double.POSITIVE_INFINITY ||tmpMainValLeft.resDouble == Double.POSITIVE_INFINITY && tmpMainValRight.resDouble == Double.NEGATIVE_INFINITY){
                                throw ResErrorException(ResErrorType.NODE_EXTREME_VAL_INVALID)
                            }
                            tmpNodeThisOutput = tmpMainValLeft.resDouble + tmpMainValRight.resDouble
                            tmpNodeThisOutputMax = tmpMainValLeft.resDoubleMax+tmpMainValRight.resDoubleMax
                            tmpNodeThisOutputMin = tmpMainValLeft.resDoubleMin+tmpMainValRight.resDoubleMin
                            tmpNodeThisOutputStr = Format.build(
                                "{replace}+{replace}",
                                tmpMainValLeft.resDetail,
                                tmpMainValRight.resDetail
                            )
                            tmpNodeThisOutputNumberStr = Format.build(
                                "{replace}+{replace}",
                                tmpMainValLeft.resDouble,
                                tmpMainValRight.resDouble
                            )
                            if(tmpPriorityThis < rootPriority) {
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }
                        }
                        "-"->{
                            if(tmpMainValLeft.resDouble == Double.NEGATIVE_INFINITY && tmpMainValRight.resDouble == Double.NEGATIVE_INFINITY ||tmpMainValLeft.resDouble == Double.POSITIVE_INFINITY && tmpMainValRight.resDouble == Double.POSITIVE_INFINITY){
                                throw ResErrorException(ResErrorType.NODE_EXTREME_VAL_INVALID)
                            }
                            tmpNodeThisOutput = tmpMainValLeft.resDouble - tmpMainValRight.resDouble
                            ResRecursive.getExtremum(tmpMainValLeft, tmpMainValRight, { a, b ->
                                a - b
                            }, { max, min ->
                                tmpNodeThisOutputMin = min
                                tmpNodeThisOutputMax = max
                            })
                            tmpNodeThisOutputStr = Format.build(
                                "{replace}-{replace}",
                                tmpMainValLeft.resDetail,
                                tmpMainValRight.resDetail
                            )
                            tmpNodeThisOutputNumberStr = Format.build(
                                "{replace}-{replace}",
                                tmpMainValLeft.resDouble,
                                tmpMainValRight.resDouble
                            )
                            if(tmpPriorityThis < rootPriority) {
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }
                        }
                        "*","x"->{
                            tmpNodeThisOutput = tmpMainValLeft.resDouble * tmpMainValRight.resDouble
                            tmpNodeThisOutputStr = Format.build(
                                "{replace}*{replace}",
                                tmpMainValLeft.resDetail,
                                tmpMainValRight.resDetail
                            )
                            tmpNodeThisOutputNumberStr = Format.build(
                                "{replace}*{replace}",
                                tmpMainValLeft.resDouble,
                                tmpMainValRight.resDouble
                            )
                            ResRecursive.getExtremum(tmpMainValLeft, tmpMainValRight, { a, b ->
                                a * b
                            }, { max, min ->
                                tmpNodeThisOutputMin = min
                                tmpNodeThisOutputMax = max
                            })
                            if(tmpPriorityThis < rootPriority){
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }else if(forkSideRight && tmpPriorityThis == rootPriority){
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }
                        }
                        "/"->{
                            tmpNodeThisOutput = tmpMainValLeft.resDouble / tmpMainValRight.resDouble
                            if(!decimalEnable){
                                tmpNodeThisOutput = floor(tmpNodeThisOutput)
                            }
                            tmpNodeThisOutputStr = Format.build(
                                "{replace}/{replace}",
                                tmpMainValLeft.resDetail,
                                tmpMainValRight.resDetail
                            )
                            tmpNodeThisOutputNumberStr = Format.build(
                                "{replace}/{replace}",
                                tmpMainValLeft.resDouble,
                                tmpMainValRight.resDouble
                            )
                            ResRecursive.getExtremum(tmpMainValLeft, tmpMainValRight, { a, b ->
                                a / b
                            }, { max, min ->
                                tmpNodeThisOutputMin = min
                                tmpNodeThisOutputMax = max
                            })
                            if(tmpPriorityThis < rootPriority){
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }else if(forkSideRight && tmpPriorityThis == rootPriority){
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }
                        }
                        "^"->{
                            if(tmpMainValLeft.resDouble == 0.0 && tmpMainValRight.resDouble == 0.0){
                                throw ResErrorException(ResErrorType.NODE_EXTREME_VAL_INVALID)
                            }
                            if((tmpMainValLeft.resDouble) >= 10000){
                                throw ResErrorException(ResErrorType.NODE_LEFT_VAL_INVALID)
                            }
                            if(tmpMainValRight.resDouble >= 10000){
                                throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID)
                            }
                            tmpNodeThisOutput = tmpMainValLeft.resDouble.pow(tmpMainValRight.resDouble)
                            ResRecursive.getExtremum(tmpMainValLeft, tmpMainValRight, { a, b ->
                                a.pow(b)
                            }, { max, min ->
                                tmpNodeThisOutputMin = min
                                tmpNodeThisOutputMax = max
                            })
                            tmpNodeThisOutputStr = Format.build(
                                "{replace}^{replace}",
                                tmpMainValLeft.resDetail,
                                tmpMainValRight.resDetail
                            )
                            tmpNodeThisOutputNumberStr = Format.build(
                                "{replace}^{replace}",
                                tmpMainValLeft.resDouble,
                                tmpMainValRight.resDouble
                            )
                            if(tmpPriorityThis < rootPriority){
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }else if(forkSideRight && tmpPriorityThis == rootPriority){
                                tmpNodeThisOutputStr = "($tmpNodeThisOutputStr)"
                            }
                        }
                        "d"->{
                            //XdYkZ 骰X个1dY统计前Z个最大值之和
                            //XdYqZ 骰X个1dY统计前Z个最小值之和
                            //XdYaZ 骰X个1dY统计超过Z的数量
                            //XdY 骰X个1dY 求和
                            val inputY = tmpMainValRight.resDouble
                            val inputX = tmpMainValLeft.resDouble
                            val inputKZ = tmpNodeThis.vals['k']
                            val inputQZ = tmpNodeThis.vals['q']
                            val inputAZ = tmpNodeThis.vals['a']
                            var kqa = 0
                            if(inputKZ!=null){
                                kqa++
                            }
                            if(inputQZ!=null){
                                kqa++
                            }
                            if(inputAZ!=null){
                                kqa++
                            }
                            if(kqa>1){
                                throw ResErrorException(ResErrorType.NODE_NONSENSE,"d的子参 k q a 只能存在一个")
                            }
                            if((tmpMainValRight.resDouble ) <= 0 || (tmpMainValRight.resDouble ) >= 10000){
                                throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"d表达式右值非法,要求1~9999")
                            }
                            if((tmpMainValLeft.resDouble) <= 0 || (tmpMainValLeft.resDouble) >= 10000){
                                throw ResErrorException(ResErrorType.NODE_LEFT_VAL_INVALID,"d表达式左值非法,要求1~9999")
                            }
                            when{
                                kqa==0->{
                                    DiceCore.calculationDice(Mode.NORMAL_DICE,inputX.toInt(),inputY.toLong())
                                }
                                inputKZ!=null->{
                                    DiceCore.calculationDice(Mode.FRONT_SUM_MAXIMUM,inputX.toInt(),inputY.toLong(),inputKZ.toInt())
                                }
                                inputQZ!=null->{
                                    DiceCore.calculationDice(Mode.FRONT_SUM_MINIMUM,inputX.toInt(),inputY.toLong(),inputQZ.toInt())
                                }
                                inputAZ!=null->{
                                    DiceCore.calculationDice(Mode.EXCESS,inputX.toInt(),inputY.toLong(),inputAZ.toInt())
                                }
                                else -> {
                                    throw ResErrorException(ResErrorType.NODE_NONSENSE)
                                }
                            }.apply {
                                tmpNodeThisOutputStr = resDetail
                                tmpNodeThisOutput = resDouble
                                tmpNodeThisOutputMax = resDoubleMax
                                tmpNodeThisOutputMin = resDoubleMin
                                tmpNodeThisOutputNumberStr = resDouble.toInt().toString()
                            }
                        }
                        "a"->{
                            val inputZ = tmpNodeThis.vals['m'] as Double
                            val inputY = tmpNodeThis.vals['k'] as Double
                            val inputX = tmpMainValRight.resDouble
                            val inputW = tmpMainValLeft.resDouble
                            //WaXkYmZ（骰W个1dZ，达到X则加骰，统计1dZ>=Y的个数） 的最大值与最小值的计算

                            if(inputX <= 1.0 || inputX >= 1000.0){
                                throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"a表达式右值非法,要求2~999")
                            }else if(inputW <= 0.0 || inputW >= 1000.0){
                                throw ResErrorException(ResErrorType.NODE_LEFT_VAL_INVALID,"a表达式左值非法,要求1~999")
                            }else if(inputZ <= 0.0 || inputZ >= 1000.0){
                                throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID,"a的子表达式m值非法,要求1~999")
                            }else if(inputY <= 0.0 || inputY >= 1000.0){
                                throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID,"a的子表达式k值非法,要求1~999")
                            }
                            DiceCore.calculationWWandDX(
                                Mode.WW,inputW.toInt(),
                                inputX.toInt(),
                                inputZ.toInt(),
                                inputY.toInt(),
                                false).apply {
                                tmpNodeThisOutputStr = resDetail
                                tmpNodeThisOutput = resDouble
                                tmpNodeThisOutputMax = resDoubleMax
                                tmpNodeThisOutputMin = resDoubleMin
                                tmpNodeThisOutputNumberStr = resDouble.toInt().toString()
                            }

                        }
                        "c"->{
                            val inputZ = tmpNodeThis.vals['m'] as Double
                            val inputY = tmpMainValRight.resDouble
                            val inputX = tmpMainValLeft.resDouble
                            //XcYmZ (骰X个1dZ，达到Y则加骰)，最终出目是 加骰轮数*10+最后一轮的1dZ最大出目
                            if(inputY <= 1L || inputY >= 1000L){
                                throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"c表达式右值非法,要求2~999")
                            }
                            if(inputX <= 0L || inputX >= 1000L){
                                throw ResErrorException(ResErrorType.NODE_LEFT_VAL_INVALID,"c表达式左值非法,要求1~999")
                            }
                            if(inputZ <= 0L || inputZ >= 1000L){
                                throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID,"c的子表达式m值非法,要求1~999")
                                //return resNoneTemplate
                            }
                            DiceCore.calculationWWandDX(
                                Mode.DX,inputX.toInt(),
                                inputY.toInt(),inputZ.toInt(),inputY.toInt(),false).apply {
                                tmpNodeThisOutputStr = resDetail
                                tmpNodeThisOutput = resDouble
                                tmpNodeThisOutputMax = resDoubleMax
                                tmpNodeThisOutputMin = resDoubleMin
                                tmpNodeThisOutputNumberStr = resDouble.toInt().toString()
                            }
                        }
                        "b"->{
                            DiceCore.calculationPunishBonus(Mode.BONUS,tmpMainValRight.resDouble.toInt()).apply {
                                tmpNodeThisOutputStr = resDetail
                                tmpNodeThisOutput = resDouble
                                tmpNodeThisOutputMax = resDoubleMax
                                tmpNodeThisOutputMin = resDoubleMin
                                tmpNodeThisOutputNumberStr = resDouble.toInt().toString()
                            }
                        }
                        "p"->{
                            DiceCore.calculationPunishBonus(Mode.PUNISH,tmpMainValRight.resDouble.toInt()).apply {
                                tmpNodeThisOutputStr = resDetail
                                tmpNodeThisOutput = resDouble
                                tmpNodeThisOutputMax = resDoubleMax
                                tmpNodeThisOutputMin = resDoubleMin
                                tmpNodeThisOutputNumberStr = resDouble.toInt().toString()
                            }
                        }
                        "f"->{
                            //XfY 骰 X个从{-(Y-1)/2 ~ (Y-1)/2}选择的随机数并求和
                            val inputX = tmpMainValLeft.resDouble
                            val inputY = tmpMainValRight.resDouble
                            if(inputY <= 1 || inputY >= 500){
                                throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"f的右参数必须小于500")
                            }
                            if(inputX <= 0 || inputX >= 1000){
                                throw ResErrorException(ResErrorType.NODE_LEFT_VAL_INVALID,"f的左参数必须小于1000")
                            }
                            if(inputY.toInt()%2==0){
                                throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"f的右参数必须是奇数")
                            }
                            DiceCore.calculationFate(inputX.toInt(),(inputY.toInt()-1)/2).apply {
                                tmpNodeThisOutputStr = resDetail
                                tmpNodeThisOutput = resDouble
                                tmpNodeThisOutputMax = resDoubleMax
                                tmpNodeThisOutputMin = resDoubleMin
                                tmpNodeThisOutputNumberStr = resDouble.toInt().toString()
                            }
                        }
                        else->{
                            throw ResErrorException(ResErrorType.NODE_OPERATION_INVALID)
                            //return resNoneTemplate
                        }
                    }
                }
                else -> {
                    throw ResErrorException(ResErrorType.NODE_OPERATION_INVALID)
                    //return resNoneTemplate
                }
            }
            return ResRecursive(
                resDouble = tmpNodeThisOutput,
                resDoubleMax = tmpNodeThisOutputMax,
                resDoubleMin = tmpNodeThisOutputMin,
                resDetail = tmpNodeThisOutputStr,
                resNumberDetail = tmpNodeThisOutputNumberStr
            )
        }
    }
    private fun getCalTree(){
        val tmpData = originData
        val tmpRes = CalNodeStack()
        val opStack = CalNodeStack()
        val lenData = tmpData.length
        var itOffset = 0
        var flagOldLong = false
        var flagLeftAsLong = false
        var countChildPara = 0
        while (itOffset < lenData){
            var flagIsOpVal = false
            var tmpOffset = 1
            val tmpDataThis = tmpData[itOffset]
            var tmpOpPeekThis = opStack.peek()
            tmpOpPeekThis?.also { it->
                if(it.getPriority()!= null && it is CalOperationNode){
                    if(it.vals.containsKey(tmpDataThis)){
                        flagIsOpVal = true
                    }
                }
            }
            if(tmpDataThis.isDigit()){
                var tmp2DataThis = CalLongNode(tmpDataThis.toString())
                if(flagOldLong) {
                    tmp2DataThis = (tmpRes.pop() as CalLongNode).appendLong(tmpDataThis)
                }
                tmpRes.push(tmp2DataThis)
                flagOldLong = true
                flagLeftAsLong = true
                tmpOffset = 1
            }else if(flagIsOpVal && opStack.size>0){
                tmpOpPeekThis = opStack.peek()
                if(tmpOpPeekThis!=null){
                    if(tmpOpPeekThis is CalOperationNode){
                        if(!flagLeftAsLong){
                            if(tmpOpPeekThis.valRightDefault != null) {
                                tmpRes.push(CalLongNode(tmpOpPeekThis.valRightDefault.toString()))
                                flagLeftAsLong = true
                            }else {
                                throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                //return
                            }
                        }
                        if(tmpOpPeekThis.vals.containsKey(tmpDataThis)){
                            if(itOffset < tmpData.length - 1){
                                if(tmpData[itOffset+1].isDigit()){
                                    var tmpLongOffset = 0
                                    var tmpLong:Long? = null
                                    while(true){
                                        tmpLongOffset++
                                        val tmpTotalOffset = itOffset + tmpLongOffset + 1
                                        val tmpValDataThis:CharSequence
                                        if(tmpTotalOffset <= tmpData.length) {
                                            tmpValDataThis = tmpData.subSequence(itOffset + 1,tmpTotalOffset)
                                        }else {
                                            //tmpValDataThis = ""
                                            tmpLongOffset -= 1
                                            break
                                        }
                                        try{
                                            tmpLong = tmpValDataThis.toString().toLong()
                                        }catch (e:Throwable){
                                            tmpLongOffset -= 1
                                            break
                                        }
                                    }
                                    if(tmpLong!= null){
                                        opStack.pop()
                                        tmpOpPeekThis.vals[tmpDataThis] = tmpLong.toDouble()
                                        opStack.push(tmpOpPeekThis)
                                    }else{
                                        throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                        //return
                                    }
                                    tmpOffset = 1 + tmpLongOffset
                                }else if(tmpData[itOffset+1]=='('){
                                    var countChildPara2 = 1
                                    var tmpLongOffset = 0
                                    while(countChildPara2 > 0){
                                        tmpLongOffset += 1
                                        val tmpTotalOffset = itOffset + tmpLongOffset + 1
                                        if(tmpTotalOffset >= tmpData.length){
                                            tmpLongOffset -= 1
                                            break
                                        }
                                        if(tmpData[tmpTotalOffset] == '(') {
                                            countChildPara2 += 1
                                        }else if(tmpData[tmpTotalOffset] == ')') {
                                            countChildPara2 -= 1
                                        }
                                    }
                                    if(countChildPara2 == 0){
                                        val tmpRdChildPara = Roll(tmpData.subSequence(itOffset + 1 , itOffset + 1 + tmpLongOffset + 1).toString(), customDefault)
                                        tmpRdChildPara.roll()
                                    }else{
                                        throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                    }
                                    tmpOffset = 1 + tmpLongOffset + 1
                                }else if(tmpOpPeekThis.valsDefault.containsKey(tmpDataThis)){
                                    opStack.pop()
                                    tmpOpPeekThis.vals[tmpDataThis] = tmpOpPeekThis.valsDefault[tmpDataThis] as Double
                                    opStack.push(tmpOpPeekThis)
                                }else{
                                    throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                    //return
                                }
                            }else if(tmpOpPeekThis.valsDefault.containsKey(tmpDataThis)){
                                opStack.pop()
                                tmpOpPeekThis.vals[tmpDataThis] = tmpOpPeekThis.valsDefault[tmpDataThis] as Double
                                opStack.push(tmpOpPeekThis)
                            }else{
                                throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                //return
                            }
                        }else{
                            throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                            //return
                        }
                    }
                }else{
                    throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                    //return
                }
            }else if(inOperation(tmpDataThis)){
                when {
                    getPriority(tmpDataThis)!= null -> {
                        tmpOpPeekThis = opStack.peek()
                        val tmpCalOperationNodeThis = CalOperationNode(tmpDataThis, customDefault)
                        if(!flagLeftAsLong){
                            if(tmpOpPeekThis==null){
                                when {
                                    tmpCalOperationNodeThis.valStarterLeftDefault != null -> {
                                        tmpRes.push(CalLongNode(tmpCalOperationNodeThis.valStarterLeftDefault.toString()))
                                    }
                                    tmpCalOperationNodeThis.valLeftDefault != null -> {
                                        tmpRes.push(CalLongNode(tmpCalOperationNodeThis.valLeftDefault.toString()))
                                    }
                                    else -> {
                                        throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                        //return
                                    }
                                }
                                //flagLeftAsLong = true
                            }else if(tmpOpPeekThis is CalOperationNode){
                                if(tmpOpPeekThis.nodeData == "("){
                                    when {
                                        tmpCalOperationNodeThis.valStarterLeftDefault!=null -> {
                                            tmpRes.push(CalLongNode(tmpCalOperationNodeThis.valStarterLeftDefault.toString()))
                                        }
                                        tmpCalOperationNodeThis.valLeftDefault != null -> {
                                            tmpRes.push(CalLongNode(tmpCalOperationNodeThis.valLeftDefault.toString()))
                                        }
                                        else -> {
                                            throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                        }
                                    }
                                }else if(tmpOpPeekThis.getPriority() == null){
                                    if(tmpCalOperationNodeThis.valLeftDefault != null) {
                                        tmpRes.push(CalLongNode(tmpCalOperationNodeThis.valLeftDefault.toString()))
                                    }else{
                                        throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                    }
                                }else if(tmpOpPeekThis.valRightDefault != null){
                                    tmpRes.push(CalLongNode(tmpOpPeekThis.valRightDefault.toString()))
                                }else if(tmpCalOperationNodeThis.valLeftDefault != null){
                                    tmpRes.push(CalLongNode(tmpCalOperationNodeThis.valLeftDefault.toString()))
                                }else{
                                    throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                                }
                                //flagLeftAsLong = true
                            }else{
                                throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                            }
                        }
                        if(tmpOpPeekThis!= null){
                            val peekPriority = tmpOpPeekThis.getPriority()
                            val priority = getPriority(tmpDataThis)
                            if(peekPriority!=null && priority!=null && priority<= peekPriority){
                                tmpRes.pushList(opStack.popTo("(", priority, true))
                            }
                        }
                        opStack.push(CalOperationNode(tmpDataThis, customDefault))
                        flagOldLong = false
                        flagLeftAsLong = false
                        tmpOffset = 1
                    }
                    tmpDataThis=='(' -> {
                        opStack.push(CalOperationNode(tmpDataThis, customDefault))
                        countChildPara += 1
                        flagOldLong = false
                        flagLeftAsLong = false
                        tmpOffset = 1
                    }
                    tmpDataThis==')' -> {
                        if(!flagLeftAsLong){
                            tmpOpPeekThis = opStack.peek()
                            if(tmpOpPeekThis!=null && tmpOpPeekThis is CalOperationNode && tmpOpPeekThis.valRightDefault != null){
                                tmpRes.push(CalLongNode(tmpOpPeekThis.valRightDefault.toString()))
                            }else{
                                throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
                            }
                        }
                        tmpRes.pushList(opStack.popTo("("))
                        countChildPara -= 1
                        flagOldLong = false
                        flagLeftAsLong = true
                        tmpOffset = 1
                    }
                    else -> {
                        throw ResErrorException(ResErrorType.INPUT_NODE_OPERATION_INVALID)
                    }
                }
            }else{
                throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
            }
            if(countChildPara < 0) {
                throw ResErrorException(ResErrorType.INPUT_CHILD_PARA_INVALID)
            }
            itOffset += tmpOffset
        }
        if(!flagLeftAsLong){
            val tmpOpPeekThis = opStack.peek() as CalOperationNode
            if(tmpOpPeekThis.valRightDefault != null){
                tmpRes.push(CalLongNode(tmpOpPeekThis.valRightDefault.toString()))
            }else{
                throw ResErrorException(ResErrorType.INPUT_RAW_INVALID)
            }
            //flagLeftAsLong = true
        }
        while(opStack.size > 0) {
            tmpRes.pushList(opStack.popTo("("))
        }
        if(countChildPara != 0) {
            throw ResErrorException(ResErrorType.INPUT_CHILD_PARA_INVALID)
        }
        calTree = tmpRes
    }
}