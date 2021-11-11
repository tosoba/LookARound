package com.lookaround.core.android.ext

import android.util.Size

operator fun Size.div(divisor: Int): Size = Size(width / divisor, height / divisor)
