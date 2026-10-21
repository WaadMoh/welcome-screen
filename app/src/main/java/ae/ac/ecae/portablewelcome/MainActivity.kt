package ae.ac.ecae.portablewelcome

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import ae.ac.ecae.portablewelcome.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    companion object {
        /*
         Replace this with the full GitHub Pages address
         of the page that displays your changing content.
        */
        private const val CONTENT_URL =
            "https://YOUR-GITHUB-NAME.github.io/YOUR-REPOSITORY/"

        /*
         A face must be detected this many consecutive times
         before the welcome screen appears.
        */
        private const val DETECTIONS_REQUIRED = 2

        /*
         The welcome overlay closes after no face has been
         detected for this number of milliseconds.
        */
        private const val DEPARTURE_DELAY_MS = 3000L

        /*
         Do not send frames to ML Kit more frequently
         than this interval.
        */
        private const val ANALYSIS_INTERVAL_MS = 300L

        private const val STATUS_HIDE_DELAY_MS = 3500L
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var faceDetector: FaceDetector

    private val analysisInProgress =
        AtomicBoolean(false)

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var personPresent = false

    private var consecutiveDetections = 0

    private var lastDetectionTime = 0L

    private var lastAnalysisTime = 0L

    private var cameraProvider: ProcessCameraProvider? = null

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permissionGranted ->

            if (permissionGranted) {
                startFrontCamera()
            } else {
                showPersistentStatus(
                    getString(
                        R.string.status_camera_permission
                    )
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        cameraExecutor =
            Executors.newSingleThreadExecutor()

        configureFaceDetector()

        configureWebView()

        enterImmersiveMode()

        configureKioskMode()

        checkCameraPermission()
    }

    private fun configureFaceDetector() {
        val options =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .setLandmarkMode(
                    FaceDetectorOptions.LANDMARK_MODE_NONE
                )
                .setContourMode(
                    FaceDetectorOptions.CONTOUR_MODE_NONE
                )
                .setClassificationMode(
                    FaceDetectorOptions.CLASSIFICATION_MODE_NONE
                )
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build()

        faceDetector =
            FaceDetection.getClient(options)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        binding.contentWebView.apply {

            settings.javaScriptEnabled = true

            settings.domStorageEnabled = true

            settings.mediaPlaybackRequiresUserGesture = false

            settings.cacheMode =
                WebSettings.LOAD_DEFAULT

            settings.useWideViewPort = true

            settings.loadWithOverviewMode = true

            settings.allowFileAccess = false

            settings.allowContentAccess = false

            settings.mixedContentMode =
                WebSettings.MIXED_CONTENT_NEVER_ALLOW

            isVerticalScrollBarEnabled = false

            isHorizontalScrollBarEnabled = false

            overScrollMode = View.OVER_SCROLL_NEVER

            webChromeClient = WebChromeClient()

            webViewClient =
                object : WebViewClient() {

                    override fun onPageStarted(
                        view: WebView?,
                        url: String?,
                        favicon: Bitmap?
                    ) {
                        super.onPageStarted(
                            view,
                            url,
                            favicon
                        )

                        showTemporaryStatus(
                            getString(
                                R.string.status_starting
                            )
                        )
                    }

                    override fun onPageFinished(
                        view: WebView?,
                        url: String?
                    ) {
                        super.onPageFinished(
                            view,
                            url
                        )

                        injectVideoControlFunctions()

                        showTemporaryStatus(
                            getString(
                                R.string.status_ready
                            )
                        )
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(
                            view,
                            request,
                            error
                        )

                        if (request?.isForMainFrame == true) {
                            showPersistentStatus(
                                getString(
                                    R.string.status_offline
                                )
                            )
                        }
                    }
                }

            loadUrl(CONTENT_URL)
        }
    }

    private fun injectVideoControlFunctions() {
        val script = """
            (function() {
                window.ECAEWelcomeSystem = {
                    pausedVideos: [],

                    pauseContent: function() {
                        this.pausedVideos = [];

                        document.querySelectorAll('video').forEach(
                            function(video) {
                                if (!video.paused) {
                                    window.ECAEWelcomeSystem.pausedVideos.push(video);
                                    video.pause();
                                }
                            }
                        );

                        document.querySelectorAll('audio').forEach(
                            function(audio) {
                                if (!audio.paused) {
                                    window.ECAEWelcomeSystem.pausedVideos.push(audio);
                                    audio.pause();
                                }
                            }
                        );

                        window.dispatchEvent(
                            new CustomEvent('ecaePersonDetected')
                        );
                    },

                    resumeContent: function() {
                        this.pausedVideos.forEach(
                            function(media) {
                                var playPromise = media.play();

                                if (
                                    playPromise !== undefined &&
                                    playPromise.catch
                                ) {
                                    playPromise.catch(
                                        function(error) {
                                            console.log(
                                                'Media resume blocked',
                                                error
                                            );
                                        }
                                    );
                                }
                            }
                        );

                        this.pausedVideos = [];

                        window.dispatchEvent(
                            new CustomEvent('ecaePersonDeparted')
                        );
                    }
                };
            })();
        """.trimIndent()

        binding.contentWebView.evaluateJavascript(
            script,
            null
        )
    }

    private fun pauseWebContent() {
        binding.contentWebView.evaluateJavascript(
            """
            if (window.ECAEWelcomeSystem) {
                window.ECAEWelcomeSystem.pauseContent();
            } else {
                document.querySelectorAll('video,audio').forEach(
                    function(media) {
                        media.pause();
                    }
                );
            }
            """.trimIndent(),
            null
        )
    }

    private fun resumeWebContent() {
        binding.contentWebView.evaluateJavascript(
            """
            if (window.ECAEWelcomeSystem) {
                window.ECAEWelcomeSystem.resumeContent();
            }
            """.trimIndent(),
            null
        )
    }

    private fun checkCameraPermission() {
        val permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            startFrontCamera()
        } else {
            permissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun startFrontCamera() {
        val providerFuture =
            ProcessCameraProvider.getInstance(this)

        providerFuture.addListener(
            {
                try {
                    val provider =
                        providerFuture.get()

                    cameraProvider = provider

                    val hasFrontCamera =
                        provider.hasCamera(
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        )

                    if (!hasFrontCamera) {
                        showPersistentStatus(
                            getString(
                                R.string.status_no_camera
                            )
                        )

                        return@addListener
                    }

                    val analysis =
                        ImageAnalysis.Builder()
                            .setTargetResolution(
                                android.util.Size(
                                    640,
                                    480
                                )
                            )
                            .setBackpressureStrategy(
                                ImageAnalysis
                                    .STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()

                    analysis.setAnalyzer(
                        cameraExecutor
                    ) { imageProxy ->

                        analyzeImage(imageProxy)
                    }

                    provider.unbindAll()

                    provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        analysis
                    )

                    showTemporaryStatus(
                        getString(
                            R.string.status_ready
                        )
                    )

                } catch (exception: Exception) {
                    exception.printStackTrace()

                    showPersistentStatus(
                        getString(
                            R.string.status_camera_error
                        )
                    )
                }
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(
        imageProxy: ImageProxy
    ) {
        val currentTime =
            System.currentTimeMillis()

        if (
            currentTime - lastAnalysisTime
            < ANALYSIS_INTERVAL_MS
        ) {
            imageProxy.close()
            return
        }

        lastAnalysisTime = currentTime

        if (
            !analysisInProgress.compareAndSet(
                false,
                true
            )
        ) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            analysisInProgress.set(false)
            imageProxy.close()
            return
        }

        val inputImage =
            InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->

                val detectionTime =
                    System.currentTimeMillis()

                if (faces.isNotEmpty()) {
                    handleFaceDetected(detectionTime)
                } else {
                    handleNoFaceDetected(detectionTime)
                }
            }
            .addOnFailureListener {
                consecutiveDetections = 0
            }
            .addOnCompleteListener {
                analysisInProgress.set(false)
                imageProxy.close()
            }
    }

    private fun handleFaceDetected(
        detectionTime: Long
    ) {
        lastDetectionTime = detectionTime

        consecutiveDetections++

        if (
            !personPresent &&
            consecutiveDetections >= DETECTIONS_REQUIRED
        ) {
            personPresent = true

            runOnUiThread {
                pauseWebContent()
                showWelcomeOverlay()
            }
        }
    }

    private fun handleNoFaceDetected(
        detectionTime: Long
    ) {
        consecutiveDetections = 0

        if (!personPresent) {
            return
        }

        val timeWithoutDetection =
            detectionTime - lastDetectionTime

        if (
            timeWithoutDetection >= DEPARTURE_DELAY_MS
        ) {
            personPresent = false

            runOnUiThread {
                hideWelcomeOverlay()
                resumeWebContent()
            }
        }
    }

    private fun showWelcomeOverlay() {
        binding.welcomeOverlay.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(450L)
                .start()
        }
    }

    private fun hideWelcomeOverlay() {
        binding.welcomeOverlay
            .animate()
            .alpha(0f)
            .setDuration(400L)
            .withEndAction {
                binding.welcomeOverlay.visibility =
                    View.GONE
            }
            .start()
    }

    private fun configureKioskMode() {
        try {
            val policyManager =
                getSystemService(
                    Context.DEVICE_POLICY_SERVICE
                ) as DevicePolicyManager

            val adminComponent =
                ComponentName(
                    this,
                    WelcomeDeviceAdminReceiver::class.java
                )

            if (
                policyManager.isDeviceOwnerApp(
                    packageName
                )
            ) {
                policyManager.setLockTaskPackages(
                    adminComponent,
                    arrayOf(packageName)
                )

                policyManager.setLockTaskFeatures(
                    adminComponent,
                    DevicePolicyManager
                        .LOCK_TASK_FEATURE_NONE
                )

                startLockTask()
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun showTemporaryStatus(
        message: String
    ) {
        binding.systemStatus.text = message

        binding.systemStatus.visibility =
            View.VISIBLE

        mainHandler.removeCallbacksAndMessages(
            "hide_status"
        )

        mainHandler.postAtTime(
            {
                binding.systemStatus.visibility =
                    View.GONE
            },
            "hide_status",
            System.currentTimeMillis() +
                STATUS_HIDE_DELAY_MS
        )
    }

    private fun showPersistentStatus(
        message: String
    ) {
        mainHandler.removeCallbacksAndMessages(
            "hide_status"
        )

        binding.systemStatus.text = message

        binding.systemStatus.visibility =
            View.VISIBLE
    }

    private fun enterImmersiveMode() {
        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {
            window.setDecorFitsSystemWindows(false)

            window.insetsController?.let { controller ->

                controller.hide(
                    WindowInsets.Type.statusBars() or
                        WindowInsets.Type.navigationBars()
                )

                controller.systemBarsBehavior =
                    WindowInsetsController
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            enterImmersiveMode()
        }
    }

    override fun onResume() {
        super.onResume()

        binding.contentWebView.onResume()

        enterImmersiveMode()
    }

    override fun onPause() {
        binding.contentWebView.onPause()

        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        /*
         Back is intentionally disabled in kiosk mode.
        */
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)

        cameraProvider?.unbindAll()

        faceDetector.close()

        cameraExecutor.shutdown()

        binding.contentWebView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }

        super.onDestroy()
    }
}
