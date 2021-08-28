package com.lookaround.repo.overpass

import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.get
import com.lookaround.core.model.IPlaceType
import com.lookaround.core.model.NodeDTO
import com.lookaround.core.repo.IPlacesRepo
import com.lookaround.repo.overpass.entity.SearchAroundInput
import nice.fontaine.overpass.models.query.settings.Filter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverpassRepo
@Inject
constructor(
    private val store: Store<SearchAroundInput, List<NodeDTO>>,
) : IPlacesRepo {
    override suspend fun attractionsAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> =
        store.get(
            SearchAroundInput(
                lat = lat,
                lng = lng,
                radiusInMeters = radiusInMeters,
                key = "tourism",
                value = "attraction",
                filter = Filter.EQUAL
            )
        )

    override suspend fun placesOfTypeAround(
        placeType: IPlaceType,
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<NodeDTO> =
        store.get(
            SearchAroundInput(
                lat = lat,
                lng = lng,
                radiusInMeters = radiusInMeters,
                key = placeType.typeKey,
                value = placeType.typeValue,
                filter = Filter.EQUAL
            )
        )

    override suspend fun imagesAround(
        lat: Double,
        lng: Double,
        radiusInMeters: Float
    ): List<String> =
        store.get(
                SearchAroundInput(
                    lat = lat,
                    lng = lng,
                    radiusInMeters = radiusInMeters,
                    key = "image",
                    value = "http",
                    filter = Filter.ILIKE
                ) { filterNot { it.tags?.get("image") == null } }
            )
            .mapNotNull { it.tags["image"] }
}
