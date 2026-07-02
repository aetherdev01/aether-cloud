const COLLECTION = "licenses";

/**
 * Skema dokumen `licenses/{key}` — HARUS sama persis dengan yang dibaca
 * LicenseRepository.kt di aplikasi Android:
 * {
 *   deviceId: string | null,
 *   status: "unused" | "active" | "revoked",
 *   activatedAt: Date | null,
 *   expiresAt: Date,
 *   createdAt: Date,
 * }
 */

async function createLicense(firestore, { token, days, note }) {
  const now = new Date();
  const expiresAt = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);

  const data = {
    deviceId: null,
    status: "unused",
    activatedAt: null,
    expiresAt,
    createdAt: now,
  };
  if (note) data.note = note;

  await firestore.createDocument(COLLECTION, token, data);
  return { token, ...data };
}

async function getLicense(firestore, token) {
  const data = await firestore.getDocument(COLLECTION, token);
  if (!data) return null;
  return { token, ...data };
}

async function updateLicense(firestore, token, fields) {
  const existing = await firestore.getDocument(COLLECTION, token);
  if (!existing) return null;
  await firestore.updateDocument(COLLECTION, token, fields);
  return getLicense(firestore, token);
}

async function deleteLicense(firestore, token) {
  return firestore.deleteDocument(COLLECTION, token);
}

async function unbindDevice(firestore, token) {
  return updateLicense(firestore, token, {
    deviceId: null,
    status: "unused",
    activatedAt: null,
  });
}

async function findLicensesByDevice(firestore, deviceId) {
  const results = await firestore.queryByField(COLLECTION, "deviceId", deviceId, { limit: 20 });
  return results.map((r) => ({ token: r.id, ...r }));
}

async function listAllTokens(firestore, { maxResults = 500 } = {}) {
  return firestore.listDocumentIds(COLLECTION, { maxResults });
}

module.exports = {
  createLicense,
  getLicense,
  updateLicense,
  deleteLicense,
  unbindDevice,
  findLicensesByDevice,
  listAllTokens,
};
