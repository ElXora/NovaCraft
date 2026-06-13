package com.novacraft.launcher.ui.screens.accounts

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.theme.*
import com.novacraft.launcher.viewmodel.AccountsViewModel

@Composable
fun AccountsScreen(
    navController: NavController,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts      by viewModel.accounts.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val isLoggingIn   by viewModel.isLoggingIn.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val error         by viewModel.error.collectAsState()

    var showMsLogin    by remember { mutableStateOf(false) }
    var deleteTarget   by remember { mutableStateOf<String?>(null) }

    Scaffold(
        bottomBar      = { NovaCraftBottomBar(navController) },
        topBar         = {
            NovaCraftTopBar(
                title    = "Accounts",
                subtitle = "${accounts.size} accounts",
                actions  = {
                    IconButton(onClick = viewModel::showAddDialog) {
                        Icon(Icons.Filled.PersonAdd, "Add Account", tint = NovaCyan)
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1A2E))))
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Error Banner ─────────────────────────────────────────────
                AnimatedVisibility(error != null) {
                    error?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorDanger.copy(0.1f))
                                .border(1.dp, ColorDanger.copy(0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, null, tint = ColorDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall, color = ColorDanger, modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp), tint = ColorDanger)
                            }
                        }
                    }
                }

                if (accounts.isEmpty()) {
                    EmptyState(
                        icon       = Icons.Outlined.Person,
                        title      = "No accounts",
                        subtitle   = "Add a Microsoft or offline account to start playing",
                        actionLabel = "Add Account",
                        onAction   = viewModel::showAddDialog,
                        modifier   = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Active account header
                        activeAccount?.let { active ->
                            item {
                                ActiveAccountBanner(account = active)
                                Spacer(Modifier.height(8.dp))
                                SectionHeader(title = "All Accounts")
                            }
                        }

                        items(accounts, key = { it.id }) { account ->
                            AccountCard(
                                account   = account,
                                isActive  = account.id == activeAccount?.id,
                                onSetActive = { viewModel.setActive(account.id) },
                                onDelete  = { deleteTarget = account.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Add Account Dialog ─────────────────────────────────────────────────────
    if (showAddDialog) {
        AddAccountDialog(
            isLoggingIn  = isLoggingIn,
            onAddOffline = { username ->
                viewModel.addOfflineAccount(username)
                viewModel.hideAddDialog()
            },
            onLoginMs    = {
                viewModel.hideAddDialog()
                showMsLogin = true
            },
            onDismiss    = viewModel::hideAddDialog
        )
    }

    // ── MS Login Webview ─────────────────────────────────────────────────────
    if (showMsLogin) {
        MicrosoftLoginSheet(
            onCode    = { code ->
                viewModel.loginMicrosoft(code)
                showMsLogin = false
            },
            onDismiss = { showMsLogin = false }
        )
    }

    // ── Delete Confirm ───────────────────────────────────────────────────────
    deleteTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title   = { Text("Remove Account", fontWeight = FontWeight.Bold) },
            text    = { Text("This will remove the account from NovaCraft. Your Minecraft profile will not be deleted.") },
            confirmButton = {
                NovaButton(text = "Remove", onClick = { viewModel.removeAccount(id); deleteTarget = null }, variant = ButtonVariant.DANGER)
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ─── Active Account Banner ────────────────────────────────────────────────────

@Composable
fun ActiveAccountBanner(account: Account) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(NovaPurple.copy(0.2f), NovaBlue.copy(0.15f))))
            .border(1.dp, NovaCyan.copy(0.3f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountAvatar(account = account, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (account.type == AccountType.MICROSOFT) Icons.Filled.VerifiedUser else Icons.Outlined.Person,
                        contentDescription = null,
                        tint = NovaCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = if (account.type == AccountType.MICROSOFT) "Microsoft Account" else "Offline Account",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaCyan
                    )
                }
                Text(
                    text  = account.uuid.take(8) + "...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.35f)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NovaCyan.copy(0.15f),
                border = BorderStroke(1.dp, NovaCyan.copy(0.3f))
            ) {
                Text(
                    "Active",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Account Card ─────────────────────────────────────────────────────────────

@Composable
fun AccountCard(
    account: Account,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick  = { if (!isActive) onSetActive() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountAvatar(account = account, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    account.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (account.type == AccountType.MICROSOFT) Icons.Filled.Microsoft else Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.45f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.45f)
                    )
                }
            }

            if (isActive) {
                Icon(Icons.Filled.CheckCircle, "Active", tint = NovaCyan, modifier = Modifier.size(20.dp))
            } else {
                TextButton(onClick = onSetActive) {
                    Text("Set Active", style = MaterialTheme.typography.labelSmall, color = NovaCyan)
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.DeleteOutline, "Remove", tint = ColorDanger.copy(0.55f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Add Account Dialog ───────────────────────────────────────────────────────

@Composable
fun AddAccountDialog(
    isLoggingIn: Boolean,
    onAddOffline: (String) -> Unit,
    onLoginMs: () -> Unit,
    onDismiss: () -> Unit
) {
    var mode     by remember { mutableIntStateOf(0) } // 0=choose, 1=offline
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = when (mode) { 1 -> "Offline Account"; else -> "Add Account" },
                fontWeight = FontWeight.Bold, color = Color.White
            )
        },
        text = {
            when (mode) {
                0 -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Microsoft option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(NovaBlue.copy(0.15f), NovaPurple.copy(0.15f))))
                            .border(1.dp, NovaCyan.copy(0.3f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onLoginMs)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Microsoft, null, tint = Color(0xFF00A4EF), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Microsoft Account", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text("Login with Xbox & full Minecraft auth", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = NovaCyan, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Offline option
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, NovaGlassBorder, RoundedCornerShape(14.dp))
                            .clickable { mode = 1 }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PersonOutline, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Offline Account", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text("Play without authentication (offline only)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
                1 -> Column {
                    OutlinedTextField(
                        value         = username,
                        onValueChange = { username = it.take(16).replace(" ", "_") },
                        label         = { Text("Username") },
                        placeholder   = { Text("Steve") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        supportingText = { Text("3–16 characters, no spaces", style = MaterialTheme.typography.labelSmall) },
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaCyan,
                            focusedLabelColor  = NovaCyan
                        )
                    )
                }
                else -> {}
            }
        },
        confirmButton = {
            when (mode) {
                1 -> NovaButton(
                    text    = "Add",
                    onClick = { if (username.length >= 3) onAddOffline(username) },
                    enabled = username.length >= 3,
                    isLoading = isLoggingIn
                )
                else -> {}
            }
        },
        dismissButton = {
            TextButton(onClick = { if (mode == 0) onDismiss() else mode = 0 }) {
                Text(if (mode == 0) "Cancel" else "Back")
            }
        }
    )
}

// ─── MS Login Sheet ──────────────────────────────────────────────────────────

@Composable
fun MicrosoftLoginSheet(
    onCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // In production: embed AndroidView with WebView loading MicrosoftAuthService.buildAuthUrl()
    // The WebView intercepts the redirect URI containing the auth code.
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = { Text("Microsoft Login", fontWeight = FontWeight.Bold, color = Color.White) },
        text  = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Microsoft, null, tint = Color(0xFF00A4EF), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "In production, this opens a WebView to sign in with your Microsoft account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
            }
        },
        confirmButton = {
            NovaButton(text = "Simulate Login", onClick = { onCode("dummy_auth_code") })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
