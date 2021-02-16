package com.lookaround.ui.camera

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.lookaround.core.android.appunta.location.LocationFactory
import com.lookaround.core.android.appunta.orientation.Orientation
import com.lookaround.core.android.appunta.orientation.OrientationManager
import com.lookaround.core.android.appunta.point.ARObject
import com.lookaround.core.android.appunta.point.Point
import com.lookaround.core.android.appunta.renderer.impl.RectViewRenderer
import com.lookaround.core.android.appunta.renderer.impl.SimplePointRenderer
import com.lookaround.core.android.appunta.view.AppuntaView
import com.lookaround.core.android.appunta.view.CameraView
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions

@RuntimePermissions
class CameraFragment :
    Fragment(R.layout.fragment_camera),
    OrientationManager.OnOrientationChangedListener,
    AppuntaView.OnPointPressedListener {

    private val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    private val orientationManager: OrientationManager by lazy(LazyThreadSafetyMode.NONE) {
        OrientationManager(requireContext()).apply {
            axisMode = OrientationManager.MODE_AR
            setOnOrientationChangeListener(this@CameraFragment)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initARWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.CAMERA)
    internal fun initAR() {
        val points = SamplePoints.get()
        points.forEach(::ARObject)
        val location = LocationFactory.create(41.383873, 2.156574, 12.0)
        binding.initARViews(points, location)
    }

    private fun FragmentCameraBinding.initARViews(points: List<Point>, location: Location) {
        eyeView.maxDistance = MAX_RENDER_DISTANCE_METERS
        eyeView.onPointPressedListener = this@CameraFragment
        eyeView.points = points
        eyeView.pointRenderer = RectViewRenderer()
        eyeView.location = location

        radarView.maxDistance = MAX_RENDER_DISTANCE_METERS
        radarView.rotableBackground = R.drawable.radar_arrow
        radarView.points = points
        radarView.pointRenderer = SimplePointRenderer()
        radarView.location = location

        cameraContainer.addView(CameraView(requireContext()))
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    internal fun onPermissionsDenied() {
        showPermissionRequiredSnackbar()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    internal fun onContactsNeverAskAgain() {
        showPermissionRequiredSnackbar()
    }

    private fun showPermissionRequiredSnackbar() {
        Snackbar.make(
            binding.root,
            "Camera access permission is required for AR camera to work.",
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        ARObject.objects.clear()
        super.onDestroyView()
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
        orientationManager.stopSensor()
        super.onPause()
    }

    override fun onOrientationChanged(orientation: Orientation) {
        binding.eyeView.orientation = orientation
        binding.eyeView.phoneRotation = OrientationManager.getPhoneRotation(requireContext())
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
