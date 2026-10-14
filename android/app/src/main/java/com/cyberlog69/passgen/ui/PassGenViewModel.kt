package com.cyberlog69.passgen.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberlog69.passgen.domain.GenerationResult
import com.cyberlog69.passgen.domain.GeneratorMode
import com.cyberlog69.passgen.domain.HistoryItem
import com.cyberlog69.passgen.domain.GuidanceStatus
import com.cyberlog69.passgen.domain.LeakedPasswordChecker
import com.cyberlog69.passgen.domain.SecureRandomGenerator
import com.cyberlog69.passgen.domain.Separator
import com.cyberlog69.passgen.domain.StrengthGuidance
import com.cyberlog69.passgen.domain.ThemePreset
import com.cyberlog69.passgen.domain.VaultCrypto
import com.cyberlog69.passgen.domain.VaultItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class PassGenUiState(
    val mode: GeneratorMode = GeneratorMode.PASSWORD,
    val passwordLength: Int = 16,
    val passphraseWordCount: Int = 4,
    val pinLength: Int = 6,
    val includeUpper: Boolean = true,
    val includeLower: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val excludeAmbiguous: Boolean = false,
    val customSymbols: String = SecureRandomGenerator.DEFAULT_SYMBOLS,
    val separator: Separator = Separator.HYPHEN,
    val capitalizeWords: Boolean = true,
    val appendNumber: Boolean = false,
    val theme: ThemePreset = ThemePreset.VIOLET,
    val modeResults: Map<GeneratorMode, GenerationResult> = emptyMap(),
    val currentResult: GenerationResult? = null,
    val history: List<HistoryItem> = emptyList(),
    val isQrDialogVisible: Boolean = false,
    val isBulkDialogVisible: Boolean = false,
    val bulkCount: Int = 10,
    val bulkResults: List<String> = emptyList(),
    val isVaultDialogVisible: Boolean = false,
    val isVaultSetup: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val vaultItems: List<VaultItem> = emptyList(),
    val isUpdateDialogVisible: Boolean = false,
    val toastMessage: String? = null,
    val isFlagSecureEnabled: Boolean = true
)

class PassGenViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("passgen_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PassGenUiState())
    val uiState: StateFlow<PassGenUiState> = _uiState.asStateFlow()

    private var autoWipeJob: Job? = null
    private var lastCopiedText: String? = null
    private val devicePepper: String
    private val pastHashes = mutableSetOf<String>()
    private var currentVaultPasscode: String? = null
    private var vaultAutoLockJob: Job? = null

    init {
        var pepper = prefs.getString("device_pepper", null)
        if (pepper == null || pepper.length != 64) {
            pepper = VaultCrypto.generatePepper()
            prefs.edit().putString("device_pepper", pepper).apply()
        }
        devicePepper = pepper

        val savedHashes = prefs.getStringSet("past_hashes", emptySet()) ?: emptySet()
        pastHashes.addAll(savedHashes)

        loadSettings()
        generate()
    }

    private fun loadSettings() {
        val savedMode = prefs.getString("mode", GeneratorMode.PASSWORD.name)?.let {
            try { GeneratorMode.valueOf(it) } catch (_: Exception) { GeneratorMode.PASSWORD }
        } ?: GeneratorMode.PASSWORD

        val savedTheme = prefs.getString("theme", ThemePreset.VIOLET.name)?.let {
            try { ThemePreset.valueOf(it) } catch (_: Exception) { ThemePreset.VIOLET }
        } ?: ThemePreset.VIOLET

        val savedSep = prefs.getString("separator", Separator.HYPHEN.name)?.let {
            try { Separator.valueOf(it) } catch (_: Exception) { Separator.HYPHEN }
        } ?: Separator.HYPHEN

        val hasVault = prefs.contains("vault_bundle")

        _uiState.update {
            it.copy(
                mode = savedMode,
                passwordLength = prefs.getInt("passwordLength", 16).coerceIn(4, 128),
                passphraseWordCount = prefs.getInt("passphraseWordCount", 4).coerceIn(3, 10),
                pinLength = prefs.getInt("pinLength", 6).coerceIn(4, 16),
                includeUpper = prefs.getBoolean("includeUpper", true),
                includeLower = prefs.getBoolean("includeLower", true),
                includeNumbers = prefs.getBoolean("includeNumbers", true),
                includeSymbols = prefs.getBoolean("includeSymbols", true),
                excludeAmbiguous = prefs.getBoolean("excludeAmbiguous", false),
                customSymbols = prefs.getString("customSymbols", SecureRandomGenerator.DEFAULT_SYMBOLS) ?: SecureRandomGenerator.DEFAULT_SYMBOLS,
                separator = savedSep,
                capitalizeWords = prefs.getBoolean("capitalizeWords", true),
                appendNumber = prefs.getBoolean("appendNumber", false),
                theme = savedTheme,
                isVaultSetup = hasVault,
                isFlagSecureEnabled = prefs.getBoolean("flagSecure", true)
            )
        }
    }

    private fun saveSettings() {
        val s = _uiState.value
        prefs.edit()
            .putString("mode", s.mode.name)
            .putInt("passwordLength", s.passwordLength)
            .putInt("passphraseWordCount", s.passphraseWordCount)
            .putInt("pinLength", s.pinLength)
            .putBoolean("includeUpper", s.includeUpper)
            .putBoolean("includeLower", s.includeLower)
            .putBoolean("includeNumbers", s.includeNumbers)
            .putBoolean("includeSymbols", s.includeSymbols)
            .putBoolean("excludeAmbiguous", s.excludeAmbiguous)
            .putString("customSymbols", s.customSymbols)
            .putString("separator", s.separator.name)
            .putBoolean("capitalizeWords", s.capitalizeWords)
            .putBoolean("appendNumber", s.appendNumber)
            .putString("theme", s.theme.name)
            .putBoolean("flagSecure", s.isFlagSecureEnabled)
            .apply()
    }

    fun generate() {
        val s = _uiState.value
        var text = ""
        var bits = 0

        for (attempt in 0 until 10) {
            val candidateText: String
            val candidateBits: Int
            when (s.mode) {
                GeneratorMode.PASSWORD -> {
                    candidateText = SecureRandomGenerator.generatePassword(
                        length = s.passwordLength,
                        includeUpper = s.includeUpper,
                        includeLower = s.includeLower,
                        includeNumbers = s.includeNumbers,
                        includeSymbols = s.includeSymbols,
                        excludeAmbiguous = s.excludeAmbiguous,
                        customSymbols = s.customSymbols
                    )
                    candidateBits = SecureRandomGenerator.calculateEntropy(
                        mode = GeneratorMode.PASSWORD,
                        length = s.passwordLength,
                        includeUpper = s.includeUpper,
                        includeLower = s.includeLower,
                        includeNumbers = s.includeNumbers,
                        includeSymbols = s.includeSymbols,
                        excludeAmbiguous = s.excludeAmbiguous,
                        customSymbols = s.customSymbols
                    )
                }
                GeneratorMode.PASSPHRASE -> {
                    candidateText = SecureRandomGenerator.generatePassphrase(
                        wordCount = s.passphraseWordCount,
                        separator = s.separator,
                        capitalize = s.capitalizeWords,
                        appendNumber = s.appendNumber
                    )
                    candidateBits = SecureRandomGenerator.calculateEntropy(
                        mode = GeneratorMode.PASSPHRASE,
                        length = s.passphraseWordCount,
                        wordCount = s.passphraseWordCount,
                        includeNumber = s.appendNumber
                    )
                }
                GeneratorMode.PIN -> {
                    candidateText = SecureRandomGenerator.generatePIN(s.pinLength)
                    candidateBits = SecureRandomGenerator.calculateEntropy(
                        mode = GeneratorMode.PIN,
                        length = s.pinLength
                    )
                }
            }

            if (candidateText.isEmpty()) {
                _uiState.update { it.copy(toastMessage = "Enable at least one character set") }
                return
            }

            val hash = VaultCrypto.hashPassword(devicePepper, candidateText)
            if (!pastHashes.contains(hash)) {
                pastHashes.add(hash)
                if (pastHashes.size > 2000) {
                    val excess = pastHashes.size - 2000
                    val toRemove = pastHashes.take(excess)
                    pastHashes.removeAll(toRemove.toSet())
                }
                prefs.edit().putStringSet("past_hashes", pastHashes).apply()
                text = candidateText
                bits = candidateBits
                break
            } else if (attempt == 9) {
                text = candidateText
                bits = candidateBits
            }
        }

        val leakResult = LeakedPasswordChecker.check(text)
        val finalBits = if (leakResult.isLeaked) minOf(bits, 20) else bits
        val guidance = if (leakResult.isLeaked) {
            StrengthGuidance(
                status = GuidanceStatus.ALERT,
                icon = "⚠️",
                text = "Compromised! " + (leakResult.reason ?: "Found in breach digests. Regenerate recommended.")
            )
        } else {
            SecureRandomGenerator.getGuidance(
                mode = s.mode,
                bits = bits,
                length = if (s.mode == GeneratorMode.PIN) s.pinLength else s.passwordLength,
                hasSymbols = s.includeSymbols,
                wordCount = s.passphraseWordCount
            )
        }
        val crackTime = SecureRandomGenerator.estimateCrackTime(finalBits)
        val strength = SecureRandomGenerator.getStrengthLevel(finalBits)

        val result = GenerationResult(
            text = text,
            entropyBits = finalBits,
            crackTime = crackTime,
            guidance = guidance,
            strengthLevel = strength
        )

        val newModeResults = s.modeResults + (s.mode to result)
        val updatedHistory = if (s.history.firstOrNull()?.text != text) {
            (listOf(HistoryItem(text = text, isMasked = true)) + s.history).take(5)
        } else {
            s.history
        }

        _uiState.update {
            it.copy(
                modeResults = newModeResults,
                currentResult = result,
                history = updatedHistory
            )
        }
    }

    fun setMode(mode: GeneratorMode) {
        _uiState.update { state ->
            val existing = state.modeResults[mode]
            state.copy(
                mode = mode,
                currentResult = existing ?: state.currentResult
            )
        }
        saveSettings()
        // If this mode does not have a generated result yet, generate the initial one
        if (!_uiState.value.modeResults.containsKey(mode)) {
            generate()
        }
    }

    fun setPasswordLength(len: Int) {
        _uiState.update { it.copy(passwordLength = len.coerceIn(4, 128)) }
        saveSettings()
    }

    fun setPassphraseWordCount(count: Int) {
        _uiState.update { it.copy(passphraseWordCount = count.coerceIn(3, 10)) }
        saveSettings()
    }

    fun setPinLength(len: Int) {
        _uiState.update { it.copy(pinLength = len.coerceIn(4, 16)) }
        saveSettings()
    }

    fun toggleUpper() {
        _uiState.update { it.copy(includeUpper = !it.includeUpper) }
        saveSettings()
    }

    fun toggleLower() {
        _uiState.update { it.copy(includeLower = !it.includeLower) }
        saveSettings()
    }

    fun toggleNumbers() {
        _uiState.update { it.copy(includeNumbers = !it.includeNumbers) }
        saveSettings()
    }

    fun toggleSymbols() {
        _uiState.update { it.copy(includeSymbols = !it.includeSymbols) }
        saveSettings()
    }

    fun toggleAmbiguous() {
        _uiState.update { it.copy(excludeAmbiguous = !it.excludeAmbiguous) }
        saveSettings()
    }

    fun setCustomSymbols(symbols: String) {
        _uiState.update { it.copy(customSymbols = symbols) }
        saveSettings()
    }

    fun resetCustomSymbols() {
        _uiState.update { it.copy(customSymbols = SecureRandomGenerator.DEFAULT_SYMBOLS) }
        saveSettings()
    }

    fun setSeparator(sep: Separator) {
        _uiState.update { it.copy(separator = sep) }
        saveSettings()
    }

    fun toggleCapitalize() {
        _uiState.update { it.copy(capitalizeWords = !it.capitalizeWords) }
        saveSettings()
    }

    fun toggleAppendNumber() {
        _uiState.update { it.copy(appendNumber = !it.appendNumber) }
        saveSettings()
    }

    fun setTheme(theme: ThemePreset) {
        _uiState.update { it.copy(theme = theme) }
        saveSettings()
    }

    fun toggleHistoryMask(id: String) {
        _uiState.update { s ->
            s.copy(history = s.history.map {
                if (it.id == id) it.copy(isMasked = !it.isMasked) else it
            })
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList(), toastMessage = "History cleared") }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("password", text)

        // Android 13+ (API 33) Privacy Flag: Mark as sensitive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        clipboard.setPrimaryClip(clip)
        lastCopiedText = text

        _uiState.update { it.copy(toastMessage = "Copied! Auto-clears in 30s") }

        // Start 30s auto-wipe
        autoWipeJob?.cancel()
        autoWipeJob = viewModelScope.launch {
            delay(30_000)
            val currentClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (currentClip == lastCopiedText) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                _uiState.update { it.copy(toastMessage = "Clipboard auto-cleared for privacy") }
            }
        }
    }

    fun openQrDialog() {
        _uiState.update { it.copy(isQrDialogVisible = true) }
    }

    fun closeQrDialog() {
        _uiState.update { it.copy(isQrDialogVisible = false) }
    }

    fun openUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogVisible = true) }
    }

    fun closeUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogVisible = false) }
    }

    fun openBulkDialog() {
        val results = generateBulkList(_uiState.value.bulkCount)
        _uiState.update {
            it.copy(
                isBulkDialogVisible = true,
                bulkResults = results
            )
        }
    }

    fun closeBulkDialog() {
        _uiState.update { it.copy(isBulkDialogVisible = false) }
    }

    fun setBulkCount(count: Int) {
        val validCount = count.coerceIn(5, 50)
        val results = generateBulkList(validCount)
        _uiState.update {
            it.copy(
                bulkCount = validCount,
                bulkResults = results
            )
        }
    }

    private fun generateBulkList(count: Int): List<String> {
        val s = _uiState.value
        return (0 until count).map {
            when (s.mode) {
                GeneratorMode.PASSWORD -> SecureRandomGenerator.generatePassword(
                    length = s.passwordLength,
                    includeUpper = s.includeUpper,
                    includeLower = s.includeLower,
                    includeNumbers = s.includeNumbers,
                    includeSymbols = s.includeSymbols,
                    excludeAmbiguous = s.excludeAmbiguous,
                    customSymbols = s.customSymbols
                )
                GeneratorMode.PASSPHRASE -> SecureRandomGenerator.generatePassphrase(
                    wordCount = s.passphraseWordCount,
                    separator = s.separator,
                    capitalize = s.capitalizeWords,
                    appendNumber = s.appendNumber
                )
                GeneratorMode.PIN -> SecureRandomGenerator.generatePIN(s.pinLength)
            }
        }
    }

    fun toggleFlagSecure() {
        _uiState.update { it.copy(isFlagSecureEnabled = !it.isFlagSecureEnabled) }
        saveSettings()
    }

    fun openVaultDialog() {
        val hasVault = prefs.contains("vault_bundle")
        _uiState.update { it.copy(isVaultDialogVisible = true, isVaultSetup = hasVault) }
        resetVaultAutoLock()
    }

    fun closeVaultDialog() {
        _uiState.update { it.copy(isVaultDialogVisible = false) }
    }

    fun setupVault(passcode: String) {
        if (passcode.length < 6) {
            _uiState.update { it.copy(toastMessage = "Passcode must be at least 6 characters") }
            return
        }
        try {
            val emptyList = emptyList<VaultItem>()
            val bundle = VaultCrypto.encryptItems(emptyList, passcode)
            prefs.edit().putString("vault_bundle", bundle.toJson()).apply()
            currentVaultPasscode = passcode
            _uiState.update {
                it.copy(
                    isVaultSetup = true,
                    isVaultUnlocked = true,
                    vaultItems = emptyList,
                    toastMessage = "Vault initialized with AES-256-GCM encryption"
                )
            }
            resetVaultAutoLock()
        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Failed to setup vault: ${e.message}") }
        }
    }

    fun unlockVault(passcode: String) {
        val bundleJson = prefs.getString("vault_bundle", null)
        if (bundleJson == null) {
            _uiState.update { it.copy(toastMessage = "No vault found. Please set up first.") }
            return
        }
        val bundle = VaultCrypto.EncryptedBundle.fromJson(bundleJson)
        if (bundle == null) {
            _uiState.update { it.copy(toastMessage = "Vault data is corrupted.") }
            return
        }

        try {
            val items = VaultCrypto.decryptItems(bundle, passcode)
            currentVaultPasscode = passcode
            _uiState.update {
                it.copy(
                    isVaultUnlocked = true,
                    vaultItems = items,
                    toastMessage = "Vault unlocked successfully"
                )
            }
            resetVaultAutoLock()
        } catch (_: Exception) {
            _uiState.update { it.copy(toastMessage = "Incorrect master passcode") }
        }
    }

    fun lockVault() {
        currentVaultPasscode = null
        vaultAutoLockJob?.cancel()
        vaultAutoLockJob = null
        _uiState.update {
            it.copy(
                isVaultUnlocked = false,
                vaultItems = emptyList(),
                toastMessage = "Vault locked"
            )
        }
    }

    fun saveCurrentToVault(title: String) {
        val currentPass = _uiState.value.currentResult?.text
        if (currentPass.isNullOrEmpty()) {
            _uiState.update { it.copy(toastMessage = "Generate a password first") }
            return
        }
        if (!_uiState.value.isVaultUnlocked || currentVaultPasscode == null) {
            openVaultDialog()
            _uiState.update { it.copy(toastMessage = "Unlock or set up vault to save") }
            return
        }

        val passcode = currentVaultPasscode!!
        val cleanTitle = if (title.isBlank()) {
            "Password (${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())})"
        } else {
            title.trim()
        }
        val newItem = VaultItem(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            password = currentPass,
            isMasked = true,
            createdAt = System.currentTimeMillis()
        )

        val updatedItems = listOf(newItem) + _uiState.value.vaultItems
        try {
            val bundle = VaultCrypto.encryptItems(updatedItems, passcode)
            prefs.edit().putString("vault_bundle", bundle.toJson()).apply()
            _uiState.update {
                it.copy(
                    vaultItems = updatedItems,
                    toastMessage = "Saved \"$cleanTitle\" to encrypted vault!"
                )
            }
            resetVaultAutoLock()
        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Failed to save: ${e.message}") }
        }
    }

    fun deleteVaultItem(id: String) {
        val passcode = currentVaultPasscode ?: return
        val updatedItems = _uiState.value.vaultItems.filter { it.id != id }
        try {
            val bundle = VaultCrypto.encryptItems(updatedItems, passcode)
            prefs.edit().putString("vault_bundle", bundle.toJson()).apply()
            _uiState.update {
                it.copy(
                    vaultItems = updatedItems,
                    toastMessage = "Deleted item from vault"
                )
            }
            resetVaultAutoLock()
        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Failed to delete: ${e.message}") }
        }
    }

    fun toggleVaultItemMask(id: String) {
        _uiState.update { s ->
            s.copy(vaultItems = s.vaultItems.map {
                if (it.id == id) it.copy(isMasked = !it.isMasked) else it
            })
        }
        resetVaultAutoLock()
    }

    fun resetVaultAutoLock() {
        if (!_uiState.value.isVaultUnlocked) return
        vaultAutoLockJob?.cancel()
        vaultAutoLockJob = viewModelScope.launch {
            delay(60_000)
            lockVault()
            _uiState.update { it.copy(toastMessage = "Vault auto-locked due to inactivity") }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun exportVaultBackup(context: Context) {
        val passcode = currentVaultPasscode
        if (passcode == null || !_uiState.value.isVaultUnlocked) {
            _uiState.update { it.copy(toastMessage = "Unlock vault to export") }
            return
        }
        try {
            val json = VaultCrypto.exportBackup(_uiState.value.vaultItems, passcode)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PassGen Vault Backup", json)
            clipboard.setPrimaryClip(clip)
            _uiState.update { it.copy(toastMessage = "Encrypted backup copied to clipboard!") }
            resetVaultAutoLock()
        } catch (e: Exception) {
            _uiState.update { it.copy(toastMessage = "Export failed: ${e.message}") }
        }
    }

    fun importVaultBackup(backupJson: String, masterPasscode: String, merge: Boolean) {
        try {
            val imported = VaultCrypto.importBackup(backupJson, masterPasscode)
            val currentItems = _uiState.value.vaultItems
            val finalItems = if (merge && currentItems.isNotEmpty()) {
                val existingIds = currentItems.map { it.id }.toSet()
                imported.filter { it.id !in existingIds } + currentItems
            } else {
                imported
            }

            val bundle = VaultCrypto.encryptItems(finalItems, masterPasscode)
            prefs.edit().putString("vault_bundle", bundle.toJson()).apply()
            currentVaultPasscode = masterPasscode

            _uiState.update {
                it.copy(
                    isVaultSetup = true,
                    isVaultUnlocked = true,
                    vaultItems = finalItems,
                    toastMessage = "Restored ${imported.size} items from backup!"
                )
            }
            resetVaultAutoLock()
        } catch (_: Exception) {
            _uiState.update { it.copy(toastMessage = "Decryption failed: Incorrect passcode or invalid backup") }
        }
    }
}
