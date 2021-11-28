package com.lookaround.repo.nominatim

import com.lookaround.core.model.AddressDTO
import com.lookaround.core.repo.IGeocodingRepo
import com.lookaround.repo.nominatim.mapper.AddressMapper
import dagger.Reusable
import fr.dudie.nominatim.client.JsonNominatimClient
import javax.inject.Inject

@Reusable
class NominatimRepo
@Inject
constructor(
    private val nominatimClient: JsonNominatimClient,
    private val addressMapper: AddressMapper,
) : IGeocodingRepo {
    override fun getAddress(lat: Double, lng: Double): AddressDTO =
        addressMapper.toDTO(nominatimClient.getAddress(lng, lat))
}
