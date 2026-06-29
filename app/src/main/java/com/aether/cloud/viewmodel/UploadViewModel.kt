package com.aether.cloud.viewmodel

import android.net.Uri
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

class UploadViewModel(
    private val moduleRepository: ModuleRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uploadState = MutableStateFlow<Resource<String>>(Resource.Success(""))
    val uploadState: StateFlow<Resource<String>> = _uploadState

    fun uploadModule(module: Module, zipUri: Uri?, screenshots: List<Uri>) {
        viewModelScope.launch {
            _uploadState.value = Resource.Loading()
            val result = moduleRepository.uploadModule(module, zipUri, screenshots)
            _uploadState.value = result
        }
    }
}

class UploadViewModelFactory(
    private val moduleRepository: ModuleRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UploadViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UploadViewModel(moduleRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
