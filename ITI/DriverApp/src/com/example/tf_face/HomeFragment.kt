package com.example.tf_face.home

import android.Manifest
import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.tf_face.AppDatabase
import com.example.tf_face.UserManagerHelper
import com.example.tf_face.R
import androidx.annotation.RequiresPermission
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.random.Random
import android.content.Intent

class HomeFragment : Fragment() {

    private val VENDOR_EXTENSION_FOG_LIGHT_CONTROL_PROPERTY = 0x0114 + 0x20000000 + 0x01000000 + 0x00400000 // 0x21400114
    private val VENDOR_EXTENSION_HEADLIGHT_LIGHT_CONTROL_PROPERTY = 0x0115 + 0x20000000 + 0x01000000 + 0x00400000 // 0x21400115
    private val VENDOR_EXTENSION_PARKING_LIGHT_CONTROL_PROPERTY = 0x0116 + 0x20000000 + 0x01000000 + 0x00400000 // 0x21400116
    private val VENDOR_EXTENSION_BATTERY_STATUS_PROPERTY = 0x0119 + 0x20000000 + 0x01000000 + 0x00400000 // 0x21400119
    private val VENDOR_EXTENSION_LDR_PROPERTY = 0x011A + 0x20000000 + 0x01000000 + 0x00400000 // 0x2140011A
    private val VENDOR_EXTENSION_SEAT_BELT_PROPERTY = 0x011B + 0x20000000 + 0x01000000 + 0x00400000 // 0x2140011B
    private val VENDOR_EXTENSION_PARKING_BRAKE_PROPERTY = 0x011C + 0x20000000 + 0x01000000 + 0x00400000 // 0x2140011C
    private val areaId = 0

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null

    private lateinit var batteryGauge: ProgressBar
    private lateinit var batteryNeedle: ImageView
    private lateinit var batteryText: TextView
    private lateinit var leftRoad: ImageView
    private lateinit var rightRoad: ImageView
    private lateinit var fogLightIndicator: ImageView
    private lateinit var headlightIndicatorLeft: ImageView
    private lateinit var lightIndicator: ImageView
    private lateinit var parkingLightIndicator: ImageView
    private lateinit var firstRightIndicator: ImageView
    private lateinit var thirdRightIndicator: ImageView
    private lateinit var driverNameText: TextView
    private lateinit var ageText: TextView
    private lateinit var genderText: TextView
    private lateinit var weightText: TextView
    private lateinit var temperatureText: TextView
    private lateinit var fuelBar1: View
    private lateinit var fuelBar2: View
    private lateinit var fuelBar3: View
    private lateinit var fuelBar4: View
    private lateinit var fuelBar5: View
    private lateinit var fuelBar6: View
    private lateinit var gearReady: TextView
    private lateinit var gearP: TextView
    private lateinit var gearR: TextView
    private lateinit var gearN: TextView
    private lateinit var gearD: TextView
    private lateinit var carImage: ImageView

    private var fogLightOn = false
    private var headlightOn = false
    private var parkingLightOn = false
    private var lightOn = false
    private var seatBeltOn = true
    private var parkingBrakeOn = true
    private var updateJob: Job? = null

    private val overlayPackage = "com.example.tf_face.lightmode"
    private val targetPackage = "com.example.tf_face"
    private val userId: String = "current" // Use current user for overlay commands

    private val handler = Handler(Looper.getMainLooper())
    private val pollInterval = 1000L // 1 second

    private val pollRunnable = object : Runnable {
        override fun run() {
            carPropertyManager?.let { manager ->
                // Battery status
                try {
                    val batteryProp = manager.getProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_BATTERY_STATUS_PROPERTY,
                        areaId
                    )
                    if (batteryProp != null) {
                        val rawBattery = batteryProp.value.toInt()
                        val batteryPercent = mapBatteryToPercentage(rawBattery)
                        updateBatteryUI(batteryPercent)
                        Log.d("HomeFragment", "Battery value: $rawBattery → $batteryPercent%")
                    } else {
                        Log.w("HomeFragment", "Battery property is null")
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("HomeFragment", "Property $VENDOR_EXTENSION_BATTERY_STATUS_PROPERTY not supported by VHAL", e)
                } catch (e: SecurityException) {
                    Log.e("HomeFragment", "Permission denied for battery property", e)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Unexpected error for battery property", e)
                }

                // Seat belt status
                try {
                    val seatBeltProp = manager.getProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_SEAT_BELT_PROPERTY,
                        areaId
                    )
                    if (seatBeltProp != null) {
                        seatBeltOn = seatBeltProp.value.toInt() == 1
                        updateSeatBeltUI()
                        Log.d("HomeFragment", "Seat belt: $seatBeltOn")
                    } else {
                        Log.w("HomeFragment", "Seat belt property is null")
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("HomeFragment", "Property $VENDOR_EXTENSION_SEAT_BELT_PROPERTY not supported by VHAL", e)
                } catch (e: SecurityException) {
                    Log.e("HomeFragment", "Permission denied for seat belt property", e)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Unexpected error for seat belt property", e)
                }

                // Parking brake status
                try {
                    val parkingBrakeProp = manager.getProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_PARKING_BRAKE_PROPERTY,
                        areaId
                    )
                    if (parkingBrakeProp != null) {
                        parkingBrakeOn = parkingBrakeProp.value.toInt() == 1
                        updateParkingBrakeUI()
                        Log.d("HomeFragment", "Parking brake: $parkingBrakeOn")
                    } else {
                        Log.w("HomeFragment", "Parking brake property is null")
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("HomeFragment", "Property $VENDOR_EXTENSION_PARKING_BRAKE_PROPERTY not supported by VHAL", e)
                } catch (e: SecurityException) {
                    Log.e("HomeFragment", "Permission denied for parking brake property", e)
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Unexpected error for parking brake property", e)
                }
            }
            handler.postDelayed(this, pollInterval)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views with null checks
        batteryGauge = view.findViewById(R.id.battery_gauge) ?: run {
            Log.e("HomeFragment", "battery_gauge is null")
            return
        }
        batteryNeedle = view.findViewById(R.id.battery_needle) ?: run {
            Log.e("HomeFragment", "battery_needle is null")
            return
        }
        batteryText = view.findViewById(R.id.battery_text) ?: run {
            Log.e("HomeFragment", "battery_text is null")
            return
        }
        leftRoad = view.findViewById(R.id.left_road) ?: run {
            Log.e("HomeFragment", "left_road is null")
            return
        }
        rightRoad = view.findViewById(R.id.right_road) ?: run {
            Log.e("HomeFragment", "right_road is null")
            return
        }
        fogLightIndicator = view.findViewById(R.id.fog_light_indicator) ?: run {
            Log.e("HomeFragment", "fog_light_indicator is null")
            return
        }
        headlightIndicatorLeft = view.findViewById(R.id.headlight_indicator_left) ?: run {
            Log.e("HomeFragment", "headlight_indicator_left is null")
            return
        }
        lightIndicator = view.findViewById(R.id.light_indicator) ?: run {
            Log.e("HomeFragment", "light_indicator is null")
            return
        }
        parkingLightIndicator = view.findViewById(R.id.parking_light_indicator) ?: run {
            Log.e("HomeFragment", "parking_light_indicator is null")
            return
        }
        firstRightIndicator = view.findViewById(R.id.first_right_indicator) ?: run {
            Log.e("HomeFragment", "first_right_indicator is null")
            return
        }
        thirdRightIndicator = view.findViewById(R.id.third_right_indicator) ?: run {
            Log.e("HomeFragment", "third_right_indicator is null")
            return
        }
        driverNameText = view.findViewById(R.id.driver_name_text) ?: run {
            Log.e("HomeFragment", "driver_name_text is null")
            return
        }
        ageText = view.findViewById(R.id.age_text) ?: run {
            Log.e("HomeFragment", "age_text is null")
            return
        }
        genderText = view.findViewById(R.id.gender_text) ?: run {
            Log.e("HomeFragment", "age_text is null")
            return
        }
        weightText = view.findViewById(R.id.weight_text) ?: run {
            Log.e("HomeFragment", "weight_text is null")
            return
        }
        temperatureText = view.findViewById(R.id.temperature_text) ?: run {
            Log.e("HomeFragment", "temperature_text is null")
            return
        }
        fuelBar1 = view.findViewById(R.id.fuel_bar_1) ?: run {
            Log.e("HomeFragment", "fuel_bar_1 is null")
            return
        }
        fuelBar2 = view.findViewById(R.id.fuel_bar_2) ?: run {
            Log.e("HomeFragment", "fuel_bar_2 is null")
            return
        }
        fuelBar3 = view.findViewById(R.id.fuel_bar_3) ?: run {
            Log.e("HomeFragment", "fuel_bar_3 is null")
            return
        }
        fuelBar4 = view.findViewById(R.id.fuel_bar_4) ?: run {
            Log.e("HomeFragment", "fuel_bar_4 is null")
            return
        }
        fuelBar5 = view.findViewById(R.id.fuel_bar_5) ?: run {
            Log.e("HomeFragment", "fuel_bar_5 is null")
            return
        }
        fuelBar6 = view.findViewById(R.id.fuel_bar_6) ?: run {
            Log.e("HomeFragment", "fuel_bar_6 is null")
            return
        }
        gearReady = view.findViewById(R.id.gear_ready) ?: run {
            Log.e("HomeFragment", "gear_ready is null")
            return
        }
        gearP = view.findViewById(R.id.gear_p) ?: run {
            Log.e("HomeFragment", "gear_p is null")
            return
        }
        gearR = view.findViewById(R.id.gear_r) ?: run {
            Log.e("HomeFragment", "gear_r is null")
            return
        }
        gearN = view.findViewById(R.id.gear_n) ?: run {
            Log.e("HomeFragment", "gear_n is null")
            return
        }
        gearD = view.findViewById(R.id.gear_d) ?: run {
            Log.e("HomeFragment", "gear_d is null")
            return
        }
        carImage = view.findViewById(R.id.car_image) ?: run {
            Log.e("HomeFragment", "car_image is null")
            return
        }

        // Log resolved color to verify theme
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        Log.i("HomeFragment", "Resolved text_primary color: #${Integer.toHexString(textPrimaryColor)}")

        // Initialize UserManagerHelper and fetch user data
        val userName = arguments?.getString("user_name") ?: "Guest User"
        val isGuest = userName == "Guest User"
        var userAge = arguments?.getInt("user_age") ?: -1
        var userGender = arguments?.getString("user_gender") ?: ""
        var userWeight = arguments?.getFloat("user_weight") ?: -1f

        if (isGuest) {
            // Randomize for guest users
            userAge = Random.nextInt(25, 60)
            userGender = if (Random.nextBoolean()) "Male" else "Female"
            userWeight = Random.nextInt(50, 100).toFloat()
        } else if (userAge == -1 || userGender.isEmpty() || userWeight == -1f) {
            // Fetch from database if intent data is missing
            context?.let { ctx ->
                val database = AppDatabase.getDatabase(ctx)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val userFaces = withContext(Dispatchers.IO) {
                            database.faceDao()?.getFacesByName(userName)?.firstOrNull()
                        }
                        userAge = userFaces?.age ?: 0
                        userGender = userFaces?.gender ?: "unknown"
                        userWeight = userFaces?.weight ?: 70f
                        // Update UI after fetching
                        updateDriverInfoUI(userName, userAge, userGender, userWeight)
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Failed to fetch user data from database", e)
                    }
                }
            }
        }

        // Set initial user data
        updateDriverInfoUI(userName, userAge, userGender, userWeight)

        // Initialize Car API
        try {
            car = Car.createCar(requireContext(), null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
                if (ready) {
                    carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                    handler.post(pollRunnable)
                } else {
                    Log.e("HomeFragment", "Failed to connect to Car service")
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Car API initialization failed: ${e.message}", e)
        }

        // Click listeners for indicators
        fogLightIndicator.setOnClickListener {
            carPropertyManager?.let { manager ->
                fogLightOn = !fogLightOn
                setVhalProperty(VENDOR_EXTENSION_FOG_LIGHT_CONTROL_PROPERTY, fogLightOn)
                fogLightIndicator.setImageResource(
                    if (fogLightOn) R.drawable.rare_fog_lights_red else R.drawable.rare_fog_lights
                )
                updateCarImage()
            }
        }
        headlightIndicatorLeft.setOnClickListener {
            carPropertyManager?.let { manager ->
                headlightOn = !headlightOn
                setVhalProperty(VENDOR_EXTENSION_HEADLIGHT_LIGHT_CONTROL_PROPERTY, headlightOn)
                headlightIndicatorLeft.setImageResource(
                    if (headlightOn) R.drawable.low_beam_headlights else R.drawable.low_beam_headlights_white
                )
                updateCarImage()
            }
        }
        parkingLightIndicator.setOnClickListener {
            carPropertyManager?.let { manager ->
                parkingLightOn = !parkingLightOn
                setVhalProperty(VENDOR_EXTENSION_PARKING_LIGHT_CONTROL_PROPERTY, parkingLightOn)
                parkingLightIndicator.setImageResource(
                    if (parkingLightOn) R.drawable.parking_lights else R.drawable.parking_lights_white
                )
                updateCarImage()
            }
        }
        lightIndicator.setOnClickListener {
            Log.i("HomeFragment", "Light indicator clicked, toggling to ${!lightOn}")
            lightOn = !lightOn
            setOverlayTheme(lightOn)
            updateCarImage()
        }
        firstRightIndicator.setOnClickListener {
            carPropertyManager?.let { manager ->
                try {
                    val seatBeltProp = manager.getProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_SEAT_BELT_PROPERTY,
                        areaId
                    )
                    if (seatBeltProp != null) {
                        seatBeltOn = seatBeltProp.value.toInt() == 1
                        updateSeatBeltUI()
                        Log.d("HomeFragment", "Seat belt: $seatBeltOn")
                    } else {
                        Log.w("HomeFragment", "Seat belt property is null")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Failed to read seat belt property", e)
                }
            }
        }
        thirdRightIndicator.setOnClickListener {
            carPropertyManager?.let { manager ->
                try {
                    val parkingBrakeProp = manager.getProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_PARKING_BRAKE_PROPERTY,
                        areaId
                    )
                    if (parkingBrakeProp != null) {
                        parkingBrakeOn = parkingBrakeProp.value.toInt() == 1
                        updateParkingBrakeUI()
                        Log.d("HomeFragment", "Parking brake: $parkingBrakeOn")
                    } else {
                        Log.w("HomeFragment", "Parking brake property is null")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Failed to read parking brake property", e)
                }
            }
        }

        // Start VHAL polling if Car API is initialized
        carPropertyManager?.let {
            handler.post(pollRunnable)
        }

        // Start periodic UI updates (fuel, temperature, gear)
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                context?.let { ctx ->
                    // Update fuel bars
                    val fuelLevel = Random.nextInt(0, 7)
                    val fuelBars = listOf(fuelBar1, fuelBar2, fuelBar3, fuelBar4, fuelBar5, fuelBar6)
                    fuelBars.forEachIndexed { index, bar ->
                        bar.setBackgroundColor(
                            ContextCompat.getColor(
                                ctx,
                                if (index < fuelLevel) R.color.fuel_bar_red else R.color.grey
                            )
                        )
                    }
                    // Update temperature
                    temperatureText.text = "${Random.nextFloat() * 20 + 90}.1"
                    // Update gear
                    val gears = listOf(gearP, gearR, gearN, gearD)
                    gears.forEach { it.alpha = 0.2f }
                    val activeGear = gears[Random.nextInt(gears.size)]
                    activeGear.alpha = 1.0f
                    // Randomize user data only for guest users
                    if (isGuest) {
                        userAge = Random.nextInt(25, 60)
                        userGender = if (Random.nextBoolean()) "Male" else "Female"
                        userWeight = Random.nextInt(50, 100).toFloat()
                        updateDriverInfoUI(userName, userAge, userGender, userWeight)
                    }
                }
                delay(3000)
            }
        }
    }

    private fun updateDriverInfoUI(name: String, age: Int, gender: String, weight: Float) {
        driverNameText.text = name
        ageText.text = "$age yrs"
        genderText.text = gender
        weightText.text = "$weight kg"
    }

    private fun mapBatteryToPercentage(rawValue: Int): Int {
        val clamped = rawValue.coerceIn(0, 10000)
        return (clamped / 100.0).toInt().coerceIn(0, 100)
    }

    private fun updateBatteryUI(battery: Int) {
        batteryGauge.progress = battery
        batteryNeedle.rotation = (battery / 100f) * 270f
        batteryText.text = "$battery%"
    }

    private fun updateSeatBeltUI() {
        firstRightIndicator.setImageResource(
            if (seatBeltOn) R.drawable.firstrighticon else R.drawable.firstrighticon_grey
        )
    }

    private fun updateParkingBrakeUI() {
        thirdRightIndicator.setImageResource(
            if (parkingBrakeOn) R.drawable.thirdrighticon else R.drawable.thirdrighticon_red
        )
    }

    private fun setVhalProperty(propertyId: Int, state: Boolean) {
        carPropertyManager?.let { manager ->
            try {
                val value = if (state) 1 else 0
                synchronized(manager) {
                    manager.setProperty(
                        Integer::class.java,
                        propertyId,
                        areaId,
                        Integer(value)
                    )
                    Log.d("HomeFragment", "Set VHAL property $propertyId to $value")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Failed to set VHAL property $propertyId", e)
            }
        }
    }

    private fun updateCarImage() {
        val drawableRes = when {
            fogLightOn && headlightOn && !parkingLightOn -> R.drawable.newcar_fog_headlight
            fogLightOn && !headlightOn && parkingLightOn -> R.drawable.newcar_fog_exteriorlight
            !fogLightOn && headlightOn && parkingLightOn -> R.drawable.newcar_exterior_headlight
            fogLightOn && !headlightOn && !parkingLightOn -> R.drawable.newcar_foglight
            !fogLightOn && headlightOn && !parkingLightOn -> R.drawable.newcar_headlight
            !fogLightOn && !headlightOn && parkingLightOn -> R.drawable.newcar_exteriorlight
            fogLightOn && headlightOn && parkingLightOn -> R.drawable.newcar_fulllight
            else -> R.drawable.newcar_nolight // Default: all off
        }
        try {
            carImage.setImageResource(drawableRes)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to set carImage resource $drawableRes: ${e.message}", e)
        }
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private fun toggleOverlayTheme() {
        setOverlayTheme(!isOverlayEnabled())
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private fun setOverlayTheme(enable: Boolean) {
        try {
            val isOverlayEnabled = isOverlayEnabled()
            Log.i("HomeFragment", "Overlay $overlayPackage current state: enabled=$isOverlayEnabled, setting to enabled=$enable")
            if (isOverlayEnabled != enable) {
                val command = "cmd overlay ${if (enable) "enable" else "disable"} --user $userId $overlayPackage"
                val (success, output) = executeShellCommand(command)
                if (!success) {
                    throw IllegalStateException("Shell command failed: $output")
                }
                Log.i("HomeFragment", "Overlay $overlayPackage set to enabled=$enable")
                lightOn = enable
                updateThemeImage()
                // Recreate activity to apply theme without killing process
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500) // Reduced delay for smoother transition
                    try {
                        requireActivity().recreate()
                        Log.i("HomeFragment", "Activity recreated to apply theme")
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Failed to recreate activity", e)
                    }
                }
            } else {
                Log.i("HomeFragment", "Overlay $overlayPackage already in desired state: enabled=$enable")
            }
        } catch (e: SecurityException) {
            Log.e("HomeFragment", "Permission denied: Ensure app is a system app with KILL_BACKGROUND_PROCESSES permission", e)
        } catch (e: IllegalStateException) {
            Log.e("HomeFragment", "Overlay $overlayPackage not found or invalid: ${e.message}", e)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to set overlay $overlayPackage: ${e.message}", e)
        }
    }

    private fun isOverlayEnabled(): Boolean {
        try {
            val (success, output) = executeShellCommand("cmd overlay list --user $userId")
            if (success) {
                val enabled = output.lines().any { it.contains("[x] $overlayPackage") }
                Log.i("HomeFragment", "Overlay state check: enabled=$enabled, output=$output")
                return enabled
            } else {
                Log.w("HomeFragment", "Failed to check overlay state: $output")
            }
        } catch (e: Exception) {
            Log.w("HomeFragment", "Error checking overlay state: ${e.message}")
        }
        Log.i("HomeFragment", "Fallback to lightOn state: enabled=$lightOn")
        return lightOn
    }

    private fun executeShellCommand(command: String): Pair<Boolean, String> {
        try {
            val process = Runtime.getRuntime().exec(command)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Log.i("HomeFragment", "Executed shell command: $command, output: $output")
                return Pair(true, output)
            } else {
                Log.e("HomeFragment", "Shell command failed: $command, error: $error, exit code: $exitCode")
                return Pair(false, error)
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to execute shell command: ${e.message}", e)
            return Pair(false, e.message ?: "Unknown error")
        }
    }

    @RequiresPermission(Manifest.permission.KILL_BACKGROUND_PROCESSES)
    private fun restartTargetApp(context: Context, targetPackage: String) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.killBackgroundProcesses(targetPackage)
            Log.i("HomeFragment", "Force-stopped $targetPackage")
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                ContextCompat.startActivity(context, intent, null)
                Log.i("HomeFragment", "Relaunched $targetPackage")
            } else {
                Log.w("HomeFragment", "No launch intent found for $targetPackage")
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to restart $targetPackage: ${e.message}", e)
            throw e // Rethrow to catch in coroutine
        }
    }

    private fun updateThemeImage() {
        try {
            val isOverlayEnabled = isOverlayEnabled()
            lightIndicator.setImageResource(
                if (isOverlayEnabled) R.drawable.lights else R.drawable.light_white
            )
            Log.i("HomeFragment", "Updated theme image: enabled=$isOverlayEnabled")
        } catch (e: Exception) {
            Log.e("HomeFragment", "Failed to update theme image: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        updateThemeImage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(pollRunnable)
        updateJob?.cancel()
        updateJob = null
        try {
            car?.disconnect()
        } catch (e: Exception) {
            Log.e("HomeFragment", "Car disconnect failed", e)
        }
    }
}