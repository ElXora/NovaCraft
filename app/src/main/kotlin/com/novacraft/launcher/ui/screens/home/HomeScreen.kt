package com.novacraft.launcher.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.ui.components.*
import com.novacraft.launcher.ui.navigation.Routes
import com.novacraft.launcher.ui.theme.*
import com.novacraft.launcher.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val profiles       by viewModel.profiles.collectAsState()
    val activeAccount  by viewModel.activeAccount.collectAsState()
    val news           by viewModel.news.collectAsState()
    val isRefreshing   by viewModel.isRefreshingNews.collectAsState()
    val context        = LocalContext.current

    Scaffold(
        bottomBar = { NovaCraftBottomBar(navController) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0A0E1A),
                            Color(0xFF0D1A2E),
                            Color(0xFF0A0E1A)
                        )
                    )
                )
        ) {
            // Ambient glow blobs
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-60).dp, y = 100.dp)
                    .background(
                        Brush.radialGradient(listOf(NovaPurple.copy(alpha = 0.12f), Color.Transparent)),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = 80.dp)
                    .background(
                        Brush.radialGradient(listOf(NovaCyan.copy(alpha = 0.08f), Color.Transparent)),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )

            LazyColumn(
                modifier           = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding     = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // ── Hero header ──────────────────────────────────────────────
                item {
                    HomeHeroSection(
                        account  = activeAccount,
                        onAddAccount = { navController.navigate(Routes.ACCOUNTS) }
                    )
                }

                // ── Quick Play ────────────────────────────────────────────────
                item {
                    QuickPlaySection(
                        profiles  = profiles.take(3),
                        onPlay    = { viewModel.launchProfile(it, context) },
                        onSeeAll  = { navController.navigate(Routes.PLAY) }
                    )
                }

                // ── Stats row ────────────────────────────────────────────────
                item {
                    StatsRow(profiles = profiles)
                }

                // ── News feed ────────────────────────────────────────────────
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader(
                        title    = "Latest News",
                        action   = "Refresh",
                        onAction = { viewModel.refreshNews() },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (isRefreshing) {
                    item {
                        Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                            CircularProgressIndicator(color = NovaCyan, modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (news.isEmpty()) {
                    item {
                        EmptyState(
                            icon      = Icons.Outlined.Newspaper,
                            title     = "No news yet",
                            subtitle  = "Pull to refresh for the latest updates",
                            modifier  = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(news, key = { it.id }) { item ->
                        NewsCard(item = item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                }
            }
        }
    }
}

// ─── Hero Section ────────────────────────────────────────────────────────────

@Composable
fun HomeHeroSection(
    account: Account?,
    onAddAccount: () -> Unit
) {
    val timeOfDay = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else      -> "Good night"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 8.dp)
    ) {
        // Branding line
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "⛏️",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text  = "NovaCraft",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text  = "Java Minecraft. Anywhere.",
                    style = MaterialTheme.typography.labelSmall,
                    color = NovaCyan.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.weight(1f))
            if (account != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .border(1.dp, NovaGlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    AccountAvatar(account = account, size = 28.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = account.username,
                        style    = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color    = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp)
                    )
                }
            } else {
                TextButton(
                    onClick = onAddAccount,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, NovaGlassBorder, RoundedCornerShape(14.dp))
                ) {
                    Icon(Icons.Outlined.PersonAdd, null, modifier = Modifier.size(16.dp), tint = NovaCyan)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Account", color = NovaCyan, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Greeting
        Text(
            text  = if (account != null) "$timeOfDay, ${account.username}!" else "Welcome to NovaCraft!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text  = "Ready to build something amazing?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

// ─── Quick Play Section ──────────────────────────────────────────────────────

@Composable
fun QuickPlaySection(
    profiles: List<GameProfile>,
    onPlay: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(20.dp))
        SectionHeader(title = "Quick Play", action = "All Profiles", onAction = onSeeAll)
        Spacer(Modifier.height(12.dp))

        if (profiles.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, null, tint = NovaCyan, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("No profiles yet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Create one in the Play tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }
        } else {
            profiles.forEach { profile ->
                QuickPlayCard(profile = profile, onPlay = { onPlay(profile.id) })
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun QuickPlayCard(profile: GameProfile, onPlay: () -> Unit) {
    GlassCard(
        modifier  = Modifier.fillMaxWidth(),
        showGlow  = false
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(NovaPurple.copy(0.3f), NovaBlue.copy(0.3f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = profile.iconEmoji, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile.versionId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                profile.lastPlayed?.let { ts ->
                    Text("Last played: ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(NovaBlue, NovaPurple)))
            ) {
                Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ─── Stats Row ────────────────────────────────────────────────────────────────

@Composable
fun StatsRow(profiles: List<GameProfile>) {
    val totalPlaytime = profiles.sumOf { 0L } // would sum from version entities
    val installedCount = profiles.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "Profiles",
            value = installedCount.toString(),
            icon  = Icons.Filled.VideoGame,
            color = NovaPurple,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Playtime",
            value = formatPlaytime(totalPlaytime),
            icon  = Icons.Filled.Timer,
            color = NovaCyan,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Storage",
            value = "—",
            icon  = Icons.Filled.Storage,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── News Card ────────────────────────────────────────────────────────────────

@Composable
fun NewsCard(item: NewsItem, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            // Category badge
            Surface(
                shape  = RoundedCornerShape(6.dp),
                color  = NovaCyan.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, NovaCyan.copy(alpha = 0.3f))
            ) {
                Text(
                    text     = item.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = NovaCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.publishedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}
