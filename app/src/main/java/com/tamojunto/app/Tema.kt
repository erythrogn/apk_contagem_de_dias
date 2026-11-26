package com.tamojunto.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mesma paleta do site: azul-tinta, creme e dourado.
private val Tinta = Color(0xFF1F3D5C)
private val Creme = Color(0xFFF3EFD9)
private val Ocre = Color(0xFF7A5A10)
private val Ouro = Color(0xFFE9C84F)

private val EsquemaCores = lightColorScheme(
    primary = Tinta,
    onPrimary = Creme,
    secondary = Ocre,
    tertiary = Ouro,
    background = Creme,
    onBackground = Tinta,
    surface = Creme,
    onSurface = Tinta
)

@Composable
fun TamoJuntoTema(conteudo: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaCores, content = conteudo)
}
