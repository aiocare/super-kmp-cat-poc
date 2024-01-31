package com.aiocare.poc.superCat.custom

import com.aiocare.model.Units
import com.aiocare.model.WaveformData
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.calibration.EnvironmentalData
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.poc.superCat.ErrorChecker
import com.aiocare.poc.superCat.InitDialogData
import com.aiocare.poc.superCat.InputData
import com.aiocare.sdk.IAioCareDevice
import com.aiocare.sdk.IAioCareScan
import com.aiocare.sdk.connecting.getIConnect
import com.aiocare.sdk.connecting.getIConnectMobile
import com.aiocare.sdk.scan.getIScan
import com.aiocare.sdk.services.readFlow
import com.aiocare.sdk.services.readHumidity
import com.aiocare.sdk.services.readPressure
import com.aiocare.sdk.services.readTemperature
import com.aiocare.supercat.api.Dir
import com.aiocare.supercat.api.HansCommand
import com.aiocare.supercat.api.HansProxyApi
import com.aiocare.supercat.api.Response
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class CustomUiState(
    val devices: List<DeviceItem> = listOf(),
    val url: InputData? = null,
    val note: InputData? = null,
    val hansSerial: InputData? = null,
    val disconnectBtn: ButtonVM = ButtonVM(visible = false, text = "disconnect"),
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
    val sendBtn: ButtonVM,
    val executeWithoutRecordingBtn: ButtonVM,
    val temperatureAndHumidity: String = "",
    val selectedWaveForm: String = "",
    val info: String = "",
    val results: List<WaveformData> = listOf(),
    val zeroFlow: List<Int>? = null,
    val before: EnvironmentalData? = null
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

    private fun executeSequence(sequence: String?) {
        try {
            sequence?.let {
                viewModelScope.launch {
                    checkEnvironmentalData()
                    checkZeroFlow()
                    processSequence(sequence)
                }
            }
        } catch (e: Exception) {
            updateProgress("executing sequence, error=${e.message}")
        }
    }

    private suspend fun checkEnvironmentalData() {
        if (uiState.customData?.before == null) {
            val loadedEnv = loadEnv()
            updateUiState {
                copy(customData = uiState.customData?.copy(before = loadedEnv))
            }
        }
    }

    private suspend fun loadEnv(): EnvironmentalData {
        var temperature: Float? = null
        var pressure: Float? = null
        var humidity: Float? = null
        while (temperature == null || pressure == null || humidity == null) {
            try {
                withTimeout(5000) {
                    delay(400)
                    if (temperature == null) {
                        temperature = device?.readTemperature()
                        updateProgress("temperature=$temperature")
                    }
                    if (pressure == null) {
                        pressure = device?.readPressure()
                        updateProgress("pressure=$pressure")
                    }
                    if (humidity == null) {
                        humidity = device?.readHumidity()
                        updateProgress("humidity=$humidity")
                    }
                }
            } catch (e: Exception) {
                updateProgress("try again loading env...")
            }
        }
        temperature?.let { temperature ->
            pressure?.let { pressure ->
                humidity?.let { humidity ->
                    return EnvironmentalData(
                        temperature, pressure, humidity
                    )
                }
            }
        }
        throw Exception("Unexpected Exception during getting Env")
    }


    private suspend fun checkZeroFlow() {
        if (uiState.customData?.zeroFlow == null) {
            updateProgress("zeroFlow....")
            val out = mutableListOf<Int>()
            val result = coroutineScope {
                val job = launch {
                    device!!.readFlow().collect {
                        it.forEach {
                            out.add(it)
                        }
                    }
                }
                val userJob: Deferred<Unit> = async { delay(5000) }
                userJob.await()
                job.cancelAndJoin()
                ErrorChecker.checkZeroFlowAndThrow(out)
                return@coroutineScope out
            }
            updateUiState { copy(customData = uiState.customData?.copy(zeroFlow = result)) }
        }

    }

    data class SequenceResultData(
        val humidity: Response,
        val temperature: Response,
        val waveFormRawSignal: List<Int>
    )

    private suspend fun processSequence(sequence: String): SequenceResultData {
        updateProgress(sequence ?: "")
        val api = HansProxyApi(uiState.url?.value ?: "")
        api.waveform(HansCommand.waveform(sequence))
        api.command(HansCommand.reset())

        val recordedRawSignal = mutableListOf<Int>()
        var recordJob: Job?
        val temperature = api.command(HansCommand.rawCommand("SendData Temperature"))
        updateProgress("temperature = ${(temperature as Response.TEXT).response}")
        val humidity = api.command(HansCommand.rawCommand("SendData RH"))
        updateProgress("humidity = ${(humidity as Response.TEXT).response}")
        coroutineScope {
            recordJob = launch {
                device?.readFlow()?.collect {
                    it.forEach {
                        recordedRawSignal.add(it)
                    }
                }
            }
        }
        val runResponse = api.command(HansCommand.rawCommand("Run"))
        recordJob?.cancelAndJoin()
        return SequenceResultData(humidity, temperature, recordedRawSignal)
    }

    private fun setupCustomData() {
        updateUiState {
            copy(
                customData = CustomData(
                    selectBtn = ButtonVM(true, "select") {
                        selectSequence()
                    },
                    executeBtn = ButtonVM(true, "execute") {
                        executeSequence(uiState.customData?.selectedWaveForm)
                    },
                    executeWithoutRecordingBtn = ButtonVM(true, "execute without recording") {
                        viewModelScope.launch {
                            try {
                                updateProgress("start execute without recording")
                                HansProxyApi(
                                    uiState.url?.value ?: ""
                                ).waveform(
                                    HansCommand.waveform(
                                        uiState.customData?.selectedWaveForm ?: ""
                                    )
                                )
                                updateProgress("finish execute without recording")
                            } catch (e: Exception) {
                                updateProgress("error execute without recording = ${e.message}")
                            }
                        }
                    },
                    resetBtn = ButtonVM(true, "reset") {
                        viewModelScope.launch {
                            try {
                                updateProgress("reseting...")
                                HansProxyApi(uiState.url?.value ?: "").command(HansCommand.reset())
                                updateProgress("reseting finished")
                            } catch (e: Exception) {
                                updateProgress("error reseting = ${e.message}")
                            }
                        }
                    },
                    sendBtn = ButtonVM(true, "send") {

                    }
                ),
            )
        }
    }

    private fun updateProgress(action: String) {
        updateUiState {
            copy(customData = customData?.copy(info = action))
        }
    }

    private fun selectSequence() {
        viewModelScope.launch {
            val sequences = HansProxyApi(uiState.url?.value ?: "").getAvailableSequences()
            updateUiState {
                copy(
                    selectData = SelectData(sequences, { result ->
                        updateUiState {
                            copy(
                                selectData = null,
                                customData = uiState.customData?.copy(selectedWaveForm = result)
                            )
                        }
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
                    customData = null
                )
            }
        }
    }

    private fun loading(state: Boolean) {
        updateUiState { copy(loading = state) }
    }
}