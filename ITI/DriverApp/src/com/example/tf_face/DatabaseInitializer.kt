package com.example.tf_face

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DatabaseInitializer(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    suspend fun initializeDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val isInitialized = sharedPreferences.getBoolean("isDatabaseInitialized", true)
        if (!isInitialized) {
            sharedPreferences.edit().putBoolean("isDatabaseInitialized", true).apply()
            Log.d("DatabaseInitializer", "Database initialized (cleared)")
        } else {
            Log.d("DatabaseInitializer", "Database already initialized")
        }
    }

    suspend fun deleteFacesByName(name: String) = withContext(Dispatchers.IO) {
        try {
            database.faceDao().deleteFacesByName(name)
            Log.d("DatabaseInitializer", "Deleted faces for $name")
            true
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error deleting faces for $name", e)
            false
        }
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        database.faceDao().getAllFaces().forEach {
            database.faceDao().delete(it)
        }
        Log.d("DatabaseInitializer", "Database cleared")
    }

    suspend fun addFace(
        name: String,
        imageBitmap: Bitmap,
        imageName: String,
        weight: Float,
        height: Float,
        gender: String,
        age: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Save bitmap as JPEG to internal storage
            val imageDir = File("/data/system/tf_face/images")
            imageDir.mkdirs()
            val file = File(imageDir, imageName)
            FileOutputStream(file).use { out ->
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            Log.d("DatabaseInitializer", "Saved image to ${file.absolutePath}")
            
            // Detect and crop face
            val detector = BlazeFaceDetector(context)
            val faces = detector.detect(imageBitmap)
            val largestFace = faces.maxByOrNull { it.width() * it.height() } ?: return@withContext false
            val faceBitmap = detector.cropFace(imageBitmap, largestFace)
            val embedding = detector.getFaceEmbedding(faceBitmap)

            // Insert into database with new fields
            database.faceDao().insert(FaceEntity(
                name = name,
                embedding = embedding,
                imageUri = imageName,
                weight = weight,
                height = height,
                gender = gender,
                age=age
            ))
            Log.d("DatabaseInitializer", "Inserted face for $name with weight=$weight, height=$height, gender=$gender")
            true
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error adding face for $name", e)
            false
        }
    }

    suspend fun printAllFaces() = withContext(Dispatchers.IO) {
        try {
            val faces = database.faceDao().getAllFaces()
            Log.d("DatabaseInitializer", "Database contains ${faces.size} faces:")
            faces.forEach { face ->
                Log.d("DatabaseInitializer", """
                    Face ID: ${face.id}
                    Name: ${face.name}
                    Age: ${face.age}
                    Gender: ${face.gender}
                    Weight: ${face.weight}
                    Height: ${face.height}
                    Image URI: ${face.imageUri}
                """.trimIndent())
            }
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error reading database", e)
        }
    }

    suspend fun getFrameCountForUser(name: String): Int = withContext(Dispatchers.IO) {
        database.faceDao().getFacesByName(name).size
    }

    suspend fun getUserDetails(name: String): FaceEntity? = withContext(Dispatchers.IO) {
        database.faceDao().getFacesByName(name).firstOrNull()
    }

    suspend fun saveAcSettingsToDatabase(name: String, fanSpeed: Int, temperature: Int, seatTemperature: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            database.faceDao().updateAcSettings(name, fanSpeed, temperature, seatTemperature)
            Log.d("DatabaseInitializer", "Successfully saved AC settings for $name: fanSpeed=$fanSpeed, temperature=$temperature°C, seatTemperature=$seatTemperature°C")
            true
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error saving AC settings for $name", e)
            false
        }
    }

    suspend fun saveSeatSettingsToDatabase(name: String, backrestAngle: Float, seatPosition: Float): Boolean = withContext(Dispatchers.IO) {
        try {
            database.faceDao().updateSeatSettings(name, backrestAngle, seatPosition)
            Log.d("DatabaseInitializer", "Successfully saved seat settings for $name: backrestAngle=$backrestAngle°, seatPosition=$seatPosition")
            true
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error saving seat settings for $name", e)
            false
        }
    }

    suspend fun saveAllSettingsToDatabase(name: String, fanSpeed: Int, temperature: Int, seatTemperature: Int, backrestAngle: Float, seatPosition: Float): Boolean = withContext(Dispatchers.IO) {
        try {
            database.faceDao().updateAllSettings(name, fanSpeed, temperature, seatTemperature, backrestAngle, seatPosition)
            Log.d("DatabaseInitializer", "Successfully saved all settings for $name: fanSpeed=$fanSpeed, temperature=$temperature°C, seatTemperature=$seatTemperature°C, backrestAngle=$backrestAngle°, seatPosition=$seatPosition")
            true
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error saving all settings for $name", e)
            false
        }
    }

    suspend fun getUserSettings(name: String): FaceEntity? = withContext(Dispatchers.IO) {
        try {
            database.faceDao().getFaceByName(name)
        } catch (e: Exception) {
            Log.e("DatabaseInitializer", "Error getting settings for $name", e)
            null
        }
    }
}