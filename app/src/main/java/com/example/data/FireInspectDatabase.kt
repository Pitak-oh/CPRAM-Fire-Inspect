package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "fire_extinguishers")
data class FireExtinguisher(
    @PrimaryKey val id: String,
    val location: String,
    val type: String, // เคมีแห้ง (Dry Chemical), CO2, โฟม (Foam), ฮาโลตรอน (Halotron)
    val size: String, // 10 lbs, 15 lbs, 20 lbs
    val installationDate: String,
    val lastMaintenanceDate: String,
    val nextMaintenanceDate: String,
    val status: String, // "NORMAL" (ปกติ), "PROBLEM" (มีปัญหา), "PENDING" (ยังไม่ได้ตรวจ)
    val lastInspectedAt: Long? = null,
    val lastInspectedBy: String? = null,
    val remarks: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val layoutX: Float = 0.5f, // X dynamic percentage on layout map [0.0 - 1.0]
    val layoutY: Float = 0.5f  // Y dynamic percentage on layout map [0.0 - 1.0]
)

@Entity(tableName = "inspection_logs")
data class InspectionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val extinguisherId: String,
    val location: String,
    val inspectorName: String,
    val timestamp: Long,
    val pressureGaugeOk: Boolean,
    val safetyPinOk: Boolean,
    val nozzleOk: Boolean,
    val physicalStatusOk: Boolean,
    val overallStatus: String, // "NORMAL" or "PROBLEM"
    val photoPath: String? = null, // Capturing mock base64/placeholder uri
    val remarks: String? = null,
    val isSynced: Boolean = true // Sync status for offline Mode!
)

@Dao
interface FireInspectDao {
    @Query("SELECT * FROM fire_extinguishers ORDER BY id ASC")
    fun getAllExtinguishersFlow(): Flow<List<FireExtinguisher>>

    @Query("SELECT * FROM fire_extinguishers")
    suspend fun getAllExtinguishersList(): List<FireExtinguisher>

    @Query("SELECT * FROM fire_extinguishers WHERE id = :id")
    suspend fun getExtinguisherById(id: String): FireExtinguisher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtinguisher(extinguisher: FireExtinguisher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtinguishers(extinguishers: List<FireExtinguisher>)

    @Update
    suspend fun updateExtinguisher(extinguisher: FireExtinguisher)

    @Query("UPDATE fire_extinguishers SET status = :status, lastInspectedAt = :inspectedAt, lastInspectedBy = :inspectedBy, remarks = :remarks WHERE id = :id")
    suspend fun updateExtinguisherStatus(id: String, status: String, inspectedAt: Long, inspectedBy: String, remarks: String?)

    @Query("SELECT * FROM inspection_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<InspectionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InspectionLog)

    @Query("SELECT * FROM inspection_logs WHERE isSynced = 0")
    suspend fun getUnsyncedLogs(): List<InspectionLog>

    @Query("UPDATE inspection_logs SET isSynced = 1 WHERE id = :logId")
    suspend fun markLogSynced(logId: Int)

    @Query("DELETE FROM fire_extinguishers")
    suspend fun deleteAllExtinguishers()

    @Query("DELETE FROM inspection_logs")
    suspend fun deleteAllLogs()
}

@Database(entities = [FireExtinguisher::class, InspectionLog::class], version = 1, exportSchema = false)
abstract class FireInspectDatabase : RoomDatabase() {
    abstract fun dao(): FireInspectDao

    companion object {
        @Volatile
        private var INSTANCE: FireInspectDatabase? = null

        fun getDatabase(context: Context): FireInspectDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FireInspectDatabase::class.java,
                    "cpram_fire_inspect_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
