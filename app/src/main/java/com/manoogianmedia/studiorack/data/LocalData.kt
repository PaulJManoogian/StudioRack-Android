package com.manoogianmedia.studiorack.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "records", primaryKeys = ["entityType", "entityId"])
data class CachedRecord(val entityType: String, val entityId: String, val revision: Int, val json: String)

@Entity(tableName = "supporting_records", primaryKeys = ["entityType", "entityId"])
data class SupportingRecord(val entityType: String, val entityId: String, val json: String)

@Entity(tableName = "pending_mutations")
data class PendingMutation(
    @androidx.room.PrimaryKey val mutationId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val baseRevision: Int,
    val json: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "sync_conflicts")
data class SyncConflict(
    @androidx.room.PrimaryKey val mutationId: String,
    val entityType: String,
    val entityId: String,
    val localJson: String,
    val serverJson: String?,
    val serverRevision: Int,
)

@Entity(tableName = "sync_state")
data class SyncState(
    @androidx.room.PrimaryKey val id: Int = 1,
    val cursor: Long = 0,
    val accountJson: String = "{}",
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)

@Dao
interface StudioRackDao {
    @Query("SELECT * FROM records WHERE entityType=:type ORDER BY entityId")
    fun observeRecords(type: String): Flow<List<CachedRecord>>

    @Query("SELECT * FROM records WHERE entityType=:type ORDER BY entityId")
    suspend fun records(type: String): List<CachedRecord>

    @Query("SELECT * FROM supporting_records WHERE entityType=:type ORDER BY entityId")
    suspend fun supporting(type: String): List<SupportingRecord>

    @Query("SELECT * FROM sync_state WHERE id=1")
    fun observeSyncState(): Flow<SyncState?>

    @Query("SELECT * FROM sync_state WHERE id=1")
    suspend fun syncState(): SyncState?

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<PendingMutation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRecords(records: List<CachedRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSupporting(records: List<SupportingRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putState(state: SyncState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putConflict(conflict: SyncConflict)

    @Query("DELETE FROM records WHERE entityType=:type AND entityId=:id")
    suspend fun deleteRecord(type: String, id: String)

    @Query("DELETE FROM records")
    suspend fun clearRecords()

    @Query("DELETE FROM supporting_records")
    suspend fun clearSupporting()

    @Query("DELETE FROM pending_mutations WHERE mutationId=:id")
    suspend fun removeMutation(id: String)

    @Transaction
    suspend fun replaceSnapshot(records: List<CachedRecord>, supporting: List<SupportingRecord>, state: SyncState) {
        clearRecords()
        clearSupporting()
        putRecords(records)
        putSupporting(supporting)
        putState(state)
    }
}

@Database(
    entities = [CachedRecord::class, SupportingRecord::class, PendingMutation::class, SyncConflict::class, SyncState::class],
    version = 1,
    exportSchema = false,
)
abstract class StudioRackDatabase : RoomDatabase() {
    abstract fun dao(): StudioRackDao

    companion object {
        fun create(context: Context): StudioRackDatabase = Room.databaseBuilder(
            context,
            StudioRackDatabase::class.java,
            "studiorack-offline.db",
        ).build()
    }
}
