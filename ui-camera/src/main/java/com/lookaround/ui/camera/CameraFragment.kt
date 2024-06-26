package com.lookaround.ui.camera

import android.Manifest
import android.content.SharedPreferences
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
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.hoko.blur.HokoBlur
import com.hoko.blur.processor.BlurProcessor
import com.imxie.exvpbs.ViewPagerBottomSheetBehavior
import com.lookaround.core.android.ar.marker.ARMarker
import com.lookaround.core.android.ar.marker.SimpleARMarker
import com.lookaround.core.android.ar.orientation.Orientation
import com.lookaround.core.android.ar.orientation.OrientationManager
import com.lookaround.core.android.ar.renderer.impl.CameraMarkerRenderer
import com.lookaround.core.android.ar.renderer.impl.RadarMarkerRenderer
import com.lookaround.core.android.ar.view.ARRadarView
import com.lookaround.core.android.architecture.filterSignals
import com.lookaround.core.android.architecture.mapStates
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
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlin.math.min
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

@AndroidEntryPoint
@WithFragmentBindings
@FlowPreview
@ExperimentalCoroutinesApi
class CameraFragment :
    Fragment(R.layout.fragment_camera), OrientationManager.OnOrientationChangedListener {
    private val binding: FragmentCameraBinding by viewBinding(FragmentCameraBinding::bind)

    private val cameraViewModel: CameraViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private val cameraMarkerRenderer: CameraMarkerRenderer by
        lazy(LazyThreadSafetyMode.NONE) { CameraMarkerRenderer(requireContext()) }
    private val radarMarkerRenderer: RadarMarkerRenderer by
        lazy(LazyThreadSafetyMode.NONE, ::RadarMarkerRenderer)

    private val defaultSharedPreferences by
        lazy(LazyThreadSafetyMode.NONE) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        }

    private val SharedPreferences.smoothFactor: Float
        get() = getInt(getString(R.string.preference_sensitivity_key), 5).toFloat() * .002f

    private val orientationManager: OrientationManager by
        lazy(LazyThreadSafetyMode.NONE) {
            OrientationManager().apply {
                axisMode = OrientationManager.Mode.AR
                onOrientationChangedListener = this@CameraFragment
                smoothFactor = defaultSharedPreferences.smoothFactor
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

    private val blurBackgroundVisibilityFlow = MutableSharedFlow<Int>()

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
        binding.blurBackground.background =
            mainViewModel.state.bitmapCache.get(BlurredBackgroundType.CAMERA)?.let {
                (blurredBackground) ->
                BitmapDrawable(resources, blurredBackground)
            }
                ?: run {
                    requireContext().getBlurredBackgroundDrawable(BlurredBackgroundType.CAMERA)
                }
                    ?: run { ContextCompat.getDrawable(requireContext(), R.drawable.background) }

        blurBackgroundVisibilityFlow
            .distinctUntilChanged()
            .onEach(binding.blurBackground::setVisibility)
            .launchIn(viewLifecycleOwner.lifecycleScope)

        lifecycleScope.launch { cameraViewModel.intent(CameraIntent.CameraViewCreated) }

        arDisabledUpdates(mainViewModel, cameraViewModel)
            .onEach {
                (
                    anyPermissionDenied,
                    googlePlayServicesNotAvailable,
                    locationDisabled,
                    pitchOutsideLimit,
                    initializationFailure) ->
                mainViewModel.signal(MainSignal.ARDisabled)
                binding.onARDisabled(
                    anyPermissionDenied = anyPermissionDenied,
                    googlePlayServicesNotAvailable = googlePlayServicesNotAvailable,
                    locationDisabled = locationDisabled,
                    pitchOutsideLimit = pitchOutsideLimit,
                    initializationFailure = initializationFailure
                )
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraTouchUpdates(mainViewModel, cameraViewModel).collect { binding.onCameraTouch() }
        }

        initARWithPermissionCheck()
    }

    private fun initARWithPermissionCheck() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    getString(R.string.permissions_required_for_ar),
                    getString(android.R.string.ok)
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    getString(R.string.permissions_required_for_ar),
                    getString(android.R.string.ok),
                    getString(android.R.string.cancel)
                )
            }
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    initAR()
                    return@request
                }

                if (
                    deniedList.contains(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ) || deniedList.contains(Manifest.permission.ACCESS_FINE_LOCATION)
                ) {
                    lifecycleScope.launch {
                        mainViewModel.intent(MainIntent.LocationPermissionDenied)
                    }
                }
                if (deniedList.contains(Manifest.permission.CAMERA)) {
                    lifecycleScope.launch {
                        cameraViewModel.intent(CameraIntent.CameraPermissionDenied)
                    }
                }
            }
    }

    private fun initAR() {
        lifecycleScope.launch { mainViewModel.intent(MainIntent.LocationPermissionGranted) }

        mainViewModel.locationReadyUpdates
            .onEach { location ->
                binding.arCameraView.povLocation = location
                binding.arRadarView.povLocation = location
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        merge(
                loadingStartedUpdates(mainViewModel, cameraViewModel),
                arEnabledUpdates(mainViewModel, cameraViewModel)
            )
            .onEach {
                latestARState =
                    when (it) {
                        is ARUpdate.Enabled -> {
                            mainViewModel.signal(MainSignal.AREnabled)
                            binding.onAREnabled(it.showingAnyMarkers)
                            CameraARState.ENABLED
                        }
                        is ARUpdate.Loading -> {
                            mainViewModel.signal(MainSignal.ARLoading)
                            binding.onLoadingStarted()
                            CameraARState.LOADING
                        }
                    }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.initARViews()
    }

    private fun FragmentCameraBinding.initARViews() {
        initARCameraPageViews()

        if (!initCamera()) return

        openGLRenderer.previewStreamStates
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
                        .map { if (it is WithValue) it.value.size else 0 }
                        .distinctUntilChanged(),
                    cameraObscuredUpdates(mainViewModel, cameraViewModel)
                        .map(CameraObscuredUpdate::obscured::get)
                        .distinctUntilChanged()
                ) { markersDrawn, firstMarkerIndex, markersSize, cameraObscured ->
                    val (currentPage, maxPage) = markersDrawn
                    CameraMarkersDrawnViewUpdate(
                        firstMarkerIndex = firstMarkerIndex,
                        markersSize = markersSize,
                        currentPage = currentPage,
                        maxPage = maxPage,
                        cameraObscured = cameraObscured
                    )
                }
            markersDrawnUpdates.distinctUntilChanged().collect { onMarkersDrawn(it) }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraMarkerRenderer.drawnRectsFlow.collect(openGLRenderer::setMarkerRects)
        }
        openGLRenderer.otherRects =
            listOf(
                RoundedRectF(
                    requireContext().bottomNavigationViewRectF,
                    OpenGLRenderer.MARKER_RECT_CORNER_RADIUS
                )
            )

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            cameraViewObscuredUpdates(mainViewModel, cameraViewModel).collectIndexed {
                index,
                (obscured, showingAnyMarkers) ->
                val useDynamicBlur =
                    defaultSharedPreferences.getBoolean(
                        getString(R.string.preference_dynamic_camera_blur_key),
                        true
                    )
                if (useDynamicBlur) {
                    if (obscured) binding.blurBackground.visibility = View.GONE
                    openGLRenderer.setBlurEnabled(
                        enabled = obscured,
                        animated = !obscured || index > 0
                    )
                } else {
                    openGLRenderer.setBlurEnabled(enabled = false, animated = false)
                    if (obscured) {
                        blurBackgroundVisibilityFlow.emit(View.VISIBLE)
                    } else if (latestARState == CameraARState.ENABLED) {
                        blurBackgroundVisibilityFlow.emit(View.GONE)
                    }
                }

                if (obscured) disableAR() else enableAR(showingAnyMarkers)
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

    private fun initCamera(): Boolean {
        if (!startSensor()) return false

        openGLRenderer.oglFatalErrorsFlow
            .onEach { cameraViewModel.intent(CameraIntent.CameraInitializationFailed) }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (preview, _, imageFlow) = cameraInitializationResult.await()
                openGLRenderer.attachInputPreview(preview, binding.cameraPreview)
                if (isRunningOnEmulator()) return@launch

                var initial = true
                imageFlow
                    .map(ImageProxy::bitmap::get)
                    .filterNotNull()
                    .filter {
                        val mainState = mainViewModel.state
                        !mainState.drawerOpen &&
                            mainState.lastLiveBottomSheetState ==
                                ViewPagerBottomSheetBehavior.STATE_HIDDEN &&
                            latestARState == CameraARState.ENABLED
                    }
                    .map(blurProcessor::blurAndGeneratePalette)
                    .flowOn(Dispatchers.Default)
                    .collect { (blurred, palette) ->
                        updateContrastingColorUsing(palette)
                        blurAndUpdateViewsUsing(blurred, palette)
                        if (initial) {
                            withContext(Dispatchers.IO) {
                                requireContext()
                                    .storeBlurredBackground(blurred, BlurredBackgroundType.CAMERA)
                            }
                            initial = false
                        }
                    }
            } catch (ex: Exception) {
                Timber.tag("CAMERA_INIT").e(ex)
                cameraViewModel.intent(CameraIntent.CameraInitializationFailed)
            }
        }

        return true
    }

    private fun startSensor(): Boolean {
        orientationManager.smoothFactor = defaultSharedPreferences.smoothFactor
        if (!orientationManager.startSensor(requireContext())) {
            lifecycleScope.launch {
                cameraViewModel.intent(CameraIntent.CameraInitializationFailed)
            }
            return false
        }

        return true
    }

    private suspend fun updateContrastingColorUsing(palette: Palette) {
        val contrastingColor = colorContrastingTo(palette.dominantSwatch?.rgb ?: return)
        withContext(Dispatchers.Main) {
            openGLRenderer.setContrastingColor(
                red = Color.red(contrastingColor),
                green = Color.green(contrastingColor),
                blue = Color.blue(contrastingColor)
            )
            mainViewModel.signal(MainSignal.ContrastingColorUpdated(contrastingColor))
        }
    }

    private suspend fun blurAndUpdateViewsUsing(blurred: Bitmap, palette: Palette) {
        mainViewModel.state.bitmapCache.put(BlurredBackgroundType.CAMERA, blurred to palette)
        with(binding) {
            val blurBackgroundDrawable = BitmapDrawable(resources, blurred)
            blurBackground.background = blurBackgroundDrawable
            mainViewModel.signal(MainSignal.BlurBackgroundUpdated(blurBackgroundDrawable, palette))
            val textColor = palette.dominantSwatch?.bodyTextColor ?: return
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
            if (
                cameraViewModel.state.firstMarkerIndex * FIRST_MARKER_INDEX_DIFF +
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
            mainViewModel
                .filterSignals(MainSignal.SnackbarStatusChanged::isShowing)
                .distinctUntilChanged()
                .collectLatest { isShowing ->
                    val bottomViewsGuidelineLayoutParams =
                        bottomViewsGuideline.layoutParams as ConstraintLayout.LayoutParams
                    bottomViewsGuidelineLayoutParams.guideEnd =
                        requireContext().dpToPx(if (isShowing) 56f + 48f else 56f).toInt()
                    bottomViewsGuideline.layoutParams = bottomViewsGuidelineLayoutParams
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
                markers.value
                    .map(::SimpleARMarker)
                    .subList(firstMarkerIndex, lastMarkerIndexExclusive)
            cameraMarkerRenderer.setMarkers(arMarkers)
            arCameraView.markers = arMarkers
            arRadarView.markers = markers.value.map(::SimpleARMarker).take(lastMarkerIndexExclusive)
            showARViews(showRadar = true)
        }
    }

    private fun FragmentCameraBinding.toggleRadarEnlarged(enlarged: Boolean) {
        val radarViewLayoutParams = arRadarView.layoutParams as ConstraintLayout.LayoutParams
        if (
            requireContext().resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        ) {
            if (enlarged) {
                radarViewLayoutParams.width = 0
                radarViewLayoutParams.rightToLeft = binding.radarViewEnlargedRightGuideline!!.id
            } else {
                radarViewLayoutParams.width =
                    requireContext().dpToPx(RADAR_VIEW_DIMENSION_DP).toInt()
                radarViewLayoutParams.rightToLeft = -1
            }
        } else {
            if (enlarged) {
                radarViewLayoutParams.height = 0
                radarViewLayoutParams.topToTop = binding.radarViewEnlargedTopGuideline!!.id
            } else {
                radarViewLayoutParams.height =
                    requireContext().dpToPx(RADAR_VIEW_DIMENSION_DP).toInt()
                radarViewLayoutParams.topToTop = -1
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
                            arRadarView.markerRenderer =
                                radarMarkerRenderer.also { it.enlarged = enlarged }
                        }
                    }
                )
            }
        )
        arRadarView.layoutParams = radarViewLayoutParams
        arRadarView.background =
            ContextCompat.getDrawable(
                requireContext(),
                if (enlarged) R.drawable.radar_background_large
                else R.drawable.radar_background_small
            )
    }

    private fun FragmentCameraBinding.onLoadingStarted() {
        binding.cameraLayout.setOnClickListener(null)
        hideARViews()
        toggleARDisabledViewsVisibility()
        viewLifecycleOwner.lifecycleScope.launch { blurBackgroundVisibilityFlow.emit(View.VISIBLE) }
        loadingShimmerLayout.showAndStart()
    }

    private fun FragmentCameraBinding.onAREnabled(showingAnyMarkers: Boolean) {
        binding.cameraLayout.setOnClickListener(null)
        toggleARDisabledViewsVisibility()
        loadingShimmerLayout.stopAndHide()
        viewLifecycleOwner.lifecycleScope.launch { blurBackgroundVisibilityFlow.emit(View.GONE) }
        enableAR(showingAnyMarkers)
    }

    private fun FragmentCameraBinding.enableAR(showingAnyMarkers: Boolean) {
        openGLRenderer.markerRectsDisabled = false
        cameraMarkerRenderer.disabled = false
        radarMarkerRenderer.disabled = false
        showARViews(showRadar = showingAnyMarkers)
    }

    private fun FragmentCameraBinding.onARDisabled(
        anyPermissionDenied: Boolean,
        googlePlayServicesNotAvailable: Boolean,
        locationDisabled: Boolean,
        pitchOutsideLimit: Boolean,
        initializationFailure: Boolean
    ) {
        disableAR()
        loadingShimmerLayout.stopAndHide()
        viewLifecycleOwner.lifecycleScope.launch { blurBackgroundVisibilityFlow.emit(View.VISIBLE) }
        toggleARDisabledViewsVisibility(
            when {
                initializationFailure -> cameraInitializationFailureTextView
                googlePlayServicesNotAvailable -> googlePlayServicesNotAvailableTextView
                locationDisabled -> locationDisabledTextView
                pitchOutsideLimit -> pitchOutsideLimitTextView
                anyPermissionDenied -> permissionsRequiredTextView
                else -> throw IllegalStateException()
            }
        )
        if (anyPermissionDenied) {
            binding.cameraLayout.setOnClickListener { initARWithPermissionCheck() }
        }

        latestARState = CameraARState.DISABLED
    }

    private fun FragmentCameraBinding.disableAR() {
        hideARViews()
        openGLRenderer.markerRectsDisabled = true
        cameraMarkerRenderer.disabled = true
        radarMarkerRenderer.disabled = true
    }

    private fun FragmentCameraBinding.toggleARDisabledViewsVisibility(vararg visibleViews: View) {
        val visibleViewIds = visibleViews.map(View::getId).toSet()
        val (visible, gone) =
            arrayOf(
                    cameraInitializationFailureTextView,
                    pitchOutsideLimitTextView,
                    locationDisabledTextView,
                    googlePlayServicesNotAvailableTextView,
                    permissionsRequiredTextView,
                )
                .partition { visibleViewIds.contains(it.id) }
        gone.forEach { it.visibility = View.GONE }
        visible.forEach { it.visibility = View.VISIBLE }
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
        startSensor()
    }

    override fun onPause() {
        binding.blurBackground.visibility = View.VISIBLE
        binding.disableAR()
        orientationManager.stopSensor()
        super.onPause()
    }

    override fun onDestroy() {
        openGLRenderer.shutdown()
        super.onDestroy()
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
        get() = pitch in -PITCH_LIMIT_RADIANS..PITCH_LIMIT_RADIANS

    private fun signalCameraTouch() {
        viewLifecycleOwner.lifecycleScope.launch {
            cameraViewModel.signal(CameraSignal.CameraTouch)
        }
    }

    private fun onMarkerPressed(marker: ARMarker) {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            mainViewModel.signal(MainSignal.ShowMapFragment(marker.wrapped))
        }
    }

    private fun FragmentCameraBinding.showARViews(showRadar: Boolean) {
        if (showRadar) {
            arViewsGroup.visibility = View.VISIBLE
            arRadarView.background =
                ContextCompat.getDrawable(
                    requireContext(),
                    if (cameraViewModel.state.radarEnlarged) R.drawable.radar_background_large
                    else R.drawable.radar_background_small
                )
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
