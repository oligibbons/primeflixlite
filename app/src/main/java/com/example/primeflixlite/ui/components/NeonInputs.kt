package com.example.primeflixlite.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.primeflixlite.ui.theme.NeonBlue
import com.example.primeflixlite.ui.theme.NeonBlueDim
import com.example.primeflixlite.ui.theme.OffBlack
import com.example.primeflixlite.ui.theme.White

@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animate the border color for that "Pulse" feel
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) NeonBlue else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "borderGlow"
    )

    // Animate background slightly to distinguish focus
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF252525) else OffBlack,
        label = "bgGlow"
    )

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            color = if (isFocused) NeonBlue else Color.Gray,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = interactionSource,
            textStyle = TextStyle(
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            cursorBrush = SolidColor(NeonBlue),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    innerTextField()
                }
            }
        )
    }
}