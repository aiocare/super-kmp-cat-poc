package com.aiocare.supercat

fun <T> List<T>.times(times: Int): List<T> {
    val list = mutableListOf<T>()
    repeat(times) {
        list.addAll(this)
    }
    return list
}

