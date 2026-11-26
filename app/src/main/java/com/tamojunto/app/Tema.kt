package com.tamojunto.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Paleta e tipografia iguais às do site (tokens.css e base.css).
private val Tinta = Color(0xFF1F3D5C)
private val TintaSuave = Color(0xFF3D5F86)
private val Creme = Color(0xFFF3EFD9)
private val Ocre = Color(0xFF7A5A10)
private val Ouro = Color(0xFFE9C84F)

// Cormorant Garamond nos títulos, Montserrat no texto corrido.
private val Serifa = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic)
)

private val Sem = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

private val TipografiaDoSite: Typography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontFamily = Sem, fontWeight = FontWeight.Normal),
        bodyMedium = t.bodyMedium.copy(fontFamily = Sem, fontWeight = FontWeight.Normal),
        bodySmall = t.bodySmall.copy(fontFamily = Sem, fontWeight = FontWeight.Normal),
        labelLarge = t.labelLarge.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold),
        labelSmall = t.labelSmall.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold)
    )
}

// Cantos retos, como os cartões do site (4px).
private val FormasDoSite = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

private val EsquemaCores = lightColorScheme(
    primary = Tinta,
    onPrimary = Creme,
    primaryContainer = Color(0xFFC7DBEC),
    onPrimaryContainer = Tinta,
    secondary = Ocre,
    onSecondary = Creme,
    tertiary = Ouro,
    onTertiary = Tinta,
    background = Color(0xFFD6E2EA),
    onBackground = Tinta,
    surface = Creme,
    onSurface = Tinta,
    onSurfaceVariant = TintaSuave,
    outline = TintaSuave
)

// Fundo em degradê azul-pó, igual ao body do site.
private val FundoDoSite = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color(0xFFE4E9E4),
        0.28f to Color(0xFFD6E2EA),
        0.65f to Color(0xFFC9DBEA),
        1f to Color(0xFFBFD3E5)
    )
)

@Composable
fun TamoJuntoTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaCores,
        typography = TipografiaDoSite,
        shapes = FormasDoSite
    ) {
        Box(modifier = Modifier.fillMaxSize().background(FundoDoSite)) {
            conteudo()
        }
    }
}
