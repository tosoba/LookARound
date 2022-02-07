package com.lookaround.ui.camera

import android.Manifest
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hoko.blur.HokoBlur
import com.hoko.blur.processor.BlurProcessor
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.marker.SimpleARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.orientation.OrientationManager
import com.lookaround.core.android.ar.renderer.impl.CameraMarkerRenderer
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import com.lookaround.core.android.ar.view.ARRadarView
import com.lookaround.core.android.camera.OpenGLRenderer
import com.lookaround.core.android.ext.*
import com.lookaround.core.android.model.*
import com.lookaround.core.delegate.lazyAsync
import com.lookaround.ui.camera.databinding.FragmentCameraBinding
import com.lookaround.ui.camera.model.CameraARState
import com.lookaround.ui.camera.model.CameraIntent
import com.lookaround.ui.camera.model.CameraSignal
import com.lookaround.ui.camera.model.CameraState
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.locationReadyUpdates
import com.lookaround.ui.main.model.MainIntent
import com.lookaround.ui.main.model.MainSignal
import com.lookaround.ui.main.model.MainState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlin.math.min
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber

@AndroidEntryPoint
@WithFragmentBindings
@FlowPreview
@ExperimentalCoroutinesApi
@RuntimePermissions
class CameraFragment :
    Fragment(R.layout.fragment_camera), OrientationManager.OnOrientationChangedListener {
    private val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    private val cameraViewModel: CameraViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private val cameraMarkerRenderer: CameraMarkerRenderer by
        lazy(LazyThreadSafetyMode.NONE) { CameraMarkerRenderer(requireContext()) }
    private val radarMarkerRenderer: RadarMarkerRenderer by
        lazy(LazyThreadSafetyMode.NONE, ::RadarMarkerRenderer)

    private val orientationManager: OrientationManager by
        lazy(LazyThreadSafetyMode.NONE) {
            OrientationManager(requireContext()).apply {
                axisMode = OrientationManager.Mode.AR
                onOrientationChangedListener = this@CameraFragment
            }
        }

    private val openGLRenderer: OpenGLRenderer by lazy(LazyThreadSafetyMode.NONE, ::OpenGLRenderer)

    private val cameraInitializationResult: Deferred<CameraInitializationResult> by
        lifecycleScope.lazyAsync {
            requireContext()
                .initCamera(
                    lifecycleOwner = this@CameraFragment,
                    rotation = rotation,
                    screenSize = requireContext().getScreenSize(),
                    imageAnalysisResolutionDivisor = 25
                )
        }

    private var latestARState: CameraARState = CameraARState.INITIAL

    private val blurProcessor: BlurProcessor by
        lazy(LazyThreadSafetyMode.NONE) {
            HokoBlur.with(requireContext())
                .sampleFactor(8f)
                .scheme(HokoBlur.SCHEME_OPENGL)
                .mode(HokoBlur.MODE_GAUSSIAN)
                .radius(15)
                .processor()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.state.bitmapCache.get(javaClass.name)?.let { blurredBackground ->
            binding.blurBackground.background = BitmapDrawable(resources, blurredBackground)
        }

        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraViewCreated) }

        arDisabledUpdates(mainViewModel, cameraViewModel)
            .onEach {
                (anyPermissionDenied, locationDisabled, pitchOutsideLimit, initializationFailure) ->
                mainViewModel.signal(MainSignal.ARDisabled)
                binding.onARDisabled(
                    anyPermissionDenied = anyPermissionDenied,
                    locationDisabled = locationDisabled,
                    pitchOutsideLimit = pitchOutsideLimit,
                    initializationFailure = initializationFailure
                )
                latestARState = CameraARState.DISABLED
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraTouchUpdates(mainViewModel, cameraViewModel).collect { binding.onCameraTouch() }
        }

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

        mainViewModel
            .locationReadyUpdates
            .onEach { location ->
                binding.arCameraView.povLocation = location
                binding.arRadarView.povLocation = location
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        loadingStartedUpdates(mainViewModel, cameraViewModel)
            .onEach {
                mainViewModel.signal(MainSignal.ARLoading)
                binding.onLoadingStarted()
                latestARState = CameraARState.LOADING
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.initARViews()

        arEnabledUpdates(mainViewModel, cameraViewModel)
            .onEach {
                mainViewModel.signal(MainSignal.AREnabled)
                binding.onAREnabled()
                latestARState = CameraARState.ENABLED
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun FragmentCameraBinding.initARViews() {
        initARCameraPageViews()

        initCamera()

        openGLRenderer
            .previewStreamStates
            .distinctUntilChanged()
            .onEach { cameraViewModel.intent(CameraIntent.CameraStreamStateChanged(it)) }
            .launchIn(lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            val markersDrawnUpdates =
                combine(
                    cameraMarkerRenderer.markersDrawnFlow,
                    cameraViewModel.mapStates(CameraState::firstMarkerIndex),
                    mainViewModel
                        .mapStates(MainState::markers)
                        .filterIsInstance<WithValue<ParcelableSortedSet<Marker>>>()
                        .map { it.value.size },
                    cameraViewObscuredUpdates(mainViewModel, cameraViewModel)
                ) {
                    markersDrawn,
                    firstMarkerIndex,
                    markersSize,
                    (cameraObscuredByFragment, cameraObscuredByBottomSheet) ->
                    val (currentPage, maxPage) = markersDrawn
                    CameraMarkersDrawnViewUpdate(
                        firstMarkerIndex = firstMarkerIndex,
                        markersSize = markersSize,
                        currentPage = currentPage,
                        maxPage = maxPage,
                        cameraObscured = cameraObscuredByFragment || cameraObscuredByBottomSheet
                    )
                }
            markersDrawnUpdates.distinctUntilChanged().collect { onMarkersDrawn(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraMarkerRenderer.drawnRectsFlow.collect(openGLRenderer::setMarkerRects)
        }
        openGLRenderer.otherRects =
            listOf(RoundedRectF(requireContext().bottomNavigationViewRectF, 0f))

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraViewObscuredUpdates(mainViewModel, cameraViewModel).collectIndexed {
                index,
                (obscuredByFragment, obscuredByBottomSheet) ->
                val obscured = obscuredByFragment || obscuredByBottomSheet
                openGLRenderer.setBlurEnabled(enabled = obscured, animated = !obscured || index > 0)
                if (obscured) {
                    changeRadarViewTopGuideline(View.GONE)
                    hideARViews()
                } else {
                    changeRadarViewTopGuideline(View.VISIBLE)
                    showARViews(showRadar = mainViewModel.state.markers is WithValue)
                }
            }
        }

        arCameraView.onMarkerPressed = ::onMarkerPressed
        arCameraView.onTouch = ::signalCameraTouch
        arCameraView.markerRenderer = cameraMarkerRenderer

        arRadarView.rotableBackground = R.drawable.radar_arrow
        arRadarView.markerRenderer = radarMarkerRenderer
        arRadarView.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                cameraViewModel.intent(CameraIntent.ToggleRadarEnlarged)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraViewModel.radarEnlargedUpdates.collect { enlarged ->
                toggleRadarEnlarged(enlarged)
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            markerUpdates(mainViewModel, cameraViewModel).collect { (markers, firstMarkerIndex) ->
                updateARMarkers(markers, firstMarkerIndex)
            }
        }
    }

    private fun initCamera() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (preview, _, imageFlow) = cameraInitializationResult.await()
                openGLRenderer.attachInputPreview(preview, binding.cameraPreview)

                imageFlow
                    .map(ImageProxy::bitmap::get)
                    .filterNotNull()
                    .flowOn(Dispatchers.Default)
                    .collect { bitmap ->
                        launch { updateContrastingColorUsing(bitmap) }
                        if (latestARState != CameraARState.ENABLED || isRunningOnEmulator()) {
                            return@collect
                        }
                        blurAndUpdateViewsUsing(bitmap)
                    }
            } catch (ex: Exception) {
                Timber.tag("CAMERA_INIT").e(ex)
                cameraViewModel.intent(CameraIntent.CameraInitializationFailed)
            }
        }
    }

    private suspend fun updateContrastingColorUsing(bitmap: Bitmap) {
        val dominantSwatch = withContext(Dispatchers.Default) { bitmap.dominantSwatch } ?: return
        val contrastingColor = colorContrastingTo(dominantSwatch.rgb)
        withContext(Dispatchers.Main) {
            openGLRenderer.setContrastingColor(
                red = Color.red(contrastingColor),
                green = Color.green(contrastingColor),
                blue = Color.blue(contrastingColor)
            )
        }
    }

    private suspend fun blurAndUpdateViewsUsing(bitmap: Bitmap) {
        val blurred = withContext(Dispatchers.Default) { blurProcessor.blur(bitmap) }
        val blurredDominantSwatch = withContext(Dispatchers.Default) { blurred.dominantSwatch }
        mainViewModel.state.bitmapCache.put(this@CameraFragment.javaClass.name, blurred)
        with(binding) {
            blurBackground.background = BitmapDrawable(resources, blurred)
            val textColor = blurredDominantSwatch?.bodyTextColor ?: return
            locationDisabledTextView.setTextColor(textColor)
            permissionsRequiredTextView.setTextColor(textColor)
            pitchOutsideLimitTextView.setTextColor(textColor)
            cameraInitializationFailureTextView.setTextColor(textColor)
            loadingTextView.setTextColor(textColor)
        }
    }

    private fun FragmentCameraBinding.initARCameraPageViews() {
        arCameraPageUpBtn.setOnClickListener {
            if (cameraMarkerRenderer.currentPage < cameraMarkerRenderer.maxPage) {
                ++cameraMarkerRenderer.currentPage
                return@setOnClickListener
            }

            val markers = mainViewModel.state.markers
            if (markers !is WithValue) return@setOnClickListener
            if (cameraViewModel.state.firstMarkerIndex * FIRST_MARKER_INDEX_DIFF +
                    FIRST_MARKER_INDEX_DIFF < markers.value.size
            ) {
                lifecycleScope.launch {
                    cameraViewModel.intent(
                        CameraIntent.CameraMarkersFirstIndexChanged(FIRST_MARKER_INDEX_DIFF)
                    )
                }
                cameraMarkerRenderer.currentPage = 0
            }
        }

        arCameraPageDownBtn.setOnClickListener {
            if (cameraMarkerRenderer.currentPage > 0) {
                --cameraMarkerRenderer.currentPage
            } else if (cameraViewModel.state.firstMarkerIndex > 0) {
                lifecycleScope.launch {
                    cameraViewModel.intent(
                        CameraIntent.CameraMarkersFirstIndexChanged(-FIRST_MARKER_INDEX_DIFF)
                    )
                }
                cameraMarkerRenderer.currentPage = Int.MAX_VALUE
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            mainViewModel.filterSignals<MainSignal.SnackbarStatusChanged>().collect { (isShowing) ->
                val pageGroupGuidelineLayoutParams =
                    arCameraViewsGroupBottomGuideline.layoutParams as ConstraintLayout.LayoutParams
                pageGroupGuidelineLayoutParams.guideEnd =
                    if (isShowing) requireContext().dpToPx(56f + 48f).toInt()
                    else requireContext().dpToPx(56f).toInt()
                arCameraViewsGroupBottomGuideline.layoutParams = pageGroupGuidelineLayoutParams
            }
        }
    }

    private fun FragmentCameraBinding.updateARMarkers(
        markers: Loadable<ParcelableSortedSet<Marker>>,
        firstMarkerIndex: Int
    ) {
        if (markers is Empty) {
            arCameraPageUpBtn.visibility = View.GONE
            arCameraPageDownBtn.visibility = View.GONE
            cameraMarkerRenderer.setMarkers(emptyList())
            arCameraView.markers = emptyList()
            arRadarView.markers = emptyList()
        } else if (markers is WithValue) {
            val lastMarkerIndexExclusive =
                min(markers.value.size, firstMarkerIndex + FIRST_MARKER_INDEX_DIFF)
            val arMarkers =
                markers
                    .value
                    .map(::SimpleARMarker)
                    .subList(firstMarkerIndex, lastMarkerIndexExclusive)
            cameraMarkerRenderer.setMarkers(arMarkers)
            arCameraView.markers = arMarkers
            arRadarView.markers = markers.value.map(::SimpleARMarker).take(lastMarkerIndexExclusive)
        }
    }

    private fun FragmentCameraBinding.toggleRadarEnlarged(enlarged: Boolean) {
        val radarViewLayoutParams = arRadarView.layoutParams as ConstraintLayout.LayoutParams
        if (requireContext().resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        ) {
            if (enlarged) {
                radarViewLayoutParams.width = 0
                radarViewLayoutParams.leftToLeft = binding.radarViewEnlargedLeftGuideline!!.id
            } else {
                radarViewLayoutParams.width =
                    requireContext().dpToPx(RADAR_VIEW_DIMENSION_DP).toInt()
                radarViewLayoutParams.leftToLeft = -1
            }
        } else {
            if (enlarged) {
                radarViewLayoutParams.height = 0
                radarViewLayoutParams.bottomToBottom = binding.radarViewEnlargedBottomGuideline!!.id
            } else {
                radarViewLayoutParams.height =
                    requireContext().dpToPx(RADAR_VIEW_DIMENSION_DP).toInt()
                radarViewLayoutParams.bottomToBottom = -1
            }
        }

        arRadarView.markerRenderer = null
        TransitionManager.beginDelayedTransition(
            cameraLayout,
            AutoTransition().apply {
                duration = 200L
                addListener(
                    object : Transition.TransitionListener {
                        override fun onTransitionStart(transition: Transition) = Unit
                        override fun onTransitionCancel(transition: Transition) = Unit
                        override fun onTransitionPause(transition: Transition) = Unit
                        override fun onTransitionResume(transition: Transition) = Unit
                        override fun onTransitionEnd(transition: Transition) {
                            arRadarView.markerRenderer = radarMarkerRenderer
                        }
                    }
                )
            }
        )
        arRadarView.layoutParams = radarViewLayoutParams
    }

    private fun FragmentCameraBinding.onLoadingStarted() {
        hideARViews()
        cameraInitializationFailureTextView.visibility = View.GONE
        pitchOutsideLimitTextView.visibility = View.GONE
        locationDisabledTextView.visibility = View.GONE
        permissionsViewsGroup.visibility = View.GONE
        blurBackground.visibility = View.VISIBLE
        loadingShimmerLayout.showAndStart()
    }

    private fun FragmentCameraBinding.onAREnabled() {
        cameraInitializationFailureTextView.visibility = View.GONE
        pitchOutsideLimitTextView.visibility = View.GONE
        locationDisabledTextView.visibility = View.GONE
        permissionsViewsGroup.visibility = View.GONE
        loadingShimmerLayout.stopAndHide()
        blurBackground.visibility = View.GONE
        showARViews(showRadar = mainViewModel.state.markers is WithValue)
        openGLRenderer.markerRectsDisabled = false
        cameraMarkerRenderer.disabled = false
        radarMarkerRenderer.disabled = false
    }

    private fun FragmentCameraBinding.onARDisabled(
        anyPermissionDenied: Boolean,
        locationDisabled: Boolean,
        pitchOutsideLimit: Boolean,
        initializationFailure: Boolean
    ) {
        hideARViews()
        loadingShimmerLayout.stopAndHide()
        blurBackground.visibility = View.VISIBLE
        if (initializationFailure) {
            permissionsViewsGroup.visibility = View.GONE
            locationDisabledTextView.visibility = View.GONE
            pitchOutsideLimitTextView.visibility = View.GONE
            cameraInitializationFailureTextView.visibility = View.VISIBLE
            return
        }
        if (anyPermissionDenied) permissionsViewsGroup.visibility = View.VISIBLE
        if (locationDisabled) locationDisabledTextView.visibility = View.VISIBLE
        pitchOutsideLimitTextView.visibility =
            if (!anyPermissionDenied && !locationDisabled && pitchOutsideLimit) View.VISIBLE
            else View.GONE
    }

    private fun FragmentCameraBinding.onMarkersDrawn(update: CameraMarkersDrawnViewUpdate) {
        val (firstMarkerIndex, markersSize, currentPage, maxPage, cameraObscured) = update
        if (markersSize == 0) arCameraPageViewsGroup.visibility = View.GONE
        else if (!cameraObscured) arCameraPageViewsGroup.visibility = View.VISIBLE
        arCameraPageUpBtn.isEnabled =
            firstMarkerIndex * FIRST_MARKER_INDEX_DIFF + FIRST_MARKER_INDEX_DIFF < markersSize ||
                currentPage < maxPage
        arCameraPageDownBtn.isEnabled = firstMarkerIndex > 0 || currentPage > 0
    }

    override fun onResume() {
        super.onResume()
        orientationManager.startSensor(requireContext())
    }

    override fun onPause() {
        binding.blurBackground.visibility = View.VISIBLE
        cameraMarkerRenderer.disabled = true
        radarMarkerRenderer.disabled = true
        openGLRenderer.markerRectsDisabled = true
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
        if (orientation.pitchWithinLimit) {
            signalPitchChanged(true)
        } else {
            signalPitchChanged(false)
            return
        }

        with(binding) {
            arCameraView.orientation = orientation
            arCameraView.phoneRotation = requireContext().phoneRotation
            arRadarView.orientation = orientation
        }
    }

    private fun signalPitchChanged(withinLimit: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            cameraViewModel.signal(CameraSignal.PitchChanged(withinLimit))
        }
    }

    private val Orientation.pitchWithinLimit: Boolean
        get() = pitch >= -PITCH_LIMIT_RADIANS && pitch <= PITCH_LIMIT_RADIANS

    private fun signalCameraTouch() {
        viewLifecycleOwner.lifecycleScope.launch {
            cameraViewModel.signal(CameraSignal.CameraTouch)
        }
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

    private fun FragmentCameraBinding.showARViews(showRadar: Boolean) {
        if (showRadar) {
            arViewsGroup.visibility = View.VISIBLE
            arRadarView.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.radar_background)
            arRadarView.rotableBackground = R.drawable.radar_arrow
            arRadarView.disabled = false
        } else {
            arCameraView.visibility = View.VISIBLE
            arRadarView.visibility = View.GONE
            arRadarView.disableARRadarView()
        }
    }

    private fun FragmentCameraBinding.hideARViews() {
        arViewsGroup.visibility = View.GONE
        arRadarView.disableARRadarView()
        arCameraPageViewsGroup.visibility = View.GONE
    }

    private fun ARRadarView.disableARRadarView() {
        background = null
        rotableBackground = -1
        disabled = true
    }

    private fun FragmentCameraBinding.onCameraTouch() {
        val targetVisibility = visibilityToggleView.toggleVisibility()
        changeRadarViewTopGuideline(targetVisibility)
        lifecycleScope.launch {
            mainViewModel.signal(MainSignal.ToggleSearchBarVisibility(targetVisibility))
        }
    }

    companion object {
        private const val FIRST_MARKER_INDEX_DIFF = 100
        private const val PITCH_LIMIT_RADIANS = Math.PI / 3
        private const val RADAR_VIEW_DIMENSION_DP = 96f
    }
}
