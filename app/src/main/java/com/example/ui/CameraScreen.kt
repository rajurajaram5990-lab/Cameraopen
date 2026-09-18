package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.CameraMode
import com.example.ui.components.BottomControls
import com.example.ui.components.EffectsFilterSheet
import com.example.ui.components.FocusRingView
import com.example.ui.components.GalleryViewerDialog
import com.example.ui.components.ModeSelector
import com.example.ui.components.MoreModesSheet
import com.example.ui.components.TopBarControls
import com.example.ui.components.ZoomLensBar
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraRed
import com.example.ui.theme.CameraTextMuted
import com.example.ui.theme.CameraTextWhite
import com.example.ui.theme.CameraYellow
import java.util.Locale

@Composable
fun CameraScreen(
  viewModel: CameraViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()

  // Runtime Permissions Check
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
    hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission || !hasAudioPermission) {
      permissionLauncher.launch(
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      )
    }
  }

  if (!hasCameraPermission) {
    // Permission Required Screen
    PermissionRequiredView(
      onRequestPermissions = {
        permissionLauncher.launch(
          arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
      }
    )
    return
  }

  var viewSize by remember { mutableStateOf(IntSize.Zero) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CameraBlack)
      .testTag("camera_screen_root")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Top Bar Controls
      TopBarControls(
        flashMode = uiState.flashMode,
        resolutionText = uiState.resolutionText,
        timerSetting = uiState.timerSetting,
        isFilterMenuOpen = uiState.isFilterMenuOpen,
        onToggleFlash = { viewModel.toggleFlash() },
        onCycleTimer = { viewModel.cycleTimer() },
        onToggleFilters = { viewModel.toggleFilterMenu() },
        modifier = Modifier.fillMaxWidth()
      )

      // 2. Camera Viewport Container (3:4 ratio)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .onSizeChanged { viewSize = it }
          .clip(RoundedCornerShape(0.dp))
          .pointerInput(Unit) {
            detectTransformGestures { _, _, zoom, _ ->
              if (zoom != 1.0f) {
                viewModel.onPinchZoom(zoom)
              }
            }
          }
          .pointerInput(Unit) {
            detectTapGestures { offset ->
              if (viewSize.width > 0 && viewSize.height > 0) {
                viewModel.onTapToFocus(offset, viewSize.width, viewSize.height)
              }
            }
          }
          .testTag("camera_viewfinder_box"),
        contentAlignment = Alignment.Center
      ) {
        // TextureView Camera Preview
        AndroidView(
          factory = { ctx ->
            TextureView(ctx).apply {
              layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              viewModel.attachTextureView(this)
            }
          },
          modifier = Modifier.fillMaxSize()
        )

        // Focus Metering Ring & Exposure Slider
        uiState.focusPoint?.let { point ->
          FocusRingView(
            focusOffset = point,
            exposureValue = uiState.exposureCompensation,
            exposureRange = uiState.exposureRange,
            onExposureAdjusted = { viewModel.onExposureAdjusted(it) }
          )
        }

        // Portrait Mode Blur Level Slider
        if (uiState.currentMode == CameraMode.PORTRAIT) {
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 16.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(Color(0x80000000))
              .padding(horizontal = 16.dp, vertical = 6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Blur Effect",
                color = CameraTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
              Slider(
                value = uiState.portraitBlurLevel,
                onValueChange = { viewModel.setPortraitBlur(it) },
                valueRange = 1f..10f,
                colors = SliderDefaults.colors(
                  thumbColor = CameraYellow,
                  activeTrackColor = CameraYellow
                ),
                modifier = Modifier
                  .size(width = 120.dp, height = 24.dp)
                  .testTag("portrait_blur_slider")
              )
            }
          }
        }

        // Video Recording Timer Overlay
        if (uiState.isRecordingVideo) {
          Box(
            modifier = Modifier
              .align(Alignment.TopCenter)
              .padding(top = 16.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0x99000000))
              .padding(horizontal = 14.dp, vertical = 6.dp)
              .testTag("recording_duration_badge"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = "Recording",
                tint = CameraRed,
                modifier = Modifier.size(12.dp)
              )
              val minutes = uiState.videoRecordingSeconds / 60
              val seconds = uiState.videoRecordingSeconds % 60
              Text(
                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                color = CameraTextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Active Countdown Timer Overlay (3, 2, 1)
        uiState.countdownValue?.let { count ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color(0x40000000)),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "$count",
              color = CameraYellow,
              fontSize = 88.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("countdown_timer_text")
            )
          }
        }

        // Shutter Flash Animation (White flash for 100ms)
        if (uiState.isCapturingFlash) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.White)
          )
        }

        // Floating Zoom / Lens Pill row at bottom of viewfinder
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp)
        ) {
          ZoomLensBar(
            availableLenses = uiState.availableLenses,
            selectedLens = uiState.selectedLens,
            currentZoom = uiState.zoomRatio,
            sceneOptimizerEnabled = uiState.sceneOptimizerEnabled,
            onLensSelected = { viewModel.selectLens(it) },
            onToggleSceneOptimizer = { viewModel.toggleSceneOptimizer() }
          )
        }

        // Filters and Effects Bottom Sheet (if active)
        EffectsFilterSheet(
          isOpen = uiState.isFilterMenuOpen,
          currentEffect = uiState.currentEffect,
          onSelectEffect = { viewModel.selectEffect(it) },
          onClose = { viewModel.closeFilterMenu() },
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 68.dp)
        )
      }

      // 3. Bottom Controls & Mode Selector Area
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(CameraBlack)
          .padding(top = 12.dp, bottom = 8.dp)
      ) {
        // Shutter, Gallery, Switch Camera
        BottomControls(
          latestMedia = uiState.latestCapturedItem,
          currentMode = uiState.currentMode,
          isRecordingVideo = uiState.isRecordingVideo,
          onShutterClick = { viewModel.onShutterPressed() },
          onSwitchCamera = { viewModel.switchCameraFacing() },
          onGalleryClick = { viewModel.openGallery() },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Mode Selector: PORTRAIT | PHOTO | VIDEO | MORE
        ModeSelector(
          currentMode = uiState.currentMode,
          onModeSelected = { viewModel.onModeSelected(it) },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // Fullscreen Gallery Viewer
    if (uiState.isGalleryOpen) {
      GalleryViewerDialog(
        item = uiState.latestCapturedItem,
        onDismiss = { viewModel.closeGallery() }
      )
    }

    // More Camera Modes Grid Sheet
    MoreModesSheet(
      isOpen = uiState.isMoreMenuOpen,
      onSelectMode = { modeTitle ->
        viewModel.closeMoreMenu()
      },
      onClose = { viewModel.closeMoreMenu() }
    )

    // Optional Toast / Notification Overlay
    uiState.toastMessage?.let { msg ->
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xCC202022))
          .padding(horizontal = 20.dp, vertical = 10.dp)
      ) {
        Text(
          text = msg,
          color = CameraTextWhite,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun PermissionRequiredView(
  onRequestPermissions: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CameraBlack)
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(Color(0x33333336)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.CameraAlt,
          contentDescription = null,
          tint = CameraYellow,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Camera Permission Needed",
        color = CameraTextWhite,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "To take high-resolution photos and videos with Camera2, please grant camera and audio access.",
        color = CameraTextMuted,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = onRequestPermissions,
        colors = ButtonDefaults.buttonColors(
          containerColor = CameraYellow,
          contentColor = CameraBlack
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .height(48.dp)
          .testTag("grant_camera_permission_button")
      ) {
        Text(
          text = "Allow Camera Access",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }
    }
  }
}
