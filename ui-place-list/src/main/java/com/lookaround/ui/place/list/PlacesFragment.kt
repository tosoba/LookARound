package com.lookaround.ui.place.list

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.ui.place.list.databinding.FragmentPlacesBinding
import com.lookaround.ui.place.map.list.PlaceMapListFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class PlacesFragment : Fragment(R.layout.fragment_places) {
    private val binding: FragmentPlacesBinding by viewBinding(FragmentPlacesBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fragments = arrayOf(PlaceListFragment(), PlaceMapListFragment())
        with(binding.placesViewPager) {
            offscreenPageLimit = fragments.size - 1
            adapter =
                object : FragmentStateAdapter(this@PlacesFragment) {
                    override fun getItemCount(): Int = fragments.size
                    override fun createFragment(position: Int): Fragment = fragments[position]
                }
        }
        binding.toggleMapsFab.setOnClickListener {
            binding.placesViewPager.currentItem =
                if (binding.placesViewPager.currentItem == 0) 1 else 0
        }
    }
}
