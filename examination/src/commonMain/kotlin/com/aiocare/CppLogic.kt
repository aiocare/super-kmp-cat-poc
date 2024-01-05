package com.aiocare

interface ICppLogic {
     fun init()
    fun calculate(input: Collection<Double>): List<Double>
}

//expect class CppLogic : ICppLogic