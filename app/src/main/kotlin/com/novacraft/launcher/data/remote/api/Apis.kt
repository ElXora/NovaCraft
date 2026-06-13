package com.novacraft.launcher.data.remote.api

import com.novacraft.launcher.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Mojang / Microsoft auth REST endpoints.
 */
interface AuthApi {

    /** Exchange Microsoft OAuth code for XBL token */
    @POST("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
    @FormUrlEncoded
    suspend fun getMicrosoftToken(
        @Field("client_id") clientId: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String,
        @Field("code") code: String
    ): Response<MicrosoftTokenDto>

    /** Refresh Microsoft token */
    @POST("https://login.microsoftonline.com/consumers/oauth2/v2.0/token")
    @FormUrlEncoded
    suspend fun refreshMicrosoftToken(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token"
    ): Response<MicrosoftTokenDto>

    /** Xbox Live authenticate */
    @POST("https://user.auth.xboxlive.com/user/authenticate")
    suspend fun authenticateXbl(
        @Body body: XblAuthRequest
    ): Response<XblAuthResponse>

    /** XSTS authorize */
    @POST("https://xsts.auth.xboxlive.com/xsts/authorize")
    suspend fun authorizeXsts(
        @Body body: XstsAuthRequest
    ): Response<XstsAuthResponse>

    /** Minecraft login with XSTS token */
    @POST("https://api.minecraftservices.com/authentication/login_with_xbox")
    suspend fun loginWithXbox(
        @Body body: MinecraftXboxLoginRequest
    ): Response<MinecraftXboxLoginResponse>

    /** Get Minecraft profile */
    @GET("https://api.minecraftservices.com/minecraft/profile")
    suspend fun getMinecraftProfile(
        @Header("Authorization") bearerToken: String
    ): Response<MinecraftProfileDto>
}

/**
 * Mojang version manifest API.
 */
interface MojangVersionApi {

    @GET("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
    suspend fun getVersionManifest(): Response<VersionManifestDto>

    @GET
    suspend fun getVersionDetails(@Url url: String): Response<VersionDetailsDto>
}

/**
 * Modrinth API for mods / resource packs / shaders.
 */
interface ModrinthApi {

    @GET("search")
    suspend fun searchProjects(
        @Query("query") query: String,
        @Query("facets") facets: String,   // JSON array facets e.g. [["project_type:mod"]]
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ModrinthSearchResultDto>

    @GET("project/{id}/version")
    suspend fun getProjectVersions(
        @Path("id") projectId: String,
        @Query("loaders") loaders: String? = null,
        @Query("game_versions") gameVersions: String? = null
    ): Response<List<ModrinthVersionDto>>

    @GET("version/{id}")
    suspend fun getVersion(@Path("id") versionId: String): Response<ModrinthVersionDto>
}

/**
 * Adoptium / Eclipse Temurin JRE API for Java runtime downloads.
 */
interface AdoptiumApi {

    @GET("v3/assets/latest/{version}/hotspot")
    suspend fun getLatestJre(
        @Path("version") featureVersion: Int,
        @Query("architecture") architecture: String = "aarch64",
        @Query("image_type") imageType: String = "jre",
        @Query("os") os: String = "linux",
        @Query("vendor") vendor: String = "eclipse"
    ): Response<List<AdoptiumAssetDto>>
}
