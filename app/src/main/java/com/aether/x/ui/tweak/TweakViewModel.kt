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
import com.aether.x.core.shell.ShellExecutor
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
    val pointerSpeed: Int = 0,
    val touchBoost: Boolean = false,
    val forceMaxRefreshRate: Boolean = false,
    val gameModeEnabled: Boolean = false,
    val message: String? = null,
    val detectedGames: List<DetectedGame> = emptyList(),
    val userId: Int? = null,
)

/**
 * Semua tweak di layar ini sekarang aktif LANGSUNG saat diubah (slider dilepas /
 * switch ditoggle) — tidak ada lagi tombol "Terapkan" terpisah. Setiap perubahan
 * langsung dieksekusi lewat [ShellExecutor] yang aktif (Shizuku/Root) dan disimpan
 * ke preferences supaya tetap tersimpan walau aplikasi ditutup.
 */
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
                detectedGames = GameLauncher.detectInstalled(application),
            )
        }

        viewModelScope.launch {
            val saved = preferences.preferences.first()
            _state.update { current ->
                current.copy(
                    pointerSpeed = saved.pointerSpeed,
                    touchBoost = saved.touchBoostEnabled,
                    forceMaxRefreshRate = saved.forceMaxRefreshRate,
                    gameModeEnabled = saved.gameModeEnabled,
                )
            }
        }

        // ID lokal pengguna (mis. "ID-67128") ditampilkan di header tab Tweak,
        // menggantikan pill status Shizuku/Root sebelumnya.
        viewModelScope.launch {
            val id = preferences.getOrCreateUserId()
            _state.update { it.copy(userId = id) }
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

    /** Slider hanya update tampilan sambil digeser; eksekusi shell dipicu saat dilepas
     *  lewat [onPointerSpeedChangeFinished] supaya tidak spam perintah shell tiap piksel. */
    fun onPointerSpeedChange(value: Float) {
        _state.update { it.copy(pointerSpeed = value.toInt()) }
    }

    fun onPointerSpeedChangeFinished() {
        val speed = _state.value.pointerSpeed
        applyAndPersist { executor -> repository.applyPointerSpeed(executor, speed) }
    }

    fun onTouchBoostChange(checked: Boolean) {
        _state.update { it.copy(touchBoost = checked) }
        applyAndPersist { executor -> repository.applyTouchBoost(executor, checked) }
    }

    fun onForceRefreshChange(checked: Boolean) {
        _state.update { it.copy(forceMaxRefreshRate = checked) }
        applyAndPersist { executor ->
            repository.applyRefreshRate(executor, checked, _state.value.displayInfo.maxRefreshRate)
        }
    }

    /** Mode Game: mengaktifkan Do Not Disturb sistem supaya notifikasi tidak
     *  mengganggu saat bermain. Dinonaktifkan lagi lewat switch yang sama. */
    fun onGameModeChange(checked: Boolean) {
        _state.update { it.copy(gameModeEnabled = checked) }
        applyAndPersist { executor -> repository.applyGameMode(executor, checked) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun resetTweaks() {
        viewModelScope.launch {
            val executor = PrivilegeManager.getExecutor()
            if (executor != null) {
                repository.applyPointerSpeed(executor, 0)
                repository.applyTouchBoost(executor, false)
                repository.applyRefreshRate(executor, enabled = false, maxHz = 60f)
                repository.applyGameMode(executor, false)
            }
            preferences.clearTweakState()
            _state.update {
                it.copy(
                    pointerSpeed = 0,
                    touchBoost = false,
                    forceMaxRefreshRate = false,
                    gameModeEnabled = false,
                    message = appString(R.string.tweak_reset_toast),
                )
            }
        }
    }

    /** Menjalankan satu perintah tweak lewat executor aktif lalu langsung menyimpan
     *  state tweak saat ini ke preferences. Kalau belum ada akses (Shizuku/Root),
     *  perubahan tetap tersimpan di UI/preferences tapi menampilkan toast peringatan. */
    private fun applyAndPersist(action: suspend (ShellExecutor) -> Unit) {
        viewModelScope.launch {
            val executor = PrivilegeManager.getExecutor()
            if (executor == null) {
                _state.update { it.copy(message = appString(R.string.tweak_no_access_toast)) }
            } else {
                action(executor)
            }
            val s = _state.value
            preferences.saveTweakState(
                pointerSpeed = s.pointerSpeed,
                touchBoostEnabled = s.touchBoost,
                forceMaxRefreshRate = s.forceMaxRefreshRate,
                gameModeEnabled = s.gameModeEnabled,
            )
        }
    }

    private fun appString(resId: Int): String = getApplication<Application>().getString(resId)
}
