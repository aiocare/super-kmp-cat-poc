package com.aiocare.supercat

import com.aiocare.model.Units

val standardPef by lazy {
    listOf(
        "PEF_(ISO23747)/Profile_A@Aat1-7Pk",
        "PEF_(ISO23747)/Profile_A@Aat2-5Pk",
        "PEF_(ISO23747)/Profile_A@Aat3-3Pk",
        "PEF_(ISO23747)/Profile_A@Aat5-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat7-5Pk",
        "PEF_(ISO23747)/Profile_A@Aat10-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat12-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat14-5Pk",
        "PEF_(ISO23747)/Profile_A@Aat17-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat3-3Pk",
        "PEF_(ISO23747)/Profile_B@Bat7-5Pk",
        "PEF_(ISO23747)/Profile_B@Bat12-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat10-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat14-5Pk",
        "PEF_(ISO23747)/Profile_B@Bat17-0Pk"
    )
}

val pefA by lazy {
    listOf(
        "PEF_(ISO23747)/Profile_A@Aat1-7Pk",
        "PEF_(ISO23747)/Profile_A@Aat2-5Pk",
        "PEF_(ISO23747)/Profile_A@Aat3-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat3-3Pk",
        "PEF_(ISO23747)/Profile_A@Aat5-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat6-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat7-5Pk",
        "PEF_(ISO23747)/Profile_A@Aat9-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat10-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat12-0Pk",
        "PEF_(ISO23747)/Profile_A@Aat14-5Pk",
        "PEF_(ISO23747)/Profile_A@Aat17-0Pk"
    )
}

val pefB by lazy {
    listOf(
        "PEF_(ISO23747)/Profile_B@Bat1-7Pk",
        "PEF_(ISO23747)/Profile_B@Bat2-5Pk",
        "PEF_(ISO23747)/Profile_B@Bat3-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat3-3Pk",
        "PEF_(ISO23747)/Profile_B@Bat5-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat6-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat7-5Pk",
        "PEF_(ISO23747)/Profile_B@Bat9-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat10-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat12-0Pk",
        "PEF_(ISO23747)/Profile_B@Bat14-5Pk",
        "PEF_(ISO23747)/Profile_B@Bat17-0Pk"
    )
}

val pefBAdj by lazy {
    listOf(
        "Custom@Bat1-7Pk-adjusted",
        "Custom@Bat2-5Pk-adjusted",
        "Custom@Bat3-0Pk-adjusted",
        "Custom@Bat3-3Pk-adjusted",
        "Custom@Bat5-0Pk-adjusted",
        "Custom@Bat6-0Pk-adjusted",
        "Custom@Bat7-5Pk-adjusted",
        "Custom@Bat9-0Pk-adjusted",
        "Custom@Bat10-0Pk-adjusted",
        "Custom@Bat12-0Pk-adjusted",
        "Custom@Bat14-5Pk-adjusted",
        "Custom@Bat17-0Pk-adjusted"
    )
}

enum class CalibrationSequenceType(val maxFlow: Units.FlowUnit.L_S) {
    OLD_0_16(Units.FlowUnit.L_S(16.0)), NEW_0_17(Units.FlowUnit.L_S(17.0))
}
