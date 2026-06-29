package com.aether.cloud.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aether.cloud.data.model.Module
import com.aether.cloud.databinding.ItemModuleBinding
import com.aether.cloud.util.formatCount
import com.aether.cloud.util.formatDate
import com.bumptech.glide.Glide

class ModuleAdapter(private val onClick: (Module) -> Unit) :
    ListAdapter<Module, ModuleAdapter.ModuleViewHolder>(DiffCallback()) {

    inner class ModuleViewHolder(private val binding: ItemModuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(module: Module) {
            binding.tvModuleName.text = module.name
            binding.tvAuthorName.text = module.authorName
            binding.tvDescription.text = module.description
            binding.tvDownloads.text = formatCount(module.downloadCount)
            binding.tvViews.text = formatCount(module.viewCount)
            binding.tvComments.text = formatCount(module.commentCount)
            binding.chipType.text = module.type
            binding.tvDate.text = formatDate(module.createdAt)

            if (module.authorPhoto.isNotEmpty()) {
                Glide.with(binding.root).load(module.authorPhoto).into(binding.ivAuthorPhoto)
            }

            if (module.screenshots.isNotEmpty()) {
                binding.rvScreenshots.visibility = View.VISIBLE
                val ssAdapter = ScreenshotAdapter(module.screenshots, false)
                binding.rvScreenshots.apply {
                    layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
                    adapter = ssAdapter
                }
            } else {
                binding.rvScreenshots.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(module) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Module>() {
        override fun areItemsTheSame(oldItem: Module, newItem: Module) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Module, newItem: Module) = oldItem == newItem
    }
}
