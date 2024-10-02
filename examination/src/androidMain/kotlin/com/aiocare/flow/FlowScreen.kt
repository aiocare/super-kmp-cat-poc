package com.aiocare.flow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

    LaunchedEffect(Unit) { viewModel.init{
        navController.navigate(it)
    } }

    Box(modifier = Modifier.fillMaxSize()) {
        ShowDialog(viewModel.uiState.dialogData) { viewModel.hideDialog() }
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())) {
            Row {
                SimpleButton(
                    modifier = Modifier,
                    buttonVM = viewModel.uiState.settingsBtn
                )
                SimpleButton(
                    modifier = Modifier,
                    buttonVM = viewModel.uiState.navigateToSuperCatBtn
                )
            }
            if(viewModel.uiState.deviceData != null)
                RoundedBox(title = "Connected device", description = "${viewModel.uiState.deviceData?.name} battery =${viewModel.uiState.deviceData?.battery}%")
            SimpleButton(
                modifier = Modifier.fillMaxWidth(),
                buttonVM = viewModel.uiState.disconnectBtn)
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
            TextField(
                label = { Text(viewModel.uiState.note.description) },
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.uiState.note.value,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                onValueChange = { viewModel.uiState.note.onValueChanged.invoke(it) })
            Text(text = "operator= ${viewModel.uiState.selectedOperator?:""}")
            Text(text = "timer= ${viewModel.uiState.measurementTimer.parseTime()}")
            if(viewModel.uiState.description.isNotEmpty())
                Text(text = viewModel.uiState.description)
            Text(text = viewModel.uiState.zeroFlowValue )
            RoundedBox(title = "rawSignal", description = viewModel.uiState.realtimeData)
        }
        if (viewModel.uiState.refreshing)
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp),
            )
    }
}

@Composable
fun ShowDialog(dialogData: Pair<Boolean, List<ButtonVM>>?, dismissDialog: () ->Unit) {

    dialogData?.let {
        if (it.first) {
            Dialog(onDismissRequest = {
                dismissDialog()
            }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        it.second.forEach {
                            SimpleButton(
                                modifier = Modifier.fillMaxWidth(),
                                buttonVM = it
                            )
                        }
                    }
                }
            }
        }
    }
}
