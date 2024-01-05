package com.aiocare.sample

import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.util.ClickAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SampleUiState(
    val clicked: Boolean = false,
    val message: String = "not loaded",
    val clickAction: ClickAction = {}
)
class SampleViewModel constructor(
    config: Config
): StatefulViewModel<SampleUiState>(SampleUiState(), config){

    fun onStart() = viewModelScope.launch {
        launch {
            delay(1000)
            updateUiState {
                it.copy(
                    message = "loaded"
                )
            }
        }
        updateUiState {
            it.copy(
                clickAction = {
                    updateUiState {
                        it.copy(
                            clicked = true
                        )
                    }
                }
            )
        }
    }
}