package com.smarthome.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 使用系统默认圆润字体
private val RoundedFamily = FontFamily.Default

val SmartHomeTypography = Typography(
    displayLarge = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    displaySmall = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineLarge = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = RoundedFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp)
)
