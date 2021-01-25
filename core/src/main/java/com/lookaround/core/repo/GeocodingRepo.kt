package com.lookaround.core.repo

import com.lookaround.core.model.AddressDTO

interface GeocodingRepo {
    fun getAddress(lat: Double, lng: Double): AddressDTO
}
