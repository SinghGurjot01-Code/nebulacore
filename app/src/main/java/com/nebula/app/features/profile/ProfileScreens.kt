package com.nebula.app.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nebula.app.core.common.LocalDemoMode
import com.nebula.app.core.common.LocalSnackbarHost
import com.nebula.app.core.common.Loadable
import com.nebula.app.core.common.formatDuration
import com.nebula.app.core.common.nebulaViewModel
import com.nebula.app.core.common.pretty
import com.nebula.app.design.components.ButtonKind
import com.nebula.app.design.components.ChipTone
import com.nebula.app.design.components.ConfirmDialog
import com.nebula.app.design.components.EmptyState
import com.nebula.app.design.components.ErrorState
import com.nebula.app.design.components.GameTile
import com.nebula.app.design.components.InfoBanner
import com.nebula.app.design.components.LoadingState
import com.nebula.app.design.components.NebulaButton
import com.nebula.app.design.components.NebulaCard
import com.nebula.app.design.components.NebulaTextField
import com.nebula.app.design.components.NebulaTopBar
import com.nebula.app.design.components.RowDivider
import com.nebula.app.design.components.SettingsRow
import com.nebula.app.design.components.StatusChip
import com.nebula.app.design.theme.NebulaColors
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    onEdit: () -> Unit,
    onFavourites: () -> Unit,
    onHistory: () -> Unit,
    onNotifications: () -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
    onVerifyEmail: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val viewModel = nebulaViewModel { ProfileViewModel(it.authRepository, it.gameRepository) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    val favourites by viewModel.favouriteCount.collectAsStateWithLifecycle()
    val sessions by viewModel.sessionCount.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(NebulaColors.Ink),
                contentAlignment = Alignment.Center
            ) {
                Text(user?.initials ?: "N", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user?.fullName ?: "Guest", style = MaterialTheme.typography.titleLarge)
                Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
                if (user != null) {
                    if (user?.emailVerified == true) StatusChip("Email verified", ChipTone.Success)
                    else StatusChip("Email not verified", ChipTone.Warning)
                }
            }
        }

        if (user != null && user?.emailVerified == false) {
            InfoBanner(
                text = "Your demo email is not verified yet. Real verification needs the backend.",
                icon = Icons.Filled.Email,
                tone = ChipTone.Warning
            )
            NebulaButton("Open verification", onVerifyEmail, kind = ButtonKind.Secondary)
        }

        NebulaCard(modifier = Modifier.fillMaxWidth()) {
            SettingsRow("Edit profile", Icons.Filled.Edit, subtitle = "Name and account details", onClick = onEdit)
            RowDivider()
            SettingsRow("Favourite games", Icons.Filled.Favorite, subtitle = "$favourites saved", onClick = onFavourites)
            RowDivider()
            SettingsRow("Gaming history", Icons.Filled.History, subtitle = "$sessions demo sessions", onClick = onHistory)
            RowDivider()
            SettingsRow("Notifications", Icons.Filled.Notifications, subtitle = "Inbox and preferences", onClick = onNotifications)
        }

        NebulaCard(modifier = Modifier.fillMaxWidth()) {
            SettingsRow("App information", Icons.Filled.Info, subtitle = "Version and integration status", onClick = onAbout)
            RowDivider()
            SettingsRow("Privacy policy", Icons.Filled.PrivacyTip, subtitle = "Placeholder", onClick = onPrivacy)
            RowDivider()
            SettingsRow("Terms and conditions", Icons.Filled.Description, subtitle = "Placeholder", onClick = onTerms)
        }

        NebulaButton(
            text = "Log out",
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth(),
            kind = ButtonKind.Danger,
            icon = Icons.AutoMirrored.Filled.ExitToApp
        )
        Spacer(Modifier.height(8.dp))
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Log out of Nebula?",
            message = "You will need to log in again to see your bookings and tickets.",
            confirmText = "Log out",
            destructive = true,
            onConfirm = {
                confirmLogout = false
                viewModel.logout(onLoggedOut)
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val viewModel = nebulaViewModel { EditProfileViewModel(it.authRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        NebulaTopBar("Edit profile", onBack = onBack)
        if (state.loading) {
            LoadingState()
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NebulaTextField(
                    value = state.fullName,
                    onValueChange = viewModel::onNameChange,
                    label = "Full name",
                    leadingIcon = Icons.Filled.Person,
                    error = state.nameError,
                    imeAction = ImeAction.Done,
                    enabled = !state.saving,
                    onImeAction = {
                        viewModel.save {
                            scope.launch { snackbar.showSnackbar("Profile updated.") }
                            onBack()
                        }
                    }
                )
                NebulaTextField(
                    value = state.email,
                    onValueChange = {},
                    label = "Email",
                    leadingIcon = Icons.Filled.Email,
                    readOnly = true,
                    enabled = false,
                    supportingText = "Changing your email needs the backend and is not available yet."
                )
                state.formError?.let { InfoBanner(it, Icons.Filled.Info, tone = ChipTone.Danger) }
                NebulaButton(
                    text = "Save changes",
                    onClick = {
                        viewModel.save {
                            scope.launch { snackbar.showSnackbar("Profile updated.") }
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    kind = ButtonKind.Accent,
                    loading = state.saving
                )
            }
        }
    }
}

@Composable
fun FavouritesScreen(onBack: () -> Unit, onOpenGame: (String) -> Unit) {
    val viewModel = nebulaViewModel { FavouritesViewModel(it.gameRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("Favourite games", onBack = onBack)
        when (val current = state) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Failed -> ErrorState(current.message, onRetry = null)
            is Loadable.Ready -> {
                if (current.value.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.Favorite,
                        title = "No favourites yet",
                        message = "Tap the heart on any game to save it here."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(current.value, key = { it.id }) { game ->
                            GameTile(
                                game = game,
                                isFavourite = true,
                                onClick = { onOpenGame(game.id) },
                                onToggleFavourite = { viewModel.toggle(game.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val viewModel = nebulaViewModel { HistoryViewModel(it.gameRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val demo = LocalDemoMode.current

    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("Gaming history", onBack = onBack)
        when (val current = state) {
            is Loadable.Loading -> LoadingState()
            is Loadable.Failed -> ErrorState(current.message, onRetry = null)
            is Loadable.Ready -> {
                if (current.value.isEmpty()) {
                    EmptyState(Icons.Filled.History, "No history yet", "Sessions you play will be listed here.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            InfoBanner(
                                if (demo) "This history is sample data. Real play time will appear once streaming is connected."
                                else "Shows your completed bookings. Real play time will appear once streaming is connected.",
                                Icons.Filled.Info
                            )
                        }
                        items(current.value, key = { it.id }) { session ->
                            NebulaCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = NebulaColors.Accent)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.gameTitle, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            session.playedOn.pretty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NebulaColors.Secondary
                                        )
                                    }
                                    StatusChip(formatDuration(session.durationMinutes), ChipTone.Neutral)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NebulaColors.Secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NebulaColors.Accent,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = NebulaColors.Subtle,
                uncheckedBorderColor = NebulaColors.Border
            )
        )
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val viewModel = nebulaViewModel { NotificationsViewModel(it.profileRepository) }
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("Notifications", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                InfoBanner(
                    "Preferences are saved on this device. Push notifications need the backend and are not sent yet.",
                    Icons.Filled.Info
                )
            }
            item {
                NebulaCard(modifier = Modifier.fillMaxWidth()) {
                    PreferenceRow("Booking reminders", "Before your sessions start", prefs.bookingReminders, viewModel::setBookingReminders)
                    RowDivider()
                    PreferenceRow("Support updates", "Replies to your tickets", prefs.supportUpdates, viewModel::setSupportUpdates)
                    RowDivider()
                    PreferenceRow("Announcements", "News and new games", prefs.announcements, viewModel::setAnnouncements)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Inbox", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    NebulaButton(
                        text = "Mark all read",
                        onClick = viewModel::markAllRead,
                        kind = ButtonKind.Secondary,
                        enabled = notifications.any { !it.read }
                    )
                }
            }
            if (notifications.isEmpty()) {
                item { EmptyState(Icons.Filled.Notifications, "Nothing here", "You are all caught up.") }
            } else {
                items(notifications, key = { it.id }) { item ->
                    NebulaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (item.read) Color.Transparent else NebulaColors.Accent)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall)
                                Text(item.body, style = MaterialTheme.typography.bodyMedium, color = NebulaColors.Secondary)
                                Text(item.timeLabel, style = MaterialTheme.typography.labelSmall, color = NebulaColors.Secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val demo = LocalDemoMode.current
    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.0"
        } catch (e: Exception) {
            "0.1.0"
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar("App information", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NebulaCard(modifier = Modifier.fillMaxWidth()) {
                InfoLine("App", "Nebula")
                RowDivider()
                InfoLine("Version", version)
                RowDivider()
                InfoLine("Build", "Frontend preview (phase 1)")
            }
            Text("Integration status", style = MaterialTheme.typography.titleLarge)
            NebulaCard(modifier = Modifier.fillMaxWidth()) {
                InfoLine("Accounts", if (demo) "Local demo only" else "Supabase Auth")
                RowDivider()
                InfoLine("Games and favourites", if (demo) "Built-in demo data" else "Supabase database")
                RowDivider()
                InfoLine("Bookings", if (demo) "Simulated, on device" else "Supabase, checked on the server")
                RowDivider()
                InfoLine("Support tickets", if (demo) "Simulated, on device" else "Supabase database")
                RowDivider()
                InfoLine("Notifications", "Preferences on device, sample inbox")
                RowDivider()
                InfoLine("Google sign-in", "Not set up yet")
                RowDivider()
                InfoLine("Cloud streaming", "Not connected")
                RowDivider()
                InfoLine("Moonlight client", "Not integrated")
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(label, color = NebulaColors.Secondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NebulaTopBar(title, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusChip("Placeholder", ChipTone.Warning)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
