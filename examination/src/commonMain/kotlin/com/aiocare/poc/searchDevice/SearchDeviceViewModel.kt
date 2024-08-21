package com.aiocare.poc.searchDevice


import com.aiocare.bluetooth.BaseAioCareDevice
import com.aiocare.mvvm.Config
import com.aiocare.mvvm.StatefulViewModel
import com.aiocare.mvvm.viewModelScope
import kotlinx.coroutines.Job
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
    val aioCareScan: BaseAioCareDevice,
    val onDeviceClicked: () -> Unit
)

class SearchDeviceViewModel constructor(
    config: Config
) : StatefulViewModel<SearchDeviceUiState>(SearchDeviceUiState(), config) {

    companion object{
        private var scanJob: Job? = null
    }


    fun onStart() {
        scanJob = viewModelScope.launch {
//            getIScan().start()?.collect { scan ->
//                updateUiState {
//                    it.copy(
//                        devices = devices.plus(DeviceItem(
//                            text = scan.getName(),
//                            aioCareScan = scan,
//                            onDeviceClicked = { scanClicked(scan) }
//                        )).distinctBy { item -> item.aioCareScan.getName() }
//                    )
//                }
//            }
        }
    }

//    private fun scanClicked(scan: IAioCareScan) {
//        updateLoader(true)
//        viewModelScope.launch {
//            val device = getIConnectMobile().connectMobile(this, scan)
//            scanJob?.cancelAndJoin()
//            updateBattery(device.readBattery())
//            updateLoader(false)
//            device.readFlowTemperatureAcc().collect{
////                val col = cpp?.calculate(it.flowData.map { it.toDouble() })
////                updateGraph(col?: listOf())
////                updateAcc(it.accelerometerData)
//            }
//        }
//    }
//    private fun updateAcc(accelerometerData: Array<AccelerometerResult>) {
//        val x = accelerometerData.map { it.x }.average()
//        val y = accelerometerData.map { it.y }.average()
//        val z = accelerometerData.map { it.z }.average()
//        updateUiState {
//            it.copy(
//                x = x,
//                y = y,
//                z = z,
//            )
//        }
//    }

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
