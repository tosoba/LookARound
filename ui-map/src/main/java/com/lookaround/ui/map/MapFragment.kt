package com.lookaround.ui.map

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.lookaround.core.android.delegate.viewBinding
import com.lookaround.ui.map.databinding.FragmentMapBinding
import com.mapzen.tangram.SceneUpdate

class MapFragment : Fragment(R.layout.fragment_map) {
    private val binding: FragmentMapBinding by viewBinding(FragmentMapBinding::bind)
    private val sceneUpdates: List<SceneUpdate> =
        listOf(SceneUpdate("global.sdk_api_key", BuildConfig.NEXTZEN_API_KEY))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.map.getMapAsync {
            it?.loadSceneFile(
                "https://www.nextzen.org/carto/bubble-wrap-style/9/bubble-wrap-style.zip",
                sceneUpdates
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
}
