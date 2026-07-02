const crypto = require("crypto");

// Charset campuran huruf besar, huruf kecil, dan angka.
// Karakter ambigu (0/O, 1/l/I) sengaja dibuang biar gampang dibaca & diketik user.
const CHARSET = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
const TOKEN_LENGTH = 7;

/**
 * Generate satu token acak sepanjang 7 karakter, campuran huruf besar/kecil + angka.
 * Menggunakan crypto.randomInt agar acak secara kriptografis (bukan Math.random).
 */
function generateRawToken() {
  let token = "";
  for (let i = 0; i < TOKEN_LENGTH; i++) {
    const idx = crypto.randomInt(0, CHARSET.length);
    token += CHARSET[idx];
  }
  return token;
}

/**
 * Generate token yang dijamin belum dipakai di Redis (cek tabrakan).
 * @param {import('@upstash/redis').Redis} redis
 * @returns {Promise<string>}
 */
async function generateUniqueToken(redis) {
  const MAX_ATTEMPTS = 20;
  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    const token = generateRawToken();
    const exists = await redis.exists(`license:${token}`);
    if (!exists) return token;
  }
  throw new Error("Gagal generate token unik setelah beberapa percobaan. Coba lagi.");
}

module.exports = { generateRawToken, generateUniqueToken, TOKEN_LENGTH, CHARSET };
