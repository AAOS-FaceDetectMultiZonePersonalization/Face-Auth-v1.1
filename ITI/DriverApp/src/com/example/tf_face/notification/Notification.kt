package com.example.tf_face.notification

/**
 * Data class representing a notification with a message, type, and revert action.
 *
 * @param message The notification message to display (non-empty).
 * @param type The type of notification (e.g., "seat_angle", "ac_fan"). Must be non-empty.
 * @param revertAction Callback to execute when reverting to default settings.
 */
data class Notification(
    val message: String,
    val type: String,
    val revertAction: RevertAction
) {
    init {
        require(message.isNotBlank()) { "Notification message must not be empty" }
        require(type.isNotBlank()) { "Notification type must not be empty" }
    }

    fun interface RevertAction {
        fun invoke()
    }
}