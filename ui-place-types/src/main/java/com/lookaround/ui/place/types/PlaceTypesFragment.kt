package com.lookaround.ui.place.types

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.lookaround.core.android.view.theme.LookARoundTheme
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets

class PlaceTypesFragment : Fragment() {
    private val searchCategoryCollections =
        listOf(
            SearchCategoryCollection(
                id = 0L,
                name = "Categories",
                categories =
                    listOf(
                        SearchCategory(
                            name = "Chips & crackers",
                            imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                        ),
                        SearchCategory(
                            name = "Fruit snacks",
                            imageUrl = "https://source.unsplash.com/SfP1PtM9Qa8"
                        ),
                        SearchCategory(
                            name = "Desserts",
                            imageUrl = "https://source.unsplash.com/_jk8KIyN_uA"
                        ),
                        SearchCategory(
                            name = "Nuts ",
                            imageUrl = "https://source.unsplash.com/UsSdMZ78Q3E"
                        )
                    )
            ),
            SearchCategoryCollection(
                id = 1L,
                name = "Lifestyles",
                categories =
                    listOf(
                        SearchCategory(
                            name = "Organic",
                            imageUrl = "https://source.unsplash.com/7meCnGCJ5Ms"
                        ),
                        SearchCategory(
                            name = "Gluten Free",
                            imageUrl = "https://source.unsplash.com/m741tj4Cz7M"
                        ),
                        SearchCategory(
                            name = "Paleo",
                            imageUrl = "https://source.unsplash.com/dt5-8tThZKg"
                        ),
                        SearchCategory(
                            name = "Vegan",
                            imageUrl = "https://source.unsplash.com/ReXxkS1m1H0"
                        ),
                        SearchCategory(
                            name = "Vegitarian",
                            imageUrl = "https://source.unsplash.com/IGfIGP5ONV0"
                        ),
                        SearchCategory(
                            name = "Whole30",
                            imageUrl = "https://source.unsplash.com/9MzCd76xLGk"
                        )
                    )
            )
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                ProvideWindowInsets {
                    LookARoundTheme { SearchCategories(searchCategoryCollections) }
                }
            }
        }
}
