package com.lookaround.ui.place

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.argument
import com.lookaround.core.android.model.Marker
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.place.databinding.FragmentPlaceBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class PlaceFragment : Fragment(R.layout.fragment_place) {
    private val binding: FragmentPlaceBinding by viewBinding(FragmentPlaceBinding::bind)

    private val markerArgument: Marker by argument(Arguments.MARKER.name)
    private val markerImageArgument: Bitmap by argument(Arguments.MARKER_IMAGE.name)

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.placeMapImageView.setImageBitmap(markerImageArgument)
    }

    companion object {
        enum class Arguments {
            MARKER,
            MARKER_IMAGE
        }

        fun new(marker: Marker, markerImage: Bitmap): PlaceFragment =
            PlaceFragment().apply {
                arguments =
                    bundleOf(
                        Arguments.MARKER.name to marker,
                        Arguments.MARKER_IMAGE.name to markerImage
                    )
            }
    }
}
