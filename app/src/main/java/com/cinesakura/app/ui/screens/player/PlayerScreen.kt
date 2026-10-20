package com.cinesakura.app.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.domain.model.StreamServer
import com.cinesakura.app.domain.model.WatchHistoryItem
import com.cinesakura.app.ui.theme.*
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    imdbId: String,
    title: String,
    isTv: Boolean,
    season: Int = 1,
    episode: Int = 1,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val movieRepo = remember { CineSakuraApp.instance.movieRepository }
    val watchlistRepo = remember { CineSakuraApp.instance.watchlistRepository }

    val servers = remember(imdbId, isTv, season, episode) {
        movieRepo.getStreamServers(imdbId, isTv, season, episode)
    }

    var selectedServer by remember { mutableStateOf(servers.firstOrNull()) }
    var isLandscape by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Record to watch history
    LaunchedEffect(imdbId) {
        watchlistRepo.recordHistory(
            WatchHistoryItem(
                mediaId = imdbId,
                imdbId = imdbId,
                title = title,
                poster = "",
                type = if (isTv) "series" else "movie",
                season = season,
                episode = episode
            )
        )
    }

    // Handle back button for fullscreen video view
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else {
            // Restore portrait on exit
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onNavigateBack()
        }
    }

    val toggleOrientation = {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            isLandscape = false
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            isLandscape = true
        }
    }

    Scaffold(
        topBar = {
            if (customView == null && !isLandscape) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            if (isTv) {
                                Text(
                                    text = "Season $season • Episode $episode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SakuraLight
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = toggleOrientation) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
                )
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Video Player Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isLandscape) 1f else 0.55f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (selectedServer != null) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mediaPlaybackRequiresUserGesture = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowFileAccess = true
                                    databaseEnabled = true
                                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                        customView = view
                                        customViewCallback = callback
                                    }

                                    override fun onHideCustomView() {
                                        customView = null
                                        customViewCallback = null
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        // Block popup ads or suspicious redirects away from embed players
                                        val allowedHosts = listOf(
                                            "vidlink.pro", "vidsrc.me", "vidsrc.pm", "vidsrc.xyz",
                                            "2embed.cc", "111movies.net", "embed.su", "google.com", "cloudflare.com"
                                        )
                                        val isAllowed = allowedHosts.any { url.contains(it) }
                                        return if (isAllowed) false else true // block unauthorized popups!
                                    }
                                }

                                loadUrl(selectedServer!!.embedUrl)
                            }
                        },
                        update = { webView ->
                            selectedServer?.let { server ->
                                if (webView.url != server.embedUrl) {
                                    webView.loadUrl(server.embedUrl)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // If fullscreen custom view is active
                if (customView != null) {
                    AndroidView(
                        factory = {
                            FrameLayout(context).apply {
                                addView(customView)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Controls & Server Switcher (Visible in Portrait mode)
            if (!isLandscape && customView == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .background(DarkBg)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Streaming Signals & Servers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        IconButton(onClick = toggleOrientation) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = SakuraPink)
                        }
                    }

                    Text(
                        text = "If playback buffers or doesn't start, switch to another signal below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // Server selection pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(servers) { server ->
                            FilterChip(
                                selected = selectedServer?.serverKey == server.serverKey,
                                onClick = { selectedServer = server },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(server.name)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (server.isHindiDubAvailable) NeonCyan.copy(alpha = 0.2f) else SakuraPink.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = server.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (server.isHindiDubAvailable) NeonCyan else SakuraPink,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DarkSurfaceVariant,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = DarkCard,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                            Column {
                                Text(
                                    text = "Active: ${selectedServer?.name ?: "Signal 1"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Built-in Ad-Shield & Popup Protector active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
