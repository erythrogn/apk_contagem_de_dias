package com.tamojunto.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val FonteSerifaLogin = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold)
)

private val CartaoLoginTopo = Color(0xFFFBF8EC)
private val CartaoLoginBase = Color(0xFFF3EFD9)
private val BordaLoginDourada = Color(0xFFD6BE7A)
private val TintaLoginAcento = Color(0xFF3F6FA6)
private val ErroFundoSuave = Color(0xFFFBEAEA)
private val ErroTextoSuave = Color(0xFFA63A3A)

@Composable
fun TelaLogin() {
    val contexto = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var entrando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var botaoPressionado by remember { mutableStateOf(false) }

    // Animações escalonadas de entrada
    val animEmblema = remember { Animatable(0f) }
    val animTitulo = remember { Animatable(0f) }
    val animAcao = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            animEmblema.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            delay(140)
            animTitulo.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            delay(280)
            animAcao.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
    }

    // Movimento contínuo sutil do emblema
    val infinito = rememberInfiniteTransition(label = "orbitaLogin")
    val anguloOrbita by infinito.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anguloOrbita"
    )
    val pulsoBrilho by infinito.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsoBrilho"
    )

    val escalaBotao by animateFloatAsState(
        targetValue = if (botaoPressionado && !entrando) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "escalaBotaoLogin"
    )

    val clienteGoogle = remember {
        GoogleSignIn.getClient(
            contexto,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(contexto.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
    }

    val lancador = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        try {
            val conta = GoogleSignIn.getSignedInAccountFromIntent(resultado.data)
                .getResult(ApiException::class.java)
            val credencial = GoogleAuthProvider.getCredential(conta.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credencial)
                .addOnCompleteListener { tarefa ->
                    entrando = false
                    if (!tarefa.isSuccessful) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        erro = "Não consegui entrar. Tente de novo."
                    }
                }
        } catch (e: ApiException) {
            entrando = false
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            erro = mensagemDeErroGoogle(e.statusCode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Papel)
    ) {
        // Luz ambiente suave no fundo em papel
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Ouro.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    radius = size.minDimension * 0.65f * pulsoBrilho
                ),
                radius = size.minDimension * 0.65f * pulsoBrilho,
                center = Offset(size.width * 0.5f, size.height * 0.34f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Topo discreto
            Text(
                text = "NOSSO ESPAÇO",
                style = TextStyle(
                    fontFamily = Sem,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 2.8.sp
                ),
                color = Ocre.copy(alpha = 0.85f),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .graphicsLayer { alpha = animEmblema.value }
            )

            // Miolo central: Emblema + Título Editorial + Cartão de Entrada
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emblema orbital minimalista
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer {
                            alpha = animEmblema.value
                            translationY = (1f - animEmblema.value) * 24.dp.toPx()
                            scaleX = 0.9f + animEmblema.value * 0.1f
                            scaleY = 0.9f + animEmblema.value * 0.1f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centro = Offset(size.width / 2f, size.height / 2f)
                        val raioExterno = size.minDimension * 0.45f
                        val raioInterno = size.minDimension * 0.34f

                        drawCircle(
                            color = BordaLoginDourada.copy(alpha = 0.45f),
                            radius = raioExterno,
                            center = centro,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = TintaLoginAcento.copy(alpha = 0.25f),
                            radius = raioInterno,
                            center = centro,
                            style = Stroke(width = 0.9.dp.toPx())
                        )

                        rotate(degrees = anguloOrbita, pivot = centro) {
                            drawCircle(
                                color = Ouro,
                                radius = 3.2.dp.toPx(),
                                center = Offset(centro.x, centro.y - raioExterno)
                            )
                            drawCircle(
                                color = TintaLoginAcento,
                                radius = 2.4.dp.toPx(),
                                center = Offset(centro.x, centro.y + raioExterno)
                            )
                        }

                        // Estrela central de 4 pontas
                        val s = 11.dp.toPx()
                        val estrela = Path().apply {
                            moveTo(centro.x, centro.y - s)
                            quadraticTo(centro.x, centro.y, centro.x + s, centro.y)
                            quadraticTo(centro.x, centro.y, centro.x, centro.y + s)
                            quadraticTo(centro.x, centro.y, centro.x - s, centro.y)
                            quadraticTo(centro.x, centro.y, centro.x, centro.y - s)
                            close()
                        }
                        drawPath(estrela, color = Ocre)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Título e subtítulo com entrada animada
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = animTitulo.value
                        translationY = (1f - animTitulo.value) * 20.dp.toPx()
                    }
                ) {
                    Text(
                        text = "Contagem de Dias",
                        style = TextStyle(
                            fontFamily = FonteSerifaLogin,
                            fontWeight = FontWeight.Normal,
                            fontSize = 38.sp,
                            lineHeight = 42.sp
                        ),
                        color = Tinta,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "cada dia contado ao seu lado",
                        style = TextStyle(
                            fontFamily = FonteSerifaLogin,
                            fontStyle = FontStyle.Italic,
                            fontSize = 17.sp
                        ),
                        color = TintaSuave,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    DivisorOrnamentalLogin(modifier = Modifier.padding(top = 16.dp, bottom = 26.dp))
                }

                // Cartão minimalista de acesso
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animAcao.value
                            translationY = (1f - animAcao.value) * 22.dp.toPx()
                        }
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = Tinta.copy(alpha = 0.12f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.verticalGradient(listOf(CartaoLoginTopo, CartaoLoginBase)))
                        .border(1.dp, BordaLoginDourada, RoundedCornerShape(16.dp))
                        .padding(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.75.dp, Ocre.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Entre com a sua conta Google para sincronizar memórias, datas e cartinhas.",
                            style = TextStyle(
                                fontFamily = Sem,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = TintaSuave,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Botão Google tátil com transição animada
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (entrando) Tinta.copy(alpha = 0.88f) else Tinta,
                            shadowElevation = if (botaoPressionado) 1.dp else 4.dp,
                            border = BorderStroke(1.dp, BordaLoginDourada.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .graphicsLayer {
                                    scaleX = escalaBotao
                                    scaleY = escalaBotao
                                }
                                .pointerInput(entrando) {
                                    if (!entrando) {
                                        detectTapGestures(
                                            onPress = {
                                                botaoPressionado = true
                                                tryAwaitRelease()
                                                botaoPressionado = false
                                            },
                                            onTap = {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                entrando = true
                                                erro = null
                                                lancador.launch(clienteGoogle.signInIntent)
                                            }
                                        )
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = entrando,
                                    transitionSpec = {
                                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                    },
                                    label = "estadoBotaoGoogle"
                                ) { carregando ->
                                    if (carregando) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Ouro,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Conectando…",
                                                style = TextStyle(
                                                    fontFamily = Sem,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.5.sp
                                                ),
                                                color = Creme
                                            )
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                EmblemaGoogle(modifier = Modifier.size(15.dp))
                                            }
                                            Text(
                                                text = "Entrar com Google",
                                                style = TextStyle(
                                                    fontFamily = Sem,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    letterSpacing = 0.3.sp
                                                ),
                                                color = Creme
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Mensagem de erro animada
                        AnimatedVisibility(
                            visible = erro != null,
                            enter = fadeIn(tween(220)) + expandVertically(spring(dampingRatio = 0.8f)),
                            exit = fadeOut(tween(160)) + shrinkVertically(tween(180))
                        ) {
                            erro?.let { mensagem ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ErroFundoSuave)
                                        .border(0.8.dp, ErroTextoSuave.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = mensagem,
                                        style = TextStyle(
                                            fontFamily = Sem,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = ErroTextoSuave,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rodapé minimalista
            Text(
                text = "— Contagem de Dias —",
                style = TextStyle(
                    fontFamily = FonteSerifaLogin,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp
                ),
                color = Ocre.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .graphicsLayer { alpha = animAcao.value }
            )
        }
    }
}

@Composable
private fun DivisorOrnamentalLogin(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 160.dp, height = 12.dp)) {
        val cor = Ocre
        val centroY = size.height / 2f
        val centroX = size.width / 2f
        drawLine(cor, Offset(0f, centroY), Offset(centroX - 16.dp.toPx(), centroY), strokeWidth = 0.9.dp.toPx())
        drawLine(cor, Offset(centroX + 16.dp.toPx(), centroY), Offset(size.width, centroY), strokeWidth = 0.9.dp.toPx())

        val losango = Path().apply {
            moveTo(centroX, 1.5.dp.toPx())
            lineTo(centroX + 5.dp.toPx(), centroY)
            lineTo(centroX, size.height - 1.5.dp.toPx())
            lineTo(centroX - 5.dp.toPx(), centroY)
            close()
        }
        drawPath(losango, color = cor)
        drawCircle(cor, radius = 1.5.dp.toPx(), center = Offset(centroX - 10.dp.toPx(), centroY))
        drawCircle(cor, radius = 1.5.dp.toPx(), center = Offset(centroX + 10.dp.toPx(), centroY))
    }
}

@Composable
private fun EmblemaGoogle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        // Vermelho (topo)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Amarelo (esquerda)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Verde (base)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Azul (direita + barra horizontal do G)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -12f,
            sweepAngle = 54f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.5f),
            end = Offset(size.width - inset * 0.3f, size.height * 0.5f),
            strokeWidth = strokeWidth * 0.92f,
            cap = StrokeCap.Butt
        )
    }
}

private fun mensagemDeErroGoogle(codigo: Int): String = when (codigo) {
    10 -> "Falta cadastrar a impressão digital SHA-1 do app no Firebase. Veja o README."
    12501 -> "Entrada cancelada."
    7 -> "Sem conexão com a internet."
    else -> "Não consegui entrar (código $codigo)."
}