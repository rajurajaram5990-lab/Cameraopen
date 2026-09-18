package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.CountDownTimer
import android.view.TextureView
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.Camera2Controller
import com.example.model.CameraFacing
import com.example.model.CameraFilterEffect
import com.example.model.CameraMode
import com.example.model.CapturedItem
import com.example.model.FlashMode
import com.example.model.LensOption
import com.example.model.TimerSetting
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraUiState(
  val currentMode: CameraMode = CameraMode.PHOTO,
  val currentFacing: CameraFacing = CameraFacing.BACK,
  val availableLenses: List<LensOption> = listOf(
    LensOption(".6", 0.6f),
    LensOption("1x", 1.0f),
    LensOption("2", 2.0f),
    LensOption("3", 3.0f)
  ),
  val selectedLens: LensOption = LensOption("1x", 1.0f),
  val flashMode: FlashMode = FlashMode.OFF,
  val resolutionText: String = "12M",
  val timerSetting: TimerSetting = TimerSetting.OFF,
  val currentEffect: CameraFilterEffect = CameraFilterEffect.ORIGINAL,
  val isFilterMenuOpen: Boolean = false,
  val isMoreMenuOpen: Boolean = false,
  val isRecordingVideo: Boolean = false,
  val videoRecordingSeconds: Int = 0,
  val zoomRatio: Float = 1.0f,
  val exposureCompensation: Int = 0,
  val exposureRange: IntRange = -4..4,
  val focusPoint: Offset? = null,
  val isCapturingFlash: Boolean = false,
  val countdownValue: Int? = null,
  val latestCapturedItem: CapturedItem? = null,
  val isGalleryOpen: Boolean = false,
  val sceneOptimizerEnabled: Boolean = true,
  val portraitBlurLevel: Float = 5.0f,
  val toastMessage: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

  val controller = Camera2Controller(application.applicationContext)

  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  private var focusFadeJob: Job? = null
  private var videoTimerJob: Job? = null

  init {
    viewModelScope.launch {
      controller.availableLensesFlow.collect { lenses ->
        if (lenses.isNotEmpty()) {
          _uiState.update { state ->
            val defaultLens = lenses.find { it.zoomRatio == 1.0f } ?: lenses.first()
            state.copy(
              availableLenses = lenses,
              selectedLens = defaultLens
            )
          }
        }
      }
    }

    viewModelScope.launch {
      controller.currentResolutionFlow.collect { res ->
        _uiState.update { it.copy(resolutionText = res) }
      }
    }

    viewModelScope.launch {
      controller.exposureCompensationRangeFlow.collect { range ->
        _uiState.update { it.copy(exposureRange = range) }
      }
    }

    viewModelScope.launch {
      controller.currentExposureFlow.collect { exp ->
        _uiState.update { it.copy(exposureCompensation = exp) }
      }
    }

    viewModelScope.launch {
      controller.latestCapturedMediaFlow.collect { item ->
        _uiState.update { it.copy(latestCapturedItem = item) }
      }
    }
  }

  fun attachTextureView(textureView: TextureView) {
    controller.attachTextureView(textureView)
  }

  fun onModeSelected(mode: CameraMode) {
    if (_uiState.value.isRecordingVideo) {
      stopVideoRecording()
    }
    if (mode == CameraMode.MORE) {
      _uiState.update { it.copy(isMoreMenuOpen = true) }
    } else {
      _uiState.update { it.copy(currentMode = mode, isMoreMenuOpen = false) }
    }
  }

  fun closeMoreMenu() {
    _uiState.update { it.copy(isMoreMenuOpen = false) }
  }

  fun toggleFlash() {
    val nextFlash = when (_uiState.value.flashMode) {
      FlashMode.OFF -> FlashMode.ON
      FlashMode.ON -> FlashMode.AUTO
      FlashMode.AUTO -> FlashMode.OFF
    }
    _uiState.update { it.copy(flashMode = nextFlash) }
    controller.setFlashMode(nextFlash)
  }

  fun cycleTimer() {
    val nextTimer = _uiState.value.timerSetting.next()
    _uiState.update { it.copy(timerSetting = nextTimer) }
  }

  fun toggleFilterMenu() {
    _uiState.update { it.copy(isFilterMenuOpen = !it.isFilterMenuOpen) }
  }

  fun closeFilterMenu() {
    _uiState.update { it.copy(isFilterMenuOpen = false) }
  }

  fun selectEffect(effect: CameraFilterEffect) {
    _uiState.update { it.copy(currentEffect = effect) }
    controller.setFilterEffect(effect)
  }

  fun selectLens(lens: LensOption) {
    _uiState.update { it.copy(selectedLens = lens, zoomRatio = lens.zoomRatio) }
    controller.setZoom(lens.zoomRatio)
  }

  fun onPinchZoom(deltaScale: Float) {
    val current = _uiState.value.zoomRatio
    val newZoom = (current * deltaScale).coerceIn(0.6f, 10.0f)
    _uiState.update { it.copy(zoomRatio = newZoom) }
    controller.setZoom(newZoom)
  }

  fun switchCameraFacing() {
    val newFacing = controller.switchFacing()
    _uiState.update {
      it.copy(
        currentFacing = newFacing,
        selectedLens = LensOption("1x", 1.0f),
        zoomRatio = 1.0f
      )
    }
  }

  fun toggleSceneOptimizer() {
    _uiState.update { it.copy(sceneOptimizerEnabled = !it.sceneOptimizerEnabled) }
  }

  fun onTapToFocus(offset: Offset, viewWidth: Int, viewHeight: Int) {
    _uiState.update { it.copy(focusPoint = offset) }
    controller.focusOnPoint(
      normalizedX = (offset.x / viewWidth).coerceIn(0f, 1f),
      normalizedY = (offset.y / viewHeight).coerceIn(0f, 1f),
      viewWidth = viewWidth,
      viewHeight = viewHeight
    )

    focusFadeJob?.cancel()
    focusFadeJob = viewModelScope.launch {
      delay(2500)
      _uiState.update { it.copy(focusPoint = null) }
    }
  }

  fun onExposureAdjusted(delta: Float) {
    val current = _uiState.value.exposureCompensation
    val range = _uiState.value.exposureRange
    val step = if (delta > 0) 1 else -1
    val next = (current + step).coerceIn(range.first, range.last)
    _uiState.update { it.copy(exposureCompensation = next) }
    controller.setExposureCompensation(next)
  }

  fun onShutterPressed() {
    val mode = _uiState.value.currentMode
    if (mode == CameraMode.VIDEO) {
      if (_uiState.value.isRecordingVideo) {
        stopVideoRecording()
      } else {
        startVideoRecording()
      }
      return
    }

    val timer = _uiState.value.timerSetting
    if (timer.seconds > 0) {
      startCountdown(timer.seconds) {
        executeCapture()
      }
    } else {
      executeCapture()
    }
  }

  private fun startCountdown(seconds: Int, onFinish: () -> Unit) {
    viewModelScope.launch {
      for (i in seconds downTo 1) {
        _uiState.update { it.copy(countdownValue = i) }
        delay(1000)
      }
      _uiState.update { it.copy(countdownValue = null) }
      onFinish()
    }
  }

  private fun executeCapture() {
    _uiState.update { it.copy(isCapturingFlash = true) }
    viewModelScope.launch {
      delay(120)
      _uiState.update { it.copy(isCapturingFlash = false) }
    }

    controller.capturePhoto(
      onSuccess = { uri ->
        viewModelScope.launch {
          controller.loadLatestMedia()
        }
      },
      onError = { err ->
        showToast(err)
      }
    )
  }

  private fun startVideoRecording() {
    controller.startVideoRecording(
      onSuccess = {
        _uiState.update { it.copy(isRecordingVideo = true, videoRecordingSeconds = 0) }
        videoTimerJob?.cancel()
        videoTimerJob = viewModelScope.launch {
          while (_uiState.value.isRecordingVideo) {
            delay(1000)
            _uiState.update { it.copy(videoRecordingSeconds = it.videoRecordingSeconds + 1) }
          }
        }
      },
      onError = { err ->
        showToast(err)
      }
    )
  }

  private fun stopVideoRecording() {
    videoTimerJob?.cancel()
    _uiState.update { it.copy(isRecordingVideo = false) }
    controller.stopVideoRecording { uri ->
      viewModelScope.launch {
        controller.loadLatestMedia()
      }
    }
  }

  fun openGallery() {
    _uiState.update { it.copy(isGalleryOpen = true) }
  }

  fun closeGallery() {
    _uiState.update { it.copy(isGalleryOpen = false) }
  }

  fun setPortraitBlur(blur: Float) {
    _uiState.update { it.copy(portraitBlurLevel = blur) }
  }

  private fun showToast(msg: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(toastMessage = msg) }
      delay(2000)
      _uiState.update { it.copy(toastMessage = null) }
    }
  }

  override fun onCleared() {
    super.onCleared()
    controller.release()
  }
}
