package onedice.function

import zhao.dice.diceFunction.DiceRandomManager
import kotlin.math.abs

object DiceRandom {
    fun random(nMin:Number,nMax:Number):Long{
        return nMin.toLong() + abs(DiceRandomManager.randomInstance.nextLong()) %(nMax.toLong()-nMin.toLong()+1)
    }
    fun random(nMin:Int,nMax:Int):Int{
        return nMin + abs(DiceRandomManager.randomInstance.nextInt()) %(nMax-nMin+1)
    }
}