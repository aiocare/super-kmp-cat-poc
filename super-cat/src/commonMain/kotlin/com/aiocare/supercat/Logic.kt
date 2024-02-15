package com.aiocare.supercat

import com.aiocare.model.SteadyFlowData
import com.aiocare.model.Units
import com.aiocare.model.WaveformData
import com.aiocare.sdk.IAioCareDevice
import com.aiocare.sdk.services.readFlow
import com.aiocare.supercat.api.HansCommand
import com.aiocare.supercat.api.HansProxyApi
import com.aiocare.supercat.api.Response
import com.aiocare.supercat.api.TimeoutTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class Logic(private val hostAddress: String) {

    private val api by lazy { HansProxyApi(hostAddress, TimeoutTypes.NORMAL) }
    private val longTimeoutApi by lazy { HansProxyApi(hostAddress, TimeoutTypes.LONG) }

    private fun prepareV7(type: CalibrationSequenceType): MutableList<Units.FlowUnit.L_S> {
        var current = Units.FlowUnit.L_S(0.1)
        val list = mutableListOf<Units.FlowUnit.L_S>()
        while (current < type.maxFlow) {
            list.add(current)
            val step = when (current.value) {
                in 0.0..0.99 -> 0.1
                else -> 0.2
            }
            current = Units.FlowUnit.L_S(current + Units.FlowUnit.L_S(step))
        }
        return list
    }

    fun preparePef(list: List<String>, repeat: Int): List<CalibrationActions> {
        return list.times(repeat).map {
            CalibrationActions(
                name = it,
                flow = Units.FlowUnit.L_S(0.0),
                volume = Units.VolumeUnit.LITER(0.0),
                beforeAction = {
//                    api.command(HansCommand.volume(Units.VolumeUnit.LITER(8.0)))
//                    api.command(HansCommand.reset())
                    api.waveformLoad(HansCommand.waveform(it))
                    longTimeoutApi.command(HansCommand.reset())
                },
                exhaleAction = {
                    api.command(HansCommand.run())
                },
                inhaleAction = {}
            )
        }
    }

    data class CalibrationActions(
        val flow: Units.FlowUnit.L_S,
        val volume: Units.VolumeUnit.LITER,
        val name: String? = null,
        val beforeAction: suspend () -> Unit,
        val exhaleAction: suspend () -> Unit,
        val inhaleAction: suspend () -> Unit
    )

    fun v7(type: CalibrationSequenceType, repeat: Int): List<CalibrationActions> =
        prepareV7(type).transformToCalibrationActions().times(repeat)

    fun v5(repeat: Int): List<CalibrationActions> = prepareV5().times(repeat)

    private fun prepareV5(): List<CalibrationActions> {
        return v5.map {
            CalibrationActions(
                flow = Units.FlowUnit.L_S(it.first),
                volume = Units.VolumeUnit.LITER(it.second),
                name = "v5",
                beforeAction = {
                    if (it.first == 0.10) {
                        api.command(HansCommand.volume(Units.VolumeUnit.LITER(8.0)))
                        longTimeoutApi.command(HansCommand.reset())
                    }
                },
                exhaleAction = {
                    api.command(
                        HansCommand.flow(
                            Units.FlowUnit.L_S(it.first),
                            Units.VolumeUnit.LITER(it.second),
                            HansCommand.Companion.Type.Exhale
                        )
                    )
                },
                inhaleAction = {
                    api.command(
                        HansCommand.flow(
                            Units.FlowUnit.L_S(it.first),
                            Units.VolumeUnit.LITER(it.second),
                            HansCommand.Companion.Type.Inhale
                        )
                    )
                }
            )
        }
    }

    private fun List<Units.FlowUnit.L_S>.transformToCalibrationActions(): List<CalibrationActions> =
        this.map { prepareCalibrationActions(it) }

    suspend fun processWaveform(
        actions: List<CalibrationActions>,
        device: IAioCareDevice,
        repeat: Int,
        log: (String) -> Unit,
        getTime: () -> Long
    ): List<WaveformData> {
        val preparedActions = actions.map { action -> List(repeat) { action } }.flatten()
        log("action size = ${preparedActions.size}")
        return preparedActions.mapIndexed { index, it ->
            var wfd: WaveformData? = null
            while (wfd == null) {
                try {
                    val recordedRawSignal = mutableListOf<Int>()
                    var recordJob: Job?
                    if (it.name != null) {
                        log("before ${it.name}")
                    } else {
                        log("before ${it.flow.value}")
                    }
                    it.beforeAction.invoke()
                    coroutineScope {
                        recordJob = launch {
                            device.readFlow().collect {
                                it.forEach {
                                    recordedRawSignal.add(it)
                                }
                            }
                        }
                        if (it.name != null) {
                            log(it.name)
                        } else {
                            log("exhale ${it.flow.value}")
                        }
                        it.exhaleAction.invoke()
                        recordJob?.cancelAndJoin()
                        val sendSpirometryResult =
                            when (val res = api.command(HansCommand.waveformData())) {
                                is Response.TEXT -> res.response
                                else -> "bad response"
                            }
                        wfd = WaveformData(
                            name = NameHelper.parse(it.name ?: "no name"),
                            rawSignal = recordedRawSignal,
                            hansCalculatedValues = sendSpirometryResult,
                            timestamp = getTime()
                        )
                    }
                } catch (e: Exception) {
                    api.command(HansCommand.volume(Units.VolumeUnit.LITER(8.0)))
                    longTimeoutApi.command(HansCommand.reset())
                    log(e.message ?: e.toString())
                }
            }
            wfd!!
        }
    }

    suspend fun processSequence(
        actions: List<CalibrationActions>,
        device: IAioCareDevice,
        repeat: Int,
        log: (String) -> Unit
    ): List<SteadyFlowData> {
        val preparedActions = actions.map { action -> List(repeat) { action } }.flatten()
        log("action size = ${preparedActions.size}")
        return preparedActions.mapIndexed { index, it ->

            var sfd: SteadyFlowData? = null
            while (sfd == null) {
                try {
                    val recordedRawSignal = mutableListOf<Int>()
                    val recordedInhaleRawSignal = mutableListOf<Int>()
                    var recordJob: Job?
                    log("before ${it.flow.value}")
                    it.beforeAction.invoke()
                    coroutineScope {
                        recordJob = launch {
                            device.readFlow().collect {
                                it.forEach {
                                    recordedRawSignal.add(it)
                                }
                            }
                        }
                        log("exhale ${it.flow.value}")
                        it.exhaleAction.invoke()
                        recordJob?.cancelAndJoin()
                        coroutineScope {
                            recordJob = launch {
                                device.readFlow().collect {
                                    it.forEach {
                                        recordedInhaleRawSignal.add(it)
                                    }
                                }
                            }
                            log("inhale ${it.flow.value}")
                            it.inhaleAction.invoke()
                            recordJob?.cancelAndJoin()
                            sfd = SteadyFlowData(
                                it.flow.value,
                                it.flow.value,
                                recordedRawSignal,
                                recordedInhaleRawSignal
                            )
                        }
                    }
                } catch (e: Exception) {
                    api.command(HansCommand.volume(Units.VolumeUnit.LITER(8.0)))
                    longTimeoutApi.command(HansCommand.reset())
                    log(e.message ?: e.toString())
                }
            }
            sfd!!
        }
    }

    fun prepareC1C11(repeat: Int): List<CalibrationActions> {
        return (1..11).map {
            CalibrationActions(
                name = "C$it",
                flow = Units.FlowUnit.L_S(it.toDouble()),
                volume = Units.VolumeUnit.LITER(it.toDouble()),
                beforeAction = {
                    api.command(HansCommand.volume(Units.VolumeUnit.LITER(8.0)))
                    longTimeoutApi.command(HansCommand.reset())
                },
                exhaleAction = {
                    api.waveformLoadRun(HansCommand.waveform("C1-C13_(ISO26782)@C$it"))
                },
                inhaleAction = {}
            )
        }.times(repeat)
    }

    private fun prepareCalibrationActions(flow: Units.FlowUnit.L_S): CalibrationActions {
        val volumeValue = when (flow.value) {
            in 0.0..1.0 -> Units.VolumeUnit.LITER(flow.value * 2)
            in 1.0..1.99 -> Units.VolumeUnit.LITER(flow.value)
            in 16.0..18.0 -> Units.VolumeUnit.LITER(8.0)
            else -> Units.VolumeUnit.LITER(flow.value / 2)
        }
        return CalibrationActions(
            flow = flow,
            volume = volumeValue,
            beforeAction = {
                if (flow == Units.FlowUnit.L_S(0.1)) {
                    api.command(HansCommand.volume(Units.VolumeUnit.LITER(8.0)))
                    longTimeoutApi.command(HansCommand.reset())
                }
            },
            exhaleAction = {
                api.command(
                    HansCommand.flow(
                        flow,
                        volumeValue,
                        HansCommand.Companion.Type.Exhale
                    )
                )
            },
            inhaleAction = {
                api.command(
                    HansCommand.flow(
                        flow,
                        volumeValue,
                        HansCommand.Companion.Type.Inhale
                    )
                )
            }
        )
    }
}
