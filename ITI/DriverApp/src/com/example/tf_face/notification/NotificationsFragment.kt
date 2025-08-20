package com.example.tf_face.notification

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tf_face.R
import kotlin.math.roundToInt

class NotificationsFragment : Fragment() {

    private val PREFS_NAME = "DriverAppPrefs"
    private val KEY_BACKREST_ANGLE = "seat_backrest_angle"
    private val KEY_DEFAULT_BACKREST_ANGLE = "default_seat_backrest_angle"
    private val KEY_BASE_POSITION = "seat_base_position"
    private val KEY_FAN_SPEED = "ac_fan_speed"
    private val KEY_TEMPERATURE = "ac_temperature"
    private val KEY_SEAT_TEMPERATURE = "ac_seat_temperature"
    private val DEFAULT_BASE_POSITION = 0f
    private val DEFAULT_FAN_SPEED = 0
    private val DEFAULT_TEMPERATURE = 22
    private val DEFAULT_SEAT_TEMPERATURE = 0
    private val ACTION_SETTINGS_CHANGED = "com.example.tf_face.SETTINGS_CHANGED"

    private lateinit var adapter: NotificationsAdapter
    private var emptyTextView: TextView? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            return inflater.inflate(R.layout.fragment_notifications, container, false)
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "Failed to inflate layout", e)
            return null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.notifications_recycler_view)
        emptyTextView = view.findViewById(R.id.empty_notifications_text)

        if (recyclerView == null) {
            Log.e("NotificationsFragment", "RecyclerView not found")
            emptyTextView?.visibility = View.VISIBLE
            emptyTextView?.text = "Error: Unable to load notifications"
            return
        }

        recyclerView?.layoutManager = LinearLayoutManager(context)
        try {
            val notifications = generateNotifications()
            Log.d("NotificationsFragment", "Generated ${notifications.size} notifications")
            adapter = NotificationsAdapter(mutableListOf()) { notification ->
                try {
                    notification.revertAction.invoke()
                    requireContext().sendBroadcast(Intent(ACTION_SETTINGS_CHANGED).apply {
                        putExtra("notification_type", notification.type)
                    })
                    val newNotifications = generateNotifications()
                    adapter.updateNotifications(newNotifications)
                    updateEmptyState(newNotifications)
                } catch (e: Exception) {
                    Log.e("NotificationsFragment", "Error handling revert action for ${notification.type}", e)
                }
            }
            recyclerView?.adapter = adapter
            adapter.updateNotifications(notifications)
            updateEmptyState(notifications)
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "Failed to initialize adapter", e)
            emptyTextView?.visibility = View.VISIBLE
            emptyTextView?.text = "Error: Failed to load notifications"
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("NotificationsFragment", "onResume called")
        try {
            val notifications = generateNotifications()
            adapter.updateNotifications(notifications)
            updateEmptyState(notifications)
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "Failed to refresh notifications in onResume", e)
            emptyTextView?.visibility = View.VISIBLE
            emptyTextView?.text = "Error: Failed to refresh notifications"
        }
    }

    private fun generateNotifications(): List<Notification> {
        val notifications = mutableListOf<Notification>()
        try {
            val context = context ?: run {
                Log.e("NotificationsFragment", "Context is null")
                return notifications
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val defaultBackrestAngle = prefs.getFloat(KEY_DEFAULT_BACKREST_ANGLE, 90f)
            val backrestAngle = prefs.getFloat(KEY_BACKREST_ANGLE, defaultBackrestAngle)
            if (backrestAngle != defaultBackrestAngle) {
                notifications.add(
                    Notification(
                        message = "Seat backrest angle changed to ${backrestAngle.roundToInt()}°. Press Reset to return back to default (${defaultBackrestAngle.roundToInt()}°).",
                        type = "seat_angle",
                        revertAction = Notification.RevertAction {
                            prefs.edit().putFloat(KEY_BACKREST_ANGLE, defaultBackrestAngle).apply()
                        }
                    )
                )
            }
            val basePosition = prefs.getFloat(KEY_BASE_POSITION, DEFAULT_BASE_POSITION)
            if (basePosition != DEFAULT_BASE_POSITION) {
                notifications.add(
                    Notification(
                        message = "Seat base position changed to ${basePosition.roundToInt()}. Press Reset to return back to default (0).",
                        type = "seat_position",
                        revertAction = Notification.RevertAction {
                            prefs.edit().putFloat(KEY_BASE_POSITION, DEFAULT_BASE_POSITION).apply()
                        }
                    )
                )
            }
            val fanSpeed = prefs.getInt(KEY_FAN_SPEED, DEFAULT_FAN_SPEED)
            if (fanSpeed != DEFAULT_FAN_SPEED) {
                notifications.add(
                    Notification(
                        message = "AC fan speed changed to $fanSpeed. Press Press Reset to return back to default (0).",
                        type = "ac_fan",
                        revertAction = Notification.RevertAction {
                            prefs.edit().putInt(KEY_FAN_SPEED, DEFAULT_FAN_SPEED).apply()
                        }
                    )
                )
            }
            val temperature = prefs.getInt(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
            if (temperature != DEFAULT_TEMPERATURE) {
                notifications.add(
                    Notification(
                        message = "AC temperature changed to $temperature°C. Press Press Reset to return back to default (22°C).",
                        type = "ac_temp",
                        revertAction = Notification.RevertAction {
                            prefs.edit().putInt(KEY_TEMPERATURE, DEFAULT_TEMPERATURE).apply()
                        }
                    )
                )
            }
            val seatTemperature = prefs.getInt(KEY_SEAT_TEMPERATURE, DEFAULT_SEAT_TEMPERATURE)
            if (seatTemperature != DEFAULT_SEAT_TEMPERATURE) {
                notifications.add(
                    Notification(
                        message = "AC seat temperature changed to $seatTemperature°C. Press Press Reset to return back to default (0°C).",
                        type = "ac_seat_temp",
                        revertAction = Notification.RevertAction {
                            prefs.edit().putInt(KEY_SEAT_TEMPERATURE, DEFAULT_SEAT_TEMPERATURE).apply()
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("NotificationsFragment", "Failed to generate notifications", e)
        }
        return notifications
    }

    private fun updateEmptyState(notifications: List<Notification>) {
        emptyTextView?.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (notifications.isEmpty()) View.GONE else View.VISIBLE
        emptyTextView?.text = "No notifications available"
    }
}