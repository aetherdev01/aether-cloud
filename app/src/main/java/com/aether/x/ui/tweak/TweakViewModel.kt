package com.aether.x.ui.tweak

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.x.R
import com.aether.x.core.apps.DetectedGame
import com.aether.x.core.apps.GameLauncher
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
    val pointerSpeed: Int = 0,
    val touchBoost: Boolean = false,
    val forceMaxRefreshRate: Boolean = false,
    val applying: Boolean = false,
    val message: String? = null,
    val detectedGames: List<DetectedGame> = emptyList(),
) {
    // DPI lebih besar = tampilan lebih "licin"/rapat (UI mengecil, elemen lebih tajam dan padat).
    // DPI lebih kecil = tampilan lebih lebar/renggang (UI membesar).
    // Rentang dibatasi supaya tidak sampai bikin UI pecah di banyak perangkat.
    val minDpi: Int get() = (displayInfo.densityDpi * 0.7f).toInt().coerceAtLeast(120)
    val maxDpi: Int get() = (displayInfo.densityDpi * 1.6f).toInt()

    // Resolusi fisik (wm size) TIDAK diubah sama sekali oleh tweak DPI.
    // Nilai wm size mengikuti resolusi asli layar apa adanya; hanya densitas (wm density)
    // yang diubah. Ini yang membuat efeknya konsisten: DPI naik -> elemen UI mengecil/rapat,
    // DPI turun -> elemen UI membesar/renggang. Tidak ada lagi slider "lebar" terpisah yang
    // bentrok secara matematis dengan slider DPI.
    val widthPx: Int get() = displayInfo.widthPx
    val heightPx: Int get() = displayInfo.heightPx

    // Perkiraan lebar layar dalam dp pada DPI yang dipilih, seperti yang akan terlihat
    // di Opsi Pengembang -> Ukuran tampilan minimum / Smallest width.
    val projectedWidthDp: Int
        get() = (widthPx.toFloat() * 160f / dpi.toFloat()).toInt()
}

class TweakViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TweakRepository()
    private val preferences = AetherXPreferences(application)

    private val _state = MutableStateFlow(TweakUiState())
    val state: StateFlow<TweakUiState> = _state.asStateFlow()

    init {
        val displayInfo = DisplayInfoProvider.read(application)
        _state.update {
            it.copy(
                displayInfo = displayInfo,
                dpi = displayInfo.densityDpi,
                detectedGames = GameLauncher.detectInstalled(application),
            )
        }

        viewModelScope.launch {
            val saved = preferences.preferences.first()
            _state.update { current ->
                current.copy(
                    dpi = if (saved.dpiValue > 0) saved.dpiValue else current.dpi,
                    pointerSpeed = saved.pointerSpeed,
                    touchBoost = saved.touchBoostEnabled,
                    forceMaxRefreshRate = saved.forceMaxRefreshRate,
                )
            }
        }
    }

    /** Dipanggil ulang saat layar kembali aktif, untuk menangkap kalau game baru dipasang/dihapus. */
    fun refreshDetectedGames() {
        val app = getApplication<Application>()
        _state.update { it.copy(detectedGames = GameLauncher.detectInstalled(app)) }
    }

    fun launchGame(packageName: String) {
        val app = getApplication<Application>()
        val launched = GameLauncher.launch(app, packageName)
        if (!launched) {
            _state.update { it.copy(message = appString(R.string.tweak_game_launch_failed)) }
        }
    }

    /** Diisi dari input manual (TextField), bukan hanya slider, agar DPI bisa diketik langsung. */
    fun onDpiChange(value: Float) {
        _state.update { it.copy(dpi = value.toInt().coerceIn(it.minDpi, it.maxDpi)) }
    }

    fun onDpiTextChange(text: String) {
        val parsed = text.toIntOrNull() ?: return
        _state.update { it.copy(dpi = parsed.coerceIn(it.minDpi, it.maxDpi)) }
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
            repository.applyPointerSpeed(executor, s.pointerSpeed)
            repository.applyTouchBoost(executor, s.touchBoost)
            repository.applyRefreshRate(executor, s.forceMaxRefreshRate, s.displayInfo.maxRefreshRate)
            preferences.saveTweakState(
                dpiValue = s.dpi,
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
                repository.resetDensity(executor)
                repository.applyPointerSpeed(executor, 0)
                repository.applyTouchBoost(executor, false)
                repository.applyRefreshRate(executor, enabled = false, maxHz = 60f)
            }
            preferences.clearTweakState()
            _state.update {
                it.copy(
                    dpi = it.displayInfo.densityDpi,
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
