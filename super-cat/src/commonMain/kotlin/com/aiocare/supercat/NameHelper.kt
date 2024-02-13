package com.aiocare.supercat

object NameHelper {

    fun parse(input: String): String = when (input.contains("@")) {
        true -> input.split("@").last()
        false -> input
    }
}
