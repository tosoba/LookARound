package com.lookaround.ui.camera

import android.Manifest
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.lookaround.core.android.ar.listener.AREventsListener
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.marker.SimpleARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.orientation.OrientationManager
import com.lookaround.core.android.ar.renderer.impl.CameraMarkerRenderer
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import com.lookaround.core.android.camera.OpenGLRenderer
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.android.view.BoxedSeekbar
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import com.lookaround.ui.camera.model.*
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.bottomSheetStateUpdates
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.markerUpdates
import com.lookaround.ui.main.model.MainIntent
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
import timber.log.Timber

@FlowPreview
@ExperimentalCoroutinesApi
@RuntimePermissions
@AndroidEntryPoint
@WithFragmentBindings
class CameraFragment :
    Fragment(R.layout.fragment_camera), OrientationManager.OnOrientationChangedListener {
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

    private val openGLRenderer: OpenGLRenderer by lazy(LazyThreadSafetyMode.NONE) {
        OpenGLRenderer()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraViewCreated) }

        arDisabledUpdates(mainViewModel, cameraViewModel)
            .onEach { (anyPermissionDenied, locationDisabled) ->
                (activity as? AREventsListener)?.onARDisabled(anyPermissionDenied, locationDisabled)
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
                (activity as? AREventsListener)?.onARLoading()
                binding.onLoadingStarted()
            }
            .launchIn(lifecycleScope)

        binding.initARViews()

        arEnabledUpdates(mainViewModel, cameraViewModel)
            .onEach {
                (activity as? AREventsListener)?.onAREnabled()
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
        initARCameraRangeViews()

        mainViewModel
            .bottomSheetStateUpdates
            .onEach { (state, _) ->
                when (state) {
                    BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN -> {
                        changeRadarViewTopGuideline(View.VISIBLE)
                        showARViews()
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        changeRadarViewTopGuideline(View.GONE)
                        hideARViews()
                    }
                }
            }
            .launchIn(lifecycleScope)

        cameraPreview.previewStreamState.observe(this@CameraFragment) {
            lifecycleScope.launch {
                cameraViewModel.intent(CameraIntent.CameraStreamStateChanged(it))
            }
        }
        cameraPreview.init(this@CameraFragment)
        cameraViewModel
            .cameraLiveUpdates
            .onEach {
                //TODO: use methods will likely not work (due to setting surfaceProvider + wrong call order) -> use shouldUseTextureView to decide what mode to use + figure out IDLE/STREAMING callback for both implementations
                when (val cameraView = binding.cameraPreview.cameraView) {
                    is SurfaceView -> openGLRenderer.use(cameraView, false)
                    is TextureView -> openGLRenderer.use(cameraView)
                    else -> return@onEach
                }
            }
            .launchIn(lifecycleScope)

        cameraRenderer
            .maxPageFlow
            .onEach { (maxPage, setCurrentPage) -> onCameraMaxPageChanged(maxPage, setCurrentPage) }
            .onStart { onCameraMaxPageChanged(cameraRenderer.maxPage, false) }
            .launchIn(lifecycleScope)

        arCameraView.onMarkerPressed = ::onMarkerPressed
        arCameraView.onTouch = ::onCameraTouch
        arCameraView.markerRenderer = cameraRenderer

        arRadarView.rotableBackground = R.drawable.radar_arrow
        arRadarView.markerRenderer = RadarMarkerRenderer()

        mainViewModel.markerUpdates.onEach(::updateMarkers).launchIn(lifecycleScope)
    }

    private fun updateMarkers(markers: List<Marker>) {
        val cameraARMarkers = markers.map(::SimpleARMarker)
        cameraRenderer += cameraARMarkers
        binding.arCameraView.markers = cameraARMarkers
        binding.arRadarView.markers = markers.map(::SimpleARMarker)
    }

    private fun FragmentCameraBinding.initARCameraPageViews() {
        arCameraPageSeekbar.setValueButtonsOnClickListeners(
            upBtn = arCameraPageUpBtn,
            downBtn = arCameraPageDownBtn
        )
        arCameraPageSeekbar.onValueChangeListener =
            object : BoxedSeekbar.OnValueChangeListener {
                override fun onValueChanged(seekbar: BoxedSeekbar, value: Int) {
                    seekbar.updateValueButtonsEnabled(
                        points = value,
                        upBtn = arCameraPageUpBtn,
                        downBtn = arCameraPageDownBtn,
                    )
                    cameraRenderer.currentPage = value
                }
            }
    }

    private fun FragmentCameraBinding.initARCameraRangeViews() {
        arCameraRangeSeekbar.setValueButtonsOnClickListeners(
            upBtn = arCameraRangeUpBtn,
            downBtn = arCameraRangeDownBtn
        )
        arCameraRangeSeekbar.onValueChangeListener =
            object : BoxedSeekbar.OnValueChangeListener {
                override fun onValueChanged(seekbar: BoxedSeekbar, value: Int) {
                    seekbar.updateValueButtonsEnabled(
                        points = value,
                        upBtn = arCameraRangeUpBtn,
                        downBtn = arCameraRangeDownBtn,
                    )
                    val meters = Range.metersFrom(ordinal = value)
                    arCameraView.maxRange = meters
                    arRadarView.maxRange = meters
                }
            }
        arCameraRangeSeekbar.valueToPointsText =
            { value ->
                Range.labelFrom(ordinal = value.coerceAtMost(Range.values().size - 1))
            }
    }

    private fun FragmentCameraBinding.onLoadingStarted() {
        hideARViews()
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
        showARViews()
    }

    private fun FragmentCameraBinding.onARDisabled(
        anyPermissionDenied: Boolean,
        locationDisabled: Boolean
    ) {
        hideARViews()
        loadingShimmerLayout.stopAndHide()
        blurBackground.visibility = View.VISIBLE
        if (anyPermissionDenied) permissionsViewsGroup.visibility = View.VISIBLE
        if (locationDisabled) locationDisabledTextView.visibility = View.VISIBLE
    }

    private fun FragmentCameraBinding.onCameraMaxPageChanged(
        maxPage: Int,
        setCurrentPage: Boolean
    ) {
        if (maxPage > 0) arCameraRangeViewsGroup.visibility = View.VISIBLE
        arCameraPageViewsGroup.visibility = if (maxPage > 0) View.VISIBLE else View.GONE
        arCameraPageSeekbar.isEnabled = maxPage > 0
        if (setCurrentPage) arCameraPageSeekbar.value = maxPage
        if (maxPage > 0) arCameraPageSeekbar.max = maxPage
        arCameraPageSeekbar.updateValueButtonsEnabled(
            upBtn = arCameraPageUpBtn,
            downBtn = arCameraPageDownBtn,
        )
    }

    override fun onResume() {
        super.onResume()
        orientationManager.startSensor(requireContext())
    }

    override fun onPause() {
        binding.hideARViews()
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

    private fun onMarkerPressed(marker: ARMarker) {
        Timber.tag("MP")
            .d("Pressed marker with id: ${marker.wrapped.id}; name: ${marker.wrapped.name}")
    }

    private fun onCameraTouch() {
        with(binding) {
            val targetVisibility = arCameraRangeViewsGroup.toggleVisibility()
            if (arCameraPageSeekbar.isEnabled) arCameraPageViewsGroup.visibility = targetVisibility
            changeRadarViewTopGuideline(targetVisibility)
            (activity as? AREventsListener)?.onCameraTouch(targetVisibility)
        }
    }

    private fun FragmentCameraBinding.changeRadarViewTopGuideline(targetVisibility: Int) {
        val radarGuidelineLayoutParams =
            radarViewTopGuideline.layoutParams as ConstraintLayout.LayoutParams
        radarGuidelineLayoutParams.guideBegin =
            if (targetVisibility == View.GONE) 0 else requireContext().dpToPx(56f).toInt()
        radarViewTopGuideline.layoutParams = radarGuidelineLayoutParams
    }

    private fun FragmentCameraBinding.showARViews() {
        arViewsGroup.visibility = View.VISIBLE
        arCameraRangeViewsGroup.visibility = View.VISIBLE
        if (arCameraPageSeekbar.isEnabled) arCameraPageViewsGroup.visibility = View.VISIBLE
    }

    private fun FragmentCameraBinding.hideARViews() {
        arViewsGroup.visibility = View.GONE
        arCameraPageViewsGroup.visibility = View.GONE
        arCameraRangeViewsGroup.visibility = View.GONE
    }
}
