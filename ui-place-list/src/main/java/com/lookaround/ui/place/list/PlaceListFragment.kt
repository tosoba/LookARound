package com.lookaround.ui.place.list

import alirezat775.lib.carouselview.Carousel
import alirezat775.lib.carouselview.CarouselListener
import alirezat775.lib.carouselview.CarouselView
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.recyclerview.locationRecyclerViewAdapterCallbacks
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.place.list.databinding.FragmentPlaceListBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceListFragment : Fragment(R.layout.fragment_place_list) {
    private val binding: FragmentPlaceListBinding by viewBinding(FragmentPlaceListBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()

    private val carousel: Carousel<PlacesRecyclerViewAdapter.ViewHolder> by
        lazy(LazyThreadSafetyMode.NONE) {
            Carousel(requireContext(), binding.placeListRecyclerView, placesRecyclerViewAdapter)
        }

    private val placesRecyclerViewAdapter: PlacesRecyclerViewAdapter by
        lazy(LazyThreadSafetyMode.NONE) {
            PlacesRecyclerViewAdapter(
                userLocationCallbacks =
                    viewLifecycleOwner.lifecycleScope.locationRecyclerViewAdapterCallbacks(
                        mainViewModel.locationReadyUpdates
                    )
            ) { position, marker ->
//                if (carousel.getCurrentPosition() != position) return@PlacesRecyclerViewAdapter
                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    mainViewModel.signal(MainSignal.CaptureMapImage(marker))
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel
            .mapStates(MainState::markers)
            .filterIsInstance<WithValue<ParcelableSortedSet<Marker>>>()
            .distinctUntilChanged()
            .map { it.value.toList() }
            .onEach(placesRecyclerViewAdapter::updateItems)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        carousel.apply {
            setOrientation(CarouselView.HORIZONTAL, false)
            scaleView(true)
            addCarouselListener(
                object : CarouselListener {
                    override fun onPositionChange(position: Int) {
                        placesRecyclerViewAdapter.items.elementAtOrNull(position)?.let { marker ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                mainViewModel.signal(MainSignal.UpdateSelectedMarker(marker))
                            }
                        }
                    }

                    override fun onScroll(dx: Int, dy: Int) = Unit
                }
            )
        }
    }

    fun scrollToPlace(uuid: UUID) {
        placesRecyclerViewAdapter
            .items
            .indexOfFirst { it.id == uuid }
            .takeUnless { it == -1 }
            ?.let(::scrollToPosition)
    }

    private fun scrollToPosition(position: Int) {
        carousel.setCurrentPosition(position)
    }
}
