package com.tamojunto.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser

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
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.2.sp
)

@Composable
internal fun BarraInferior(abas: List<Aba>, atual: Aba, aoEscolher: (Aba) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = Tinta.copy(alpha = 0.08f),
                ambientColor = Tinta.copy(alpha = 0.04f)
            )
            .background(Papel)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Ouro.copy(alpha = 0.25f),
                            Ouro.copy(alpha = 0.85f),
                            Ouro.copy(alpha = 0.25f)
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            abas.forEach { aba ->
                val ativa = aba == atual
                val cor by animateColorAsState(
                    targetValue = if (ativa) Tinta else TintaSuave,
                    animationSpec = tween(durationMillis = 220),
                    label = "corAba"
                )
                val fundoPilula by animateColorAsState(
                    targetValue = if (ativa) Ouro.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 220),
                    label = "fundoPilulaAba"
                )
                val larguraTraco by animateDpAsState(
                    targetValue = if (ativa) 20.dp else 0.dp,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "tracoAba"
                )
                val escalaIcone by animateFloatAsState(
                    targetValue = if (ativa) 1.06f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "escalaIconeAba"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { aoEscolher(aba) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(fundoPilula)
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.graphicsLayer {
                                scaleX = escalaIcone
                                scaleY = escalaIcone
                            }
                        ) {
                            IconeLinha(
                                caminhos = aba.caminhos,
                                tamanho = 22.dp,
                                cor = cor,
                                espessura = if (ativa) aba.espessura + 0.15f else aba.espessura
                            )
                        }
                    }
                    Text(
                        text = aba.rotulo,
                        style = EstiloMenu,
                        color = cor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(larguraTraco)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Ouro)
                    )
                }
            }
        }
    }
}