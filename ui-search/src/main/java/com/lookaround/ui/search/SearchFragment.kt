package com.lookaround.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
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
import kotlinx.coroutines.flow.*

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
            .onEach { query -> searchViewModel.intent(SearchIntent.QueryChanged(query)) }
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
                        val points =
                            searchViewModel
                                .states
                                .map { it.points }
                                .distinctUntilChanged()
                                .collectAsState(Empty)
                                .value
                        when (points) {
                            is Empty -> Text("Search for places nearby.")
                            is LoadingInProgress -> CircularProgressIndicator()
                            is Ready -> {
                                val items = points.value.items
                                if (items.isEmpty()) Text("No places found.")
                                else SearchResults(items)
                            }
                            is Failed -> {
                                when (points.error) {
                                    is QueryTooShortExcecption -> Text("Search query is too short.")
                                    is PlacesLoadingException ->
                                        // TODO: retry button?
                                        Text("Places loading error occurred.")
                                }
                            }
                        }
                    }
                }
            }
        }
}
