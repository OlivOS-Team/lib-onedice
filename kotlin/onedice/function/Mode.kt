package onedice.function

enum class Mode{
    PUNISH,//惩罚骰x数量
    BONUS,//奖励骰x数量
    NORMAL_DICE,//普通的d
    EXCESS,//统计有多少个超过x的
    FRONT_SUM_MAXIMUM,//前x个最大值之和
    FRONT_SUM_MINIMUM,//前x个最小值之和
    DX,//dx规则
    WW,//无限规则
}