package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.CameraMode
import com.example.model.CapturedItem
import com.example.ui.theme.CameraRed
import com.example.ui.theme.CameraTextWhite

@Composable
fun BottomControls(
  latestMedia: CapturedItem?,
  currentMode: CameraMode,
  isRecordingVideo: Boolean,
  onShutterClick: () -> Unit,
  onSwitchCamera: () -> Unit,
  onGalleryClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var switchRotationAngle by remember { mutableFloatStateOf(0f) }
  val animatedSwitchRotation by animateFloatAsState(
    targetValue = switchRotationAngle,
    animationSpec = tween(durationMillis = 350),
    label = "switch_rotation"
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(100.dp)
      .padding(horizontal = 36.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Gallery Thumbnail (Left)
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(Color(0x44303032))
        .border(1.5.dp, Color(0x33FFFFFF), CircleShape)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          onGalleryClick()
        }
        .testTag("gallery_thumbnail_button"),
      contentAlignment = Alignment.Center
    ) {
      if (latestMedia != null) {
        AsyncImage(
          model = latestMedia.uri,
          contentDescription = "Latest captured media",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
        )
      } else {
        Icon(
          imageVector = Icons.Default.PhotoLibrary,
          contentDescription = "Open Gallery",
          tint = Color(0x99FFFFFF),
          modifier = Modifier.size(24.dp)
        )
      }
    }

    // 2. Large Shutter Button (Center)
    ShutterButton(
      isRecordingVideo = isRecordingVideo,
      isVideoMode = currentMode == CameraMode.VIDEO,
      onClick = onShutterClick,
      modifier = Modifier.testTag("camera_shutter_button")
    )

    // 3. Camera Switch Button (Right)
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(Color(0x55323236))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) {
          switchRotationAngle += 180f
          onSwitchCamera()
        }
        .testTag("camera_switch_button"),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Cached,
        contentDescription = "Switch Camera",
        tint = CameraTextWhite,
        modifier = Modifier
          .size(28.dp)
          .rotate(animatedSwitchRotation)
      )
    }
  }
}

@Composable
fun ShutterButton(
  isRecordingVideo: Boolean,
  isVideoMode: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1.0f,
    animationSpec = tween(120),
    label = "shutter_scale"
  )

  Box(
    modifier = modifier
      .size(76.dp)
      .scale(scale)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    if (isVideoMode) {
      // Outer white ring
      Box(
        modifier = Modifier
          .size(76.dp)
          .border(4.dp, Color.White, CircleShape)
      )

      if (isRecordingVideo) {
        // Red rounded square (Stop recording)
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CameraRed)
        )
      } else {
        // Red solid circle (Start recording)
        Box(
          modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(CameraRed)
        )
      }
    } else {
      // Photo Shutter: Large solid white circle with subtle dark ring border
      Box(
        modifier = Modifier
          .size(76.dp)
          .clip(CircleShape)
          .background(Color(0x33FFFFFF))
          .padding(4.dp)
      ) {
        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color.White)
        )
      }
    }
  }
}
