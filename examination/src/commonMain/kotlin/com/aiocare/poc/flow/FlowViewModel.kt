package com.aiocare.poc.flow

import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.poc.superCat.custom.CustomUiState

class FlowViewModel(config: Config) : StatefulViewModel<CustomUiState>(CustomUiState(), config){

}