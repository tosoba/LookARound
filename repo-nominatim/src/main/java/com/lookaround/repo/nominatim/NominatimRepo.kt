package com.lookaround.repo.nominatim

import com.lookaround.core.model.AddressDTO
import com.lookaround.core.repo.IGeocodingRepo
import com.lookaround.repo.nominatim.mapper.AddressMapper
import fr.dudie.nominatim.client.JsonNominatimClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NominatimRepo
@Inject
constructor(
    private val nominatimClient: JsonNominatimClient,
    private val addressMapper: AddressMapper,
) : IGeocodingRepo {
    override fun getAddress(lat: Double, lng: Double): AddressDTO =
        addressMapper.toDTO(nominatimClient.getAddress(lng, lat))
}
