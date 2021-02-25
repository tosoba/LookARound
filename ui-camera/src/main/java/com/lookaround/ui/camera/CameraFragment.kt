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
import com.google.android.material.snackbar.Snackbar
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.orientation.OrientationManager
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.impl.NoOverlapRenderer
import com.lookaround.core.android.appunta.renderer.impl.SimplePointRenderer
import com.lookaround.core.android.appunta.view.AppuntaView
import com.lookaround.core.android.ext.*
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
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@RuntimePermissions
@AndroidEntryPoint
@WithFragmentBindings
class CameraFragment :
    Fragment(R.layout.fragment_camera),
    OrientationManager.OnOrientationChangedListener,
    AppuntaView.OnPointPressedListener {

    private val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    @Inject
    internal lateinit var viewModelFactory: CameraViewModel.Factory
    private val viewModel: CameraViewModel by assistedViewModel { viewModelFactory.create(it) }

    private val eyeViewRenderer: NoOverlapRenderer by lazy(LazyThreadSafetyMode.NONE) {
        NoOverlapRenderer()
    }

    private val orientationManager: OrientationManager by lazy(LazyThreadSafetyMode.NONE) {
        OrientationManager(requireContext()).apply {
            axisMode = OrientationManager.Mode.AR
            onOrientationChangedListener = this@CameraFragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initARWithPermissionCheck()
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
        viewModel.states
            .map { it.location }
            .filterNotNull()
            .onEach {
                binding.eyeView.location = it
                binding.radarView.location = it
            }
            .launchIn(lifecycleScope)

        viewModel.signals
            .filterIsInstance<CameraSignal.LocationUnavailable>()
            .onEach { Timber.e("Location unavailable") }
            .launchIn(lifecycleScope)
    }

    private fun FragmentCameraBinding.initARViews(points: List<Point> = SamplePoints.get()) {
        cameraPreview.previewStreamState.observe(
            this@CameraFragment,
            ::onPreviewViewStreamStateChanged
        )
        cameraPreview.init(this@CameraFragment)

        eyeViewRenderer += points

        eyeView.maxDistance = MAX_RENDER_DISTANCE_METERS
        eyeView.onPointPressedListener = this@CameraFragment
        eyeView.points = points
        eyeView.pointRenderer = eyeViewRenderer

        radarView.maxDistance = MAX_RENDER_DISTANCE_METERS
        radarView.rotableBackground = R.drawable.radar_arrow
        radarView.points = points
        radarView.pointRenderer = SimplePointRenderer()
    }

    private fun onPreviewViewStreamStateChanged(state: PreviewView.StreamState) {
        when (state) {
            PreviewView.StreamState.IDLE -> with(binding) {
                blurBackground.visibility = View.VISIBLE
                shimmerLayout.showAndStart()
            }
            PreviewView.StreamState.STREAMING -> with(binding) {
                shimmerLayout.stopAndHide()
                blurBackground.fadeOut()
                radarView.fadeIn()
                eyeView.fadeIn()
            }
        }
    }

    @Suppress("unused")
    @OnPermissionDenied(Manifest.permission.CAMERA)
    internal fun onPermissionsDenied() {
        showPermissionRequiredSnackbar()
    }

    @Suppress("unused")
    @OnNeverAskAgain(Manifest.permission.CAMERA)
    internal fun onNeverAskAgain() {
        showPermissionRequiredSnackbar()
    }

    private fun showPermissionRequiredSnackbar() {
        Snackbar.make(
            binding.root,
            "Camera access permission is required for AR camera to work.",
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onResume() {
        super.onResume()
        orientationManager.startSensor(requireContext())
    }

    override fun onPause() {
        binding.radarView.visibility = View.GONE
        binding.eyeView.visibility = View.GONE
        orientationManager.stopSensor()
        super.onPause()
    }

    override fun onOrientationChanged(orientation: Orientation) {
        binding.eyeView.orientation = orientation
        binding.eyeView.phoneRotation = requireContext().phoneRotation
        binding.radarView.orientation = orientation
    }

    override fun onPointPressed(point: Point) {
        Toast.makeText(requireContext(), "Pressed point with id: ${point.id}", Toast.LENGTH_LONG)
            .show()
    }

    companion object {
        private const val MAX_RENDER_DISTANCE_METERS = 10_000.0
    }
}
