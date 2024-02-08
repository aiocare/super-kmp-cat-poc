package com.aiocare.supercat.api

sealed class Response {
    sealed class NoInteractive(val key: String) : Response() {
        object OK : NoInteractive(":")
        object NOT_RECOGNIZED : NoInteractive("?")
    }

    class TEXT(val response: String) : Response() {

        fun parse(): String {
            val parts = response.split(",")
            return "${parts[parts.size - 2]}${parts.first()}"
        }
    }
}
