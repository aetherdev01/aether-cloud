package com.aether.cloud.ui.mymodules

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aether.cloud.data.repository.ModuleRepository
import com.aether.cloud.databinding.FragmentMyModulesBinding
import com.aether.cloud.ui.module.ModuleDetailActivity
import com.aether.cloud.util.Resource
import com.aether.cloud.viewmodel.HomeViewModel
import com.aether.cloud.viewmodel.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyModulesFragment : Fragment() {
    private var _binding: FragmentMyModulesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModelFactory(ModuleRepository()) }
    private lateinit var adapter: MyModuleAdapter
    private var userId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyModulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = MyModuleAdapter { module ->
            val intent = Intent(requireContext(), ModuleDetailActivity::class.java).apply {
                putExtra("MODULE_ID", module.id)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyModulesFragment.adapter
        }

        userId = FirebaseAuth.getInstance().currentUser?.uid

        binding.swipeRefresh.setOnRefreshListener {
            userId?.let { observeModules(it, isRefresh = true) }
        }

        userId?.let { observeModules(it) }
    }

    private fun observeModules(userId: String, isRefresh: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val repo = ModuleRepository()
                repo.getMyModules(userId).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            if (!binding.swipeRefresh.isRefreshing) {
                                binding.includeShimmer.shimmerLayout.visibility = View.VISIBLE
                                binding.includeShimmer.shimmerLayout.startShimmer()
                                binding.recyclerView.visibility = View.GONE
                            }
                            binding.layoutEmpty.visibility = View.GONE
                        }
                        is Resource.Success -> {
                            stopShimmer()
                            binding.swipeRefresh.isRefreshing = false
                            val modules = resource.data ?: emptyList()
                            if (modules.isEmpty()) {
                                binding.recyclerView.visibility = View.GONE
                                binding.layoutEmpty.visibility = View.VISIBLE
                                binding.tvEmptyTitle.setText(com.aether.cloud.R.string.empty_my_modules_title)
                                binding.tvEmptyDesc.setText(com.aether.cloud.R.string.empty_my_modules_desc)
                            } else {
                                binding.layoutEmpty.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                adapter.submitList(modules)
                            }
                        }
                        is Resource.Error -> {
                            stopShimmer()
                            binding.swipeRefresh.isRefreshing = false
                            binding.recyclerView.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.tvEmptyTitle.text = "Gagal memuat data"
                            binding.tvEmptyDesc.text = resource.message
                        }
                    }
                }
            }
        }
    }

    private fun stopShimmer() {
        binding.includeShimmer.shimmerLayout.stopShimmer()
        binding.includeShimmer.shimmerLayout.visibility = View.GONE
    }

    override fun onDestroyView() {
        binding.includeShimmer.shimmerLayout.stopShimmer()
        super.onDestroyView()
        _binding = null
    }
}
