# AetherX License Bot (Telegram) — v2, Firestore

Bot Telegram untuk kelola lisensi AetherX: generate, edit, hapus, cek device ID —
**langsung ke Firebase Firestore**, koleksi `licenses`, skema yang SAMA PERSIS
dengan yang dibaca `LicenseRepository.kt` di aplikasi Android.

> ⚠️ Ini pengganti versi bot sebelumnya yang salah pakai Redis Upstash — Redis
> **tidak dibaca sama sekali** oleh aplikasi Android, makanya token dari versi lama
> muncul "Kode tidak ditemukan". Versi ini menulis ke tempat yang benar.

## Format Token

7 karakter acak, campuran huruf besar, huruf kecil, dan angka.
Contoh: `aB3xK9m`, `Qz7vTp2`
(Karakter ambigu `0/O/1/l/I` dibuang biar gampang dibaca/diketik ulang pembeli.)

## Skema Firestore (harus sama persis dengan app)

Koleksi `licenses`, document ID = kode lisensi itu sendiri:

```
licenses/{token}
  deviceId: string | null
  status: "unused" | "active" | "revoked"
  activatedAt: timestamp | null
  expiresAt: timestamp
  createdAt: timestamp
  note: string (opsional, tambahan dari bot ini — tidak dipakai app)
```

Ini identik dengan skema yang dipakai `tools/license-generator-termux/generate_licenses.py`
dan `LicenseRepository.kt`. Karena skemanya sama, kode yang dibuat lewat bot ini
akan langsung bisa dipakai di aplikasi Android tanpa perlu ubah apa pun di app.

## Setup

1. **Ambil Service Account Key** (kalau belum ada / mau pakai punya sendiri):
   Firebase Console → project AetherX → ⚙️ Project Settings → Service accounts →
   **Generate new private key** → simpan sebagai `serviceAccountKey.json` di
   folder project ini (sejajar dengan `bot.js`).

   ⚠️ **File ini kunci akses PENUH ke Firestore-mu (bypass semua Security Rules).**
   - Jangan commit ke git
   - Jangan upload ke tempat publik
   - Jangan ikut ter-zip saat share project Android

   > Kalau kamu masih punya `serviceAccountKey.json` dari
   > `tools/license-generator-termux/` di project Android, itu bisa langsung
   > dipakai — tinggal copy ke folder bot ini.

2. Install dependency:
   ```bash
   npm install
   ```

3. Salin `.env.example` jadi `.env`, lalu isi:
   ```env
   TELEGRAM_BOT_TOKEN=8699927303:AAEiiP_JLYwyiEMaXlQNzowB_07w0MDlUks
   ADMIN_TELEGRAM_ID=123456789
   SERVICE_ACCOUNT_PATH=./serviceAccountKey.json
   ```
   `ADMIN_TELEGRAM_ID`: chat `/start` ke **@userinfobot** buat dapetin ID kamu.

4. Jalankan:
   ```bash
   npm start
   ```

   Untuk keep-alive 24 jam pakai `pm2`:
   ```bash
   npm install -g pm2
   pm2 start bot.js --name aetherx-license-bot
   pm2 save
   ```

## Perintah Bot

| Perintah | Akses | Keterangan |
|---|---|---|
| `/start` | semua | Tampilkan menu utama |
| `/help` | semua | Daftar perintah |
| `/generate <hari> [catatan]` | admin | Buat lisensi baru, status awal `unused`. Contoh: `/generate 30 Promo Juli` |
| `/check <token>` | semua | Lihat detail lisensi |
| `/edit <token>` | admin | Edit status, expiry, device ID (paksa aktivasi manual), atau catatan |
| `/delete <token>` | admin | Hapus lisensi permanen (minta konfirmasi) |
| `/device <deviceId>` | semua | Cari lisensi yang terkunci ke device ID tsb |
| `/unbind <token>` | admin | Lepas device dari lisensi → status kembali `unused`, bisa dipakai device lain |
| `/list` | admin | Daftar semua token lisensi |
| `/cancel` | semua | Batalkan proses multi-langkah yang sedang berjalan |

## Alur normal pemakaian

1. Admin `/generate 30` di bot → dapat token 7 karakter, status `unused`, `deviceId: null`.
2. Token dikasih ke pembeli.
3. Pembeli masukkan token di aplikasi Android → `LicenseRepository.activate()` mengunci
   `deviceId` ke device tsb dan ubah status jadi `active` — **ini terjadi otomatis dari
   sisi app**, bot tidak perlu ikut campur di langkah ini.
4. Kalau pembeli ganti HP dan perlu pindah lisensi: admin `/unbind <token>` di bot →
   `deviceId` dikosongkan, status balik `unused` → bisa diaktivasi ulang di device baru.

## Kenapa tidak pakai `firebase-admin`?

Supaya ringan dan gampang jalan di Termux (`firebase-admin` kadang berat/gagal
install di Termux karena native dependency). Bot ini bicara langsung ke
**Firestore REST API** pakai JWT yang ditandatangani manual dengan modul `crypto`
bawaan Node — persis pola yang dipakai `generate_licenses.py`, cuma versi JS.

## Keamanan

- Hanya `ADMIN_TELEGRAM_ID` yang bisa generate/edit/hapus/unbind.
- `serviceAccountKey.json` dan `.env` sudah masuk `.gitignore` — jangan pernah
  di-share ke luar.
- Kalau `serviceAccountKey.json` pernah tidak sengaja ter-upload/ter-share ke
  tempat lain, **segera generate key baru** dari Firebase Console dan hapus
  yang lama (Project Settings → Service accounts → kelola key lama).
