package com.lookaround.ui.search.model

import com.lookaround.core.android.base.arch.StateUpdate
import com.lookaround.core.android.model.ParcelableList
import com.lookaround.core.android.model.Point
import com.lookaround.core.android.model.Ready
import com.lookaround.core.android.model.WithValue
import com.lookaround.core.model.PointDTO
import com.lookaround.ui.search.exception.BlankQueryException
import com.lookaround.ui.search.exception.PlacesLoadingException
import com.lookaround.ui.search.exception.QueryTooShortExcecption

sealed class SearchStateUpdate : StateUpdate<SearchState> {
    object LoadingPlaces : SearchStateUpdate() {
        override fun invoke(state: SearchState): SearchState =
            state.copy(points = state.points.copyWithLoadingInProgress)
    }

    data class PlacesLoadingError(private val throwable: Throwable) : SearchStateUpdate() {
        override fun invoke(state: SearchState): SearchState =
            state.copyWithPointsException(PlacesLoadingException(throwable))
    }

    data class PlacesLoaded(val points: List<PointDTO>) : SearchStateUpdate() {
        override fun invoke(state: SearchState): SearchState =
            state.copy(
                points =
                    if (state.points is WithValue) Ready(state.points.value + points.map(::Point))
                    else Ready(ParcelableList(points.map(::Point)))
            )
    }

    object BlankQueryUpdate : SearchStateUpdate() {
        override fun invoke(state: SearchState): SearchState =
            state.copyWithPointsException(BlankQueryException)
    }

    object QueryTooShortUpdate : SearchStateUpdate() {
        override fun invoke(state: SearchState): SearchState =
            state.copyWithPointsException(QueryTooShortExcecption)
    }

    protected fun SearchState.copyWithPointsException(throwable: Throwable): SearchState =
        copy(points = points.copyWithError(throwable))
}
