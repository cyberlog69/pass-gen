package com.cyberlog69.passgen.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberlog69.passgen.domain.GeneratorMode
import com.cyberlog69.passgen.domain.ThemePreset
import com.cyberlog69.passgen.ui.PassGenViewModel
import com.cyberlog69.passgen.ui.components.BulkGeneratorDialog
import com.cyberlog69.passgen.ui.components.HistoryList
import com.cyberlog69.passgen.ui.components.ModeTabs
import com.cyberlog69.passgen.ui.components.PasswordControls
import com.cyberlog69.passgen.ui.components.PasswordDisplayCard
import com.cyberlog69.passgen.ui.components.PassphraseControls
import com.cyberlog69.passgen.ui.components.PinControls
import com.cyberlog69.passgen.ui.components.QrCodeDialog
import com.cyberlog69.passgen.ui.components.StrengthIndicator
import com.cyberlog69.passgen.ui.components.VaultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PassGenViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotationDegrees, label = "rotation")

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dismissToast()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PassGen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // 100% Offline Badge
                        Surface(
                            shape = RoundedCornerShape(99.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "OFFLINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openVaultDialog() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Encrypted Vault",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Theme picker dots
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemePreset.values().forEach { preset ->
                            val isSelected = state.theme == preset
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 18.dp else 14.dp)
                                    .clip(CircleShape)
                                    .background(preset.accentColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setTheme(preset) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Tabs (Password, Passphrase, PIN)
            ModeTabs(
                currentMode = state.mode,
                onModeSelected = { viewModel.setMode(it) }
            )

            // Password Output Display Card
            PasswordDisplayCard(
                text = state.currentResult?.text ?: "",
                onRegenerate = {
                    rotationDegrees += 360f
                    viewModel.generate()
                },
                onCopy = {
                    state.currentResult?.text?.let { viewModel.copyToClipboard(context, it) }
                },
                onShowQr = { viewModel.openQrDialog() },
                onSaveVault = { viewModel.saveCurrentToVault("") }
            )

            // Strength Indicator & Guidance
            state.currentResult?.let { res ->
                StrengthIndicator(
                    strengthLevel = res.strengthLevel,
                    entropyBits = res.entropyBits,
                    crackTime = res.crackTime,
                    guidance = res.guidance
                )
            }

            // Controls by Mode
            when (state.mode) {
                GeneratorMode.PASSWORD -> {
                    PasswordControls(
                        length = state.passwordLength,
                        onLengthChange = { viewModel.setPasswordLength(it) },
                        includeUpper = state.includeUpper,
                        onToggleUpper = { viewModel.toggleUpper() },
                        includeLower = state.includeLower,
                        onToggleLower = { viewModel.toggleLower() },
                        includeNumbers = state.includeNumbers,
                        onToggleNumbers = { viewModel.toggleNumbers() },
                        includeSymbols = state.includeSymbols,
                        onToggleSymbols = { viewModel.toggleSymbols() },
                        excludeAmbiguous = state.excludeAmbiguous,
                        onToggleAmbiguous = { viewModel.toggleAmbiguous() },
                        customSymbols = state.customSymbols,
                        onCustomSymbolsChange = { viewModel.setCustomSymbols(it) },
                        onResetSymbols = { viewModel.resetCustomSymbols() }
                    )
                }
                GeneratorMode.PASSPHRASE -> {
                    PassphraseControls(
                        wordCount = state.passphraseWordCount,
                        onWordCountChange = { viewModel.setPassphraseWordCount(it) },
                        separator = state.separator,
                        onSeparatorChange = { viewModel.setSeparator(it) },
                        capitalize = state.capitalizeWords,
                        onToggleCapitalize = { viewModel.toggleCapitalize() },
                        appendNumber = state.appendNumber,
                        onToggleAppendNumber = { viewModel.toggleAppendNumber() }
                    )
                }
                GeneratorMode.PIN -> {
                    PinControls(
                        length = state.pinLength,
                        onLengthChange = { viewModel.setPinLength(it) }
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        rotationDegrees += 360f
                        viewModel.generate()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Generate",
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(animatedRotation)
                    )
                    Text(
                        text = " Generate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.openBulkDialog() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Bulk",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Bulk",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            // History Section
            HistoryList(
                history = state.history,
                onToggleMask = { viewModel.toggleHistoryMask(it) },
                onCopy = { viewModel.copyToClipboard(context, it) },
                onClear = { viewModel.clearHistory() }
            )
        }

        // Modals
        if (state.isBulkDialogVisible) {
            BulkGeneratorDialog(
                mode = state.mode,
                bulkCount = state.bulkCount,
                bulkResults = state.bulkResults,
                onCountChange = { viewModel.setBulkCount(it) },
                onCopyAll = { viewModel.copyToClipboard(context, it) },
                onDismiss = { viewModel.closeBulkDialog() }
            )
        }

        if (state.isQrDialogVisible) {
            QrCodeDialog(
                text = state.currentResult?.text ?: "",
                onDismiss = { viewModel.closeQrDialog() }
            )
        }

        if (state.isVaultDialogVisible) {
            VaultDialog(
                isSetup = state.isVaultSetup,
                isUnlocked = state.isVaultUnlocked,
                vaultItems = state.vaultItems,
                currentPassword = state.currentResult?.text,
                onSetup = { viewModel.setupVault(it) },
                onUnlock = { viewModel.unlockVault(it) },
                onLock = { viewModel.lockVault() },
                onSaveCurrent = { viewModel.saveCurrentToVault(it) },
                onDeleteItem = { viewModel.deleteVaultItem(it) },
                onToggleMask = { viewModel.toggleVaultItemMask(it) },
                onCopy = { viewModel.copyToClipboard(context, it) },
                onDismiss = { viewModel.closeVaultDialog() },
                onActivity = { viewModel.resetVaultAutoLock() }
            )
        }
    }
}
