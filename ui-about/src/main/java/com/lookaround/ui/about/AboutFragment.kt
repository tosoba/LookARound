package com.lookaround.ui.about

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.ui.about.databinding.FragmentAboutBinding
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.aboutToolbar.setNavigationOnClickListener { activity?.onBackPressed() }
    }
}
