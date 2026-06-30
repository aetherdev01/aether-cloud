package com.aether.cloud.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aether.cloud.MainActivity
import com.aether.cloud.data.model.User
import com.aether.cloud.data.repository.AuthRepository
import com.aether.cloud.databinding.ActivityProfileSetupBinding
import com.aether.cloud.util.Resource
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileSetupBinding
    private var photoUri: Uri? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it
            Glide.with(this)
                .load(it)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefillFromGoogleAccount()

        binding.ivProfile.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // Clear inline errors as the user types
        binding.etDisplayName.doOnTextChanged { binding.etDisplayName.error = null }
        binding.etUsername.doOnTextChanged { binding.tilUsername.error = null }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun prefillFromGoogleAccount() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        currentUser.displayName?.let { name ->
            binding.etDisplayName.setText(name)
        }

        currentUser.photoUrl?.let { photo ->
            Glide.with(this)
                .load(photo)
                .placeholder(binding.ivProfile.drawable)
                .circleCrop()
                .into(binding.ivProfile)
        }

        // Suggest a username from the email or display name, e.g. "john.doe" -> "johndoe"
        val suggestion = currentUser.email?.substringBefore("@")
            ?: currentUser.displayName?.replace(" ", "")
        suggestion?.let {
            binding.etUsername.setText(it.lowercase().replace(Regex("[^a-z0-9_]"), ""))
        }
    }

    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        val displayName = binding.etDisplayName.text.toString().trim()
        val telegram = binding.etTelegram.text.toString().trim()
        val github = binding.etGithub.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()

        var hasError = false

        if (displayName.isEmpty()) {
            binding.etDisplayName.error = "Name is required"
            hasError = true
        }

        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            hasError = true
        } else if (!username.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))) {
            binding.tilUsername.error = "3-20 characters: letters, numbers, underscore only"
            hasError = true
        }

        if (hasError) return

        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.btnSave.isEnabled = false

                var photoUrl = currentUser.photoUrl?.toString() ?: ""

                if (photoUri != null) {
                    val ref = FirebaseStorage.getInstance().reference
                        .child("users/${currentUser.uid}/profile.jpg")
                    ref.putFile(photoUri!!).await()
                    photoUrl = ref.downloadUrl.await().toString()
                }

                val user = User(
                    uid = currentUser.uid,
                    username = username,
                    displayName = displayName,
                    email = currentUser.email ?: "",
                    photoUrl = photoUrl,
                    telegramChannel = telegram,
                    githubUrl = github,
                    website = website,
                    isDeveloper = true
                )

                val authRepo = AuthRepository(this@ProfileSetupActivity)
                val result = authRepo.createUserProfile(user)

                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSave.isEnabled = true

                if (result is Resource.Success) {
                    Toast.makeText(this@ProfileSetupActivity, "Profile saved!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@ProfileSetupActivity, MainActivity::class.java))
                    finish()
                } else {
                    val msg = (result as? Resource.Error)?.message ?: "Unknown error"
                    Toast.makeText(
                        this@ProfileSetupActivity,
                        "Gagal menyimpan profil: $msg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(this@ProfileSetupActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun android.widget.EditText.doOnTextChanged(action: () -> Unit) {
    addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { action() }
        override fun afterTextChanged(s: android.text.Editable?) {}
    })
}
