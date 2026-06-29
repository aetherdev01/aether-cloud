package com.aether.cloud.ui.mymodules

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aether.cloud.data.model.Module
import com.aether.cloud.databinding.ItemMyModuleBinding
import com.aether.cloud.util.formatCount

class MyModuleAdapter(private val onClick: (Module) -> Unit) :
    ListAdapter<Module, MyModuleAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemMyModuleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(module: Module) {
            binding.tvModuleName.text = module.name
            binding.tvVersion.text = "v${module.version}"
            binding.tvStats.text = "${formatCount(module.downloadCount)} downloads · ${formatCount(module.viewCount)} views"
            binding.tvType.text = module.type
            binding.root.setOnClickListener { onClick(module) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyModuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Module>() {
        override fun areItemsTheSame(oldItem: Module, newItem: Module) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Module, newItem: Module) = oldItem == newItem
    }
}
