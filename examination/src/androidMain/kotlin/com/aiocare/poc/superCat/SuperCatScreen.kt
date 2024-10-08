package com.aiocare.poc.superCat

import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.aiocare.KeepScreenOn
import com.aiocare.SimpleButton
import com.aiocare.custom.RoundedBox
import com.aiocare.examination.R
import com.aiocare.poc.calibration.parseTime
import com.aiocare.util.ButtonVM

@Composable
fun SuperCatScreen(viewModel: SuperCatViewModel, navController: NavController) {

    BackHandler {}

    KeepScreenOn()

    LaunchedEffect(key1 = "", block = { viewModel.initViewModel{
        navController.navigate(it)
    } })

    val devicesScrollState = rememberScrollState()

    val context = LocalContext.current

    val mediaPlayerSuccess = remember { MediaPlayer.create(context, R.raw.success_bell) }
    val mediaPlayerFail = remember { MediaPlayer.create(context, R.raw.error_bell) }

    if (viewModel.uiState.playMusicSuccess && !mediaPlayerSuccess.isPlaying) {
        mediaPlayerSuccess.start()
        viewModel.playingStarted()
    }
    if (viewModel.uiState.playMusicFail && !mediaPlayerFail.isPlaying) {
        mediaPlayerFail.start()
        viewModel.playingStarted()
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        if (viewModel.uiState.devices.isNotEmpty())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScroll(devicesScrollState)
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

        listOfNotNull(
            viewModel.uiState.url,
            viewModel.uiState.hansSerial,
            viewModel.uiState.note,
        )
            .forEach { data ->
                TextField(
                    label = { Text(data.description) },
                    modifier = Modifier.fillMaxWidth(),
                    value = data.value,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = if (data.numberKeyboardType) KeyboardType.Number else KeyboardType.Text),
                    onValueChange = { data.onValueChanged.invoke(it) })
            }
        if(viewModel.uiState.deviceData != null)
            RoundedBox(title = "Connected device", description = "${viewModel.uiState.deviceData?.name} battery =${viewModel.uiState.deviceData?.battery}%")
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            SimpleButton(buttonVM = viewModel.uiState.showInitAgain)
            SimpleButton(buttonVM = viewModel.uiState.navCustomBtn)
            SimpleButton(buttonVM = viewModel.uiState.navFlowBtn)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            SimpleButton(buttonVM = viewModel.uiState.examBtn)
            SimpleButton(buttonVM = viewModel.uiState.cancelBtn)
            SimpleButton(buttonVM = viewModel.uiState.disconnectBtn)
            SimpleButton(buttonVM = viewModel.uiState.envBtn)
            SimpleButton(buttonVM = viewModel.uiState.envAfterBtn)
        }
        Text(text = viewModel.uiState.currentSequence)
        Text(text = viewModel.uiState.measurementTimer.parseTime())
        Text(text = viewModel.uiState.info)
        Text(text = viewModel.uiState.progress)
        Text(text = "env: temp:${viewModel.uiState.before.temperature}, press=${viewModel.uiState.before.pressure}, hum=${viewModel.uiState.before.humidity}, batt=${viewModel.uiState.battery ?: ""}")
        Text(text = "after = ${viewModel.uiState.after}")
        if (viewModel.uiState.loading) {
            CircularProgressIndicator()
        }
        RepeatDialog(viewModel.uiState.repeatDialog)
        TryAgainDialog(viewModel.uiState.repeatSendingDialog)
        InitDialog(viewModel.uiState.initDataDialog)
        ExamDialog(viewModel.uiState.examDialogData)
        ZeroFlowDialog(viewModel.uiState.zeroFlowDialog)
    }
}

@Composable
fun ZeroFlowDialog(zeroFlowDialog: ZeroFlowDialogData?) {
    zeroFlowDialog?.let {
        Dialog(onDismissRequest = { zeroFlowDialog.close() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    Text(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .wrapContentSize(Alignment.Center)
                        , text = it.message?:"")
                    SimpleButton(modifier = Modifier.fillMaxWidth(), buttonVM = ButtonVM(true, "close") { it.close() })
                }
            }
        }

    }
}

@Preview
@Composable
fun ZeroFlowTest(){
    ZeroFlowDialog(zeroFlowDialog = ZeroFlowDialogData("ddd", {}))
}

@Composable
fun ExamDialog(examDialogData: ExamDialogData?) {
    examDialogData?.let {
        Dialog(onDismissRequest = { it.close() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    it.exam.forEach {
                        SimpleButton(modifier = Modifier.fillMaxWidth(), buttonVM = it)
                    }
                    SimpleButton(modifier = Modifier.fillMaxWidth(), ButtonVM(true, "close") { it.close() })
                }
            }
        }
    }
}

@Composable
fun InitDialog(initDataDialog: InitDialogData?) {
    initDataDialog?.let {
        if (it.visible) {
            Dialog(onDismissRequest = { initDataDialog.close() }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Hans serial =${it.selectedName ?: ""}")
                        Row(
                            Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            it.hansName.forEach { SimpleButton(buttonVM = it) }
                        }
                        Text(text = "Hans address=${it.selectedAddress ?: ""}")
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            it.address.forEach { SimpleButton(buttonVM = it) }
                        }
                        Text(text = "operator= ${it.selectedOperator ?: ""}")
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            it.operator.forEach { SimpleButton(buttonVM = it) }
                        }
                        SimpleButton(
                            modifier = Modifier.fillMaxWidth(),
                            buttonVM = ButtonVM(
                                true,
                                "close"
                            ) { initDataDialog.close() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TryAgainDialog(repeatSendingDialog: DialogData?) {
    repeatSendingDialog?.let {
        Dialog(onDismissRequest = { repeatSendingDialog.onDismiss() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                SimpleButton(modifier = Modifier.fillMaxWidth(),
                    buttonVM = ButtonVM(true, "Try again sending to api") { it.onAccept() })
            }
        }
    }
}

@Composable
fun RepeatDialog(repeatDialogData: RepeatDialogData?) {
    if (repeatDialogData?.repeatCounter?.isNotEmpty() == true)
        Dialog(onDismissRequest = {
            repeatDialogData.close()
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(text = "Number of repetitions")
                Column {
                    repeatDialogData.repeatCounter.forEach {
                        SimpleButton(modifier = Modifier.fillMaxWidth(), it)
                    }
                }
            }
        }
}