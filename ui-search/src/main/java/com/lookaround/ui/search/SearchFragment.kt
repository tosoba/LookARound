package com.lookaround.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lookaround.core.android.ext.assistedActivityViewModel
import com.lookaround.core.android.ext.assistedViewModel
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.theme.LookARoundTheme
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.search.composable.SearchResults
import com.lookaround.ui.search.exception.PlacesLoadingException
import com.lookaround.ui.search.exception.QueryTooShortExcecption
import com.lookaround.ui.search.model.SearchIntent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class SearchFragment : Fragment() {
    @Inject internal lateinit var searchViewModelFactory: SearchViewModel.Factory
    private val searchViewModel: SearchViewModel by assistedViewModel {
        searchViewModelFactory.create(it)
    }

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel
            .searchQueryUpdates
            .run { if (savedInstanceState != null) drop(1) else this }
            .onEach { query ->
                val (_, locationState) = mainViewModel.state
                searchViewModel.intent(
                    SearchIntent.SearchPlaces(
                        query = query,
                        priorityLocation =
                            if (locationState is WithValue) locationState.value else null
                    )
                )
            }
            .launchIn(lifecycleScope)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ComposeView(requireContext()).apply {
            setContent {
                ProvideWindowInsets {
                    LookARoundTheme {
                        val (points, lastPerformedWithLocationPriority) =
                            searchViewModel.states.collectAsState().value
                        val paddingHorizontalModifier = Modifier.padding(horizontal = 10.dp)
                        val paddingTopModifier = Modifier.padding(top = 66.dp)
                        when (points) {
                            is Empty -> {
                                Text(
                                    "Search for places nearby.",
                                    paddingTopModifier.then(paddingHorizontalModifier)
                                )
                            }
                            is LoadingInProgress -> {
                                CircularProgressIndicator(paddingTopModifier.wrapContentSize())
                            }
                            is Ready -> {
                                PointsReady(
                                    points,
                                    lastPerformedWithLocationPriority,
                                )
                            }
                            is Failed -> PointsFailed(points)
                        }
                    }
                }
            }
        }

    @Composable
    private fun PointsReady(
        points: Ready<ParcelableList<Point>>,
        lastPerformedWithLocationPriority: Boolean,
    ) {
        val items = points.value.items
        val paddingHorizontalModifier = Modifier.padding(horizontal = 10.dp)
        val paddingTopModifier = Modifier.padding(top = 66.dp)
        when {
            items.isEmpty() ->
                Text("No places found.", paddingTopModifier.then(paddingHorizontalModifier))
            !lastPerformedWithLocationPriority ->
                Column(paddingTopModifier) {
                    Text(
                        "WARNING - Search performed with no location priority.",
                        paddingHorizontalModifier
                    )
                    // TODO: retry button when location retrieved?
                    SearchResults(items)
                }
            else -> SearchResults(items, paddingTopModifier)
        }
    }

    @Composable
    private fun PointsFailed(points: Failed) {
        val modifier = Modifier.padding(top = 66.dp).then(Modifier.padding(horizontal = 10.dp))
        when (points.error) {
            is QueryTooShortExcecption -> Text("Search query is too short.", modifier)
            is PlacesLoadingException -> {
                // TODO: retry button?
                Text("Places loading error occurred.", modifier)
            }
        }
    }
}
