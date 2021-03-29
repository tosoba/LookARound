package com.lookaround.ui.place.list

import android.graphics.Bitmap
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lookaround.core.android.ext.captureFrame
import com.lookaround.core.android.ext.init
import com.mapzen.tangram.*
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLSurfaceViewHolderFactory
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun LocationBitmapLoadingMapView(
    // TODO: maybe send it markers in lists of 3 to 5 and call conflate on locationFlow
    // so loading frames can be cancelled when user swipes the list hard and starts loading multiple
    // places at once
    // https://stackoverflow.com/questions/66712286/get-last-visible-item-index-in-jetpack-compose-lazycolumn
    // check if state.layoutInfo.visibleItemsInfo or smth similar can be used to retrieve visible
    // items' locations
    locationsFlow: Flow<Location>,
    bitmapsFlow: MutableStateFlow<Bitmap>,
    httpHandler: HttpHandler? = null,
    glViewHolderFactory: GLViewHolderFactory = GLSurfaceViewHolderFactory()
): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val coroutineScope = rememberCoroutineScope()
    coroutineScope.launch {
        val controller = mapView.init(httpHandler, glViewHolderFactory)
        locationsFlow.collect {
            controller.updateCameraPosition(
                CameraUpdateFactory.newLngLatZoom(LngLat(it.longitude, it.latitude), 13f)
            )
            bitmapsFlow.emit(controller.captureFrame())
        }
    }
    remember(mapView) {
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            }
        }
    }
    return mapView
}
