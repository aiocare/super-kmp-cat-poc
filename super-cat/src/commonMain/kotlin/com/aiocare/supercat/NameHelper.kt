package com.aiocare.supercat

object NameHelper {

    fun parse(input: String): String = when (input.replace("/", "@").contains("@")) {
        true -> input.replace("/", "@").split("@").last()
        false -> input
    }
}
