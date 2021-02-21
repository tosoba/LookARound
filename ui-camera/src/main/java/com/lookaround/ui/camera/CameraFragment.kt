package com.lookaround.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.gms.location.LocationRequest
import com.google.android.material.snackbar.Snackbar
import com.jintin.fancylocation.LocationData
import com.jintin.fancylocation.LocationFlow
import com.lookaround.core.android.appunta.location.LocationFactory
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.orientation.OrientationManager
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.impl.NoOverlapRenderer
import com.lookaround.core.android.appunta.renderer.impl.SimplePointRenderer
import com.lookaround.core.android.appunta.view.AppuntaView
import com.lookaround.core.android.ext.*
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber

@ExperimentalCoroutinesApi
@RuntimePermissions
class CameraFragment :
    Fragment(R.layout.fragment_camera),
    OrientationManager.OnOrientationChangedListener,
    AppuntaView.OnPointPressedListener {

    private val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    private val userLocation: Location = LocationFactory
        .create(41.383873, 2.156574, 12.0)

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

    @SuppressLint("MissingPermission")
    private fun initLocation() {
        val locationRequest = LocationRequest.create()
            .setInterval(3000)
            .setFastestInterval(3000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        LocationFlow(requireContext(), locationRequest)
            .get()
            .onEach { locationData ->
                when (locationData) {
                    is LocationData.Success -> {
                        binding.eyeView.location = locationData.location
                        binding.radarView.location = locationData.location
                    }
                    is LocationData.Fail -> Timber.e("Location data fail.")
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun FragmentCameraBinding.initARViews(points: List<Point> = SamplePoints.get()) {
        cameraPreview.previewStreamState.observe(
            this@CameraFragment,
            ::onPreviewViewStreamStateChanged
        )
        cameraPreview.init(this@CameraFragment)

        eyeView.maxDistance = MAX_RENDER_DISTANCE_METERS
        eyeView.onPointPressedListener = this@CameraFragment
        eyeView.points = points
        eyeView.pointRenderer = NoOverlapRenderer(userLocation, points)
        eyeView.location = userLocation

        radarView.maxDistance = MAX_RENDER_DISTANCE_METERS
        radarView.rotableBackground = R.drawable.radar_arrow
        radarView.points = points
        radarView.pointRenderer = SimplePointRenderer()
        radarView.location = userLocation
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

    @OnPermissionDenied(Manifest.permission.CAMERA)
    internal fun onPermissionsDenied() {
        showPermissionRequiredSnackbar()
    }

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
