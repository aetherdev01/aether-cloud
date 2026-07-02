function escapeMd(text) {
  // Escape karakter spesial MarkdownV2 Telegram
  return String(text).replace(/[_*[\]()~`>#+\-=|{}.!]/g, "\\$&");
}

function formatDate(ms) {
  if (!ms) return "—";
  const d = new Date(ms);
  return d.toLocaleString("id-ID", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Asia/Jakarta",
  }) + " WIB";
}

function statusEmoji(status) {
  switch (status) {
    case "active":
      return "🟢 Aktif";
    case "suspended":
      return "🟡 Ditangguhkan";
    case "revoked":
      return "🔴 Dicabut";
    default:
      return status;
  }
}

function formatLicenseCard(license) {
  const lines = [
    `🔑 *Token:* \`${escapeMd(license.token)}\``,
    `📌 *Status:* ${statusEmoji(license.status)}`,
    `📦 *Plan:* ${escapeMd(license.plan)}`,
    `📱 *Device terkunci:* ${license.deviceId ? "`" + escapeMd(license.deviceId) + "`" : "belum ada"}`,
    `🔢 *Maks device:* ${license.maxDevices}`,
    `🗓️ *Dibuat:* ${escapeMd(formatDate(license.createdAt))}`,
    `⏳ *Kadaluarsa:* ${license.expiresAt ? escapeMd(formatDate(license.expiresAt)) : "tidak pernah"}`,
    `✅ *Diaktivasi:* ${license.activatedAt ? escapeMd(formatDate(license.activatedAt)) : "belum diaktivasi"}`,
  ];
  if (license.note) {
    lines.push(`📝 *Catatan:* ${escapeMd(license.note)}`);
  }
  return lines.join("\n");
}

function isValidToken(token) {
  return /^[a-zA-Z0-9]{7}$/.test(token);
}

function isValidDeviceId(deviceId) {
  return typeof deviceId === "string" && deviceId.length >= 4 && deviceId.length <= 128;
}

module.exports = { escapeMd, formatDate, statusEmoji, formatLicenseCard, isValidToken, isValidDeviceId };
