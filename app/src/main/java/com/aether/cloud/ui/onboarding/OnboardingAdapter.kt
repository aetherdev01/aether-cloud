package com.aether.cloud.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aether.cloud.databinding.ItemOnboardingPageBinding

data class OnboardingPage(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int
)

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    inner class PageViewHolder(val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.binding.ivIcon.setImageResource(page.iconRes)
        holder.binding.tvTitle.setText(page.titleRes)
        holder.binding.tvDescription.setText(page.descRes)
    }

    override fun getItemCount(): Int = pages.size
}
