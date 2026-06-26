package com.ben.inly.presentation.shared.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.PoppinsFont

@Composable
fun InlyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                fontFamily = PoppinsFont,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        },
        singleLine = singleLine,
        textStyle = TextStyle(
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedContainerColor = if (LocalAppIsDark.current) Color(0xFF363636) else MaterialTheme.colorScheme.surface,
            focusedContainerColor = if (LocalAppIsDark.current) Color(0xFF363636) else MaterialTheme.colorScheme.surface,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    )
}