package com.lookaround.core.android.ext

import java.math.BigDecimal
import java.math.RoundingMode

fun <T : Number> T.roundToDecimalPlaces(places: Int): BigDecimal {
    var rounded = BigDecimal(toString())
    rounded = rounded.setScale(places, RoundingMode.HALF_UP)
    return rounded
}
