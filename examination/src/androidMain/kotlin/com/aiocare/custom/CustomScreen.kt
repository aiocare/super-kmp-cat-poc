package com.aiocare.custom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.aiocare.KeepScreenOn
import com.aiocare.SimpleButton
import com.aiocare.poc.superCat.InitDialog
import com.aiocare.poc.superCat.TryAgainDialog
import com.aiocare.poc.superCat.custom.CustomData
import com.aiocare.poc.superCat.custom.CustomViewModel
import com.aiocare.poc.superCat.custom.ErrorData
import com.aiocare.util.ButtonVM

@Composable
fun CustomScreen(
    viewModel: CustomViewModel,
    navController: NavController
) {
    BackHandler {}

    KeepScreenOn()

    LaunchedEffect(key1 = "", block = { viewModel.initViewModel() })

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (viewModel.uiState.devices.isNotEmpty())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                viewModel.uiState.devices.forEach {
                    SimpleButtonWithoutMargin(
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
        Row {
            SimpleButtonWithoutMargin(buttonVM = viewModel.uiState.disconnectBtn)
            SimpleButtonWithoutMargin(buttonVM = viewModel.uiState.initDialogBtn)
        }
        CustomDataView(viewModel.uiState.customData)
    }
    viewModel.uiState.selectData?.let { selectedData ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())){
            DirView(dir = selectedData.dir, path = "", onClicked = {selectedData.onSelected(it)})
        }
    }
    if (viewModel.uiState.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    InitDialog(viewModel.uiState.initDataDialog)
    TryAgainDialog(viewModel.uiState.repeatSendingDialog)
    ErrorDataDialog(errorData = viewModel.uiState.errorData)
}

@Composable
fun ErrorDataDialog(errorData: ErrorData?) {
    errorData?.let {
        Dialog(onDismissRequest = { errorData.onClose() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column {
                    Text(modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = errorData.title)
                    Text(text = errorData.description)
                    SimpleButton(modifier = Modifier.fillMaxWidth(),
                        buttonVM = ButtonVM(true, "close") { it.onClose() })
                }
            }
        }
    }
}

@Composable
fun SimpleButtonWithoutMargin(buttonVM: ButtonVM){
    if (buttonVM.visible)
        Button(
            onClick = { buttonVM.onClickAction.invoke() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp)
        ) {
            Text(text = buttonVM.text)
        }
}

@Composable
fun CustomDataView(customData: CustomData?) {
    customData?.let {
        Column {
            sequenceOf(
                customData.selectBtn,
                customData.resetBtn,
                customData.executeBtn,
                customData.executeWithoutRecordingBtn,
                if(customData.results.isNotEmpty())
                customData.sendBtn else null,
            ).filterNotNull().chunked(2).forEach {
                Row {
                    it.forEach {
                        SimpleButtonWithoutMargin(buttonVM = it)
                    }
                }
            }
            RoundedBox(title = "Selected waveform", description = customData.selectedWaveForm)
            RoundedBox(title = "Current info", description = customData.info)
            RoundedBox(title = "current env", description = customData.currentEnvData)
            if(customData.results.isNotEmpty())
                RoundedBox(title = "recorded data", description = customData.results
                .joinToString("\n") { it.name })
        }
    }
}

@Composable
fun RoundedBox(title: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}