package com.aiocare.poc.searchDevice

import com.aiocare.sdk.IAioCareScan
import com.aiocare.sdk.connecting.getIConnectMobile
import com.aiocare.model.AccelerometerResult
import com.aiocare.sdk.scan.getIScan
import com.aiocare.sdk.services.readBattery
import com.aiocare.sdk.services.readFlowTemperatureAcc
//import com.aiocare.CppLogic
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch


data class SearchDeviceUiState(
    val devices: List<DeviceItem> = listOf(),
    val loading: Boolean = false,
    val battery: String = "",
    val graphItems: List<Float> = listOf(),
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
)

data class DeviceItem(
    val text: String,
    val aioCareScan: IAioCareScan,
    val onDeviceClicked: () -> Unit
)

class SearchDeviceViewModel constructor(
    config: Config
) : StatefulViewModel<SearchDeviceUiState>(SearchDeviceUiState(), config) {

    companion object{
        private var scanJob: Job? = null
//        private var cpp: CppLogic? = null
    }

//    fun initCortex(cppLogic: CppLogic){
//        cppLogic.init()
//        cpp = cppLogic
//    }

    fun onStart() {
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
        updateLoader(true)
        viewModelScope.launch {
            val device = getIConnectMobile().connectMobile(this, scan)
            scanJob?.cancelAndJoin()
            updateBattery(device.readBattery())
            updateLoader(false)
            device.readFlowTemperatureAcc().collect{
//                val col = cpp?.calculate(it.flowData.map { it.toDouble() })
//                updateGraph(col?: listOf())
//                updateAcc(it.accelerometerData)
            }
        }
    }
    private fun updateAcc(accelerometerData: Array<AccelerometerResult>) {
        val x = accelerometerData.map { it.x }.average()
        val y = accelerometerData.map { it.y }.average()
        val z = accelerometerData.map { it.z }.average()
        updateUiState {
            it.copy(
                x = x,
                y = y,
                z = z,
            )
        }
    }

    private fun updateGraph(array: Collection<Double>) {
        val items = uiState.graphItems.plus((array.average().toFloat())).toMutableList()
        if(items.size>40)
            items.removeAt(0)
        updateUiState {
            it.copy(
                graphItems  = items
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

    private fun updateBattery(battery: Int) {
        updateUiState {
            it.copy(
                battery = "battery = $battery"
            )
        }
    }
}
