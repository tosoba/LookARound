package com.lookaround.repo.photon

import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.get
import com.lookaround.core.model.PointDTO
import com.lookaround.core.repo.IPlacesAutocompleteRepo
import com.lookaround.repo.photon.entity.AutocompleteSearchInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotonRepo
@Inject
constructor(
    private val store: Store<AutocompleteSearchInput, List<PointDTO>>,
) : IPlacesAutocompleteRepo {
    override suspend fun searchPoints(
        query: String,
        priorityLat: Double?,
        priorityLon: Double?
    ): List<PointDTO> =
        store.get(
            AutocompleteSearchInput(
                query = query,
                priorityLat = priorityLat,
                priorityLon = priorityLon
            )
        )
}
