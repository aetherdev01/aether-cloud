package com.aether.cloud.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aether.cloud.R
import com.aether.cloud.data.repository.AuthRepository
import com.aether.cloud.data.repository.UserRepository
import com.aether.cloud.databinding.FragmentSettingsBinding
import com.aether.cloud.ui.auth.LoginActivity
import com.aether.cloud.ui.mymodules.MyModulesFragment
import com.aether.cloud.util.Resource
import com.aether.cloud.viewmodel.ProfileViewModel
import com.aether.cloud.viewmodel.ProfileViewModelFactory
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(UserRepository(), AuthRepository(requireContext()))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardMyModules.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_activity_main, MyModulesFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardUploadModule.setOnClickListener {
            val navView = requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
            navView.selectedItemId = R.id.navigation_upload
        }

        binding.cardSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Support: t.me/aethercloud", Toast.LENGTH_SHORT).show()
        }

        binding.cardLogout.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        observeProfile()
    }

    private fun observeProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val user = resource.data
                            binding.tvName.text = user?.displayName
                            binding.tvUsername.text = "@${user?.username}"
                            binding.tvTelegram.text = user?.telegramChannel?.takeIf { it.isNotEmpty() } ?: "No Telegram"
                            binding.tvGithub.text = user?.githubUrl?.takeIf { it.isNotEmpty() } ?: "No GitHub"

                            if (!user?.photoUrl.isNullOrEmpty()) {
                                Glide.with(requireContext()).load(user?.photoUrl).into(binding.ivProfile)
                            }
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
