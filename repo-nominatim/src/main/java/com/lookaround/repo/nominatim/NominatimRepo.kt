package com.lookaround.repo.nominatim

import com.lookaround.core.model.AddressDTO
import com.lookaround.core.repo.GeocodingRepo
import com.lookaround.repo.nominatim.mapper.AddressMapper
import fr.dudie.nominatim.client.JsonNominatimClient
import javax.inject.Inject

class NominatimRepo @Inject constructor(
    private val nominatimClient: JsonNominatimClient,
    private val addressMapper: AddressMapper,
) : GeocodingRepo {
    override fun getAddress(lat: Double, lng: Double): AddressDTO = addressMapper
        .toDTO(nominatimClient.getAddress(lng, lat))
}
