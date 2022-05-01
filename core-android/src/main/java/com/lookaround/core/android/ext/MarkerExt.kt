package com.lookaround.core.android.ext

import com.lookaround.core.android.model.Marker

val Marker.address: String?
    get() =
        tags["addr:street"]?.let { street ->
            val address = StringBuilder(street)
            tags["addr:housenumber"]?.let { address.append(" ").append(it) }
            address.toString()
        }

val Marker.hasContacts: Boolean
    get() =
        tags.containsKey("phone") ||
            tags.containsKey("contact:phone") ||
            tags.containsKey("contact:mobile") ||
            tags.containsKey("email") ||
            tags.containsKey("contact:email") ||
            tags.containsKey("website") ||
            tags.containsKey("contact:website") ||
            tags.containsKey("contact:facebook") ||
            tags.containsKey("contact:instagram")

val Marker.contactPhone: String?
    get() = tags["contact:phone"] ?: tags["phone"] ?: tags["contact:mobile"]

val Marker.contactWebsite: String?
    get() = tags["website"] ?: tags["contact:website"]

val Marker.contactEmail: String?
    get() = tags["email"] ?: tags["contact:email"]
