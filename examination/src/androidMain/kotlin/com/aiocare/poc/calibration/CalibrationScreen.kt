package com.aiocare.poc.calibration

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aiocare.SimpleButton
import com.aiocare.util.ButtonVM

fun Int.parseTime(): String {
    val minutes = this / 60
    val remainingSeconds = this % 60

    val formattedMinutes = String.format("%02d", minutes)
    val formattedSeconds = String.format("%02d", remainingSeconds)

    return "$formattedMinutes:$formattedSeconds"
}


@Composable
fun CalibrationScreen(viewModel: CalibrationViewModel) {

    BackHandler {

    }

    var toastCounter by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.uiState.deviceInfo.name,
                onValueChange = { viewModel.updateName(it) })
            viewModel.uiState.devices.forEach {
                SimpleButton(
                    buttonVM = ButtonVM(
                        visible = true,
                        onClickAction = it.onDeviceClicked,
                        text = it.text
                    )
                )
            }
            Text(text = "temperature")
            Row {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "${viewModel.uiState.before.temperature ?: ""}"
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "${viewModel.uiState.after.temperature ?: ""}"
                )
            }
            Text(text = "pressure")
            Row {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "${viewModel.uiState.before.pressure ?: ""}"
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "${viewModel.uiState.after.pressure ?: ""}"
                )
            }
            Text(text = "humidity")
            Row {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "${viewModel.uiState.before.humidity ?: ""}"
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    text = "${viewModel.uiState.after.humidity ?: ""}"
                )
            }
            Text(text = "battery = ${viewModel.uiState.deviceInfo.battery}")
            Text(text = "measurement time = ${viewModel.uiState.deviceInfo.measurementTimer.parseTime()}")
            Text(text = viewModel.uiState.infoData)
        }

        val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            SimpleButton(buttonVM = viewModel.uiState.startBtn)
            SimpleButton(buttonVM = viewModel.uiState.stopBtn)
            SimpleButton(buttonVM = viewModel.uiState.envBeforeBtn)
            SimpleButton(buttonVM = viewModel.uiState.envAfterBtn)
            SimpleButton(buttonVM = viewModel.uiState.sendBtn)
            SimpleButton(buttonVM = viewModel.uiState.startAgainBtn)
        }
        if (viewModel.uiState.loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
            )
        }
    }

    if (viewModel.uiState.toastData?.first ?: 0 > toastCounter) {
        toastCounter++
        Toast.makeText(
            LocalContext.current,
            viewModel.uiState.toastData?.second ?: "",
            Toast.LENGTH_SHORT
        ).show()
    }
}