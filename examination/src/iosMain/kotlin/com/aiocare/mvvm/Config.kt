package com.aiocare.mvvm

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

actual class Config actual constructor(
    val paramsHolder: ParamsHolder
) {

    internal var scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.Default
        private set

    internal var mainDispatcher: CoroutineDispatcher = Dispatchers.Main
        private set

    constructor(
        paramsHolder: ParamsHolder,
        scope: CoroutineScope
    ) : this(paramsHolder) {
        this.scope = scope
    }

    constructor(
        paramsHolder: ParamsHolder,
        ioDispatcher: CoroutineDispatcher,
        mainDispatcher: CoroutineDispatcher,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    ) : this(paramsHolder) {
        this.scope = scope
        this.ioDispatcher = ioDispatcher
        this.mainDispatcher = mainDispatcher
    }
}

actual fun createDefaultConfig(params: Map<String, Any>): Config =
    Config(IosParamsHolder(params = params))
