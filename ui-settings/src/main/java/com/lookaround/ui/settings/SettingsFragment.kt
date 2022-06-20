package com.lookaround.ui.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.core.android.ext.getBlurredBackgroundDrawable
import com.lookaround.core.android.ext.setNightMode
import com.lookaround.core.android.map.LocationBitmapCaptureCache
import com.lookaround.core.android.model.BlurredBackgroundType
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.settings.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private val binding: FragmentSettingsBinding by viewBinding(FragmentSettingsBinding::bind)
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settingsCoordinatorLayout.background =
            mainViewModel.state.bitmapCache.get(BlurredBackgroundType.CAMERA)?.let {
                (blurredBackground) ->
                BitmapDrawable(resources, blurredBackground)
            }
                ?: run { requireContext().getBlurredBackgroundDrawable(BlurredBackgroundType.CAMERA) }
                    ?: run { ContextCompat.getDrawable(requireContext(), R.drawable.background) }

        binding.settingsToolbar.setNavigationOnClickListener { activity?.onBackPressed() }
    }

    @AndroidEntryPoint
    @WithFragmentBindings
    class PreferencesFragment : PreferenceFragmentCompat() {
        @Inject internal lateinit var locationBitmapCaptureCache: LocationBitmapCaptureCache

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            findPreference<Preference>(getString(R.string.preference_clear_cache_key))?.apply {
                setOnPreferenceClickListener {
                    locationBitmapCaptureCache.clear()
                    true
                }
            }
            findPreference<Preference>(getString(R.string.preference_theme_key))?.apply {
                setOnPreferenceChangeListener { _, newValue ->
                    requireContext().setNightMode(newValue as String)
                    true
                }
            }
        }
    }
}
