const crypto = require("crypto");
const fs = require("fs");

const FIRESTORE_SCOPE = "https://www.googleapis.com/auth/datastore";
const TOKEN_URL = "https://oauth2.googleapis.com/token";

function b64url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

class FirestoreClient {
  /**
   * @param {string} serviceAccountPath - path ke serviceAccountKey.json
   */
  constructor(serviceAccountPath) {
    if (!fs.existsSync(serviceAccountPath)) {
      throw new Error(
        `serviceAccountKey.json tidak ditemukan di ${serviceAccountPath}. ` +
          `Ambil dari Firebase Console -> Project Settings -> Service accounts -> Generate new private key.`
      );
    }
    this.serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf-8"));
    this.projectId = this.serviceAccount.project_id;
    this._accessToken = null;
    this._tokenExpiresAt = 0;
  }

  async _getAccessToken() {
    // Cache token selama masih berlaku (kurangi 60 detik buat jaga-jaga).
    if (this._accessToken && Date.now() < this._tokenExpiresAt - 60_000) {
      return this._accessToken;
    }

    const now = Math.floor(Date.now() / 1000);
    const header = { alg: "RS256", typ: "JWT" };
    const claims = {
      iss: this.serviceAccount.client_email,
      scope: FIRESTORE_SCOPE,
      aud: TOKEN_URL,
      iat: now,
      exp: now + 3600,
    };

    const signingInput = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(claims))}`;
    const signer = crypto.createSign("RSA-SHA256");
    signer.update(signingInput);
    signer.end();
    const signature = signer.sign(this.serviceAccount.private_key);
    const jwt = `${signingInput}.${b64url(signature)}`;

    const body = new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    });

    const res = await fetch(TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: body.toString(),
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Gagal menukar JWT ke access token: ${res.status} ${text}`);
    }

    const json = await res.json();
    this._accessToken = json.access_token;
    this._tokenExpiresAt = Date.now() + json.expires_in * 1000;
    return this._accessToken;
  }

  _baseUrl() {
    return `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents`;
  }

  /** Konversi objek JS biasa ke format Firestore REST `fields`. */
  static toFirestoreFields(obj) {
    const fields = {};
    for (const [key, value] of Object.entries(obj)) {
      fields[key] = FirestoreClient.toFirestoreValue(value);
    }
    return fields;
  }

  static toFirestoreValue(value) {
    if (value === null || value === undefined) return { nullValue: null };
    if (typeof value === "boolean") return { booleanValue: value };
    if (typeof value === "number") {
      return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
    }
    if (value instanceof Date) {
      return { timestampValue: value.toISOString() };
    }
    if (typeof value === "string") return { stringValue: value };
    throw new Error(`Tipe tidak didukung untuk Firestore: ${typeof value}`);
  }

  /** Konversi dokumen Firestore REST (fields) balik ke objek JS biasa. */
  static fromFirestoreFields(fields) {
    const obj = {};
    for (const [key, value] of Object.entries(fields || {})) {
      obj[key] = FirestoreClient.fromFirestoreValue(value);
    }
    return obj;
  }

  static fromFirestoreValue(value) {
    if ("nullValue" in value) return null;
    if ("booleanValue" in value) return value.booleanValue;
    if ("integerValue" in value) return parseInt(value.integerValue, 10);
    if ("doubleValue" in value) return value.doubleValue;
    if ("timestampValue" in value) return new Date(value.timestampValue);
    if ("stringValue" in value) return value.stringValue;
    return undefined;
  }

  /**
   * Ambil satu dokumen berdasarkan collection + document ID.
   * @returns {Promise<object|null>} null kalau tidak ditemukan
   */
  async getDocument(collection, docId) {
    const token = await this._getAccessToken();
    const res = await fetch(`${this._baseUrl()}/${collection}/${encodeURIComponent(docId)}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.status === 404) return null;
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Gagal mengambil dokumen ${collection}/${docId}: ${res.status} ${text}`);
    }
    const json = await res.json();
    return FirestoreClient.fromFirestoreFields(json.fields);
  }

  /**
   * Buat dokumen baru dengan document ID eksplisit.
   * Gagal (throw) kalau dokumen dengan ID tsb sudah ada.
   */
  async createDocument(collection, docId, data) {
    const token = await this._getAccessToken();
    const url = `${this._baseUrl()}/${collection}?documentId=${encodeURIComponent(docId)}`;
    const res = await fetch(url, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ fields: FirestoreClient.toFirestoreFields(data) }),
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Gagal membuat dokumen ${collection}/${docId}: ${res.status} ${text}`);
    }
    const json = await res.json();
    return FirestoreClient.fromFirestoreFields(json.fields);
  }

  /**
   * Update (merge) field-field tertentu pada dokumen yang sudah ada.
   * Hanya field yang disebutkan di `data` yang akan diubah (field lain tetap).
   */
  async updateDocument(collection, docId, data) {
    const token = await this._getAccessToken();
    const fieldPaths = Object.keys(data).map((k) => `updateMask.fieldPaths=${encodeURIComponent(k)}`).join("&");
    const url = `${this._baseUrl()}/${collection}/${encodeURIComponent(docId)}?${fieldPaths}`;
    const res = await fetch(url, {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ fields: FirestoreClient.toFirestoreFields(data) }),
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Gagal mengupdate dokumen ${collection}/${docId}: ${res.status} ${text}`);
    }
    const json = await res.json();
    return FirestoreClient.fromFirestoreFields(json.fields);
  }

  async deleteDocument(collection, docId) {
    const token = await this._getAccessToken();
    const res = await fetch(`${this._baseUrl()}/${collection}/${encodeURIComponent(docId)}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.status === 404) return false;
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Gagal menghapus dokumen ${collection}/${docId}: ${res.status} ${text}`);
    }
    return true;
  }

  /**
   * List semua document ID dalam sebuah collection (pakai pagination internal).
   * `pageSize` dan limit total dijaga supaya tidak kebablasan pada collection besar.
   */
  async listDocumentIds(collection, { maxResults = 500 } = {}) {
    const token = await this._getAccessToken();
    let ids = [];
    let pageToken = undefined;

    do {
      const params = new URLSearchParams({ pageSize: "300" });
      if (pageToken) params.set("pageToken", pageToken);
      const res = await fetch(`${this._baseUrl()}/${collection}?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(`Gagal listing collection ${collection}: ${res.status} ${text}`);
      }
      const json = await res.json();
      const docs = json.documents || [];
      ids.push(...docs.map((d) => d.name.split("/").pop()));
      pageToken = json.nextPageToken;
    } while (pageToken && ids.length < maxResults);

    return ids.slice(0, maxResults);
  }

  /**
   * Query dokumen dalam collection dengan filter kesamaan field sederhana
   * (dipakai untuk cari lisensi berdasarkan deviceId).
   */
  async queryByField(collection, fieldName, value, { limit = 20 } = {}) {
    const token = await this._getAccessToken();
    const body = {
      structuredQuery: {
        from: [{ collectionId: collection }],
        where: {
          fieldFilter: {
            field: { fieldPath: fieldName },
            op: "EQUAL",
            value: FirestoreClient.toFirestoreValue(value),
          },
        },
        limit,
      },
    };
    const res = await fetch(
      `https://firestore.googleapis.com/v1/projects/${this.projectId}/databases/(default)/documents:runQuery`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      }
    );
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Gagal query ${collection} where ${fieldName}=${value}: ${res.status} ${text}`);
    }
    const results = await res.json();
    return results
      .filter((r) => r.document)
      .map((r) => ({
        id: r.document.name.split("/").pop(),
        ...FirestoreClient.fromFirestoreFields(r.document.fields),
      }));
  }
}

module.exports = { FirestoreClient };
