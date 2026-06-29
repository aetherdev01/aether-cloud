package com.aether.cloud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aether.cloud.data.model.User
import com.aether.cloud.data.repository.AuthRepository
import com.aether.cloud.data.repository.UserRepository
import com.aether.cloud.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _userProfile = MutableStateFlow<Resource<User>?>(null)
    val userProfile: StateFlow<Resource<User>?> = _userProfile

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            userRepository.getUserProfile(uid).collect { _userProfile.value = it }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}

class ProfileViewModelFactory(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
