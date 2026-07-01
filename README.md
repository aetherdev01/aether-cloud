# AetherX 🎮

**Pelicin Layar untuk Free Fire** — optimalkan sensitivitas sentuhan, DPI, resolusi, dan refresh rate tanpa perlu komputer.

---

## Fitur

| Fitur | Keterangan |
|---|---|
| **Kecepatan Pointer** | Atur sensitivitas sentuh sistem (`settings put system pointer_speed`) |
| **Touch Sensitivity Boost** | Toggle eksperimental untuk perangkat yang mendukung |
| **DPI / Screen Density** | Turunkan DPI agar sentuhan lebih presisi (`wm density`) |
| **Resolusi Layar** | Ubah lebar resolusi render (`wm size`) |
| **Refresh Rate Maksimal** | Kunci layar ke refresh rate tertinggi perangkat |
| **Reset** | Kembalikan semua pengaturan ke nilai pabrik |
| **Dynamic Color** | Warna otomatis ambil palet dari wallpaper (Android 12+) |

---

## Persyaratan Akses

AetherX menggunakan salah satu dari dua backend:

- **Shizuku** — untuk HP *tidak di-root*. Aktifkan lewat Wireless Debugging di menu Developer Options, lalu buka aplikasi [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) dan jalankan service-nya.
- **Root** — untuk HP yang sudah di-root dengan **Magisk**, **KernelSU**, atau **APatch**. Cukup setujui permintaan superuser saat pertama kali muncul.

---

## Build

### Prasyarat

- Android Studio Hedgehog (2023.1.1) atau lebih baru
- JDK 17
- Android SDK 35

### Langkah

```bash
# 1. Clone repo
git clone https://github.com/aetherdev01/AetherX.git
cd AetherX

# 2. Buat file signing (lihat local.properties.example)
cp local.properties.example local.properties
# → isi STORE_FILE, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD

# 3. Build
./gradlew assembleRelease
# APK ada di: app/build/outputs/apk/release/
```

### CI (GitHub Actions)

Build otomatis berjalan di setiap push ke `main`. Diperlukan secrets:

| Secret | Isi |
|---|---|
| `KEYSTORE_BASE64` | Keystore di-encode Base64 (`base64 aetherx.jks`) |
| `STORE_PASSWORD` | Password keystore |
| `KEY_ALIAS` | Alias key |
| `KEY_PASSWORD` | Password key |
| `TELEGRAM_BOT_TOKEN` | Token bot Telegram untuk notifikasi |
| `TELEGRAM_CHAT_ID` | Chat ID tujuan notifikasi |

---

## Arsitektur

```
com.aether.x
├── core/
│   ├── display/        ← Baca info layar (resolusi, DPI, refresh rate)
│   ├── permission/     ← PrivilegeManager (Shizuku + root status & request)
│   └── shell/          ← ShellExecutor interface + Shizuku & Root implementasi
├── data/
│   ├── AetherXPreferences.kt  ← DataStore (onboarding, tema, nilai tweak)
│   └── TweakRepository.kt     ← Perintah shell per fitur tweak
└── ui/
    ├── components/     ← SectionCard, TweakSlider, TweakSwitch, StatusPill, dll
    ├── main/           ← MainScreen (bottom nav host)
    ├── navigation/     ← Route constants + NavHost
    ├── onboarding/     ← PermissionSetupScreen, GuideScreen
    ├── settings/       ← SettingsScreen + ViewModel
    ├── theme/          ← AetherXTheme (M3 + dynamic color)
    └── tweak/          ← TweakScreen + TweakViewModel
```

---

## Perizinan

Semua perintah yang dijalankan AetherX adalah perintah **Android resmi** yang sama persis dengan yang bisa dieksekusi lewat `adb shell`:

```bash
wm density <dpi>          # ubah DPI
wm size <width>x<height>  # ubah resolusi
settings put system pointer_speed <-7..7>
settings put system peak_refresh_rate <hz>
```

Tidak ada modifikasi kernel, tidak ada akses ke partisi sistem, dan setiap perubahan bisa dikembalikan sepenuhnya lewat tombol **Reset** di tab Tweak.

---

## Lisensi

MIT License — bebas digunakan dan dimodifikasi.
