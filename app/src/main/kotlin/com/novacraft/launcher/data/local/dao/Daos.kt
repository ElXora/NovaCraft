package com.novacraft.launcher.data.local.dao

import androidx.room.*
import com.novacraft.launcher.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE accounts SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE accounts SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)
}

@Dao
interface VersionDao {
    @Query("SELECT * FROM versions ORDER BY releaseDate DESC")
    fun observeAll(): Flow<List<VersionEntity>>

    @Query("SELECT * FROM versions WHERE isInstalled = 1 ORDER BY releaseDate DESC")
    fun observeInstalled(): Flow<List<VersionEntity>>

    @Query("SELECT * FROM versions WHERE id = :id")
    suspend fun getById(id: String): VersionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(versions: List<VersionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: VersionEntity)

    @Update
    suspend fun update(version: VersionEntity)

    @Query("DELETE FROM versions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY lastPlayed DESC NULLS LAST, createdAt DESC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ModDao {
    @Query("SELECT * FROM mods WHERE profileId = :profileId ORDER BY name ASC")
    fun observeByProfile(profileId: String): Flow<List<ModEntity>>

    @Query("SELECT * FROM mods WHERE id = :id")
    suspend fun getById(id: String): ModEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mod: ModEntity)

    @Update
    suspend fun update(mod: ModEntity)

    @Query("DELETE FROM mods WHERE id = :id AND profileId = :profileId")
    suspend fun delete(id: String, profileId: String)

    @Query("UPDATE mods SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status NOT IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadEntity)

    @Update
    suspend fun update(task: DownloadEntity)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'CANCELLED', 'FAILED')")
    suspend fun clearCompleted()

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: String, bytes: Long, progress: Float)
}

@Dao
interface JavaRuntimeDao {
    @Query("SELECT * FROM java_runtimes ORDER BY version ASC")
    fun observeAll(): Flow<List<JavaRuntimeEntity>>

    @Query("SELECT * FROM java_runtimes WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): JavaRuntimeEntity?

    @Query("SELECT * FROM java_runtimes WHERE id = :id")
    suspend fun getById(id: String): JavaRuntimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(runtime: JavaRuntimeEntity)

    @Update
    suspend fun update(runtime: JavaRuntimeEntity)

    @Query("UPDATE java_runtimes SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE java_runtimes SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: String)
}

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY isFavorite DESC, name ASC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE servers SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Update
    suspend fun update(server: ServerEntity)
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY publishedAt DESC LIMIT 20")
    fun observeAll(): Flow<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<NewsEntity>)

    @Query("DELETE FROM news")
    suspend fun clearAll()
}

@Dao
interface CrashReportDao {
    @Query("SELECT * FROM crash_reports ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CrashReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: CrashReportEntity)

    @Query("DELETE FROM crash_reports WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM crash_reports")
    suspend fun deleteAll()
}
