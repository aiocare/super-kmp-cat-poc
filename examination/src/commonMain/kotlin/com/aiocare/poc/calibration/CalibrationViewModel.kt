package com.aiocare.poc.calibration

import com.aiocare.cortex.cat.InputData
import com.aiocare.cortex.cat.hans.Point
import com.aiocare.model.RawData
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import com.aiocare.poc.Holder
import com.aiocare.poc.searchDevice.DeviceItem
import com.aiocare.sdk.IAioCareDevice
import com.aiocare.sdk.IAioCareScan
import com.aiocare.sdk.connecting.getIConnectMobile
import com.aiocare.sdk.scan.getIScan
import com.aiocare.sdk.services.readBattery
import com.aiocare.sdk.services.readFlow
import com.aiocare.sdk.services.readHumidity
import com.aiocare.sdk.services.readPressure
import com.aiocare.sdk.services.readTemperature
import com.aiocare.util.ButtonVM
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch


data class CalibrationUiState(
    val devices: List<DeviceItem> = listOf(),
    val loading: Boolean = false,
    val graphObjects: GraphObjects = GraphObjects(),

    val startBtn: ButtonVM = ButtonVM(visible = false, text = "start"),
    val stopBtn: ButtonVM = ButtonVM(visible = false, text = "stop"),
    val sendBtn: ButtonVM = ButtonVM(visible = false, text = "send"),
    val startAgainBtn: ButtonVM = ButtonVM(visible = false, text = "startAgain"),
    val envAfterBtn: ButtonVM = ButtonVM(visible = false, text = "Env aft"),
    val envBeforeBtn: ButtonVM = ButtonVM(visible = false, text = "Env bef"),

    val toastData: Pair<Int, String>? = null,
    val deviceInfo: DeviceInfo = DeviceInfo(),

    val before: EnvironmentalData = EnvironmentalData(),
    val after: EnvironmentalData = EnvironmentalData(),

    val infoData: String = ""
)

data class DeviceInfo(
    val name: String = "MS",
    val battery: Int? = null,
    val measurementTimer: Int = 0
)

data class EnvironmentalData(
    val temperature: Float? = null,
    val pressure: Float? = null,
    val humidity: Float? = null,
)

data class GraphObjects(
    val graphItems: List<Float> = listOf(),
    val exhalePoints: List<Point> = listOf(),
    val inhalePoints: List<Point> = listOf()
)

class CalibrationViewModel(
    config: Config
) : StatefulViewModel<CalibrationUiState>(CalibrationUiState(), config) {

    companion object {
        private var scanJob: Job? = null
        private var flowJob: Job? = null
        private var timerJob: Job? = null
        private var afterEnvJob: Job? = null

        private val rawSignal = mutableListOf<RawData>()

        private var device: IAioCareDevice? = null
    }

    init {
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

    private fun scanClicked(scan: IAioCareScan) {
        GlobalScope.launch {
            updateLoader(true)
            device = getIConnectMobile().connectMobile(this, scan)
            scanJob?.cancelAndJoin()

            val temperature = device?.readTemperature()
            updateUiState {
                it.copy(before = before.copy(temperature = temperature))
            }

            val pressure = device?.readPressure()
            updateUiState {
                it.copy(before = before.copy(pressure = pressure))
            }
            val humidity = device?.readHumidity()
            updateUiState {
                it.copy(before = before.copy(humidity = humidity))
            }
            val battery = device?.readBattery()
            updateUiState {
                it.copy(deviceInfo = deviceInfo.copy(battery = battery))
            }

            updateLoader(false)
            updateName(scan.getName())

            updateUiState {
                it.copy(
                    devices = listOf(),
                    startBtn = startBtn.copy(visible = true, onClickAction = {
                        device?.let {
                            startFlow(it)
                        }
                    }),
                    stopBtn = stopBtn.copy(onClickAction = {
                        stopFlow()
                    })
                )
            }
        }
    }

    private fun envAfter() {
        afterEnvJob?.cancel()
        afterEnvJob = viewModelScope.launch {
            updateLoader(true)
            try {
                val temperature = device?.readTemperature()
                updateUiState {
                    it.copy(after = after.copy(temperature = temperature))
                }
                val pressure = device?.readPressure()
                updateUiState {
                    it.copy(after = after.copy(pressure = pressure))
                }
                val humidity = device?.readHumidity()
                updateUiState {
                    it.copy(after = after.copy(humidity = humidity))
                }

            } catch (e: Throwable) {
                updateUiState {
                    it.copy(
                        toastData = Pair((uiState.toastData?.first ?: 0) + 1, "error! ${e}")
                    )
                }
            }
            updateLoader(false)
        }
    }


    private fun stopFlow() {
        viewModelScope.launch {
            flowJob?.cancelAndJoin()
            timerJob?.cancelAndJoin()
            updateUiState {
                it.copy(
                    sendBtn = sendBtn.copy(visible = true, onClickAction = {}),
                    envAfterBtn = envAfterBtn.copy(
                        visible = true,
                        onClickAction = {
                            envAfter()
                        }
                    ),
                    envBeforeBtn = envBeforeBtn.copy(
                        visible = true,
                        onClickAction = {
                            envBefore()
                        }
                    ),
                    startAgainBtn = startAgainBtn.copy(
                        visible = true,
                        onClickAction = {
                            startAgain()
                        }
                    ),
                    startBtn = startBtn.copy(visible = false),
                    stopBtn = stopBtn.copy(visible = false),
                    graphObjects = graphObjects.copy(graphItems = rawSignal.map { it.flow }
                        .flatten().let {
                            InputData(it)
                        }.map { it.toFloat() })
                )
            }
        }
    }

    private fun envBefore() {
        afterEnvJob?.cancel()
        afterEnvJob = viewModelScope.launch {
            updateLoader(true)
            try {

                val temperature = device?.readTemperature()
                updateUiState {
                    it.copy(before = before.copy(temperature = temperature))
                }

                val pressure = device?.readPressure()
                updateUiState {
                    it.copy(before = before.copy(pressure = pressure))
                }
                val humidity = device?.readHumidity()
                updateUiState {
                    it.copy(before = before.copy(humidity = humidity))
                }
                val battery = device?.readBattery()
                updateUiState {
                    it.copy(deviceInfo = deviceInfo.copy(battery = battery))
                }

            } catch (e: Throwable) {
                updateUiState {
                    it.copy(
                        toastData = Pair((uiState.toastData?.first ?: 0) + 1, "error! ${e}")
                    )
                }
            }
            updateLoader(false)
        }
    }

    private fun startAgain() {
        startFlow(device!!)
    }

    private fun startCalculatingSeconds() {
        updateUiState {
            it.copy(
                deviceInfo = deviceInfo.copy(measurementTimer = 0)
            )
        }

        timerJob = GlobalScope.launch {
            while (true) {
                delay(1000)
                updateUiState {
                    it.copy(
                        deviceInfo = deviceInfo.copy(measurementTimer = deviceInfo.measurementTimer + 1)
                    )
                }
            }
        }
    }

    private fun startFlow(device: IAioCareDevice) {
        flowJob = viewModelScope.launch {
            startCalculatingSeconds()
            rawSignal.clear()
            updateUiState {
                it.copy(
                    graphObjects = GraphObjects(),
                    startBtn = startBtn.copy(visible = false),
                    stopBtn = stopBtn.copy(visible = true),
                    sendBtn = sendBtn.copy(visible = false),
                    envAfterBtn = envAfterBtn.copy(visible = false),
                    envBeforeBtn = envBeforeBtn.copy(visible = false),
                )
            }
            try {
                device.readFlow().collectIndexed { index, value ->
                    rawSignal.add(
                        RawData(
                            flow = value.toList(),
//                        temperature = value.second.toList(),
//                        pressure = tempPressure,
//                        humidity = tempHumidity
                        )
                    )
                    updateGraph(value.toList())
                }
            } catch (e: Throwable) {
                updateUiState {
                    it.copy(
                        toastData = Pair((uiState.toastData?.first ?: 0) + 1, "error! ${e}")
                    )
                }
            }
        }
    }

    private fun updateGraph(array: Collection<Int>) {
        val items =
            uiState.graphObjects.graphItems.plus((array.average().toFloat())).toMutableList()
        if (items.size > 100)
            items.removeAt(0)
        updateUiState {
            it.copy(
                graphObjects = graphObjects.copy(graphItems = items),
            )
        }
    }


    private fun updateLoader(load: Boolean) {
        updateUiState {
            it.copy(
                loading = load
            )
        }
    }


    fun updateName(name: String) {
        Holder.name = name
        updateUiState {
            it.copy(
                deviceInfo = deviceInfo.copy(name = name)
            )
        }
    }
}