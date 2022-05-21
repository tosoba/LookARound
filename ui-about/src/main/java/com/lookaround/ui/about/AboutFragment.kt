package com.lookaround.ui.about

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.tabs.TabLayoutMediator
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

        binding.aboutViewPager.adapter =
            object : FragmentStateAdapter(requireActivity()) {
                override fun getItemCount(): Int = 2

                override fun createFragment(position: Int): Fragment =
                    when (position) {
                        0 -> GeneralFragment()
                        1 -> DonateFragment()
                        else -> throw IllegalArgumentException()
                    }
            }

        TabLayoutMediator(binding.aboutTabLayout, binding.aboutViewPager) { tab, position ->
                tab.text =
                    when (position) {
                        0 -> getString(R.string.general)
                        1 -> getString(R.string.donate)
                        else -> throw IllegalArgumentException()
                    }
            }
            .attach()
    }

    class GeneralFragment : Fragment(R.layout.fragment_general) {}

    class DonateFragment : Fragment(R.layout.fragment_donate) {}
}
