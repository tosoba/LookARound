package com.lookaround.ui.place.list

import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.lookaround.ui.place.list.databinding.FragmentPlaceListBinding

class PlaceListFragment : Fragment(R.layout.fragment_place_list) {
    private val binding: FragmentPlaceListBinding by viewBinding(FragmentPlaceListBinding::bind)
}
