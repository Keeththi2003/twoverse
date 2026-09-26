package app.twoverse.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.twoverse.R

private val ManropeFamily = FontFamily(
    listOf(400, 500, 600, 700).map { weight ->
        Font(
            resId = R.font.manrope,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

/** Fraunces with the optical-size axis matched to the rendered size, as browsers do. */
private fun fraunces(opticalSize: Float) = FontFamily(
    listOf(500, 600).map { weight ->
        Font(
            resId = R.font.fraunces,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    },
)

private fun serif(size: TextUnit, weight: Int, lineHeight: Float, letterSpacing: TextUnit) = TextStyle(
    fontFamily = fraunces(size.value),
    fontWeight = FontWeight(weight),
    fontSize = size,
    lineHeight = lineHeight.em,
    letterSpacing = letterSpacing,
)

private fun sans(size: TextUnit, weight: Int, lineHeight: Float, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = ManropeFamily,
    fontWeight = FontWeight(weight),
    fontSize = size,
    lineHeight = lineHeight.em,
    letterSpacing = letterSpacing,
)

internal val TwoverseTypography = Typography(
    displayLarge = serif(104.sp, 600, lineHeight = 1f, letterSpacing = (-2).sp),
    displayMedium = serif(60.sp, 600, lineHeight = 1f, letterSpacing = (-1).sp),
    displaySmall = serif(46.sp, 600, lineHeight = 1.05f, letterSpacing = (-0.5).sp),
    headlineLarge = serif(36.sp, 600, lineHeight = 1.1f, letterSpacing = (-0.5).sp),
    headlineMedium = serif(30.sp, 600, lineHeight = 1.2f, letterSpacing = (-0.5).sp),
    headlineSmall = serif(24.sp, 600, lineHeight = 1.2f, letterSpacing = 0.sp),
    titleLarge = sans(17.sp, 700, lineHeight = 1.3f),
    titleMedium = sans(16.sp, 700, lineHeight = 1.3f),
    titleSmall = sans(15.sp, 600, lineHeight = 1.35f),
    bodyLarge = sans(16.sp, 400, lineHeight = 1.55f),
    bodyMedium = sans(15.sp, 500, lineHeight = 1.5f),
    bodySmall = sans(13.sp, 400, lineHeight = 1.45f),
    labelLarge = sans(14.sp, 600, lineHeight = 1.4f),
    labelMedium = sans(13.sp, 600, lineHeight = 1.35f),
    labelSmall = sans(12.sp, 600, lineHeight = 1.35f, letterSpacing = 0.sp),
)

/** Text styles Material 3 has no slot for. */
@Immutable
data class TwoverseTextStyles(
    /** Uppercase section header ("PRIVACY", "YOUR COUPLE CODE"). */
    val sectionHeader: TextStyle = TwoverseTypography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    ),
    /** The couple code shown on Pair ("AB72-KP91"). */
    val coupleCode: TextStyle = sans(36.sp, 700, lineHeight = 1.2f, letterSpacing = 4.sp),
    /** Text typed into the partner-code field. */
    val coupleCodeInput: TextStyle = sans(18.sp, 600, lineHeight = 1.3f, letterSpacing = 3.sp),
    /** Hours, minutes and seconds values on Until We Meet. */
    val countdownUnitValue: TextStyle = sans(26.sp, 700, lineHeight = 1.2f),
    /** Uppercase unit labels under countdown values ("HOURS"). */
    val countdownUnitLabel: TextStyle = sans(11.sp, 700, lineHeight = 1.3f, letterSpacing = 1.sp),
)
