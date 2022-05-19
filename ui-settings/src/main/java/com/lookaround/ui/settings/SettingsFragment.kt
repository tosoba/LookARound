package com.lookaround.ui.settings

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainState
import com.lookaround.ui.settings.databinding.FragmentSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
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
        mainViewModel.state.bitmapCache.get(MainState.BlurredBackgroundType.CAMERA)?.let {
            blurredBackground ->
            binding.settingsCoordinatorLayout.background =
                BitmapDrawable(resources, blurredBackground)
        }

        binding.settingsToolbar.setNavigationOnClickListener { activity?.onBackPressed() }
    }
}
