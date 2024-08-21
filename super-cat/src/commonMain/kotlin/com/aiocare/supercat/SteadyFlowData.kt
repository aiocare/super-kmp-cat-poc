package com.aiocare.supercat

import com.aiocare.list.ToArray
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class SteadyFlowData(
    val flow: Double,
    val volume: Double,
    val exhaleRawSignal: List<Int>,
    val inhaleRawSignal: List<Int>,
    val exhaleRawSignalCount: Int,
    val exhaleRawSignalTime: Long,
    val inhaleRawSignalCount: Int,
    val inhaleRawSignalTime: Long,
    val createdAt: String = Clock.System.now().epochSeconds.toString()
)

fun List<SteadyFlowData>.toSteadyFlowDataList() = SteadyFlowDataList(this)

@Serializable
@JsExport
class SteadyFlowDataList(
    val list: List<SteadyFlowData> = listOf()
) : List<SteadyFlowData> by list, ToArray<SteadyFlowData> {

    override fun toArray(): Array<SteadyFlowData> = list.toTypedArray()
}
