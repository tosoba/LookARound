package com.lookaround.ui.place.types

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.model.Amenity
import com.lookaround.ui.place.types.model.PlaceType
import com.lookaround.ui.place.types.model.PlaceTypeGroup
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets

class PlaceTypesFragment : Fragment() {
    private val placeTypeGroups =
        listOf(
            PlaceTypeGroup(
                name = "General",
                placeTypes =
                    listOf(
                        PlaceType(
                            wrapped = Amenity.PARKING,
                            imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                        ),
                        PlaceType(
                            wrapped = Amenity.RESTAURANT,
                            imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                        ),
                        PlaceType(
                            wrapped = Amenity.FUEL,
                            imageUrl = "https://source.unsplash.com/_jk8KIyN_uA"
                        ),
                        PlaceType(
                            wrapped = Amenity.BANK,
                            imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                        )
                    )
            ),
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent { ProvideWindowInsets { LookARoundTheme { PlaceTypes(placeTypeGroups) } } }
        }
}
