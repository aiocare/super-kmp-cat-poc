
package com.aiocare.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiocare.sdk.IAioCareDevice
import com.aiocare.sdk.IAioCareScan
import com.aiocare.sdk.connecting.getIConnectMobile
import com.aiocare.model.AccelerometerResult
import com.aiocare.sdk.scan.getIScan
import com.aiocare.sdk.services.chargingState
import com.aiocare.sdk.services.getHrWithSpO2
import com.aiocare.sdk.services.readBattery
import com.aiocare.sdk.services.readFirmware
import com.aiocare.sdk.services.readHardware
import com.aiocare.sdk.services.readHumidity
import com.aiocare.sdk.services.readPressure
import com.aiocare.sdk.services.readTemperature
import com.aiocare.sdk.services.setHeartRateLedPower
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class MainModel(
    val devices: List<IAioCareScan> = listOf(),
    val temperature: Float? = null,
    val humidity: Float? = null,
    val pressure: Float? = null,
    val battery: Int? = null,
    val acc: AccelerometerResult? = null,
    val firmware: String? = null,
    val hardware: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scanScope = viewModelScope
    internal val state: MutableStateFlow<MainModel> = MutableStateFlow(value = MainModel())
    private var currentAioCareDevice: IAioCareDevice? = null

    private var searchingJob: Job? = null

    fun start() {
        searchingJob = scanScope.launch {
            getIScan().start()?.collect { device ->
                state.update { it.copy(devices = listOf(device)) }
            }
        }
    }
    fun stop() {
        scanScope.launch { searchingJob?.cancelAndJoin() }
    }

    private var accJob: Job? = null
    fun startAcc(){
        scanScope.launch {
            val state = currentAioCareDevice?.chargingState()
            println(state?.name)
        }
        accJob = scanScope.launch {
            currentAioCareDevice?.setHeartRateLedPower(8,8)
            currentAioCareDevice?.getHrWithSpO2()?.collect{ acc ->
                println("xDDD -> ${acc.irData.joinToString()}")
                println("xDDD -> 2${acc.redData.joinToString()}")
            }
        }
    }

    fun stopAcc(){
        scanScope.launch {
            accJob?.cancelAndJoin()
        }
    }

    fun deviceClicked(aioCareScan: IAioCareScan) {
        scanScope.launch {
            currentAioCareDevice = getIConnectMobile().connectMobile(this, aioCareScan)
            updateFields()

//            scanScope.launch {
//                currentAioCareDevice?.readTemperatureFromFlowSensor()?.collect{ acc ->
////                    state.update { it.copy(acc = acc) }
//                    Log.d("XDDD", acc.joinToString() )
//                }
//            }
        }
    }

    fun updateFields() {
        scanScope.launch {
            supervisorScope {
                launch { state.update { it.copy(battery = currentAioCareDevice?.readBattery()) } }
                launch { state.update { it.copy(temperature = currentAioCareDevice?.readTemperature()) } }
                launch { state.update { it.copy(humidity = currentAioCareDevice?.readHumidity()) } }
                launch { state.update { it.copy(pressure = currentAioCareDevice?.readPressure()) } }
                launch { state.update { it.copy(hardware = currentAioCareDevice?.readHardware()) } }
                launch { state.update { it.copy(firmware = currentAioCareDevice?.readFirmware()) } }
            }
        }
    }}