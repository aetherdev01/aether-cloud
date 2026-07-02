# AetherX License Generator

Skrip admin untuk membuat kode lisensi secara massal di Firestore, dijalankan
dari komputer kamu sendiri — **bukan** bagian dari aplikasi Android, dan
**tidak boleh** ikut di-bundle ke APK.

## Setup (sekali saja)

1. **Ambil service account key:**
   Firebase Console → project AetherX → ⚙️ **Project Settings** →
   **Service accounts** → tombol **Generate new private key**.
   File JSON yang terunduh, simpan sebagai `serviceAccountKey.json` **di
   folder ini juga** (`tools/license-generator/`).

   ⚠️ **PENTING**: file ini adalah kunci akses PENUH ke seluruh database
   Firestore-mu, bypass semua Security Rules. Jangan pernah:
   - Commit ke git (folder ini sudah dikecualikan lewat `.gitignore`)
   - Upload ke tempat publik / share ke orang lain
   - Ikut ter-zip saat kamu build/share project Android-nya

2. **Install dependency:**
   ```bash
   cd tools/license-generator
   npm install
   ```

## Pemakaian

```bash
# Bikin 50 kode, masing-masing berlaku 30 hari sejak hari ini
node generate-licenses.js --count 50 --days 30

# Bikin 10 kode berlaku 7 hari, prefix custom
node generate-licenses.js --count 10 --days 7 --prefix TRIAL

# Lihat dulu kode apa saja yang AKAN dibuat, TANPA menulis ke Firestore
node generate-licenses.js --count 5 --days 30 --dry-run

# Simpan hasil ke nama file tertentu
node generate-licenses.js --count 20 --days 365 --out yearly-batch.csv
```

Setiap kali dijalankan (mode live, bukan `--dry-run`), skrip akan:
1. Generate kode acak format `PREFIX-XXXX-XXXX-XXXX`
2. Menulis dokumen ke `licenses/{kode}` di Firestore dengan `status: "unused"`,
   `deviceId: null`, `expiresAt` sesuai `--days` yang diisi
3. Menyimpan daftar kode yang baru dibuat ke file `.csv` lokal — ini file
   yang kamu bagikan ke pembeli/pengguna, **bukan** ke publik

## Kenapa lewat skrip, bukan tombol di dalam app?

Membuat kode lisensi butuh hak tulis yang **tidak dimiliki** aplikasi client
(lihat `firestore.rules` — `licenses/{key}` sengaja `allow create: if false`
untuk semua client). Ini mencegah siapa pun yang sekadar decompile APK bisa
membuat kode lisensi sendiri.
