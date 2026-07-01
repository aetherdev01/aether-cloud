package com.aether.x.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aether.x.R
import com.aether.x.core.permission.PrivilegeManager
import kotlinx.coroutines.delay

/**
 * Splash screen di dalam aplikasi (tampil setelah splash sistem Android 12+).
 *
 * Menggantikan layar "Siapkan Akses" lama: alih-alih menunggu pengguna menekan
 * tombol "Minta Izin" satu per satu, layar ini otomatis memicu semua dialog
 * izin yang dibutuhkan (Shizuku, root/su, tulis pengaturan sistem, overlay,
 * notifikasi) begitu aplikasi dibuka, lalu lanjut sendiri ke panduan/main
 * setelah proses selesai — tanpa memblokir pengguna kalau salah satu izin
 * ditolak atau tidak tersedia di perangkatnya.
 */
@Composable
fun SplashScreen(
    onDone: () -> Unit,
) {
    val context = LocalContext.current

    var statusLabel by remember { mutableStateOf(context.getString(R.string.splash_status_checking)) }
    var finished by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        PrivilegeManager.refreshSupportingPermissions(context)
    }

    // Saat kembali dari layar pengaturan sistem (overlay / write settings),
    // cek ulang status begitu activity kembali ke foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                PrivilegeManager.refreshSupportingPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        statusLabel = context.getString(R.string.splash_status_checking)

        // 1. Shizuku & root — otomatis memicu dialog/prompt kalau memungkinkan.
        statusLabel = context.getString(R.string.splash_status_access)
        PrivilegeManager.autoRequestAccess()

        // 2. Izin pendukung: overlay & tulis pengaturan sistem dibuka otomatis
        //    lewat halaman sistem kalau belum aktif, notifikasi lewat dialog
        //    runtime standar Android 13+.
        statusLabel = context.getString(R.string.splash_status_permissions)
        PrivilegeManager.refreshSupportingPermissions(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PrivilegeManager.status.value.notificationsGranted
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            delay(400)
        }

        if (!PrivilegeManager.status.value.overlayGranted) {
            PrivilegeManager.requestOverlayPermission(context)
            delay(600)
        }

        if (!PrivilegeManager.status.value.writeSettingsGranted) {
            PrivilegeManager.requestWriteSettings(context)
            delay(600)
        }

        // Beri sedikit jeda supaya splash tidak berkedip sekilas di perangkat cepat,
        // lalu lanjut apapun hasil izinnya — pengguna tetap bisa membuka ulang
        // izin yang terlewat lewat menu "Kelola Akses" di halaman utama.
        statusLabel = context.getString(R.string.splash_status_ready)
        delay(350)
        finished = true
    }

    LaunchedEffect(finished) {
        if (finished) onDone()
    }

    SplashScreenContent(statusLabel = statusLabel)
}

@Composable
private fun SplashScreenContent(statusLabel: String) {
    Scaffold(containerColor = Color(0xFF0A0A0C)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedBrandMark()

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp),
            )

            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .size(28.dp),
                color = Color(0xFF7FA8FF),
                strokeWidth = 2.5.dp,
            )
        }
    }
}

@Composable
private fun AnimatedBrandMark() {
    val infinite = rememberInfiniteTransition(label = "splash")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val ringProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing)),
        label = "ring",
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(32.dp))
            .graphicsLayer { /* no-op container, keeps mark centered */ },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.28f
            listOf(0f, 0.5f).forEach { phase ->
                val p = (ringProgress + phase) % 1f
                drawCircle(
                    color = Color(0xFF7FA8FF).copy(alpha = (1f - p) * 0.4f),
                    radius = baseRadius + p * size.minDimension * 0.22f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_aetherx_mark),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse },
        )
    }
}
