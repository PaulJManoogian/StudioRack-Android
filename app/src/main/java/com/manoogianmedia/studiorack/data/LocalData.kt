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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "records", primaryKeys = ["entityType", "entityId"])
data class CachedRecord(val entityType: String, val entityId: String, val revision: Int, val json: String)

data class RecordRef(val entityType: String, val entityId: String)

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
    val operation: String = "upsert",
)

@Entity(tableName = "sync_state")
data class SyncState(
    @androidx.room.PrimaryKey val id: Int = 1,
    val cursor: Long = 0,
    val accountJson: String = "{}",
    val performanceSettingsJson: String = "{}",
    val lastSyncAt: Long? = null,
    val lastError: String? = null,
)

@Entity(tableName = "cached_attachments")
data class CachedAttachment(
    @androidx.room.PrimaryKey val attachmentId: String,
    val songId: String,
    val revision: Int,
    val fileRef: String,
    val displayName: String,
    val attachmentType: String,
    val localPath: String?,
    val mimeType: String?,
    val sha256: String?,
    val byteCount: Long?,
    val status: String,
    val error: String? = null,
    val cachedAt: Long? = null,
)

@Dao
interface StudioRackDao {
    @Query("SELECT * FROM records WHERE entityType=:type ORDER BY entityId")
    fun observeRecords(type: String): Flow<List<CachedRecord>>

    @Query("SELECT * FROM records WHERE entityType=:type ORDER BY entityId")
    suspend fun records(type: String): List<CachedRecord>

    @Query("SELECT * FROM records WHERE entityType=:type AND entityId=:id LIMIT 1")
    suspend fun record(type: String, id: String): CachedRecord?

    @Query("SELECT * FROM supporting_records WHERE entityType=:type ORDER BY entityId")
    suspend fun supporting(type: String): List<SupportingRecord>

    @Query("SELECT * FROM supporting_records WHERE entityType=:type ORDER BY entityId")
    fun observeSupporting(type: String): Flow<List<SupportingRecord>>

    @Query("SELECT * FROM cached_attachments ORDER BY attachmentId")
    fun observeCachedAttachments(): Flow<List<CachedAttachment>>

    @Query("SELECT * FROM cached_attachments ORDER BY attachmentId")
    suspend fun cachedAttachments(): List<CachedAttachment>

    @Query("SELECT * FROM sync_state WHERE id=1")
    fun observeSyncState(): Flow<SyncState?>

    @Query("SELECT * FROM sync_state WHERE id=1")
    suspend fun syncState(): SyncState?

    @Query("SELECT * FROM pending_mutations ORDER BY createdAt, mutationId LIMIT :limit")
    suspend fun pending(limit: Int = 100): List<PendingMutation>

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM sync_conflicts ORDER BY entityType, entityId")
    fun observeConflicts(): Flow<List<SyncConflict>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putRecords(records: List<CachedRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSupporting(records: List<SupportingRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putState(state: SyncState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putConflict(conflict: SyncConflict)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPending(mutation: PendingMutation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCachedAttachment(attachment: CachedAttachment)

    @Query("DELETE FROM cached_attachments WHERE attachmentId=:id")
    suspend fun deleteCachedAttachment(id: String)

    @Query("DELETE FROM records WHERE entityType=:type AND entityId=:id")
    suspend fun deleteRecord(type: String, id: String)

    @Query("DELETE FROM records")
    suspend fun clearRecords()

    @Query("DELETE FROM supporting_records")
    suspend fun clearSupporting()

    @Query("DELETE FROM pending_mutations WHERE mutationId=:id")
    suspend fun removeMutation(id: String)

    @Query("DELETE FROM pending_mutations WHERE entityType=:type AND entityId=:id")
    suspend fun removePendingForEntity(type: String, id: String)

    @Query("DELETE FROM sync_conflicts WHERE mutationId=:id")
    suspend fun removeConflict(id: String)

    @Transaction
    suspend fun applyLocalBundle(upserts: List<CachedRecord>, deletes: List<RecordRef>, mutations: List<PendingMutation>) {
        deletes.forEach { ref ->
            deleteRecord(ref.entityType, ref.entityId)
            removePendingForEntity(ref.entityType, ref.entityId)
        }
        upserts.forEach { record ->
            putRecords(listOf(record))
            removePendingForEntity(record.entityType, record.entityId)
        }
        mutations.forEach { putPending(it) }
    }

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
    entities = [CachedRecord::class, SupportingRecord::class, PendingMutation::class, SyncConflict::class, SyncState::class, CachedAttachment::class],
    version = 4,
    exportSchema = false,
)
abstract class StudioRackDatabase : RoomDatabase() {
    abstract fun dao(): StudioRackDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS cached_attachments (
                        attachmentId TEXT NOT NULL PRIMARY KEY,
                        songId TEXT NOT NULL,
                        revision INTEGER NOT NULL,
                        fileRef TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        attachmentType TEXT NOT NULL,
                        localPath TEXT,
                        mimeType TEXT,
                        sha256 TEXT,
                        byteCount INTEGER,
                        status TEXT NOT NULL,
                        error TEXT,
                        cachedAt INTEGER
                    )""".trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sync_state ADD COLUMN performanceSettingsJson TEXT NOT NULL DEFAULT '{}'")
            }
        }


        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sync_conflicts ADD COLUMN operation TEXT NOT NULL DEFAULT 'upsert'")
            }
        }

        fun create(context: Context): StudioRackDatabase = Room.databaseBuilder(
            context,
            StudioRackDatabase::class.java,
            "studiorack-offline.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}
