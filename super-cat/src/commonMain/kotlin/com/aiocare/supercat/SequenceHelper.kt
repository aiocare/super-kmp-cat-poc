package com.aiocare.supercat

import com.aiocare.model.Units

val standardPef by lazy {
    listOf(
        "Aat1-7Pk",
        "Aat2-5Pk",
        "Aat3-3Pk",
        "Aat5-0Pk",
        "Aat7-5Pk",
        "Aat10-0Pk",
        "Aat12-0Pk",
        "Aat14-5Pk",
        "Aat17-0Pk",
        "Bat3-3Pk",
        "Bat7-5Pk",
        "Bat12-0Pk",
        "Bat10-0Pk",
        "Bat14-5Pk",
        "Bat17-0Pk"
    )
}

val pefA by lazy {
    listOf(
        "Aat1-7Pk",
        "Aat2-5Pk",
        "Aat3-0Pk",
        "Aat3-3Pk",
        "Aat5-0Pk",
        "Aat6-0Pk",
        "Aat7-5Pk",
        "Aat9-0Pk",
        "Aat10-0Pk",
        "Aat12-0Pk",
        "Aat14-5Pk",
        "Aat17-0Pk",
    )
}

val pefB by lazy {
    listOf(
        "Bat1-7Pk",
        "Bat2-5Pk",
        "Bat3-0Pk",
        "Bat3-3Pk",
        "Bat5-0Pk",
        "Bat6-0Pk",
        "Bat7-5Pk",
        "Bat9-0Pk",
        "Bat10-0Pk",
        "Bat12-0Pk",
        "Bat14-5Pk",
        "Bat17-0Pk",
    )
}

val pefBAdj by lazy {
    listOf(
        "Bat1-7Pk-adjusted",
        "Bat2-5Pk-adjusted",
        "Bat3-0Pk-adjusted",
        "Bat3-3Pk-adjusted",
        "Bat5-0Pk-adjusted",
        "Bat6-0Pk-adjusted",
        "Bat7-5Pk-adjusted",
        "Bat9-0Pk-adjusted",
        "Bat10-0Pk-adjusted",
        "Bat12-0Pk-adjusted",
        "Bat14-5Pk-adjusted",
        "Bat17-0Pk-adjusted"
    )
}

enum class CalibrationSequenceType(val maxFlow: Units.FlowUnit.L_S) {
    OLD_0_16(Units.FlowUnit.L_S(16.0)), NEW_0_17(Units.FlowUnit.L_S(17.0))
}