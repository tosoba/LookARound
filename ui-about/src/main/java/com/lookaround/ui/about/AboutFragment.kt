package com.lookaround.ui.about

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lookaround.core.android.ext.fadeSetVisibility
import com.lookaround.ui.about.databinding.FragmentAboutBinding
import com.lookaround.ui.about.databinding.FragmentDonateBinding
import com.lookaround.ui.about.databinding.FragmentGeneralBinding
import com.lookaround.ui.about.databinding.WalletItemBinding
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
            (blurredBackground, palette) ->
            binding.aboutCoordinatorLayout.background = BitmapDrawable(resources, blurredBackground)
            val dominantSwatch = palette.dominantSwatch ?: return@let
            binding.aboutTabLayout.setTabTextColors(
                dominantSwatch.titleTextColor,
                dominantSwatch.bodyTextColor
            )
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

        binding.aboutTabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    binding.githubFab.fadeSetVisibility(
                        if (tab.position == 0) View.VISIBLE else View.GONE
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )

        binding.githubFab.setOnClickListener {}
    }

    class GeneralFragment : Fragment(R.layout.fragment_general) {
        private val binding: FragmentGeneralBinding by viewBinding(FragmentGeneralBinding::bind)
        private val mainViewModel: MainViewModel by activityViewModels()

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            mainViewModel.state.bitmapCache.get(MainState.BlurredBackgroundType.CAMERA)?.let {
                (_, palette) ->
                val dominantSwatch = palette.dominantSwatch ?: return@let
                binding.generalHiTextView.setTextColor(dominantSwatch.bodyTextColor)
                binding.generalInfoTextView.setTextColor(dominantSwatch.bodyTextColor)
            }
        }
    }

    class DonateFragment : Fragment(R.layout.fragment_donate) {
        private val binding: FragmentDonateBinding by viewBinding(FragmentDonateBinding::bind)
        private val mainViewModel: MainViewModel by activityViewModels()

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            binding.walletsRecyclerView.adapter =
                WalletsAdapter(
                    wallets =
                        listOf(
                            Wallet(getString(R.string.btc_address), R.drawable.ic_bitcoin_btc_logo)
                        ),
                    textColor =
                        mainViewModel.state.bitmapCache
                            .get(MainState.BlurredBackgroundType.CAMERA)
                            ?.let { (_, palette) ->
                                val dominantSwatch = palette.dominantSwatch ?: return
                                dominantSwatch.bodyTextColor
                            }
                )
        }

        private class WalletsAdapter(
            private val wallets: List<Wallet>,
            @ColorInt private val textColor: Int?,
        ) : RecyclerView.Adapter<WalletViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder =
                WalletViewHolder(
                    WalletItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                )

            override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
                val wallet = wallets[position]
                with(holder.binding) {
                    walletLogoImageView.setImageDrawable(
                        ContextCompat.getDrawable(holder.binding.root.context, wallet.drawableRes)
                    )
                    walletAddressTextView.text = wallet.address
                    textColor?.let(walletAddressTextView::setTextColor)
                    root.setOnClickListener {
                        Toast.makeText(
                                holder.binding.root.context,
                                holder.binding.root.context.getString(R.string.address_copied),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }

            override fun getItemCount(): Int = wallets.size
        }

        private class WalletViewHolder(val binding: WalletItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        private data class Wallet(val address: String, @DrawableRes val drawableRes: Int)
    }
}
