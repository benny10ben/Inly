package com.ben.inly.presentation.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.sync.SyncPairingData
import com.ben.inly.presentation.shared.sync.QrCodeDisplay
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.serialization.json.Json

@Composable
fun SyncPairingDialog(
    pairingData: SyncPairingData?,
    onDismiss: () -> Unit
) {
    if (pairingData == null) return

    val jsonString = remember(pairingData) {
        Json.encodeToString(pairingData)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Pair Mobile Device",
                fontFamily = PoppinsFont,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan this QR code using the Inly mobile app to connect securely to your desktop.",
                    fontFamily = PoppinsFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeDisplay(
                        data = jsonString,
                        size = 180,
                        modifier = Modifier
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Secret Token: ${pairingData.authToken.take(8)}...",
                    fontFamily = PoppinsFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    fontFamily = PoppinsFont,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}