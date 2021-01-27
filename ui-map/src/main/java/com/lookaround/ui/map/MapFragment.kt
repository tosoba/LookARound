package com.lookaround.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.lookaround.core.android.delegate.viewBinding
import com.lookaround.core.android.ext.init
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.MapController
import com.mapzen.tangram.SceneUpdate
import kotlinx.coroutines.*

class MapFragment :
    Fragment(R.layout.fragment_map),
    CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)
    private val mapController: Deferred<MapController> by lazyAsync { binding.map.init() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapController.launch {
            loadSceneFile(
                "https://www.nextzen.org/carto/bubble-wrap-style/9/bubble-wrap-style.zip",
                listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))
            )
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

    private fun Deferred<MapController>.launch(block: MapController.() -> Unit) {
        this@MapFragment.launch { this@launch.await().block() }
    }
}
