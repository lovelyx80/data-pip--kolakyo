/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 * Spotify-style Player Design
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.music.ui.theme.SpotifyGreen

/**
 * Spotify-style mini player for the bottom of the screen
 * Shows current song with play/pause controls and progress
 */
@Composable
fun SpotifyMiniPlayer(
    modifier: Modifier = Modifier,
    songTitle: String = "Song Title",
    artistName: String = "Artist Name",
    progress: Float = 0.5f,
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
    albumArtUrl: String? = null,
    onPlayerClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF1E1E1E))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onPlayerClick
            )
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF282828))
            ) {
                if (albumArtUrl != null) {
                    AsyncImage(
                        model = albumArtUrl,
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Song info
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    songTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artistName,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = SpotifyGreen
                    )
                ) {
                    Text("⏮", fontSize = 16.sp)
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = SpotifyGreen
                    )
                ) {
                    Text(
                        if (isPlaying) "⏸" else "▶",
                        fontSize = 16.sp
                    )
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = SpotifyGreen
                    )
                ) {
                    Text("⏭", fontSize = 16.sp)
                }
            }
        }

        // Progress bar at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color(0xFF282828))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(SpotifyGreen)
            )
        }
    }
}

/**
 * Spotify-style full player screen with gradient background and large album art
 */
@Composable
fun SpotifyFullPlayer(
    modifier: Modifier = Modifier,
    songTitle: String = "Song Title",
    artistName: String = "Artist Name",
    albumName: String = "Album Name",
    progress: Float = 0.5f,
    duration: Long = 180000L, // ms
    currentTime: Long = 90000L, // ms
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
    albumArtUrl: String? = null,
    onCloseClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    onRepeatClick: () -> Unit = {},
    isShuffle: Boolean = false,
    repeatMode: Int = 0 // 0: off, 1: all, 2: one
) {
    val formattedCurrentTime = formatTime(currentTime)
    val formattedDuration = formatTime(duration)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1DB954).copy(alpha = 0.1f),
                            Color(0xFF121212)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Now Playing",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("×", fontSize = 28.sp, color = Color.White)
                }
            }

            // Album art with shadow
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF282828))
            ) {
                if (albumArtUrl != null) {
                    AsyncImage(
                        model = albumArtUrl,
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Song info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    songTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artistName,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    albumName,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Progress bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = onProgressChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyGreen,
                        activeTrackColor = SpotifyGreen,
                        inactiveTrackColor = Color(0xFF404040)
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formattedCurrentTime,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        formattedDuration,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onShuffleClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isShuffle) SpotifyGreen else Color.Gray
                    )
                ) {
                    Text("🔀", fontSize = 20.sp)
                }

                // Previous
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("⏮", fontSize = 24.sp)
                }

                // Play/Pause (large button)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen)
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource,
                            onClick = onPlayPauseClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isPlaying) "⏸" else "▶",
                        fontSize = 32.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Next
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("⏭", fontSize = 24.sp)
                }

                // Repeat
                IconButton(
                    onClick = onRepeatClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = when (repeatMode) {
                            2 -> SpotifyGreen // One
                            1 -> SpotifyGreen // All
                            else -> Color.Gray // Off
                        }
                    )
                ) {
                    Text(
                        if (repeatMode == 2) "R¹" else "R",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Additional controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Add to library */ },
                    modifier = Modifier
                        .size(40.dp)
                        .weight(1f),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("♡", fontSize = 24.sp)
                }

                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier
                        .size(40.dp)
                        .weight(1f),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("📤", fontSize = 24.sp)
                }

                IconButton(
                    onClick = { /* More options */ },
                    modifier = Modifier
                        .size(40.dp)
                        .weight(1f),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text("⋮", fontSize = 20.sp)
                }
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
