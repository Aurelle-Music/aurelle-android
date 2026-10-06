package com.aurelle.music.download

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Linha do banco: uma música baixada em um destino. A chave é (id, target), então a mesma música pode
 * existir uma vez no Dispositivo e uma vez no App. `target` e `quality` guardam o nome do enum (texto).
 */
@Entity(tableName = "downloads", primaryKeys = ["id", "target"])
data class DownloadEntity(
    val id: String,
    val target: String,
    val title: String,
    val artist: String,
    val imageUrl: String?,
    val durationSeconds: Long?,
    val uri: String,
    val format: String,
    val bitrateKbps: Int,
    val sizeBytes: Long,
    val downloadedAt: Long,
    val quality: String,
    val album: String? = null,
    val trackNumber: Int? = null,
)

fun DownloadEntity.toTrack() = DownloadedTrack(
    id = id,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    durationSeconds = durationSeconds,
    target = runCatching { DownloadTarget.valueOf(target) }.getOrDefault(DownloadTarget.DEVICE),
    uri = uri,
    format = format,
    bitrateKbps = bitrateKbps,
    sizeBytes = sizeBytes,
    downloadedAt = downloadedAt,
    quality = runCatching { DownloadQuality.valueOf(quality) }.getOrDefault(DownloadQuality.MAX),
    album = album,
    trackNumber = trackNumber,
)

fun DownloadedTrack.toEntity() = DownloadEntity(
    id = id,
    target = target.name,
    title = title,
    artist = artist,
    imageUrl = imageUrl,
    durationSeconds = durationSeconds,
    uri = uri,
    format = format,
    bitrateKbps = bitrateKbps,
    sizeBytes = sizeBytes,
    downloadedAt = downloadedAt,
    quality = quality.name,
    album = album,
    trackNumber = trackNumber,
)

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads")
    suspend fun getAll(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE id = :id AND target = :target LIMIT 1")
    suspend fun get(id: String, target: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DownloadEntity>)

    @Query("DELETE FROM downloads WHERE id = :id AND target = :target")
    suspend fun delete(id: String, target: String)
}

@Database(entities = [DownloadEntity::class], version = 2, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloads(): DownloadDao

    companion object {
        fun create(context: Context): DownloadDatabase =
            Room.databaseBuilder(context.applicationContext, DownloadDatabase::class.java, "aurelle.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** v0.5.x -> v0.6.0: guarda o álbum e a posição da faixa (nulos nas músicas já baixadas). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN album TEXT")
                db.execSQL("ALTER TABLE downloads ADD COLUMN trackNumber INTEGER")
            }
        }
    }
}
