package com.example.tf_face

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import android.content.pm.UserInfo
import android.view.View
import android.widget.ImageButton

class RecognitionActivity : AppCompatActivity() {
    private var faceDetector: BlazeFaceDetector? = null
    private var database: AppDatabase? = null
    private var cameraHelper: CameraHelper? = null
    private var textureView: TextureView? = null
    private var greetingText: TextView? = null
    private val isProcessing = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val CAMERA_PERMISSION_REQUEST_CODE = 102
    private var userManagerHelper: UserManagerHelper? = null

    private val recognitionBuffer = mutableListOf<String>()
    private var noFaceCounter = 0
    private val REQUIRED_CONSECUTIVE_MATCHES = 3
    private val MAX_NO_FACE_FRAMES = 10
    private var isDialogShowing = false
    private var shouldPauseDetection = false
    private var isActivityFinishing = false

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
        
        try {
            setContentView(R.layout.activity_recognition)
        } catch (e: Exception) {
            Log.e("RecognitionActivity", "Failed to set content view", e)
            finish()
            return
        }

        // Initialize views
        textureView = findViewById(R.id.cameraTextureView)
        textureView?.alpha = 0f
        greetingText = findViewById(R.id.greetingTextView)

        if (textureView == null || greetingText == null) {
            Log.e("RecognitionActivity", "Failed to initialize views")
            greetingText?.text = "UI initialization error"
            finish()
            return
        }

        faceDetector = BlazeFaceDetector(this)
        database = AppDatabase.getDatabase(this)
        cameraHelper = CameraHelper(this, textureView!!)
        userManagerHelper = UserManagerHelper(this)

        Log.d("RecognitionActivity", "Activity created, checking camera permission")

        // Check database contents
        CoroutineScope(Dispatchers.Main).launch {
            val faces = database?.faceDao()?.getAllFaces() ?: emptyList()
            Log.d("RecognitionActivity", "Database contains ${faces.size} faces")
            if (faces.isEmpty()) {
                Log.e("RecognitionActivity", "No users in database")
                greetingText?.text = "No users in database. Please register a new user."
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            startCameraAndRecognition()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("RecognitionActivity", "Camera permission granted")
            startCameraAndRecognition()
        } else {
            Log.w("RecognitionActivity", "Camera permission denied")
            greetingText?.text = "Camera permission denied. Please grant permission to use face recognition."
        }
    }

    private fun startCameraAndRecognition() {
        Log.d("RecognitionActivity", "Starting camera and recognition")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                cameraHelper?.startCamera()
                Log.d("RecognitionActivity", "Camera started, beginning detection loop")
                startDetectionLoop()
            } catch (e: Exception) {
                Log.e("RecognitionActivity", "Failed to start camera", e)
                greetingText?.text = "Error starting camera: ${e.message}"
            }
        }
    }

    private fun startDetectionLoop() {
        coroutineScope.launch {
            Log.d("RecognitionActivity", "Detection loop started")
            while (isActive && !isActivityFinishing) {
                if (shouldPauseDetection || isDialogShowing) {
                    delay(100)
                    continue
                }

                if (isProcessing.compareAndSet(false, true)) {
                    try {
                        val bitmap = textureView?.bitmap
                        if (bitmap == null) {
                            Log.w("RecognitionActivity", "Bitmap is null, skipping frame")
                            withContext(Dispatchers.Main) {
                                greetingText?.text = "Camera not ready"
                            }
                            continue
                        }

                        Log.d("RecognitionActivity", "Processing frame: ${bitmap.width}x${bitmap.height}")
                        val faces = faceDetector?.detect(bitmap) ?: emptyList()
                        Log.d("RecognitionActivity", "Detected ${faces.size} faces")

                        withContext(Dispatchers.Main) {
                            if (faces.isEmpty()) {
                                noFaceCounter++
                                if (noFaceCounter >= MAX_NO_FACE_FRAMES) {
                                    recognitionBuffer.clear()
                                    shouldPauseDetection = true
                                    showNoFaceDetectedDialog {
                                        shouldPauseDetection = false
                                        noFaceCounter = 0
                                        greetingText?.text = "Looking for face..."
                                    }
                                } else {
                                    greetingText?.text = "Align your face (${MAX_NO_FACE_FRAMES - noFaceCounter})"
                                }
                                return@withContext
                            } else {
                                noFaceCounter = 0
                                greetingText?.text = "Face detected - processing..."
                            }

                            val largestFace = faces.maxByOrNull { it.width() * it.height() }
                            if (largestFace != null) {
                                val recognition = faceDetector?.recognizeFace(bitmap, database!!, threshold = 0.7f)

                                if (recognition != null) {
                                    val recognizedName = recognition.first.toString()
                                    recognitionBuffer.add(recognizedName)

                                    if (recognitionBuffer.size > REQUIRED_CONSECUTIVE_MATCHES) {
                                        recognitionBuffer.removeAt(0)
                                    }

                                    if (recognitionBuffer.size >= REQUIRED_CONSECUTIVE_MATCHES) {
                                        val allSame = recognitionBuffer.distinct().size == 1
                                        if (allSame) {
                                            isDialogShowing = true
                                            suspendUntilDialogClosed {
                                                showKnownUserDialog(recognizedName, it)
                                                isDialogShowing = false
                                            }
                                            recognitionBuffer.clear()
                                        }
                                    }
                                } else {
                                    if (recognitionBuffer.size >= REQUIRED_CONSECUTIVE_MATCHES &&
                                        recognitionBuffer.all { it == "UNKNOWN" }) {
                                        isDialogShowing = true
                                        suspendUntilDialogClosed {
                                            showUnknownUserDialog {
                                                isDialogShowing = false
                                                it()
                                            }
                                        }
                                        recognitionBuffer.clear()
                                    } else {
                                        recognitionBuffer.add("UNKNOWN")
                                        if (recognitionBuffer.size > REQUIRED_CONSECUTIVE_MATCHES) {
                                            recognitionBuffer.removeAt(0)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RecognitionActivity", "Frame processing error", e)
                        withContext(Dispatchers.Main) {
                            greetingText?.text = "Error: ${e.message?.take(20)}..."
                        }
                        recognitionBuffer.clear()
                    } finally {
                        isProcessing.set(false)
                    }
                }
                delay(100)
            }
        }
    }

    private suspend fun suspendUntilDialogClosed(
        buildDialog: (onDismiss: () -> Unit) -> Unit
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        buildDialog {
            if (continuation.isActive) continuation.resume(Unit) {}
        }
    }

    private fun showNoFaceDetectedDialog(onDismiss: () -> Unit) {
        isDialogShowing = true
        AlertDialog.Builder(this)
            .setTitle("No Face Detected")
            .setMessage("Can't detect any face. Please:\n\n• Center your face\n• Ensure good lighting\n• Remove obstructions")
            .setPositiveButton("Retry") { _, _ ->
                onDismiss()
                isDialogShowing = false
            }
            .setNegativeButton("Cancel") { _, _ ->
                onDismiss()
                isDialogShowing = false
            }
            .setOnDismissListener {
                onDismiss()
                isDialogShowing = false
            }
            .setCancelable(false)
            .show()
    }

    private fun showKnownUserDialog(name: String, onDismiss: () -> Unit) {
        greetingText?.text = "Welcome, $name!"
        AlertDialog.Builder(this)
            .setTitle("Known User")
            .setMessage("Welcome back, $name!")
            .setPositiveButton("OK") { _, _ ->
                coroutineScope.launch {
                    try {
                        // Fetch user data from the database
                        val faceDao = database?.faceDao()
                        val userFace = faceDao?.getFaceByName(name)
                        if (userFace == null) {
                            Log.e("RecognitionActivity", "No FaceEntity found for $name")
                            withContext(Dispatchers.Main) {
                                greetingText?.text = "User data not found"
                                onDismiss()
                            }
                            return@launch
                        }

                        // Get user list from UserManager
                        val userList = userManagerHelper?.listUsers() ?: emptyList()
                        var newId = userList.find { it.name == name }?.id ?: -1

                        // If user not found in UserManager, create a new one
                        if (newId == -1) {
                            Log.w("RecognitionActivity", "User $name not found in UserManager, creating new user")
                            val newUser = userManagerHelper?.createNewUser(name, isGuest = false)
                            newId = newUser?.id ?: -1
                            if (newId == -1) {
                                Log.e("RecognitionActivity", "Failed to create new user for $name")
                                withContext(Dispatchers.Main) {
                                    greetingText?.text = "Failed to create user"
                                    onDismiss()
                                }
                                return@launch
                            }
                        }

                        // Switch user if different from current
                        val currentUserId = userManagerHelper?.getCurrentUserId() ?: 0
                        if (newId != currentUserId) {
                            val switched = userManagerHelper?.switchUser(newId) ?: false
                            if (!switched) {
                                Log.e("RecognitionActivity", "Failed to switch to user ID: $newId")
                                withContext(Dispatchers.Main) {
                                    greetingText?.text = "Failed to switch user"
                                    onDismiss()
                                }
                                return@launch
                            }
                            Log.d("RecognitionActivity", "Switched to user ID: $newId")
                        } else {
                            Log.d("RecognitionActivity", "User $name is already the current user")
                        }

                        // Launch GreetingActivity with all user data and FINISH this activity
                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@RecognitionActivity, GreetingActivity::class.java).apply {
                                putExtra("user_name", name)
                                putExtra("user_age", userFace.age)
                                putExtra("user_gender", userFace.gender)
                                putExtra("user_weight", userFace.weight)
                                putExtra("is_guest", false) // Explicitly mark as not guest
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            isActivityFinishing = true
                            startActivity(intent)
                            finish() // CRITICAL: Finish this activity to prevent re-opening
                        }
                    } catch (e: Exception) {
                        Log.e("RecognitionActivity", "Error processing user $name", e)
                        withContext(Dispatchers.Main) {
                            greetingText?.text = "Error: ${e.message?.take(20)}..."
                            onDismiss()
                        }
                    }
                }
            }
            .setNegativeButton("NO") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showUnknownUserDialog(onDismiss: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Unknown User")
            .setMessage("You are recognized as unknown. What would you like to do?")
            .setPositiveButton("Register") { _, _ ->
                // Start NewUserActivity and finish this one
                isActivityFinishing = true
                startActivity(Intent(this, NewUserActivity::class.java))
                finish() // CRITICAL: Finish this activity
            }
            .setNegativeButton("Continue as Guest") { _, _ ->
                // Continue as guest and finish this activity
                val guestUserId = userManagerHelper?.getGuestUserId() ?: -1
                if (guestUserId != -1) {
                    userManagerHelper?.switchUser(guestUserId)
                } else {
                    val newGuestUser = userManagerHelper?.createNewUser("Guest User", isGuest = true)
                    if (newGuestUser != null) {
                        userManagerHelper?.switchUser(newGuestUser.id)
                    }
                }
                
                // Launch GreetingActivity as guest and finish this activity
                val intent = Intent(this, GreetingActivity::class.java).apply {
                    putExtra("user_name", "Guest User")
                    putExtra("is_guest", true) // Explicitly mark as guest
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                isActivityFinishing = true
                startActivity(intent)
                finish() 
            }
            .setNeutralButton("Retry") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            .setOnCancelListener {
                onDismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RecognitionActivity", "Activity destroyed")
        isActivityFinishing = true
        coroutineScope.cancel()
        faceDetector?.close()
    }
}