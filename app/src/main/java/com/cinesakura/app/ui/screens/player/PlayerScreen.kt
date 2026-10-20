package com.cinesakura.app.ui.screens.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cinesakura.app.CineSakuraApp
import com.cinesakura.app.domain.model.StreamServer
import com.cinesakura.app.domain.model.WatchHistoryItem
import com.cinesakura.app.ui.theme.*
import kotlinx.coroutines.delay

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

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
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val movieRepo = remember { CineSakuraApp.instance.movieRepository }
    val watchlistRepo = remember { CineSakuraApp.instance.watchlistRepository }

    val servers = remember(imdbId, isTv, season, episode) {
        movieRepo.getStreamServers(imdbId, isTv, season, episode)
    }

    var selectedServer by remember { mutableStateOf(servers.firstOrNull()) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var showControlsOverlay by remember { mutableStateOf(true) }

    // Auto-hide controls overlay in landscape mode after 4 seconds
    LaunchedEffect(showControlsOverlay, isLandscape) {
        if (isLandscape && showControlsOverlay) {
            delay(4000)
            showControlsOverlay = false
        }
    }

    // Manage system bars for immersive fullscreen in landscape
    DisposableEffect(isLandscape, customView != null) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isLandscape || customView != null) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val win = activity?.window
            if (win != null) {
                val controller = WindowCompat.getInsetsController(win, win.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Save to Watch History
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

    // Handle back button
    BackHandler {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
        } else if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onNavigateBack()
        }
    }

    val toggleOrientation = {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLandscape) {
            // Fullscreen Landscape Video Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControlsOverlay = !showControlsOverlay
                    }
            ) {
                if (selectedServer != null) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                configureVideoWebView(this, selectedServer!!.embedUrl) { v, cb ->
                                    customView = v
                                    customViewCallback = cb
                                }
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

                // Fullscreen HTML5 Video View if triggered inside WebChromeClient
                customView?.let { cv ->
                    AndroidView(
                        factory = {
                            FrameLayout(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                (cv.parent as? ViewGroup)?.removeView(cv)
                                addView(cv)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Floating Overlay in Landscape
                AnimatedVisibility(
                    visible = showControlsOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(16.dp)
                    ) {
                        // Top Bar: Back, Title, Server Chips & Rotate Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }

                                Column {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1
                                    )
                                    if (isTv) {
                                        Text(
                                            text = "Season $season • Episode $episode",
                                            color = SakuraPink,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Server switcher quick chip
                                servers.take(4).forEach { s ->
                                    FilterChip(
                                        selected = selectedServer?.serverKey == s.serverKey,
                                        onClick = { selectedServer = s },
                                        label = { Text(s.name.substringBefore(" ("), fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SakuraPink,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.Black.copy(alpha = 0.6f),
                                            labelColor = Color.White
                                        )
                                    )
                                }

                                IconButton(
                                    onClick = toggleOrientation,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ScreenRotation, contentDescription = "Exit Landscape", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Portrait Layout: Top Bar + 16:9 Video Player + Controls
            Scaffold(
                topBar = {
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
                                Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = SakuraPink)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
                    )
                },
                containerColor = DarkBg
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Video Player Box (16:9 Aspect Ratio)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        if (selectedServer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        configureVideoWebView(this, selectedServer!!.embedUrl) { v, cb ->
                                            customView = v
                                            customViewCallback = cb
                                        }
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

                        // Fullscreen button overlay on player
                        IconButton(
                            onClick = toggleOrientation,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Content & Server Switcher
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Streaming Server",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Button(
                                onClick = toggleOrientation,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SakuraPink),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Landscape", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "If video buffers, switch to any backup server below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        // Server Chips Carousel
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(servers) { server ->
                                FilterChip(
                                    selected = selectedServer?.serverKey == server.serverKey,
                                    onClick = { selectedServer = server },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
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

                        // Shield Info Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen)
                                Column {
                                    Text(
                                        text = "Active Server: ${selectedServer?.name ?: "Primary Server"}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Ad-Shield active • Rotate phone anytime for immersive fullscreen",
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
}

private fun configureVideoWebView(
    webView: WebView,
    initialUrl: String,
    onCustomViewShow: (View, WebChromeClient.CustomViewCallback) -> Unit
) {
    webView.apply {
        setBackgroundColor(android.graphics.Color.BLACK)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (view != null && callback != null) {
                    onCustomViewShow(view, callback)
                }
            }

            override fun onHideCustomView() {}
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Block external app intent redirects (e.g. market, intent, whatsapp)
                val isExternalAppScheme = url.startsWith("intent://") ||
                        url.startsWith("market://") ||
                        url.startsWith("whatsapp://") ||
                        url.startsWith("tg://") ||
                        url.startsWith("vnd.youtube:")

                if (isExternalAppScheme) {
                    return true // block unwanted app switches
                }

                // Allow all embedded web stream traffic inside player
                return false
            }
        }

        loadUrl(initialUrl)
    }
}
