package com.aiocare.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect

@Composable
fun SampleScreen(viewModel: SampleViewModel) {
    SideEffect {
        viewModel.onStart()
    }
    Column {
        Text(viewModel.uiState.message)
        TextButton({ viewModel.uiState.clickAction.invoke()}, content = {
                Text(viewModel.uiState.clicked.toString())
        })
    }
}