package com.aiocare.poc.superCat

object InitHolder {
    var hansName: String? = null
    var address: String? = null
    var operator: String? = null

    fun isFilled() = hansName?.isNotEmpty()==true
            && address?.isNotEmpty()==true
            && operator?.isNotEmpty()==true
}