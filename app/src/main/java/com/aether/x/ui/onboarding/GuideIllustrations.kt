package com.aether.x.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aether.x.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ilustrasi animasi ringan per halaman panduan, pengganti kotak angka statis
 * "1, 2, 3, 4". Dibuat murni pakai Compose animation + Canvas (tanpa aset
 * Lottie eksternal) supaya tetap ringan, tidak butuh file tambahan, dan
 * langsung jalan di semua device tanpa dependensi baru.
 */
@Composable
fun GuideIllustration(page: Int, modifier: Modifier = Modifier) {
    when (page) {
        0 -> WelcomeIllustration(modifier)
        1 -> TouchPrecisionIllustration(modifier)
        2 -> SafeReversibleIllustration(modifier)
        else -> ReadyToTweakIllustration(modifier)
    }
}

/** Halaman 1: logo AetherX berdenyut dikelilingi ring radar + partikel mengorbit. */
@Composable
private fun WelcomeIllustration(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "welcome")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val ringProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2200, easing = LinearEasing)),
        label = "ring",
    )
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(9000, easing = LinearEasing)),
        label = "orbit",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.22f

            listOf(0f, 0.5f).forEach { phase ->
                val p = (ringProgress + phase) % 1f
                drawCircle(
                    color = Color.White.copy(alpha = (1f - p) * 0.35f),
                    radius = baseRadius + p * size.minDimension * 0.28f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            val orbitRadius = baseRadius + size.minDimension * 0.18f
            listOf(0f, 120f, 240f).forEach { offsetDeg ->
                val angle = Math.toRadians((orbitAngle + offsetDeg).toDouble())
                val dotCenter = Offset(
                    x = center.x + (orbitRadius * cos(angle)).toFloat(),
                    y = center.y + (orbitRadius * sin(angle)).toFloat(),
                )
                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 3.dp.toPx(), center = dotCenter)
            }
        }
        Image(
            painter = painterResource(R.drawable.ic_aetherx_mark),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse },
        )
    }
}

/** Halaman 2: ripple sentuhan presisi di atas grid titik halus. */
@Composable
private fun TouchPrecisionIllustration(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "touch")
    val rippleProgress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing)),
        label = "ripple",
    )
    val tapScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tapScale",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val spacing = size.minDimension / 7f
            for (gx in -3..3) {
                for (gy in -2..2) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.18f),
                        radius = 1.6.dp.toPx(),
                        center = Offset(center.x + gx * spacing, center.y + gy * spacing),
                    )
                }
            }

            listOf(0f, 0.33f, 0.66f).forEach { phase ->
                val p = (rippleProgress + phase) % 1f
                drawCircle(
                    color = Color.White.copy(alpha = (1f - p) * 0.4f),
                    radius = size.minDimension * 0.1f + p * size.minDimension * 0.22f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.TouchApp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { scaleX = tapScale; scaleY = tapScale },
        )
    }
}

/** Halaman 3: dua ring putus-putus berputar berlawanan arah (aman & reversibel). */
@Composable
private fun SafeReversibleIllustration(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "safe")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing)),
        label = "rotation",
    )
    val reverseRotation by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(9000, easing = LinearEasing)),
        label = "reverseRotation",
    )
    val checkPulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "checkPulse",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val outerRadius = size.minDimension * 0.3f
            val innerRadius = size.minDimension * 0.21f

            rotate(rotation) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = outerRadius,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f),
                    ),
                )
            }
            rotate(reverseRotation) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = innerRadius,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f),
                    ),
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer { scaleX = checkPulse; scaleY = checkPulse },
        )
    }
}

/** Halaman 4: demo slider bergerak + ikon petir memantul (siap tweak). */
@Composable
private fun ReadyToTweakIllustration(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "ready")
    val thumbProgress by infinite.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "thumb",
    )
    val boltOffset by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bolt",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(44.dp)
                    .offset(y = boltOffset.dp)
                    .padding(bottom = 18.dp),
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(28.dp),
            ) {
                val trackY = size.height / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(0f, trackY),
                    end = Offset(size.width, trackY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                val thumbX = size.width * thumbProgress
                drawLine(
                    color = Color.White,
                    start = Offset(0f, trackY),
                    end = Offset(thumbX, trackY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 16.dp.toPx(),
                    center = Offset(thumbX, trackY),
                )
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = Offset(thumbX, trackY),
                )
            }
        }
    }
}
