package com.aether.cloud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aether.cloud.data.model.Module
import com.aether.cloud.data.repository.ModuleRepository
import com.aether.cloud.data.repository.UserRepository
import com.aether.cloud.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ModuleDetailViewModel(
    private val moduleRepository: ModuleRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _module = MutableStateFlow<Resource<Module>>(Resource.Loading())
    val module: StateFlow<Resource<Module>> = _module

    fun loadModule(moduleId: String) {
        viewModelScope.launch {
            moduleRepository.getModuleById(moduleId).collect { _module.value = it }
        }
    }

    fun incrementView(moduleId: String) {
        viewModelScope.launch { moduleRepository.incrementView(moduleId) }
    }

    fun incrementDownload(moduleId: String) {
        viewModelScope.launch { moduleRepository.incrementDownload(moduleId) }
    }
}

class ModuleDetailViewModelFactory(
    private val moduleRepository: ModuleRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModuleDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModuleDetailViewModel(moduleRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
