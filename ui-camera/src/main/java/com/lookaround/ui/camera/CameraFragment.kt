package com.lookaround.ui.camera

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.camera.core.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
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
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import com.lookaround.ui.camera.model.*
import com.lookaround.ui.camera.model.CameraState
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import java.util.*
import javax.inject.Inject
import kotlin.math.min
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

    private val cameraRenderer: CameraMarkerRenderer by
        lazy(LazyThreadSafetyMode.NONE) { CameraMarkerRenderer(requireContext()) }

    private val orientationManager: OrientationManager by
        lazy(LazyThreadSafetyMode.NONE) {
            OrientationManager(requireContext()).apply {
                axisMode = OrientationManager.Mode.AR
                onOrientationChangedListener = this@CameraFragment
            }
        }

    private val openGLRenderer: OpenGLRenderer by
        lazy(LazyThreadSafetyMode.NONE) { OpenGLRenderer() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraViewCreated) }

        arDisabledUpdates(mainViewModel, cameraViewModel)
            .onEach { (anyPermissionDenied, locationDisabled) ->
                (activity as? AREventsListener)?.onARDisabled(anyPermissionDenied, locationDisabled)
                binding.onARDisabled(anyPermissionDenied, locationDisabled)
            }
            .launchIn(lifecycleScope)

        cameraTouchUpdates(mainViewModel, cameraViewModel)
            .onEach { binding.onCameraTouch() }
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

        requireContext()
            .initCamera(
                lifecycleOwner = this@CameraFragment,
                openGLRenderer = openGLRenderer,
                cameraPreviewStub = binding.cameraPreview
            )

        openGLRenderer
            .previewStreamStateLiveData
            .asFlow()
            .distinctUntilChanged()
            // TODO: is there a need to strore streamState like that? Maybe replace it with
            // observing it straight from openGLRenderer
            .onEach { cameraViewModel.intent(CameraIntent.CameraStreamStateChanged(it)) }
            .launchIn(lifecycleScope)

        combine(
                cameraRenderer.markersDrawnFlow,
                cameraViewModel.states.map(CameraState::firstMarkerIndex::get),
                mainViewModel
                    .states
                    .map(MainState::markers::get)
                    .filterIsInstance<WithValue<ParcelableSortedSet<Marker>>>()
                    .map { it.value.size }
            ) { markersDrawn, firstMarkerIndex, markersSize ->
                Triple(markersDrawn, firstMarkerIndex, markersSize)
            }
            .onEach { (markersDrawn, firstMarkerIndex, markersSize) ->
                val (currentPage, maxPage) = markersDrawn
                onMarkersDrawn(
                    firstMarkerIndex = firstMarkerIndex,
                    markersSize = markersSize,
                    currentPage = currentPage,
                    maxPage = maxPage
                )
            }
            .launchIn(lifecycleScope)

        cameraRenderer
            .drawnRectsFlow
            .onEach(openGLRenderer::drawnRects::set)
            .launchIn(lifecycleScope)

        cameraViewObscuredUpdates(mainViewModel, cameraViewModel)
            .onEach { obscured ->
                openGLRenderer.setBlurEnabled(obscured, true)
                if (obscured) {
                    changeRadarViewTopGuideline(View.GONE)
                    hideARViews()
                } else {
                    changeRadarViewTopGuideline(View.VISIBLE)
                    showARViews()
                }
            }
            .launchIn(lifecycleScope)

        arCameraView.onMarkerPressed = ::onMarkerPressed
        arCameraView.onTouch = ::signalCameraTouch
        arCameraView.markerRenderer = cameraRenderer

        arRadarView.rotableBackground = R.drawable.radar_arrow
        arRadarView.markerRenderer = RadarMarkerRenderer()

        getMarkerUpdates(mainViewModel, cameraViewModel)
            .onEach { (markers, firstMarkerIndex) -> updateARMarkers(markers, firstMarkerIndex) }
            .launchIn(lifecycleScope)
    }

    private fun updateARMarkers(
        markers: Loadable<ParcelableSortedSet<Marker>>,
        firstMarkerIndex: Int
    ) {
        when (markers) {
            is Empty -> return
            is LoadingInProgress -> {
                // TODO: show loading msg (like a snackbar or smth...)
            }
            is Failed -> {
                // TODO: show an error snackbar
            }
            is WithValue -> {
                fun WithValue<ParcelableSortedSet<Marker>>.renderedWindow(): List<SimpleARMarker> =
                    value
                        .map(::SimpleARMarker)
                        .subList(
                            firstMarkerIndex,
                            min(value.size, firstMarkerIndex + MARKERS_FIRST_INDEX_DIFF)
                        )

                val renderedMarkers = markers.renderedWindow()
                cameraRenderer.setMarkers(renderedMarkers)
                binding.arCameraView.markers = renderedMarkers
                binding.arRadarView.markers = markers.renderedWindow()
            }
        }
    }

    private fun FragmentCameraBinding.initARCameraPageViews() {
        arCameraPageUpBtn.setOnClickListener {
            if (cameraRenderer.currentPage < cameraRenderer.maxPage) {
                ++cameraRenderer.currentPage
                return@setOnClickListener
            }

            val markers = mainViewModel.state.markers
            if (markers !is WithValue) return@setOnClickListener
            if (cameraViewModel.state.firstMarkerIndex * MARKERS_FIRST_INDEX_DIFF +
                    MARKERS_FIRST_INDEX_DIFF < markers.value.size
            ) {
                lifecycleScope.launch {
                    cameraViewModel.intent(
                        CameraIntent.CameraMarkersFirstIndexChanged(MARKERS_FIRST_INDEX_DIFF)
                    )
                }
            }
        }
        arCameraPageDownBtn.setOnClickListener {
            if (cameraRenderer.currentPage > 0) {
                --cameraRenderer.currentPage
            } else if (cameraViewModel.state.firstMarkerIndex > 0) {
                lifecycleScope.launch {
                    cameraViewModel.intent(
                        CameraIntent.CameraMarkersFirstIndexChanged(-MARKERS_FIRST_INDEX_DIFF)
                    )
                }
            }
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

    private fun FragmentCameraBinding.onMarkersDrawn(
        firstMarkerIndex: Int,
        markersSize: Int,
        currentPage: Int,
        maxPage: Int,
    ) {
        arCameraPageUpBtn.isEnabled =
            firstMarkerIndex * MARKERS_FIRST_INDEX_DIFF + MARKERS_FIRST_INDEX_DIFF < markersSize ||
                currentPage < maxPage
        arCameraPageDownBtn.isEnabled = firstMarkerIndex > 0 || currentPage > 0
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

    override fun onDestroy() {
        openGLRenderer.shutdown()
        super.onDestroy()
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

    private fun signalCameraTouch() {
        lifecycleScope.launch { cameraViewModel.signal(CameraSignal.CameraTouch) }
    }

    private fun onMarkerPressed(marker: ARMarker) {
        Timber.tag("MP")
            .d("Pressed marker with id: ${marker.wrapped.id}; name: ${marker.wrapped.name}")
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
    }

    private fun FragmentCameraBinding.hideARViews() {
        arViewsGroup.visibility = View.GONE
        arCameraPageViewsGroup.visibility = View.GONE
    }

    private fun FragmentCameraBinding.onCameraTouch() {
        val targetVisibility = arCameraPageViewsGroup.toggleVisibility()
        changeRadarViewTopGuideline(targetVisibility)
        (activity as? AREventsListener)?.onCameraTouch(targetVisibility)
    }

    companion object {
        private const val MARKERS_FIRST_INDEX_DIFF = 100
    }
}
