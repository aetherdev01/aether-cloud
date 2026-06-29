package com.aether.cloud.ui.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aether.cloud.databinding.ItemScreenshotBinding
import com.bumptech.glide.Glide

class ScreenshotAdapter(
    private val items: List<Any>,
    private val isUploadMode: Boolean,
    private val onRemove: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<ScreenshotAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemScreenshotBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScreenshotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is String -> Glide.with(holder.binding.root).load(item).into(holder.binding.ivScreenshot)
            is Uri -> Glide.with(holder.binding.root).load(item).into(holder.binding.ivScreenshot)
        }

        if (isUploadMode) {
            holder.binding.btnRemove.visibility = View.VISIBLE
            holder.binding.btnRemove.setOnClickListener { onRemove?.invoke(position) }
        } else {
            holder.binding.btnRemove.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}
