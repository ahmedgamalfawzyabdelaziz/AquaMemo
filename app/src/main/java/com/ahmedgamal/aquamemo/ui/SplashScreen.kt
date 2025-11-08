package com.ahmedgamal.aquamemo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi // ✅ 1. Import the annotation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ahmedgamal.aquamemo.R

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    isVisible: Boolean,
    fontSize: String = "medium" // We keep this for compatibility with MainActivity
) {
    val context = LocalContext.current

    // 1. Initialize the ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Build the MediaItem from the raw resource file
            // **Remember to add your video as 'splash_video.mp4' in 'res/raw/'**
            val mediaItem = MediaItem.fromUri(
                "android.resource://${context.packageName}/${R.raw.splash_video}"
            )
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // 2. Clean up the player when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 3. Use AnimatedVisibility just like the old screen
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500)), // Shorter fade for video
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 4. Use AndroidView to display the PlayerView
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false // Hide video controls
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}