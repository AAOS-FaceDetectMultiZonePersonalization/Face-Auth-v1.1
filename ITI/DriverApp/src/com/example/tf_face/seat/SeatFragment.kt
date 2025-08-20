package com.example.tf_face.seat

import android.Manifest
import android.annotation.SuppressLint
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import com.example.tf_face.AppDatabase
import com.example.tf_face.DatabaseInitializer
import com.example.tf_face.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


@SuppressLint("MissingPermission")
class SeatFragment : Fragment() {

private lateinit var databaseInitializer: DatabaseInitializer
    private var userName: String = "Guest User"

    private val VENDOR_EXTENSION_SEAT_BACK_CONTROL_PROPERTY = 0x0118 + 0x20000000 + 0x01000000 + 0x00400000 // 0x21400118
    private val VENDOR_EXTENSION_SEAT_BASE_CONTROL_PROPERTY = 0x0117 + 0x20000000 + 0x01000000 + 0x00400000 // 0x21400117
    private val areaId = 0
    private val PREFS_NAME = "DriverAppPrefs"
    private val DRIVER_PREFS_NAME = "DriverPrefs"
    private val KEY_BACKREST_ANGLE = "seat_backrest_angle"
    private val KEY_BASE_POSITION = "seat_base_position"
    private val KEY_DEFAULT_BACKREST_ANGLE = "default_seat_backrest_angle"
    private var DEFAULT_BACKREST_ANGLE = 90f // Will be calculated based on age and weight
    private val DEFAULT_BASE_POSITION = 0f

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private lateinit var seatController: SeatController
    private lateinit var backrestImage: ImageView
    private lateinit var baseImage: ImageView
    private lateinit var angleDisplay: TextView
    private lateinit var point1: ImageView
    private lateinit var point2: ImageView
    private lateinit var point3: ImageView

    // Buttons
    private lateinit var btnUp: ImageButton
    private lateinit var btnDown: ImageButton
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btn0: ImageButton
    private lateinit var btn2: ImageButton
    private lateinit var btn3: ImageButton
    private lateinit var btnReset: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get user name from arguments
        userName = arguments?.getString("user_name") ?: "Guest User"
        databaseInitializer = DatabaseInitializer(requireContext())


        // Initialize Car API
        try {
            car = Car.createCar(requireContext().applicationContext, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
                if (ready) {
                    carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                } else {
                    Log.e("SeatFragment", "Failed to connect to Car service")
                }
            }
        } catch (e: Exception) {
            Log.e("SeatFragment", "Car API initialization failed: ${e.message}", e)
            return
        }

        seatController = SeatController(requireContext())
        initViews(view)
        // Calculate default backrest angle using driver data
        calculateDefaultBackrestAngle()
        setupButtonListeners()

        // Load saved settings
        loadSeatSettings()
    }

    private fun calculateDefaultBackrestAngle() {
        // Get driver data from arguments or database
        val userName = arguments?.getString("user_name") ?: "Guest User"
        val isGuest = userName == "Guest User"
        var userAge = arguments?.getInt("user_age") ?: -1
        var userWeight = arguments?.getFloat("user_weight") ?: -1f

        if (!isGuest && (userAge == -1 || userWeight == -1f)) {
            // Fetch from database if intent data is missing
            context?.let { ctx ->
                val database = AppDatabase.getDatabase(ctx)
                CoroutineScope(Dispatchers.Main).launch {
                    val userFaces = withContext(Dispatchers.IO) {
                        database.faceDao()?.getFacesByName(userName)?.firstOrNull()
                    }
                    userAge = userFaces?.age ?: 35
                    userWeight = userFaces?.weight ?: 75f
                    // Calculate angle after fetching
                    computeDefaultAngle(userAge, userWeight)
                }
            }
        } else {
            // Use default values for guest or if data is available
            userAge = if (userAge == -1) 35 else userAge
            userWeight = if (userWeight == -1f) 75f else userWeight
            computeDefaultAngle(userAge, userWeight)
        }
    }

    private fun computeDefaultAngle(age: Int, weight: Float) {
        // Calculate recline degrees based on age and weight (arbitrary formula)
        val reclineDegrees = 0.2f * age + 0.1f * weight
        DEFAULT_BACKREST_ANGLE = (90f - reclineDegrees).coerceIn(seatController.minAngle, seatController.maxAngle)
        Log.d("SeatFragment", "Calculated default backrest angle: $DEFAULT_BACKREST_ANGLE based on age $age and weight $weight")

        // Store the calculated default angle in SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putFloat(KEY_DEFAULT_BACKREST_ANGLE, DEFAULT_BACKREST_ANGLE)
            apply()
        }
        Log.d("SeatFragment", "Stored default backrest angle: $DEFAULT_BACKREST_ANGLE in SharedPreferences")
    }

    private fun initViews(view: View) {
        backrestImage = view.findViewById(R.id.imageView) ?: run {
            Log.e("SeatFragment", "backrestImage is null")
            return
        }
        baseImage = view.findViewById(R.id.imageView2) ?: run {
            Log.e("SeatFragment", "baseImage is null")
            return
        }
        angleDisplay = view.findViewById(R.id.angleDisplay) ?: run {
            Log.e("SeatFragment", "angleDisplay is null")
            return
        }
        point1 = view.findViewById(R.id.imageView4) ?: run {
            Log.e("SeatFragment", "point1 is null")
            return
        }
        point2 = view.findViewById(R.id.imageView5) ?: run {
            Log.e("SeatFragment", "point2 is null")
            return
        }
        point3 = view.findViewById(R.id.imageView6) ?: run {
            Log.e("SeatFragment", "point3 is null")
            return
        }
        btnUp = view.findViewById(R.id.btnUp) ?: run {
            Log.e("SeatFragment", "btnUp is null")
            return
        }
        btnDown = view.findViewById(R.id.btnDown) ?: run {
            Log.e("SeatFragment", "btnDown is null")
            return
        }
        btnLeft = view.findViewById(R.id.btnLeft) ?: run {
            Log.e("SeatFragment", "btnLeft is null")
            return
        }
        btnRight = view.findViewById(R.id.btnRight) ?: run {
            Log.e("SeatFragment", "btnRight is null")
            return
        }
        btn0 = view.findViewById(R.id.btn0) ?: run {
            Log.e("SeatFragment", "btn0 is null")
            return
        }
        btn2 = view.findViewById(R.id.btn2) ?: run {
            Log.e("SeatFragment", "btn2 is null")
            return
        }
        btn3 = view.findViewById(R.id.btn3) ?: run {
            Log.e("SeatFragment", "btn3 is null")
            return
        }
        btnReset = view.findViewById(R.id.btnReset) ?: run {
            Log.e("SeatFragment", "btnReset is null")
            return
        }

        backrestImage.post {
            backrestImage.pivotX = backrestImage.width * 0.2f
            backrestImage.pivotY = backrestImage.height * 0.8f
        }
    }

    private fun loadSeatSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val angle = prefs.getFloat(KEY_BACKREST_ANGLE, DEFAULT_BACKREST_ANGLE)
        val position = prefs.getFloat(KEY_BASE_POSITION, DEFAULT_BASE_POSITION)

        // Update SeatController state
        seatController.adjustAngle(angle - seatController.getCurrentAngle())
        seatController.moveForward() // Temporary move to set position
        seatController.moveBackward() // Reset to ensure correct offset
        while (seatController.getCurrentHorizontalOffset() < position) {
            seatController.moveForward()
        }
        while (seatController.getCurrentHorizontalOffset() > position) {
            seatController.moveBackward()
        }

        // Apply to UI and VHAL
        animateBackrestAngle(angle)
        animateSeatPosition(position)
        updateAngleDisplay(angle)
        updateActivePoint(angle)
        setVhalProperty(VENDOR_EXTENSION_SEAT_BACK_CONTROL_PROPERTY, angle.roundToInt())
        setVhalProperty(VENDOR_EXTENSION_SEAT_BASE_CONTROL_PROPERTY, position.roundToInt())
    }

   private fun saveSeatSettings() {
        val angle = seatController.getCurrentAngle()
        val position = seatController.getCurrentHorizontalOffset()

        // Save to SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_BACKREST_ANGLE, angle)
            putFloat(KEY_BASE_POSITION, position)
            commit() 
        }

        // Save to database if not guest
        if (userName != "Guest User") {
            CoroutineScope(Dispatchers.Main).launch {
                databaseInitializer.saveSeatSettingsToDatabase(userName, angle, position)
            }
        }

        Log.d("SeatFragment", "Saved seat settings: angle=$angle, position=$position")
    }


    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun setupButtonListeners() {
        btnUp.setOnClickListener {
            carPropertyManager?.let { manager ->
                val newAngle = seatController.adjustAngle(seatController.angleStep)
                setVhalProperty(VENDOR_EXTENSION_SEAT_BACK_CONTROL_PROPERTY, newAngle.roundToInt())
                animateBackrestAngle(newAngle)
                saveSeatSettings()
                seatController.vibrate(10)
                Log.d("SeatFragment", "Set seat back angle to $newAngle°")
            }
        }

        btnDown.setOnClickListener {
            carPropertyManager?.let { manager ->
                val newAngle = seatController.adjustAngle(-seatController.angleStep)
                setVhalProperty(VENDOR_EXTENSION_SEAT_BACK_CONTROL_PROPERTY, newAngle.roundToInt())
                animateBackrestAngle(newAngle)
                saveSeatSettings()
                seatController.vibrate(10)
                Log.d("SeatFragment", "Set seat back angle to $newAngle°")
            }
        }

        btnRight.setOnClickListener {
            carPropertyManager?.let { manager ->
                val newOffset = seatController.moveForward()
                setVhalProperty(VENDOR_EXTENSION_SEAT_BASE_CONTROL_PROPERTY, newOffset.roundToInt())
                animateSeatPosition(newOffset)
                saveSeatSettings()
                seatController.vibrate(10)
                Log.d("SeatFragment", "Set seat base position to $newOffset")
            }
        }

        btnLeft.setOnClickListener {
            carPropertyManager?.let { manager ->
                val newOffset = seatController.moveBackward()
                setVhalProperty(VENDOR_EXTENSION_SEAT_BASE_CONTROL_PROPERTY, newOffset.roundToInt())
                animateSeatPosition(newOffset)
                saveSeatSettings()
                seatController.vibrate(10)
                Log.d("SeatFragment", "Set seat base position to $newOffset")
            }
        }

        btn0.setOnClickListener { savePreset(0) }
        btn2.setOnClickListener { savePreset(2) }
        btn3.setOnClickListener { savePreset(3) }
        btnReset.setOnClickListener { resetSeatPosition() }
    }

    private fun setVhalProperty(propertyId: Int, value: Int) {
        carPropertyManager?.let { manager ->
            try {
                synchronized(manager) {
                    manager.setProperty(
                        Integer::class.java,
                        propertyId,
                        areaId,
                        Integer(value)
                    )
                    Log.d("SeatFragment", "Set VHAL property $propertyId to $value")
                }
            } catch (e: Exception) {
                Log.e("SeatFragment", "Failed to set VHAL property $propertyId", e)
            }
        }
    }

    private fun savePreset(index: Int) {
        seatController.savePreset(index)
        val preset = seatController.getPreset(index)
        Log.d(
            "SeatFragment",
            "Saved preset $index - Angle: ${preset?.angle}°, Position: ${preset?.position}"
        )
    }

    private fun animateSeatPosition(targetOffset: Float) {
        seatController.createAnimator(baseImage.translationX, targetOffset).apply {
            addUpdateListener { anim ->
                val value = anim.animatedValue as Float
                baseImage.translationX = value
                backrestImage.translationX = value
            }
            start()
        }
    }

    private fun animateBackrestAngle(targetAngle: Float) {
        seatController.createAnimator(backrestImage.rotation, 90f - targetAngle).apply {
            addUpdateListener { anim ->
                backrestImage.rotation = anim.animatedValue as Float
                updateAngleDisplay(targetAngle)
                updateActivePoint(targetAngle)
            }
            start()
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun setPresetPosition(angle: Float) {
        seatController.resetPosition()
        seatController.adjustAngle(angle - seatController.getCurrentAngle())
        animateSeatPosition(seatController.getCurrentHorizontalOffset())
        animateBackrestAngle(angle)
        saveSeatSettings()
        updateAngleDisplay(angle)
        updateActivePoint(angle)
        seatController.vibrate(20)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun resetSeatPosition() {
        setPresetPosition(DEFAULT_BACKREST_ANGLE)
    }

    private fun updateAngleDisplay(angle: Float) {
        val roundedAngle = angle.roundToInt()
        angleDisplay.text = "$roundedAngle°"
    }

    private fun updateActivePoint(angle: Float) {
        point1.setImageResource(if (angle >= 90) R.drawable.arch_point_active else R.drawable.arch_point)
        point2.setImageResource(if (angle in 81f..89f) R.drawable.arch_point_active_blue else R.drawable.arch_point)
        point3.setImageResource(if (angle < 80) R.drawable.arch_point_active_green else R.drawable.arch_point)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            car?.disconnect()
        } catch (e: Exception) {
            Log.e("SeatFragment", "Car disconnect failed", e)
        }
    }
}
