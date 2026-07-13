package com.metrolist.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

private val DrawerAccent = Color(0xFFBB86FC)
private val DrawerAccentLight = Color(0xFFCE93D8)

@Composable
fun NavigationDrawerContent(
    navController: NavController,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (useLoginForBrowse, onUseLoginForBrowseChange) = rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) = rememberPreference(YtmSyncKey, true)

    val homeViewModel: HomeViewModel = hiltViewModel()
    val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
    val accountName by homeViewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by homeViewModel.accountImageUrl.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        DefaultDialog(
            onDismiss = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout_dialog_title)) },
            content = {
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                accountSettingsViewModel.logoutAndClearLibraryData(context)
                            } catch (e: Exception) {
                                Timber.e(e, "Error clearing library data")
                            }
                            onInnerTubeCookieChange("")
                            showLogoutDialog = false
                            onClose()
                        }
                    },
                ) {
                    Text(stringResource(R.string.logout_clear))
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            accountSettingsViewModel.logoutKeepData(context, onInnerTubeCookieChange)
                            showLogoutDialog = false
                            onClose()
                        }
                    },
                ) {
                    Text(stringResource(R.string.logout_keep))
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF121212),
                        Color(0xFF1A1A2E),
                        Color(0xFF0F0F23),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
                .verticalScroll(rememberScrollState())
                .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)),
        ) {
            // Header / Profile section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(DrawerAccentLight, DrawerAccent),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoggedIn && accountImageUrl != null) {
                        AsyncImage(
                            model = accountImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lovely Beats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLoggedIn) accountName else "Guest",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DrawerAccent.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (isLoggedIn) "LOGGED IN" else "FREE",
                                color = DrawerAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Account row
            NavigationDrawerItem(
                icon = painterResource(R.drawable.person),
                title = if (isLoggedIn) "Your Account" else "Sign In",
                subtitle = if (isLoggedIn) "Manage your account" else "Tap to sign in",
                trailing = if (isLoggedIn) {
                    {
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.6f),
                            ),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.action_logout),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                } else null,
                onClick = {
                    onClose()
                    if (isLoggedIn) {
                        navController.navigate("account")
                    } else {
                        navController.navigate("login")
                    }
                },
            )

            DrawerThinDivider()

            NavigationDrawerItem(
                icon = painterResource(R.drawable.cached),
                title = "More content",
                subtitle = "Browse with your account",
                trailing = {
                    Switch(
                        checked = useLoginForBrowse,
                        onCheckedChange = {
                            YouTube.useLoginForBrowse = it
                            onUseLoginForBrowseChange(it)
                        },
                        enabled = isLoggedIn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DrawerAccentLight,
                            checkedTrackColor = DrawerAccent.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        ),
                    )
                },
                enabled = isLoggedIn,
                onClick = { },
            )

            NavigationDrawerItem(
                icon = painterResource(R.drawable.cached),
                title = "YT Sync",
                subtitle = "Sync YouTube Music library",
                trailing = {
                    Switch(
                        checked = ytmSync,
                        onCheckedChange = onYtmSyncChange,
                        enabled = isLoggedIn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DrawerAccentLight,
                            checkedTrackColor = DrawerAccent.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        ),
                    )
                },
                enabled = isLoggedIn,
                onClick = { },
            )

            // Section: Navigation
            Spacer(Modifier.height(12.dp))
            DrawerSectionHeader("Navigation")

            NavigationDrawerItem(
                icon = painterResource(R.drawable.home_filled),
                title = "Home",
                onClick = {
                    onClose()
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )

            NavigationDrawerItem(
                icon = painterResource(R.drawable.library_music_filled),
                title = "Library",
                onClick = {
                    onClose()
                    navController.navigate("library") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )

            // Section: Settings
            Spacer(Modifier.height(12.dp))
            DrawerSectionHeader("Settings")

            NavigationDrawerItem(
                leading = {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp),
                    )
                },
                title = "Settings",
                subtitle = "Appearance, playback, and more",
                onClick = {
                    onClose()
                    navController.navigate("settings")
                },
            )

            // Section: Support
            Spacer(Modifier.height(12.dp))
            DrawerSectionHeader("Support")

            NavigationDrawerItem(
                icon = painterResource(R.drawable.github),
                title = "GitHub",
                subtitle = "View source code",
                onClick = {
                    uriHandler.openUri("https://github.com/anomalco/metrolist")
                },
            )

            NavigationDrawerItem(
                icon = painterResource(R.drawable.bug_report),
                title = "Report Issue",
                subtitle = "Help improve the app",
                onClick = {
                    uriHandler.openUri("https://github.com/anomalco/metrolist/issues")
                },
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DrawerSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = DrawerAccent.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp),
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun NavigationDrawerItem(
    icon: Painter? = null,
    leading: (@Composable () -> Unit)? = null,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val alpha = if (enabled) 1f else 0.38f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f * alpha),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f * alpha),
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.45f * alpha),
                )
            }
        }

        trailing?.let {
            Spacer(Modifier.width(8.dp))
            it()
        }
    }
}

@Composable
private fun DrawerThinDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.06f)),
    )
}
