package com.lookaround.ui.recent.searches

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.accompanist.insets.ProvideWindowInsets
import com.lookaround.core.android.ext.addCollapseTopViewOnScrollListener
import com.lookaround.core.android.ext.scrollToTopAndShow
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.composable.*
import com.lookaround.core.android.view.recyclerview.LoadMoreRecyclerViewScrollListener
import com.lookaround.core.android.view.recyclerview.locationRecyclerViewAdapterCallbacks
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
                userLocationCallbacks =
                    viewLifecycleOwner.lifecycleScope.locationRecyclerViewAdapterCallbacks(
                        mainViewModel.locationReadyUpdates
                    ),
                onItemLongClicked = { (id, label, type) ->
                    AlertDialog.Builder(requireContext())
                        .setTitle(label)
                        .setMessage(getString(R.string.remove_from_recent_searches))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.remove)) { _, _ ->
                            lifecycleScope.launch {
                                recentSearchesViewModel.intent(
                                    RecentSearchesIntent.DeleteSearch(id, type)
                                )
                            }
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            ) { (id, _, type) ->
                lifecycleScope.launch {
                    mainViewModel.intent(
                        when (type) {
                            SearchType.AROUND -> MainIntent.LoadSearchAroundResults(id)
                            SearchType.AUTOCOMPLETE -> MainIntent.LoadSearchAutocompleteResults(id)
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
            recentSearchesViewModel.states
                .map { it.searches.hasNoValueOrEmpty() }
                .distinctUntilChanged()

        binding.recentSearchesSearchBar.setContent {
            ProvideWindowInsets {
                LookARoundTheme {
                    val scope = rememberCoroutineScope()

                    val topSpacerHeightPx = remember { mutableStateOf(0) }
                    remember {
                        snapshotFlow(topSpacerHeightPx::value)
                            .drop(1)
                            .distinctUntilChanged()
                            .debounce(500L)
                            .onEach(::addTopSpacer)
                            .launchIn(scope)
                    }

                    val emptyRecentSearches =
                        emptyRecentSearchesFlow.collectAsState(initial = false)
                    if (emptyRecentSearches.value) {
                        LaunchedEffect(true) { mainViewModel.signal(MainSignal.HideBottomSheet) }
                    }

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
                        leadingUnfocused = {
                            IconButton(onClick = { requireActivity().onBackPressed() }) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    tint = LookARoundTheme.colors.iconPrimary,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        },
                        modifier = Modifier.onSizeChanged { topSpacerHeightPx.value = it.height }
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
                    if (recentSearchesRecyclerViewAdapter.items.firstOrNull()
                            is RecentSearchesRecyclerViewAdapter.Item.Spacer
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
        if (recentSearchesRecyclerViewAdapter.items.firstOrNull()
                is RecentSearchesRecyclerViewAdapter.Item.Spacer
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

    private enum class SavedStateKey {
        SEARCH_QUERY,
        SEARCH_FOCUSED
    }
}
