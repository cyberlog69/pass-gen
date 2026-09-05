package com.cyberlog69.passgen.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PasswordControls(
    length: Int,
    onLengthChange: (Int) -> Unit,
    includeUpper: Boolean,
    onToggleUpper: () -> Unit,
    includeLower: Boolean,
    onToggleLower: () -> Unit,
    includeNumbers: Boolean,
    onToggleNumbers: () -> Unit,
    includeSymbols: Boolean,
    onToggleSymbols: () -> Unit,
    excludeAmbiguous: Boolean,
    onToggleAmbiguous: () -> Unit,
    customSymbols: String,
    onCustomSymbolsChange: (String) -> Unit,
    onResetSymbols: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Length Slider & Number display
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PASSWORD LENGTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "$length",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = length.toFloat(),
                onValueChange = { onLengthChange(it.toInt()) },
                valueRange = 4f..128f,
                steps = 124,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Toggles Grid (2x2)
        Text(
            text = "CHARACTER TYPES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleCard(
                title = "Uppercase (A-Z)",
                checked = includeUpper,
                onCheckedChange = onToggleUpper,
                modifier = Modifier.weight(1f)
            )
            ToggleCard(
                title = "Lowercase (a-z)",
                checked = includeLower,
                onCheckedChange = onToggleLower,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleCard(
                title = "Numbers (0-9)",
                checked = includeNumbers,
                onCheckedChange = onToggleNumbers,
                modifier = Modifier.weight(1f)
            )
            ToggleCard(
                title = "Symbols",
                checked = includeSymbols,
                onCheckedChange = onToggleSymbols,
                modifier = Modifier.weight(1f)
            )
        }

        // Full Width Toggle: Exclude Ambiguous
        ToggleCard(
            title = "Exclude Ambiguous (l, 1, I, O, 0, o)",
            checked = excludeAmbiguous,
            onCheckedChange = onToggleAmbiguous,
            modifier = Modifier.fillMaxWidth()
        )

        // Custom Symbols
        if (includeSymbols) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ALLOWED SYMBOLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    TextButton(onClick = onResetSymbols) {
                        Text(text = "Reset Default", fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = customSymbols,
                    onValueChange = onCustomSymbolsChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.clickable { onCheckedChange() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}
