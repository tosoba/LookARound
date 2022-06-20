package com.lookaround.ui.place.list

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.darkMode
import com.lookaround.core.android.ext.preciseFormattedDistanceTo
import com.lookaround.core.android.ext.setListBackgroundItemDrawableWith
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.recyclerview.LocationRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.locationRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.theme.Neutral7
import com.lookaround.core.android.view.theme.Neutral8
import com.lookaround.core.ext.getFieldValueByName
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.place.list.databinding.FragmentPlaceListBinding
import com.lookaround.ui.place.list.databinding.PlaceListItemBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.imaginativeworld.whynotimagecarousel.ImageCarousel
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.listener.CarouselOnScrollListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceListFragment : Fragment(R.layout.fragment_place_list) {
    private val binding: FragmentPlaceListBinding by viewBinding(FragmentPlaceListBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()

    private val userLocationCallbacks: LocationRecyclerViewAdapterCallbacks<UUID> by
        lazy(LazyThreadSafetyMode.NONE) {
            viewLifecycleOwner.lifecycleScope.locationRecyclerViewAdapterCallbacks(
                mainViewModel.locationReadyUpdates
            )
        }

    private var carouselLastPosition: Int = 0

    private val carouselRecyclerView: RecyclerView?
        get() = binding.placeListRecyclerView.getFieldValueByName("recyclerView")

    private val carouselSnapHelper: LinearSnapHelper?
        get() = binding.placeListRecyclerView.getFieldValueByName("snapHelper")

    private val carouselListener by
        lazy(LazyThreadSafetyMode.NONE) {
            object : CarouselListener {
                override fun onCreateViewHolder(
                    layoutInflater: LayoutInflater,
                    parent: ViewGroup
                ): ViewBinding =
                    PlaceListItemBinding.inflate(layoutInflater, parent, false).apply {
                        if (!darkMode) {
                            placeNameText.setTextColor(Neutral8.toArgb())
                            placeDistanceText.setTextColor(Neutral7.toArgb())
                        }
                    }

                override fun onBindViewHolder(
                    binding: ViewBinding,
                    item: CarouselItem,
                    position: Int
                ) {
                    val marker = items[position]
                    with(binding as PlaceListItemBinding) {
                        root.setListBackgroundItemDrawableWith(
                            contrastingColor =
                                if (darkMode) {
                                    binding.root.context.getColor(R.color.dark_gray_background)
                                } else {
                                    Color.WHITE
                                },
                            alpha = 0xff
                        )
                        placeNameText.text = marker.name
                        userLocationCallbacks.onBindViewHolder(marker.id) { userLocation ->
                            placeDistanceText.text =
                                userLocation.preciseFormattedDistanceTo(marker.location)
                        }
                        root.setOnClickListener {
                            if (carouselLastPosition != position) return@setOnClickListener

                            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                                mainViewModel.signal(MainSignal.ShowPlaceFragment(marker))
                            }
                        }
                    }
                }
            }
        }

    private val carouselOnScrollListener by
        lazy(LazyThreadSafetyMode.NONE) {
            var firstScroll = true
            object : CarouselOnScrollListener {
                override fun onScrollStateChanged(
                    recyclerView: RecyclerView,
                    newState: Int,
                    position: Int,
                    carouselItem: CarouselItem?
                ) {
                    if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                    if (firstScroll) {
                        firstScroll = false
                        return
                    }
                    val marker = items.elementAtOrNull(position) ?: return

                    carouselLastPosition = position
                    viewLifecycleOwner.lifecycleScope.launch {
                        mainViewModel.signal(MainSignal.UpdateSelectedMarker(marker))
                    }
                }

                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                    position: Int,
                    carouselItem: CarouselItem?
                ) = Unit
            }
        }

    private var items: List<Marker> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState
            ?.getInt(SavedStateKey.LAST_CAROUSEL_POSITION.name)
            ?.let(::carouselLastPosition::set)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.placeListRecyclerView.carouselListener = carouselListener
        binding.placeListRecyclerView.onScrollListener = carouselOnScrollListener

        var scrollPositionShouldBeRestored =
            savedInstanceState?.containsKey(SavedStateKey.LAST_CAROUSEL_POSITION.name) ?: false
        mainViewModel
            .mapStates(MainState::markers)
            .distinctUntilChanged()
            .map { if (it is WithValue) it.value.toList() else emptyList() }
            .onEach {
                items = it
                binding.placeListRecyclerView.setData(items.map { CAROUSEL_ITEM_DUMMY })
                if (scrollPositionShouldBeRestored) {
                    scrollPositionShouldBeRestored = false
                    binding.placeListRecyclerView.restoreCarouselPosition()
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SavedStateKey.LAST_CAROUSEL_POSITION.name, carouselLastPosition)
    }

    override fun onDestroyView() {
        userLocationCallbacks.onDetachedFromRecyclerView()
        super.onDestroyView()
    }

    fun scrollToPlace(uuid: UUID) {
        items.indexOfFirst { it.id == uuid }.takeUnless { it == -1 }?.let(::scrollToPosition)
    }

    private fun scrollToPosition(position: Int) {
        carouselLastPosition = position
        with(binding.placeListRecyclerView) {
            postDelayed(
                {
                    smoothScrollEnabled = false
                    currentPosition = position
                    smoothScrollEnabled = true
                    currentPosition = position
                },
                500L
            )
        }
    }

    private fun ImageCarousel.restoreCarouselPosition() {
        postDelayed(
            {
                currentPosition = carouselLastPosition
                carouselRecyclerView?.snapToPosition(carouselLastPosition)
            },
            500L
        )
    }

    private fun RecyclerView.snapToPosition(position: Int) {
        val layoutManager = layoutManager as? LinearLayoutManager ?: return
        val targetView = layoutManager.findViewByPosition(position) ?: return
        val distanceToSnapHorizontal =
            carouselSnapHelper
                ?.calculateDistanceToFinalSnap(layoutManager, targetView)
                ?.firstOrNull()
                ?: return
        scrollBy(distanceToSnapHorizontal, 0)
        return
    }

    private enum class SavedStateKey {
        LAST_CAROUSEL_POSITION
    }

    companion object {
        private val CAROUSEL_ITEM_DUMMY = CarouselItem()
    }
}
