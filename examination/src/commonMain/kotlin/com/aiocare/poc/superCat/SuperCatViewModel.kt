package com.aiocare.poc.superCat

import com.aiocare.Screens
import com.aiocare.bluetooth.BaseAioCareDevice
import com.aiocare.bluetooth.device.AioCareDevice
import com.aiocare.bluetooth.deviceFactory.DeviceFactory
import com.aiocare.bluetooth.deviceProvider.DeviceProvider
import com.aiocare.bluetooth.di.inject
//import com.aiocare.model.SteadyFlowData
//import com.aiocare.model.WaveformData
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.VersionHolder
import com.aiocare.poc.calibration.EnvironmentalData
import com.aiocare.poc.calibration.GraphObjects
import com.aiocare.poc.ktor.Api
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.poc.superCat.custom.DeviceData
//import com.aiocare.sdk.IAioCareDevice
//import com.aiocare.sdk.IAioCareScan
//import com.aiocare.sdk.connecting.getIConnect
//import com.aiocare.sdk.connecting.getIConnectMobile
//import com.aiocare.sdk.scan.getIScan
//import com.aiocare.sdk.services.readBattery
//import com.aiocare.sdk.services.readFlow
//import com.aiocare.sdk.services.readHumidity
//import com.aiocare.sdk.services.readPressure
//import com.aiocare.sdk.services.readTemperature
import com.aiocare.supercat.CalibrationSequenceType
import com.aiocare.supercat.Logic
import com.aiocare.supercat.PhoneInfo
import com.aiocare.supercat.SteadyFlowData
import com.aiocare.supercat.WaveformData
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
    val cancelBtn: ButtonVM = ButtonVM(visible = false, text = "Cancel"),
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
    val deviceData: DeviceData? = null
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
    private var device: AioCareDevice? = null
    private var beforeTime: Long? = null
    private var operator: String = "not_selected"

    private val deviceProvider = inject<DeviceProvider>()
    private val deviceFactory = inject<DeviceFactory>()

    private fun startSearching() {
        scanJob = viewModelScope.launch {
            deviceProvider.devices.collect { scan ->
                updateUiState {
                    it.copy(
                        devices = devices.plus(DeviceItem(
                            text = scan.name,
                            aioCareScan = scan,
                            onDeviceClicked = { scanClicked(scan) }
                        )).distinctBy { item -> item.aioCareScan.name }
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
                    InitHolder.address ?: "http://192.168.1.217:8080",
                    "url",
                    onValueChanged = { v ->
                        InitHolder.address = v
                        updateUiState {
                            copy(url = url?.copy(value = v))
                        }
                    }),
                note = InputData(
                    "",
                    "note",
                    onValueChanged = { v ->
                        updateUiState { copy(note = note?.copy(value = v)) }
                    }),
                hansSerial = InputData(
                    InitHolder.hansName ?: "112-093",
                    "HansSerial",
                    onValueChanged = { v ->
                        InitHolder.hansName = v
                        updateUiState {
                            copy(hansSerial = hansSerial?.copy(value = v))
                        }
                    },
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
            InitHolder.operator?.let { operator = it }
            copy(
                url = InitHolder.address?.let { it1 -> uiState.url?.copy(value = it1) },
                hansSerial = InitHolder.hansName?.let { it1 -> uiState.hansSerial?.copy(value = it1) },
                initDataDialog = InitDialogData(
                    visible = !InitHolder.isFilled(),
                    selectedAddress = InitHolder.address,
                    selectedOperator = InitHolder.operator,
                    selectedName = InitHolder.hansName,
                    hansName = listOf("112-121", "112-093", "112-123", "112-131").map { current ->
                        ButtonVM(true, current, true, {
                            InitHolder.hansName = current
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
                        ButtonVM(true, current, true, {
                            InitHolder.address = current
                            updateUiState {
                                copy(
                                    initDataDialog = uiState.initDataDialog?.copy(selectedAddress = current),
                                    url = uiState.url?.copy(value = "http://${current}")
                                )
                            }
                        })
                    },
                    operator = listOf("Piotr", "Milena", "Darek", "Szymon").map {
                        ButtonVM(true, it, true, {
                            operator = it
                            InitHolder.operator = it
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
            device?.observeConnectionStateCommand?.values?.collect {
                if (!it) {
                    actionJob?.cancelAndJoin()
                    timerJob?.cancelAndJoin()
                    device = null
                    updateProgress("disconnect")
                    updateSequenceName("")
                    startSearching()
                    updateUiState {
                        copy(
                            deviceData = null,
                            disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                            after = "", before = EnvironmentalData(), measurementTimer = 0
                        )
                    }
                }
            }
        }
    }

    private fun scanClicked(scan: BaseAioCareDevice) {
            viewModelScope.launch {
                try{
                updateLoader(true)
                device = deviceFactory.create(scan)
                scanJob?.cancelAndJoin()
                startObservingState()
                val battery = device?.readBatteryCommand?.execute()?:0
                updateUiState {
                    copy(
                        deviceData = DeviceData(scan.name, battery),
                        devices = listOf(),
                        cancelBtn = cancelBtn.copy(
                            visible = true,
                            onClickAction = {
                                timerJob?.cancel()
                                actionJob?.cancel()
                            }
                        ),
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
            }catch (e: Exception){
                updateLoader(false)
                    updateUiState {
                        copy(
                            zeroFlowDialog = ZeroFlowDialogData("${e::class.simpleName} ${e.message}") {
                                updateUiState { copy(zeroFlowDialog = null) }
                            },
                        )
                    }
                }
        }
    }

    private fun disconnect() {
        updateProgress("disconnect")
        updateSequenceName("")
        viewModelScope.launch {
            timerJob?.cancelAndJoin()
            actionJob?.cancelAndJoin()
//            getIConnect().disconnect()
            startSearching()
            updateUiState {
                copy(
                    deviceData = null,
                    disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                    after = "", before = EnvironmentalData(), measurementTimer = 0
                )
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
            copy(currentSequence = "${name} ${deviceData?.name}")
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

    private data class ZeroFlowData(
        val zeroFlow: List<Int>,
        val zeroFlowDataTime: Long,
        val zeroFlowDataCounter: Int)

    private suspend fun zeroFlow(): ZeroFlowData {
        updateProgress("zeroFlow....")
        val out = mutableListOf<Int>()
        val startTime = Clock.System.now().toEpochMilliseconds()
        val data =  coroutineScope {
            val job = launch {
                device!!.readFlowCommand.values.collect {
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
        val finishTime = Clock.System.now().toEpochMilliseconds()
        return ZeroFlowData(data, finishTime-startTime, data.size)
    }

    private suspend fun processSending(
        zeroFlow: ZeroFlowData,
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
                        temperature = device?.readSingleTemperatureCommand?.execute()?.toFloat()
                    updateProgress("collecting after..... pressure")
                    if (pressure == null)
                        pressure = device?.readPressureCommand?.execute()?.toFloat()
                    updateProgress("collecting after..... humidity")
                    if (humidity == null)
                        humidity = device?.readHumidityCommand?.execute()?.toFloat()
                }
            } catch (e: Exception) {
                updateProgress("try again...")
            }
        }

        updateUiState {
            it.copy(after = "${temperature} ${pressure} ${humidity}")
        }

        updateProgress("try send")

        val totalCount: Int = steadyFlowRawData?.map { it.exhaleRawSignalControlCount + it.inhaleRawSignalControlCount }?.sum()?:0
        val totalRawSignal : Int = steadyFlowRawData?.map { it.exhaleRawSignal.size + it.inhaleRawSignal.size }?.sum()?:0
        val percentageLoss = if(totalCount == 0) 0.0 else
            100 - (((totalRawSignal).toDouble() / totalCount) * 100)
        delay(1000)
        val request = Api.PostData(
            environment = Api.Environment(
                recordingDevice = PhoneInfo().getPhoneModel(),
                hansIpAddress = uiState.url?.value ?: "hans",
                hansSerialNumber = uiState.hansSerial?.value ?: "hans_serial_number",
                hansCalibrationId = (uiState.hansSerial?.value ?: "000-000").takeLast(3),
                appVersion = VersionHolder.version,
                spirometerDeviceSerial = uiState.deviceData?.name?:"",
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
            zeroFlowData = zeroFlow.zeroFlow,
            zeroFlowDataTime = zeroFlow.zeroFlowDataTime,
            zeroFlowDataCount = zeroFlow.zeroFlowDataCounter,
            steadyFlowRawData = steadyFlowRawData,
            waveformRawData = waveformRawData,
            type = name,
            rawDataType = rawDataType.name,
            notes = uiState.note?.value ?: "",
            totalRawSignalControlCount = totalCount,
            totalRawSignalCount = totalRawSignal,
            overallSampleLoss =  totalCount - totalRawSignal,
            overallPercentageLoss = percentageLoss
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
        updateBattery()
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
                        temperature = device?.readSingleTemperatureCommand?.execute()?.toFloat()
                    updateProgress("collecting after..... pressure")
                    if (pressure == null)
                        pressure = device?.readPressureCommand?.execute()?.toFloat()
                    updateProgress("collecting after..... humidity")
                    if (humidity == null)
                        humidity = device?.readHumidityCommand?.execute()?.toFloat()
                    batt = device?.readBatteryCommand?.execute()
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
        val battery = device?.readBatteryCommand?.execute()
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

    private suspend fun updateBattery(){
        device?.readBatteryCommand?.execute()?.let {  bat ->
            updateUiState {
                copy(deviceData = deviceData?.copy(battery = bat))
            }
        }
    }
}
