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
import com.cyberlog69.passgen.domain.SecureRandomGenerator
import com.cyberlog69.passgen.domain.Separator
import com.cyberlog69.passgen.domain.ThemePreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val toastMessage: String? = null,
    val isFlagSecureEnabled: Boolean = true
)

class PassGenViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("passgen_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PassGenUiState())
    val uiState: StateFlow<PassGenUiState> = _uiState.asStateFlow()

    private var autoWipeJob: Job? = null
    private var lastCopiedText: String? = null

    init {
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
        val text: String
        val bits: Int

        when (s.mode) {
            GeneratorMode.PASSWORD -> {
                text = SecureRandomGenerator.generatePassword(
                    length = s.passwordLength,
                    includeUpper = s.includeUpper,
                    includeLower = s.includeLower,
                    includeNumbers = s.includeNumbers,
                    includeSymbols = s.includeSymbols,
                    excludeAmbiguous = s.excludeAmbiguous,
                    customSymbols = s.customSymbols
                )
                bits = SecureRandomGenerator.calculateEntropy(
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
                text = SecureRandomGenerator.generatePassphrase(
                    wordCount = s.passphraseWordCount,
                    separator = s.separator,
                    capitalize = s.capitalizeWords,
                    appendNumber = s.appendNumber
                )
                bits = SecureRandomGenerator.calculateEntropy(
                    mode = GeneratorMode.PASSPHRASE,
                    length = s.passphraseWordCount,
                    wordCount = s.passphraseWordCount,
                    includeNumber = s.appendNumber
                )
            }
            GeneratorMode.PIN -> {
                text = SecureRandomGenerator.generatePIN(s.pinLength)
                bits = SecureRandomGenerator.calculateEntropy(
                    mode = GeneratorMode.PIN,
                    length = s.pinLength
                )
            }
        }

        if (text.isEmpty()) {
            _uiState.update { it.copy(toastMessage = "Enable at least one character set") }
            return
        }

        val crackTime = SecureRandomGenerator.estimateCrackTime(bits)
        val guidance = SecureRandomGenerator.getGuidance(
            mode = s.mode,
            bits = bits,
            length = if (s.mode == GeneratorMode.PIN) s.pinLength else s.passwordLength,
            hasSymbols = s.includeSymbols,
            wordCount = s.passphraseWordCount
        )
        val strength = SecureRandomGenerator.getStrengthLevel(bits)

        val result = GenerationResult(
            text = text,
            entropyBits = bits,
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

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
