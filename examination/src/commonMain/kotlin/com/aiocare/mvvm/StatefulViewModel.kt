package com.aiocare.mvvm

import com.jstarczewski.kstate.StateHolder
import com.jstarczewski.kstate.stateful

abstract class StatefulViewModel<State : Any>(
    initialUiState: State,
    config: Config
) : ViewModel(config), StateHolder by StateHolder() {

    private var _uiState by stateful(initialUiState)

    val uiState: State
        get() = _uiState

    protected fun updateUiState(update: State.(State) -> State) {
        _uiState = uiState.update(uiState)
    }
}
