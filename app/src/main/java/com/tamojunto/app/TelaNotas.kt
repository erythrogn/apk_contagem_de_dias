@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tamojunto.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private val FonteSerifaNotas = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold)
)

private val CartaoNotasTopo = Color(0xFFFBF8EC)
private val CartaoNotasBase = Color(0xFFF3EFD9)
private val CartaoCartaAmarelo = Color(0xFFFCF7E2)
private val BordaNotasDourada = Color(0xFFD6BE7A)
private val TintaNotasAcento = Color(0xFF3F6FA6)
private val AzulPillNotas = Color(0xFFC7DAEC)
private val DestaqueNotaRecem = Color(0xFFFFF6CF)
private val PerigoNotaCor = Color(0xFFB44343)

private val FILTROS_NOTAS = listOf(
    "todos" to "Tudo",
    "texto" to "Textos",
    "lista" to "Listas",
    "pergunta" to "Perguntas",
    "carta" to "Cartas"
)

private data class TipoNotaInfo(
    val chave: String,
    val nome: String,
    val descricao: String
)

private val TIPOS_NOTA = listOf(
    TipoNotaInfo("texto", "Texto", "Um pensamento, memória ou recado"),
    TipoNotaInfo("lista", "Lista", "Desejos, planos, coisas a fazer"),
    TipoNotaInfo("pergunta", "Pergunta", "A outra pessoa responde"),
    TipoNotaInfo("carta", "Carta", "Lacrada até a outra pessoa abrir")
)

private data class ExtraNota(
    val fixada: Boolean = false,
    val aberta: Boolean = false
)

@Composable
fun TelaNotas(dados: DadosDoCasal) {
    if (dados.casalId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Papel)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(CartaoNotasTopo, CartaoNotasBase)))
                    .border(1.dp, BordaNotasDourada, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Cofre de Memórias",
                    style = TextStyle(fontFamily = FonteSerifaNotas, fontSize = 26.sp),
                    color = Tinta
                )
                Text(
                    text = "Você ainda não tem um casal. O Bloco de Notas fica dentro do espaço do casal.",
                    style = TextStyle(fontFamily = Sem, fontSize = 13.sp, lineHeight = 19.sp),
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var liberado by rememberSaveable { mutableStateOf(false) }

    AnimatedContent(
        targetState = liberado,
        transitionSpec = {
            fadeIn(tween(280)) togetherWith fadeOut(tween(220))
        },
        label = "transicaoCofreNotas"
    ) { aberto ->
        if (aberto) {
            ListaNotas(dados)
        } else {
            GateNotas(aoLiberar = { liberado = true })
        }
    }
}

@Composable
private fun GateNotas(aoLiberar: () -> Unit) {
    val escopo = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var senha by rememberSaveable { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    val animEntrada = remember { Animatable(0f) }
    val tremorX = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animEntrada.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
    }

    fun acionarTremor() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        escopo.launch {
            val offsets = listOf(-12f, 11f, -8f, 7f, -4f, 0f)
            for (alvo in offsets) {
                tremorX.animateTo(alvo, tween(55))
            }
        }
    }

    fun autenticar() {
        if (senha.isBlank() || enviando) return
        enviando = true
        aviso = ""
        escopo.launch {
            val r = Servidor.postJson(
                "/api/bloco/auth",
                JSONObject().put("hash", sha256Hex(senha.trim())),
                repetirSeSessaoExpirou = false
            )
            enviando = false
            if (r.ok) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                senha = ""
                aoLiberar()
            } else {
                aviso = r.msg
                acionarTremor()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Papel)
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = animEntrada.value
                    translationY = (1f - animEntrada.value) * 20.dp.toPx()
                    translationX = tremorX.value.dp.toPx()
                }
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = Tinta.copy(alpha = 0.14f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(CartaoNotasTopo, CartaoNotasBase)))
                .border(1.dp, BordaNotasDourada, RoundedCornerShape(18.dp))
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.75.dp, Ocre.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícone de cadeado editorial
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Ouro.copy(alpha = 0.16f))
                        .border(1.dp, BordaNotasDourada.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconeCadeado(cor = Ocre, modifier = Modifier.size(28.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cofre de Memórias",
                    style = TextStyle(
                        fontFamily = FonteSerifaNotas,
                        fontSize = 30.sp,
                        lineHeight = 34.sp
                    ),
                    color = Tinta,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Este espaço é só nosso.",
                    style = TextStyle(
                        fontFamily = FonteSerifaNotas,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp
                    ),
                    color = TintaSuave,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                DivisorNotas(modifier = Modifier.padding(vertical = 18.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = {
                        senha = it
                        if (aviso.isNotBlank()) aviso = ""
                    },
                    label = { Text("Senha do casal") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { autenticar() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.75f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.55f),
                        focusedBorderColor = TintaNotasAcento,
                        unfocusedBorderColor = BordaNotasDourada
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    enabled = senha.isNotBlank() && !enviando,
                    onClick = { autenticar() },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Tinta,
                        contentColor = Creme,
                        disabledContainerColor = TintaSuave.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Ouro,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Entrar no cofre",
                            style = TextStyle(
                                fontFamily = Sem,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                letterSpacing = 0.4.sp
                            )
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = aviso.isNotBlank(),
                    enter = fadeIn(tween(200)) + expandVertically(),
                    exit = fadeOut(tween(150)) + shrinkVertically()
                ) {
                    Text(
                        text = aviso,
                        style = TextStyle(fontFamily = Sem, fontSize = 12.sp),
                        color = PerigoNotaCor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ListaNotas(dados: DadosDoCasal) {
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var criando by rememberSaveable { mutableStateOf(false) }
    var notaEditando by remember { mutableStateOf<Nota?>(null) }
    var notaApagando by remember { mutableStateOf<Nota?>(null) }

    var filtroTipo by rememberSaveable { mutableStateOf("todos") }
    var busca by rememberSaveable { mutableStateOf("") }
    var notaDestaqueChave by remember { mutableStateOf<String?>(null) }

    val extrasNotas = remember { mutableStateListOf<Pair<String, ExtraNota>>() }

    val fabExpandido by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 60 }
    }

    // Sincroniza metadados complementares do site (fixada e carta aberta) sem alterar o schema web.
    DisposableEffect(dados.casalId) {
        val casalId = dados.casalId
        if (casalId.isNullOrBlank()) {
            onDispose { }
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("casais/$casalId/notas")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lista = mutableListOf<Pair<String, ExtraNota>>()
                    for (child in snapshot.children) {
                        val chave = child.key ?: continue
                        val fixada = (child.child("pinned").getValue(Boolean::class.java) ?: false) ||
                            (child.child("fixada").getValue(Boolean::class.java) ?: false)
                        val aberta = (child.child("opened").getValue(Boolean::class.java) ?: false) ||
                            (child.child("aberta").getValue(Boolean::class.java) ?: false)
                        if (fixada || aberta) {
                            lista.add(chave to ExtraNota(fixada = fixada, aberta = aberta))
                        }
                    }
                    extrasNotas.clear()
                    extrasNotas.addAll(lista)
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        }
    }

    LaunchedEffect(notaDestaqueChave) {
        if (notaDestaqueChave != null) {
            delay(1800)
            notaDestaqueChave = null
        }
    }

    val todas = dados.notas.toList()
    val visiveis = remember(todas, filtroTipo, busca, extrasNotas.size) {
        val termo = busca.trim().lowercase()
        val filtradas = todas.filter { nota ->
            val tipoOk = filtroTipo == "todos" || nota.tipo == filtroTipo
            val buscaOk = termo.isEmpty() ||
                nota.titulo.lowercase().contains(termo) ||
                nota.conteudo.lowercase().contains(termo) ||
                nota.resposta.lowercase().contains(termo)
            tipoOk && buscaOk
        }
        filtradas.sortedByDescending { nota ->
            extrasNotas.firstOrNull { it.first == nota.chave }?.second?.fixada == true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Papel)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = LocalAlturaDock.current),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabeçalho minimalista
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COFRE DE MEMÓRIAS",
                                style = TextStyle(
                                    fontFamily = Sem,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 2.4.sp
                                ),
                                color = Ocre
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "Bloco de ",
                                    style = TextStyle(
                                        fontFamily = FonteSerifaNotas,
                                        fontSize = 32.sp,
                                        lineHeight = 36.sp
                                    ),
                                    color = Tinta
                                )
                                Text(
                                    text = "Notas",
                                    style = TextStyle(
                                        fontFamily = FonteSerifaNotas,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 32.sp,
                                        lineHeight = 36.sp
                                    ),
                                    color = TintaNotasAcento
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CartaoNotasTopo,
                            border = BorderStroke(1.dp, BordaNotasDourada.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "${todas.size} ${if (todas.size == 1) "nota" else "notas"}",
                                style = TextStyle(
                                    fontFamily = Sem,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                ),
                                color = Tinta,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // Chips horizontais de filtro por tipo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FILTROS_NOTAS.forEach { (chave, rotulo) ->
                            val contagem = if (chave == "todos") todas.size else todas.count { it.tipo == chave }
                            val ativo = filtroTipo == chave
                            FilterChip(
                                selected = ativo,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    filtroTipo = chave
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CartaoNotasTopo.copy(alpha = 0.85f),
                                    labelColor = TintaSuave,
                                    selectedContainerColor = Tinta,
                                    selectedLabelColor = Creme
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = ativo,
                                    borderColor = BordaNotasDourada.copy(alpha = 0.65f),
                                    selectedBorderColor = Tinta
                                ),
                                label = {
                                    Text(
                                        text = if (contagem > 0) "$rotulo · $contagem" else rotulo,
                                        style = TextStyle(
                                            fontFamily = Sem,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // Campo de busca minimalista
                    OutlinedTextField(
                        value = busca,
                        onValueChange = { busca = it },
                        placeholder = {
                            Text(
                                text = "Buscar nas notas…",
                                style = TextStyle(fontFamily = Sem, fontSize = 13.sp, color = TintaSuave)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(999.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CartaoNotasTopo,
                            unfocusedContainerColor = CartaoNotasTopo.copy(alpha = 0.85f),
                            focusedBorderColor = TintaNotasAcento,
                            unfocusedBorderColor = BordaNotasDourada.copy(alpha = 0.65f),
                            focusedTextColor = Tinta,
                            unfocusedTextColor = Tinta
                        ),
                        trailingIcon = {
                            if (busca.isNotEmpty()) {
                                TextButton(
                                    onClick = { busca = "" },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("×", fontSize = 18.sp, color = Ocre)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            if (visiveis.isEmpty()) {
                item(key = "vazio") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(CartaoNotasTopo, CartaoNotasBase)))
                            .border(1.dp, BordaNotasDourada.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                            .padding(vertical = 38.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (todas.isEmpty()) "Nada aqui ainda. Escreva a primeira anotação." else "Nenhuma nota encontrada.",
                                style = TextStyle(
                                    fontFamily = FonteSerifaNotas,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 18.sp
                                ),
                                color = TintaSuave,
                                textAlign = TextAlign.Center
                            )
                            if (todas.isEmpty()) {
                                Text(
                                    text = "Toque no + para criar um texto, lista, pergunta ou carta",
                                    style = TextStyle(fontFamily = Sem, fontSize = 11.5.sp),
                                    color = Ocre,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(visiveis, key = { it.chave }) { nota ->
                    val extra = extrasNotas.firstOrNull { it.first == nota.chave }?.second ?: ExtraNota()
                    CartaNota(
                        nota = nota,
                        meuSlot = dados.meuSlot,
                        casalId = dados.casalId,
                        extra = extra,
                        destacada = notaDestaqueChave == nota.chave,
                        aoAlternarFixar = {
                            val casalId = dados.casalId ?: return@CartaNota
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val novoValor = !extra.fixada
                            FirebaseDatabase.getInstance()
                                .getReference("casais/$casalId/notas/${nota.chave}/pinned")
                                .setValue(novoValor)
                        },
                        aoAbrirCarta = {
                            val casalId = dados.casalId ?: return@CartaNota
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            FirebaseDatabase.getInstance()
                                .getReference("casais/$casalId/notas/${nota.chave}/opened")
                                .setValue(true)
                        },
                        aoEditar = { notaEditando = nota },
                        aoApagar = { notaApagando = nota },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        // FAB expansível no canto inferior direito
        FloatingActionButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                criando = true
            },
            shape = RoundedCornerShape(999.dp),
            containerColor = Tinta,
            contentColor = Creme,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                // Acima da barra flutuante: LocalAlturaDock já inclui a área de gesto do sistema.
                .padding(end = 20.dp, bottom = LocalAlturaDock.current + 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (fabExpandido) 18.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontFamily = FonteSerifaNotas,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp
                    ),
                    color = Ouro
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = fabExpandido,
                    enter = fadeIn(tween(180)) + expandVertically(),
                    exit = fadeOut(tween(140)) + shrinkVertically()
                ) {
                    Text(
                        text = "Nova nota",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            letterSpacing = 0.4.sp
                        ),
                        color = Creme
                    )
                }
            }
        }
    }

    if (criando) {
        SheetNovaNota(
            dados = dados,
            aoFechar = { criando = false },
            aoCriada = { chaveNova -> notaDestaqueChave = chaveNova }
        )
    }

    notaEditando?.let { nota ->
        SheetEditarNota(
            dados = dados,
            nota = nota,
            aoFechar = { notaEditando = null }
        )
    }

    notaApagando?.let { nota ->
        DialogoConfirmar(
            titulo = "Apagar anotação?",
            texto = nota.titulo.ifBlank { "Esta nota será removida para os dois." },
            rotuloConfirmar = "Apagar",
            aoConfirmar = {
                val casalId = dados.casalId
                if (!casalId.isNullOrBlank()) {
                    FirebaseDatabase.getInstance()
                        .getReference("casais/$casalId/notas/${nota.chave}")
                        .removeValue()
                }
            },
            aoFechar = { notaApagando = null }
        )
    }
}

@Composable
private fun CartaNota(
    nota: Nota,
    meuSlot: String?,
    casalId: String?,
    extra: ExtraNota,
    destacada: Boolean,
    aoAlternarFixar: () -> Unit,
    aoAbrirCarta: () -> Unit,
    aoEditar: () -> Unit,
    aoApagar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val minha = nota.autor == meuSlot
    val quem = if (minha) "por você" else "pela outra pessoa"
    val isCarta = nota.tipo == "carta"
    var respostaDigitada by remember(nota.chave) { mutableStateOf("") }

    val fundoCartao by animateColorAsState(
        targetValue = when {
            destacada -> DestaqueNotaRecem
            isCarta -> CartaoCartaAmarelo
            else -> CartaoNotasTopo
        },
        animationSpec = tween(450),
        label = "fundoCartaNota"
    )

    val rotuloTipo = when (nota.tipo) {
        "lista" -> "Lista"
        "pergunta" -> "Pergunta"
        "carta" -> "Carta"
        else -> "Texto"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Tinta.copy(alpha = 0.11f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(fundoCartao)
            .border(
                width = 1.dp,
                color = if (extra.fixada) BordaNotasDourada else Tinta.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Barra dourada à esquerda quando a nota está fixada (como no notes.css)
        if (extra.fixada) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(Ouro)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (extra.fixada) 18.dp else 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
                .animateContentSize(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
        ) {
            // Cabeçalho: badge de tipo + autor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TintaNotasAcento.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        IconeTipoNota(tipo = nota.tipo, cor = TintaNotasAcento, modifier = Modifier.size(13.dp))
                        Text(
                            text = rotuloTipo,
                            style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = Tinta
                        )
                    }
                }

                Text(
                    text = quem,
                    style = TextStyle(
                        fontFamily = FonteSerifaNotas,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp
                    ),
                    color = TintaSuave
                )
            }

            if (nota.titulo.isNotBlank()) {
                Text(
                    text = nota.titulo,
                    style = TextStyle(
                        fontFamily = FonteSerifaNotas,
                        fontSize = 23.sp,
                        lineHeight = 27.sp
                    ),
                    color = Tinta,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // Corpo de acordo com o tipo da nota
            when (nota.tipo) {
                "lista" -> {
                    val linhas = nota.conteudo.lines().filter { it.isNotBlank() }
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        linhas.forEachIndexed { idx, linhaBruta ->
                            val feita = linhaBruta.trimStart().startsWith("[x]", ignoreCase = true)
                            val textoLimpo = linhaBruta.trimStart()
                                .removePrefix("[x]")
                                .removePrefix("[X]")
                                .removePrefix("[ ]")
                                .trim()

                            val pulso by animateFloatAsState(
                                targetValue = if (feita) 1.12f else 1f,
                                animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
                                label = "pulsoItemLista"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!casalId.isNullOrBlank()) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val novasLinhas = linhas.mapIndexed { i, l ->
                                                if (i == idx) {
                                                    val clean = l.trimStart()
                                                        .removePrefix("[x]")
                                                        .removePrefix("[X]")
                                                        .removePrefix("[ ]")
                                                        .trim()
                                                    if (feita) "[ ] $clean" else "[x] $clean"
                                                } else {
                                                    l
                                                }
                                            }
                                            FirebaseDatabase.getInstance()
                                                .getReference("casais/$casalId/notas/${nota.chave}/content")
                                                .setValue(novasLinhas.joinToString("\n"))
                                        }
                                    }
                                    .padding(vertical = 7.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .graphicsLayer {
                                            scaleX = pulso
                                            scaleY = pulso
                                        }
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (feita) TintaNotasAcento else Color.Transparent)
                                        .border(1.5.dp, TintaNotasAcento, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (feita) {
                                        Canvas(modifier = Modifier.size(11.dp)) {
                                            val p = Path().apply {
                                                moveTo(size.width * 0.2f, size.height * 0.52f)
                                                lineTo(size.width * 0.44f, size.height * 0.76f)
                                                lineTo(size.width * 0.84f, size.height * 0.24f)
                                            }
                                            drawPath(p, Creme, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                                        }
                                    }
                                }

                                Text(
                                    text = textoLimpo,
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp,
                                        textDecoration = if (feita) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (feita) TintaSuave else Tinta,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                "carta" -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (minha || extra.aberta) {
                        Text(
                            text = nota.conteudo,
                            style = TextStyle(
                                fontFamily = FonteSerifaNotas,
                                fontSize = 18.sp,
                                lineHeight = 25.sp
                            ),
                            color = Tinta
                        )
                        if (minha) {
                            Text(
                                text = if (extra.aberta) "Aberta pela outra pessoa" else "Ainda lacrada para quem recebe",
                                style = TextStyle(fontFamily = Sem, fontSize = 11.sp),
                                color = Ocre,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        // Envelope lacrado até a outra pessoa abrir
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Ouro.copy(alpha = 0.16f))
                                .border(0.8.dp, BordaNotasDourada.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Ouro)
                                    .border(2.dp, Ouro.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✦", fontSize = 11.sp, color = Tinta)
                            }
                            Text(
                                text = "Uma carta lacrada esperando por você.",
                                style = TextStyle(
                                    fontFamily = FonteSerifaNotas,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 16.sp
                                ),
                                color = Tinta
                            )
                            Button(
                                onClick = aoAbrirCarta,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Creme),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Abrir carta",
                                    style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                                )
                            }
                        }
                    }
                }

                "pergunta" -> {
                    if (nota.conteudo.isNotBlank()) {
                        Text(
                            text = nota.conteudo,
                            style = TextStyle(
                                fontFamily = Sem,
                                fontSize = 13.5.sp,
                                lineHeight = 20.sp
                            ),
                            color = TintaSuave,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TintaNotasAcento.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (nota.resposta.isNotBlank()) {
                            Text(
                                text = "RESPOSTA",
                                style = TextStyle(
                                    fontFamily = Sem,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.4.sp
                                ),
                                color = Ocre
                            )
                            Text(
                                text = nota.resposta,
                                style = TextStyle(
                                    fontFamily = FonteSerifaNotas,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 18.sp,
                                    lineHeight = 23.sp
                                ),
                                color = Tinta
                            )
                        } else if (!minha) {
                            Text(
                                text = "Sua vez de responder",
                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                color = Ocre
                            )
                            OutlinedTextField(
                                value = respostaDigitada,
                                onValueChange = { respostaDigitada = it },
                                placeholder = { Text("Escreva sua resposta…", fontSize = 12.5.sp) },
                                minLines = 2,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.75f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                                    focusedBorderColor = TintaNotasAcento,
                                    unfocusedBorderColor = BordaNotasDourada.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                enabled = respostaDigitada.isNotBlank() && !casalId.isNullOrBlank(),
                                onClick = {
                                    val cid = casalId ?: return@Button
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    FirebaseDatabase.getInstance()
                                        .getReference("casais/$cid/notas/${nota.chave}/answer")
                                        .setValue(respostaDigitada.trim())
                                    respostaDigitada = ""
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Creme),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Responder", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Text(
                                text = "Aguardando resposta…",
                                style = TextStyle(
                                    fontFamily = FonteSerifaNotas,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 15.sp
                                ),
                                color = TintaSuave
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = nota.conteudo,
                        style = TextStyle(
                            fontFamily = Sem,
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp
                        ),
                        color = TintaSuave,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Rodapé com data e ações táteis (44dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .border(0.dp, Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nota.data,
                    style = TextStyle(fontFamily = Sem, fontSize = 11.sp),
                    color = TintaSuave
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BotaoAcaoNota(
                        ativo = extra.fixada,
                        onClick = aoAlternarFixar
                    ) {
                        Text(
                            text = "★",
                            fontSize = 13.sp,
                            color = if (extra.fixada) Ocre else TintaSuave
                        )
                    }

                    if (!isCarta) {
                        BotaoAcaoNota(
                            ativo = false,
                            onClick = aoEditar
                        ) {
                            Text(
                                text = "✎",
                                fontSize = 13.sp,
                                color = TintaSuave
                            )
                        }
                    }

                    BotaoAcaoNota(
                        ativo = false,
                        onClick = aoApagar
                    ) {
                        Text(
                            text = "×",
                            fontSize = 16.sp,
                            color = PerigoNotaCor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BotaoAcaoNota(
    ativo: Boolean,
    onClick: () -> Unit,
    conteudo: @Composable () -> Unit
) {
    var pressionado by remember { mutableStateOf(false) }
    // O pointerInput(Unit) não se reinicia; este estado garante que o toque use sempre o onClick atual.
    val aoTocar by rememberUpdatedState(onClick)
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "escalaAcaoNota"
    )
    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(CircleShape)
            .background(if (ativo) Ouro.copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = 0.8.dp,
                color = if (ativo) BordaNotasDourada else Tinta.copy(alpha = 0.16f),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressionado = true
                        tryAwaitRelease()
                        pressionado = false
                    },
                    onTap = { aoTocar() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        conteudo()
    }
}

@Composable
private fun SheetNovaNota(
    dados: DadosDoCasal,
    aoFechar: () -> Unit,
    aoCriada: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    var passo by rememberSaveable { mutableStateOf(1) }
    var tipo by rememberSaveable { mutableStateOf("texto") }
    var titulo by rememberSaveable { mutableStateOf("") }
    var conteudo by rememberSaveable { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }

    val progresso by animateFloatAsState(
        targetValue = if (passo == 1) 0.5f else 1f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "progressoNovaNota"
    )

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = sheetState,
        containerColor = CartaoNotasTopo,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Barra de progresso dos 2 passos (como no forms.css)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(Tinta.copy(alpha = 0.14f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progresso)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(TintaNotasAcento)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1. Tipo",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = if (passo >= 1) Tinta else TintaSuave
                    )
                    Text(
                        text = "2. Conteúdo",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = if (passo == 2) Tinta else TintaSuave
                    )
                }
            }

            AnimatedContent(
                targetState = passo,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 3 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { -it / 3 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { it / 3 } + fadeOut(tween(180)))
                    }
                },
                label = "passosNovaNota"
            ) { etapa ->
                if (etapa == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "O que você quer criar?",
                            style = TextStyle(fontFamily = FonteSerifaNotas, fontSize = 26.sp),
                            color = Tinta
                        )

                        // Grade 2x2 com os 4 tipos do site
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TIPOS_NOTA.chunked(2).forEach { par ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    par.forEach { info ->
                                        val selecionado = tipo == info.chave
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (selecionado) Color.White else Color.White.copy(alpha = 0.5f))
                                                .border(
                                                    width = if (selecionado) 1.8.dp else 1.dp,
                                                    color = if (selecionado) TintaNotasAcento else Tinta.copy(alpha = 0.16f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    tipo = info.chave
                                                }
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconeTipoNota(
                                                tipo = info.chave,
                                                cor = TintaNotasAcento,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = info.nome,
                                                style = TextStyle(fontFamily = FonteSerifaNotas, fontSize = 20.sp),
                                                color = Tinta,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                            Text(
                                                text = info.descricao,
                                                style = TextStyle(fontFamily = Sem, fontSize = 11.sp, lineHeight = 15.sp),
                                                color = TintaSuave,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                passo = 2
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Creme),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Continuar →",
                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = when (tipo) {
                                "carta" -> "Escreva a carta"
                                "pergunta" -> "Faça uma pergunta"
                                "lista" -> "Monte a lista"
                                else -> "Escreva o conteúdo"
                            },
                            style = TextStyle(fontFamily = FonteSerifaNotas, fontSize = 26.sp),
                            color = Tinta
                        )

                        if (tipo == "carta") {
                            Text(
                                text = "Você lê a carta agora. Quem recebe vê um envelope lacrado até abrir.",
                                style = TextStyle(
                                    fontFamily = FonteSerifaNotas,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 15.sp
                                ),
                                color = TintaSuave
                            )
                        }

                        OutlinedTextField(
                            value = titulo,
                            onValueChange = { titulo = it.take(80) },
                            label = { Text(if (tipo == "pergunta") "Pergunta" else "Título (opcional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = conteudo,
                            onValueChange = {
                                conteudo = it
                                if (aviso.isNotBlank()) aviso = ""
                            },
                            label = {
                                Text(
                                    when (tipo) {
                                        "lista" -> "Itens, um por linha"
                                        "pergunta" -> "Contexto ou detalhe (opcional)"
                                        "carta" -> "Mensagem da carta"
                                        else -> "Conteúdo"
                                    }
                                )
                            },
                            minLines = 4,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (aviso.isNotBlank()) {
                            Text(aviso, fontSize = 12.sp, color = PerigoNotaCor)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { passo = 1 },
                                modifier = Modifier.weight(0.35f)
                            ) {
                                Text("← Voltar", color = TintaSuave)
                            }
                            Button(
                                onClick = {
                                    val tituloFinal = titulo.trim()
                                    val conteudoFinal = conteudo.trim()
                                    if (conteudoFinal.isBlank() && !(tipo == "pergunta" && tituloFinal.isNotBlank())) {
                                        aviso = "Escreva o conteúdo da nota."
                                    } else {
                                        val chave = dados.criarNota(tituloFinal, conteudoFinal, tipo)
                                        if (chave == null) {
                                            aviso = "Não consegui salvar agora. Tente de novo."
                                        } else {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            aoCriada(chave)
                                            aoFechar()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Creme),
                                modifier = Modifier
                                    .weight(0.65f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Gravar",
                                    style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SheetEditarNota(
    dados: DadosDoCasal,
    nota: Nota,
    aoFechar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    var titulo by rememberSaveable(nota.chave) { mutableStateOf(nota.titulo) }
    var conteudo by rememberSaveable(nota.chave) { mutableStateOf(nota.conteudo) }

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = sheetState,
        containerColor = CartaoNotasTopo,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Editar anotação",
                style = TextStyle(fontFamily = FonteSerifaNotas, fontSize = 26.sp),
                color = Tinta
            )
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it.take(80) },
                label = { Text("Título") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = conteudo,
                onValueChange = { conteudo = it },
                label = { Text(if (nota.tipo == "lista") "Itens, um por linha" else "Conteúdo") },
                minLines = 4,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = conteudo.isNotBlank() || titulo.isNotBlank(),
                onClick = {
                    val casalId = dados.casalId
                    if (!casalId.isNullOrBlank()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        FirebaseDatabase.getInstance()
                            .getReference("casais/$casalId/notas/${nota.chave}")
                            .updateChildren(
                                mapOf(
                                    "title" to titulo.trim(),
                                    "content" to conteudo.trim()
                                )
                            )
                    }
                    aoFechar()
                },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tinta, contentColor = Creme),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Salvar alterações",
                    style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun IconeCadeado(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = cor,
            topLeft = Offset(w * 0.16f, h * 0.46f),
            size = Size(w * 0.68f, h * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = stroke
        )
        val arco = Path().apply {
            moveTo(w * 0.30f, h * 0.46f)
            lineTo(w * 0.30f, h * 0.31f)
            arcTo(
                rect = Rect(w * 0.30f, h * 0.12f, w * 0.70f, h * 0.50f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w * 0.70f, h * 0.46f)
        }
        drawPath(arco, color = cor, style = stroke)
    }
}

@Composable
private fun IconeTipoNota(tipo: String, cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tipo) {
            "lista" -> {
                listOf(0.28f, 0.5f, 0.72f).forEach { y ->
                    drawCircle(cor, radius = 1.2.dp.toPx(), center = Offset(w * 0.18f, h * y))
                    drawLine(cor, Offset(w * 0.34f, h * y), Offset(w * 0.86f, h * y), stroke.width, StrokeCap.Round)
                }
            }
            "pergunta" -> {
                drawCircle(cor, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(cor, radius = 1.2.dp.toPx(), center = Offset(w * 0.5f, h * 0.72f))
                val curva = Path().apply {
                    moveTo(w * 0.38f, h * 0.38f)
                    quadraticTo(w * 0.5f, h * 0.24f, w * 0.6f, h * 0.38f)
                    quadraticTo(w * 0.6f, h * 0.48f, w * 0.5f, h * 0.54f)
                }
                drawPath(curva, color = cor, style = stroke)
            }
            "carta" -> {
                drawRoundRect(
                    color = cor,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.56f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx()),
                    style = stroke
                )
                val aba = Path().apply {
                    moveTo(w * 0.14f, h * 0.26f)
                    lineTo(w * 0.5f, h * 0.54f)
                    lineTo(w * 0.86f, h * 0.26f)
                }
                drawPath(aba, color = cor, style = stroke)
            }
            else -> {
                drawLine(cor, Offset(w * 0.18f, h * 0.3f), Offset(w * 0.82f, h * 0.3f), stroke.width, StrokeCap.Round)
                drawLine(cor, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(cor, Offset(w * 0.18f, h * 0.7f), Offset(w * 0.62f, h * 0.7f), stroke.width, StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun DivisorNotas(modifier: Modifier = Modifier) {
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