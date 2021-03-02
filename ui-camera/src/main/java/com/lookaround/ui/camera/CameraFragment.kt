package com.lookaround.ui.camera

import android.Manifest
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
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.view.BoxedVerticalSeekbar
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber
import java.util.*
import javax.inject.Inject

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
        initARWithPermissionCheck()
        binding.grantPermissionsButton.setOnClickListener { initARWithPermissionCheck() }
    }

    @NeedsPermission(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    internal fun initAR() {
        binding.initARViews()
        initLocation()
    }

    private fun initLocation() {
        viewModel
            .states
            .map { it.location }
            .filterNotNull()
            .distinctUntilChangedBy { Objects.hash(it.latitude, it.longitude) }
            .onEach {
                binding.arCameraView.povLocation = it
                binding.arRadarView.povLocation = it
            }
            .launchIn(lifecycleScope)

        viewModel
            .signals
            .filterIsInstance<CameraSignal.LocationUnavailable>()
            .onEach { Timber.e("Location unavailable") }
            .launchIn(lifecycleScope)
    }

    private fun FragmentCameraBinding.initARViews(markers: List<ARMarker> = SampleMarkers.get()) {
        initARCameraPageViews()

        cameraPreview.previewStreamState.observe(
            this@CameraFragment,
            ::onPreviewViewStreamStateChanged
        )
        cameraPreview.init(this@CameraFragment)

        cameraRenderer += markers
        cameraRenderer
            .maxPageFlow
            .distinctUntilChanged()
            .onEach { if (it == 0) {} else {} }
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
        fun updatePageButtonsEnabled(points: Int) {
            arCameraPageUpBtn.isEnabled = points < arCameraPageSeekbar.max
            arCameraPageDownBtn.isEnabled = points > arCameraPageSeekbar.min
        }

        arCameraPageUpBtn.setOnClickListener {
            ++arCameraPageSeekbar.value
            updatePageButtonsEnabled(arCameraPageSeekbar.value)
        }
        arCameraPageDownBtn.setOnClickListener {
            --arCameraPageSeekbar.value
            updatePageButtonsEnabled(arCameraPageSeekbar.value)
        }
        arCameraPageSeekbar.setOnBoxedPointsChangeListener(
            object : BoxedVerticalSeekbar.OnValuesChangeListener {
                override fun onPointsChanged(seekbar: BoxedVerticalSeekbar, points: Int) {
                    updatePageButtonsEnabled(points)
                }

                override fun onStartTrackingTouch(seekbar: BoxedVerticalSeekbar) = Unit

                override fun onStopTrackingTouch(seekbar: BoxedVerticalSeekbar) = Unit
            }
        )
    }

    private fun onPreviewViewStreamStateChanged(state: PreviewView.StreamState) {
        when (state) {
            PreviewView.StreamState.IDLE ->
                with(binding) {
                    permissionsViewsGroup.visibility = View.GONE
                    blurBackground.fadeIn()
                    shimmerLayout.showAndStart()
                }
            PreviewView.StreamState.STREAMING ->
                with(binding) {
                    shimmerLayout.stopAndHide()
                    blurBackground.fadeOut()
                    arViewsGroup.fadeIn()
                    arCameraPageViewsGroup.fadeIn()
                }
        }
    }

    @Suppress("unused")
    @OnPermissionDenied(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    internal fun onPermissionsDenied() {
        showPermissionsRequiredView()
    }

    @Suppress("unused")
    @OnNeverAskAgain(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )
    internal fun onNeverAskAgain() {
        showPermissionsRequiredView()
    }

    private fun showPermissionsRequiredView() {
        with(binding) {
            arViewsGroup.visibility = View.GONE
            arCameraPageViewsGroup.visibility = View.GONE
            shimmerLayout.stopAndHide()
            blurBackground.fadeIn()
            permissionsViewsGroup.fadeIn()
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
