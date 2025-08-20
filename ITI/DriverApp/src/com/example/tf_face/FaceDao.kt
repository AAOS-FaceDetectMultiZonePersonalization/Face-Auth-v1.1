package com.example.tf_face

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FaceDao {
    @Insert
    suspend fun insert(face: FaceEntity)

    @Update
    suspend fun update(face: FaceEntity)

    @Query("SELECT * FROM FaceEntity")
    suspend fun getAllFaces(): List<FaceEntity>

    @Query("SELECT * FROM FaceEntity WHERE name = :name")
    suspend fun getFacesByName(name: String): List<FaceEntity>

    @Delete
    suspend fun delete(face: FaceEntity)
    
    @Query("DELETE FROM FaceEntity WHERE name = :name")
    suspend fun deleteFacesByName(name: String)
    
    @Query("SELECT * FROM FaceEntity WHERE gender = :gender")
    suspend fun getFacesByGender(gender: String): List<FaceEntity>
    
    @Query("SELECT * FROM FaceEntity WHERE weight BETWEEN :min AND :max")
    suspend fun getFacesByWeightRange(min: Float, max: Float): List<FaceEntity>
    
    @Query("SELECT * FROM FaceEntity WHERE height BETWEEN :min AND :max")
    suspend fun getFacesByHeightRange(min: Float, max: Float): List<FaceEntity>
    
    @Query("SELECT * FROM FaceEntity WHERE name = :name LIMIT 1")
    suspend fun getFaceByName(name: String): FaceEntity?

    @Query("SELECT DISTINCT name FROM FaceEntity")
    suspend fun getAllUserNames(): List<String>

    // New methods for AC and seat settings
    @Query("UPDATE FaceEntity SET fanSpeed = :fanSpeed, temperature = :temperature, seatTemperature = :seatTemperature WHERE name = :name")
    suspend fun updateAcSettings(name: String, fanSpeed: Int, temperature: Int, seatTemperature: Int)

    @Query("UPDATE FaceEntity SET backrestAngle = :backrestAngle, seatPosition = :seatPosition WHERE name = :name")
    suspend fun updateSeatSettings(name: String, backrestAngle: Float, seatPosition: Float)

    @Query("UPDATE FaceEntity SET fanSpeed = :fanSpeed, temperature = :temperature, seatTemperature = :seatTemperature, backrestAngle = :backrestAngle, seatPosition = :seatPosition WHERE name = :name")
    suspend fun updateAllSettings(name: String, fanSpeed: Int, temperature: Int, seatTemperature: Int, backrestAngle: Float, seatPosition: Float)
}