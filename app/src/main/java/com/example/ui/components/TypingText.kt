package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun TypingText(
    fullText: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    typingDelayMs: Long = 18L
) {
    Text(
        text = fullText,
        modifier = modifier,
        style = style
    )
}