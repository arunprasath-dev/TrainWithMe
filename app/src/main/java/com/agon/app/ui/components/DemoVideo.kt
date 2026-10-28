package com.agon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Muted, looping demo clip for a workout exercise.
 *
 * Contract: no audio, always looping; released when it leaves composition and paused when the
 * host lifecycle stops; if the asset is missing the player renders a designed placeholder with
 * an icon instead of crashing — the technique cues remain the source of truth.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun LoopingAssetVideo(
    assetPath: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    var failed by remember(assetPath) { mutableStateOf(false) }
    var isBuffering by remember(assetPath) { mutableStateOf(false) }

    val player = remember(assetPath) {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri("asset:///$assetPath"))
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                playWhenReady = playing
                prepare()
            }
        }.getOrElse {
            failed = true
            null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player?.pause()
                Lifecycle.Event.ON_START -> if (playing) player?.play()
                else -> Unit
            }
        }
        val playerListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                failed = true
                isBuffering = false
                player?.pause()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player?.addListener(playerListener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.removeListener(playerListener)
            player?.release()
        }
    }

    LaunchedEffect(playing, player) {
        if (playing) player?.play() else player?.pause()
    }

    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ),
            )
            .semantics {
                this.contentDescription = contentDescription ?: "Exercise demonstration video"
            },
        contentAlignment = Alignment.Center,
    ) {
        if (player == null || failed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    Icon(
                        Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                    )
                }
                Text(
                    "Demo clip unavailable — follow the technique steps below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                },
                update = { view -> view.player = player },
            )
            // cinematic vignette + loop badge
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.28f),
                            ),
                        ),
                    ),
            )
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            ) {
                Text(
                    "DEMO · LOOP",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            if (isBuffering && playing) {
                BufferingRing()
            }
            if (!playing) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play demo",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp),
                    )
                }
            }
        }
    }
}
