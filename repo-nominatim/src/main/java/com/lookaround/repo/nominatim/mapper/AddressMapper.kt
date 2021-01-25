package com.lookaround.repo.nominatim.mapper

import com.lookaround.core.model.AddressDTO
import fr.dudie.nominatim.model.Address
import fr.dudie.nominatim.model.Element
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

@Mapper(componentModel = "jsr330")
interface AddressMapper {
    @Mappings(
        value = [
            Mapping(source = "latitude", target = "lat"),
            Mapping(source = "longitude", target = "lng"),
            Mapping(
                source = "addressElements",
                target = "elements",
                qualifiedBy = [ElementsArrayToMap::class]
            ),
            Mapping(
                source = "nameDetails",
                target = "details",
                qualifiedBy = [ElementsArrayToMap::class]
            ),
        ]
    )
    fun toDTO(node: Address): AddressDTO

    companion object {
        @ElementsArrayToMap
        @JvmStatic
        fun elementsArrayToMap(elements: Array<Element>?): Map<String, String> = elements
            ?.map { it.key to it.value }
            ?.toMap()
            ?: emptyMap()
    }
}