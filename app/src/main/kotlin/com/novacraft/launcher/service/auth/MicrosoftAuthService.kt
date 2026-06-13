package com.novacraft.launcher.service.auth

import com.novacraft.launcher.data.remote.api.AuthApi
import com.novacraft.launcher.data.remote.dto.*
import com.novacraft.launcher.domain.model.Account
import com.novacraft.launcher.domain.model.AccountType
import com.novacraft.launcher.util.Constants
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MicrosoftAuthService
 *
 * Implements the full Microsoft → XBL → XSTS → Minecraft authentication chain.
 * Follows the spec documented at:
 * https://wiki.vg/Microsoft_Authentication_Scheme
 */
@Singleton
class MicrosoftAuthService @Inject constructor(
    private val api: AuthApi
) {

    /**
     * Full login flow from OAuth2 authorization code.
     *
     * @param authCode  Code received from Microsoft OAuth2 consent screen.
     * @return Result wrapping the authenticated Account.
     */
    suspend fun loginWithCode(authCode: String): Result<Account> = runCatching {
        Timber.d("Starting Microsoft auth flow")

        // Step 1: Exchange code for Microsoft access token
        val msToken = api.getMicrosoftToken(
            clientId    = Constants.MS_CLIENT_ID,
            redirectUri = Constants.MS_REDIRECT_URI,
            grantType   = "authorization_code",
            code        = authCode
        ).body() ?: error("Failed to get Microsoft token")

        Timber.d("Got Microsoft token, authenticating with XBL...")

        // Step 2: Xbox Live authentication
        val xblResp = api.authenticateXbl(
            XblAuthRequest(
                properties = XblProperties(rpsTicket = "d=${msToken.accessToken}")
            )
        ).body() ?: error("XBL authentication failed")
        val xblToken = xblResp.token
        val userHash  = xblResp.displayClaims.xui.firstOrNull()?.userHash
            ?: error("No user hash from XBL")

        Timber.d("XBL authenticated, getting XSTS token...")

        // Step 3: XSTS authorization
        val xstsResp = api.authorizeXsts(
            XstsAuthRequest(
                properties = XstsProperties(userTokens = listOf(xblToken))
            )
        ).body() ?: error("XSTS authorization failed")
        val xstsToken = xstsResp.token

        Timber.d("XSTS authorized, logging into Minecraft...")

        // Step 4: Minecraft login
        val mcResp = api.loginWithXbox(
            MinecraftXboxLoginRequest(
                identityToken = "XBL3.0 x=$userHash;$xstsToken"
            )
        ).body() ?: error("Minecraft login failed")

        Timber.d("Minecraft login successful, fetching profile...")

        // Step 5: Get Minecraft profile
        val profile = api.getMinecraftProfile("Bearer ${mcResp.accessToken}")
            .body() ?: error("Failed to fetch Minecraft profile")

        Timber.d("Full auth flow complete for: ${profile.name}")

        Account(
            username     = profile.name,
            uuid         = profile.id,
            type         = AccountType.MICROSOFT,
            accessToken  = mcResp.accessToken,
            refreshToken = msToken.refreshToken,
            tokenExpiry  = System.currentTimeMillis() + mcResp.expiresIn * 1000L,
            avatarUrl    = "https://crafatar.com/avatars/${profile.id}?size=64&overlay=true",
            isActive     = true
        )
    }

    /**
     * Refresh an expired Minecraft access token using the stored Microsoft refresh token.
     */
    suspend fun refreshToken(microsoftRefreshToken: String): MinecraftXboxLoginResponse {
        val msToken = api.refreshMicrosoftToken(
            clientId     = Constants.MS_CLIENT_ID,
            refreshToken = microsoftRefreshToken
        ).body() ?: error("Failed to refresh Microsoft token")

        val xblResp = api.authenticateXbl(
            XblAuthRequest(properties = XblProperties(rpsTicket = "d=${msToken.accessToken}"))
        ).body() ?: error("XBL re-authentication failed")

        val xstsResp = api.authorizeXsts(
            XstsAuthRequest(properties = XstsProperties(userTokens = listOf(xblResp.token)))
        ).body() ?: error("XSTS re-authorization failed")

        val userHash = xblResp.displayClaims.xui.firstOrNull()?.userHash ?: error("No user hash")

        return api.loginWithXbox(
            MinecraftXboxLoginRequest("XBL3.0 x=$userHash;${xstsResp.token}")
        ).body() ?: error("Minecraft token refresh failed")
    }

    /**
     * Build the Microsoft OAuth2 consent URL for the WebView login flow.
     */
    fun buildAuthUrl(): String {
        val scopes = "XboxLive.signin%20offline_access"
        return "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize" +
               "?client_id=${Constants.MS_CLIENT_ID}" +
               "&response_type=code" +
               "&redirect_uri=${Constants.MS_REDIRECT_URI}" +
               "&scope=$scopes" +
               "&prompt=select_account"
    }
}
