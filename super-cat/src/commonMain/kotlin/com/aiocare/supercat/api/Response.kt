package com.aiocare.supercat.api

sealed class Response {
    sealed class NoInteractive(val key: String) : Response() {
        object OK : NoInteractive(":")
        object NOT_RECOGNIZED : NoInteractive("?")
    }

    class TEXT(val response: String) : Response()
}
