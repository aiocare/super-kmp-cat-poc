package com.aiocare.poc.superCat.custom

import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.calibration.EnvironmentalData
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.poc.superCat.InitDialogData
import com.aiocare.poc.superCat.InputData
import com.aiocare.sdk.IAioCareDevice
import com.aiocare.sdk.IAioCareScan
import com.aiocare.sdk.connecting.getIConnect
import com.aiocare.sdk.connecting.getIConnectMobile
import com.aiocare.sdk.scan.getIScan
import com.aiocare.supercat.api.Dir
import com.aiocare.supercat.api.HansCommand
import com.aiocare.supercat.api.HansProxyApi
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

data class CustomUiState(
    val devices: List<DeviceItem> = listOf(),
    val url: InputData? = null,
    val note: InputData? = null,
    val hansSerial: InputData? = null,
    val disconnectBtn: ButtonVM = ButtonVM(visible = false, text = "disconnect"),
    val before: EnvironmentalData = EnvironmentalData(),
    val initDataDialog: InitDialogData? = null,
    val showInitAgain: ButtonVM = ButtonVM(true, "init dialog") {},
    val customData: CustomData? = null,
    val selectData: SelectData? = null,
    val loading: Boolean = false,
)

data class SelectData(val dir: Dir, val onSelected: (String) -> Unit)

data class CustomData(
    val selectBtn: ButtonVM,
    val resetBtn: ButtonVM,
    val executeBtn: ButtonVM,
    val executeWithoutRecordingBtn: ButtonVM,
    val temperatureAndHumidity: String = "",
    val selectedWaveForm: String = "",
    val info: String = ""
)

class CustomViewModel(
    config: Config
) : StatefulViewModel<CustomUiState>(CustomUiState(), config) {

    private var scanJob: Job? = null
    private var actionJob: Job? = null
    private var device: IAioCareDevice? = null
    private var deviceName = ""
    private var operator: String = "not_selected"


    private fun startSearching() {
        scanJob = viewModelScope.launch {
            getIScan().start()?.collect { scan ->
                updateUiState {
                    it.copy(
                        disconnectBtn = disconnectBtn.copy(visible = false),
                        devices = devices.plus(DeviceItem(
                            text = scan.getName(),
                            aioCareScan = scan,
                            onDeviceClicked = { scanClicked(scan) }
                        )).distinctBy { item -> item.aioCareScan.getName() }
                    )
                }
            }
        }
    }

    fun initViewModel() {
        prepareInitDialog()
        startSearching()
        updateUiState {
            copy(
                url = InputData(
                    "http://192.168.1.217:8080",
                    "url",
                    onValueChanged = { v -> updateUiState { copy(url = url?.copy(value = v)) } }),
                note = InputData(
                    "",
                    "note",
                    onValueChanged = { v -> updateUiState { copy(note = note?.copy(value = v)) } }),
                hansSerial = InputData(
                    "112-093",
                    "HansSerial",
                    onValueChanged = { v -> updateUiState { copy(hansSerial = hansSerial?.copy(value = v)) } },
                    numberKeyboardType = true
                ),
                showInitAgain = uiState.showInitAgain.copy(onClickAction = {
                    updateUiState {
                        copy(
                            initDataDialog = uiState.initDataDialog?.copy(visible = true)
                        )
                    }
                })
            )
        }
    }

    private fun prepareInitDialog() {
        updateUiState {
            copy(initDataDialog = InitDialogData(
                hansName = listOf("112-121", "112-093", "112-123").map { current ->
                    ButtonVM(true, current, {
                        updateUiState {
                            copy(
                                hansSerial = uiState.hansSerial?.copy(value = current),
                                initDataDialog = uiState.initDataDialog?.copy(selectedName = current)
                            )
                        }
                    })
                },
                address = listOf(
                    "192.168.1.203:8080",
                    "192.168.1.217:8080",
                    "192.168.1.183:8080"
                ).map { current ->
                    ButtonVM(true, current, {
                        updateUiState {
                            copy(
                                initDataDialog = uiState.initDataDialog?.copy(selectedAddress = current),
                                url = uiState.url?.copy(value = "http://${current}")
                            )
                        }
                    })
                },
                operator = listOf("Piotr", "Milena", "Darek", "Szymon").map {
                    ButtonVM(true, it, {
                        operator = it
                        updateUiState {
                            copy(
                                initDataDialog = uiState.initDataDialog?.copy(
                                    selectedOperator = operator
                                )
                            )
                        }
                    })
                },
                close = {
                    updateUiState {
                        copy(
                            initDataDialog = uiState.initDataDialog?.copy(
                                visible = false
                            )
                        )
                    }
                }
            )
            )
        }
    }

    private fun startObservingState() {
        viewModelScope.launch {
            getIConnect().getConnectedFlow().collect {
                if (!it) {
                    actionJob?.cancelAndJoin()
                    device = null
                    deviceName = ""
                    startSearching()
                    updateUiState {
                        copy(
                            disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                            customData = null,
                            before = EnvironmentalData()
                        )
                    }
                }
            }
        }
    }

    private fun scanClicked(scan: IAioCareScan) {
        viewModelScope.launch {
            loading(true)
            deviceName = scan.getName()
            device = getIConnectMobile().connectMobile(this, scan)
            scanJob?.cancelAndJoin()
            startObservingState()
            setupCustomData()
            updateUiState {
                copy(
                    devices = listOf(),
                    disconnectBtn = ButtonVM(true, text = "disconnected") {
                        disconnect()
                    })
            }
            loading(false)
        }
    }

    private fun setupCustomData() {
        updateUiState {
            copy(
                customData = CustomData(
                    selectBtn = ButtonVM(true, "select") {
                        selectSequence()
                    },
                    executeBtn = ButtonVM(true, "execute"){

                    },
                    executeWithoutRecordingBtn = ButtonVM(true, "execute without recording"){
                        viewModelScope.launch {
                            try {
                                updateProgress("start execute without recording")
                                HansProxyApi(uiState.url?.value ?: "").waveform(HansCommand.waveform(uiState.customData?.selectedWaveForm?:""))
                                updateProgress("finish execute without recording")
                            }catch (e: Exception){
                                updateProgress("error execute without recording = ${e.message}")
                            }
                        }
                    },
                    resetBtn = ButtonVM(true, "reset"){
                        viewModelScope.launch {
                            try {
                                updateProgress("reseting...")
                                HansProxyApi(uiState.url?.value ?: "").command(HansCommand.reset())
                                updateProgress("reseting finished")
                            }catch (e: Exception){
                                updateProgress("error reseting = ${e.message}")
                            }
                        }
                    }
                ),
            )
        }
    }

    private fun updateProgress(action: String){
        updateUiState {
            copy(customData = customData?.copy(info = action))
        }
    }

    private fun selectSequence() {
        viewModelScope.launch {
            val sequences = HansProxyApi(uiState.url?.value ?: "").getAvailableSequences()
            updateUiState {
                copy(
                    selectData = SelectData(sequences, {result ->
                        updateUiState { copy(selectData = null,
                            customData = uiState.customData?.copy(selectedWaveForm = result)
                            ) }
                    })
                )
            }
        }
    }


    private fun disconnect() {
        loading(true)
        deviceName = ""
        viewModelScope.launch {
            actionJob?.cancelAndJoin()
            getIConnect().disconnect()
            startSearching()
            loading(false)
            updateUiState {
                copy(
                    disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                    customData = null,
                    before = EnvironmentalData(),
                    )
            }
        }
    }

    private fun loading(state: Boolean){
        updateUiState { copy(loading = state) }
    }
}