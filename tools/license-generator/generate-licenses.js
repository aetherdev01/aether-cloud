#!/usr/bin/env node
/**
 * Generator kode lisensi AetherX — dijalankan dari komputer kamu sendiri
 * (BUKAN dari dalam app Android), karena butuh service account key yang
 * bypass Firestore Security Rules.
 *
 * Setup sekali saja:
 *   1. Firebase Console → Project Settings → Service accounts →
 *      "Generate new private key" → simpan file JSON-nya, mis. sebagai
 *      serviceAccountKey.json DI FOLDER INI.
 *      PENTING: jangan pernah commit file ini ke git / upload ke tempat
 *      publik — siapa pun yang punya file ini bisa baca/tulis SELURUH
 *      database Firestore kamu tanpa terikat rules.
 *   2. npm install firebase-admin
 *
 * Pemakaian:
 *   node generate-licenses.js --count 50 --days 30
 *   node generate-licenses.js --count 10 --days 7 --prefix AETX
 *   node generate-licenses.js --count 1 --days 365 --out yearly.csv
 *
 * Argumen:
 *   --count   jumlah kode yang mau dibuat (wajib)
 *   --days    masa berlaku dalam hari sejak SEKARANG (wajib)
 *   --prefix  awalan kode, default "AETX"
 *   --out     nama file CSV hasil, default "licenses-<timestamp>.csv"
 *   --dry-run tampilkan kode yang akan dibuat TANPA menulis ke Firestore
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// ---------- Parse argumen CLI ----------
function parseArgs(argv) {
  const args = { prefix: 'AETX', dryRun: false };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    if (arg === '--count') args.count = parseInt(argv[++i], 10);
    else if (arg === '--days') args.days = parseInt(argv[++i], 10);
    else if (arg === '--prefix') args.prefix = argv[++i];
    else if (arg === '--out') args.out = argv[++i];
    else if (arg === '--dry-run') args.dryRun = true;
    else {
      console.error(`Argumen tidak dikenal: ${arg}`);
      process.exit(1);
    }
  }
  return args;
}

// ---------- Generate satu kode acak yang sulit ditebak ----------
// Format: PREFIX-XXXX-XXXX-XXXX, huruf besar + angka, tanpa karakter yang
// gampang ketuker (0/O, 1/I) supaya nyaman diketik manual oleh pengguna.
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';

function randomSegment(length) {
  let out = '';
  const bytes = crypto.randomBytes(length);
  for (let i = 0; i < length; i++) {
    out += ALPHABET[bytes[i] % ALPHABET.length];
  }
  return out;
}

function generateCode(prefix) {
  return `${prefix}-${randomSegment(4)}-${randomSegment(4)}-${randomSegment(4)}`;
}

// ---------- Main ----------
async function main() {
  const args = parseArgs(process.argv.slice(2));

  if (!args.count || args.count <= 0) {
    console.error('Wajib isi --count <jumlah>, contoh: --count 50');
    process.exit(1);
  }
  if (!args.days || args.days <= 0) {
    console.error('Wajib isi --days <jumlah_hari>, contoh: --days 30');
    process.exit(1);
  }

  const keyPath = path.join(__dirname, 'serviceAccountKey.json');
  if (!args.dryRun && !fs.existsSync(keyPath)) {
    console.error(
      `File serviceAccountKey.json tidak ditemukan di ${keyPath}.\n` +
      'Lihat komentar di atas skrip ini untuk cara mendapatkannya, atau ' +
      'jalankan dengan --dry-run dulu untuk melihat kode tanpa menulis ke Firestore.',
    );
    process.exit(1);
  }

  // Generate semua kode dulu, dengan pengecekan duplikat DI DALAM BATCH ini
  // (peluang tabrakan antar kode 12-karakter dari alfabet 33 karakter nyaris
  // nol, tapi tetap dijaga eksplisit daripada mengandalkan probabilitas).
  const codes = new Set();
  while (codes.size < args.count) {
    codes.add(generateCode(args.prefix));
  }
  const codeList = Array.from(codes);

  const now = new Date();
  const expiresAt = new Date(now.getTime() + args.days * 24 * 60 * 60 * 1000);

  console.log(`Akan membuat ${args.count} kode lisensi:`);
  console.log(`  Prefix       : ${args.prefix}`);
  console.log(`  Masa berlaku : ${args.days} hari (kadaluarsa ${expiresAt.toISOString()})`);
  console.log(`  Mode         : ${args.dryRun ? 'DRY RUN (tidak menulis ke Firestore)' : 'LIVE (menulis ke Firestore)'}`);
  console.log('');

  if (args.dryRun) {
    codeList.forEach((code) => console.log(code));
  } else {
    const serviceAccount = require(keyPath);

    // Jaga-jaga: pastikan key ini benar-benar untuk project Firebase yang
    // sama dengan aplikasi Android (app/google-services.json). Kalau beda,
    // batch write ini akan SUKSES tanpa error tapi kodenya nyasar ke
    // project lain — app tidak akan pernah menemukan kode itu. Isi
    // EXPECTED_PROJECT_ID di environment untuk mengaktifkan pengecekan ini.
    const expectedProjectId = process.env.EXPECTED_PROJECT_ID;
    if (expectedProjectId && serviceAccount.project_id !== expectedProjectId) {
      console.error(
        `serviceAccountKey.json ini untuk project Firebase "${serviceAccount.project_id}", ` +
        `tapi EXPECTED_PROJECT_ID diisi "${expectedProjectId}".\n` +
        'Kode lisensi yang dibuat TIDAK akan terlihat di aplikasi Android kalau project-nya beda. ' +
        'Ambil ulang serviceAccountKey.json dari project yang benar.',
      );
      process.exit(1);
    }
    if (!expectedProjectId) {
      console.warn(
        `Peringatan: EXPECTED_PROJECT_ID belum di-set — kode akan ditulis ke project ` +
        `"${serviceAccount.project_id}" tanpa verifikasi. Pastikan ini project Firebase yang sama ` +
        'dengan project_id di app/google-services.json aplikasi Android-mu.\n',
      );
    }

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    const db = admin.firestore();

    // Ditulis per-batch (maks 500 write per Firestore batch) supaya efisien
    // dan tetap aman untuk --count besar.
    const BATCH_LIMIT = 500;
    for (let i = 0; i < codeList.length; i += BATCH_LIMIT) {
      const batch = db.batch();
      const chunk = codeList.slice(i, i + BATCH_LIMIT);

      for (const code of chunk) {
        const docRef = db.collection('licenses').doc(code);
        batch.set(docRef, {
          deviceId: null,
          status: 'unused',
          activatedAt: null,
          expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }

      await batch.commit();
      console.log(`Tertulis ${Math.min(i + BATCH_LIMIT, codeList.length)}/${codeList.length} kode ke Firestore...`);
    }

    console.log('\nSelesai. Semua kode sudah ada di collection `licenses`.');
  }

  // Selalu simpan hasilnya ke CSV, baik dry-run maupun live — supaya ada
  // catatan lokal kode mana saja yang baru dibuat (untuk dibagikan ke
  // pembeli/pengguna).
  const outPath = args.out || `licenses-${now.toISOString().replace(/[:.]/g, '-')}.csv`;
  const csvLines = ['code,expires_at', ...codeList.map((c) => `${c},${expiresAt.toISOString()}`)];
  fs.writeFileSync(outPath, csvLines.join('\n'), 'utf8');
  console.log(`Daftar kode disimpan ke: ${outPath}`);
}

main().catch((err) => {
  console.error('Gagal menjalankan skrip:', err);
  process.exit(1);
});
