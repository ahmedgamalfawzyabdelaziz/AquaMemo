// SplashScreen
package com.ahmedgamal.aquamemo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ahmedgamal.aquamemo.R
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun SplashScreen(
    isVisible: Boolean,
    onSkipClicked: () -> Unit,
    onVideoEnd: () -> Unit,
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
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // ⬅️ عند انتهاء الفيديو، نشغل دالة الإخفاء فوراً
                    onVideoEnd()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
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
            // ✅ 5. زر التخطي (Skip Button)
            TextButton(
                onClick = onSkipClicked, // ⬅️ عند الضغط، يتم استدعاء دالة التخطي
                modifier = Modifier
                    .align(Alignment.TopEnd) // وضعه في الزاوية العلوية اليسرى (لأنه RTL)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.skip),
                    color = androidx.compose.ui.graphics.Color.White, // اختر لوناً واضحاً فوق الفيديو
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}