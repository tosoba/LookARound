package com.lookaround.core.android.model

enum class Range(val meters: Double, val label: String) {
    _100_M(100.0, "100 m"),
    _250_M(250.0, "250 m"),
    _500_M(500.0, "500 m"),
    _1_KM(1_000.0, "1 km"),
    _2_KM(2_000.0, "2 km"),
    _5_KM(5_000.0, "5 km"),
    _10_KM(10_000.0, "10 km");

    companion object {
        val DEFAULT_METERS: Double
            get() = _1_KM.meters

        fun metersFrom(ordinal: Int): Double {
            require(ordinal in values().indices)
            return values()[ordinal].meters
        }

        fun labelFrom(ordinal: Int): String {
            require(ordinal in values().indices)
            return values()[ordinal].label
        }
    }
}
