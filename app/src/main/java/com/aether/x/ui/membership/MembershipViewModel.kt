package com.aether.x.ui.membership

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aether.x.R
import com.aether.x.data.AetherXPreferences
import com.aether.x.data.LicenseRepository
import com.aether.x.data.LicenseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class MembershipUiStatus { CHECKING, INACTIVE, ACTIVE, EXPIRED }

/**
 * Menampung seluruh state & logika layar Membership — dipisah dari tab
 * Pengaturan (lihat MEMBERSHIP di [com.aether.x.ui.main.MainScreen]) supaya
 * jadi tab tersendiri di bottom navigation, terpisah dari pengaturan umum
 * aplikasi. Logikanya sama persis dengan yang sebelumnya ada di
 * `SettingsViewModel` — hanya dipindahkan ke sini.
 *
 * Sengaja BUKAN gerbang wajib: aplikasi (termasuk semua tweak di tab Tweak)
 * tetap 100% bisa dipakai tanpa lisensi aktif sama sekali. Layar ini murni
 * status/badge + form aktivasi opsional, mirip menu "Upgrade ke Premium" di
 * banyak aplikasi umum — bukan blocking screen sebelum masuk aplikasi.
 *
 * Penegakan sesungguhnya (satu kode = satu device, tidak bisa dipakai ulang
 * di device lain) tetap ada di [LicenseRepository] dan firestore.rules.
 */
class MembershipViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = AetherXPreferences(application)
    private val licenseRepository = LicenseRepository(application)

    private val _status = MutableStateFlow(MembershipUiStatus.CHECKING)
    val status: StateFlow<MembershipUiStatus> = _status.asStateFlow()

    private val _expiresAtMillis = MutableStateFlow<Long?>(null)
    val expiresAtMillis: StateFlow<Long?> = _expiresAtMillis.asStateFlow()

    private val _keyInput = MutableStateFlow("")
    val keyInput: StateFlow<String> = _keyInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    init {
        // Cek status membership sekali saat tab Membership pertama kali dibuka
        // (bukan blocking apa pun — cuma mengisi kartu ini). Kalau tidak ada
        // cache lokal sama sekali, langsung dianggap INACTIVE tanpa perlu
        // ke jaringan.
        viewModelScope.launch {
            val cached = preferences.preferences.first()
            val cachedKey = cached.licenseKey
            if (cachedKey.isNullOrBlank()) {
                _status.value = MembershipUiStatus.INACTIVE
                return@launch
            }

            when (val result = licenseRepository.revalidate(cachedKey)) {
                is LicenseResult.Valid -> {
                    preferences.setLicenseCache(cachedKey, result.expiresAtMillis)
                    _status.value = MembershipUiStatus.ACTIVE
                    _expiresAtMillis.value = result.expiresAtMillis
                }
                is LicenseResult.Expired -> {
                    preferences.clearLicenseCache()
                    _status.value = MembershipUiStatus.EXPIRED
                    _expiresAtMillis.value = result.expiredAtMillis
                }
                LicenseResult.Revoked, LicenseResult.BoundToOtherDevice, LicenseResult.NotFound -> {
                    preferences.clearLicenseCache()
                    _status.value = MembershipUiStatus.INACTIVE
                }
                LicenseResult.NetworkError -> {
                    // Offline: percaya cache lokal terakhir apa adanya (kalau
                    // sempat tersimpan sebagai Valid sebelumnya) daripada
                    // memaksa tampil INACTIVE hanya karena tidak ada koneksi.
                    val cachedExpiry = cached.licenseExpiresAtMillis
                    _status.value = if (cachedExpiry != null && cachedExpiry > System.currentTimeMillis()) {
                        MembershipUiStatus.ACTIVE
                    } else {
                        MembershipUiStatus.INACTIVE
                    }
                    _expiresAtMillis.value = cachedExpiry
                }
            }
        }
    }

    fun setKeyInput(value: String) {
        // Format lisensi sekarang bebas: huruf besar/kecil apa pun dan angka,
        // tidak lagi dipaksa mengikuti pola "AETX-XXXX-XXXX-XXXX" empat blok
        // saja. Satu-satunya normalisasi yang tetap dilakukan adalah membuang
        // spasi di awal/akhir saat submit (lihat [activate]), supaya kode
        // yang di-copy-paste dengan spasi tambahan tetap valid.
        _keyInput.value = value
        _errorMessage.value = null
    }

    fun activate() {
        val key = _keyInput.value.trim()
        if (key.isEmpty()) {
            _errorMessage.value = appString(R.string.membership_key_error_empty)
            return
        }
        _errorMessage.value = null
        _isSubmitting.value = true
        viewModelScope.launch {
            when (val result = licenseRepository.activate(key)) {
                is LicenseResult.Valid -> {
                    preferences.setLicenseCache(key, result.expiresAtMillis)
                    _status.value = MembershipUiStatus.ACTIVE
                    _expiresAtMillis.value = result.expiresAtMillis
                    _keyInput.value = ""
                }
                is LicenseResult.Expired -> {
                    _errorMessage.value = appString(R.string.membership_key_error_expired)
                }
                LicenseResult.Revoked -> {
                    _errorMessage.value = appString(R.string.membership_key_error_revoked)
                }
                LicenseResult.BoundToOtherDevice -> {
                    _errorMessage.value = appString(R.string.membership_key_error_bound)
                }
                LicenseResult.NotFound -> {
                    _errorMessage.value = appString(R.string.membership_key_error_not_found)
                }
                LicenseResult.NetworkError -> {
                    _errorMessage.value = appString(R.string.membership_key_error_network)
                }
            }
            _isSubmitting.value = false
        }
    }

    private fun appString(resId: Int): String = getApplication<Application>().getString(resId)
}
