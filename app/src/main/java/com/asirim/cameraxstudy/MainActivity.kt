package com.asirim.cameraxstudy

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.asirim.cameraxstudy.databinding.ActivityMainBinding
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * After 'Step 8' the following log wrote. After this log, I changed something at 'My Personal Step or Last Step'.
 *
 * No supported surface combination is found for camera device - Id : 1.  May be attempting to bind too many use cases. Existing surfaces: [] New configs: [androidx.camera.video.impl.VideoCaptureConfig@a0ec75c, androidx.camera.core.impl.ImageCaptureConfig@ec9c1cf, androidx.camera.core.impl.PreviewConfig@791222e]
 */
typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Image operation
    private var imageCapture: ImageCapture? = null

    /**
     * My Personal Step or Last Step
     *
     * 'videoCapture' and 'recording' closed at 'Combine imageCapture and imageAnalyzer at the end' commit
     * */
    // Video operation
//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    // ------------------ Lifecycle Functions ------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle permissions
        if (allPermissionsGranted()) startCamera() else requestPermissions()

        // Set up the listeners for take photo and video capture buttons
        binding.buttonImageCapture.setOnClickListener { takePhoto() }
        /**
         * My Personal Step or Last Step
         *
         * 'buttonVideoCapture.setOnClickListener' closed at 'Combine imageCapture and imageAnalyzer at the end' commit
         * */
//        binding.buttonVideoCapture.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // ------------------ CameraX Operation Functions ------------------

    /**
     * Step 2
     *
     * Created at 'Add CameraX dependency and set MainActivity structure' commit
     * */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Step 3.1
     *
     * Created at 'Request the necessary permissions' commit
     * */
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            // Handle Permission granted/rejected
            var permissionGranted = true

            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }

            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }

        }

    /**
     * Step 3.2
     *
     * Created at 'Request the necessary permissions' commit
     * */
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    /**
     * Step 4
     * * Created at 'Implement Preview use case' commit
     *
     * Step 5.2
     * * Edited at 'Implement ImageCapture use case' commit
     *
     * Step 6.2
     * * Edited at 'Implement ImageAnalysis use case' commit
     *
     * Step 7.2
     * * Edited at 'Implement VideoCapture use case' commit
     *
     * My Personal Step or Last Step
     * * Edited at 'Combine imageCapture and imageAnalyzer at the end' commit
     * */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        binding.cameraPreviewView.surfaceProvider
                    )
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        LuminosityAnalyzer { luma ->
                            Log.d(TAG, "Average luminosity: $luma")
                        }
                    )
                }


            /**
             * My Personal Step or Last Step
             *
             * 'recorder' and 'videoCapture' closed at 'Combine imageCapture and imageAnalyzer at the end' commit
             * */
//            val recorder = Recorder.Builder()
//                .setQualitySelector(QualitySelector.from(Quality.HIGHEST,
//                    FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
//                .build()
//            videoCapture = VideoCapture.withOutput(recorder)

            // Select a camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer,
                    /**
                     * My Personal Step or Last Step
                     *
                     * 'videoCapture' closed at 'Combine imageCapture and imageAnalyzer at the end' commit
                     * */
//                    videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Step 5.1
     *
     * Created at 'Implement ImageCapture use case' commit
     * */
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(imageCaptureException: ImageCaptureException) {
                    Log.e(
                        TAG,
                        "Photo capture failed: ${imageCaptureException.message}",
                        imageCaptureException
                    )
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    /**
     * Step 6.1
     *
     * Created at 'Implement ImageAnalysis use case' commit
     * */
    private class LuminosityAnalyzer(
        private val listener: LumaListener
    ) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    /**
     * My Personal Step or Last Step
     *
     * 'captureVideo' closed at 'Combine imageCapture and imageAnalyzer at the end' commit
     * */
    // Implements VideoCapture use case, including start and stop capturing.
//    private fun captureVideo() {
//        val videoCapture = this.videoCapture ?: return
//
//        binding.buttonVideoCapture.isEnabled = false
//
//        val currentRecording = recording
//        if (currentRecording != null) {
//            // Stop the current recording session.
//            currentRecording.stop()
//            recording = null
//            return
//        }
//
//        // create and start a new recording session
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
//            }
//        }
//
//        val mediaStoreOutputOptions = MediaStoreOutputOptions
//            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
//        recording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutputOptions)
//            .apply {
//                if (PermissionChecker.checkSelfPermission(
//                        this@MainActivity,
//                        Manifest.permission.RECORD_AUDIO
//                    ) ==
//                    PermissionChecker.PERMISSION_GRANTED
//                ) {
//                    withAudioEnabled()
//                }
//            }.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
//                when (recordEvent) {
//                    is VideoRecordEvent.Start -> {
//                        binding.buttonVideoCapture.apply {
//                            text = getString(R.string.stop_capture)
//                            isEnabled = true
//                        }
//                    }
//
//                    is VideoRecordEvent.Finalize -> {
//                        if (!recordEvent.hasError()) {
//                            val message = "Video capture succeeded: ${
//                                recordEvent.outputResults.outputUri
//                            }"
//                            Toast.makeText(baseContext, message, Toast.LENGTH_SHORT)
//                                .show()
//                            Log.d(TAG, message)
//                        } else {
//                            recording?.close()
//                            recording = null
//                            Log.e(
//                                TAG,
//                                "Video capture ends with error: ${
//                                    recordEvent.error
//                                }"
//                            )
//                        }
//                        binding.buttonVideoCapture.apply {
//                            text = getString(R.string.start_capture)
//                            isEnabled = true
//                        }
//                    }
//                }
//            }
//    }

    companion object {
        private const val TAG = "CameraX Study"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    this.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}
       