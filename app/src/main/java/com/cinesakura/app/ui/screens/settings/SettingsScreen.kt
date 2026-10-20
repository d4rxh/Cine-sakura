package com.cinesakura.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.R
import com.cinesakura.app.ui.theme.DarkCard
import com.cinesakura.app.ui.theme.DarkSurface
import com.cinesakura.app.ui.theme.SakuraPink
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Watch History?") },
            text = { Text("This will permanently remove all your recorded watch history from local storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            CineSakuraApp.instance.watchlistRepository.clearHistory()
                            Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                            showClearHistoryDialog = false
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & About",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SakuraPink.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieFilter,
                            contentDescription = "Cine-Sakura Logo",
                            tint = SakuraPink,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Cine-Sakura",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version 1.0.0 • Native Android",
                        fontSize = 12.sp,
                        color = SakuraPink
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "High-definition streaming & catalog for movies, series, and anime. Built with Kotlin & Jetpack Compose.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Stream Servers Info
            Text(
                text = "STREAMING SERVERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SakuraPink,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "Multi-Server Playback Engine",
                    subtitle = "VidLink Pro (Primary), VidSrc Prime, 2Embed, and 111Movies"
                )
                Divider(color = DarkCard)
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Ad & Popup Shield",
                    subtitle = "Automated popup suppression & sandboxed playback engine"
                )
            }

            // Data & Storage
            Text(
                text = "DATA & CACHE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SakuraPink,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.DeleteOutline,
                    title = "Clear Watch History",
                    subtitle = "Delete local playback logs",
                    onClick = { showClearHistoryDialog = true }
                )
                Divider(color = DarkCard)
                SettingsItem(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Image Cache",
                    subtitle = "Free up device storage",
                    onClick = {
                        val imageLoader = ImageLoader(context)
                        imageLoader.diskCache?.clear()
                        imageLoader.memoryCache?.clear()
                        Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // About & Links
            Text(
                text = "COMMUNITY & LEGAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SakuraPink,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsGroup {
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "GitHub Repository",
                    subtitle = "github.com/d4rxh/Cine-sakura",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/d4rxh/Cine-sakura"))
                        context.startActivity(intent)
                    }
                )
                Divider(color = DarkCard)
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy First",
                    subtitle = "No accounts, no user telemetry, direct client-side requests"
                )
                Divider(color = DarkCard)
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Disclaimer",
                    subtitle = "Cine-Sakura does not host any media files. All streams are delivered via third-party web scrapers and embed providers."
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SakuraPink,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
