package com.example.tf_face

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val embedding: FloatArray,
    val imageUri: String,
    val weight: Float,
    val height: Float,
    val gender: String,
    val age: Int,
    // New AC and seat settings
    val fanSpeed: Int = 0,
    val temperature: Int = 22,
    val seatTemperature: Int = 0,
    val backrestAngle: Float = 90f,
    val seatPosition: Float = 0f

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (imageUri != other.imageUri) return false
        if (weight != other.weight) return false
        if (height != other.height) return false
        if (gender != other.gender) return false
        if (age != other.age) return false
        if (fanSpeed != other.fanSpeed) return false
        if (temperature != other.temperature) return false
        if (seatTemperature != other.seatTemperature) return false
        if (backrestAngle != other.backrestAngle) return false
        if (seatPosition != other.seatPosition) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + imageUri.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + gender.hashCode()
        result = 31 * result + age
        result = 31 * result + fanSpeed
        result = 31 * result + temperature
        result = 31 * result + seatTemperature
        result = 31 * result + backrestAngle.hashCode()
        result = 31 * result + seatPosition.hashCode()
        return result
    }
}