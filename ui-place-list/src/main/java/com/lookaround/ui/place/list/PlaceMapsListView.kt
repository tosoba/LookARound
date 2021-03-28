package com.lookaround.ui.place.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lookaround.core.android.ext.init
import com.mapzen.tangram.MapView
import com.mapzen.tangram.Marker
import com.mapzen.tangram.networking.HttpHandler
import com.mapzen.tangram.viewholder.GLSurfaceViewHolderFactory
import com.mapzen.tangram.viewholder.GLViewHolderFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun MapView(
    locationFlow: Flow<Marker>,
    httpHandler: HttpHandler? = null,
    glViewHolderFactory: GLViewHolderFactory = GLSurfaceViewHolderFactory()
): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val coroutineScope = rememberCoroutineScope()
    coroutineScope.launch {
        val controller = mapView.init(httpHandler, glViewHolderFactory)
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
