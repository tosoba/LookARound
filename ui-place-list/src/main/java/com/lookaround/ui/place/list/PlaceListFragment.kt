package com.lookaround.ui.place.list

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.model.Marker
import com.lookaround.core.android.model.ParcelableSortedSet
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.android.view.recyclerview.BoundsOffsetDecoration
import com.lookaround.core.android.view.recyclerview.LinearHorizontalSpacingDecoration
import com.lookaround.core.android.view.recyclerview.ProminentLayoutManager
import com.lookaround.core.android.view.recyclerview.locationRecyclerViewAdapterCallbacks
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.place.list.databinding.FragmentPlaceListBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class PlaceListFragment : Fragment(R.layout.fragment_place_list) {
    private val binding: FragmentPlaceListBinding by viewBinding(FragmentPlaceListBinding::bind)

    private val mainViewModel: MainViewModel by activityViewModels()

    private val placesRecyclerViewAdapter by
        lazy(LazyThreadSafetyMode.NONE) {
            PlacesRecyclerViewAdapter(
                userLocationCallbacks =
                    viewLifecycleOwner.lifecycleScope.locationRecyclerViewAdapterCallbacks(
                        mainViewModel.locationReadyUpdates
                    )
            ) { marker -> }
        }

    private val snapHelper by lazy(LazyThreadSafetyMode.NONE, ::PagerSnapHelper)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(binding.placeListRecyclerView) {
            setItemViewCacheSize(4)
            layoutManager = ProminentLayoutManager(requireContext())
            adapter = placesRecyclerViewAdapter

            val spacing = resources.getDimensionPixelSize(R.dimen.carousel_spacing)
            addItemDecoration(LinearHorizontalSpacingDecoration(spacing))
            addItemDecoration(BoundsOffsetDecoration())

            snapHelper.attachToRecyclerView(this)

            mainViewModel
                .mapStates(MainState::markers)
                .filterIsInstance<WithValue<ParcelableSortedSet<Marker>>>()
                .distinctUntilChanged()
                .map { it.value.toList() }
                .onEach(placesRecyclerViewAdapter::updateItems)
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }
    }
}
