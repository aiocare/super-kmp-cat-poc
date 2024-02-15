package com.aiocare.poc.superCat

object InitHolder {
    var hansName: String? = null
    var address: String? = null
        set(value) {
            if(value?.startsWith("http://")==true)
                field = value
            else
                field = "http://${value}"
        }
    var operator: String? = null

    fun isFilled() = hansName?.isNotEmpty()==true
            && address?.isNotEmpty()==true
            && operator?.isNotEmpty()==true
}