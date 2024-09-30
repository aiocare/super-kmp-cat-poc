package com.aiocare.poc.flow

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
import com.aiocare.poc.calibration.DeviceInfo
import com.aiocare.poc.calibration.EnvironmentalData
import com.aiocare.poc.ktor.Api
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.poc.superCat.ErrorChecker
import com.aiocare.poc.superCat.InitHolder
import com.aiocare.poc.superCat.InputData
import com.aiocare.poc.superCat.SuperCatViewModel.ZeroFlowData
import com.aiocare.poc.superCat.custom.DeviceData
import com.aiocare.supercat.PhoneInfo
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CalibrationUiState(
    val startBtn: ButtonVM = ButtonVM(visible = false, text = "start"),
    val stopBtn: ButtonVM = ButtonVM(visible = false, text = "stop"),
    val sendBtn: ButtonVM = ButtonVM(visible = false, text = "send"),
    val navigateToSuperCatBtn: ButtonVM = ButtonVM(visible = false, text = "Nav to superCat"),
    val settingsBtn: ButtonVM = ButtonVM(visible = false, text = "settings"),
    val dialogData: Pair<Boolean, List<ButtonVM>>? = null,
    val selectedOperator: String? = null,
    val note: InputData = InputData("", "note", {}),
    val devices: List<DeviceItem> = listOf(),
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val deviceData: DeviceData? = null,
    val disconnectBtn: ButtonVM = ButtonVM(visible = false, text = "disconnect"),
    val measurementTimer: Int = 0,
    val realtimeData: String = "",
    val description: String = "",
    val before: EnvironmentalData = EnvironmentalData(),
    val refreshing: Boolean = false,
)

class FlowViewModel(config: Config) :
    StatefulViewModel<CalibrationUiState>(CalibrationUiState(), config) {

    private var scanJob: Job? = null
    private val deviceProvider = inject<DeviceProvider>()
    private val deviceFactory = inject<DeviceFactory>()
    private var device: AioCareDevice? = null
    private var timerJob: Job? = null
    private var actionJob: Job? = null
    private val rawSignal = mutableListOf<IntArray>()
    private var zeroFlow: ZeroFlowData? = null
    private var beforeTime: Long? = null


    fun init(navigate: (String) -> Unit) {
        updateUiState {
            copy(
                note = note.copy(onValueChanged = { val newValue = it
                    updateUiState { copy(note = note.copy(newValue)) }
                }),
                selectedOperator = InitHolder.operator,
                navigateToSuperCatBtn = navigateToSuperCatBtn.copy(true) {
                    disconnect()
                    navigate.invoke(Screens.SuperCat.route)
                },
                dialogData = Pair(false, listOf("Piotr", "Milena", "Darek", "Szymon").map {
                    ButtonVM(true, text = it, onClickAction = {
                        val item = it
                        InitHolder.operator = it
                        updateUiState {
                            copy(
                                selectedOperator = item,
                                dialogData = dialogData?.copy(false)
                            )
                        }
                    })
                }),
                settingsBtn = settingsBtn.copy(true) {
                    updateUiState { copy(dialogData = dialogData?.copy(true)) }
                }
            )
        }
        startSearching()
    }

    fun hideDialog() {
        updateUiState {
            copy(dialogData = dialogData?.copy(false))
        }
    }

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

    private suspend fun zeroFlow(): ZeroFlowData {
        updateProgress("zeroFlow....")
        val out = mutableListOf<Int>()
        val startTime = Clock.System.now().toEpochMilliseconds()
        val data = coroutineScope {
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
        return ZeroFlowData(data, finishTime - startTime, data.size)
    }


    private fun startObservingState() {
        viewModelScope.launch {
            device?.observeConnectionStateCommand?.values?.collect {
                if (!it) {
                    actionJob?.cancelAndJoin()
                    timerJob?.cancelAndJoin()
                    device = null
                    startSearching()
                    updateUiState {
                        copy(
                            startBtn = startBtn.copy(visible = false),
                            stopBtn = stopBtn.copy(visible = false),
                            sendBtn = sendBtn.copy(visible = false),
                            deviceData = null,
                            disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                            measurementTimer = 0
                        )
                    }
                }
            }
        }
    }

    fun updateProgress(refreshing: Boolean){
        updateUiState { copy(refreshing = refreshing) }
    }

    private fun disconnect() {
        viewModelScope.launch {
            timerJob?.cancelAndJoin()
            actionJob?.cancelAndJoin()
            startSearching()
            updateUiState {
                copy(
                    startBtn = startBtn.copy(visible = false),
                    stopBtn = stopBtn.copy(visible = false),
                    sendBtn = sendBtn.copy(visible = false),
                    deviceData = null,
                    disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                    measurementTimer = 0
                )
            }
        }
    }


    private fun scanClicked(scan: BaseAioCareDevice) {
        updateProgress(true)
        viewModelScope.launch {
            try {
                device = deviceFactory.create(scan)
                updateUiState {
                    copy(deviceData = DeviceData(device?.name ?: "", battery = 0))
                }
                scanJob?.cancelAndJoin()
                startObservingState()
                val battery = device?.readBatteryCommand?.execute() ?: 0
                beforeTime = Clock.System.now().toEpochMilliseconds()
                updateProgress(false)
                updateUiState {
                    copy(
                        deviceData = DeviceData(scan.name, battery),
                        devices = listOf(),
                        disconnectBtn = disconnectBtn.copy(
                            visible = true,
                            onClickAction = {
                                viewModelScope.launch {
                                    disconnect()
                                }
                            }),
                        startBtn = startBtn.copy(
                            visible = true,
                            onClickAction = {
                                start()
                            }
                        ),
                        stopBtn = stopBtn.copy(
                            visible = false,
                            onClickAction = {
                                stop()
                            }
                        ),
                        sendBtn = sendBtn.copy(
                            visible = false,
                            onClickAction = {
                                actionJob?.cancel()
                                actionJob = viewModelScope.launch {
                                    send()
                                }
                            }
                        ),
                    )
                }
            } catch (e: Exception) {
                updateProgress(false)
                updateUiState {
                    copy(description = "${e::class.simpleName} ${e.message}")
                }
            }
        }
    }

    private fun start() {
        updateUiState {
            copy(
                startBtn = startBtn.copy(false),
                stopBtn = stopBtn.copy(true),
                sendBtn = sendBtn.copy(false)
            )
        }
        timerJob?.cancel()
        timerJob = GlobalScope.launch {
            updateUiState {
                it.copy(measurementTimer = 0)
            }
            while (true) {
                delay(1000)
                updateUiState {
                    it.copy(measurementTimer = measurementTimer + 1)
                }
            }
        }
        actionJob = viewModelScope.launch {
            updateUiState {
                it.copy(realtimeData = "")
            }
            rawSignal.clear()
            try {
                zeroFlow = zeroFlow()
                val temperature = device?.readSingleTemperatureCommand?.execute()
                updateUiState {
                    it.copy(
                        before = before.copy(temperature = temperature?.toFloat()),
                        description = "temperature"
                    )
                }

                val pressure = device?.readPressureCommand?.execute()
                updateUiState {
                    it.copy(
                        before = before.copy(pressure = pressure?.toFloat()),
                        description = "pressure"
                    )
                }
                val humidity = device?.readHumidityCommand?.execute()
                updateUiState {
                    it.copy(
                        before = before.copy(humidity = humidity?.toFloat()),
                        description = "humidity"
                    )
                }
                device?.readFlowCommand?.values?.collect {
                    rawSignal.add(it)
                    updateData()
                }
            } catch (e: CancellationException) {
                updateUiState { copy(description = "action stopped") }
                stop()
                updateUiState { copy(sendBtn = sendBtn.copy(false)) }
            } catch (e: Exception) {
                timerJob?.cancel()
                updateUiState { copy(description = "${e::class.simpleName} - ${e.message}") }
                stop()
                updateUiState { copy(sendBtn = sendBtn.copy(false)) }
            }
        }
    }

    private fun updateData() {
        updateUiState {
            copy(
                realtimeData = rawSignal.takeLast(15)
                    .joinToString("\n", "", "") { (it.sum().toDouble() / it.size).toString() }
            )
        }
    }

    private fun stop() {
        updateUiState {
            copy(
                startBtn = startBtn.copy(true),
                stopBtn = stopBtn.copy(false),
                sendBtn = sendBtn.copy(true)
            )
        }
        timerJob?.cancel()
        actionJob?.cancel()
        updateUiState {
            copy(
                sendBtn = sendBtn.copy(visible = true)
            )
        }
    }

    private fun updateProgress(description: String) {
        updateUiState {
            copy(description = description)
        }
    }

    private fun calculateDate(): String {
        return Clock.System.now().toString()
    }

    private suspend fun send() {

        val temperature = device?.readSingleTemperatureCommand?.execute()
        updateUiState {
            it.copy(description = "temperature")
        }

        val pressure = device?.readPressureCommand?.execute()
        updateUiState {
            it.copy(description = "pressure")
        }
        val humidity = device?.readHumidityCommand?.execute()
        updateUiState {
            it.copy(description = "humidity")
        }

        val request = Api.PostData(
            environment = Api.Environment(
                recordingDevice = PhoneInfo().getPhoneModel(),
                hansIpAddress = null,
                hansSerialNumber = null,
                hansCalibrationId = null,
                appVersion = VersionHolder.version,
                spirometerDeviceSerial = uiState.deviceData?.name ?: "",
                operator = InitHolder.operator ?: "",
                date = calculateDate()
            ),
            environmentalParamBefore = Api.Env(
                temperature = uiState.before.temperature!!,
                pressure = uiState.before.pressure!!,
                humidity = uiState.before.humidity!!,
                timestamp = beforeTime!!
            ),
            environmentalParamAfter = Api.Env(
                temperature = temperature!!.toFloat(),
                pressure = pressure!!.toFloat(),
                humidity = humidity!!.toFloat(),
                timestamp = Clock.System.now().toEpochMilliseconds()
            ),
            zeroFlowData = zeroFlow!!.zeroFlow,
            zeroFlowDataTime = zeroFlow!!.zeroFlowDataTime,
            zeroFlowDataCount = zeroFlow!!.zeroFlowDataCounter,
            steadyFlowRawData = null,
            waveformRawData = null,
            flowRawData = Api.FlowRawData(rawSignal.flatMap { it.toList() }),
            type = "FLOW",
            rawDataType = "FLOW",
            notes = uiState.note.value,
            totalRawSignalControlCount = null,
            totalRawSignalCount = null,
            overallSampleLoss = null,
            overallPercentageLoss = null
        )
        trySendToApi(request)
    }

    private suspend fun trySendToApi(request: Api.PostData) {
        try {
            val response = Api().postNewRawData(request)
            updateProgress(response)
        } catch (e: Exception) {
            updateUiState {
                copy(
                    description = "${e::class.simpleName} - ${e.message}"
                )
            }
        }
    }

}