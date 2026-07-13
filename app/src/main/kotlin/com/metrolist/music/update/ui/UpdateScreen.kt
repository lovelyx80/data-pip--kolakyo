package com.metrolist.music.update.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.update.UpdateState
import com.metrolist.music.update.normalizeLineBreaks
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single entry point that renders whichever screen [updateState] currently
 * calls for. Crossfade animates smoothly between states (including fading
 * away to nothing when the server comes back Normal) with no app restart.
 */
@Composable
fun UpdateScreenOverlay(
    updateState: UpdateState,
    onDismissUpdate: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    // Block back button while Maintenance or Force Update is blocking the app.
    if (updateState is UpdateState.Maintenance || updateState is UpdateState.ForceUpdate) {
        BackHandler(enabled = true) { /* consume - no bypass */ }
    }

    Crossfade(
        targetState = updateState,
        animationSpec = tween(300),
        label = "update_state_crossfade",
    ) { state ->
        when (state) {
            is UpdateState.Loading, is UpdateState.Normal -> {
                // Nothing to draw - lets the real app content show through.
                Box(Modifier)
            }

            is UpdateState.Maintenance -> {
                val isChecking by viewModel.isChecking.collectAsState()
                val lastCheckedTime by viewModel.lastCheckedTime.collectAsState()
                val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState()

                MaintenanceOverlay(
                    message = state.message,
                    isChecking = isChecking,
                    lastCheckedTime = lastCheckedTime,
                    isNetworkAvailable = isNetworkAvailable,
                )
            }

            is UpdateState.ForceUpdate -> {
                ForceUpdateOverlay(
                    message = state.message,
                    updateUrl = state.updateUrl,
                )
            }

            is UpdateState.OptionalUpdate -> {
                UpdateAvailableDialog(
                    latestVersion = state.latestVersion,
                    message = state.message,
                    updateUrl = state.updateUrl,
                    onDismiss = onDismissUpdate,
                )
            }
        }
    }
}

@Composable
private fun MaintenanceOverlay(
    message: String,
    isChecking: Boolean,
    lastCheckedTime: Long,
    isNetworkAvailable: Boolean,
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = if (lastCheckedTime > 0) dateFormat.format(Date(lastCheckedTime)) else "--:--:--"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                painter = painterResource(R.drawable.warning),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Server Under Maintenance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                // Requirement #7: \n / \n\n / \r\n all render as real line breaks.
                text = if (!isNetworkAvailable) "No Internet Connection" else message.normalizeLineBreaks(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (!isNetworkAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp,
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Checking server...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Next check in a few seconds",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.cached),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Last checked: $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun ForceUpdateOverlay(
    message: String,
    updateUrl: String,
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.update),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Update Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message.normalizeLineBreaks(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    try {
                        uriHandler.openUri(updateUrl)
                    } catch (_: Exception) { }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(
                    text = "Update Now",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    latestVersion: String,
    message: String,
    updateUrl: String,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Version $latestVersion Available",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message.normalizeLineBreaks(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        uriHandler.openUri(updateUrl)
                    } catch (_: Exception) { }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(text = "Update Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Later")
            }
        },
    )
}

@Composable
fun PopupDialog(popupState: PopupState?) {
    if (popupState == null) return

    val popup = popupState.config
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = popupState.onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = popup.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = popup.description.normalizeLineBreaks(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            if (popup.primaryButton != null) {
                Button(
                    onClick = {
                        popup.primaryButton.url?.let { url ->
                            try {
                                uriHandler.openUri(url)
                            } catch (_: Exception) { }
                        }
                        popupState.onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(text = popup.primaryButton.text, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (popup.secondaryButton != null) {
                    TextButton(
                        onClick = {
                            popup.secondaryButton.url?.let { url ->
                                try {
                                    uriHandler.openUri(url)
                                } catch (_: Exception) { }
                            }
                            popupState.onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = popup.secondaryButton.text)
                    }
                }

                if (popup.dontShowAgain) {
                    TextButton(
                        onClick = popupState.onDontShowAgain,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "Don't show again")
                    }
                }

                if (popup.cancelButton != null) {
                    TextButton(
                        onClick = popupState.onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = popup.cancelButton.text)
                    }
                }
            }
        },
    )
}
