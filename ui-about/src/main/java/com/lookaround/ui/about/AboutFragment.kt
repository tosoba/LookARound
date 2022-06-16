package com.lookaround.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lookaround.core.android.ext.fadeSetVisibility
import com.lookaround.ui.about.databinding.*
import com.lookaround.ui.main.MainViewModel
import com.lookaround.core.android.model.BlurredBackgroundType
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
        mainViewModel.state.bitmapCache.get(BlurredBackgroundType.CAMERA)?.let {
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
            mainViewModel.state.bitmapCache.get(BlurredBackgroundType.CAMERA)?.let {
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
                            Wallet(getString(R.string.btc_address), R.drawable.ic_bitcoin_btc_logo),
                            Wallet(
                                getString(R.string.eth_address),
                                R.drawable.ic_ethereum_eth_logo
                            ),
                            Wallet(getString(R.string.bnb_address), R.drawable.ic_bnb_bnb_logo),
                            Wallet(getString(R.string.ada_address), R.drawable.ic_cardano_ada_logo),
                            Wallet(getString(R.string.sol_address), R.drawable.ic_solana_sol_logo),
                            Wallet(getString(R.string.xrp_address), R.drawable.ic_xrp_xrp_logo),
                            Wallet(getString(R.string.xmr_address), R.drawable.ic_monero_xmr_logo),
                            Wallet(
                                getString(R.string.usdt_address),
                                R.drawable.ic_tether_usdt_logo
                            ),
                            Wallet(getString(R.string.xlm_address), R.drawable.ic_stellar_xlm_logo),
                            Wallet(
                                getString(R.string.algo_address),
                                R.drawable.ic_algorand_algo_logo
                            ),
                            Wallet(getString(R.string.btc_address), R.drawable.ic_tezos_xtz_logo),
                            Wallet(getString(R.string.btc_address), R.drawable.ic_vechain_vet_logo),
                        ),
                    textColor =
                        mainViewModel.state.bitmapCache
                            .get(BlurredBackgroundType.CAMERA)
                            ?.let { (_, palette) ->
                                val dominantSwatch = palette.dominantSwatch ?: return
                                dominantSwatch.bodyTextColor
                            }
                )
        }

        private class WalletsAdapter(
            private val wallets: List<Wallet>,
            @ColorInt private val textColor: Int?,
        ) : RecyclerView.Adapter<WalletsAdapter.ViewHolder>() {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
                ViewHolder(
                    if (viewType == ViewType.HEADER.ordinal) {
                        HeaderItemBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )
                    } else {
                        WalletItemBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )
                    }
                )

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                if (position == 0) {
                    with(holder.binding as HeaderItemBinding) {
                        textColor?.let(donateCryptoTextView::setTextColor)
                    }
                    return
                }

                val wallet = wallets[position - 1]
                with(holder.binding as WalletItemBinding) {
                    walletLogoImageView.setImageDrawable(
                        ContextCompat.getDrawable(holder.binding.root.context, wallet.drawableRes)
                    )
                    walletAddressTextView.text = wallet.address
                    textColor?.let(walletAddressTextView::setTextColor)
                    root.setOnClickListener {
                        val clipboard =
                            holder.binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                        val clip =
                            ClipData.newPlainText("address", wallet.address)
                                ?: return@setOnClickListener
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                                holder.binding.root.context,
                                holder.binding.root.context.getString(R.string.address_copied),
                                Toast.LENGTH_SHORT
                            )
                            .show()
                    }
                }
            }
            override fun getItemViewType(position: Int): Int {
                return if (position == 0) ViewType.HEADER.ordinal else ViewType.WALLET.ordinal
            }

            override fun getItemCount(): Int = wallets.size + 1

            private enum class ViewType {
                HEADER,
                WALLET
            }

            private class ViewHolder(val binding: ViewBinding) :
                RecyclerView.ViewHolder(binding.root)
        }

        private data class Wallet(val address: String, @DrawableRes val drawableRes: Int)
    }
}
