package com.aiocare.poc.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aiocare.SimpleButton
import com.aiocare.cat.Graph

@Composable
fun HansInputScreen(
    viewModel: HansInputViewModel,
    navController: NavController
) {
    var inited by remember { mutableStateOf(false) }

    if (!inited) {
        viewModel.init(navigate = { navController.navigate(it) })

        inited = true
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            viewModel.uiState.inputs.forEach {
                SimpleButton(modifier = Modifier.fillMaxWidth(), buttonVM = it)
            }
            SimpleButton(
                modifier = Modifier.fillMaxWidth(),
                buttonVM = viewModel.uiState.refreshButtonVM
            )
            viewModel.uiState.details?.let { Text(text = it) }
            Graph(graphItems = viewModel.uiState.indexedInputData)

        }
        if (viewModel.uiState.refreshing)
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
            )
        SimpleButton(
            modifier = Modifier.align(Alignment.BottomCenter),
            buttonVM = viewModel.uiState.nextButton
        )
    }
}