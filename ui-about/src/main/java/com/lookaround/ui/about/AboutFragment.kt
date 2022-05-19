package com.lookaround.ui.about

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.ui.about.databinding.FragmentAboutBinding
import com.lookaround.ui.main.MainViewModel
import com.lookaround.ui.main.model.MainState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
@WithFragmentBindings
class AboutFragment : Fragment(R.layout.fragment_about) {
    private val binding: FragmentAboutBinding by viewBinding(FragmentAboutBinding::bind)
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainViewModel.state.bitmapCache.get(MainState.BlurredBackgroundType.CAMERA)?.let {
            blurredBackground ->
            binding.aboutCoordinatorLayout.background = BitmapDrawable(resources, blurredBackground)
        }

        binding.aboutToolbar.setNavigationOnClickListener { activity?.onBackPressed() }
    }
}
