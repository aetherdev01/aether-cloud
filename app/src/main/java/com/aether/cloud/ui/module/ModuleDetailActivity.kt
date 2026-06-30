package com.aether.cloud.ui.module

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.aether.cloud.data.model.Comment
import com.aether.cloud.data.repository.ModuleRepository
import com.aether.cloud.data.repository.UserRepository
import com.aether.cloud.databinding.ActivityModuleDetailBinding
import com.aether.cloud.databinding.BottomSheetCommentsBinding
import com.aether.cloud.ui.home.ScreenshotAdapter
import com.aether.cloud.util.Resource
import com.aether.cloud.util.formatCount
import com.aether.cloud.util.formatDate
import com.aether.cloud.util.formatFileSize
import com.aether.cloud.viewmodel.ModuleDetailViewModel
import com.aether.cloud.viewmodel.ModuleDetailViewModelFactory
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class ModuleDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModuleDetailBinding
    private lateinit var viewModel: ModuleDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModuleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val moduleId = intent.getStringExtra("MODULE_ID") ?: run {
            finish()
            return
        }

        val moduleRepo = ModuleRepository()
        viewModel = ModuleDetailViewModelFactory(moduleRepo, UserRepository())
            .create(ModuleDetailViewModel::class.java)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnDownload.setOnClickListener {
            viewModel.module.value?.data?.let { module ->
                if (module.fileUrl.isNotEmpty()) {
                    downloadFile(module.fileUrl, module.name)
                    viewModel.incrementDownload(moduleId)
                } else if (module.mirrorUrl.isNotEmpty()) {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(module.mirrorUrl)))
                    viewModel.incrementDownload(moduleId)
                } else {
                    Toast.makeText(this, "No download link available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnComments.setOnClickListener {
            showCommentsBottomSheet(moduleId)
        }

        observeModule(moduleId)
        viewModel.loadModule(moduleId)
        viewModel.incrementView(moduleId)
    }

    private fun observeModule(moduleId: String) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.module.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val module = resource.data ?: return@collect

                            binding.toolbar.title = module.name
                            binding.tvModuleName.text = module.name
                            binding.tvVersion.text = "v${module.version}"
                            binding.tvType.text = module.type
                            binding.tvDescription.text = module.description
                            binding.tvDownloads.text = formatCount(module.downloadCount)
                            binding.tvViews.text = formatCount(module.viewCount)
                            binding.tvComments.text = formatCount(module.commentCount)
                            binding.tvDate.text = formatDate(module.createdAt)
                            binding.tvAuthorName.text = module.authorName
                            binding.tvFileSize.text = formatFileSize(module.fileSize)

                            if (module.type == "ROOT") {
                                binding.tvType.setTextColor(getColor(com.aether.cloud.R.color.root_red))
                            } else {
                                binding.tvType.setTextColor(getColor(com.aether.cloud.R.color.noroot_green))
                            }

                            Glide.with(this@ModuleDetailActivity)
                                .load(module.authorPhoto)
                                .placeholder(com.aether.cloud.R.drawable.ic_person)
                                .into(binding.ivAuthorPhoto)

                            if (module.screenshots.isNotEmpty()) {
                                binding.rvScreenshots.visibility = View.VISIBLE
                                val adapter = ScreenshotAdapter(module.screenshots, false)
                                binding.rvScreenshots.apply {
                                    layoutManager = LinearLayoutManager(this@ModuleDetailActivity, LinearLayoutManager.HORIZONTAL, false)
                                    this.adapter = adapter
                                }
                            } else {
                                binding.rvScreenshots.visibility = View.GONE
                            }
                        }
                        is Resource.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this@ModuleDetailActivity, resource.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun showCommentsBottomSheet(moduleId: String) {
        val bottomSheet = BottomSheetDialog(this)
        val sheetBinding = BottomSheetCommentsBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        val moduleRepo = ModuleRepository()
        val adapter = CommentAdapter()
        sheetBinding.rvComments.layoutManager = LinearLayoutManager(this)
        sheetBinding.rvComments.adapter = adapter

        lifecycleScope.launch {
            moduleRepo.getComments(moduleId).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val comments = resource.data ?: emptyList()
                        adapter.submitList(comments)
                        sheetBinding.layoutEmptyComments.visibility =
                            if (comments.isEmpty()) View.VISIBLE else View.GONE
                        sheetBinding.rvComments.visibility =
                            if (comments.isEmpty()) View.GONE else View.VISIBLE
                    }
                    else -> {}
                }
            }
        }

        sheetBinding.btnSend.setOnClickListener {
            val text = sheetBinding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                val comment = Comment(
                    id = UUID.randomUUID().toString(),
                    moduleId = moduleId,
                    userId = currentUser?.uid ?: "",
                    userName = currentUser?.displayName ?: "Anonymous",
                    userPhoto = currentUser?.photoUrl?.toString() ?: "",
                    text = text
                )
                lifecycleScope.launch {
                    moduleRepo.addComment(comment)
                    sheetBinding.etComment.text?.clear()
                }
            }
        }

        bottomSheet.show()
    }

    private fun downloadFile(url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading module…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "$fileName.zip")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(this, "Download started to /sdcard/Download", Toast.LENGTH_SHORT).show()
    }
}
