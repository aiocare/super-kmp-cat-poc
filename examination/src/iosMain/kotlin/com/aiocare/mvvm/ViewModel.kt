package com.aiocare.mvvm

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

actual abstract class ViewModel actual constructor(val config: Config) {

    actual val paramsHolder: ParamsHolder = config.paramsHolder

    actual fun onClear() {
        config.scope.cancel()
    }
}

actual val ViewModel.viewModelScope: CoroutineScope
    get() = config.scope

actual val ViewModel.ioDispatcher: CoroutineDispatcher
    get() = config.ioDispatcher

actual val ViewModel.mainDispatcher: CoroutineDispatcher
    get() = config.mainDispatcher
