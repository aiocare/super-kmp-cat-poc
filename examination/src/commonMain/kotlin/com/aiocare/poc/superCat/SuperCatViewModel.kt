package com.aiocare.poc.superCat

import com.aiocare.Screens
import com.aiocare.model.SteadyFlowData
import com.aiocare.model.WaveformData
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.VersionHolder
import com.aiocare.poc.calibration.EnvironmentalData
import com.aiocare.poc.calibration.GraphObjects
import com.aiocare.poc.ktor.Api
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.sdk.IAioCareDevice
import com.aiocare.sdk.IAioCareScan
import com.aiocare.sdk.connecting.getIConnect
import com.aiocare.sdk.connecting.getIConnectMobile
import com.aiocare.sdk.scan.getIScan
import com.aiocare.sdk.services.readBattery
import com.aiocare.sdk.services.readFlow
import com.aiocare.sdk.services.readHumidity
import com.aiocare.sdk.services.readPressure
import com.aiocare.sdk.services.readTemperature
import com.aiocare.supercat.CalibrationSequenceType
import com.aiocare.supercat.Logic
import com.aiocare.supercat.PhoneInfo
import com.aiocare.supercat.pefA
import com.aiocare.supercat.pefB
import com.aiocare.supercat.pefBAdj
import com.aiocare.supercat.standardPef
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock

data class SuperCatUiState(
    val devices: List<DeviceItem> = listOf(),
    val loading: Boolean = false,
    val url: InputData? = null,
    val note: InputData? = null,
    val hansSerial: InputData? = null,
    val disconnectBtn: ButtonVM = ButtonVM(visible = false, text = "disconnect"),
    val examBtn: ButtonVM = ButtonVM(visible = false, text = "Sequences"),
    val envBtn: ButtonVM = ButtonVM(visible = false, text = "read device env"),
    val envAfterBtn: ButtonVM = ButtonVM(visible = false, text = "batt"),
    val info: String = "",
    val progress: String = "",
    val measurementTimer: Int = 0,
    val graphObjects: GraphObjects = GraphObjects(),
    val before: EnvironmentalData = EnvironmentalData(),
    val battery: Int? = null,
    val examDialogData: ExamDialogData? = null,
    val after: String = "",
    val currentSequence: String = "",
    val playMusicSuccess: Boolean = false,
    val playMusicFail: Boolean = false,
    val repeatDialog: RepeatDialogData? = null,
    val repeatSendingDialog: DialogData? = null,
    val initDataDialog: InitDialogData? = null,
    val zeroFlowDialog: ZeroFlowDialogData? = null,
    val showInitAgain: ButtonVM = ButtonVM(true, "Settings") {},
    val navCustomBtn: ButtonVM = ButtonVM(false, "Nav to custom") {},
    val deviceName: String = "",
)

data class ZeroFlowDialogData(val message: String?, val close: () -> Unit)

data class RepeatDialogData(
    val repeatCounter: List<ButtonVM> = listOf(),
    val close: () -> Unit
)

data class ExamDialogData(
    val exam: List<ButtonVM> = listOf(),
    val close: () -> Unit
)

data class InitDialogData(
    val visible: Boolean = true,
    val selectedName: String? = null,
    val hansName: List<ButtonVM>,
    val selectedAddress: String? = null,
    val address: List<ButtonVM>,
    val selectedOperator: String? = null,
    val operator: List<ButtonVM>,
    val close: () -> Unit,
)

data class DialogData(val onAccept: () -> Unit, val onDismiss: () -> Unit)

data class InputData(
    val value: String,
    val description: String,
    val onValueChanged: (String) -> Unit,
    val numberKeyboardType: Boolean = false,
)

class SuperCatViewModel(
    config: Config,
) : StatefulViewModel<SuperCatUiState>(SuperCatUiState(), config) {

    private var scanJob: Job? = null
    private var timerJob: Job? = null
    private var actionJob: Job? = null
    private var device: IAioCareDevice? = null
    private var beforeTime: Long? = null
    private var operator: String = "not_selected"


    private fun startSearching() {
        scanJob = viewModelScope.launch {
            getIScan().start()?.collect { scan ->
                updateUiState {
                    it.copy(
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

    fun initViewModel(navigate: (String) -> Unit) {
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
                }),
                navCustomBtn = uiState.navCustomBtn.copy(
                    visible = true,
                    onClickAction = {
                        disconnect()
                        navigate(Screens.Custom.route)
                    }
                )
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
                    timerJob?.cancelAndJoin()
                    device = null
                    updateProgress("disconnect")
                    updateSequenceName("")
                    startSearching()
                    updateUiState {
                        copy(
                            deviceName = "",
                            disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                            after = "", before = EnvironmentalData(), measurementTimer = 0
                        )
                    }
                }
            }
        }
    }

    private fun scanClicked(scan: IAioCareScan) {
        viewModelScope.launch {
            updateLoader(true)
            device = getIConnectMobile().connectMobile(this, scan)
            scanJob?.cancelAndJoin()
            startObservingState()
            updateUiState {
                copy(
                    deviceName = scan.getName(),
                    devices = listOf(),
                    examBtn = examBtn.copy(
                        visible = true,
                        onClickAction = {
                            updateUiState {
                                copy(examDialogData = ExamDialogData(
                                    listOf(
                                        ButtonVM(true, "calibration seq v7") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(
                                                                true,
                                                                "${it}"
                                                            ) {
                                                                v7(
                                                                    CalibrationSequenceType.OLD_0_16,
                                                                    it
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        },
                                        ButtonVM(true, "calibration seq v5") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(
                                                                true,
                                                                "${it}"
                                                            ) {
                                                                v5(it)
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        },
                                        ButtonVM(true, "calibration seq v7 17l/s") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(
                                                                true,
                                                                "${it}"
                                                            ) {
                                                                v7(
                                                                    CalibrationSequenceType.NEW_0_17,
                                                                    it
                                                                )
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        },
                                        ButtonVM(true, "ISO c1-c11") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(
                                                                true,
                                                                "${it}"
                                                            ) { c1c11(it) }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        },
                                        ButtonVM(true, "PEF(Profile A+B)") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(true, "${it}") {
                                                                pef(standardPef, it)
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        }, ButtonVM(true, "PEF(Profile A)") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(true, "${it}") {
                                                                pef(pefA, it)
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        },
                                        ButtonVM(true, "PEF(Profile B)") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(true, "${it}") {
                                                                pef(pefB, it)
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        },
                                        ButtonVM(true, "PEF(Profile V Adjusted)") {
                                            updateUiState {
                                                copy(
                                                    examDialogData = null,
                                                    repeatDialog = RepeatDialogData(
                                                        (1..5).map {
                                                            ButtonVM(true, "${it}") {
                                                                pef(pefBAdj, it)
                                                            }
                                                        }
                                                    ) {
                                                        updateUiState { copy(repeatDialog = null) }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                ) {
                                    updateUiState {
                                        copy(examDialogData = null)
                                    }
                                })
                            }
                        }
                    ),
                    envBtn = envBtn.copy(
                        visible = true,
                        onClickAction = {
                            viewModelScope.launch {
                                loadEnv()
                            }
                        }),
                    envAfterBtn = envAfterBtn.copy(
                        visible = true,
                        onClickAction = {
                            viewModelScope.launch {
                                envAfter()
                            }
                        }),
                    disconnectBtn = disconnectBtn.copy(
                        visible = true,
                        onClickAction = {
                            viewModelScope.launch {
                                disconnect()
                            }
                        }),
                )
            }
            updateLoader(false)
        }
    }

    private fun disconnect() {
        updateProgress("disconnect")
        updateSequenceName("")
        viewModelScope.launch {
            timerJob?.cancelAndJoin()
            actionJob?.cancelAndJoin()
            getIConnect().disconnect()
            startSearching()
            updateUiState {
                copy(
                    deviceName = "",
                    disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                    after = "", before = EnvironmentalData(), measurementTimer = 0)
            }
        }
    }

    private fun clearDialog() {
        updateUiState {
            copy(repeatDialog = null)
        }
    }

    private fun pef(list: List<String>, repeat: Int) {
        clearDialog()
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            loadEnv()
            handleWaveform(
                Logic(uiState.url!!.value).preparePef(list, repeat),
                RecordingType.ISO_PEF.name
            )
        }
    }

    private fun v7(type: CalibrationSequenceType, repeat: Int) {
        clearDialog()
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            loadEnv()
            handleSequence(Logic(uiState.url!!.value).v7(type, repeat))
        }
    }

    private fun v5(repeat: Int) {
        clearDialog()
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            loadEnv()
            handleSequence(Logic(uiState.url!!.value).v5(repeat))
        }
    }

    private fun c1c11(repeat: Int) {
        clearDialog()
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            loadEnv()
            handleWaveform(
                Logic(uiState.url!!.value).prepareC1C11(repeat),
                RecordingType.ISO_C1C11.name
            )
        }
    }

    private fun updateSequenceName(name: String) {
        updateUiState {
            copy(currentSequence = "${name} ${deviceName}")
        }
    }

    private suspend fun handleWaveform(sequence: List<Logic.CalibrationActions>, name: String) {
        try {
            startCalculatingSeconds()
            updateSequenceName(name)
            Logic(uiState.url!!.value).apply {
                val zf = zeroFlow()
                val out = processWaveform(sequence, device!!, 1, {
                    updateProgress(it)
                }, { Clock.System.now().toEpochMilliseconds() })
                processSending(zf, null, out, name, RawDataType.WAVEFORM)
            }
        } catch (e: Exception) {
            if (e is SequenceException.ZeroFlowException) {
                timerJob?.cancelAndJoin()
                updateUiState {
                    copy(
                        playMusicFail = true,
                        zeroFlowDialog = ZeroFlowDialogData(e.message) {
                            updateUiState { copy(zeroFlowDialog = null) }
                        },
                        measurementTimer = 0
                    )
                }
            } else
                updateProgress(e.message ?: e.toString())
        }
    }

    private fun calculateDate(): String {
        return Clock.System.now().toString()
    }

    private suspend fun zeroFlow(): List<Int> {
        updateProgress("zeroFlow....")
        val out = mutableListOf<Int>()
        return coroutineScope {
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
    }

    private suspend fun processSending(
        zeroFlow: List<Int>,
        steadyFlowRawData: List<SteadyFlowData>?,
        waveformRawData: List<WaveformData>?,
        name: String,
        rawDataType: RawDataType
    ) {
        timerJob?.cancelAndJoin()

        updateProgress("collecting after.....")


        var temperature: Float? = null
        var pressure: Float? = null
        var humidity: Float? = null

        while (temperature == null || pressure == null || humidity == null) {
            try {
                withTimeout(5000) {
                    delay(400)
                    updateProgress("collecting after..... temperature")
                    if (temperature == null)
                        temperature = device?.readTemperature()
                    updateProgress("collecting after..... pressure")
                    if (pressure == null)
                        pressure = device?.readPressure()
                    updateProgress("collecting after..... humidity")
                    if (humidity == null)
                        humidity = device?.readHumidity()
                }
            } catch (e: Exception) {
                updateProgress("try again...")
            }
        }

        updateUiState {
            it.copy(after = "${temperature} ${pressure} ${humidity}")
        }

        updateProgress("try send")

        delay(1000)
        val request = Api.PostData(
            environment = Api.Environment(
                recordingDevice = PhoneInfo().getPhoneModel(),
                hansIpAddress = uiState.url?.value ?: "hans",
                hansSerialNumber = uiState.hansSerial?.value ?: "hans_serial_number",
                hansCalibrationId = (uiState.hansSerial?.value ?: "000-000").takeLast(3),
                appVersion = VersionHolder.version,
                spirometerDeviceSerial = uiState.deviceName,
                operator = operator,
                date = calculateDate()
            ),
            environmentalParamBefore = Api.Env(
                temperature = uiState.before.temperature!!,
                pressure = uiState.before.pressure!!,
                humidity = uiState.before.humidity!!,
                timestamp = beforeTime!!
            ),
            environmentalParamAfter = Api.Env(
                temperature = temperature!!,
                pressure = pressure!!,
                humidity = humidity!!,
                timestamp = Clock.System.now().toEpochMilliseconds()
            ),
            zeroFlowData = zeroFlow,
            steadyFlowRawData = steadyFlowRawData,
            waveformRawData = waveformRawData,
            type = name,
            rawDataType = rawDataType.name,
            notes = uiState.note?.value ?: ""
        )
        trySendToApi(request)
    }

    private suspend fun trySendToApi(request: Api.PostData) {
        try {
            val response = Api().postNewRawData(request)
            updateProgress(response)
            updateUiState { copy(playMusicSuccess = true, repeatSendingDialog = null) }
        } catch (e: Exception) {
            updateUiState {
                copy(
                    playMusicFail = true,
                    repeatSendingDialog = DialogData({
                        GlobalScope.launch { trySendToApi(request) }
                    }, {
                        updateUiState { copy(repeatSendingDialog = null) }
                    })
                )
            }
        }
    }

    private suspend fun handleSequence(sequence: List<Logic.CalibrationActions>) {
        try {
            updateSequenceName("calibrationSequence")
            startCalculatingSeconds()
            Logic(uiState.url!!.value).apply {
                val zf = zeroFlow()
                val out = processSequence(sequence, device!!, 1) {
                    updateProgress(it)
                }
                processSending(
                    zf,
                    out,
                    null,
                    RecordingType.CALIBRATION_SEQUENCE.name,
                    RawDataType.STEADY_FLOW
                )
            }
        } catch (e: Exception) {
            if (e is SequenceException.ZeroFlowException) {
                timerJob?.cancelAndJoin()
                updateUiState {
                    copy(
                        playMusicFail = true,
                        zeroFlowDialog = ZeroFlowDialogData(e.message) {
                            updateUiState { copy(zeroFlowDialog = null) }
                        },
                        measurementTimer = 0
                    )
                }
            } else
                updateProgress(e.message ?: e.toString())
        }

    }

    private suspend fun loadEnv() {
        updateLoader(true)
        updateProgress("loading before....")
        var temperature: Float? = null
        var pressure: Float? = null
        var humidity: Float? = null
        var batt: Int? = null
        beforeTime = Clock.System.now().toEpochMilliseconds()

        while (temperature == null || pressure == null || humidity == null || batt == null) {
            try {
                withTimeout(5000) {
                    delay(400)
                    updateProgress("collecting after..... temperature")
                    if (temperature == null)
                        temperature = device?.readTemperature()
                    updateProgress("collecting after..... pressure")
                    if (pressure == null)
                        pressure = device?.readPressure()
                    updateProgress("collecting after..... humidity")
                    if (humidity == null)
                        humidity = device?.readHumidity()
                    batt = device?.readBattery()
                }
            } catch (e: Exception) {
                e.toString()
                updateProgress("try again...")
            }
        }

        updateUiState {
            it.copy(
                before = before.copy(
                    temperature = temperature,
                    pressure = pressure,
                    humidity = humidity
                ),
                battery = batt
            )
        }
        updateLoader(false)
    }

    private suspend fun envAfter() {
        updateLoader(true)
        val battery = device?.readBattery()
        beforeTime = Clock.System.now().toEpochMilliseconds()

        updateUiState {
            it.copy(battery = battery)
        }
        updateLoader(false)
    }


    private fun startCalculatingSeconds() {
        updateUiState {
            it.copy(measurementTimer = 0)
        }
        timerJob?.cancel()
        timerJob = GlobalScope.launch {
            while (true) {
                delay(1000)
                updateUiState {
                    it.copy(measurementTimer = measurementTimer + 1)
                }
            }
        }
    }

    private fun updateLoader(load: Boolean) {
        updateUiState {
            it.copy(
                loading = load
            )
        }
    }

    private fun updateProgress(value: String) {
        updateUiState {
            it.copy(
                progress = "$value"
            )
        }
    }

    fun playingStarted() {
        updateUiState { copy(playMusicSuccess = false, playMusicFail = false) }
    }
}
