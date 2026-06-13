package com.novacraft.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.novacraft.launcher.data.local.dao.*
import com.novacraft.launcher.data.local.entities.*

/**
 * Room type converters for complex types.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}

/**
 * NovaCraft Room Database
 *
 * Single database holding all launcher persistent state.
 * Migration strategy: destructive for early development, proper migrations for production.
 */
@Database(
    entities = [
        AccountEntity::class,
        VersionEntity::class,
        ProfileEntity::class,
        ModEntity::class,
        DownloadEntity::class,
        JavaRuntimeEntity::class,
        ServerEntity::class,
        NewsEntity::class,
        CrashReportEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NovaCraftDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun versionDao(): VersionDao
    abstract fun profileDao(): ProfileDao
    abstract fun modDao(): ModDao
    abstract fun downloadDao(): DownloadDao
    abstract fun javaRuntimeDao(): JavaRuntimeDao
    abstract fun serverDao(): ServerDao
    abstract fun newsDao(): NewsDao
    abstract fun crashReportDao(): CrashReportDao

    companion object {
        const val DATABASE_NAME = "novacraft_db"
    }
}
