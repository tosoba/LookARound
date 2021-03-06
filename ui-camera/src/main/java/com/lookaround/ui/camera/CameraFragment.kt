package com.lookaround.ui.camera

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.orientation.OrientationManager
import com.lookaround.core.android.ar.renderer.impl.CameraMarkerRenderer
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import com.lookaround.core.android.ar.view.ARView
import com.lookaround.core.android.exception.LocationDisabledException
import com.lookaround.core.android.exception.LocationPermissionDeniedException
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.BoxedVerticalSeekbar
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import com.lookaround.ui.camera.model.CameraIntent
import com.lookaround.ui.camera.model.CameraPreviewState
import com.lookaround.ui.camera.model.SampleMarkers
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

    @Inject internal lateinit var viewModelFactory: CameraViewModel.Factory
    private val viewModel: CameraViewModel by assistedViewModel { viewModelFactory.create(it) }

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
        lifecycleScope.launch { viewModel.intent(CameraIntent.CameraViewCreated) }
        viewModel
            .states
            .map { (locationState, cameraPreviewState) ->
                ((locationState is Failed &&
                    locationState.error is LocationPermissionDeniedException) ||
                    (cameraPreviewState is CameraPreviewState.PermissionDenied)) to
                    (locationState is Failed && locationState.error is LocationDisabledException)
            }
            .distinctUntilChanged()
            .filter { (anyPermissionDenied, locationDisabled) ->
                anyPermissionDenied || locationDisabled
            }
            .onEach { (anyPermissionDenied, locationDisabled) ->
                showARDisabledViews(anyPermissionDenied, locationDisabled)
            }
            .launchIn(lifecycleScope)
        initARWithPermissionCheck()
        binding.grantPermissionsButton.setOnClickListener { initARWithPermissionCheck() }
    }

    @NeedsPermission(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    internal fun initAR() {
        viewModel
            .states
            .map { (locationState, cameraPreviewState) ->
                (locationState is LoadingInProgress) to
                    (cameraPreviewState is CameraPreviewState.Initial ||
                        (cameraPreviewState is CameraPreviewState.Active &&
                            cameraPreviewState.streamState == PreviewView.StreamState.IDLE))
            }
            .distinctUntilChanged()
            .filter { (loadingLocation, loadingCamera) -> loadingLocation || loadingCamera }
            .onEach {
                with(binding) {
                    locationDisabledTextView.visibility = View.GONE
                    permissionsViewsGroup.visibility = View.GONE
                    blurBackground.visibility = View.VISIBLE
                    loadingShimmerLayout.showAndStart()
                }
            }
            .launchIn(lifecycleScope)
        initLocation()
        binding.initARViews()
    }

    private fun initLocation() {
        lifecycleScope.launch { viewModel.intent(CameraIntent.LocationPermissionGranted) }

        viewModel
            .states
            .map { it.locationState }
            .filterIsInstance<WithValue<Location>>()
            .map { it.value }
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }
            .onEach {
                binding.arCameraView.povLocation = it
                binding.arRadarView.povLocation = it
            }
            .launchIn(lifecycleScope)
    }

    private fun FragmentCameraBinding.initARViews(markers: List<ARMarker> = SampleMarkers.get()) {
        initARCameraPageViews()

        cameraPreview.previewStreamState.observe(this@CameraFragment) {
            lifecycleScope.launch { viewModel.intent(CameraIntent.CameraStreamStateChanged(it)) }
        }
        cameraPreview.init(this@CameraFragment)

        viewModel
            .states
            .map { (locationState, cameraPreviewState) ->
                (locationState is Ready) to
                    (cameraPreviewState is CameraPreviewState.Active &&
                        cameraPreviewState.streamState == PreviewView.StreamState.STREAMING)
            }
            .distinctUntilChanged()
            .filter { (locationReady, cameraStreaming) -> locationReady && cameraStreaming }
            .onEach {
                locationDisabledTextView.visibility = View.GONE
                permissionsViewsGroup.visibility = View.GONE
                loadingShimmerLayout.stopAndHide()
                blurBackground.visibility = View.GONE
                arViewsGroup.visibility = View.VISIBLE
            }
            .launchIn(lifecycleScope)

        cameraRenderer += markers
        cameraRenderer
            .maxPageFlow
            .distinctUntilChanged()
            .onEach { (maxPage, setCurrentPage) ->
                arCameraPageViewsGroup.visibility = if (maxPage == 0) View.GONE else View.VISIBLE
                if (setCurrentPage) arCameraPageSeekbar.value = maxPage
                if (maxPage > 0) arCameraPageSeekbar.max = maxPage
                if (setCurrentPage) binding.updatePageButtonsEnabled(maxPage)
            }
            .launchIn(lifecycleScope)

        arCameraView.maxDistance = MAX_RENDER_DISTANCE_METERS
        arCameraView.onMarkerPressedListener = this@CameraFragment
        arCameraView.markers = markers
        arCameraView.markerRenderer = cameraRenderer

        arRadarView.maxDistance = MAX_RENDER_DISTANCE_METERS
        arRadarView.rotableBackground = R.drawable.radar_arrow
        arRadarView.markers = markers
        arRadarView.markerRenderer = RadarMarkerRenderer()
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
        arCameraPageSeekbar.onValuesChangeListener =
            object : BoxedVerticalSeekbar.OnValuesChangeListener {
                override fun onPointsChanged(seekbar: BoxedVerticalSeekbar, points: Int) {
                    updatePageButtonsEnabled(points)
                    cameraRenderer.currentPage = points
                }
            }
    }

    private fun FragmentCameraBinding.updatePageButtonsEnabled(points: Int) {
        arCameraPageUpBtn.isEnabled = points < arCameraPageSeekbar.max
        arCameraPageDownBtn.isEnabled = points > arCameraPageSeekbar.min
    }

    @Suppress("unused")
    @OnPermissionDenied(Manifest.permission.CAMERA)
    internal fun onCameraPermissionDenied() {
        lifecycleScope.launch { viewModel.intent(CameraIntent.CameraPermissionDenied) }
    }

    @Suppress("unused")
    @OnPermissionDenied(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    internal fun onLocationPermissionDenied() {
        lifecycleScope.launch { viewModel.intent(CameraIntent.LocationPermissionDenied) }
    }

    @Suppress("unused")
    @OnNeverAskAgain(Manifest.permission.CAMERA)
    internal fun onCameraPermissionNeverAskAgain() {
        lifecycleScope.launch { viewModel.intent(CameraIntent.CameraPermissionDenied) }
    }

    @Suppress("unused")
    @OnNeverAskAgain(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    internal fun onLocationPermissionNeverAskAgain() {
        lifecycleScope.launch { viewModel.intent(CameraIntent.LocationPermissionDenied) }
    }

    private fun showARDisabledViews(anyPermissionDenied: Boolean, locationDisabled: Boolean) {
        with(binding) {
            arViewsGroup.visibility = View.GONE
            arCameraPageViewsGroup.visibility = View.GONE
            loadingShimmerLayout.stopAndHide()
            blurBackground.visibility = View.VISIBLE
            if (anyPermissionDenied) permissionsViewsGroup.visibility = View.VISIBLE
            if (locationDisabled) locationDisabledTextView.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        onRequestPermissionsResult(requestCode, grantResults)
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
