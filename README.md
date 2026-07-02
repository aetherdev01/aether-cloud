# AetherX License Bot (Telegram)

Bot Telegram untuk kelola lisensi AetherX: generate, edit, hapus, cek device ID — terhubung langsung ke Redis Upstash yang sama dengan license platform (`promoted-seal-66351`).

## Format Token

Token lisensi: **7 karakter acak**, campuran huruf besar, huruf kecil, dan angka.
Contoh: `aB3xK9m`, `Qz7vTp2`

Karakter ambigu (`0/O`, `1/l/I`) sengaja dibuang biar gampang dibaca & diketik ulang oleh pembeli.

## Setup

1. Install dependency:
   ```bash
   npm install
   ```

2. Salin `.env.example` jadi `.env`, lalu isi:
   ```env
   TELEGRAM_BOT_TOKEN=8699927303:AAEiiP_JLYwyiEMaXlQNzowB_07w0MDlUks
   ADMIN_TELEGRAM_ID=123456789
   UPSTASH_REDIS_REST_URL=https://promoted-seal-66351.upstash.io
   UPSTASH_REDIS_REST_TOKEN=isi_token_upstash_kamu
   ```

   - `ADMIN_TELEGRAM_ID`: Telegram user ID kamu (angka). Cara dapat: chat `/start` ke **@userinfobot**.
   - `UPSTASH_REDIS_REST_TOKEN`: ambil dari dashboard Upstash Redis kamu (yang sama dipakai license platform Vercel). **Jangan** commit token ini ke git.

3. Jalankan bot:
   ```bash
   npm start
   ```

   Bot pakai **polling** (bukan webhook), jadi tinggal jalan di Termux, VPS, atau PC — asal proses tetap hidup. Untuk keep-alive di VPS bisa pakai `pm2`:
   ```bash
   npm install -g pm2
   pm2 start bot.js --name aetherx-license-bot
   pm2 save
   ```

## Perintah Bot

Semua perintah bisa dipakai lewat command langsung (`/generate ...`) atau lewat tombol menu `/start`.

| Perintah | Akses | Keterangan |
|---|---|---|
| `/start` | semua | Tampilkan menu utama |
| `/help` | semua | Daftar perintah |
| `/generate [plan] [maxDevices] [hariBerlaku]` | admin | Buat lisensi baru. Contoh: `/generate lifetime 1 0` atau `/generate monthly 2 30` |
| `/check <token>` | semua | Lihat detail lisensi |
| `/edit <token>` | admin | Edit status, plan, maxDevices, expiry, device ID, atau catatan (flow interaktif via tombol) |
| `/delete <token>` | admin | Hapus lisensi permanen (minta konfirmasi) |
| `/device <deviceId>` | semua | Cari lisensi yang terpasang di device ID tsb |
| `/unbind <token>` | admin | Lepas device dari lisensi, supaya bisa dipakai di device lain |
| `/list` | admin | Daftar semua token lisensi (maks 50 ditampilkan) |
| `/stats` | admin | Jumlah total lisensi tersimpan |
| `/cancel` | semua | Batalkan proses multi-langkah yang sedang berjalan |

## Struktur Data di Redis

Setiap lisensi disimpan sebagai **Hash** di key `license:<token>`:

```
{
  token, status (active|suspended|revoked),
  deviceId, maxDevices, plan, note,
  createdAt, expiresAt, activatedAt
}
```

Index tambahan:
- `license:all` — Set berisi semua token yang pernah dibuat (dipakai untuk `/list`, `/stats`)
- `license:by-device:<deviceId>` — Set token yang terkait device tsb (dipakai untuk `/device`)

Skema ini **kompatibel** untuk dibaca langsung dari backend Next.js license platform kamu — tinggal pakai `hgetall("license:<token>")` di endpoint checkout/validasi.

## Keamanan

- Hanya `ADMIN_TELEGRAM_ID` yang bisa generate/edit/hapus/unbind lisensi.
- User biasa tetap bisa `/check` dan `/device` untuk keperluan support (lihat status lisensi sendiri) — kalau mau ini juga dikunci ke admin saja, tinggal tambahkan `requireAdmin(msg)` di handler `/check` dan `/device`.
- **Jangan** commit file `.env` ke git atau sertakan di zip yang dibagikan.
- Token bot Telegram yang kamu berikan sudah tertanam sebagai contoh di `.env.example` — sebaiknya tetap pindahkan ke `.env` asli dan jangan publikasikan repo ini secara publik apa adanya.
