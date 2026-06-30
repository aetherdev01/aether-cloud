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
            Glide.with(this).load(it).into(binding.ivProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivProfile.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        val displayName = binding.etDisplayName.text.toString().trim()
        val telegram = binding.etTelegram.text.toString().trim()
        val github = binding.etGithub.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()

        if (username.isEmpty() || displayName.isEmpty()) {
            Toast.makeText(this, "Username and Name are required", Toast.LENGTH_SHORT).show()
            return
        }

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
                        "Gagal menyimpan profil: $msg\nPeriksa Firestore Security Rules.",
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
