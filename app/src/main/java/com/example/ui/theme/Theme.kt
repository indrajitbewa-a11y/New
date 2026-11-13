package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class HisabSemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val halfDay: Color,
    val halfDayContainer: Color,
    val onHalfDayContainer: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

private val LightSemanticColors = HisabSemanticColors(
    income = EmeraldProfit,
    incomeContainer = EmeraldContainer,
    onIncomeContainer = EmeraldOnContainer,
    expense = CrimsonLoss,
    expenseContainer = CrimsonContainer,
    onExpenseContainer = CrimsonOnContainer,
    warning = SaffronAmber,
    warningContainer = SaffronContainer,
    onWarningContainer = SaffronOnContainer,
    halfDay = HalfDayBlue,
    halfDayContainer = HalfDayBlueContainer,
    onHalfDayContainer = HalfDayOnBlueContainer,
    heroStart = KhataNavy,
    heroEnd = Color(0xFF1A4266),
)

private val DarkSemanticColors = HisabSemanticColors(
    income = EmeraldBright,
    incomeContainer = Color(0xFF064E3B),
    onIncomeContainer = Color(0xFFA7F3D0),
    expense = Color(0xFFF87171),
    expenseContainer = Color(0xFF7F1D1D),
    onExpenseContainer = Color(0xFFFECACA),
    warning = SaffronBright,
    warningContainer = Color(0xFF78350F),
    onWarningContainer = Color(0xFFFDE68A),
    halfDay = Color(0xFF60A5FA),
    halfDayContainer = Color(0xFF1E3A8A),
    onHalfDayContainer = Color(0xFFBFDBFE),
    heroStart = Color(0xFF102A43),
    heroEnd = Color(0xFF1B3F61),
)

val LocalHisabColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = KhataNavy,
    onPrimary = Color.White,
    primaryContainer = KhataNavyContainer,
    onPrimaryContainer = KhataOnNavyContainer,
    secondary = SaffronAmber,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = SaffronOnContainer,
    tertiary = EmeraldProfit,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldOnContainer,
    error = CrimsonLoss,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = WarmParchment,
    onBackground = InkPrimary,
    surface = WarmSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = WarmOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = SaffronBright,
    onPrimary = KhataNavyDark,
    primaryContainer = Color(0xFF1D3B5A),
    onPrimaryContainer = Color(0xFFE2ECF7),
    secondary = SaffronAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5C3508),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldBright,
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkOutline,
)

val HisabShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalHisabColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = HisabShapes,
            content = content,
        )
    }
}
