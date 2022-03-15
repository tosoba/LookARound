package com.lookaround.ui.recent.searches

import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.lookaround.core.android.ext.addCollapseTopViewOnScrollListener
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.composable.*
import com.lookaround.core.android.view.recyclerview.LoadMoreRecyclerViewScrollListener
import com.lookaround.core.android.view.recyclerview.LocationRecyclerViewAdapterCallbacks
import com.lookaround.core.android.view.recyclerview.contrastingColorCallbacks
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.core.model.SearchType
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.recent.searches.databinding.FragmentRecentSearchesBinding
import com.lookaround.ui.recent.searches.model.RecentSearchModel
import com.lookaround.ui.recent.searches.model.RecentSearchesIntent
import com.lookaround.ui.recent.searches.model.RecentSearchesState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
@WithFragmentBindings
@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@FlowPreview
class RecentSearchesFragment : Fragment(R.layout.fragment_recent_searches) {
    private val binding: FragmentRecentSearchesBinding by
        viewBinding(FragmentRecentSearchesBinding::bind)

    private val recentSearchesViewModel: RecentSearchesViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private var searchQuery: String = ""
    private var searchFocused: Boolean = false

    private val recentSearchesRecyclerViewAdapter by
        lazy(LazyThreadSafetyMode.NONE) {
            RecentSearchesRecyclerViewAdapter(
                viewLifecycleOwner.lifecycleScope.contrastingColorCallbacks(
                    mainViewModel.filterSignals(MainSignal.ContrastingColorUpdated::color)
                ),
                object : LocationRecyclerViewAdapterCallbacks {
                    private val jobs = mutableMapOf<UUID, Job>()

                    override fun onBindViewHolder(
                        uuid: UUID,
                        action: (userLocation: Location) -> Unit
                    ) {
                        if (jobs.containsKey(uuid)) return
                        jobs[uuid] =
                            mainViewModel
                                .locationReadyUpdates
                                .onEach(action)
                                .launchIn(viewLifecycleOwner.lifecycleScope)
                    }

                    override fun onViewDetachedFromWindow(uuid: UUID) {
                        jobs.remove(uuid)?.cancel()
                    }

                    override fun onDetachedFromRecyclerView() {
                        jobs.values.forEach(Job::cancel)
                    }
                }
            ) { recentSearch ->
                lifecycleScope.launch {
                    mainViewModel.intent(
                        when (recentSearch.type) {
                            SearchType.AROUND -> MainIntent.LoadSearchAroundResults(recentSearch.id)
                            SearchType.AUTOCOMPLETE ->
                                MainIntent.LoadSearchAutocompleteResults(recentSearch.id)
                        }
                    )
                    mainViewModel.signal(MainSignal.HideBottomSheet)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) initFromSavedState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SavedStateKey.SEARCH_QUERY.name, searchQuery)
        outState.putBoolean(SavedStateKey.SEARCH_FOCUSED.name, searchFocused)
    }

    private fun initFromSavedState(savedInstanceState: Bundle) {
        with(savedInstanceState) {
            getString(SavedStateKey.SEARCH_QUERY.name)?.let(::searchQuery::set)
            searchFocused = getBoolean(SavedStateKey.SEARCH_FOCUSED.name)
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emptyRecentSearchesFlow =
            recentSearchesViewModel
                .states
                .map { it.searches.hasNoValueOrEmpty() }
                .distinctUntilChanged()

        binding.recentSearchesSearchBar.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val emptyRecentSearches =
                        emptyRecentSearchesFlow.collectAsState(initial = false)
                    if (emptyRecentSearches.value) {
                        LaunchedEffect(true) { mainViewModel.signal(MainSignal.HideBottomSheet) }
                    }

                    val scope = rememberCoroutineScope()
                    var searchFocusedState by remember { mutableStateOf(searchFocused) }
                    SearchBar(
                        query = searchQuery,
                        focused = searchFocusedState,
                        onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
                        onSearchFocusChange = {
                            searchFocusedState = it
                            searchFocused = it
                        },
                        onTextFieldValueChange = {
                            searchQuery = it.text
                            scope.launch {
                                recentSearchesViewModel.intent(
                                    RecentSearchesIntent.LoadSearches(it.text)
                                )
                            }
                        },
                        modifier = Modifier.onSizeChanged { addTopSpacer(it.height) }
                    )
                }
            }
        }

        binding.recentSearchesRecyclerView.adapter = recentSearchesRecyclerViewAdapter
        recentSearchesViewModel
            .mapStates(RecentSearchesState::searches)
            .filterIsInstance<WithValue<ParcelableList<RecentSearchModel>>>()
            .map(WithValue<ParcelableList<RecentSearchModel>>::value::get)
            .distinctUntilChanged()
            .onEach {
                val recentSearchItems = it.map(RecentSearchesRecyclerViewAdapter.Item::Search)
                recentSearchesRecyclerViewAdapter.updateItems(
                    if (recentSearchesRecyclerViewAdapter.items.firstOrNull() is
                            RecentSearchesRecyclerViewAdapter.Item.Spacer
                    ) {
                        listOf(recentSearchesRecyclerViewAdapter.items.first()) + recentSearchItems
                    } else {
                        recentSearchItems
                    }
                )
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
        val orientation = resources.configuration.orientation
        val spanCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 4 else 2
        binding.recentSearchesRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), spanCount, GridLayoutManager.VERTICAL, false)
                .apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int =
                                when (recentSearchesRecyclerViewAdapter.items[position]) {
                                    is RecentSearchesRecyclerViewAdapter.Item.Spacer -> spanCount
                                    else -> 1
                                }
                        }
                }
        binding.recentSearchesRecyclerView.addCollapseTopViewOnScrollListener(
            binding.recentSearchesSearchBar
        )
        binding.recentSearchesRecyclerView.addOnScrollListener(
            LoadMoreRecyclerViewScrollListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    recentSearchesViewModel.intent(RecentSearchesIntent.LoadSearches(searchQuery))
                }
            }
        )
    }

    private fun addTopSpacer(height: Int) {
        if (recentSearchesRecyclerViewAdapter.items.firstOrNull() is
                RecentSearchesRecyclerViewAdapter.Item.Spacer
        ) {
            binding.recentSearchesRecyclerView.visibility = View.VISIBLE
            return
        }
        val layoutManager = binding.recentSearchesRecyclerView.layoutManager as LinearLayoutManager
        val wasNotScrolled = layoutManager.findFirstCompletelyVisibleItemPosition() == 0
        recentSearchesRecyclerViewAdapter.addTopSpacer(
            RecentSearchesRecyclerViewAdapter.Item.Spacer(height)
        )
        binding.recentSearchesRecyclerView.apply {
            if (wasNotScrolled) scrollToTopAndShow() else visibility = View.VISIBLE
        }
    }

    private fun RecyclerView.scrollToTopAndShow() {
        post {
            scrollToPosition(0)
            visibility = View.VISIBLE
        }
    }

    private enum class SavedStateKey {
        SEARCH_QUERY,
        SEARCH_FOCUSED
    }
}
