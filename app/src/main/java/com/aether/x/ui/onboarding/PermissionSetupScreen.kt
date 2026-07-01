package com.aether.x.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aether.x.R
import com.aether.x.core.permission.PrivilegeManager
import com.aether.x.ui.components.PermissionMethodCard

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val SHIZUKU_PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE"

@Composable
fun PermissionSetupScreen(
    onContinue: () -> Unit,
    requireAccessToContinue: Boolean = true,
) {
    val context = LocalContext.current
    val status by PrivilegeManager.status.collectAsStateWithLifecycle()
    val canContinue = !requireAccessToContinue || status.hasAccess

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (requireAccessToContinue) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canContinue) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (canContinue) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (canContinue) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                    Text(
                        text = if (canContinue) {
                            stringResource(R.string.setup_ready_hint)
                        } else {
                            stringResource(R.string.setup_required_banner)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (canContinue) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                    )
                }
            }

            PermissionMethodCard(
                title = stringResource(R.string.setup_method_shizuku),
                description = stringResource(R.string.setup_method_shizuku_desc),
                statusText = when {
                    !status.shizukuAvailable -> stringResource(R.string.setup_status_not_installed)
                    status.shizukuGranted -> stringResource(R.string.setup_status_granted)
                    else -> stringResource(R.string.setup_status_not_granted)
                },
                granted = status.shizukuGranted,
                actionLabel = when {
                    !status.shizukuAvailable -> stringResource(R.string.setup_action_install_shizuku)
                    status.shizukuGranted -> stringResource(R.string.setup_action_open_shizuku)
                    else -> stringResource(R.string.setup_action_request)
                },
                onAction = {
                    when {
                        !status.shizukuAvailable -> openShizukuStorePage(context)
                        status.shizukuGranted -> openShizukuApp(context)
                        else -> PrivilegeManager.requestShizukuPermission()
                    }
                },
            )

            if (!status.hasAccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.setup_or_divider),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            PermissionMethodCard(
                title = stringResource(R.string.setup_method_root),
                description = stringResource(R.string.setup_method_root_desc),
                statusText = when {
                    status.checkingRoot -> stringResource(R.string.setup_status_checking)
                    status.rootGranted -> stringResource(R.string.setup_status_granted)
                    status.shizukuGranted -> stringResource(R.string.setup_status_not_needed)
                    else -> stringResource(R.string.setup_status_not_granted)
                },
                granted = status.rootGranted,
                actionLabel = stringResource(R.string.setup_action_request),
                onAction = { PrivilegeManager.requestRoot() },
            )

            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onContinue,
                    enabled = canContinue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.setup_action_continue))
                }
                AnimatedVisibility(visible = !canContinue, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = stringResource(R.string.setup_locked_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun openShizukuApp(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
    if (intent != null) {
        context.startActivity(intent)
    } else {
        openShizukuStorePage(context)
    }
}

private fun openShizukuStorePage(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_PLAY_STORE_URL)))
    }
}
