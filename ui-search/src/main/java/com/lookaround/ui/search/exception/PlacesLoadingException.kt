package com.lookaround.ui.search.exception

class PlacesLoadingException(override val cause: Throwable) :
    Throwable("Places loading error occurred.")
