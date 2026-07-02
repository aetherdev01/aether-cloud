package com.aether.x.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.x.core.overlay.CrosshairOverlayService
import com.aether.x.core.overlay.FpsMonitorOverlayService
import com.aether.x.data.AetherXPreferences
import com.aether.x.data.AppPreferences
import com.aether.x.data.CrosshairStyle
import com.aether.x.data.DarkModePref
import com.aether.x.data.FpsMonitorStyle
import com.aether.x.data.LicenseRepository
import com.aether.x.data.LicenseResult
import com.aether.x.data.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AetherXPreferences(application)
    private val licenseRepository = LicenseRepository(application)

    val state: StateFlow<AppPreferences> = preferences.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppPreferences(),
    )

    private val _membershipStatus = MutableStateFlow(MembershipUiStatus.CHECKING)
    val membershipStatus: StateFlow<MembershipUiStatus> = _membershipStatus.asStateFlow()

    private val _membershipExpiresAtMillis = MutableStateFlow<Long?>(null)
    val membershipExpiresAtMillis: StateFlow<Long?> = _membershipExpiresAtMillis.asStateFlow()

    private val _membershipKeyInput = MutableStateFlow("")
    val membershipKeyInput: StateFlow<String> = _membershipKeyInput.asStateFlow()

    private val _membershipError = MutableStateFlow<String?>(null)
    val membershipError: StateFlow<String?> = _membershipError.asStateFlow()

    private val _membershipSubmitting = MutableStateFlow(false)
    val membershipSubmitting: StateFlow<Boolean> = _membershipSubmitting.asStateFlow()

    init {
        // Cek status membership sekali saat Settings pertama kali dibuka
        // (bukan blocking apa pun — cuma mengisi kartu ini). Kalau tidak ada
        // cache lokal sama sekali, langsung dianggap INACTIVE tanpa perlu
        // ke jaringan.
        viewModelScope.launch {
            val cached = preferences.preferences.first()
            val cachedKey = cached.licenseKey
            if (cachedKey.isNullOrBlank()) {
                _membershipStatus.value = MembershipUiStatus.INACTIVE
                return@launch
            }

            when (val result = licenseRepository.revalidate(cachedKey)) {
                is LicenseResult.Valid -> {
                    preferences.setLicenseCache(cachedKey, result.expiresAtMillis)
                    _membershipStatus.value = MembershipUiStatus.ACTIVE
                    _membershipExpiresAtMillis.value = result.expiresAtMillis
                }
                is LicenseResult.Expired -> {
                    preferences.clearLicenseCache()
                    _membershipStatus.value = MembershipUiStatus.EXPIRED
                    _membershipExpiresAtMillis.value = result.expiredAtMillis
                }
                LicenseResult.Revoked, LicenseResult.BoundToOtherDevice, LicenseResult.NotFound -> {
                    preferences.clearLicenseCache()
                    _membershipStatus.value = MembershipUiStatus.INACTIVE
                }
                LicenseResult.NetworkError -> {
                    // Offline: percaya cache lokal terakhir apa adanya (kalau
                    // sempat tersimpan sebagai Valid sebelumnya) daripada
                    // memaksa tampil INACTIVE hanya karena tidak ada koneksi.
                    val cachedExpiry = cached.licenseExpiresAtMillis
                    _membershipStatus.value = if (cachedExpiry != null && cachedExpiry > System.currentTimeMillis()) {
                        MembershipUiStatus.ACTIVE
                    } else {
                        MembershipUiStatus.INACTIVE
                    }
                    _membershipExpiresAtMillis.value = cachedExpiry
                }
            }
        }
    }

    fun setMembershipKeyInput(value: String) {
        _membershipKeyInput.value = value
        _membershipError.value = null
    }

    fun activateMembership() {
        val key = _membershipKeyInput.value.trim()
        if (key.isEmpty()) {
            _membershipError.value = "Masukkan kode lisensi terlebih dulu."
            return
        }
        _membershipError.value = null
        _membershipSubmitting.value = true
        viewModelScope.launch {
            when (val result = licenseRepository.activate(key)) {
                is LicenseResult.Valid -> {
                    preferences.setLicenseCache(key, result.expiresAtMillis)
                    _membershipStatus.value = MembershipUiStatus.ACTIVE
                    _membershipExpiresAtMillis.value = result.expiresAtMillis
                    _membershipKeyInput.value = ""
                }
                is LicenseResult.Expired -> {
                    _membershipError.value = "Kode ini sudah kedaluwarsa."
                }
                LicenseResult.Revoked -> {
                    _membershipError.value = "Kode ini sudah dicabut oleh admin."
                }
                LicenseResult.BoundToOtherDevice -> {
                    _membershipError.value = "Kode ini sudah dipakai di perangkat lain."
                }
                LicenseResult.NotFound -> {
                    _membershipError.value = "Kode tidak ditemukan. Periksa lagi penulisannya."
                }
                LicenseResult.NetworkError -> {
                    _membershipError.value = "Gagal terhubung ke server. Coba lagi."
                }
            }
            _membershipSubmitting.value = false
        }
    }

    fun setDarkModePref(pref: DarkModePref) {
        viewModelScope.launch { preferences.setDarkModePref(pref) }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferences.setTemperatureUnit(unit) }
    }

    /** true kalau izin "Tampil di atas aplikasi lain" sudah diberikan. */
    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(getApplication())

    fun openOverlayPermissionSettings() {
        val app = getApplication<Application>()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${app.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { app.startActivity(intent) }
    }

    fun setCrosshairEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setCrosshairEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled && canDrawOverlays()) {
                CrosshairOverlayService.start(app)
            } else {
                CrosshairOverlayService.stop(app)
            }
        }
    }

    fun setCrosshairStyle(style: CrosshairStyle) = updateCrosshair { it.copy(crosshairStyle = style) }

    fun setCrosshairColor(color: Long) = updateCrosshair { it.copy(crosshairColor = color) }

    fun setCrosshairSize(size: Int) = updateCrosshair { it.copy(crosshairSize = size) }

    fun setCrosshairThickness(thickness: Int) = updateCrosshair { it.copy(crosshairThickness = thickness) }

    fun setCrosshairOpacity(opacity: Int) = updateCrosshair { it.copy(crosshairOpacity = opacity) }

    fun setDragMode(enabled: Boolean) {
        CrosshairOverlayService.setDragMode(getApplication(), enabled)
    }

    fun resetCrosshairPosition() {
        viewModelScope.launch { preferences.setCrosshairOffset(0, 0) }
    }

    fun setFpsMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setFpsMonitorEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled && canDrawOverlays()) {
                FpsMonitorOverlayService.start(app)
            } else {
                FpsMonitorOverlayService.stop(app)
            }
        }
    }

    fun setFpsMonitorStyle(style: FpsMonitorStyle) {
        viewModelScope.launch { preferences.setFpsMonitorStyle(style) }
    }

    private fun updateCrosshair(transform: (AppPreferences) -> AppPreferences) {
        viewModelScope.launch {
            val current = state.value
            val updated = transform(current)
            preferences.saveCrosshairConfig(
                style = updated.crosshairStyle,
                color = updated.crosshairColor,
                size = updated.crosshairSize,
                thickness = updated.crosshairThickness,
                opacity = updated.crosshairOpacity,
                offsetX = updated.crosshairOffsetX,
                offsetY = updated.crosshairOffsetY,
            )
        }
    }
}
