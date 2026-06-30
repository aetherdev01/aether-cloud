package com.aether.cloud.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.aether.cloud.R
import com.aether.cloud.databinding.ActivityOnboardingBinding
import com.aether.cloud.ui.auth.LoginActivity
import com.aether.cloud.util.AppPreferences
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: AppPreferences
    private lateinit var adapter: OnboardingAdapter

    private val pages = listOf(
        OnboardingPage(R.drawable.ic_rocket, R.string.onboarding_title_1, R.string.onboarding_desc_1),
        OnboardingPage(R.drawable.ic_upload, R.string.onboarding_title_2, R.string.onboarding_desc_2),
        OnboardingPage(R.drawable.ic_comment, R.string.onboarding_title_3, R.string.onboarding_desc_3),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = AppPreferences(this)
        adapter = OnboardingAdapter(pages)

        binding.viewPager.adapter = adapter
        setupIndicator()
        updateIndicator(0)
        updateButtonLabel(0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
                updateButtonLabel(position)
            }
        })

        binding.tvSkip.setOnClickListener { finishOnboarding() }

        binding.btnNext.setOnClickListener {
            val next = binding.viewPager.currentItem + 1
            if (next < pages.size) {
                binding.viewPager.currentItem = next
            } else {
                finishOnboarding()
            }
        }
    }

    private fun setupIndicator() {
        binding.layoutIndicator.removeAllViews()
        val dotSize = resources.getDimensionPixelSize(R.dimen.dot_size_inactive)
        repeat(pages.size) { index ->
            val dot = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginStart = 6
                    marginEnd = 6
                }
                setBackgroundResource(R.drawable.dot_indicator_inactive)
                tag = "dot_$index"
            }
            binding.layoutIndicator.addView(dot)
        }
    }

    private fun updateIndicator(position: Int) {
        val activeWidth = resources.getDimensionPixelSize(R.dimen.dot_width_active)
        val inactiveSize = resources.getDimensionPixelSize(R.dimen.dot_size_inactive)
        for (i in pages.indices) {
            val dot = binding.layoutIndicator.findViewWithTag<View>("dot_$i") ?: continue
            val isActive = i == position
            dot.setBackgroundResource(
                if (isActive) R.drawable.dot_indicator_active else R.drawable.dot_indicator_inactive
            )
            val params = dot.layoutParams as android.widget.LinearLayout.LayoutParams
            params.width = if (isActive) activeWidth else inactiveSize
            dot.layoutParams = params
        }
    }

    private fun updateButtonLabel(position: Int) {
        val isLast = position == pages.size - 1
        binding.btnNext.setText(
            if (isLast) R.string.onboarding_get_started else R.string.onboarding_next
        )
        binding.tvSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            prefs.setOnboardingDone(true)
            startActivity(Intent(this@OnboardingActivity, LoginActivity::class.java))
            finish()
        }
    }
}
