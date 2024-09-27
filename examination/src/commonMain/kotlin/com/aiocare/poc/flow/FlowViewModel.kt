package com.aiocare.poc.flow

import com.aiocare.bluetooth.BaseAioCareDevice
import com.aiocare.bluetooth.device.AioCareDevice
import com.aiocare.bluetooth.deviceFactory.DeviceFactory
import com.aiocare.bluetooth.deviceProvider.DeviceProvider
import com.aiocare.bluetooth.di.inject
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.poc.superCat.InputData
import com.aiocare.poc.superCat.custom.DeviceData
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CalibrationUiState(
    val startBtn: ButtonVM = ButtonVM(visible = false, text = "start"),
    val stopBtn: ButtonVM = ButtonVM(visible = false, text = "stop"),
    val sendBtn: ButtonVM = ButtonVM(visible = false, text = "send"),
    val note: InputData? = null,
    val devices: List<DeviceItem> = listOf(),
    val deviceData: DeviceData? = null,
    val disconnectBtn: ButtonVM = ButtonVM(visible = false, text = "disconnect"),
    val measurementTimer: Int = 0,
    val realtimeData: String = "",
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

    fun init(){
        startSearching()
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
                            deviceData = null,
                            disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                            measurementTimer = 0
                        )
                    }
                }
            }
        }
    }

    private fun disconnect() {
//        updateProgress("disconnect")
//        updateSequenceName("")
        viewModelScope.launch {
            timerJob?.cancelAndJoin()
            actionJob?.cancelAndJoin()
//            getIConnect().disconnect()
            startSearching()
            updateUiState {
                copy(
                    deviceData = null,
                    disconnectBtn = uiState.disconnectBtn.copy(visible = false),
                    measurementTimer = 0
                )
            }
        }
    }


    private fun scanClicked(scan: BaseAioCareDevice) {
        viewModelScope.launch {
            try {
                device = deviceFactory.create(scan)
                scanJob?.cancelAndJoin()
                startObservingState()
                val battery = device?.readBatteryCommand?.execute() ?: 0
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
                            visible = true,
                            onClickAction = {
                                stop()
                            }
                        ),
                        sendBtn = sendBtn.copy(
                            visible = false,
                            onClickAction = {
                                send()
                            }
                        ),
                    )
                }
            } catch (e: Exception) {
//                updateUiState {
//                    copy(
//                        zeroFlowDialog = ZeroFlowDialogData("${e::class.simpleName} ${e.message}") {
//                            updateUiState { copy(zeroFlowDialog = null) }
//                        },
//                    )
//                }
            }
        }
    }

    private fun start() {
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
            device?.readFlowCommand?.values?.collect{
                rawSignal.add(it)
                updateData()
            }
        }
    }

    private fun updateData(){
        updateUiState {
            copy(
                realtimeData = rawSignal.takeLast(15)
                    .joinToString("\n", "", "") { (it.sum().toDouble() / it.size).toString() }
            )
        }
    }

    private fun stop() {
        timerJob?.cancel()
        actionJob?.cancel()
        updateUiState {
            copy(
                sendBtn = sendBtn.copy(visible = true)
            )
        }
    }

    private fun send() {

    }

}