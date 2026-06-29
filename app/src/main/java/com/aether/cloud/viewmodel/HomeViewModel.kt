package com.aether.cloud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aether.cloud.data.model.Module
import com.aether.cloud.data.repository.ModuleRepository
import com.aether.cloud.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ModuleRepository) : ViewModel() {
    private val _modules = MutableStateFlow<Resource<List<Module>>>(Resource.Loading())
    val modules: StateFlow<Resource<List<Module>>> = _modules

    fun loadModules(filter: String) {
        viewModelScope.launch {
            repository.getModules(filter).collect { _modules.value = it }
        }
    }

    fun searchModules(query: String) {
        viewModelScope.launch {
            _modules.value = Resource.Loading()
            repository.searchModules(query).collect { _modules.value = it }
        }
    }
}

class HomeViewModelFactory(private val repository: ModuleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
