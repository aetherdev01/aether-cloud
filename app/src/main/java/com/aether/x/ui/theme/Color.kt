package com.aether.x.ui.theme

import androidx.compose.ui.graphics.Color

// === AetherX Dark UI ===
// Palet gelap "hacker/tactical" — biru-abu dingin di atas hitam pekat,
// bukan lagi M3 default. Referensi: dashboard gelap dengan aksen biru pucat.

// Background & surface
val BgVoid = Color(0xFF0A0A0C)            // background paling belakang (hampir hitam)
val BgBase = Color(0xFF0D0D10)            // background dasar layar
val SurfaceCard = Color(0xFF17171C)       // kartu section utama
val SurfaceCardAlt = Color(0xFF1C1C22)    // kartu bertingkat / hero card
val SurfaceRaised = Color(0xFF222229)     // elemen di atas kartu (track switch off, dsb)
val StrokeSubtle = Color(0xFF2A2A32)      // border/divider halus

// Aksen biru (primary)
val AccentBlue = Color(0xFF7FA8FF)        // biru pucat khas referensi (judul, ikon aktif)
val AccentBlueSoft = Color(0xFFAFC6FF)    // biru lebih muda untuk subtitle/link
val AccentBlueDim = Color(0xFF3D4A6B)     // biru redup untuk track OFF berwarna
val OnAccentBlue = Color(0xFF0A0F1F)

// Aksen kuning/emas (locked / premium)
val AccentGold = Color(0xFFE8B84B)
val AccentGoldDim = Color(0xFF6B5A2E)

// Aksen merah (disconnected/error)
val AccentRed = Color(0xFFFF6B5E)

// Teks
val TextPrimary = Color(0xFFF2F3F7)       // putih pudar untuk judul besar
val TextSecondary = Color(0xFFB9BAC6)     // abu terang untuk body text
val TextMuted = Color(0xFF7A7B87)         // abu redup untuk caption/disabled
val TextOnCard = Color(0xFFE7E8EE)
