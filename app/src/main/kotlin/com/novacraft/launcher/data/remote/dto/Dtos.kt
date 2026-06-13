package com.novacraft.launcher.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── Microsoft Auth DTOs ─────────────────────────────────────────────────────

data class MicrosoftTokenDto(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in")    val expiresIn: Int
)

data class XblAuthRequest(
    @SerializedName("Properties") val properties: XblProperties,
    @SerializedName("RelyingParty") val relyingParty: String = "http://auth.xboxlive.com",
    @SerializedName("TokenType") val tokenType: String = "JWT"
)

data class XblProperties(
    @SerializedName("AuthMethod") val authMethod: String = "RPS",
    @SerializedName("SiteName") val siteName: String = "user.auth.xboxlive.com",
    @SerializedName("RpsTicket") val rpsTicket: String   // "d=<msAccessToken>"
)

data class XblAuthResponse(
    @SerializedName("Token") val token: String,
    @SerializedName("DisplayClaims") val displayClaims: DisplayClaims
)

data class DisplayClaims(
    @SerializedName("xui") val xui: List<XuiClaim>
)

data class XuiClaim(
    @SerializedName("uhs") val userHash: String
)

data class XstsAuthRequest(
    @SerializedName("Properties") val properties: XstsProperties,
    @SerializedName("RelyingParty") val relyingParty: String = "rp://api.minecraftservices.com/",
    @SerializedName("TokenType") val tokenType: String = "JWT"
)

data class XstsProperties(
    @SerializedName("SandboxId") val sandboxId: String = "RETAIL",
    @SerializedName("UserTokens") val userTokens: List<String>
)

data class XstsAuthResponse(
    @SerializedName("Token") val token: String,
    @SerializedName("DisplayClaims") val displayClaims: DisplayClaims
)

data class MinecraftXboxLoginRequest(
    @SerializedName("identityToken") val identityToken: String  // "XBL3.0 x=<uhs>;<xstsToken>"
)

data class MinecraftXboxLoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in")   val expiresIn: Int
)

data class MinecraftProfileDto(
    @SerializedName("id")   val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("skins") val skins: List<SkinDto> = emptyList()
)

data class SkinDto(
    @SerializedName("url") val url: String,
    @SerializedName("state") val state: String
)

// ─── Mojang Version Manifest DTOs ─────────────────────────────────────────────

data class VersionManifestDto(
    @SerializedName("latest")   val latest: LatestVersions,
    @SerializedName("versions") val versions: List<VersionSummaryDto>
)

data class LatestVersions(
    @SerializedName("release")  val release: String,
    @SerializedName("snapshot") val snapshot: String
)

data class VersionSummaryDto(
    @SerializedName("id")          val id: String,
    @SerializedName("type")        val type: String,   // "release", "snapshot", etc.
    @SerializedName("url")         val url: String,
    @SerializedName("releaseTime") val releaseTime: String
)

data class VersionDetailsDto(
    @SerializedName("id")             val id: String,
    @SerializedName("type")           val type: String,
    @SerializedName("mainClass")      val mainClass: String,
    @SerializedName("javaVersion")    val javaVersion: JavaVersionDto?,
    @SerializedName("libraries")      val libraries: List<LibraryDto>,
    @SerializedName("downloads")      val downloads: VersionDownloadsDto,
    @SerializedName("assetIndex")     val assetIndex: AssetIndexDto,
    @SerializedName("assets")         val assets: String,
    @SerializedName("minecraftArguments") val minecraftArguments: String?,
    @SerializedName("arguments")      val arguments: ArgumentsDto?
)

data class JavaVersionDto(
    @SerializedName("majorVersion") val majorVersion: Int
)

data class LibraryDto(
    @SerializedName("name")      val name: String,
    @SerializedName("downloads") val downloads: LibraryDownloadsDto?,
    @SerializedName("rules")     val rules: List<LibraryRuleDto>?
)

data class LibraryDownloadsDto(
    @SerializedName("artifact")   val artifact: ArtifactDto?,
    @SerializedName("classifiers") val classifiers: Map<String, ArtifactDto>?
)

data class ArtifactDto(
    @SerializedName("path")  val path: String,
    @SerializedName("url")   val url: String,
    @SerializedName("sha1")  val sha1: String,
    @SerializedName("size")  val size: Long
)

data class LibraryRuleDto(
    @SerializedName("action") val action: String,
    @SerializedName("os")     val os: OsRuleDto?
)

data class OsRuleDto(
    @SerializedName("name") val name: String?
)

data class VersionDownloadsDto(
    @SerializedName("client") val client: ArtifactDto,
    @SerializedName("server") val server: ArtifactDto?
)

data class AssetIndexDto(
    @SerializedName("id")  val id: String,
    @SerializedName("url") val url: String,
    @SerializedName("sha1") val sha1: String,
    @SerializedName("size") val size: Long,
    @SerializedName("totalSize") val totalSize: Long
)

data class ArgumentsDto(
    @SerializedName("game") val game: List<Any>?,
    @SerializedName("jvm")  val jvm: List<Any>?
)

// ─── Modrinth DTOs ────────────────────────────────────────────────────────────

data class ModrinthSearchResultDto(
    @SerializedName("hits")       val hits: List<ModrinthProjectDto>,
    @SerializedName("total_hits") val totalHits: Int
)

data class ModrinthProjectDto(
    @SerializedName("project_id")   val projectId: String,
    @SerializedName("slug")         val slug: String,
    @SerializedName("title")        val title: String,
    @SerializedName("description")  val description: String,
    @SerializedName("author")       val author: String,
    @SerializedName("downloads")    val downloads: Long,
    @SerializedName("follows")      val follows: Long,
    @SerializedName("icon_url")     val iconUrl: String?,
    @SerializedName("categories")   val categories: List<String>,
    @SerializedName("versions")     val versions: List<String>,
    @SerializedName("latest_version") val latestVersion: String?
)

data class ModrinthVersionDto(
    @SerializedName("id")             val id: String,
    @SerializedName("project_id")     val projectId: String,
    @SerializedName("version_number") val versionNumber: String,
    @SerializedName("loaders")        val loaders: List<String>,
    @SerializedName("game_versions")  val gameVersions: List<String>,
    @SerializedName("files")          val files: List<ModrinthFileDto>
)

data class ModrinthFileDto(
    @SerializedName("url")      val url: String,
    @SerializedName("filename") val filename: String,
    @SerializedName("size")     val size: Long,
    @SerializedName("primary")  val primary: Boolean
)

// ─── Adoptium JRE DTOs ────────────────────────────────────────────────────────

data class AdoptiumAssetDto(
    @SerializedName("binary") val binary: AdoptiumBinaryDto,
    @SerializedName("version") val version: AdoptiumVersionDto
)

data class AdoptiumBinaryDto(
    @SerializedName("package") val pkg: AdoptiumPackageDto,
    @SerializedName("architecture") val architecture: String
)

data class AdoptiumPackageDto(
    @SerializedName("link") val link: String,
    @SerializedName("size") val size: Long,
    @SerializedName("checksum") val checksum: String,
    @SerializedName("name") val name: String
)

data class AdoptiumVersionDto(
    @SerializedName("semver") val semver: String,
    @SerializedName("major")  val major: Int
)
