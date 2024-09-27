package com.aiocare.flow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aiocare.KeepScreenOn
import com.aiocare.SimpleButton
import com.aiocare.custom.RoundedBox
import com.aiocare.poc.calibration.parseTime
import com.aiocare.poc.flow.FlowViewModel
import com.aiocare.util.ButtonVM

@Composable
fun FlowScreen(
    viewModel: FlowViewModel,
    navController: NavController
) {
    BackHandler {}
    KeepScreenOn()

    LaunchedEffect(Unit) { viewModel.init() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            if(viewModel.uiState.deviceData != null)
                RoundedBox(title = "Connected device", description = "${viewModel.uiState.deviceData?.name}")

            if (viewModel.uiState.devices.isNotEmpty())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    viewModel.uiState.devices.forEach {
                        SimpleButton(
                            buttonVM = ButtonVM(
                                visible = true,
                                onClickAction = it.onDeviceClicked,
                                text = it.text
                            )
                        )
                    }
                }
            SimpleButton(
                modifier = Modifier.fillMaxWidth(),
                buttonVM = viewModel.uiState.startBtn
            )
            SimpleButton(
                modifier = Modifier.fillMaxWidth(),
                buttonVM = viewModel.uiState.stopBtn
            )
            SimpleButton(
                modifier = Modifier.fillMaxWidth(),
                buttonVM = viewModel.uiState.sendBtn
            )
            Text(text = "timer= ${viewModel.uiState.measurementTimer.parseTime()}")
            Text(text = viewModel.uiState.description)
            Text(text = "rawSignal:")
            Text(text = viewModel.uiState.realtimeData)
        }
    }
}