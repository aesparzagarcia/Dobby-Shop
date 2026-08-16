package com.ares.ewe_shop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ares.ewe_shop.core.theme.DobbyShopColors

/**
 * Seis círculos de dígitos, respaldados por un campo de texto oculto para pegar / autocompletar.
 */
@Composable
fun SixDigitCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
) {
    val digits = value.filter { it.isDigit() }.take(6)
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(autoFocus) {
        if (autoFocus && enabled) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
            ) {
                focusRequester.requestFocus()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (index in 0 until 6) {
                val digit = digits.getOrNull(index)?.toString().orEmpty()
                val isActive = isFocused && index == digits.length && digits.length < 6
                DigitCircle(digit = digit, isActive = isActive)
            }
        }

        BasicTextField(
            value = digits,
            onValueChange = { raw ->
                onValueChange(raw.filter { it.isDigit() }.take(6))
            },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(
                color = Color.Transparent,
                fontSize = 1.sp,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
        )
    }
}

@Composable
private fun DigitCircle(
    digit: String,
    isActive: Boolean,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) DobbyShopColors.Accent else DobbyShopColors.Border,
                shape = CircleShape,
            )
            .padding(3.dp)
            .border(1.dp, DobbyShopColors.Border, CircleShape)
            .clip(CircleShape)
            .background(DobbyShopColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit,
            color = DobbyShopColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
