package com.aiocare.mvvm

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

actual class Config actual constructor(val paramsHolder: ParamsHolder) {

    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        private set

    internal var mainDispatcher: CoroutineDispatcher = Dispatchers.Main
        private set

    var coroutineScope: CoroutineScope? = null

    constructor(
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
        coroutineScope: CoroutineScope? = null,
        paramsHolder: ParamsHolder
    ) : this(paramsHolder) {
        this.ioDispatcher = ioDispatcher
        this.mainDispatcher = mainDispatcher
        this.coroutineScope = coroutineScope
    }
}

actual fun createDefaultConfig(params: Map<String, Any>): Config =
    Config(AndroidParamsHolder(savedStateHandle = SavedStateHandle()))

fun createConfig(savedStateHandle: SavedStateHandle) =
    Config(AndroidParamsHolder(savedStateHandle = savedStateHandle))
