package com.aether.cloud.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aether.cloud.MainActivity
import com.aether.cloud.data.repository.AuthRepository
import com.aether.cloud.databinding.ActivityLoginBinding
import com.aether.cloud.util.Resource
import com.aether.cloud.viewmodel.AuthViewModel
import com.aether.cloud.viewmodel.AuthViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel
    private lateinit var authRepository: AuthRepository

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Google sign in failed: ID token is null", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(this)
        viewModel = AuthViewModelFactory(authRepository).create(AuthViewModel::class.java)

        if (authRepository.getCurrentUser() != null) {
            checkProfileAndProceed()
            return
        }

        binding.btnGoogle.setOnClickListener {
            val signInIntent = authRepository.getGoogleSignInIntent()
            googleSignInLauncher.launch(signInIntent)
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(this, "Facebook Login requires setup in strings.xml", Toast.LENGTH_LONG).show()
        }

        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = android.view.View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = android.view.View.GONE
                            checkProfileAndProceed()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = android.view.View.GONE
                            Toast.makeText(this@LoginActivity, resource.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun checkProfileAndProceed() {
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                val hasProfile = authRepository.checkUserProfile(currentUser.uid)
                if (hasProfile) {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this@LoginActivity, ProfileSetupActivity::class.java))
                    finish()
                }
            }
        }
    }
}
