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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChips()
        setupSearch()
        observeModules()

        binding.fabUpload.setOnClickListener {
            val navView = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.nav_view)
            navView.selectedItemId = R.id.navigation_upload
        }

        viewModel.loadModules("ALL")
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
            viewModel.loadModules(filter)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchModules(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.loadModules("ALL")
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
                            binding.progressBar.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE
                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val modules = resource.data ?: emptyList()
                            if (modules.isEmpty()) {
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.recyclerView.visibility = View.GONE
                            } else {
                                binding.tvEmpty.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                moduleAdapter.submitList(modules)
                            }
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmpty.text = resource.message
                        }
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
