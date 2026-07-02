require("dotenv").config();
const TelegramBot = require("node-telegram-bot-api");

const {
  createLicense,
  getLicense,
  updateLicense,
  deleteLicense,
  bindDevice,
  unbindDevice,
  findLicensesByDevice,
  listAllTokens,
  countAllLicenses,
} = require("./lib/redisClient");
const { generateUniqueToken } = require("./lib/tokenGenerator");
const { formatLicenseCard, isValidToken, isValidDeviceId, escapeMd } = require("./lib/format");

const TOKEN = process.env.TELEGRAM_BOT_TOKEN;
const ADMIN_ID = String(process.env.ADMIN_TELEGRAM_ID || "");

if (!TOKEN) {
  console.error("TELEGRAM_BOT_TOKEN belum diisi di .env");
  process.exit(1);
}
if (!ADMIN_ID) {
  console.error("ADMIN_TELEGRAM_ID belum diisi di .env");
  process.exit(1);
}

const bot = new TelegramBot(TOKEN, { polling: true });

// ── State percakapan sederhana per chat (untuk alur multi-langkah: edit, generate custom, dsb) ──
// Map<chatId, { action: string, step: string, data: object }>
const sessions = new Map();

function isAdmin(msg) {
  return String(msg.from.id) === ADMIN_ID;
}

function requireAdmin(msg) {
  if (!isAdmin(msg)) {
    bot.sendMessage(msg.chat.id, "⛔ Kamu tidak punya akses ke perintah ini.");
    return false;
  }
  return true;
}

function clearSession(chatId) {
  sessions.delete(chatId);
}

function mainMenuKeyboard() {
  return {
    reply_markup: {
      inline_keyboard: [
        [
          { text: "🎫 Generate Lisensi", callback_data: "menu:generate" },
          { text: "🔍 Cek Lisensi", callback_data: "menu:check" },
        ],
        [
          { text: "✏️ Edit Lisensi", callback_data: "menu:edit" },
          { text: "🗑️ Hapus Lisensi", callback_data: "menu:delete" },
        ],
        [
          { text: "📱 Cek Device ID", callback_data: "menu:device" },
          { text: "🔓 Reset Device", callback_data: "menu:unbind" },
        ],
        [
          { text: "📋 Daftar Lisensi", callback_data: "menu:list" },
          { text: "📊 Statistik", callback_data: "menu:stats" },
        ],
      ],
    },
  };
}

// ─────────────────────────────────────────────────────────
// /start & /help
// ─────────────────────────────────────────────────────────
bot.onText(/^\/start$/, (msg) => {
  const chatId = msg.chat.id;
  clearSession(chatId);
  const name = escapeMd(msg.from.first_name || "");
  bot.sendMessage(
    chatId,
    `👋 Halo, ${name}\\!\n\n` +
      `Bot manajemen lisensi *AetherX* siap dipakai\\.\n` +
      `Pilih menu di bawah, atau ketik /help untuk daftar perintah lengkap\\.`,
    { parse_mode: "MarkdownV2", ...mainMenuKeyboard() }
  );
});

bot.onText(/^\/help$/, (msg) => {
  const helpText = [
    "*Perintah tersedia:*",
    "",
    "/generate — Buat lisensi baru (token 7 karakter acak)",
    "/check <token> — Lihat detail lisensi",
    "/edit <token> — Edit status/plan/catatan/expiry lisensi",
    "/delete <token> — Hapus lisensi permanen",
    "/device <deviceId> — Cari lisensi yang terpasang di device ID tsb",
    "/unbind <token> — Lepas device dari lisensi (biar bisa dipakai device lain)",
    "/list — Daftar semua token lisensi",
    "/stats — Statistik jumlah lisensi",
    "/cancel — Batalkan proses yang sedang berjalan",
    "",
    "Semua perintah admin di atas hanya bisa dipakai oleh admin yang terdaftar\\.",
  ].join("\n");
  bot.sendMessage(msg.chat.id, helpText, { parse_mode: "MarkdownV2" });
});

bot.onText(/^\/cancel$/, (msg) => {
  clearSession(msg.chat.id);
  bot.sendMessage(msg.chat.id, "❎ Proses dibatalkan.", mainMenuKeyboard());
});

// ─────────────────────────────────────────────────────────
// GENERATE LICENSE
// /generate [plan] [maxDevices] [hariBerlaku]
// Contoh: /generate lifetime 1
//         /generate monthly 2 30
// ─────────────────────────────────────────────────────────
bot.onText(/^\/generate(?:\s+(.*))?$/, async (msg, match) => {
  if (!requireAdmin(msg)) return;
  const chatId = msg.chat.id;
  const args = (match[1] || "").trim().split(/\s+/).filter(Boolean);

  const plan = args[0] || "default";
  const maxDevices = args[1] ? parseInt(args[1], 10) : 1;
  const days = args[2] ? parseInt(args[2], 10) : 0;

  try {
    const token = await generateUniqueToken(require("./lib/redisClient").redis);
    const expiresAt = days > 0 ? Date.now() + days * 24 * 60 * 60 * 1000 : 0;

    const license = await createLicense({
      token,
      plan,
      maxDevices: Number.isFinite(maxDevices) && maxDevices > 0 ? maxDevices : 1,
      expiresAt,
    });

    await bot.sendMessage(
      chatId,
      `✅ *Lisensi baru berhasil dibuat\\!*\n\n${formatLicenseCard(license)}`,
      { parse_mode: "MarkdownV2" }
    );
  } catch (err) {
    console.error(err);
    bot.sendMessage(chatId, `❌ Gagal generate lisensi: ${err.message}`);
  }
});

// ─────────────────────────────────────────────────────────
// CHECK LICENSE
// /check <token>
// ─────────────────────────────────────────────────────────
bot.onText(/^\/check(?:\s+(.*))?$/, async (msg, match) => {
  const chatId = msg.chat.id;
  const token = (match[1] || "").trim();

  if (!token) {
    sessions.set(chatId, { action: "check", step: "waiting_token" });
    return bot.sendMessage(chatId, "Kirim token lisensi yang mau dicek (7 karakter):");
  }
  await handleCheck(chatId, token);
});

async function handleCheck(chatId, token) {
  if (!isValidToken(token)) {
    return bot.sendMessage(chatId, "⚠️ Format token tidak valid. Token harus 7 karakter huruf/angka.");
  }
  const license = await getLicense(token);
  if (!license) {
    return bot.sendMessage(chatId, `❌ Token \`${token}\` tidak ditemukan.`, { parse_mode: "Markdown" });
  }
  bot.sendMessage(chatId, formatLicenseCard(license), { parse_mode: "MarkdownV2" });
}

// ─────────────────────────────────────────────────────────
// DEVICE ID LOOKUP
// /device <deviceId>
// ─────────────────────────────────────────────────────────
bot.onText(/^\/device(?:\s+(.*))?$/, async (msg, match) => {
  const chatId = msg.chat.id;
  const deviceId = (match[1] || "").trim();

  if (!deviceId) {
    sessions.set(chatId, { action: "device", step: "waiting_device" });
    return bot.sendMessage(chatId, "Kirim Device ID yang mau dicek:");
  }
  await handleDeviceCheck(chatId, deviceId);
});

async function handleDeviceCheck(chatId, deviceId) {
  if (!isValidDeviceId(deviceId)) {
    return bot.sendMessage(chatId, "⚠️ Device ID tidak valid (minimal 4 karakter).");
  }
  const licenses = await findLicensesByDevice(deviceId);
  if (licenses.length === 0) {
    return bot.sendMessage(chatId, `📱 Device ID \`${deviceId}\` belum terdaftar di lisensi manapun.`, {
      parse_mode: "Markdown",
    });
  }
  const cards = licenses.map(formatLicenseCard).join("\n\n───\n\n");
  bot.sendMessage(chatId, `📱 Lisensi yang terkait Device ID ini:\n\n${cards}`, { parse_mode: "MarkdownV2" });
}

// ─────────────────────────────────────────────────────────
// UNBIND DEVICE
// /unbind <token>
// ─────────────────────────────────────────────────────────
bot.onText(/^\/unbind(?:\s+(.*))?$/, async (msg, match) => {
  if (!requireAdmin(msg)) return;
  const chatId = msg.chat.id;
  const token = (match[1] || "").trim();

  if (!token) {
    sessions.set(chatId, { action: "unbind", step: "waiting_token" });
    return bot.sendMessage(chatId, "Kirim token lisensi yang device-nya mau direset:");
  }
  await handleUnbind(chatId, token);
});

async function handleUnbind(chatId, token) {
  if (!isValidToken(token)) {
    return bot.sendMessage(chatId, "⚠️ Format token tidak valid.");
  }
  const license = await getLicense(token);
  if (!license) {
    return bot.sendMessage(chatId, `❌ Token \`${token}\` tidak ditemukan.`, { parse_mode: "Markdown" });
  }
  await unbindDevice(token);
  bot.sendMessage(chatId, `🔓 Device berhasil dilepas dari token \`${token}\`. Lisensi bisa dipakai di device baru.`, {
    parse_mode: "Markdown",
  });
}

// ─────────────────────────────────────────────────────────
// DELETE LICENSE
// /delete <token>
// ─────────────────────────────────────────────────────────
bot.onText(/^\/delete(?:\s+(.*))?$/, async (msg, match) => {
  if (!requireAdmin(msg)) return;
  const chatId = msg.chat.id;
  const token = (match[1] || "").trim();

  if (!token) {
    sessions.set(chatId, { action: "delete", step: "waiting_token" });
    return bot.sendMessage(chatId, "Kirim token lisensi yang mau dihapus:");
  }
  await confirmDelete(chatId, token);
});

async function confirmDelete(chatId, token) {
  if (!isValidToken(token)) {
    return bot.sendMessage(chatId, "⚠️ Format token tidak valid.");
  }
  const license = await getLicense(token);
  if (!license) {
    return bot.sendMessage(chatId, `❌ Token \`${token}\` tidak ditemukan.`, { parse_mode: "Markdown" });
  }
  sessions.set(chatId, { action: "delete", step: "confirm", data: { token } });
  bot.sendMessage(
    chatId,
    `⚠️ Yakin mau hapus lisensi ini secara permanen?\n\n${formatLicenseCard(license)}`,
    {
      parse_mode: "MarkdownV2",
      reply_markup: {
        inline_keyboard: [
          [
            { text: "✅ Ya, hapus", callback_data: `delete_confirm:${token}` },
            { text: "❌ Batal", callback_data: "delete_cancel" },
          ],
        ],
      },
    }
  );
}

// ─────────────────────────────────────────────────────────
// LIST & STATS
// ─────────────────────────────────────────────────────────
bot.onText(/^\/list$/, async (msg) => {
  if (!requireAdmin(msg)) return;
  const chatId = msg.chat.id;
  const tokens = await listAllTokens();
  if (tokens.length === 0) {
    return bot.sendMessage(chatId, "Belum ada lisensi yang dibuat.");
  }
  const preview = tokens.slice(0, 50);
  const text =
    `📋 *Total ${tokens.length} lisensi*${tokens.length > 50 ? " (menampilkan 50 pertama)" : ""}:\n\n` +
    preview.map((t) => `\`${t}\``).join("\n");
  bot.sendMessage(chatId, text, { parse_mode: "Markdown" });
});

bot.onText(/^\/stats$/, async (msg) => {
  if (!requireAdmin(msg)) return;
  const chatId = msg.chat.id;
  const total = await countAllLicenses();
  bot.sendMessage(chatId, `📊 Total lisensi tersimpan: *${total}*`, { parse_mode: "Markdown" });
});

// ─────────────────────────────────────────────────────────
// EDIT LICENSE (conversation flow)
// /edit <token>
// ─────────────────────────────────────────────────────────
bot.onText(/^\/edit(?:\s+(.*))?$/, async (msg, match) => {
  if (!requireAdmin(msg)) return;
  const chatId = msg.chat.id;
  const token = (match[1] || "").trim();

  if (!token) {
    sessions.set(chatId, { action: "edit", step: "waiting_token" });
    return bot.sendMessage(chatId, "Kirim token lisensi yang mau diedit:");
  }
  await startEditFlow(chatId, token);
});

async function startEditFlow(chatId, token) {
  if (!isValidToken(token)) {
    return bot.sendMessage(chatId, "⚠️ Format token tidak valid.");
  }
  const license = await getLicense(token);
  if (!license) {
    return bot.sendMessage(chatId, `❌ Token \`${token}\` tidak ditemukan.`, { parse_mode: "Markdown" });
  }
  sessions.set(chatId, { action: "edit", step: "choose_field", data: { token } });
  bot.sendMessage(chatId, `✏️ Edit lisensi \`${token}\`. Pilih field yang mau diubah:`, {
    parse_mode: "Markdown",
    reply_markup: {
      inline_keyboard: [
        [
          { text: "Status", callback_data: "edit_field:status" },
          { text: "Plan", callback_data: "edit_field:plan" },
        ],
        [
          { text: "Maks Device", callback_data: "edit_field:maxDevices" },
          { text: "Expiry", callback_data: "edit_field:expiresAt" },
        ],
        [
          { text: "Device ID", callback_data: "edit_field:deviceId" },
          { text: "Catatan", callback_data: "edit_field:note" },
        ],
        [{ text: "❌ Batal", callback_data: "edit_cancel" }],
      ],
    },
  });
}

// ─────────────────────────────────────────────────────────
// CALLBACK QUERY HANDLER (tombol inline)
// ─────────────────────────────────────────────────────────
bot.on("callback_query", async (query) => {
  const chatId = query.message.chat.id;
  const data = query.data;
  const msg = { chat: { id: chatId }, from: query.from };

  bot.answerCallbackQuery(query.id).catch(() => {});

  // Menu utama
  if (data.startsWith("menu:")) {
    const key = data.split(":")[1];
    if (key === "generate") {
      if (!requireAdmin(msg)) return;
      sessions.set(chatId, { action: "generate", step: "ask_plan" });
      return bot.sendMessage(
        chatId,
        "🎫 Bikin lisensi baru.\nKirim nama plan (mis. `monthly`, `lifetime`), atau ketik `-` untuk pakai default:",
        { parse_mode: "Markdown" }
      );
    }
    if (key === "check") {
      sessions.set(chatId, { action: "check", step: "waiting_token" });
      return bot.sendMessage(chatId, "Kirim token lisensi yang mau dicek (7 karakter):");
    }
    if (key === "edit") {
      if (!requireAdmin(msg)) return;
      sessions.set(chatId, { action: "edit", step: "waiting_token" });
      return bot.sendMessage(chatId, "Kirim token lisensi yang mau diedit:");
    }
    if (key === "delete") {
      if (!requireAdmin(msg)) return;
      sessions.set(chatId, { action: "delete", step: "waiting_token" });
      return bot.sendMessage(chatId, "Kirim token lisensi yang mau dihapus:");
    }
    if (key === "device") {
      sessions.set(chatId, { action: "device", step: "waiting_device" });
      return bot.sendMessage(chatId, "Kirim Device ID yang mau dicek:");
    }
    if (key === "unbind") {
      if (!requireAdmin(msg)) return;
      sessions.set(chatId, { action: "unbind", step: "waiting_token" });
      return bot.sendMessage(chatId, "Kirim token lisensi yang device-nya mau direset:");
    }
    if (key === "list") {
      if (!requireAdmin(msg)) return;
      const tokens = await listAllTokens();
      if (tokens.length === 0) return bot.sendMessage(chatId, "Belum ada lisensi yang dibuat.");
      const preview = tokens.slice(0, 50);
      return bot.sendMessage(
        chatId,
        `📋 *Total ${tokens.length} lisensi*:\n\n` + preview.map((t) => `\`${t}\``).join("\n"),
        { parse_mode: "Markdown" }
      );
    }
    if (key === "stats") {
      if (!requireAdmin(msg)) return;
      const total = await countAllLicenses();
      return bot.sendMessage(chatId, `📊 Total lisensi tersimpan: *${total}*`, { parse_mode: "Markdown" });
    }
    return;
  }

  // Konfirmasi hapus
  if (data.startsWith("delete_confirm:")) {
    if (!requireAdmin(msg)) return;
    const token = data.split(":")[1];
    const ok = await deleteLicense(token);
    clearSession(chatId);
    return bot.sendMessage(
      chatId,
      ok ? `🗑️ Token \`${token}\` berhasil dihapus.` : `❌ Gagal menghapus token \`${token}\`.`,
      { parse_mode: "Markdown" }
    );
  }
  if (data === "delete_cancel") {
    clearSession(chatId);
    return bot.sendMessage(chatId, "❎ Penghapusan dibatalkan.");
  }

  // Pilihan field edit
  if (data.startsWith("edit_field:")) {
    if (!requireAdmin(msg)) return;
    const field = data.split(":")[1];
    const session = sessions.get(chatId);
    if (!session || session.action !== "edit") {
      return bot.sendMessage(chatId, "Sesi edit tidak ditemukan, mulai lagi dengan /edit <token>");
    }
    session.step = "awaiting_value";
    session.data.field = field;
    sessions.set(chatId, session);

    const prompts = {
      status: "Kirim status baru: `active`, `suspended`, atau `revoked`",
      plan: "Kirim nama plan baru (mis. `monthly`, `lifetime`):",
      maxDevices: "Kirim jumlah maksimal device (angka):",
      expiresAt: "Kirim jumlah hari masa berlaku dari sekarang (angka), atau `0` untuk tidak pernah kadaluarsa:",
      deviceId: "Kirim Device ID baru untuk dikunci ke lisensi ini, atau `-` untuk mengosongkan:",
      note: "Kirim catatan baru:",
    };
    return bot.sendMessage(chatId, prompts[field] || "Kirim nilai baru:", { parse_mode: "Markdown" });
  }
  if (data === "edit_cancel") {
    clearSession(chatId);
    return bot.sendMessage(chatId, "❎ Proses edit dibatalkan.");
  }
});

// ─────────────────────────────────────────────────────────
// MESSAGE HANDLER — dipakai untuk melanjutkan conversation flow
// (generate custom, check/device/delete/unbind/edit tanpa argumen)
// ─────────────────────────────────────────────────────────
bot.on("message", async (msg) => {
  if (!msg.text || msg.text.startsWith("/")) return; // command sudah ditangani onText
  const chatId = msg.chat.id;
  const session = sessions.get(chatId);
  if (!session) return;

  const text = msg.text.trim();

  try {
    // ── CHECK ──
    if (session.action === "check" && session.step === "waiting_token") {
      clearSession(chatId);
      return handleCheck(chatId, text);
    }

    // ── DEVICE ──
    if (session.action === "device" && session.step === "waiting_device") {
      clearSession(chatId);
      return handleDeviceCheck(chatId, text);
    }

    // ── UNBIND ──
    if (session.action === "unbind" && session.step === "waiting_token") {
      clearSession(chatId);
      return handleUnbind(chatId, text);
    }

    // ── DELETE ──
    if (session.action === "delete" && session.step === "waiting_token") {
      return confirmDelete(chatId, text);
    }

    // ── EDIT: minta token dulu ──
    if (session.action === "edit" && session.step === "waiting_token") {
      return startEditFlow(chatId, text);
    }

    // ── EDIT: menerima nilai baru untuk field terpilih ──
    if (session.action === "edit" && session.step === "awaiting_value") {
      if (!requireAdmin(msg)) return;
      const { token, field } = session.data;
      const updates = {};

      if (field === "status") {
        if (!["active", "suspended", "revoked"].includes(text)) {
          return bot.sendMessage(chatId, "⚠️ Status harus salah satu dari: active, suspended, revoked");
        }
        updates.status = text;
      } else if (field === "plan") {
        updates.plan = text;
      } else if (field === "maxDevices") {
        const n = parseInt(text, 10);
        if (!Number.isFinite(n) || n <= 0) {
          return bot.sendMessage(chatId, "⚠️ Masukkan angka yang valid, lebih dari 0.");
        }
        updates.maxDevices = n;
      } else if (field === "expiresAt") {
        const days = parseInt(text, 10);
        if (!Number.isFinite(days) || days < 0) {
          return bot.sendMessage(chatId, "⚠️ Masukkan angka hari yang valid (0 = tidak pernah kadaluarsa).");
        }
        updates.expiresAt = days > 0 ? Date.now() + days * 24 * 60 * 60 * 1000 : 0;
      } else if (field === "deviceId") {
        if (text === "-") {
          await unbindDevice(token);
        } else {
          if (!isValidDeviceId(text)) {
            return bot.sendMessage(chatId, "⚠️ Device ID tidak valid (minimal 4 karakter).");
          }
          await bindDevice(token, text);
        }
      } else if (field === "note") {
        updates.note = text;
      }

      if (Object.keys(updates).length > 0) {
        await updateLicense(token, updates);
      }

      clearSession(chatId);
      const updated = await getLicense(token);
      return bot.sendMessage(chatId, `✅ Lisensi berhasil diupdate\\!\n\n${formatLicenseCard(updated)}`, {
        parse_mode: "MarkdownV2",
      });
    }

    // ── GENERATE (flow dari tombol menu) ──
    if (session.action === "generate" && session.step === "ask_plan") {
      session.data = { plan: text === "-" ? "default" : text };
      session.step = "ask_maxDevices";
      sessions.set(chatId, session);
      return bot.sendMessage(chatId, "Berapa maksimal device yang boleh pakai lisensi ini? (angka, mis. `1`)", {
        parse_mode: "Markdown",
      });
    }
    if (session.action === "generate" && session.step === "ask_maxDevices") {
      const n = parseInt(text, 10);
      if (!Number.isFinite(n) || n <= 0) {
        return bot.sendMessage(chatId, "⚠️ Masukkan angka yang valid, lebih dari 0.");
      }
      session.data.maxDevices = n;
      session.step = "ask_days";
      sessions.set(chatId, session);
      return bot.sendMessage(
        chatId,
        "Berapa hari masa berlaku lisensi ini? Kirim `0` untuk tidak pernah kadaluarsa:",
        { parse_mode: "Markdown" }
      );
    }
    if (session.action === "generate" && session.step === "ask_days") {
      const days = parseInt(text, 10);
      if (!Number.isFinite(days) || days < 0) {
        return bot.sendMessage(chatId, "⚠️ Masukkan angka hari yang valid (0 = tidak pernah kadaluarsa).");
      }
      clearSession(chatId);
      const token = await generateUniqueToken(require("./lib/redisClient").redis);
      const expiresAt = days > 0 ? Date.now() + days * 24 * 60 * 60 * 1000 : 0;
      const license = await createLicense({
        token,
        plan: session.data.plan,
        maxDevices: session.data.maxDevices,
        expiresAt,
      });
      return bot.sendMessage(chatId, `✅ *Lisensi baru berhasil dibuat\\!*\n\n${formatLicenseCard(license)}`, {
        parse_mode: "MarkdownV2",
      });
    }
  } catch (err) {
    console.error(err);
    clearSession(chatId);
    bot.sendMessage(chatId, `❌ Terjadi error: ${err.message}`);
  }
});

bot.on("polling_error", (err) => {
  console.error("Polling error:", err.message);
});

console.log("🤖 Bot AetherX License Manager berjalan (polling mode)...");
