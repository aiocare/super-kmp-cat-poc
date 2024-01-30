package com.aiocare.poc.superCat.custom

import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel

data class CustomUiState(val a:String="")


class CustomViewModel(config: Config
) : StatefulViewModel<CustomUiState>(CustomUiState(), config) {

}