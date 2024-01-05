package com.aiocare.mvvm

expect class Config(
    paramsHolder: ParamsHolder
)

expect fun createDefaultConfig(params: Map<String, Any> = emptyMap()): Config
