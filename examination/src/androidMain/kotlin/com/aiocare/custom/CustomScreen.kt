package com.aiocare.custom

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aiocare.KeepScreenOn
import com.aiocare.SimpleButton
import com.aiocare.poc.superCat.custom.CustomData
import com.aiocare.poc.superCat.custom.CustomViewModel
import com.aiocare.supercat.api.Dir
import com.aiocare.util.ButtonVM
import com.google.gson.Gson

@Composable
fun CustomScreen(
    viewModel: CustomViewModel,
    navController: NavController
) {
    BackHandler {}

    KeepScreenOn()

    LaunchedEffect(key1 = "", block = { viewModel.initViewModel() })

    Column {
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
        SimpleButton(buttonVM = viewModel.uiState.disconnectBtn)
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
        CircularProgressIndicator()
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
                customData.executeWithoutRecordingBtn
            ).forEach {
                SimpleButton(buttonVM = it)
            }
            Text(text = customData.selectedWaveForm)
            Text(text = customData.temperatureAndHumidity)
            Text(text = customData.info)
        }
    }
}

private fun getDataFromJson(): Dir {
    val json = "{\"name\":\"waveforms\",\"files\":[],\"dirs\":[{\"name\":\"aaa\",\"files\":[{\"name\":\"Bat2-5Pk.fvw\"}],\"dirs\":[]},{\"name\":\"C1-C13 (ISO26782)\",\"files\":[{\"name\":\"C1.fvw\"},{\"name\":\"C10.fvw\"},{\"name\":\"C11.fvw\"},{\"name\":\"C12.fvw\"},{\"name\":\"C13.fvw\"},{\"name\":\"C2.fvw\"},{\"name\":\"C3.fvw\"},{\"name\":\"C4.fvw\"},{\"name\":\"C5.fvw\"},{\"name\":\"C6.fvw\"},{\"name\":\"C7.fvw\"},{\"name\":\"C8.fvw\"},{\"name\":\"C9.fvw\"}],\"dirs\":[]},{\"name\":\"Custom\",\"files\":[{\"name\":\".DS_Store\"},{\"name\":\"Bat1-7Pk-Adjusted.fvw\"},{\"name\":\"Bat12-0Pk-Adjusted.fvw\"}],\"dirs\":[]},{\"name\":\"PEF (ISO23747)\",\"files\":[{\"name\":\".DS_Store\"}],\"dirs\":[{\"name\":\"Profile A\",\"files\":[{\"name\":\"Aat1-7Pk.fvw\"},{\"name\":\"Aat10-0Pk.fvw\"},{\"name\":\"Aat12-0Pk.fvw\"},{\"name\":\"Aat14-5Pk.fvw\"},{\"name\":\"Aat17-0Pk.fvw\"},{\"name\":\"Aat2-5Pk.fvw\"},{\"name\":\"Aat3-0Pk.fvw\"},{\"name\":\"Aat3-3Pk.fvw\"},{\"name\":\"Aat5-0Pk.fvw\"},{\"name\":\"Aat6-0Pk.fvw\"},{\"name\":\"Aat7-5Pk.fvw\"},{\"name\":\"Aat9-0Pk.fvw\"}],\"dirs\":[]},{\"name\":\"Profile B\",\"files\":[{\"name\":\".DS_Store\"},{\"name\":\"Bat1-7Pk.fvw\"},{\"name\":\"Bat10-0Pk.fvw\"},{\"name\":\"Bat12-0Pk.fvw\"},{\"name\":\"Bat14-5Pk.fvw\"},{\"name\":\"Bat17-0Pk.fvw\"},{\"name\":\"Bat2-5Pk.fvw\"},{\"name\":\"Bat3-0Pk.fvw\"},{\"name\":\"Bat3-3Pk.fvw\"},{\"name\":\"Bat5-0Pk.fvw\"},{\"name\":\"Bat6-0Pk.fvw\"},{\"name\":\"Bat7-5Pk.fvw\"},{\"name\":\"Bat9-0Pk.fvw\"}],\"dirs\":[]}]}]}"
    return Gson().fromJson(json, Dir::class.java)
}