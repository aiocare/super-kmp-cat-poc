package com.aiocare.poc.input

import com.aiocare.Screens
import com.aiocare.cortex.cat.Entry
import com.aiocare.cortex.cat.IndexedInputData
import com.aiocare.cortex.cat.hans.Command
import com.aiocare.cortex.cat.hans.FlowType
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.Holder
import com.aiocare.poc.ktor.Api
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.launch

data class HansInputUiState(
    val inputs: List<ButtonVM> = listOf(),
    val refreshButtonVM: ButtonVM = ButtonVM(text = "refresh", visible = true),
    val details: String? = null,
    val refreshing: Boolean = false,
    val nextButton: ButtonVM = ButtonVM(visible = false, text = "next"),
    val indexedInputData: IndexedInputData = IndexedInputData(listOf())
)


class HansInputViewModel(
    config: Config
) : StatefulViewModel<HansInputUiState>(HansInputUiState(), config) {

    private val api = Api()

    private lateinit var navigateCallback: (String) -> Unit


    fun init(navigate: (String) -> Unit) {
        navigateCallback = navigate
        refresh()
        updateUiState {
            it.copy(
                refreshButtonVM = refreshButtonVM.copy(onClickAction = { refresh() })
            )
        }
    }

    private fun refresh() {
        updateUiState {
            it.copy(
                refreshing = true
            )
        }
        viewModelScope.launch {
            val response = api.getList()
            updateUiState {
                it.copy(
                    inputs = response.map {
                        ButtonVM(visible = true, text = it.name, onClickAction = {
                            downloadAndSelect(it)
                        })
                    },
                    refreshing = false
                )
            }
        }
    }

    private fun downloadAndSelect(item: Api.Item) {
        viewModelScope.launch {
            val data = api.getHansSequence(item)
            Holder.selectedSequence = data
            val exhale = data.filter { it is Command.Flow && it.type == FlowType.Exhale }
            val inhale = data.filter { it is Command.Flow && it.type == FlowType.Inhale }

            updateUiState {
                it.copy(
                    details = "selected:\ninhales = ${inhale.size}\nexhales = ${exhale.size}",
                    nextButton = nextButton.copy(visible = true, onClickAction = {
                        navigateCallback.invoke(Screens.Calibration.route)
                    }),
                    indexedInputData = data.toIndexedInputData()
                )
            }
        }
    }
}

fun List<Command>.toIndexedInputData(): IndexedInputData {
    return IndexedInputData(mapIndexed { index, command ->
        if (command is Command.Flow) {
            when (command.type) {
                FlowType.Exhale -> Entry(index, (command.flow * 100).toInt())
                FlowType.Inhale -> Entry(index, (command.flow * -100).toInt())
            }
        } else Entry(index, 0)
    })
}