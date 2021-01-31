package com.lookaround.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.init
import com.lookaround.core.android.ext.restoreCameraPosition
import com.lookaround.core.android.ext.saveCameraPosition
import com.lookaround.core.android.ext.zoomOnDoubleTap
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.*
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class MapFragment :
    Fragment(R.layout.fragment_map),
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)
    private val mapController: Deferred<MapController> by lazyAsync { binding.map.init() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapController.launch {
            loadSceneFile(
                "https://www.nextzen.org/carto/bubble-wrap-style/9/bubble-wrap-style.zip",
                listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
            )
            restoreCameraPosition(savedInstanceState)
            zoomOnDoubleTap()
        }
    }

    override fun onDestroyView() {
        binding.map.onDestroy()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mapController.launch { saveCameraPosition(outState) }
    }

    private fun Deferred<MapController>.launch(block: MapController.() -> Unit) {
        this@MapFragment.launch(Dispatchers.Main.immediate) { this@launch.await().block() }
    }
}