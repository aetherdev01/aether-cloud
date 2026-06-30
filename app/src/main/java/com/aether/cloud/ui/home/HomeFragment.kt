package com.aether.cloud.ui.home

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
import com.aether.cloud.R
import com.aether.cloud.data.model.Module
import com.aether.cloud.data.repository.ModuleRepository
import com.aether.cloud.databinding.FragmentHomeBinding
import com.aether.cloud.ui.module.ModuleDetailActivity
import com.aether.cloud.util.Resource
import com.aether.cloud.viewmodel.HomeViewModel
import com.aether.cloud.viewmodel.HomeViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModelFactory(ModuleRepository()) }
    private lateinit var moduleAdapter: ModuleAdapter
    private var currentFilter = "ALL"
    private var currentQuery: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChips()
        setupSearch()
        setupSwipeRefresh()
        observeModules()

        binding.fabUpload.setOnClickListener {
            val navView = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.nav_view)
            navView.selectedItemId = R.id.navigation_upload
        }

        viewModel.loadModules("ALL")
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val query = currentQuery
            if (!query.isNullOrEmpty()) {
                viewModel.searchModules(query)
            } else {
                viewModel.loadModules(currentFilter)
            }
        }
    }

    private fun setupRecyclerView() {
        moduleAdapter = ModuleAdapter { module ->
            val intent = Intent(requireContext(), ModuleDetailActivity::class.java).apply {
                putExtra("MODULE_ID", module.id)
            }
            startActivity(intent)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moduleAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupChips() {
        binding.chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val filter = when (checkedId) {
                R.id.chipPopular -> "POPULAR"
                R.id.chipRoot -> "ROOT"
                R.id.chipNoRoot -> "NO_ROOT"
                else -> "ALL"
            }
            currentFilter = filter
            currentQuery = null
            viewModel.loadModules(filter)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    currentQuery = it
                    viewModel.searchModules(it)
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    currentQuery = null
                    viewModel.loadModules(currentFilter)
                }
                return true
            }
        })
    }

    private fun observeModules() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.modules.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            // Only show the shimmer skeleton for the *first* load;
                            // a pull-to-refresh already has its own spinner and
                            // shouldn't replace existing content with placeholders.
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
                                showEmptyState()
                            } else {
                                binding.layoutEmpty.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                moduleAdapter.submitList(modules)
                            }
                        }
                        is Resource.Error -> {
                            stopShimmer()
                            binding.swipeRefresh.isRefreshing = false
                            binding.recyclerView.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.tvEmptyTitle.text = "Gagal memuat modul"
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

    private fun showEmptyState() {
        binding.recyclerView.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        if (!currentQuery.isNullOrEmpty()) {
            binding.tvEmptyTitle.setText(R.string.empty_search_title)
            binding.tvEmptyDesc.setText(R.string.empty_search_desc)
        } else {
            binding.tvEmptyTitle.setText(R.string.empty_modules_title)
            binding.tvEmptyDesc.setText(R.string.empty_modules_desc)
        }
    }

    override fun onDestroyView() {
        binding.includeShimmer.shimmerLayout.stopShimmer()
        super.onDestroyView()
        _binding = null
    }
}
