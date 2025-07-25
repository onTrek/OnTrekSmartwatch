package com.ontrek.wear.utils.functions

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp


fun calculateFontSize(text: String): TextUnit {
    val maxFontSize = 12.sp
    val minFontSize = 7.sp

    return when {
        text.length <= 10 -> maxFontSize
        text.length >= 20 -> minFontSize
        else -> {
            val ratio = (text.length - 10) / 15f
            val fontSize = maxFontSize.value - ((maxFontSize.value - minFontSize.value) * ratio)
            fontSize.sp
        }
    }
}