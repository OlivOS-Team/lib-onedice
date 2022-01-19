package onedice.function

import onedice.data.Format
import onedice.data.ResRecursive
import onedice.defines.ResErrorException
import onedice.defines.ResErrorType
import java.util.*

object DiceCore {
    private const val MAX_PB_NUMBER = 9
    fun calculationPunishBonus(mode: Mode, num: Int): ResRecursive {
        if(mode != Mode.PUNISH && mode != Mode.BONUS){
            throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID)
        }
        if (num < 1 || num > MAX_PB_NUMBER) {
            throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"无法计算,要求 惩罚骰个数 必须不大于" + MAX_PB_NUMBER + "且 大于0")
        }
        val str = StringBuilder()
        var random1To100 = DiceRandom.random(1, 100)
        var random10position = random1To100 / 10
        str.append("{")
        if (mode == Mode.PUNISH){
            str.append("惩罚")
        } else {
            str.append("奖励")
        }
        str.append("d100=").append(random1To100).append("|")
        run {
            val adds = StringBuilder()
            for (i in 0 until num) {
                var random1To10 = DiceRandom.random(0, 9)
                if (random1To100 % 10 == 0) //初骰的个位数是0
                    random1To10++ //从0-9变成1-10
                if (mode == Mode.BONUS && random1To10 < random10position || mode == Mode.PUNISH && random1To10 > random10position) {
                    //替换十位
                    random1To100 = random1To10 * 10 + random1To100 % 10
                    random10position = random1To10
                }
                adds.append(random1To10)
                adds.append(" ")
            }
            val addsString = adds.toString()
            str.append(addsString, 0, addsString.length - 1)
        }
        str.append("}($random1To100)")
        return ResRecursive(random1To100.toDouble(), str.toString(),resDoubleMin = 1.0,resDoubleMax=100.0)
    }
    fun calculationDice(mode: Mode, times: Int, maxValue: Long, addition:Int=1): ResRecursive {
        if(mode != Mode.EXCESS &&
            mode != Mode.FRONT_SUM_MAXIMUM &&
            mode != Mode.FRONT_SUM_MINIMUM &&
            mode != Mode.NORMAL_DICE){
            throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID)
        }
        var total = 0L
        val process = StringBuilder()
        process.append("{")
        val randNumbers = Array(times){
            DiceRandom.random(1, maxValue)
        }
        if(mode == Mode.EXCESS){
            for (i in randNumbers.indices) {
                val number = randNumbers[i]
                if(i > 0)
                    process.append(",")
                if(number >= addition){
                    total++
                    process.append("{$number}")
                }else{
                    process.append(number)
                }
            }
        }else if (mode == Mode.FRONT_SUM_MAXIMUM || mode == Mode.FRONT_SUM_MINIMUM) {
            if (addition > 0) {
                Arrays.sort(randNumbers) { o1: Long, o2: Long ->
                    o2.compareTo(o1)
                }
            } else { //q>0
                Arrays.sort(randNumbers)
            }
            if (addition > randNumbers.size) {
                throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID, "参数在 $mode 模式下不应超过 ${randNumbers.size}")
            }
            for (i in 0 until addition) {
                total += randNumbers[i]
            }
            process.append(
                if(mode== Mode.FRONT_SUM_MAXIMUM){
                    "MAX"
                }else{
                    "MIN"
                }
            )
            process.append(" ")
            for (i in randNumbers.indices) {
                if (i==addition) {
                    process.append("|")
                }else if (i > 0) {
                    process.append(",")
                }
                process.append(randNumbers[i])
            }
            process.append(" ")
        } else {
            //大于等于20个骰才排序
            if(times>=20)
                Arrays.sort(randNumbers)
            for (i in randNumbers.indices) {
                total += randNumbers[i]
                if (i > 0)
                    process.append("+")
                process.append(randNumbers[i])
            }
        }
        process.append("}($total)")
        return when(mode){
            Mode.NORMAL_DICE->{
                ResRecursive(total.toDouble(),process.toString(),resDoubleMin=times.toDouble(),resDoubleMax = times*maxValue.toDouble())
            }
            Mode.EXCESS->{
                ResRecursive(total.toDouble(),process.toString(),
                    resDoubleMin = 0.0,
                    resDoubleMax = times.toDouble())
            }
            Mode.FRONT_SUM_MAXIMUM,Mode.FRONT_SUM_MINIMUM->{
                ResRecursive(total.toDouble(),process.toString(),
                    resDoubleMin = addition.toDouble(),
                    resDoubleMax = addition*maxValue.toDouble())
            }
            else -> {
                ResRecursive(total.toDouble(),process.toString())
            }
        }
    }//DiceCalculation.RollResult(total, process.toString())
    fun calculationFate(times: Int,range:Int): ResRecursive {
        val sb = StringBuilder()
        var sum = 0
        if (times > 50) {
            throw ResErrorException(ResErrorType.NODE_LEFT_VAL_INVALID,"f的左值最大值为50")
        }
        sb.append("{")
        for (i in 0 until times) {
            when (DiceRandom.random(-range, range)) {
                0 -> sb.append('0')
                1 -> {
                    sum++
                    sb.append('+')
                }
                -1 -> {
                    sum--
                    sb.append('-')
                }
            }
            if (times - 1 != i) {
                sb.append(' ')
            }
        }
        sb.append("}($sum)")
        return ResRecursive(sum.toDouble(), sb.toString(),resDoubleMax = times.toDouble(), resDoubleMin = -times.toDouble())
    }
    fun calculationWWandDX(mode: Mode, num: Int, addLine: Int, maxValue:Int, successLine: Int, wrappedLine:Boolean): ResRecursive {
        if(mode != Mode.DX && mode != Mode.WW){
            throw ResErrorException(ResErrorType.NODE_SUB_VAL_INVALID)
        }
        if (addLine < 2 || addLine > 99) {
            throw ResErrorException(ResErrorType.NODE_RIGHT_VAL_INVALID,"计算失败 加骰线要大于2 且 小于99")
        }
        if(maxValue<successLine && mode == Mode.WW){
            throw ResErrorException(ResErrorType.NODE_NONSENSE)
        }
        var numOfBiggerThanAddLine = num
        var numOfBiggerOrEqualThan8 = 0
        var addTimes = 0
        var lastBig: Int
        val process = StringBuilder()
        process.append("{")
        do {
            var newNumOfBiggerThanAddLine = 0
            var newNumOfBiggerOrEqualThan8 = 0
            lastBig = 0
            val childProcess = StringBuilder("{")
            for (i in 0 until numOfBiggerThanAddLine) {
                val rand1To10 = DiceRandom.random(1, maxValue)
                if (rand1To10 > lastBig) {
                    lastBig = rand1To10
                }
                val biggerThanAddLine = rand1To10 >= addLine
                val biggerOrEqualThanSuccessLine = rand1To10 >= successLine
                if (mode==Mode.DX) {
                    if (biggerThanAddLine) {
                        childProcess.append("<").append(rand1To10).append(">")
                        newNumOfBiggerThanAddLine++
                    } else {
                        childProcess.append(rand1To10)
                    }
                } else {
                    if (biggerThanAddLine && biggerOrEqualThanSuccessLine) {
                        childProcess.append("<[").append(rand1To10).append("]>")
                        newNumOfBiggerThanAddLine++
                        newNumOfBiggerOrEqualThan8++
                    } else if (biggerThanAddLine) {
                        childProcess.append("<").append(rand1To10).append(">")
                        newNumOfBiggerThanAddLine++
                    } else if (biggerOrEqualThanSuccessLine) {
                        childProcess.append("[").append(rand1To10).append("]")
                        newNumOfBiggerOrEqualThan8++
                    } else {
                        childProcess.append(rand1To10)
                    }
                }
                if (i != numOfBiggerThanAddLine - 1) {
                    childProcess.append(",")
                }
            }
            childProcess.append("}")
            numOfBiggerThanAddLine = newNumOfBiggerThanAddLine
            numOfBiggerOrEqualThan8 += newNumOfBiggerOrEqualThan8
            process.append(childProcess.toString())
            if(numOfBiggerThanAddLine > 0) {
                process.append(",")
            }
            if(wrappedLine){
                process.append('\n')
            }
            addTimes++
        } while (numOfBiggerThanAddLine > 0)
        addTimes-- //只计算加骰次数
        process.append("}")
        val resDoubleMax = when{
            maxValue>=addLine->Double.POSITIVE_INFINITY
            else->1.0
        }
        return if (mode == Mode.DX) {
            val final = (addTimes * 10 + lastBig).toDouble()
            process.append(Format.build("({replace}*10+{replace})({replace})",addTimes,lastBig,final))
            ResRecursive(final, process.toString().trim(),resDoubleMin = 1.0,resDoubleMax=resDoubleMax)
        }else{
            val final = (numOfBiggerOrEqualThan8).toDouble()
            process.append(Format.build("({replace})",final))
            ResRecursive(final, process.toString().trim(),resDoubleMin = 0.0,resDoubleMax=resDoubleMax)
        }
    }
}