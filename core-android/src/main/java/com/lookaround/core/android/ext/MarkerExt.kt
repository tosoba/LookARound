package com.lookaround.core.android.ext

import com.lookaround.core.android.model.Marker

val Marker.address: String?
    get() =
        tags["addr:street"]?.let { street ->
            val address = StringBuilder(street)
            tags["addr:housenumber"]?.let { address.append(" ").append(it) }
            address.toString()
        }
