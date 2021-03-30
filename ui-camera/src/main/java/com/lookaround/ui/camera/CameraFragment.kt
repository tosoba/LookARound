package com.lookaround.ui.camera

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ar.listener.ARStateListener
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.marker.SimpleARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.orientation.OrientationManager
import com.lookaround.core.android.ar.renderer.impl.CameraMarkerRenderer
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import com.lookaround.core.android.ar.view.ARView
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.BoxedSeekbar
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import com.lookaround.ui.camera.model.*
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.locationReadyUpdates
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions

@FlowPreview
@ExperimentalCoroutinesApi
@RuntimePermissions
@AndroidEntryPoint
@WithFragmentBindings
class CameraFragment :
    Fragment(R.layout.fragment_camera),
    OrientationManager.OnOrientationChangedListener,
    ARView.OnMarkerPressedListener {
    private val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    @Inject internal lateinit var cameraViewModelFactory: CameraViewModel.Factory
    private val cameraViewModel: CameraViewModel by assistedViewModel {
        cameraViewModelFactory.create(it)
    }

    @Inject internal lateinit var mainViewModelFactory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by assistedActivityViewModel {
        mainViewModelFactory.create(it)
    }

    private val cameraRenderer: CameraMarkerRenderer by lazy(LazyThreadSafetyMode.NONE) {
        CameraMarkerRenderer(requireContext())
    }

    private val orientationManager: OrientationManager by lazy(LazyThreadSafetyMode.NONE) {
        OrientationManager(requireContext()).apply {
            axisMode = OrientationManager.Mode.AR
            onOrientationChangedListener = this@CameraFragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraViewCreated) }

        arDisabledUpdates(mainViewModel, cameraViewModel)
            .onEach { (anyPermissionDenied, locationDisabled) ->
                (activity as? ARStateListener)?.onARDisabled(anyPermissionDenied, locationDisabled)
                binding.onARDisabled(anyPermissionDenied, locationDisabled)
            }
            .launchIn(lifecycleScope)

        initARWithPermissionCheck()

        binding.grantPermissionsButton.setOnClickListener { initARWithPermissionCheck() }
    }

    @NeedsPermission(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    internal fun initAR() {
        lifecycleScope.launch { mainViewModel.intent(MainIntent.LocationPermissionGranted) }

        loadingStartedUpdates(mainViewModel, cameraViewModel)
            .onEach {
                (activity as? ARStateListener)?.onARLoading()
                binding.onLoadingStarted()
            }
            .launchIn(lifecycleScope)

        binding.initARViews()

        arEnabledUpdates(mainViewModel, cameraViewModel)
            .onEach {
                (activity as? ARStateListener)?.onAREnabled()
                binding.onAREnabled()
            }
            .launchIn(lifecycleScope)

        mainViewModel
            .locationReadyUpdates
            .onEach {
                binding.arCameraView.povLocation = it
                binding.arRadarView.povLocation = it
            }
            .launchIn(lifecycleScope)
    }

    private fun FragmentCameraBinding.initARViews() {
        initARCameraPageViews()

        cameraPreview.previewStreamState.observe(this@CameraFragment) {
            lifecycleScope.launch {
                cameraViewModel.intent(CameraIntent.CameraStreamStateChanged(it))
            }
        }
        cameraPreview.init(this@CameraFragment)

        cameraRenderer
            .maxPageFlow
            .onEach { (maxPage, setCurrentPage) -> onCameraMaxPageChanged(maxPage, setCurrentPage) }
            .onStart { onCameraMaxPageChanged(cameraRenderer.maxPage, false) }
            .launchIn(lifecycleScope)

        arCameraView.maxDistance = MAX_RENDER_DISTANCE_METERS
        arCameraView.onMarkerPressedListener = this@CameraFragment
        arCameraView.markerRenderer = cameraRenderer

        arRadarView.maxDistance = MAX_RENDER_DISTANCE_METERS
        arRadarView.rotableBackground = R.drawable.radar_arrow
        arRadarView.markerRenderer = RadarMarkerRenderer()

        mainViewModel
            .markerUpdates
            .onEach { markers ->
                val cameraARMarkers = markers.map(::SimpleARMarker)
                cameraRenderer += cameraARMarkers
                arCameraView.markers = cameraARMarkers
                arRadarView.markers = markers.map(::SimpleARMarker)
            }
            .launchIn(lifecycleScope)
    }

    private fun FragmentCameraBinding.initARCameraPageViews() {
        arCameraPageUpBtn.setOnClickListener {
            ++arCameraPageSeekbar.value
            updatePageButtonsEnabled(arCameraPageSeekbar.value)
        }
        arCameraPageDownBtn.setOnClickListener {
            --arCameraPageSeekbar.value
            updatePageButtonsEnabled(arCameraPageSeekbar.value)
        }
        arCameraPageSeekbar.onValueChangeListener =
            object : BoxedSeekbar.OnValueChangeListener {
                override fun onValueChanged(seekbar: BoxedSeekbar, value: Int) {
                    updatePageButtonsEnabled(value)
                    cameraRenderer.currentPage = value
                }
            }
    }

    private fun FragmentCameraBinding.updatePageButtonsEnabled(points: Int) {
        arCameraPageUpBtn.isEnabled = points < arCameraPageSeekbar.max
        arCameraPageDownBtn.isEnabled = points > arCameraPageSeekbar.min
    }

    private fun FragmentCameraBinding.onLoadingStarted() {
        arViewsGroup.visibility = View.GONE
        arCameraPageViewsGroup.visibility = View.GONE
        locationDisabledTextView.visibility = View.GONE
        permissionsViewsGroup.visibility = View.GONE
        blurBackground.visibility = View.VISIBLE
        loadingShimmerLayout.showAndStart()
    }

    private fun FragmentCameraBinding.onAREnabled() {
        locationDisabledTextView.visibility = View.GONE
        permissionsViewsGroup.visibility = View.GONE
        loadingShimmerLayout.stopAndHide()
        blurBackground.visibility = View.GONE
        arViewsGroup.visibility = View.VISIBLE
    }

    private fun FragmentCameraBinding.onARDisabled(
        anyPermissionDenied: Boolean,
        locationDisabled: Boolean
    ) {
        arViewsGroup.visibility = View.GONE
        arCameraPageViewsGroup.visibility = View.GONE
        loadingShimmerLayout.stopAndHide()
        blurBackground.visibility = View.VISIBLE
        if (anyPermissionDenied) permissionsViewsGroup.visibility = View.VISIBLE
        if (locationDisabled) locationDisabledTextView.visibility = View.VISIBLE
    }

    private fun FragmentCameraBinding.onCameraMaxPageChanged(
        maxPage: Int,
        setCurrentPage: Boolean
    ) {
        arCameraPageViewsGroup.visibility = if (maxPage == 0) View.GONE else View.VISIBLE
        if (setCurrentPage) arCameraPageSeekbar.value = maxPage
        if (maxPage > 0) arCameraPageSeekbar.max = maxPage
        if (setCurrentPage) binding.updatePageButtonsEnabled(maxPage)
    }

    override fun onResume() {
        super.onResume()
        orientationManager.startSensor(requireContext())
    }

    override fun onPause() {
        binding.arCameraPageViewsGroup.visibility = View.GONE
        binding.arViewsGroup.visibility = View.GONE
        orientationManager.stopSensor()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @Suppress("unused")
    @OnPermissionDenied(Manifest.permission.CAMERA)
    internal fun onCameraPermissionDenied() {
        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraPermissionDenied) }
    }

    @Suppress("unused")
    @OnNeverAskAgain(Manifest.permission.CAMERA)
    internal fun onCameraPermissionNeverAskAgain() {
        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraPermissionDenied) }
    }

    @Suppress("unused")
    @OnPermissionDenied(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    internal fun onLocationPermissionDenied() {
        lifecycleScope.launch { mainViewModel.intent(MainIntent.LocationPermissionDenied) }
    }

    @Suppress("unused")
    @OnNeverAskAgain(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    internal fun onLocationPermissionNeverAskAgain() {
        lifecycleScope.launch { mainViewModel.intent(MainIntent.LocationPermissionDenied) }
    }

    override fun onOrientationChanged(orientation: Orientation) {
        binding.arCameraView.orientation = orientation
        binding.arCameraView.phoneRotation = requireContext().phoneRotation
        binding.arRadarView.orientation = orientation
    }

    override fun onMarkerPressed(marker: ARMarker) {
        Toast.makeText(
                requireContext(),
                "Pressed marker with id: ${marker.wrapped.id}",
                Toast.LENGTH_LONG
            )
            .show()
    }

    companion object {
        private const val MAX_RENDER_DISTANCE_METERS = 10_000.0
    }
}
