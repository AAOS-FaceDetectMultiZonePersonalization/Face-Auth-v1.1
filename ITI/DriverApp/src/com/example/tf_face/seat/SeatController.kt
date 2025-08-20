package com.example.tf_face.seat

import android.Manifest
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.RequiresPermission
import android.animation.ValueAnimator
import com.example.tf_face.R

class SeatController(private val context: Context) {

    // Seat position state (horizontal offset of the seat base)
    private var horizontalOffset = 0f

    // Seat movement limits (defined in resources)
    private var maxForward: Float = 0f
    private var maxBackward: Float = 0f
    private var horizontalStep: Float = 0f

    // Backrest angle state
    private var currentAngle = 90f
    var minAngle = 45f
    var maxAngle = 90f
    var angleStep = 2f

    // Animation properties
    private val shortAnimDuration: Int
    private val mediumAnimDuration: Int
    private val interpolator = AccelerateDecelerateInterpolator()

    init {
        val res = context.resources
        // Load seat movement configuration from resources
        maxForward = res.getDimension(R.dimen.seat_max_forward)
        maxBackward = res.getDimension(R.dimen.seat_max_backward)
        horizontalStep = res.getDimension(R.dimen.seat_step_horizontal)

        // Load backrest configuration from resources
        minAngle = res.getInteger(R.integer.backrest_min_angle).toFloat()
        maxAngle = res.getInteger(R.integer.backrest_max_angle).toFloat()
        angleStep = res.getInteger(R.integer.backrest_angle_step).toFloat()

        // Load animation durations
        shortAnimDuration = res.getInteger(R.integer.anim_short)
        mediumAnimDuration = res.getInteger(R.integer.anim_medium)
    }

    // Move seat forward (toward maxForward)
    fun moveForward(): Float {
        val newOffset = (horizontalOffset + horizontalStep).coerceAtMost(maxForward)
        if (newOffset != horizontalOffset) {
            horizontalOffset = newOffset
            return newOffset
        }
        return horizontalOffset
    }

    // Vibrate device for haptic feedback
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrate(duration: Long) {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(
            VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    // Move seat backward (toward maxBackward)
    fun moveBackward(): Float {
        val newOffset = (horizontalOffset - horizontalStep).coerceAtLeast(maxBackward)
        if (newOffset != horizontalOffset) {
            horizontalOffset = newOffset
            return newOffset
        }
        return horizontalOffset
    }

    // Adjust backrest angle (positive for recline, negative for upright)
    fun adjustAngle(change: Float): Float {
        currentAngle = (currentAngle + change).coerceIn(minAngle, maxAngle)
        return currentAngle
    }

    // Reset seat to default position (center, upright)
    fun resetPosition() {
        horizontalOffset = 0f
        currentAngle = maxAngle // Reset to 90 degrees
    }

    // Getters for current state
    fun getCurrentAngle(): Float = currentAngle
    fun getCurrentHorizontalOffset(): Float = horizontalOffset

    // Create animator for smooth UI transitions
    fun createAnimator(start: Float, end: Float, duration: Int = shortAnimDuration): ValueAnimator {
        return ValueAnimator.ofFloat(start, end).apply {
            this.duration = duration.toLong()
            interpolator = this@SeatController.interpolator
        }
    }

    // Data class for seat presets
    data class SeatPreset(val angle: Float, val position: Float)
    private val presets = mutableMapOf<Int, SeatPreset?>(
        0 to null,  // Preset 1
        2 to null,  // Preset 2
        3 to null   // Preset 3
    )

    // Save current seat settings as a preset
    fun savePreset(index: Int) {
        presets[index] = SeatPreset(currentAngle, horizontalOffset)
    }

    // Retrieve a preset by index
    fun getPreset(index: Int): SeatPreset? = presets[index]
}