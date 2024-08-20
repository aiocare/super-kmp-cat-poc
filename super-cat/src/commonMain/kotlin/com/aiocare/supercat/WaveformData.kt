package com.aiocare.supercat

import kotlinx.serialization.Serializable
import kotlin.js.JsExport

@Serializable
@JsExport
data class WaveformData(
    val name: String,
    val rawSignal: List<Int>,
    val hansCalculatedValues: String,
    val timestamp: Long
)