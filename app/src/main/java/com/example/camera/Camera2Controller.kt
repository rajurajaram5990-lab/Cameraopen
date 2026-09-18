package com.example.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.media.MediaActionSound
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import com.example.model.CameraFacing
import com.example.model.CameraFilterEffect
import com.example.model.CameraMode
import com.example.model.CapturedItem
import com.example.model.FlashMode
import com.example.model.LensOption
import com.example.model.ResolutionOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Camera2Controller(private val context: Context) {

  private val tag = "Camera2Controller"
  private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
  private var sound: MediaActionSound? = null

  private fun playSound(soundType: Int) {
    try {
      if (sound == null) {
        sound = MediaActionSound().apply {
          try {
            load(MediaActionSound.SHUTTER_CLICK)
            load(MediaActionSound.START_VIDEO_RECORDING)
            load(MediaActionSound.STOP_VIDEO_RECORDING)
          } catch (e: Throwable) {
            Log.w(tag, "Sound loading warning: ${e.message}")
          }
        }
      }
      sound?.play(soundType)
    } catch (e: Throwable) {
      Log.w(tag, "Failed to play camera sound: ${e.message}")
    }
  }

  private fun getDeviceRotation(): Int {
    return try {
      val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      @Suppress("DEPRECATION")
      windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
    } catch (e: Throwable) {
      Surface.ROTATION_0
    }
  }

  // Background Thread
  private var backgroundThread: HandlerThread? = null
  private var backgroundHandler: Handler? = null

  // Active Camera State
  private var cameraDevice: CameraDevice? = null
  private var captureSession: CameraCaptureSession? = null
  private var previewRequestBuilder: CaptureRequest.Builder? = null
  private var imageReader: android.media.ImageReader? = null
  private var textureView: TextureView? = null
  private var previewSurface: Surface? = null

  // Current Parameters
  private var currentCameraId: String = ""
  private var currentFacing: CameraFacing = CameraFacing.BACK
  private var currentFlashMode: FlashMode = FlashMode.OFF
  private var currentFilterEffect: CameraFilterEffect = CameraFilterEffect.ORIGINAL
  private var currentZoomRatio: Float = 1.0f
  private var currentExposureCompensation: Int = 0
  private var isFrontCamera: Boolean = false

  // Characteristics Cache
  private var characteristics: CameraCharacteristics? = null
  private var sensorArraySize: Rect = Rect()
  private var sensorOrientation: Int = 90
  private var zoomRange: Range<Float> = Range(1.0f, 1.0f)
  private var maxDigitalZoom: Float = 1.0f
  private var exposureRange: Range<Int> = Range(0, 0)
  private var exposureStep: Float = 1.0f
  private var supportedEffects: IntArray = intArrayOf()
  private var hasFlash: Boolean = false

  // Dynamic Capabilities Exposed to UI
  val availableLensesFlow = MutableStateFlow<List<LensOption>>(emptyList())
  val supportedResolutionsFlow = MutableStateFlow<List<ResolutionOption>>(emptyList())
  val currentResolutionFlow = MutableStateFlow("12M")
  val currentZoomRatioFlow = MutableStateFlow(1.0f)
  val exposureCompensationRangeFlow = MutableStateFlow(-4..4)
  val currentExposureFlow = MutableStateFlow(0)
  val latestCapturedMediaFlow = MutableStateFlow<CapturedItem?>(null)
  val isCameraReadyFlow = MutableStateFlow(false)

  // Video Recording
  private var mediaRecorder: MediaRecorder? = null
  private var videoOutputFile: File? = null
  private var isRecordingVideo: Boolean = false

  init {
    loadLatestMedia()
  }

  fun startBackgroundThread() {
    if (backgroundThread == null) {
      backgroundThread = HandlerThread("Camera2Bg").apply {
        start()
        backgroundHandler = Handler(looper)
      }
    }
  }

  fun stopBackgroundThread() {
    backgroundThread?.quitSafely()
    try {
      backgroundThread?.join(500)
    } catch (e: InterruptedException) {
      Log.e(tag, "Error stopping bg thread", e)
    }
    backgroundThread = null
    backgroundHandler = null
  }

  fun attachTextureView(view: TextureView) {
    this.textureView = view
    view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
      override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        startBackgroundThread()
        openCamera(currentFacing)
      }

      override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        configureTransform(width, height)
      }

      override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        closeCamera()
        return true
      }

      override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    if (view.isAvailable) {
      startBackgroundThread()
      openCamera(currentFacing)
    }
  }

  fun switchFacing(): CameraFacing {
    val newFacing = if (currentFacing == CameraFacing.BACK) CameraFacing.FRONT else CameraFacing.BACK
    currentFacing = newFacing
    currentZoomRatio = 1.0f
    currentZoomRatioFlow.value = 1.0f
    closeCamera()
    openCamera(newFacing)
    return newFacing
  }

  @SuppressLint("MissingPermission")
  fun openCamera(facing: CameraFacing) {
    startBackgroundThread()
    currentFacing = facing

    val handler = backgroundHandler ?: return
    handler.post {
      try {
        val cameraId = selectCameraIdForFacing(facing) ?: return@post
        currentCameraId = cameraId
        characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val char = characteristics ?: return@post

        isFrontCamera = char.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        sensorOrientation = char.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
        sensorArraySize = char.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: Rect(0, 0, 4000, 3000)
        hasFlash = char.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

        // Detect Exposure Compensation
        val expRange = char.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: Range(0, 0)
        exposureRange = expRange
        exposureCompensationRangeFlow.value = expRange.lower..expRange.upper
        val stepRational = char.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        exposureStep = stepRational?.toFloat() ?: 1.0f

        // Detect Zoom Range
        detectZoomCapabilities(char)

        // Detect Supported Resolutions
        detectResolutions(char)

        // Detect Effects
        supportedEffects = char.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS) ?: intArrayOf()

        // Configure ImageReader with highest supported resolution
        setupImageReader(char)

        cameraManager.openCamera(cameraId, cameraStateCallback, handler)
      } catch (e: Exception) {
        Log.e(tag, "Failed to open camera", e)
      }
    }
  }

  private fun selectCameraIdForFacing(facing: CameraFacing): String? {
    return try {
      val targetFacing = if (facing == CameraFacing.BACK) {
        CameraCharacteristics.LENS_FACING_BACK
      } else {
        CameraCharacteristics.LENS_FACING_FRONT
      }

      val cameraIds = cameraManager.cameraIdList ?: emptyArray()
      for (id in cameraIds) {
        val char = cameraManager.getCameraCharacteristics(id)
        if (char.get(CameraCharacteristics.LENS_FACING) == targetFacing) {
          return id
        }
      }
      cameraIds.firstOrNull()
    } catch (e: Throwable) {
      Log.e(tag, "Failed to query camera devices", e)
      null
    }
  }

  private fun detectZoomCapabilities(char: CameraCharacteristics) {
    var minZoom = 1.0f
    var maxZoom = 1.0f

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val ratioRange = char.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
      if (ratioRange != null) {
        minZoom = ratioRange.lower
        maxZoom = ratioRange.upper
        zoomRange = ratioRange
      }
    }

    val maxDigZoom = char.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
    maxDigitalZoom = maxDigZoom
    if (maxZoom <= 1.0f && maxDigZoom > 1.0f) {
      maxZoom = maxDigZoom
      zoomRange = Range(1.0f, maxDigZoom)
    }

    // Detect available lenses dynamically
    val lenses = mutableListOf<LensOption>()

    // Check if wide lens (< 1.0f) is supported by device
    if (minZoom < 0.9f) {
      val label = if (minZoom <= 0.65f) ".6" else String.format(Locale.US, ".%d", (minZoom * 10).roundToInt())
      lenses.add(LensOption(label = label, zoomRatio = minZoom))
    }

    // Main 1x lens is always available
    lenses.add(LensOption(label = "1x", zoomRatio = 1.0f))

    // 2x lens if max zoom is at least 2.0x
    if (maxZoom >= 2.0f) {
      lenses.add(LensOption(label = "2", zoomRatio = 2.0f))
    }

    // 3x or telephoto lens if max zoom is at least 3.0x
    if (maxZoom >= 3.0f) {
      lenses.add(LensOption(label = "3", zoomRatio = 3.0f))
    }

    // Check physical camera capabilities on logical multi-cameras (API 28+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val physicalIds = char.physicalCameraIds
      if (physicalIds.isNotEmpty()) {
        Log.d(tag, "Logical multi-camera with physical IDs: $physicalIds")
      }
    }

    availableLensesFlow.value = lenses
  }

  private fun detectResolutions(char: CameraCharacteristics) {
    val map = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
    val jpegSizes = map.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()

    val options = mutableListOf<ResolutionOption>()
    for (size in jpegSizes.sortedByDescending { it.width * it.height }) {
      val mp = (size.width * size.height) / 1_000_000.0f
      val mpFormatted = if (mp >= 10.0f) "${mp.roundToInt()}M" else String.format(Locale.US, "%.1fM", mp)
      val label = "${size.width}x${size.height}"
      options.add(ResolutionOption(size.width, size.height, label, mpFormatted))
    }

    supportedResolutionsFlow.value = options
    val best = options.firstOrNull()
    if (best != null) {
      currentResolutionFlow.value = best.megaPixels
    }
  }

  private fun setupImageReader(char: CameraCharacteristics) {
    try {
      imageReader?.close()
      val map = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
      val sizes = map.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
      val chosenSize = sizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

      val reader = android.media.ImageReader.newInstance(
        chosenSize.width,
        chosenSize.height,
        ImageFormat.JPEG,
        2
      )
      reader.setOnImageAvailableListener({ r ->
        try {
          val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
          backgroundHandler?.post {
            try {
              saveImageToMediaStore(image)
            } finally {
              image.close()
            }
          }
        } catch (e: Throwable) {
          Log.e(tag, "Error handling captured image", e)
        }
      }, backgroundHandler)

      this.imageReader = reader
    } catch (e: Throwable) {
      Log.e(tag, "Failed to setup ImageReader", e)
    }
  }

  private val cameraStateCallback = object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
      cameraDevice = camera
      createCameraPreviewSession()
    }

    override fun onDisconnected(camera: CameraDevice) {
      try {
        camera.close()
      } catch (e: Throwable) {
        Log.e(tag, "Error closing camera on disconnect", e)
      }
      cameraDevice = null
      isCameraReadyFlow.value = false
    }

    override fun onError(camera: CameraDevice, error: Int) {
      Log.e(tag, "Camera error: $error")
      try {
        camera.close()
      } catch (e: Throwable) {
        Log.e(tag, "Error closing camera on error", e)
      }
      cameraDevice = null
      isCameraReadyFlow.value = false
    }
  }

  private fun createCameraPreviewSession() {
    val camera = cameraDevice ?: return
    val texture = textureView?.surfaceTexture ?: return
    val char = characteristics ?: return
    val handler = backgroundHandler ?: return

    try {
      // Find optimal preview size
      val map = char.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
      val previewSizes = map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray()
      // Pick 1920x1440 or 1440x1080 (4:3 aspect ratio)
      val previewSize = previewSizes.firstOrNull { it.width <= 1920 && it.width * 3 == it.height * 4 }
        ?: previewSizes.firstOrNull { it.width <= 1920 }
        ?: previewSizes.firstOrNull()
        ?: Size(1440, 1080)

      texture.setDefaultBufferSize(previewSize.width, previewSize.height)
      previewSurface = Surface(texture)

      val surfaces = mutableListOf<Surface>()
      previewSurface?.let { surfaces.add(it) }
      imageReader?.surface?.let { surfaces.add(it) }

      if (surfaces.isEmpty()) return

      previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
        previewSurface?.let { addTarget(it) }
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        applyFlashMode(this, currentFlashMode)
        applyZoom(this, currentZoomRatio)
        applyFilterEffect(this, currentFilterEffect)
        applyExposure(this, currentExposureCompensation)
      }

      camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
          if (cameraDevice == null) return
          captureSession = session
          try {
            val request = previewRequestBuilder?.build() ?: return
            session.setRepeatingRequest(request, null, handler)
            isCameraReadyFlow.value = true

            // Configure Matrix transformation on main thread
            textureView?.post {
              textureView?.let { configureTransform(it.width, it.height) }
            }
          } catch (e: Throwable) {
            Log.e(tag, "Failed to start preview request", e)
          }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
          Log.e(tag, "CaptureSession configuration failed")
          isCameraReadyFlow.value = false
        }
      }, handler)

    } catch (e: Throwable) {
      Log.e(tag, "Failed to create preview session", e)
    }
  }

  fun setZoom(ratio: Float) {
    currentZoomRatio = ratio
    currentZoomRatioFlow.value = ratio
    val builder = previewRequestBuilder ?: return
    val session = captureSession ?: return
    val handler = backgroundHandler ?: return

    applyZoom(builder, ratio)
    try {
      session.setRepeatingRequest(builder.build(), null, handler)
    } catch (e: Exception) {
      Log.e(tag, "Failed to update zoom", e)
    }
  }

  private fun applyZoom(builder: CaptureRequest.Builder, ratio: Float) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && zoomRange.upper > 1.0f) {
      val clamped = ratio.coerceIn(zoomRange.lower, zoomRange.upper)
      builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, clamped)
    } else {
      // Fallback crop region
      val activeRect = sensorArraySize
      val maxZoom = max(1.0f, maxDigitalZoom)
      val clampedRatio = ratio.coerceIn(1.0f, maxZoom)
      val cropW = (activeRect.width() / clampedRatio).toInt()
      val cropH = (activeRect.height() / clampedRatio).toInt()
      val cropX = (activeRect.width() - cropW) / 2
      val cropY = (activeRect.height() - cropH) / 2
      builder.set(CaptureRequest.SCALER_CROP_REGION, Rect(cropX, cropY, cropX + cropW, cropY + cropH))
    }
  }

  fun setFlashMode(mode: FlashMode) {
    currentFlashMode = mode
    val builder = previewRequestBuilder ?: return
    val session = captureSession ?: return
    val handler = backgroundHandler ?: return

    applyFlashMode(builder, mode)
    try {
      session.setRepeatingRequest(builder.build(), null, handler)
    } catch (e: Exception) {
      Log.e(tag, "Failed to update flash", e)
    }
  }

  private fun applyFlashMode(builder: CaptureRequest.Builder, mode: FlashMode) {
    if (!hasFlash) {
      builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
      builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
      return
    }

    when (mode) {
      FlashMode.OFF -> {
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
      }
      FlashMode.ON -> {
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        builder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE)
      }
      FlashMode.AUTO -> {
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
      }
    }
  }

  fun setFilterEffect(effect: CameraFilterEffect) {
    currentFilterEffect = effect
    val builder = previewRequestBuilder ?: return
    val session = captureSession ?: return
    val handler = backgroundHandler ?: return

    applyFilterEffect(builder, effect)
    try {
      session.setRepeatingRequest(builder.build(), null, handler)
    } catch (e: Exception) {
      Log.e(tag, "Failed to update effect", e)
    }
  }

  private fun applyFilterEffect(builder: CaptureRequest.Builder, effect: CameraFilterEffect) {
    val mode = effect.camera2EffectMode
    if (mode != null && supportedEffects.contains(mode)) {
      builder.set(CaptureRequest.CONTROL_EFFECT_MODE, mode)
    } else {
      builder.set(CaptureRequest.CONTROL_EFFECT_MODE, CameraMetadata.CONTROL_EFFECT_MODE_OFF)
    }
  }

  fun setExposureCompensation(value: Int) {
    val clamped = value.coerceIn(exposureRange.lower, exposureRange.upper)
    currentExposureCompensation = clamped
    currentExposureFlow.value = clamped

    val builder = previewRequestBuilder ?: return
    val session = captureSession ?: return
    val handler = backgroundHandler ?: return

    applyExposure(builder, clamped)
    try {
      session.setRepeatingRequest(builder.build(), null, handler)
    } catch (e: Exception) {
      Log.e(tag, "Failed to update exposure", e)
    }
  }

  private fun applyExposure(builder: CaptureRequest.Builder, value: Int) {
    builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value)
  }

  fun focusOnPoint(normalizedX: Float, normalizedY: Float, viewWidth: Int, viewHeight: Int) {
    val session = captureSession ?: return
    val builder = previewRequestBuilder ?: return
    val handler = backgroundHandler ?: return
    val activeRect = sensorArraySize

    // Map view coordinates to sensor metering rectangle
    val sensorX = (normalizedX * activeRect.width()).toInt().coerceIn(0, activeRect.width())
    val sensorY = (normalizedY * activeRect.height()).toInt().coerceIn(0, activeRect.height())
    val halfSize = 120
    val meteringRect = Rect(
      max(0, sensorX - halfSize),
      max(0, sensorY - halfSize),
      min(activeRect.width(), sensorX + halfSize),
      min(activeRect.height(), sensorY + halfSize)
    )

    try {
      val afRegion = MeteringRectangle(meteringRect, MeteringRectangle.METERING_WEIGHT_MAX)
      builder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(afRegion))
      builder.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(afRegion))
      builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
      builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)

      session.capture(builder.build(), null, handler)

      // Resume repeating auto-focus
      builder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE)
      session.setRepeatingRequest(builder.build(), null, handler)
    } catch (e: Exception) {
      Log.e(tag, "Tap to focus error", e)
    }
  }

  fun capturePhoto(onSuccess: (Uri) -> Unit, onError: (String) -> Unit) {
    val camera = cameraDevice ?: run {
      onError("Camera not ready")
      return
    }
    val session = captureSession ?: run {
      onError("Session not ready")
      return
    }
    val reader = imageReader ?: run {
      onError("ImageReader not ready")
      return
    }
    val handler = backgroundHandler ?: run {
      onError("Handler not ready")
      return
    }

    try {
      playSound(MediaActionSound.SHUTTER_CLICK)

      val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
        addTarget(reader.surface)
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        applyFlashMode(this, currentFlashMode)
        applyZoom(this, currentZoomRatio)
        applyFilterEffect(this, currentFilterEffect)
        applyExposure(this, currentExposureCompensation)

        // Calculate rotation safely
        val rotation = getDeviceRotation()
        val jpegOrientation = getJpegOrientation(sensorOrientation, rotation, isFrontCamera)
        set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
      }

      session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
          session: CameraCaptureSession,
          request: CaptureRequest,
          result: TotalCaptureResult
        ) {
          Log.d(tag, "Still capture completed successfully")
        }
      }, handler)

    } catch (e: Exception) {
      Log.e(tag, "Error capturing photo", e)
      onError(e.message ?: "Failed to capture photo")
    }
  }

  private fun getJpegOrientation(sensorOrientation: Int, deviceRotation: Int, isFacingFront: Boolean): Int {
    var deviceOrientationDegrees = when (deviceRotation) {
      Surface.ROTATION_0 -> 0
      Surface.ROTATION_90 -> 90
      Surface.ROTATION_180 -> 180
      Surface.ROTATION_270 -> 270
      else -> 0
    }
    return if (isFacingFront) {
      (sensorOrientation + deviceOrientationDegrees) % 360
    } else {
      (sensorOrientation - deviceOrientationDegrees + 360) % 360
    }
  }

  private fun saveImageToMediaStore(image: android.media.Image) {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "IMG_${timeStamp}.jpg"

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, filename)
      put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
    }

    try {
      val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
      if (imageUri != null) {
        resolver.openOutputStream(imageUri)?.use { out ->
          out.write(bytes)
          out.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
          resolver.update(imageUri, contentValues, null, null)
        }

        val item = CapturedItem(
          uri = imageUri,
          name = filename,
          dateAdded = System.currentTimeMillis(),
          isVideo = false,
          width = image.width,
          height = image.height,
          sizeBytes = bytes.size.toLong()
        )
        latestCapturedMediaFlow.value = item
        Log.d(tag, "Photo saved successfully: $imageUri")
      }
    } catch (e: Exception) {
      Log.e(tag, "Failed to save photo to MediaStore", e)
    }
  }

  fun startVideoRecording(onSuccess: () -> Unit, onError: (String) -> Unit) {
    val camera = cameraDevice ?: run {
      onError("Camera not ready")
      return
    }
    val texture = textureView?.surfaceTexture ?: run {
      onError("Preview not ready")
      return
    }
    val handler = backgroundHandler ?: return

    try {
      playSound(MediaActionSound.START_VIDEO_RECORDING)

      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val videoFile = File(context.cacheDir, "VID_${timeStamp}.mp4")
      this.videoOutputFile = videoFile

      @Suppress("DEPRECATION")
      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        MediaRecorder()
      }

      recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setVideoSource(MediaRecorder.VideoSource.SURFACE)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setOutputFile(videoFile.absolutePath)
        setVideoEncodingBitRate(10_000_000)
        setVideoFrameRate(30)
        setVideoSize(1920, 1080)
        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        val rotation = getDeviceRotation()
        val orientation = getJpegOrientation(sensorOrientation, rotation, isFrontCamera)
        setOrientationHint(orientation)
        prepare()
      }
      this.mediaRecorder = recorder

      val recorderSurface = recorder.surface
      val previewSurface = Surface(texture)

      val surfaces = listOf(previewSurface, recorderSurface)
      val recordRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
        addTarget(previewSurface)
        addTarget(recorderSurface)
        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
        applyFlashMode(this, currentFlashMode)
        applyZoom(this, currentZoomRatio)
        applyExposure(this, currentExposureCompensation)
      }

      camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
          captureSession = session
          session.setRepeatingRequest(recordRequestBuilder.build(), null, handler)
          recorder.start()
          isRecordingVideo = true
          onSuccess()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
          onError("Failed to configure recording session")
        }
      }, handler)

    } catch (e: Exception) {
      Log.e(tag, "Failed to start recording", e)
      onError(e.message ?: "Failed to start recording")
    }
  }

  fun stopVideoRecording(onComplete: (Uri?) -> Unit) {
    if (!isRecordingVideo) return
    isRecordingVideo = false
    playSound(MediaActionSound.STOP_VIDEO_RECORDING)

    backgroundHandler?.post {
      try {
        mediaRecorder?.stop()
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null

        val file = videoOutputFile
        if (file != null && file.exists()) {
          val uri = saveVideoToMediaStore(file)
          file.delete()
          onComplete(uri)
        } else {
          onComplete(null)
        }

        // Restore photo preview session
        createCameraPreviewSession()
      } catch (e: Exception) {
        Log.e(tag, "Failed to stop recording", e)
        onComplete(null)
      }
    }
  }

  private fun saveVideoToMediaStore(file: File): Uri? {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "VID_${timeStamp}.mp4"

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.Video.Media.DISPLAY_NAME, filename)
      put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }

    try {
      val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
      if (videoUri != null) {
        resolver.openOutputStream(videoUri)?.use { out ->
          file.inputStream().use { input ->
            input.copyTo(out)
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
          resolver.update(videoUri, contentValues, null, null)
        }

        val item = CapturedItem(
          uri = videoUri,
          name = filename,
          dateAdded = System.currentTimeMillis(),
          isVideo = true,
          sizeBytes = file.length()
        )
        latestCapturedMediaFlow.value = item
        return videoUri
      }
    } catch (e: Exception) {
      Log.e(tag, "Failed to save video to MediaStore", e)
    }
    return null
  }

  private fun configureTransform(viewWidth: Int, viewHeight: Int) {
    val view = textureView ?: return
    if (viewWidth <= 0 || viewHeight <= 0) return
    try {
      val rotation = getDeviceRotation()

      val matrix = Matrix()
      val viewRect = android.graphics.RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
      val bufferRect = android.graphics.RectF(0f, 0f, 1080f, 1440f)
      val centerX = viewRect.centerX()
      val centerY = viewRect.centerY()

      if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
        val scale = max(
          viewHeight.toFloat() / 1440f,
          viewWidth.toFloat() / 1080f
        )
        matrix.postScale(scale, scale, centerX, centerY)
        matrix.postRotate(if (rotation == Surface.ROTATION_90) 270f else 90f, centerX, centerY)
      } else if (rotation == Surface.ROTATION_180) {
        matrix.postRotate(180f, centerX, centerY)
      }
      view.setTransform(matrix)
    } catch (e: Throwable) {
      Log.e(tag, "Error configuring transform matrix", e)
    }
  }

  fun loadLatestMedia() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val projection = arrayOf(
          MediaStore.Images.Media._ID,
          MediaStore.Images.Media.DISPLAY_NAME,
          MediaStore.Images.Media.DATE_ADDED,
          MediaStore.Images.Media.WIDTH,
          MediaStore.Images.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
          MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          projection,
          null,
          null,
          sortOrder
        )?.use { cursor ->
          if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val date = cursor.getLong(dateColumn)
            val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())

            latestCapturedMediaFlow.value = CapturedItem(
              uri = uri,
              name = name,
              dateAdded = date,
              isVideo = false
            )
          }
        }
      } catch (e: Throwable) {
        Log.e(tag, "Failed to load latest media", e)
      }
    }
  }

  fun closeCamera() {
    try {
      captureSession?.close()
      captureSession = null
      cameraDevice?.close()
      cameraDevice = null
      imageReader?.close()
      imageReader = null
      previewSurface?.release()
      previewSurface = null
      isCameraReadyFlow.value = false
    } catch (e: Exception) {
      Log.e(tag, "Error closing camera", e)
    }
  }

  fun release() {
    closeCamera()
    stopBackgroundThread()
    try {
      sound?.release()
      sound = null
    } catch (e: Throwable) {
      Log.e(tag, "Error releasing sound", e)
    }
  }
}
