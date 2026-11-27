package com.tamojunto.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser

private val DockSuperficie = Color(0xFFFBF8EC)
private val DockSuperficieBase = Color(0xFFF2EAD5)
private val DockBorda = Color(0xFFD6BE7A)

// Espaço que as listas deixam no fim para a barra flutuante não cobrir o conteúdo.
// TelaPrincipal soma a área de gesto do sistema a este valor.
internal val AlturaBarraInferior = 112.dp
internal val LocalAlturaDock = compositionLocalOf { AlturaBarraInferior }

@Composable
internal fun IconeLinha(caminhos: List<String>, tamanho: Dp, cor: Color, espessura: Float = 1.25f) {
    val prontos = remember(caminhos) {
        caminhos.map { PathParser.createPathFromPathData(it).asComposePath() }
    }
    Canvas(modifier = Modifier.size(tamanho)) {
        val escala = size.minDimension / 24f
        withTransform({ scale(escala, escala, Offset.Zero) }) {
            prontos.forEach { caminho ->
                drawPath(
                    path = caminho,
                    color = cor,
                    style = Stroke(width = espessura, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

internal val EstiloMenu = TextStyle(
    fontFamily = Sem,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.25.sp
)

@Composable
internal fun BarraInferior(abas: List<Aba>, atual: Aba, aoEscolher: (Aba) -> Unit) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .shadow(
                    elevation = 34.dp,
                    shape = RoundedCornerShape(999.dp),
                    spotColor = Tinta.copy(alpha = 0.34f),
                    ambientColor = Tinta.copy(alpha = 0.18f)
                )
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DockSuperficie.copy(alpha = 0.88f),
                            DockSuperficieBase.copy(alpha = 0.84f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            DockBorda.copy(alpha = 0.92f)
                        )
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 9.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            abas.forEach { aba ->
                val ativa = aba == atual
                var pressionada by remember { mutableStateOf(false) }

                val corIcone by animateColorAsState(
                    targetValue = if (ativa) Tinta else TintaSuave,
                    animationSpec = tween(durationMillis = 220),
                    label = "corIconeDock"
                )
                val fundoPilula by animateColorAsState(
                    targetValue = if (ativa) Ouro.copy(alpha = 0.30f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 240),
                    label = "fundoPilulaDock"
                )
                val bordaPilula by animateColorAsState(
                    targetValue = if (ativa) DockBorda.copy(alpha = 0.95f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 240),
                    label = "bordaPilulaDock"
                )
                val escala by animateFloatAsState(
                    targetValue = when {
                        pressionada -> 0.92f
                        ativa -> 1.05f
                        else -> 1f
                    },
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "escalaItemDock"
                )
                val elevacaoVertical by animateDpAsState(
                    targetValue = if (ativa) (-2).dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "levitacaoItemDock"
                )
                val tamanhoPonto by animateDpAsState(
                    targetValue = if (ativa) 0.dp else 3.dp,
                    animationSpec = tween(durationMillis = 200),
                    label = "pontoInativoDock"
                )

                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            translationY = elevacaoVertical.toPx()
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(fundoPilula)
                        .border(0.8.dp, bordaPilula, RoundedCornerShape(999.dp))
                        .semantics {
                            role = Role.Tab
                            selected = ativa
                        }
                        // Com ativa como chave, o toque sempre enxerga se a aba está ativa agora.
                        .pointerInput(aba, ativa) {
                            detectTapGestures(
                                onPress = {
                                    pressionada = true
                                    tryAwaitRelease()
                                    pressionada = false
                                },
                                onTap = {
                                    if (!ativa) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        aoEscolher(aba)
                                    }
                                }
                            )
                        }
                        .padding(
                            horizontal = if (ativa) 14.dp else 10.dp,
                            vertical = 9.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconeLinha(
                            caminhos = aba.caminhos,
                            tamanho = 20.dp,
                            cor = corIcone,
                            espessura = if (ativa) aba.espessura + 0.2f else aba.espessura
                        )
                        if (!ativa && tamanhoPonto > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(tamanhoPonto)
                                    .clip(CircleShape)
                                    .background(TintaSuave.copy(alpha = 0.35f))
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = ativa,
                        enter = fadeIn(tween(200)) + expandHorizontally(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                            expandFrom = Alignment.Start
                        ),
                        exit = fadeOut(tween(140)) + shrinkHorizontally(
                            animationSpec = tween(180),
                            shrinkTowards = Alignment.Start
                        )
                    ) {
                        Text(
                            text = aba.rotulo,
                            style = EstiloMenu,
                            color = Tinta,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 7.dp)
                        )
                    }
                }
            }
        }
    }
}