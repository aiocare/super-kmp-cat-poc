package com.aiocare.poc.superCat

object ErrorChecker {

    fun checkZeroFlowAndThrow(zeroFlowList: List<Int>){
        when(val avg = zeroFlowList.average()){
            in 0.0..50.0 ->  {}
            else -> {
                throw SequenceException.ZeroFlowException("avg of zeroFlow= $avg")
            }
        }
    }
}


sealed class SequenceException(override val message: String?) : Exception() {
    class ZeroFlowException(override val message: String?) : SequenceException(message)
}
