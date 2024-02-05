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

val v5 by lazy {
    listOf(
        Pair(0.10, 0.1),
        Pair(0.20, 0.1),
        Pair(0.40, 0.2),
        Pair(0.60, 0.3),
        Pair(0.80, 0.4),
        Pair(1.00, 0.5),
        Pair(1.20, 0.6),
        Pair(1.40, 0.7),
        Pair(1.60, 0.8),
        Pair(1.80, 0.9),
        Pair(2.00, 1.0),
        Pair(2.20, 1.1),
        Pair(2.40, 1.2),
        Pair(2.60, 1.3),
        Pair(2.80, 1.4),
        Pair(3.00, 1.5),
        Pair(3.20, 1.6),
        Pair(3.40, 1.7),
        Pair(3.60, 1.8),
        Pair(3.80, 1.9),
        Pair(4.00, 2.0),
        Pair(4.20, 2.1),
        Pair(4.40, 2.2),
        Pair(4.60, 2.3),
        Pair(4.80, 2.4),
        Pair(5.00, 2.5),
        Pair(5.20, 2.6),
        Pair(5.40, 2.7),
        Pair(5.60, 2.8),
        Pair(5.80, 2.9),
        Pair(6.00, 3.0),
        Pair(6.20, 3.1),
        Pair(6.40, 3.2),
        Pair(6.60, 3.3),
        Pair(6.80, 3.4),
        Pair(7.00, 3.5),
        Pair(7.20, 3.6),
        Pair(7.40, 3.7),
        Pair(7.60, 3.8),
        Pair(7.80, 3.9),
        Pair(8.00, 4.0),
        Pair(8.20, 4.1),
        Pair(8.40, 4.2),
        Pair(8.60, 4.3),
        Pair(8.80, 4.4),
        Pair(9.00, 4.5),
        Pair(9.20, 4.6),
        Pair(9.40, 4.7),
        Pair(9.60, 4.8),
        Pair(9.80, 4.9),
        Pair(10.00, 5.0),
        Pair(10.20, 5.1),
        Pair(10.40, 5.2),
        Pair(10.60, 5.3),
        Pair(10.80, 5.4),
        Pair(11.00, 5.5),
        Pair(11.20, 5.6),
        Pair(11.40, 5.7),
        Pair(11.60, 5.8),
        Pair(11.80, 5.9),
        Pair(12.00, 6.0),
        Pair(12.20, 6.1),
        Pair(12.40, 6.2),
        Pair(12.60, 6.3),
        Pair(12.80, 6.4),
        Pair(13.00, 6.5),
        Pair(13.20, 6.6),
        Pair(13.40, 6.7),
        Pair(13.60, 6.8),
        Pair(13.80, 6.9),
        Pair(14.00, 7.0),
        Pair(14.20, 7.1),
        Pair(14.40, 7.2),
        Pair(14.60, 7.3),
        Pair(14.80, 7.4),
        Pair(15.00, 7.5),
        Pair(15.20, 7.6),
        Pair(15.40, 7.7),
        Pair(15.60, 7.8),
        Pair(15.80, 7.9),
        Pair(16.00, 8.0)
    )
}

enum class CalibrationSequenceType(val maxFlow: Units.FlowUnit.L_S) {
    OLD_0_16(Units.FlowUnit.L_S(16.0)), NEW_0_17(Units.FlowUnit.L_S(17.0))
}
