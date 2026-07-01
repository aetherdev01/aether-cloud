package com.aether.x.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aether.x.R

/**
 * Tombol mengambang di sisi kanan bawah layar untuk langsung membuka game
 * yang terdeteksi (mis. Free Fire), memakai logo AetherX sebagai ikonnya.
 * Dipisah dari daftar tweak supaya selalu terlihat & mudah dijangkau ibu jari
 * tanpa perlu scroll.
 */
@Composable
fun GameFab(
    gameName: String,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_aetherx_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 0.dp),
                )
            },
            text = {
                Text(text = stringResource(R.string.tweak_fab_open_game, gameName))
            },
        )
    }
}
