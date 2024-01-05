package com.aiocare.mvvm

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import androidx.lifecycle.ViewModel as AndroidXViewModel

actual abstract class ViewModel actual constructor(val config: Config) : AndroidXViewModel() {

    actual val paramsHolder: ParamsHolder = config.paramsHolder

    final override fun onCleared() {
        onClear()
        super.onCleared()
    }

    actual fun onClear() {
        config.coroutineScope?.cancel()
    }
}

actual val ViewModel.viewModelScope: CoroutineScope
    get() = config.coroutineScope ?: this.viewModelScope

actual val ViewModel.ioDispatcher: CoroutineDispatcher
    get() = config.ioDispatcher

actual val ViewModel.mainDispatcher: CoroutineDispatcher
    get() = config.mainDispatcher
