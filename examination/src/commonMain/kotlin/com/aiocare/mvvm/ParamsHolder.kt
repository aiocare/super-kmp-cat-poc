package com.aiocare.mvvm

interface ParamsHolder {

    fun <T> get(key: String): T?
}
