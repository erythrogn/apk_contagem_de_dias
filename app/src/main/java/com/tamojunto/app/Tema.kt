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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta e tipografia iguais às do site (tokens.css e convite.css).
internal val Tinta = Color(0xFF1F3D5C)
internal val TintaSuave = Color(0xFF3D5F86)
internal val Creme = Color(0xFFF3EFD9)
internal val Ocre = Color(0xFF7A5A10)
internal val Ouro = Color(0xFFE9C84F)
internal val Papel = Color(0xFFFBF6E6)

// Cormorant Garamond nos títulos e números, Montserrat no texto corrido.
internal val Serifa = FontFamily(
    Font(R.font.cormorant_garamond_light, FontWeight.Light),
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic)
)

internal val Sem = FontFamily(
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold)
)

// Estilos recorrentes da página inicial do site.
internal val EstiloTituloSecao = TextStyle(
    fontFamily = Serifa,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 30.sp,
    lineHeight = 34.sp
)
internal val EstiloApoio = TextStyle(
    fontFamily = Serifa,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 19.sp,
    lineHeight = 26.sp
)
internal val EstiloNumero = TextStyle(
    fontFamily = Serifa,
    fontWeight = FontWeight.Light,
    fontSize = 92.sp,
    lineHeight = 92.sp
)
internal val EstiloRelogio = TextStyle(
    fontFamily = Serifa,
    fontWeight = FontWeight.Normal,
    fontSize = 30.sp,
    lineHeight = 32.sp
)
internal val EstiloChip = TextStyle(
    fontFamily = Sem,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 17.sp
)
internal val EstiloBotao = TextStyle(
    fontFamily = Sem,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp
)
internal val EstiloRotulo = TextStyle(
    fontFamily = Sem,
    fontWeight = FontWeight.Light,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

private val TipografiaDoSite: Typography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = Serifa, fontWeight = FontWeight.Light),
        displaySmall = t.displaySmall.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontFamily = Serifa, fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontFamily = Sem, fontWeight = FontWeight.Normal),
        bodyMedium = t.bodyMedium.copy(fontFamily = Sem, fontWeight = FontWeight.Normal),
        bodySmall = t.bodySmall.copy(fontFamily = Sem, fontWeight = FontWeight.Light),
        labelLarge = t.labelLarge.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontFamily = Sem, fontWeight = FontWeight.SemiBold),
        labelSmall = t.labelSmall.copy(fontFamily = Sem, fontWeight = FontWeight.Light)
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
    surface = Papel,
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

// Papel creme com moldura dupla (filete dourado e fio azul), como o painel da página inicial do site.
internal fun Modifier.papelDoSite(): Modifier = this
    .background(Papel)
    .drawBehind {
        val espessura = 1.dp.toPx()
        val margemOuro = 12.dp.toPx()
        val margemFio = 17.dp.toPx()
        drawRect(
            color = Ouro.copy(alpha = 0.8f),
            topLeft = Offset(margemOuro, margemOuro),
            size = Size(size.width - 2 * margemOuro, size.height - 2 * margemOuro),
            style = Stroke(espessura)
        )
        drawRect(
            color = Tinta.copy(alpha = 0.18f),
            topLeft = Offset(margemFio, margemFio),
            size = Size(size.width - 2 * margemFio, size.height - 2 * margemFio),
            style = Stroke(espessura)
        )
    }

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
