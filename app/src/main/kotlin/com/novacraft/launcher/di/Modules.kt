package com.novacraft.launcher.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.novacraft.launcher.data.local.NovaCraftDatabase
import com.novacraft.launcher.data.local.dao.*
import com.novacraft.launcher.data.remote.api.*
import com.novacraft.launcher.data.repository.*
import com.novacraft.launcher.domain.repository.*
import com.novacraft.launcher.service.auth.MicrosoftAuthService
import com.novacraft.launcher.service.download.DownloadManager
import com.novacraft.launcher.util.Constants
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

// ─── Database Module ──────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NovaCraftDatabase =
        Room.databaseBuilder(context, NovaCraftDatabase::class.java, NovaCraftDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAccountDao(db: NovaCraftDatabase): AccountDao = db.accountDao()
    @Provides fun provideVersionDao(db: NovaCraftDatabase): VersionDao = db.versionDao()
    @Provides fun provideProfileDao(db: NovaCraftDatabase): ProfileDao = db.profileDao()
    @Provides fun provideModDao(db: NovaCraftDatabase): ModDao = db.modDao()
    @Provides fun provideDownloadDao(db: NovaCraftDatabase): DownloadDao = db.downloadDao()
    @Provides fun provideJavaRuntimeDao(db: NovaCraftDatabase): JavaRuntimeDao = db.javaRuntimeDao()
    @Provides fun provideServerDao(db: NovaCraftDatabase): ServerDao = db.serverDao()
    @Provides fun provideNewsDao(db: NovaCraftDatabase): NewsDao = db.newsDao()
    @Provides fun provideCrashReportDao(db: NovaCraftDatabase): CrashReportDao = db.crashReportDao()
}

// ─── Network Module ──────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    @Named("mojang")
    fun provideMojangRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://launchermeta.mojang.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("modrinth")
    fun provideModrinthRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.modrinth.com/v2/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("adoptium")
    fun provideAdoptiumRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.adoptium.net/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideMojangVersionApi(@Named("mojang") retrofit: Retrofit): MojangVersionApi =
        retrofit.create(MojangVersionApi::class.java)

    @Provides
    @Singleton
    fun provideModrinthApi(@Named("modrinth") retrofit: Retrofit): ModrinthApi =
        retrofit.create(ModrinthApi::class.java)

    @Provides
    @Singleton
    fun provideAdoptiumApi(@Named("adoptium") retrofit: Retrofit): AdoptiumApi =
        retrofit.create(AdoptiumApi::class.java)
}

// ─── Security Module ─────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideEncryptedPrefs(@ApplicationContext context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "novacraft_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}

// ─── Repository Bindings ──────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindVersionRepository(impl: VersionRepositoryImpl): VersionRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds @Singleton
    abstract fun bindModRepository(impl: ModRepositoryImpl): ModRepository

    @Binds @Singleton
    abstract fun bindJavaRepository(impl: JavaRepositoryImpl): JavaRepository

    @Binds @Singleton
    abstract fun bindDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository

    @Binds @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository
}
