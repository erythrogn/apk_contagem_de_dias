package com.tamojunto.app

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Secao(
    val rotulo: String,
    val singular: String,
    val plural: String,
    val colecao: String,
    val rotuloFeitos: String,
    val statusOpcoes: List<Pair<String, String>>
) {
    FILMES(
        "Filmes", "filme", "filmes", "filmes", "assistidos",
        listOf("todos" to "Todos", "assistir" to "Para assistir", "assistido" to "Assistidos")
    ),
    SERIES(
        "Séries", "série", "séries", "series", "concluídas",
        listOf("todos" to "Todas", "quero ver" to "Quero ver", "assistindo" to "Assistindo", "concluida" to "Concluídas")
    ),
    JOGOS(
        "Jogos", "jogo", "jogos", "jogos", "zerados",
        listOf("todos" to "Todos", "quero jogar" to "Quero jogar", "jogando" to "Jogando", "zerado" to "Zerados")
    )
}

private val FonteSerifaMidia = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold)
)

private val CartaoMidiaTopo = Color(0xFFFBF8EC)
private val CartaoMidiaBase = Color(0xFFF3EFD9)
private val BordaMidiaDourada = Color(0xFFD6BE7A)
private val TintaMidiaAcento = Color(0xFF3F6FA6)
private val AzulPillMidia = Color(0xFFC7DAEC)

private fun generosDe(texto: String): List<String> =
    texto.split(',').map { it.trim() }.filter { it.isNotBlank() }

private fun Item.combinaStatus(filtro: String): Boolean = when (filtro) {
    "todos" -> true
    "assistir" -> !feito
    "assistido" -> feito
    else -> status == filtro
}

private fun Secao.rotuloFeito(): String = when (this) {
    Secao.FILMES -> "Assistido"
    Secao.SERIES -> "Concluída"
    Secao.JOGOS -> "Zerado"
}

private fun Secao.situacaoDe(item: Item): String = when (this) {
    Secao.FILMES -> if (item.feito) "Assistido" else "Para assistir"
    else -> statusOpcoes.firstOrNull { it.first == item.status }?.second.orEmpty()
}

private fun campoNota(slot: String?): String? = when (slot) {
    "pessoa1" -> "ratingPessoa1"
    "pessoa2" -> "ratingPessoa2"
    else -> null
}

private fun notasDoCasal(item: Item, eu: String?): Pair<Int, Int> = when (eu) {
    "pessoa2" -> item.nota2 to item.nota1
    else -> item.nota1 to item.nota2
}

@Composable
private fun CapaMidia(poster: String, titulo: String, modifier: Modifier = Modifier, cinza: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CartaoMidiaTopo, CartaoMidiaBase)
                )
            )
            .border(0.8.dp, BordaMidiaDourada.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (poster.isNotBlank()) {
            AsyncImage(
                model = poster,
                contentDescription = titulo,
                contentScale = ContentScale.Crop,
                colorFilter = if (cinza) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Ouro.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", fontSize = 12.sp, color = Ocre)
                }
                Text(
                    text = titulo.ifBlank { "Sem capa" },
                    style = TextStyle(
                        fontFamily = FonteSerifaMidia,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp
                    ),
                    color = TintaSuave,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFilmes(dados: DadosDoCasal) {
    val contexto = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyGridState()

    var secao by rememberSaveable { mutableStateOf(Secao.FILMES) }
    var statusEscolhido by rememberSaveable { mutableStateOf("todos") }
    var generoEscolhido by rememberSaveable { mutableStateOf("todos") }
    var modo by rememberSaveable { mutableStateOf("capas") }
    var filtrosAbertos by rememberSaveable { mutableStateOf(false) }
    var novaAberta by rememberSaveable { mutableStateOf(false) }
    var chaveAberta by rememberSaveable { mutableStateOf<String?>(null) }
    var nomesPessoas by remember { mutableStateOf(emptyMap<String, String>()) }

    val fabExpandido by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 60 }
    }

    LaunchedEffect(dados.casalId) {
        if (dados.casalId != null) {
            val (inicio, _) = InicioServidorCasal.carregar()
            nomesPessoas = inicio?.nomes ?: emptyMap()
        }
    }

    val eu = dados.meuSlot
    val outra = if (eu == "pessoa2") "pessoa1" else "pessoa2"
    fun nomeDe(slot: String): String = nomesPessoas[slot].orEmpty().ifBlank {
        if (slot == eu) "Você" else "Outra pessoa"
    }

    val lista: List<Item> = when (secao) {
        Secao.FILMES -> dados.filmes.toList()
        Secao.SERIES -> dados.series.toList()
        Secao.JOGOS -> dados.jogos.toList()
    }
    val generos = remember(lista) { lista.flatMap { generosDe(it.genero) }.distinct().sorted() }
    val visiveis = remember(lista, statusEscolhido, generoEscolhido) {
        lista.filter {
            it.combinaStatus(statusEscolhido) &&
                (generoEscolhido == "todos" || generoEscolhido in generosDe(it.genero))
        }
    }
    val feitos = lista.count { it.feito }
    val fracao = if (lista.isEmpty()) 0f else feitos.toFloat() / lista.size
    val fracaoAnimada by animateFloatAsState(
        targetValue = fracao,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "progressoMidia"
    )
    val aberto = chaveAberta?.let { chave -> lista.firstOrNull { it.chave == chave } }
    val temFiltroAtivo = statusEscolhido != "todos" || generoEscolhido != "todos"

    fun trocarSecao(nova: Secao) {
        if (secao != nova) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            secao = nova
            statusEscolhido = "todos"
            generoEscolhido = "todos"
            chaveAberta = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Papel)
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(if (modo == "capas") 2 else 1),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NOSSA ESTANTE",
                                style = TextStyle(
                                    fontFamily = Sem,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 2.4.sp
                                ),
                                color = Ocre
                            )
                            AnimatedContent(
                                targetState = secao,
                                transitionSpec = {
                                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                },
                                label = "tituloSecaoMidia"
                            ) { s ->
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "Nossos ",
                                        style = TextStyle(
                                            fontFamily = FonteSerifaMidia,
                                            fontSize = 32.sp,
                                            lineHeight = 36.sp
                                        ),
                                        color = Tinta
                                    )
                                    Text(
                                        text = s.plural,
                                        style = TextStyle(
                                            fontFamily = FonteSerifaMidia,
                                            fontStyle = FontStyle.Italic,
                                            fontSize = 32.sp,
                                            lineHeight = 36.sp
                                        ),
                                        color = TintaMidiaAcento
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CartaoMidiaTopo,
                            border = BorderStroke(1.dp, BordaMidiaDourada.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (lista.isNotEmpty() && feitos == lista.size) Ouro else TintaMidiaAcento)
                                )
                                Text(
                                    text = "$feitos/${lista.size}",
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    ),
                                    color = Tinta
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(AzulPillMidia.copy(alpha = 0.45f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fracaoAnimada.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(TintaMidiaAcento, Ouro)))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(CartaoMidiaTopo)
                            .border(1.dp, BordaMidiaDourada.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Secao.entries.forEach { s ->
                            val ativa = secao == s
                            val fundoTab by animateColorAsState(
                                targetValue = if (ativa) Tinta else Color.Transparent,
                                animationSpec = tween(220),
                                label = "fundoTabSecao"
                            )
                            val corTextoTab by animateColorAsState(
                                targetValue = if (ativa) Creme else TintaSuave,
                                animationSpec = tween(220),
                                label = "corTextoTabSecao"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(fundoTab)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { trocarSecao(s) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.rotulo,
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = corTextoTab
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("capas" to "Capas", "lista" to "Lista", "destaque" to "Carrossel").forEach { (valor, rotulo) ->
                                val ativo = modo == valor
                                FilterChip(
                                    selected = ativo,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        modo = valor
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = CartaoMidiaTopo.copy(alpha = 0.8f),
                                        labelColor = TintaSuave,
                                        selectedContainerColor = TintaMidiaAcento,
                                        selectedLabelColor = Creme
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = ativo,
                                        borderColor = BordaMidiaDourada.copy(alpha = 0.6f),
                                        selectedBorderColor = TintaMidiaAcento
                                    ),
                                    label = {
                                        Text(
                                            text = rotulo,
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

                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                filtrosAbertos = !filtrosAbertos
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (filtrosAbertos || temFiltroAtivo) Tinta else CartaoMidiaTopo,
                            border = BorderStroke(
                                1.dp,
                                if (filtrosAbertos || temFiltroAtivo) Tinta else BordaMidiaDourada.copy(alpha = 0.65f)
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                IconeFiltroMidia(
                                    cor = if (filtrosAbertos || temFiltroAtivo) Creme else Tinta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = filtrosAbertos,
                        enter = fadeIn(tween(200)) + expandVertically(spring(dampingRatio = 0.82f)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CartaoMidiaTopo.copy(alpha = 0.92f))
                                .border(1.dp, BordaMidiaDourada.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SITUAÇÃO",
                                style = TextStyle(
                                    fontFamily = Sem,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.4.sp
                                ),
                                color = Ocre
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                secao.statusOpcoes.forEach { (valor, rotulo) ->
                                    val ativo = statusEscolhido == valor
                                    FilterChip(
                                        selected = ativo,
                                        onClick = { statusEscolhido = valor },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color.Transparent,
                                            labelColor = TintaSuave,
                                            selectedContainerColor = AzulPillMidia,
                                            selectedLabelColor = Tinta
                                        ),
                                        label = {
                                            Text(
                                                text = rotulo,
                                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                            )
                                        }
                                    )
                                }
                            }

                            if (generos.isNotEmpty()) {
                                Text(
                                    text = "GÊNERO",
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.5.sp,
                                        letterSpacing = 1.4.sp
                                    ),
                                    color = Ocre,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = generoEscolhido == "todos",
                                        onClick = { generoEscolhido = "todos" },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = Color.Transparent,
                                            labelColor = TintaSuave,
                                            selectedContainerColor = AzulPillMidia,
                                            selectedLabelColor = Tinta
                                        ),
                                        label = {
                                            Text(
                                                text = "Todos",
                                                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                            )
                                        }
                                    )
                                    generos.forEach { g ->
                                        val ativo = generoEscolhido == g
                                        FilterChip(
                                            selected = ativo,
                                            onClick = { generoEscolhido = g },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = Color.Transparent,
                                                labelColor = TintaSuave,
                                                selectedContainerColor = AzulPillMidia,
                                                selectedLabelColor = Tinta
                                            ),
                                            label = {
                                                Text(
                                                    text = g,
                                                    style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (visiveis.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "vazio") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(CartaoMidiaTopo, CartaoMidiaBase)))
                            .border(1.dp, BordaMidiaDourada.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (lista.isEmpty()) "Nenhum ${secao.singular} por aqui ainda." else "Nada encontrado nesse filtro.",
                                style = TextStyle(
                                    fontFamily = FonteSerifaMidia,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 18.sp
                                ),
                                color = TintaSuave,
                                textAlign = TextAlign.Center
                            )
                            if (lista.isEmpty()) {
                                Text(
                                    text = "Toque no + para adicionar o primeiro",
                                    style = TextStyle(fontFamily = Sem, fontSize = 11.5.sp),
                                    color = Ocre
                                )
                            }
                        }
                    }
                }
            } else if (modo == "destaque") {
                item(span = { GridItemSpan(maxLineSpan) }, key = "carrossel") {
                    key(secao, statusEscolhido, generoEscolhido) {
                        DestaqueFilmes(
                            visiveis = visiveis,
                            secao = secao,
                            aoAbrir = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                chaveAberta = it
                            }
                        )
                    }
                }
            } else if (modo == "capas") {
                items(visiveis, key = { it.chave }) { midia ->
                    CartaoMidia(
                        item = midia,
                        rotuloFeito = secao.rotuloFeito(),
                        aoAbrir = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            chaveAberta = midia.chave
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            } else {
                visiveis.forEach { midia ->
                    item(key = midia.chave, span = { GridItemSpan(maxLineSpan) }) {
                        LinhaMidia(
                            item = midia,
                            situacao = secao.situacaoDe(midia),
                            aoAbrir = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                chaveAberta = midia.chave
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                novaAberta = true
            },
            shape = RoundedCornerShape(999.dp),
            containerColor = Tinta,
            contentColor = Creme,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (fabExpandido) 18.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        fontFamily = FonteSerifaMidia,
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
                        text = secao.singular.replaceFirstChar { it.uppercase() },
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

    if (novaAberta) {
        FormularioNovaMidia(secao = secao, dados = dados, aoFechar = { novaAberta = false })
    }

    aberto?.let { item ->
        PainelMidia(
            dados = dados,
            secao = secao,
            item = item,
            eu = eu,
            nomeEu = nomeDe(eu ?: "pessoa1"),
            nomeOutra = nomeDe(outra),
            aoAbrirTrailer = { url -> contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            aoFechar = { chaveAberta = null }
        )
    }
}

@Composable
private fun IconeFiltroMidia(cor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(cor, Offset(w * 0.15f, h * 0.32f), Offset(w * 0.85f, h * 0.32f), stroke.width, StrokeCap.Round)
        drawLine(cor, Offset(w * 0.15f, h * 0.68f), Offset(w * 0.85f, h * 0.68f), stroke.width, StrokeCap.Round)
        drawCircle(CartaoMidiaTopo, radius = 2.5.dp.toPx(), center = Offset(w * 0.38f, h * 0.32f))
        drawCircle(cor, radius = 2.5.dp.toPx(), center = Offset(w * 0.38f, h * 0.32f), style = stroke)
        drawCircle(CartaoMidiaTopo, radius = 2.5.dp.toPx(), center = Offset(w * 0.64f, h * 0.68f))
        drawCircle(cor, radius = 2.5.dp.toPx(), center = Offset(w * 0.64f, h * 0.68f), style = stroke)
    }
}

@Composable
private fun DestaqueFilmes(visiveis: List<Item>, secao: Secao, aoAbrir: (String) -> Unit) {
    val total = visiveis.size
    val pagerState = rememberPagerState(pageCount = { total })
    val escopo = rememberCoroutineScope()
    val atual = pagerState.currentPage
    val listaMiniaturas = rememberLazyListState()

    LaunchedEffect(atual) { listaMiniaturas.animateScrollToItem(atual) }

    val progresso by animateFloatAsState(
        targetValue = (atual + 1).toFloat() / total,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "progressoCarrossel"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(410.dp),
            contentPadding = PaddingValues(horizontal = 54.dp),
            pageSpacing = 14.dp
        ) { pagina ->
            val midia = visiveis[pagina]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Tinta.copy(alpha = 0.2f),
                        spotColor = Tinta.copy(alpha = 0.25f)
                    )
                    .graphicsLayer {
                        val distancia = abs((pagerState.currentPage - pagina) + pagerState.currentPageOffsetFraction)
                        val proximidade = 1f - distancia.coerceIn(0f, 1f)
                        scaleX = lerp(0.84f, 1f, proximidade)
                        scaleY = scaleX
                        alpha = lerp(0.45f, 1f, proximidade)
                    }
                    .clickable { aoAbrir(midia.chave) }
            ) {
                CapaMidia(
                    poster = midia.poster,
                    titulo = midia.titulo,
                    cinza = midia.feito,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.2.dp, BordaMidiaDourada, RoundedCornerShape(12.dp))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotaoSeta(
                caminho = "M15 19l-7-7 7-7",
                habilitado = atual > 0,
                aoClicar = { escopo.launch { pagerState.animateScrollToPage(atual - 1) } }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${atual + 1} de $total", style = EstiloRotulo, color = TintaSuave)
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(Tinta.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progresso)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(Ouro)
                    )
                }
            }
            BotaoSeta(
                caminho = "M9 18l6-6-6-6",
                habilitado = atual < total - 1,
                aoClicar = { escopo.launch { pagerState.animateScrollToPage(atual + 1) } }
            )
        }

        Crossfade(targetState = atual, label = "textoCarrossel") { pagina ->
            visiveis.getOrNull(pagina)?.let { midia ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = midia.titulo.ifBlank { "Sem título" },
                        style = TextStyle(
                            fontFamily = FonteSerifaMidia,
                            fontSize = 26.sp,
                            lineHeight = 30.sp
                        ),
                        color = Tinta,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = listOf(midia.ano, midia.genero, secao.situacaoDe(midia)).filter { it.isNotBlank() }
                    Text(meta.joinToString(" · "), style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
                    BotaoSite(
                        texto = "Abrir ficha",
                        aoClicar = { aoAbrir(midia.chave) },
                        primario = true,
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }

        LazyRow(
            state = listaMiniaturas,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(visiveis, key = { _, midia -> midia.chave }) { indice, midia ->
                val selecionada = indice == atual
                val escala by animateFloatAsState(
                    targetValue = if (selecionada) 1f else 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "miniatura"
                )
                CapaMidia(
                    poster = midia.poster,
                    titulo = midia.titulo,
                    cinza = midia.feito,
                    modifier = Modifier
                        .width(52.dp)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            alpha = if (selecionada) 1f else 0.65f
                        }
                        .then(if (selecionada) Modifier.border(2.dp, Ouro, RoundedCornerShape(10.dp)) else Modifier)
                        .clickable { escopo.launch { pagerState.animateScrollToPage(indice) } }
                )
            }
        }
    }
}

@Composable
private fun BotaoSeta(caminho: String, habilitado: Boolean, aoClicar: () -> Unit) {
    val opacidade by animateFloatAsState(
        targetValue = if (habilitado) 1f else 0.35f,
        label = "opacidadeSeta"
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { alpha = opacidade }
            .clip(CircleShape)
            .background(CartaoMidiaTopo)
            .border(1.dp, BordaMidiaDourada, CircleShape)
            .clickable(enabled = habilitado, onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        IconeLinha(caminhos = listOf(caminho), tamanho = 19.dp, cor = Tinta)
    }
}

@Composable
private fun Modifier.toqueSuave(fonte: MutableInteractionSource): Modifier {
    val pressionado by fonte.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.965f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "toqueMidia"
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

@Composable
private fun CartaoMidia(item: Item, rotuloFeito: String, aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val fonte = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .toqueSuave(fonte)
            .clip(RoundedCornerShape(12.dp))
            .background(CartaoMidiaTopo)
            .border(0.8.dp, BordaMidiaDourada.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = fonte, indication = null, onClick = aoAbrir)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            CapaMidia(poster = item.poster, titulo = item.titulo, modifier = Modifier.fillMaxSize())
            val escalaSelo by animateFloatAsState(
                targetValue = if (item.feito) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "seloMidia"
            )
            if (escalaSelo > 0.01f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .graphicsLayer {
                            scaleX = escalaSelo
                            scaleY = escalaSelo
                            alpha = escalaSelo.coerceIn(0f, 1f)
                        }
                        .clip(RoundedCornerShape(999.dp))
                        .background(Ouro)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = rotuloFeito,
                        style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                        color = Tinta
                    )
                }
            }
        }
        Text(
            text = item.titulo.ifBlank { "Sem título" },
            style = TextStyle(fontFamily = FonteSerifaMidia, fontSize = 18.sp),
            color = Tinta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val meta = listOf(item.ano, item.genero).filter { it.isNotBlank() }.joinToString(" · ")
            Text(
                text = meta.ifBlank { " " },
                style = EstiloRotulo,
                color = TintaSuave,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.nota1 > 0 || item.nota2 > 0) {
                val maior = maxOf(item.nota1, item.nota2)
                Text(
                    text = "★ $maior",
                    style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    color = Ocre,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LinhaMidia(item: Item, situacao: String, aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val fonte = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toqueSuave(fonte)
            .clip(RoundedCornerShape(12.dp))
            .background(CartaoMidiaTopo)
            .border(0.8.dp, BordaMidiaDourada.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = fonte, indication = null, onClick = aoAbrir)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CapaMidia(
            poster = item.poster,
            titulo = item.titulo,
            modifier = Modifier
                .width(50.dp)
                .aspectRatio(2f / 3f)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = item.titulo.ifBlank { "Sem título" },
                style = TextStyle(fontFamily = FonteSerifaMidia, fontSize = 19.sp),
                color = Tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOf(item.ano, item.genero).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = EstiloRotulo,
                    color = TintaSuave,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (item.feito) Ouro.copy(alpha = 0.25f) else AzulPillMidia.copy(alpha = 0.45f)
        ) {
            Text(
                text = situacao,
                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                color = if (item.feito) Ocre else Tinta,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PainelMidia(
    dados: DadosDoCasal,
    secao: Secao,
    item: Item,
    eu: String?,
    nomeEu: String,
    nomeOutra: String,
    aoAbrirTrailer: (String) -> Unit,
    aoFechar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptics = LocalHapticFeedback.current
    var onde by remember(item.chave) { mutableStateOf(item.onde) }
    var confirmarApagar by remember { mutableStateOf(false) }
    val (minha, dela) = notasDoCasal(item, eu)
    val statusAtual = if (secao == Secao.FILMES) (if (item.feito) "assistido" else "assistir") else item.status

    fun mudarStatus(valor: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (secao == Secao.FILMES) {
            dados.atualizarItem("filmes", item.chave, mapOf("watched" to (valor == "assistido")), item.titulo) {}
        } else {
            dados.atualizarItem(secao.colecao, item.chave, mapOf("status" to valor), item.titulo) {}
        }
    }

    fun salvarNota(nota: Int) {
        val campo = campoNota(eu) ?: return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        dados.atualizarItem(secao.colecao, item.chave, mapOf(campo to nota), item.titulo) {}
    }

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = estado,
        containerColor = CartaoMidiaTopo,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CapaMidia(
                poster = item.poster,
                titulo = item.titulo,
                modifier = Modifier
                    .width(156.dp)
                    .aspectRatio(2f / 3f)
                    .shadow(10.dp, RoundedCornerShape(10.dp))
            )
            Text(
                text = item.titulo.ifBlank { "Sem título" },
                style = TextStyle(fontFamily = FonteSerifaMidia, fontSize = 28.sp, lineHeight = 32.sp),
                color = Tinta,
                textAlign = TextAlign.Center
            )
            val meta = listOf(item.ano, item.genero, item.quando).filter { it.isNotBlank() }
            if (meta.isNotEmpty()) {
                Text(meta.joinToString(" · "), style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
            }
            if (item.diretor.isNotBlank()) {
                Text(
                    (if (secao == Secao.SERIES) "Criação: " else "Direção: ") + item.diretor,
                    style = EstiloApoio,
                    textAlign = TextAlign.Center
                )
            }
            if (item.elenco.isNotBlank()) {
                Text("Elenco: ${item.elenco}", style = EstiloApoio, textAlign = TextAlign.Center)
            }
            if (item.quem.isNotBlank()) {
                Text("Indicado por ${item.quem}", style = EstiloRotulo, color = Ocre)
            }
            if (secao == Secao.JOGOS && item.plataforma.isNotBlank()) {
                Text("Plataforma: ${item.plataforma}", style = EstiloRotulo, color = TintaSuave)
            }

            Text(
                text = "SITUAÇÃO",
                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.6.sp),
                color = Ocre,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secao.statusOpcoes.drop(1).forEach { (valor, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = statusAtual == valor, aoClicar = { mudarStatus(valor) })
                }
            }

            Text(
                text = "NOTAS DO CASAL",
                style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.6.sp),
                color = Ocre,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (eu == null) {
                Text("Entre no casal para dar sua nota.", style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CartaoMidiaBase.copy(alpha = 0.7f))
                    .border(0.8.dp, BordaMidiaDourada.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nomeEu, style = EstiloRotulo, color = Tinta, modifier = Modifier.width(96.dp))
                    Estrelas(valor = minha, aoMudar = if (eu != null) ({ nota: Int -> salvarNota(nota) }) else null, tamanho = 26.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nomeOutra, style = EstiloRotulo, color = Tinta, modifier = Modifier.width(96.dp))
                    Estrelas(valor = dela, tamanho = 26.dp)
                }
            }

            OutlinedTextField(
                value = onde,
                onValueChange = { onde = it },
                label = { Text("Onde assistir ou jogar") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TintaMidiaAcento,
                    unfocusedBorderColor = BordaMidiaDourada
                ),
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Salvar onde",
                aoClicar = { dados.atualizarItem(secao.colecao, item.chave, mapOf("where" to onde.trim()), item.titulo) {} },
                habilitado = onde.trim() != item.onde,
                modifier = Modifier.fillMaxWidth()
            )

            if (item.trailer.isNotBlank()) {
                BotaoSite(
                    texto = "Ver trailer",
                    aoClicar = { aoAbrirTrailer(item.trailer) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            TextButton(onClick = { confirmarApagar = true }) {
                Text("Apagar da lista", style = EstiloBotao, color = Ocre)
            }
        }
    }

    if (confirmarApagar) {
        DialogoConfirmar(
            titulo = "Apagar ${item.titulo}?",
            texto = "Isso remove da lista do casal.",
            rotuloConfirmar = "Apagar",
            aoConfirmar = {
                dados.apagarItem(secao.colecao, item.chave, item.titulo) {}
                aoFechar()
            },
            aoFechar = { confirmarApagar = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioNovaMidia(secao: Secao, dados: DadosDoCasal, aoFechar: () -> Unit) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titulo by rememberSaveable { mutableStateOf("") }
    var ano by rememberSaveable { mutableStateOf("") }
    var genero by rememberSaveable { mutableStateOf("") }
    var indicado by rememberSaveable { mutableStateOf("") }
    var quando by rememberSaveable { mutableStateOf("") }
    var plataforma by rememberSaveable { mutableStateOf("") }
    var statusNovo by rememberSaveable { mutableStateOf(secao.statusOpcoes.getOrNull(1)?.first ?: "") }
    var ficha by remember { mutableStateOf<FichaMidia?>(null) }
    var buscando by remember { mutableStateOf(false) }
    val dataValida = quando.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(quando.trim())

    LaunchedEffect(titulo, ano, secao) {
        ficha = null
        if (titulo.trim().length >= 2) {
            delay(600)
            buscando = true
            ficha = when (secao) {
                Secao.FILMES -> BuscaMidia.filmeOuSerie(false, titulo.trim(), ano.trim())
                Secao.SERIES -> BuscaMidia.filmeOuSerie(true, titulo.trim(), ano.trim())
                Secao.JOGOS -> BuscaMidia.jogo(titulo.trim())
            }
            buscando = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = aoFechar,
        sheetState = estado,
        containerColor = CartaoMidiaTopo,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Novo ${secao.singular}",
                style = TextStyle(fontFamily = FonteSerifaMidia, fontSize = 26.sp),
                color = Tinta,
                textAlign = TextAlign.Center
            )
            CapaMidia(
                poster = ficha?.poster.orEmpty(),
                titulo = titulo,
                modifier = Modifier
                    .width(124.dp)
                    .aspectRatio(2f / 3f)
            )
            if (buscando) {
                Text("Procurando a capa…", style = EstiloRotulo, color = TintaSuave)
            } else if (ficha != null && ficha?.poster.isNullOrBlank()) {
                Text("Capa não encontrada. Você pode salvar mesmo assim.", style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
            }

            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ano,
                    onValueChange = { ano = it },
                    label = { Text("Ano") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.38f)
                )
                OutlinedTextField(
                    value = genero,
                    onValueChange = { genero = it },
                    label = { Text("Gênero (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.62f)
                )
            }
            OutlinedTextField(
                value = indicado,
                onValueChange = { indicado = it },
                label = { Text("Quem indicou (opcional)") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val hoje = remember { LocalDate.now(FUSO_BELEM) }
            val atalhosData = remember(hoje) {
                listOf(
                    "Hoje" to hoje.toString(),
                    "Amanhã" to hoje.plusDays(1).toString(),
                    "Sábado" to hoje.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)).toString(),
                    "Sem data" to ""
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                atalhosData.forEach { (rotulo, iso) ->
                    FilterChip(
                        selected = quando.trim() == iso,
                        onClick = { quando = iso },
                        label = { Text(rotulo, fontSize = 11.sp) }
                    )
                }
            }

            OutlinedTextField(
                value = quando,
                onValueChange = { quando = it },
                label = { Text("Data (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                isError = !dataValida,
                supportingText = { if (!dataValida) Text("Use o formato AAAA-MM-DD") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (secao == Secao.JOGOS) {
                OutlinedTextField(
                    value = plataforma,
                    onValueChange = { plataforma = it },
                    label = { Text("Plataforma (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (secao != Secao.FILMES) {
                Text("Situação", style = EstiloRotulo, color = TintaSuave)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    secao.statusOpcoes.drop(1).forEach { (valor, rotulo) ->
                        FiltroSite(texto = rotulo, ativo = statusNovo == valor, aoClicar = { statusNovo = valor })
                    }
                }
            }

            BotaoSite(
                texto = "Salvar ${secao.singular}",
                aoClicar = {
                    val f = ficha
                    val campos = mutableMapOf<String, Any?>(
                        "title" to titulo.trim(),
                        "year" to ano.trim(),
                        "genre" to genero.trim(),
                        "who" to indicado.trim(),
                        "targetDate" to quando.trim(),
                        "poster" to (f?.poster.orEmpty()),
                        "trailer" to (f?.trailer.orEmpty()),
                        "cast" to (f?.elenco.orEmpty()),
                        "releaseDate" to (f?.dataEstreia.orEmpty())
                    )
                    when (secao) {
                        Secao.FILMES -> {
                            campos["watched"] = false
                            campos["director"] = f?.diretor.orEmpty()
                        }
                        Secao.SERIES -> {
                            campos["status"] = statusNovo.ifBlank { "quero ver" }
                            campos["creators"] = f?.diretor.orEmpty()
                        }
                        Secao.JOGOS -> {
                            campos["status"] = statusNovo.ifBlank { "quero jogar" }
                            campos["platform"] = plataforma.trim()
                            campos["where"] = ""
                        }
                    }
                    dados.criarItem(secao.colecao, campos, titulo.trim()) {}
                    aoFechar()
                },
                habilitado = titulo.isNotBlank() && dataValida,
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
