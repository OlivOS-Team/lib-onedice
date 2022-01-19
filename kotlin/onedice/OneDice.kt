package onedice

import onedice.defines.ResErrorException
import java.util.*
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
        @JvmStatic
        fun main(s:Array<String>){/*
            val strParaList = arrayOf(
                "10d10-7a5k7*10+6",
                "10d10",
                "1d(7a5k7)a(1d4)",
                "(1d100)^(7a5k7)",
                "(1d100)^(1d20)",
                "7a5k7",
                "b+5"
            )
            for(strPara in strParaList){
                val rdPara = RD(strPara)
                try {
                    rdPara.roll()
                    println("取值范围：${rdPara.resDoubleMin} ~ ${rdPara.resDoubleMax}")
                    println("运算细节：${rdPara.originData} = ${rdPara.resDetail} = ${rdPara.resDouble}")
                }catch (e:ResErrorException){
                    e.printStackTrace()
                    println(e.type)
                }
                println("================")
            }*/
            println("""
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
请输入表达式，如“1d4”，按回车继续。
            """.trimIndent())
            val scanner = Scanner(System.`in`)
            while (true) {
                val text: String = scanner.nextLine().trim()
                if ("" == text) {
                    continue
                }
                val rdPara = Roll(text)
                try {
                    val result = rdPara.roll()
                    println("取值范围：${result.resDoubleMin} ~ ${result.resDoubleMax}")
                    println("运算细节：${rdPara.originData} = ${result.resDetail} = ${result.resDouble}")
                }catch (e:ResErrorException){
                    e.printStackTrace()
                    println("ERROR:"+e.type)
                }
                println("================")
            }
        }
    }
}