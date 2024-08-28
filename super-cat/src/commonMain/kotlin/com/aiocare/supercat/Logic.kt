package com.aiocare.supercat

import com.aiocare.bluetooth.command.FlowControlData
import com.aiocare.bluetooth.device.AioCareDevice
import com.aiocare.list.toIntList
import com.aiocare.supercat.api.HansCommand
import com.aiocare.supercat.api.HansProxyApi
import com.aiocare.supercat.api.Response
import com.aiocare.supercat.api.TimeoutTypes
import com.aiocare.units.Units
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class Logic(private val hostAddress: String) {

    private val api by lazy { HansProxyApi(hostAddress, TimeoutTypes.NORMAL) }
    private val longTimeoutApi by lazy { HansProxyApi(hostAddress, TimeoutTypes.LONG) }

    private fun prepareV7(type: CalibrationSequenceType): MutableList<Units.FlowUnit.Ls> {
        var current = Units.FlowUnit.Ls(0.1)
        val list = mutableListOf<Units.FlowUnit.Ls>()
        while (current < type.maxFlow) {
            list.add(current)
            val step = when (current.value) {
                in 0.0..0.99 -> 0.1
                else -> 0.2
            }
            current = Units.FlowUnit.Ls(current + Units.FlowUnit.Ls(step))
        }
        return list
    }

    fun preparePef(list: List<String>, repeat: Int): List<CalibrationActions> {
        return list.times(repeat).map {
            CalibrationActions(
                name = it,
                flow = Units.FlowUnit.Ls(0.0),
                volume = Units.VolumeUnit.Liter(0.0),
                beforeAction = {
                    api.command(HansCommand.volume(Units.VolumeUnit.Liter(8.0)))
                    api.command(HansCommand.reset())
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
        val flow: Units.FlowUnit.Ls,
        val volume: Units.VolumeUnit.Liter,
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
                flow = Units.FlowUnit.Ls(it.first),
                volume = Units.VolumeUnit.Liter(it.second),
                name = "v5",
                beforeAction = {
                    if (it.first == 0.10) {
                        api.command(HansCommand.volume(Units.VolumeUnit.Liter(8.0)))
                        longTimeoutApi.command(HansCommand.reset())
                    }
                },
                exhaleAction = {
                    api.command(
                        HansCommand.flow(
                            Units.FlowUnit.Ls(it.first),
                            Units.VolumeUnit.Liter(it.second),
                            HansCommand.Companion.Type.Exhale
                        )
                    )
                },
                inhaleAction = {
                    api.command(
                        HansCommand.flow(
                            Units.FlowUnit.Ls(it.first),
                            Units.VolumeUnit.Liter(it.second),
                            HansCommand.Companion.Type.Inhale
                        )
                    )
                }
            )
        }
    }

    private fun List<Units.FlowUnit.Ls>.transformToCalibrationActions(): List<CalibrationActions> =
        this.map { prepareCalibrationActions(it) }

    suspend fun processWaveform(
        actions: List<CalibrationActions>,
        device: AioCareDevice,
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
                            device.readFlowCommand.values.collect {
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
                    api.command(HansCommand.volume(Units.VolumeUnit.Liter(8.0)))
                    longTimeoutApi.command(HansCommand.reset())
                    log(e.message ?: e.toString())
                }
            }
            wfd!!
        }
    }

    suspend fun processSequence(
        actions: List<CalibrationActions>,
        device: AioCareDevice,
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
                    val exhaleStartTime = Clock.System.now().toEpochMilliseconds()
                    val readFlowExhale = device.readControlFlowCommand
                    coroutineScope {
                        val deferredExhale = CompletableDeferred<Int>()
                        recordJob = launch {
                            readFlowExhale.values.collect {
                                when (it) {
                                    is FlowControlData.Control -> deferredExhale.complete(it.amount)
                                    is FlowControlData.Data -> it.list.forEach {
                                        recordedRawSignal.add(
                                            it
                                        )
                                    }
                                }
                            }
                        }
                        log("exhale ${it.flow.value}")
                        it.exhaleAction.invoke()
                        readFlowExhale.cancelStream()
                        val exhaleCalculated = deferredExhale.await()
                        recordJob?.cancelAndJoin()
                        val deferredInhale = CompletableDeferred<Int>()
                        val readFlowInhale = device.readControlFlowCommand
                        val exhaleFinishTime = Clock.System.now().toEpochMilliseconds()
                        coroutineScope {
                            recordJob = launch {
                                readFlowInhale.values.collect {
                                    when(it){
                                        is FlowControlData.Control -> deferredInhale.complete(it.amount)
                                        is FlowControlData.Data -> it.list.forEach { recordedInhaleRawSignal.add(it) }
                                    }
                                }
                            }
                            log("inhale ${it.flow.value}")
                            it.inhaleAction.invoke()
                            readFlowInhale.cancelStream()
                            val inhaleCalculated = deferredInhale.await()
                            recordJob?.cancelAndJoin()
                            val inhaleFinishTime = Clock.System.now().toEpochMilliseconds()
                            sfd = SteadyFlowData(
                                flow = it.flow.value,
                                volume = it.volume.value,
                                exhaleRawSignal = recordedRawSignal.toIntList(),
                                inhaleRawSignal = recordedInhaleRawSignal.toIntList(),
                                exhaleRawSignalTime =exhaleFinishTime - exhaleStartTime,
                                exhaleRawSignalCount = recordedRawSignal.size,
                                inhaleRawSignalTime =inhaleFinishTime - exhaleFinishTime,
                                inhaleRawSignalCount = recordedInhaleRawSignal.size,
                                exhaleRawSignalControlCount = exhaleCalculated,
                                inhaleRawSignalControlCount = inhaleCalculated
                            )
                        }
                    }
                } catch (e: Exception) {
                    api.command(HansCommand.volume(Units.VolumeUnit.Liter(8.0)))
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
                flow = Units.FlowUnit.Ls(it.toDouble()),
                volume = Units.VolumeUnit.Liter(it.toDouble()),
                beforeAction = {
                    api.command(HansCommand.volume(Units.VolumeUnit.Liter(8.0)))
                    longTimeoutApi.command(HansCommand.reset())
                },
                exhaleAction = {
                    api.waveformLoadRun(HansCommand.waveform("C1-C13_(ISO26782)@C$it"))
                },
                inhaleAction = {}
            )
        }.times(repeat)
    }

    private fun prepareCalibrationActions(flow: Units.FlowUnit.Ls): CalibrationActions {
        val volumeValue = when (flow.value) {
            in 0.0..1.0 -> Units.VolumeUnit.Liter(flow.value * 2)
            in 1.0..1.99 -> Units.VolumeUnit.Liter(flow.value)
            in 16.0..18.0 -> Units.VolumeUnit.Liter(8.0)
            else -> Units.VolumeUnit.Liter(flow.value / 2)
        }
        return CalibrationActions(
            flow = flow,
            volume = volumeValue,
            beforeAction = {
                if (flow == Units.FlowUnit.Ls(0.1)) {
                    api.command(HansCommand.volume(Units.VolumeUnit.Liter(8.0)))
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
