package onedice.defines

class ResErrorException(val type:ResErrorType,msg:String="",cause:Throwable?=null): IllegalStateException(msg,cause){
    override fun toString(): String {
        return "ResErrorException{type=$type,msg=${super.message},cause=${cause}}"
    }
}