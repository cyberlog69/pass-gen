package com.cyberlog69.passgen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberlog69.passgen.domain.GuidanceStatus
import com.cyberlog69.passgen.domain.StrengthGuidance
import com.cyberlog69.passgen.domain.StrengthLevel

@Composable
fun StrengthIndicator(
    strengthLevel: StrengthLevel,
    entropyBits: Int,
    crackTime: String,
    guidance: StrengthGuidance?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label + Strength Text
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STRENGTH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            val animatedColor by animateColorAsState(
                targetValue = strengthLevel.color,
                label = "strength_color"
            )

            Text(
                text = strengthLevel.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = animatedColor
            )
        }

        // 4 Segments Track
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..4) {
                val isActive = i <= strengthLevel.segments
                val segColor by animateColorAsState(
                    targetValue = if (isActive) strengthLevel.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    label = "seg_$i"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(segColor)
                )
            }
        }

        // Metrics Row (Entropy & Crack Time)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "~$entropyBits bits entropy",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Time to crack: $crackTime",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Guidance Box
        if (guidance != null) {
            val (bgColor, borderColor) = when (guidance.status) {
                GuidanceStatus.GOOD -> Pair(Color(0x1522C55E), Color(0x4022C55E))
                GuidanceStatus.WARN -> Pair(Color(0x15EAB308), Color(0x40EAB308))
                GuidanceStatus.ALERT -> Pair(Color(0x15EF4444), Color(0x40EF4444))
            }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = guidance.icon, fontSize = 14.sp)
                    Text(
                        text = guidance.text,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
