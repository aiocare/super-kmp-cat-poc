package com.aiocare

data class Calibration(
    val version: Int,
//    val self_adjusting: CalibrationData,
    val parameters: List<Double>,
    val pro: Boolean,
    val tube: String
){
    data class CalibrationData(
        val range_min: Double,
        val range_max: Double,
        val x: List<Double>,
    )
}