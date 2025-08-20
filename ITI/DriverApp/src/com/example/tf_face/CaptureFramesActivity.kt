package com.example.tf_face

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.content.pm.UserInfo
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel

class CaptureFramesActivity : AppCompatActivity() {
    private var cameraHelper: CameraHelper? = null
    private var textureView: TextureView? = null
    private var captureButton: Button? = null
    private var statusText: TextView? = null
    private var databaseInitializer: DatabaseInitializer? = null
    private var faceDetector: BlazeFaceDetector? = null
    private var currentFrame = 0
    private val instructions = listOf(
        "Position 1: Face in the middle",
        "Position 2: Face tilted right",
        "Position 3: Face tilted left",
        "Position 4: Face tilted up",
        "Position 5: Face tilted down"
    )
    private var faceOverlay: ImageView? = null
    private var faceOverlayView: FaceOverlayView? = null
    private val overlayImages = listOf(
        R.drawable.center,
        R.drawable.right,
        R.drawable.left,
        R.drawable.up,
        R.drawable.down
    )
    private val CAMERA_PERMISSION_REQUEST_CODE = 101
    private val detectionScope = CoroutineScope(Dispatchers.Default)
    private var detectionJob: Job? = null
    private var isDetectionActive = true

    private var userManagerHelper: UserManagerHelper? = null
    private var userInfo: UserInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_capture_frames)

        // Initialize views
        textureView = findViewById(R.id.cameraTextureView)
        captureButton = findViewById(R.id.btnCapturePosition)
        statusText = findViewById(R.id.statusTextView)
        faceOverlayView = findViewById(R.id.faceOverlayView)
        faceOverlay = findViewById(R.id.faceOverlayImageView)

        if (textureView == null || captureButton == null || statusText == null) {
            Log.e("CaptureFramesActivity", "Failed to initialize views")
            finish()
            return
        }

        databaseInitializer = DatabaseInitializer(this)
        faceDetector = BlazeFaceDetector(this)
        userManagerHelper = UserManagerHelper(this)

        val userName = intent.getStringExtra("user_name") ?: "user_${System.currentTimeMillis()}"
        val userAge = intent.getIntExtra("user_age", 0)
        val userGender = intent.getStringExtra("user_gender") ?: "unknown"
        val userWeight = intent.getFloatExtra("user_weight", 70f)

        // Initialize camera
        cameraHelper = CameraHelper(this, textureView!!)

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            initialize()
        }

        // Set initial instruction
        statusText?.text = instructions[currentFrame]
        faceOverlay?.setImageResource(overlayImages[currentFrame])

        // Prevent multiple rapid clicks
        captureButton?.setOnClickListener {
            it.isEnabled = false
            captureFrame(userName, userAge, userGender, userWeight)
            // Re-enable after a short delay to prevent rapid clicks
            it.postDelayed({ it.isEnabled = true }, 1000)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initialize()
        } else {
            Log.e("CaptureFramesActivity", "Camera permission denied")
            finish()
        }
    }

    private fun initialize() {
        CoroutineScope(Dispatchers.Main).launch {
            cameraHelper?.startCamera()
            startFaceDetectionLoop()
            showNextInstruction()
        }
    }

    private fun startFaceDetectionLoop() {
        detectionJob = detectionScope.launch {
            while (isDetectionActive) {
                val bitmap = textureView?.bitmap
                if (bitmap != null) {
                    val faces = faceDetector?.detect(bitmap) ?: emptyList()
                    withContext(Dispatchers.Main) {
                        faceOverlayView?.setFaces(faces, bitmap.width, bitmap.height)
                    }
                }
                delay(100)
            }
        }
    }

    private fun showNextInstruction() {
        if (currentFrame < instructions.size) {
            statusText?.text = instructions[currentFrame]
            faceOverlay?.setImageResource(overlayImages[currentFrame])
        }
    }

    private fun captureFrame(userName: String, userAge: Int, userGender: String, userWeight: Float) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = textureView?.bitmap ?: run {
                    Log.w("CaptureFramesActivity", "Bitmap is null")
                    statusText?.text = "Failed to capture frame"
                    return@launch
                }
                val faces = faceDetector?.detect(bitmap) ?: emptyList()
                if (faces.isEmpty()) {
                    Log.w("CaptureFramesActivity", "No faces detected")
                    statusText?.text = "No face detected. Please try again."
                    return@launch
                }

                val largestFace = faces.maxByOrNull { it.width() * it.height() } ?: return@launch
                val croppedFace = faceDetector?.cropFace(bitmap, largestFace) ?: return@launch
                val imageName = "${userName}_${currentFrame + 1}.jpeg"

                val height = 170f // Default height or get from intent if needed

                val success = databaseInitializer?.addFace(
                    userName,
                    croppedFace,
                    imageName,
                    userWeight,
                    height,
                    userGender,
                    userAge
                ) ?: false

                if (success) {
                    databaseInitializer?.printAllFaces()

                    currentFrame++
                    Log.d("CaptureFramesActivity", "Captured frame $currentFrame for $userName")
                    statusText?.text = "Captured frame $currentFrame for $userName"
                    if (currentFrame >= 5) {
                        userInfo = userManagerHelper?.createNewUser(userName)
                        if (userInfo == null) {
                            Log.e("CaptureFramesActivity", "Failed to create user")
                            statusText?.text = "Failed to create user. Please try again."
                            return@launch
                        } else {
                            val user_id = userInfo?.id ?: 0
                            val switchSuccess = userManagerHelper?.switchUser(user_id) ?: false
                            if (switchSuccess) {
                                Log.d("CaptureFramesActivity", "Switched to user ID: ${userInfo?.id}")
                                
                                // Redirect to RecognitionActivity instead of GreetingActivity
                                val intent = Intent(this@CaptureFramesActivity, RecognitionActivity::class.java).apply {
                                    putExtra("user_name", userName)
                                    putExtra("user_age", userAge)
                                    putExtra("user_gender", userGender)
                                    putExtra("user_weight", userWeight)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                Log.e("CaptureFramesActivity", "Failed to switch to user ID: $user_id")
                                statusText?.text = "Failed to switch user. Please try again."
                            }
                        }
                    } else {
                        showNextInstruction()
                    }
                } else {
                    Log.w("CaptureFramesActivity", "Failed to capture frame $currentFrame")
                    statusText?.text = "Failed to capture frame $currentFrame for $userName"
                }
            } catch (e: Exception) {
                Log.e("CaptureFramesActivity", "Error capturing frame", e)
                statusText?.text = "Error capturing frame: ${e.message}"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isDetectionActive = false
        detectionJob?.cancel()
        detectionScope.cancel()
        faceDetector?.close()
    }
}