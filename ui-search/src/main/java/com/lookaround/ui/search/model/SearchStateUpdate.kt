package com.lookaround.ui.search.model

import com.lookaround.core.android.model.*
import com.lookaround.core.model.PointDTO
import com.lookaround.ui.search.exception.PlacesLoadingException
import com.lookaround.ui.search.exception.QueryTooShortExcecption

object LoadingPlaces : (SearchState) -> SearchState {
    override fun invoke(state: SearchState): SearchState =
        state.copy(points = state.points.copyWithLoadingInProgress)
}

data class PlacesLoadingError(private val throwable: Throwable) : (SearchState) -> SearchState {
    override fun invoke(state: SearchState): SearchState =
        state.copyWithPointsException(PlacesLoadingException(throwable))
}

data class PlacesLoaded(
    val points: List<PointDTO>,
    val withLocationPriority: Boolean,
) : (SearchState) -> SearchState {
    override fun invoke(state: SearchState): SearchState =
        state.copy(
            points =
                if (state.points is WithValue) Ready(state.points.value + points.map(::Point))
                else Ready(ParcelableList(points.map(::Point))),
            lastPerformedWithLocationPriority = withLocationPriority
        )
}

object BlankQueryUpdate : (SearchState) -> SearchState {
    override fun invoke(state: SearchState): SearchState = state.copy(points = Empty)
}

object QueryTooShortUpdate : (SearchState) -> SearchState {
    override fun invoke(state: SearchState): SearchState =
        state.copyWithPointsException(QueryTooShortExcecption)
}
