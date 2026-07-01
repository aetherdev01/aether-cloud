package com.aether.x.ui.tweak

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.x.R
import com.aether.x.core.display.DisplayInfo
import com.aether.x.core.display.DisplayInfoProvider
import com.aether.x.core.permission.PrivilegeManager
import com.aether.x.data.AetherXPreferences
import com.aether.x.data.TweakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TweakUiState(
    val displayInfo: DisplayInfo = DisplayInfo(1080, 2400, 420, listOf(60f)),
    val dpi: Int = 420,
    val width: Int = 1080,
    val pointerSpeed: Int = 0,
    val touchBoost: Boolean = false,
    val forceMaxRefreshRate: Boolean = false,
    val applying: Boolean = false,
    val message: String? = null,
) {
    val minDpi: Int get() = (displayInfo.densityDpi - 200).coerceAtLeast(160)
    val maxDpi: Int get() = displayInfo.densityDpi
    val minWidth: Int get() = (displayInfo.widthPx * 0.55f).toInt()
    val maxWidth: Int get() = displayInfo.widthPx
    val projectedHeight: Int
        get() = (width * displayInfo.aspectRatio).toInt().let { if (it % 2 != 0) it + 1 else it }
}

class TweakViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TweakRepository()
    private val preferences = AetherXPreferences(application)

    private val _state = MutableStateFlow(TweakUiState())
    val state: StateFlow<TweakUiState> = _state.asStateFlow()

    init {
        val displayInfo = DisplayInfoProvider.read(application)
        _state.update { it.copy(displayInfo = displayInfo, dpi = displayInfo.densityDpi, width = displayInfo.widthPx) }

        viewModelScope.launch {
            val saved = preferences.preferences.first()
            _state.update { current ->
                current.copy(
                    dpi = if (saved.dpiValue > 0) saved.dpiValue else current.dpi,
                    width = if (saved.widthValue > 0) saved.widthValue else current.width,
                    pointerSpeed = saved.pointerSpeed,
                    touchBoost = saved.touchBoostEnabled,
                    forceMaxRefreshRate = saved.forceMaxRefreshRate,
                )
            }
        }
    }

    fun onDpiChange(value: Float) {
        _state.update { it.copy(dpi = value.toInt()) }
    }

    fun onWidthChange(value: Float) {
        _state.update { it.copy(width = value.toInt()) }
    }

    fun onPointerSpeedChange(value: Float) {
        _state.update { it.copy(pointerSpeed = value.toInt()) }
    }

    fun onTouchBoostChange(checked: Boolean) {
        _state.update { it.copy(touchBoost = checked) }
    }

    fun onForceRefreshChange(checked: Boolean) {
        _state.update { it.copy(forceMaxRefreshRate = checked) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun applyTweaks() {
        val executor = PrivilegeManager.getExecutor()
        if (executor == null) {
            _state.update { it.copy(message = appString(R.string.tweak_no_access_toast)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(applying = true) }
            val s = _state.value
            repository.applyDensity(executor, s.dpi)
            repository.applySize(executor, s.width, s.projectedHeight)
            repository.applyPointerSpeed(executor, s.pointerSpeed)
            repository.applyTouchBoost(executor, s.touchBoost)
            repository.applyRefreshRate(executor, s.forceMaxRefreshRate, s.displayInfo.maxRefreshRate)
            preferences.saveTweakState(
                dpiValue = s.dpi,
                widthValue = s.width,
                pointerSpeed = s.pointerSpeed,
                touchBoostEnabled = s.touchBoost,
                forceMaxRefreshRate = s.forceMaxRefreshRate,
            )
            _state.update { it.copy(applying = false, message = appString(R.string.tweak_applied_toast)) }
        }
    }

    fun resetTweaks() {
        viewModelScope.launch {
            _state.update { it.copy(applying = true) }
            val executor = PrivilegeManager.getExecutor()
            if (executor != null) {
                repository.resetAll(executor)
            }
            preferences.clearTweakState()
            _state.update {
                it.copy(
                    dpi = it.displayInfo.densityDpi,
                    width = it.displayInfo.widthPx,
                    pointerSpeed = 0,
                    touchBoost = false,
                    forceMaxRefreshRate = false,
                    applying = false,
                    message = appString(R.string.tweak_reset_toast),
                )
            }
        }
    }

    private fun appString(resId: Int): String = getApplication<Application>().getString(resId)
}
