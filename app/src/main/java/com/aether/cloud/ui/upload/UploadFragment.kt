package com.aether.cloud.ui.upload

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aether.cloud.data.model.Module
import com.aether.cloud.data.repository.ModuleRepository
import com.aether.cloud.data.repository.UserRepository
import com.aether.cloud.databinding.FragmentUploadBinding
import com.aether.cloud.ui.home.ScreenshotAdapter
import com.aether.cloud.util.Resource
import com.aether.cloud.viewmodel.UploadViewModel
import com.aether.cloud.viewmodel.UploadViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class UploadFragment : Fragment() {
    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by viewModels {
        UploadViewModelFactory(ModuleRepository(), UserRepository())
    }

    private var zipUri: Uri? = null
    private val screenshots = mutableListOf<Uri>()
    private lateinit var screenshotAdapter: ScreenshotAdapter

    private val zipPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Some file managers / share targets (WhatsApp, Telegram, Drive, etc.)
            // tag .zip files with a generic mime type like application/octet-stream
            // or application/x-zip-compressed instead of application/zip. If we'd
            // launched the picker filtered strictly to "application/zip", those
            // files simply don't show up — so it *looks* like nothing happens when
            // tapping "add file". We accept any mime type here and instead validate
            // by file extension/name after the user picks something.
            val name = getFileName(it)
            if (!name.lowercase().endsWith(".zip")) {
                Toast.makeText(requireContext(), "File harus berformat .zip", Toast.LENGTH_SHORT).show()
                return@let
            }
            zipUri = it
            binding.tvZipFile.text = "Selected: $name"
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            screenshots.addAll(it)
            screenshotAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupScreenshotRecycler()

        binding.btnPickZip.setOnClickListener { zipPicker.launch("*/*") }
        binding.btnPickScreenshots.setOnClickListener { imagePicker.launch("image/*") }
        binding.btnUpload.setOnClickListener { uploadModule() }

        observeUploadState()
    }

    private fun setupScreenshotRecycler() {
        screenshotAdapter = ScreenshotAdapter(screenshots, true) { position ->
            screenshots.removeAt(position)
            screenshotAdapter.notifyDataSetChanged()
        }
        binding.rvScreenshots.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = screenshotAdapter
        }
    }

    private fun uploadModule() {
        val name = binding.etModuleName.text.toString().trim()
        val version = binding.etVersion.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val mirrorUrl = binding.etMirrorUrl.text.toString().trim()
        val type = if (binding.rbRoot.isChecked) "ROOT" else "NO_ROOT"

        if (name.isEmpty() || version.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (zipUri == null && mirrorUrl.isEmpty()) {
            Toast.makeText(requireContext(), "Please provide a file or mirror link", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val module = Module(
            id = UUID.randomUUID().toString(),
            authorId = currentUser.uid,
            authorName = currentUser.displayName ?: "Unknown",
            authorPhoto = currentUser.photoUrl?.toString() ?: "",
            name = name,
            description = description,
            version = version,
            type = type,
            mirrorUrl = mirrorUrl,
            fileSize = getFileSize(zipUri) ?: 0
        )

        viewModel.uploadModule(module, zipUri, screenshots)
    }

    private fun observeUploadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uploadState.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnUpload.isEnabled = false
                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnUpload.isEnabled = true
                            Toast.makeText(requireContext(), "Upload successful!", Toast.LENGTH_SHORT).show()
                            clearForm()
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnUpload.isEnabled = true
                            Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun clearForm() {
        binding.etModuleName.text?.clear()
        binding.etVersion.text?.clear()
        binding.etDescription.text?.clear()
        binding.etMirrorUrl.text?.clear()
        binding.tvZipFile.text = "No file selected"
        zipUri = null
        screenshots.clear()
        screenshotAdapter.notifyDataSetChanged()
    }

    private fun getFileName(uri: Uri): String {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex) ?: "unknown.zip"
        } ?: "unknown.zip"
    }

    private fun getFileSize(uri: Uri?): Long? {
        uri ?: return null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            it.moveToFirst()
            try { it.getLong(sizeIndex) } catch (_: Exception) { null }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
