package com.example.tf_face.ac

import android.car.Car
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.tf_face.DatabaseInitializer
import com.example.tf_face.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AcFragment : Fragment() {
    private lateinit var databaseInitializer: DatabaseInitializer
    private var userName: String = "Guest User"

    private val VENDOR_EXTENSION_FAN_SPEED_CONTROL_PROPERTY = 0x0111 + 0x20000000 + 0x01000000 + 0x00400000
    private val VENDOR_EXTENSION_TEMP_CONTROL_PROPERTY = 0x0112 + 0x20000000 + 0x01000000 + 0x00400000
    private val VENDOR_EXTENSION_SEAT_TEMP_CONTROL_PROPERTY = 0x0113 + 0x20000000 + 0x01000000 + 0x00400000
    private val areaId = 0
    private val PREFS_NAME = "DriverAppPrefs"
    private val KEY_FAN_SPEED = "ac_fan_speed"
    private val KEY_TEMPERATURE = "ac_temperature"
    private val KEY_SEAT_TEMPERATURE = "ac_seat_temperature"
    private val DEFAULT_FAN_SPEED = 0
    private val DEFAULT_TEMPERATURE = 22
    private val DEFAULT_SEAT_TEMPERATURE = 0

    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private lateinit var leftTempBar: SeekBar
    private lateinit var leftTempValue: TextView
    private lateinit var leftSeatTempBar: SeekBar
    private lateinit var leftSeatTempValue: TextView
    private lateinit var leftFanSpeedBar: SeekBar
    private lateinit var autoButton: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ac, container, false)
        userName = arguments?.getString("user_name") ?: "Guest User"
        databaseInitializer = DatabaseInitializer(requireContext())

        // Initialize Car API
        try {
            car = Car.createCar(requireContext().applicationContext)
            if (car == null) {
                Log.e("AcFragment", "Car service initialization failed")
                return view
            }
            carPropertyManager = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            if (carPropertyManager == null) {
                Log.e("AcFragment", "CarPropertyManager initialization failed")
                return view
            }
        } catch (e: Exception) {
            Log.e("AcFragment", "Car init failed", e)
            return view
        }

        // Initialize views
        leftTempBar = view.findViewById(R.id.leftTempBar)
        leftTempValue = view.findViewById(R.id.leftTempValue)
        leftSeatTempBar = view.findViewById(R.id.leftSeatTempBar)
        leftSeatTempValue = view.findViewById(R.id.textView)
        leftFanSpeedBar = view.findViewById(R.id.leftFanSpeedBar)
        autoButton = view.findViewById(R.id.autoButton)

        // Load saved settings
        loadAcSettings()

        // Left fan speed seekbar listener
        leftFanSpeedBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    carPropertyManager?.let { manager ->
                        try {
                            synchronized(manager) {
                                manager.setProperty(
                                    Integer::class.java,
                                    VENDOR_EXTENSION_FAN_SPEED_CONTROL_PROPERTY,
                                    areaId,
                                    Integer(progress)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("AcFragment", "Failed to set fan speed", e)
                        }
                    }
                    // Save all current settings
                    saveAcSettings(progress, leftTempBar.progress, leftSeatTempBar.progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Left temperature seekbar listener
        leftTempBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    leftTempValue.text = "$progress°C"
                    carPropertyManager?.let { manager ->
                        try {
                            synchronized(manager) {
                                manager.setProperty(
                                    Integer::class.java,
                                    VENDOR_EXTENSION_TEMP_CONTROL_PROPERTY,
                                    areaId,
                                    Integer(progress)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("AcFragment", "Failed to set temperature", e)
                        }
                    }
                    // Save all current settings
                    saveAcSettings(leftFanSpeedBar.progress, progress, leftSeatTempBar.progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Left seat temperature seekbar listener
        leftSeatTempBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    leftSeatTempValue.text = "$progress°C"
                    carPropertyManager?.let { manager ->
                        try {
                            synchronized(manager) {
                                manager.setProperty(
                                    Integer::class.java,
                                    VENDOR_EXTENSION_SEAT_TEMP_CONTROL_PROPERTY,
                                    areaId,
                                    Integer(progress)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("AcFragment", "Failed to set seat temperature", e)
                        }
                    }
                    // Save all current settings
                    saveAcSettings(leftFanSpeedBar.progress, leftTempBar.progress, progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Auto button listener (assuming it resets to defaults)
        autoButton.setOnClickListener {
            resetToDefaults()
        }

        return view
    }

    private fun loadAcSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fanSpeed = prefs.getInt(KEY_FAN_SPEED, DEFAULT_FAN_SPEED)
        val temperature = prefs.getInt(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
        val seatTemperature = prefs.getInt(KEY_SEAT_TEMPERATURE, DEFAULT_SEAT_TEMPERATURE)

        leftFanSpeedBar.progress = fanSpeed
        leftTempBar.progress = temperature
        leftTempValue.text = "$temperature°C"
        leftSeatTempBar.progress = seatTemperature
        leftSeatTempValue.text = "$seatTemperature°C"

        // Update VHAL
        carPropertyManager?.let { manager ->
            try {
                synchronized(manager) {
                    manager.setProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_FAN_SPEED_CONTROL_PROPERTY,
                        areaId,
                        Integer(fanSpeed)
                    )
                    manager.setProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_TEMP_CONTROL_PROPERTY,
                        areaId,
                        Integer(temperature)
                    )
                    manager.setProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_SEAT_TEMP_CONTROL_PROPERTY,
                        areaId,
                        Integer(seatTemperature)
                    )
                }
            } catch (e: Exception) {
                Log.e("AcFragment", "Failed to set initial AC properties", e)
            }
        }
    }

    private fun saveAcSettings(fanSpeed: Int, temperature: Int, seatTemperature: Int) {
    Log.d("AcFragment", "Saving AC settings: fanSpeed=$fanSpeed, temperature=$temperature, seatTemperature=$seatTemperature")
    val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    with(prefs.edit()) {
        putInt(KEY_FAN_SPEED, fanSpeed)
        putInt(KEY_TEMPERATURE, temperature)
        putInt(KEY_SEAT_TEMPERATURE, seatTemperature)
        apply()
    }
    Log.d("AcFragment", "Saved to SharedPreferences")

    Log.d("AcFragment", "Current userName: $userName")
    if (userName != "Guest User") {
        Log.d("AcFragment", "Attempting to save AC settings to database for $userName")
        CoroutineScope(Dispatchers.IO).launch { // Changed to Dispatchers.IO for database operations
            try {
                val userExists = databaseInitializer.getUserDetails(userName) != null
                Log.d("AcFragment", "User $userName exists in database: $userExists")
                if (!userExists) {
                    Log.e("AcFragment", "User $userName not found in database, cannot save AC settings")
                    return@launch
                }
                val success = databaseInitializer.saveAcSettingsToDatabase(userName, fanSpeed, temperature, seatTemperature)
                Log.d("AcFragment", "Database save result for $userName: $success")
                if (!success) {
                    Log.e("AcFragment", "Database save returned false for $userName")
                }
            } catch (e: Exception) {
                Log.e("AcFragment", "Exception in database save for $userName", e)
            }
        }
    } else {
        Log.d("AcFragment", "Skipping database save for Guest User")
    }
}

    private fun resetToDefaults() {
        // Set seek bars to default values
        leftFanSpeedBar.progress = DEFAULT_FAN_SPEED
        leftTempBar.progress = DEFAULT_TEMPERATURE
        leftTempValue.text = "$DEFAULT_TEMPERATURE°C"
        leftSeatTempBar.progress = DEFAULT_SEAT_TEMPERATURE
        leftSeatTempValue.text = "$DEFAULT_SEAT_TEMPERATURE°C"

        // Update VHAL
        carPropertyManager?.let { manager ->
            try {
                synchronized(manager) {
                    manager.setProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_FAN_SPEED_CONTROL_PROPERTY,
                        areaId,
                        Integer(DEFAULT_FAN_SPEED)
                    )
                    manager.setProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_TEMP_CONTROL_PROPERTY,
                        areaId,
                        Integer(DEFAULT_TEMPERATURE)
                    )
                    manager.setProperty(
                        Integer::class.java,
                        VENDOR_EXTENSION_SEAT_TEMP_CONTROL_PROPERTY,
                        areaId,
                        Integer(DEFAULT_SEAT_TEMPERATURE)
                    )
                    Log.d("AcFragment", "Reset to defaults: fanSpeed=$DEFAULT_FAN_SPEED, temperature=$DEFAULT_TEMPERATURE°C, seatTemperature=$DEFAULT_SEAT_TEMPERATURE°C")
                }
            } catch (e: Exception) {
                Log.e("AcFragment", "Failed to reset VHAL properties", e)
            }
        }

        // Save to SharedPreferences
        saveAcSettings(DEFAULT_FAN_SPEED, DEFAULT_TEMPERATURE, DEFAULT_SEAT_TEMPERATURE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            car?.disconnect()
        } catch (e: Exception) {
            Log.e("AcFragment", "Car disconnect failed", e)
        }
    }
}