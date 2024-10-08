package com.aiocare.poc.superCat.custom

import com.aiocare.Screens
import com.aiocare.bluetooth.BaseAioCareDevice
import com.aiocare.bluetooth.device.AioCareDevice
import com.aiocare.bluetooth.deviceFactory.DeviceFactory
import com.aiocare.bluetooth.deviceProvider.DeviceProvider
import com.aiocare.bluetooth.di.inject
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.VersionHolder
import com.aiocare.poc.calibration.EnvironmentalData
import com.aiocare.poc.ktor.Api
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.poc.superCat.DialogData
import com.aiocare.poc.superCat.ErrorChecker
import com.aiocare.poc.superCat.InitDialogData
import com.aiocare.poc.superCat.InitHolder
import com.aiocare.poc.superCat.InputData
import com.aiocare.poc.superCat.RawDataType
import com.aiocare.poc.superCat.RecordingTypeHelper
import com.aiocare.supercat.NameHelper
import com.aiocare.supercat.PhoneInfo
import com.aiocare.supercat.WaveformData
import com.aiocare.supercat.api.Dir
import com.aiocare.supercat.api.HansCommand
import com.aiocare.supercat.api.HansProxyApi
import com.aiocare.supercat.api.Response
import com.aiocare.supercat.api.TimeoutTypes
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock

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
    val repeatSendingDialog: DialogData? = null,
    val errorData: ErrorData? = null,
    val initDialogBtn: ButtonVM = ButtonVM(true, "Settings") {},
    val navSuperCatBtn: ButtonVM = ButtonVM(true, "nav to superCat") {},
    val deviceData: DeviceData? = null
)

data class DeviceData(val name: String, val battery: Int)

data class ErrorData(val title: String, val description: String, val onClose: () -> Unit)

data class SelectData(val dir: Dir, val onSelected: (String) -> Unit)

data class CustomData(
    val selectBtn: ButtonVM,
    val resetBtn: ButtonVM,
    val executeBtn: ButtonVM,
    val sendBtn: ButtonVM,
    val executeWithoutRecordingBtn: ButtonVM,
    val selectedWaveForm: String = "",
    val info: String = "",
    val results: MutableList<CustomViewModel.SequenceResultData> = mutableListOf(),
    val zeroFlow: List<Int>? = null,
    val zeroFlowDataTime: Long? = null,
    val zeroFlowDataCount: Int? = null,
    val before: EnvironmentalData? = null,
    val beforeTime: Long? = null,
    val currentEnvData: String = "",
    val history: MutableList<List<String>> = mutableListOf(),
)

class CustomViewModel(
    config: Config
) : StatefulViewModel<CustomUiState>(CustomUiState(), config) {

    private var scanJob: Job? = null
    private var actionJob: Job? = null
    private var device: AioCareDevice? = null
    private var operator: String = "not_selected"

    private val deviceProvider = inject<DeviceProvider>()
    private val deviceFactory = inject<DeviceFactory>()


    private fun startSearching() {
        scanJob = viewModelScope.launch {
            deviceProvider.devices.collect { scan ->
                updateUiState {
                    it.copy(
                        disconnectBtn = disconnectBtn.copy(visible = false),
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
                    InitHolder.address?:"http://192.168.1.221:8080",
                    "url",
                    onValueChanged = { v ->
                        InitHolder.address = v
                        updateUiState { copy(url = url?.copy(value = v)) }
                        viewModelScope.launch { setupCollectingEnv() }
                    }),
                note = InputData(
                    "",
                    "note",
                    onValueChanged = { v -> updateUiState { copy(note = note?.copy(value = v)) } }),
                hansSerial = InputData(
                    InitHolder.hansName?:"112-093",
                    "HansSerial",
                    onValueChanged = { v -> updateUiState {
                        InitHolder.hansName = v
                        copy(hansSerial = hansSerial?.copy(value = v))
                    } },
                    numberKeyboardType = true
                ),
                initDialogBtn = uiState.initDialogBtn.copy(onClickAction = {
                    updateUiState {
                        copy(initDataDialog = uiState.initDataDialog?.copy(visible = true))
                    }
                }),
                showInitAgain = uiState.showInitAgain.copy(onClickAction = {
                    updateUiState {
                        copy(
                            initDataDialog = uiState.initDataDialog?.copy(visible = true)
                        )
                    }
                }),
                navSuperCatBtn = uiState.navSuperCatBtn.copy(onClickAction = {
                    disconnect()
                    navigate.invoke(Screens.SuperCat.route)
                })
            )
        }
        viewModelScope.launch { setupCollectingEnv() }
    }

    private suspend fun safeCancelJob(){
        if(actionJob!=null && ! actionJob!!.isCancelled)
            actionJob?.cancelAndJoin()
    }

    private suspend fun setupCollectingEnv() {
        safeCancelJob()
        actionJob = viewModelScope.launch {
            while (true) {
                withContext(NonCancellable) {
                    try {
                        HansProxyApi(uiState.url?.value ?: "", TimeoutTypes.NORMAL).apply {
                            val temp = (command(HansCommand.readTemperature()))
                            val hum = (command(HansCommand.readHumidity()))

                            if (temp is Response.TEXT && hum is Response.TEXT)
                                updateUiState {
                                    copy(customData = customData?.copy(currentEnvData = "temp=${temp.parse()},\nhum=${hum.parse()}"))
                                }
                            delay(500)
                        }
                    } catch (e: Exception) {
                        updateUiState {
                            copy(customData = customData?.copy(currentEnvData = "error ${e.message}"))
                        }
                        actionJob?.cancelAndJoin()
                    }
                }
                yield()
            }
        }
    }

    private fun prepareInitDialog() {
        InitHolder.operator?.let { operator = it }
        updateUiState {
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
                            viewModelScope.launch { setupCollectingEnv() }
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
        viewModelScope.launch { setupCollectingEnv() }
    }

    private fun startObservingState() {
        viewModelScope.launch {
            device?.observeConnectionStateCommand?.values?.collect {
                if (!it) {
                    safeCancelJob()
                    device = null
                    startSearching()
                    updateUiState {
                        copy(
                            disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                            deviceData = null
                        )
                    }
                }
            }
        }
    }

    private fun scanClicked(scan: BaseAioCareDevice) {
        viewModelScope.launch {
            loading(true)
            device = deviceFactory.create(scan)
            scanJob?.cancelAndJoin()
            startObservingState()
            setupCustomData()
            val battery = device?.readBatteryCommand?.execute()?:0
            updateUiState {
                copy(
                    deviceData = DeviceData(scan.name, battery),
                    devices = listOf(),
                    disconnectBtn = ButtonVM(true, text = "disconnected") {
                        disconnect()
                    })
            }
            loading(false)
            viewModelScope.launch { setupCollectingEnv() }
        }
    }

    private fun executeSequence(sequence: String?) {
        sequence?.let {
            viewModelScope.launch {
                try {
                    loading(true)
                    safeCancelJob()
                    updateBattery()
                    checkEnvironmentalData()
                    checkZeroFlow()
                    uiState.customData?.results?.add(processSequence(sequence))
                    setupCollectingEnv()
                    loading(false)
                } catch (e: Exception) {
                    loading(false)
                    updateProgress("executing sequence, error=${e.message}")
                }
            }
        }
    }

    private suspend fun checkEnvironmentalData() {
        if (uiState.customData?.before == null) {
            val loadedEnv = loadEnv()
            updateUiState {
                copy(
                    customData = uiState.customData?.copy(
                        before = loadedEnv,
                        beforeTime = Clock.System.now().toEpochMilliseconds()
                    )
                )
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
                        temperature = device?.readSingleTemperatureCommand?.execute()?.toFloat()
                        updateProgress("temperature=$temperature")
                    }
                    if (pressure == null) {
                        pressure = device?.readPressureCommand?.execute()?.toFloat()
                        updateProgress("pressure=$pressure")
                    }
                    if (humidity == null) {
                        humidity = device?.readHumidityCommand?.execute()?.toFloat()
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
            val startTime = Clock.System.now().toEpochMilliseconds()
            val result = coroutineScope {
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
            updateUiState { copy(customData = uiState.customData?.copy(
                zeroFlow = result,
                zeroFlowDataCount = result.size,
                zeroFlowDataTime = finishTime - startTime
            )) }
        }
    }

    data class SequenceResultData(
        val name: String,
        val sendSpirometryResult: String,
        val humidity: Response,
        val temperature: Response,
        val waveFormRawSignal: List<Int>,
        val timestamp: Long,
    )

    private suspend fun processSequence(sequence: String): SequenceResultData {
        updateProgress(sequence ?: "")
        val api = HansProxyApi(uiState.url?.value ?: "", TimeoutTypes.NORMAL)
        api.waveformLoad(HansCommand.waveform(sequence))
        api.command(HansCommand.reset())
        val recordedRawSignal = mutableListOf<Int>()
        var recordJob: Job?
        val temperature = api.command(HansCommand.readTemperature())
        updateProgress("temperature = ${(temperature as Response.TEXT).parse()}")
        val humidity = api.command(HansCommand.readHumidity())
        updateProgress("humidity = ${(humidity as Response.TEXT).parse()}")
        return coroutineScope {
            recordJob = launch {
                device?.readFlowCommand?.values?.collect {
                    it.forEach {
                        recordedRawSignal.add(it)
                    }
                }
            }
            updateProgress("start waveform")
            api.command(HansCommand.run())
            recordJob?.cancelAndJoin()
            val sendSpirometryResult =
                when (val res = api.command(HansCommand.waveformData())) {
                    is Response.TEXT -> res.response
                    else -> "bad response"
                }
            updateProgress("finished waveform")
            return@coroutineScope SequenceResultData(
                sequence,
                sendSpirometryResult,
                humidity,
                temperature,
                recordedRawSignal,
                Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private fun setupCustomData() {
        if(uiState.customData==null)
        updateUiState {
            copy(
                customData = CustomData(
                    selectBtn = ButtonVM(true, "Choose waveform") {
                        selectSequence()
                    },
                    executeBtn = ButtonVM(true, "Run waveform") {
                        executeSequence(uiState.customData?.selectedWaveForm)
                    },
                    executeWithoutRecordingBtn = ButtonVM(true, "run waveform without recording") {
                        viewModelScope.launch {
                            safeCancelJob()
                            try {
                                loading(true)
                                updateBattery()
                                updateProgress("start execute without recording, run")
                                HansProxyApi(uiState.url?.value ?: "", TimeoutTypes.NORMAL).command(HansCommand.run())
                                updateProgress("finish execute without recording")
                                setupCollectingEnv()
                                loading(false)
                            } catch (e: Exception) {
                                loading(false)
                                updateProgress("error execute without recording = ${e.message}")
                            }
                        }
                    },
                    resetBtn = ButtonVM(true, "hans Reset") {
                        viewModelScope.launch {
                            try {
                                safeCancelJob()
                                updateProgress("reseting...")
                                HansProxyApi(uiState.url?.value ?: "", TimeoutTypes.LONG).command(HansCommand.reset())
                                updateProgress("reseting finished")
                                setupCollectingEnv()
                            } catch (e: Exception) {
                                updateProgress("error reseting = ${e.message}")
                            }
                        }
                    },
                    sendBtn = ButtonVM(true, "save results") {
                        viewModelScope.launch {
                            safeCancelJob()
                            afterSendData()
                        }
                    }
                ),
            )
        }
    }

    private fun calculateDate(): String {
        return Clock.System.now().toString()
    }

    private suspend fun afterSendData() {
        val after = loadEnv()
        updateProgress("env collected")
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
                temperature = uiState.customData!!.before!!.temperature!!,
                pressure = uiState.customData!!.before!!.pressure!!,
                humidity = uiState.customData!!.before!!.humidity!!,
                timestamp = uiState.customData!!.beforeTime!!
            ),
            environmentalParamAfter = Api.Env(
                temperature = after.temperature!!,
                pressure = after.pressure!!,
                humidity = after.humidity!!,
                timestamp = Clock.System.now().toEpochMilliseconds()
            ),
            zeroFlowData = uiState.customData!!.zeroFlow!!,
            zeroFlowDataTime = uiState.customData!!.zeroFlowDataTime!!,
            zeroFlowDataCount = uiState.customData!!.zeroFlowDataCount!!,
            steadyFlowRawData = null,
            waveformRawData = uiState.customData!!.results.map {
                WaveformData(
                    NameHelper.parse(it.name),
                    it.waveFormRawSignal,
                    "${it.sendSpirometryResult}\n${(it.humidity as Response.TEXT).response}\n${(it.temperature as Response.TEXT).response}",
                    it.timestamp
                )
            },
            type = RecordingTypeHelper.findTypeBasedOnNames(uiState.customData!!.results.map { it.name }).name,
            rawDataType = RawDataType.WAVEFORM.name,
            notes = uiState.note?.value ?: "",
            totalRawSignalControlCount = 0,
            totalRawSignalCount = 0,
            overallSampleLoss =  0,
            overallPercentageLoss = 0.0,
            flowRawData = null
        )
        trySendToApi(request)
    }


    private suspend fun trySendToApi(request: Api.PostData) {
        try {
            loading(true)
            val response = Api().postNewRawData(request)
            updateProgress(response)
            val current = uiState.customData?.results?.map { it.name } ?: listOf()
            val added = (uiState.customData?.history ?: mutableListOf()).apply {
                add(current)
            }
            updateUiState {
                copy(
                    customData = uiState.customData?.copy(
                        results = mutableListOf(),
                        history = added
                    )
                )
            }
            loading(false)
            setupCollectingEnv()
        } catch (e: Exception) {
            loading(false)
            updateUiState {
                copy(
                    repeatSendingDialog = DialogData({
                        GlobalScope.launch { trySendToApi(request) }
                    }, {
                        updateUiState { copy(repeatSendingDialog = null) }
                    })
                )
            }
        }
    }


    private fun updateProgress(action: String) {
        updateUiState {
            copy(customData = customData?.copy(info = action))
        }
    }

    private fun selectSequence() {
        viewModelScope.launch {
            try {
                val sequences = HansProxyApi(uiState.url?.value ?: "",TimeoutTypes.NORMAL).getAvailableSequences()
                updateUiState {
                    copy(
                        selectData = SelectData(sequences, { result ->
                            viewModelScope.launch {
                                safeCancelJob()
                                try {
                                    HansProxyApi(
                                        uiState.url?.value ?: "", TimeoutTypes.NORMAL
                                    ).waveformLoad(
                                        HansCommand.waveform(result.removePrefix("/waveforms/"))
                                    )

                                    updateUiState {
                                        copy(
                                            selectData = null,
                                            customData = uiState.customData?.copy(
                                                selectedWaveForm = result.removePrefix(
                                                    "/waveforms/"
                                                )
                                            )
                                        )
                                    }
                                }catch (e: Exception){
                                    updateUiState {
                                        copy(
                                            selectData = null,
                                        )
                                    }
                                    updateProgress("error during setting waveform: ${e.message}")
                                }
                            }
                        })
                    )
                }
                setupCollectingEnv()
            } catch (e: Exception) {
                showError("Erorr", e)
            }
        }
    }

    private fun showError(title: String, e: Exception) {
        updateUiState {
            copy(errorData = ErrorData(title, e.message ?: e.toString()) {
                updateUiState { copy(errorData = null) }
            })
        }
    }

    private suspend fun updateBattery(){
        device?.readBatteryCommand?.execute()?.let {  bat ->
            updateUiState {
                copy(deviceData = deviceData?.copy(battery = bat))
            }
        }
    }

    private fun disconnect() {
        loading(true)
        viewModelScope.launch {
            safeCancelJob()
            device?.disconnectCommand?.execute()
            startSearching()
            loading(false)
            updateUiState {
                copy(
                    deviceData = null,
                    disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                )
            }
        }
    }

    private fun loading(state: Boolean) {
        updateUiState { copy(loading = state) }
    }
}