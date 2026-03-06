package com.example.nids

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import retrofit2.http.GET

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey val id: Long,
    val verdict: String,
    val confidence: Double,
    val timestamp: Long,
    val userAction: String = "PENDING"
)

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threats ORDER BY timestamp DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertThreats(threats: List<ThreatEntity>)

    @Query("UPDATE threats SET userAction = :action WHERE id = :id")
    suspend fun updateAction(id: Long, action: String)
}

@Database(entities = [ThreatEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun threatDao(): ThreatDao
}

interface NidsApiService {
    @GET("threats")
    suspend fun getThreats(): List<ThreatEntity>
}