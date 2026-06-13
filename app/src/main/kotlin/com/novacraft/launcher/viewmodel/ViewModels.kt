package com.novacraft.launcher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novacraft.launcher.domain.model.*
import com.novacraft.launcher.domain.repository.*
import com.novacraft.launcher.service.launch.LaunchEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ─── Home ViewModel ───────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val newsRepository: NewsRepository,
    private val versionRepository: VersionRepository
) : ViewModel() {

    val profiles = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount = accountRepository.observeActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val news = newsRepository.observeCachedNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshingNews = MutableStateFlow(false)
    val isRefreshingNews = _isRefreshingNews.asStateFlow()

    init { refreshNews() }

    fun refreshNews() {
        viewModelScope.launch {
            _isRefreshingNews.value = true
            newsRepository.fetchNews()
            _isRefreshingNews.value = false
        }
    }

    fun launchProfile(profileId: String, context: android.content.Context) {
        viewModelScope.launch {
            val profile = profileRepository.getProfile(profileId) ?: return@launch
            Timber.d("Launching profile: ${profile.name}")
            // GameActivity.start(context, profileId)
        }
    }
}

// ─── Versions ViewModel ───────────────────────────────────────────────────────

@HiltViewModel
class VersionsViewModel @Inject constructor(
    private val versionRepository: VersionRepository
) : ViewModel() {

    private val _availableVersions = MutableStateFlow<List<GameVersion>>(emptyList())
    val availableVersions = _availableVersions.asStateFlow()

    val installedVersions = versionRepository.observeInstalledVersions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Filters
    private val _selectedLoader = MutableStateFlow(LoaderType.VANILLA)
    val selectedLoader = _selectedLoader.asStateFlow()

    private val _selectedReleaseType = MutableStateFlow<ReleaseType?>(ReleaseType.RELEASE)
    val selectedReleaseType = _selectedReleaseType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredVersions: StateFlow<List<GameVersion>> = combine(
        _availableVersions, _selectedLoader, _selectedReleaseType, _searchQuery
    ) { versions, loader, releaseType, query ->
        versions.filter { v ->
            (loader == LoaderType.VANILLA || v.loaderType == loader) &&
            (releaseType == null || v.releaseType == releaseType) &&
            (query.isBlank() || v.minecraftVersion.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Install progress map: versionId -> 0..1
    private val _installProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val installProgress = _installProgress.asStateFlow()

    init { fetchVersions() }

    fun fetchVersions() {
        viewModelScope.launch {
            _isLoading.value = true
            versionRepository.fetchAvailableVersions()
                .onSuccess { _availableVersions.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun installVersion(version: GameVersion) {
        viewModelScope.launch {
            versionRepository.installVersion(version) { progress ->
                _installProgress.value = _installProgress.value + (version.id to progress)
            }.onFailure { _error.value = "Install failed: ${it.message}" }
            _installProgress.value = _installProgress.value - version.id
        }
    }

    fun setLoader(loader: LoaderType) { _selectedLoader.value = loader }
    fun setReleaseType(type: ReleaseType?) { _selectedReleaseType.value = type }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun clearError() { _error.value = null }
}

// ─── Mods ViewModel ──────────────────────────────────────────────────────────

@HiltViewModel
class ModsViewModel @Inject constructor(
    private val modRepository: ModRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId = _selectedProfileId.asStateFlow()

    val profiles = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedMods: StateFlow<List<Mod>> = _selectedProfileId.flatMapLatest { profileId ->
        if (profileId != null) modRepository.observeInstalledMods(profileId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchResults = MutableStateFlow<List<Mod>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _installProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val installProgress = _installProgress.asStateFlow()

    fun selectProfile(profileId: String) { _selectedProfileId.value = profileId }

    fun searchMods(query: String, loaderType: LoaderType = LoaderType.FABRIC, mcVersion: String = "1.21.1") {
        viewModelScope.launch {
            _isSearching.value = true
            _searchQuery.value = query
            modRepository.searchMods(query, loaderType, mcVersion)
                .onSuccess { _searchResults.value = it }
                .onFailure { _error.value = it.message }
            _isSearching.value = false
        }
    }

    fun installMod(mod: Mod) {
        val profileId = _selectedProfileId.value ?: return
        viewModelScope.launch {
            modRepository.installMod(mod, profileId) { progress ->
                _installProgress.value = _installProgress.value + (mod.id to progress)
            }
            _installProgress.value = _installProgress.value - mod.id
        }
    }

    fun toggleMod(modId: String, enabled: Boolean) {
        val profileId = _selectedProfileId.value ?: return
        viewModelScope.launch { modRepository.toggleMod(modId, profileId, enabled) }
    }

    fun uninstallMod(modId: String) {
        val profileId = _selectedProfileId.value ?: return
        viewModelScope.launch { modRepository.uninstallMod(modId, profileId) }
    }
}

// ─── Accounts ViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accounts = accountRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAccount = accountRepository.observeActiveAccount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn = _isLoggingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog = _showAddDialog.asStateFlow()

    fun addOfflineAccount(username: String) {
        viewModelScope.launch {
            val account = Account(
                username = username,
                uuid = generateOfflineUuid(username),
                type = AccountType.OFFLINE,
                isActive = accounts.value.isEmpty()
            )
            accountRepository.addAccount(account)
        }
    }

    fun loginMicrosoft(authCode: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            accountRepository.loginMicrosoft(authCode)
                .onSuccess { accountRepository.addAccount(it) }
                .onFailure { _error.value = "Login failed: ${it.message}" }
            _isLoggingIn.value = false
        }
    }

    fun setActive(id: String) {
        viewModelScope.launch { accountRepository.setActiveAccount(id) }
    }

    fun removeAccount(id: String) {
        viewModelScope.launch { accountRepository.removeAccount(id) }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }
    fun clearError() { _error.value = null }

    private fun generateOfflineUuid(username: String): String {
        // Same algorithm as Minecraft offline: md5("OfflinePlayer:<username>")
        val input = "OfflinePlayer:$username"
        val md5 = java.security.MessageDigest.getInstance("MD5").digest(input.toByteArray())
        md5[6] = (md5[6].toInt() and 0x0f or 0x30).toByte()
        md5[8] = (md5[8].toInt() and 0x3f or 0x80).toByte()
        return buildString {
            for (i in md5.indices) {
                if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
                append(String.format("%02x", md5[i]))
            }
        }
    }
}

// ─── Settings ViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            _isSaving.value = true
            settingsRepository.updateSettings(settings)
            _isSaving.value = false
        }
    }

    fun exportSettings(destPath: String) {
        viewModelScope.launch { settingsRepository.exportSettings(destPath) }
    }

    fun importSettings(srcPath: String) {
        viewModelScope.launch { settingsRepository.importSettings(srcPath) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepository.resetSettings() }
    }
}

// ─── Play ViewModel ──────────────────────────────────────────────────────────

@HiltViewModel
class PlayViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
    private val versionRepository: VersionRepository,
    private val javaRepository: JavaRepository,
    private val launchEngine: LaunchEngine
) : ViewModel() {

    val profiles = profileRepository.observeProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val installedVersions = versionRepository.observeInstalledVersions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId = _selectedProfileId.asStateFlow()

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Idle)
    val launchState = _launchState.asStateFlow()

    fun selectProfile(id: String) { _selectedProfileId.value = id }

    fun launch(context: android.content.Context) {
        viewModelScope.launch {
            val profileId = _selectedProfileId.value ?: return@launch
            val profile = profileRepository.getProfile(profileId) ?: return@launch
            val account  = accountRepository.observeActiveAccount().firstOrNull() ?: run {
                _launchState.value = LaunchState.Error("No active account. Please add an account.")
                return@launch
            }
            val version = versionRepository.getVersion(profile.versionId) ?: run {
                _launchState.value = LaunchState.Error("Version not found. Please install it first.")
                return@launch
            }
            if (!version.isInstalled) {
                _launchState.value = LaunchState.Error("Version not installed. Please install ${version.id}.")
                return@launch
            }

            _launchState.value = LaunchState.Launching("Starting ${profile.name}...")
            try {
                // GameActivity.start(context, profileId)
                _launchState.value = LaunchState.Running
            } catch (e: Exception) {
                _launchState.value = LaunchState.Error("Launch failed: ${e.message}")
            }
        }
    }

    fun createProfile(name: String, versionId: String) {
        viewModelScope.launch {
            val profile = GameProfile(name = name, versionId = versionId)
            profileRepository.createProfile(profile)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch { profileRepository.deleteProfile(id) }
    }

    fun clearLaunchState() { _launchState.value = LaunchState.Idle }
}

sealed class LaunchState {
    object Idle : LaunchState()
    data class Launching(val message: String) : LaunchState()
    object Running : LaunchState()
    data class Error(val message: String) : LaunchState()
}
