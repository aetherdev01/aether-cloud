# AetherX License Generator — versi Termux

Skrip Python murni (tanpa `pip install` package berat) untuk membuat kode
lisensi massal langsung dari HP lewat Termux. Cocok dipakai kalau
`firebase-admin` (versi Node.js di folder `tools/license-generator/`)
bermasalah saat di-install di Termux.

## Kenapa versi ini berbeda

Alih-alih pakai Firebase Admin SDK, skrip ini bicara langsung ke
**Firestore REST API** pakai modul bawaan Python (`urllib`, `json`,
`hashlib`) plus binary `openssl` untuk menandatangani JWT service account.
Tidak ada native module yang perlu di-compile — jadi jauh lebih ringan dan
jarang gagal install di Termux.

## Setup (sekali saja)

```bash
pkg update
pkg install python openssl-tool
```

Ambil **service account key**:
Firebase Console → project AetherX → ⚙️ **Project Settings** →
**Service accounts** → **Generate new private key** → simpan file JSON
yang terunduh sebagai `serviceAccountKey.json` **di folder ini juga**
(`tools/license-generator-termux/`).

⚠️ **PENTING**: file ini kunci akses PENUH ke seluruh Firestore-mu, bypass
semua Security Rules.
- Jangan pernah commit ke git
- Jangan upload/share ke tempat publik
- Jangan ikut ter-zip saat kamu bagikan project Android-nya

Kalau kamu terima file ini lewat WhatsApp/Telegram/email ke HP, pindahkan
ke Termux dengan:
```bash
termux-setup-storage   # sekali saja, untuk izinkan akses storage
cp /sdcard/Download/serviceAccountKey.json ~/aetherX/tools/license-generator-termux/
```

## Pemakaian

```bash
cd tools/license-generator-termux

# Bikin 50 kode, masing-masing berlaku 30 hari
python3 generate_licenses.py --count 50 --days 30

# Bikin 10 kode berlaku 7 hari, prefix custom
python3 generate_licenses.py --count 10 --days 7 --prefix TRIAL

# Lihat dulu kode yang AKAN dibuat, TANPA menulis ke Firestore
python3 generate_licenses.py --count 5 --days 30 --dry-run
```

Setiap kali dijalankan (mode live), skrip akan:
1. Generate kode acak format `PREFIX-XXXX-XXXX-XXXX`
2. Menandatangani JWT dengan `openssl` lalu menukarnya ke access token OAuth2
3. Membuat dokumen `licenses/{kode}` di Firestore lewat REST API
   (`status: "unused"`, `deviceId: null`, `expiresAt` sesuai `--days`)
4. Menyimpan daftar kode ke file `.csv` lokal — itu yang kamu bagikan ke
   pembeli/pengguna

## Troubleshooting

**`openssl: command not found`**
Jalankan `pkg install openssl-tool` (bukan `openssl` — nama paketnya
`openssl-tool` di Termux).

**Error `Host not in allowlist` / gagal koneksi**
Pastikan Termux punya izin akses internet dan koneksi data/wifi aktif.
Firestore REST API dan Google OAuth2 butuh koneksi keluar ke
`oauth2.googleapis.com` dan `firestore.googleapis.com`.

**Sebagian kode gagal (`[GAGAL]`) saat generate banyak sekaligus**
Access token berlaku 1 jam — kalau `--count` sangat besar (ratusan) dan
koneksi lambat, token bisa kadaluarsa di tengah jalan. Jalankan ulang
dengan `--count` lebih kecil per batch.

## Kenapa lewat skrip, bukan tombol di dalam app?

Membuat kode lisensi butuh hak tulis yang **tidak dimiliki** aplikasi
client (lihat `firestore.rules` — `licenses/{key}` sengaja
`allow create: if false` untuk semua client). Ini mencegah siapa pun yang
sekadar decompile APK bisa membuat kode lisensi sendiri.
