const { Redis } = require("@upstash/redis");

const redis = new Redis({
  url: process.env.UPSTASH_REDIS_REST_URL,
  token: process.env.UPSTASH_REDIS_REST_TOKEN,
});

/**
 * Struktur data lisensi disimpan sebagai Redis Hash di key `license:<token>`:
 * {
 *   token: string,
 *   status: "active" | "suspended" | "revoked",
 *   deviceId: string | "",          // device ID yang sudah terkunci ke lisensi ini
 *   maxDevices: number,             // default 1
 *   plan: string,                   // mis. "monthly", "lifetime"
 *   note: string,                   // catatan bebas admin
 *   createdAt: number (ms epoch),
 *   expiresAt: number (ms epoch) | 0,  // 0 = tidak pernah expired
 *   activatedAt: number (ms epoch) | 0,
 * }
 *
 * Index tambahan:
 * - Set "license:all"          -> semua token yang pernah dibuat
 * - Set "license:by-device:<deviceId>" -> token yang terkait device tsb (untuk lookup cepat)
 */

const LICENSE_KEY = (token) => `license:${token}`;
const ALL_LICENSES_SET = "license:all";
const DEVICE_INDEX_KEY = (deviceId) => `license:by-device:${deviceId}`;

async function createLicense({ token, plan, maxDevices, expiresAt, note }) {
  const now = Date.now();
  const data = {
    token,
    status: "active",
    deviceId: "",
    maxDevices: maxDevices ?? 1,
    plan: plan ?? "default",
    note: note ?? "",
    createdAt: now,
    expiresAt: expiresAt ?? 0,
    activatedAt: 0,
  };
  await redis.hset(LICENSE_KEY(token), data);
  await redis.sadd(ALL_LICENSES_SET, token);
  return data;
}

async function getLicense(token) {
  const data = await redis.hgetall(LICENSE_KEY(token));
  if (!data || Object.keys(data).length === 0) return null;
  return normalizeLicense(data);
}

function normalizeLicense(raw) {
  return {
    token: raw.token,
    status: raw.status,
    deviceId: raw.deviceId || "",
    maxDevices: Number(raw.maxDevices ?? 1),
    plan: raw.plan || "default",
    note: raw.note || "",
    createdAt: Number(raw.createdAt ?? 0),
    expiresAt: Number(raw.expiresAt ?? 0),
    activatedAt: Number(raw.activatedAt ?? 0),
  };
}

async function updateLicense(token, fields) {
  const exists = await redis.exists(LICENSE_KEY(token));
  if (!exists) return null;
  await redis.hset(LICENSE_KEY(token), fields);
  return getLicense(token);
}

async function deleteLicense(token) {
  const license = await getLicense(token);
  if (!license) return false;
  if (license.deviceId) {
    await redis.srem(DEVICE_INDEX_KEY(license.deviceId), token);
  }
  await redis.del(LICENSE_KEY(token));
  await redis.srem(ALL_LICENSES_SET, token);
  return true;
}

async function bindDevice(token, deviceId) {
  await redis.hset(LICENSE_KEY(token), {
    deviceId,
    activatedAt: Date.now(),
  });
  await redis.sadd(DEVICE_INDEX_KEY(deviceId), token);
}

async function unbindDevice(token) {
  const license = await getLicense(token);
  if (!license || !license.deviceId) return;
  await redis.srem(DEVICE_INDEX_KEY(license.deviceId), token);
  await redis.hset(LICENSE_KEY(token), { deviceId: "", activatedAt: 0 });
}

async function findLicensesByDevice(deviceId) {
  const tokens = await redis.smembers(DEVICE_INDEX_KEY(deviceId));
  if (!tokens || tokens.length === 0) return [];
  const licenses = await Promise.all(tokens.map((t) => getLicense(t)));
  return licenses.filter(Boolean);
}

async function listAllTokens() {
  return redis.smembers(ALL_LICENSES_SET);
}

async function countAllLicenses() {
  return redis.scard(ALL_LICENSES_SET);
}

module.exports = {
  redis,
  createLicense,
  getLicense,
  updateLicense,
  deleteLicense,
  bindDevice,
  unbindDevice,
  findLicensesByDevice,
  listAllTokens,
  countAllLicenses,
};
