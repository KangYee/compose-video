/*
 * Copyright 2023 Dora Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sanghun.compose.video

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import io.sanghun.compose.video.controller.VideoPlayerControllerConfig
import io.sanghun.compose.video.uri.VideoPlayerMediaItem
import io.sanghun.compose.video.uri.toUri

/**
 * [VideoPlayer] is UI component that can play video in Jetpack Compose. It works based on ExoPlayer.
 * You can play local (e.g. asset files, resource files) files and all video files in the network environment.
 * For all video formats supported by the [VideoPlayer] component, see the ExoPlayer link below.
 *
 * If you rotate the screen, the default action is to reset the player state.
 * To prevent this happening, put the following options in the `android:configChanges` option of your app's AndroidManifest.xml to keep the settings.
 * ```
 * keyboard|keyboardHidden|orientation|screenSize|screenLayout|smallestScreenSize|uiMode
 * ```
 *
 * This component is linked with Compose [androidx.compose.runtime.DisposableEffect].
 * This means that it move out of the Composable Scope, the ExoPlayer instance will automatically be destroyed as well.
 *
 * @see <a href="https://exoplayer.dev/supported-formats.html">Exoplayer Support Formats</a>
 *
 * @param modifier Modifier to apply to this layout node.
 * @param mediaItem [VideoPlayerMediaItem] to be played by the video player
 * @param handleLifecycle Sets whether to automatically play/stop the player according to the activity lifecycle. Default is true.
 * @param autoPlay Autoplay when media item prepared. Default is true.
 * @param usePlayerController Using player controller. Default is true.
 * @param controllerConfig Player controller config. You can customize the Video Player Controller UI.
 */
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    mediaItem: VideoPlayerMediaItem,
    handleLifecycle: Boolean = true,
    autoPlay: Boolean = true,
    usePlayerController: Boolean = true,
    controllerConfig: VideoPlayerControllerConfig = VideoPlayerControllerConfig.Default,
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context)
            .build()
    }

    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    val defaultPlayerView = remember {
        StyledPlayerView(context)
    }

    LaunchedEffect(usePlayerController) {
        defaultPlayerView.useController = usePlayerController
    }

    LaunchedEffect(player) {
        defaultPlayerView.player = player
    }

    LaunchedEffect(mediaItem) {
        val exoPlayerMediaItem = MediaItem.Builder()
            .apply {
                val uri = mediaItem.toUri(context)
                setUri(uri)
            }.build()

        player.setMediaItem(exoPlayerMediaItem)
        player.prepare()

        if (autoPlay) {
            player.play()
        }
    }

    LaunchedEffect(controllerConfig) {
        defaultPlayerView.rootView.findViewById<View>(com.google.android.exoplayer2.R.id.exo_settings).isVisible =
            controllerConfig.showSpeedAndPitchOverlay
    }

    DisposableEffect(
        AndroidView(
            modifier = modifier,
            factory = {
                defaultPlayerView.apply {
                    useController = usePlayerController
                }
            },
        ),
    ) {
        val observer = LifecycleEventObserver { _, event ->
            if (handleLifecycle) {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        player.pause()
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        player.play()
                    }

                    else -> {}
                }
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            player.release()
            lifecycle.removeObserver(observer)
        }
    }
}
