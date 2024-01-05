package com.aiocare.examination

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform