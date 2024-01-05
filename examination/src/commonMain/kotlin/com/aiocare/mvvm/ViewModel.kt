package com.aiocare.mvvm

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

expect abstract class ViewModel constructor(config: Config) {

    val paramsHolder: ParamsHolder

    fun onClear()
}

expect val ViewModel.viewModelScope: CoroutineScope

expect val ViewModel.ioDispatcher: CoroutineDispatcher

expect val ViewModel.mainDispatcher: CoroutineDispatcher
