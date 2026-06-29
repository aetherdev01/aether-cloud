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

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            observeModules(userId)
        }
    }

    private fun observeModules(userId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val repo = ModuleRepository()
                repo.getMyModules(userId).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
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
                                adapter.submitList(modules)
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
