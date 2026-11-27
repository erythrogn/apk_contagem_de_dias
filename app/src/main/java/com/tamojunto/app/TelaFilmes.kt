package com.tamojunto.app

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Filmes, séries e jogos. Cada seção tem a coleção no banco, os textos e os status do site.
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

private fun generosDe(texto: String): List<String> =
    texto.split(',').map { it.trim() }.filter { it.isNotBlank() }

// Filtro de status: "assistir" e "assistido" usam o campo watched; o resto usa o campo status.
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

// Texto da situação de um item, como aparece na lista.
private fun Secao.situacaoDe(item: Item): String = when (this) {
    Secao.FILMES -> if (item.feito) "Assistido" else "Para assistir"
    else -> statusOpcoes.firstOrNull { it.first == item.status }?.second.orEmpty()
}

// Campo de nota de cada pessoa (mesmo nome usado pelo site).
private fun campoNota(slot: String?): String? = when (slot) {
    "pessoa1" -> "ratingPessoa1"
    "pessoa2" -> "ratingPessoa2"
    else -> null
}

// Notas do casal: a minha e a da outra pessoa.
private fun notasDoCasal(item: Item, eu: String?): Pair<Int, Int> = when (eu) {
    "pessoa2" -> item.nota2 to item.nota1
    else -> item.nota1 to item.nota2
}

// Capa. Sem capa, mostra o título no papel do site.
@Composable
private fun CapaMidia(poster: String, titulo: String, modifier: Modifier = Modifier, cinza: Boolean = false) {
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Tinta.copy(alpha = 0.08f)),
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
            Text(
                titulo.ifBlank { "Sem capa" },
                style = EstiloApoio,
                color = TintaSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

// Tela de Filmes, Séries e Jogos: capas ou lista. Tocar num item abre a ficha, onde tudo se faz.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFilmes(dados: DadosDoCasal) {
    val contexto = LocalContext.current
    var secao by rememberSaveable { mutableStateOf(Secao.FILMES) }
    var statusEscolhido by rememberSaveable { mutableStateOf("todos") }
    var generoEscolhido by rememberSaveable { mutableStateOf("todos") }
    var modo by rememberSaveable { mutableStateOf("capas") }
    var novaAberta by rememberSaveable { mutableStateOf(false) }
    var chaveAberta by rememberSaveable { mutableStateOf<String?>(null) }
    var nomesPessoas by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

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
    val generos = lista.flatMap { generosDe(it.genero) }.distinct().sorted()
    val visiveis = lista.filter {
        it.combinaStatus(statusEscolhido) &&
            (generoEscolhido == "todos" || generoEscolhido in generosDe(it.genero))
    }
    val feitos = lista.count { it.feito }
    val fracao = if (lista.isEmpty()) 0f else feitos.toFloat() / lista.size
    // A barra de progresso desliza até o novo valor, em vez de pular.
    val fracaoAnimada by animateFloatAsState(
        targetValue = fracao,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "progresso"
    )
    val aberto = chaveAberta?.let { chave -> lista.firstOrNull { it.chave == chave } }

    fun trocarSecao(nova: Secao) {
        secao = nova
        statusEscolhido = "todos"
        generoEscolhido = "todos"
        chaveAberta = null
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (modo == "capas") 2 else 1),
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabeçalho: seções, progresso, o botão de adicionar, a forma de ver e os filtros.
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Crossfade(targetState = secao, label = "titulo") { s ->
                    Text("nossos ${s.plural}", style = EstiloTituloSecao, textAlign = TextAlign.Center)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Secao.entries.forEach { s ->
                        AbaSite(texto = s.rotulo, ativa = secao == s, aoClicar = { trocarSecao(s) })
                    }
                }
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "$feitos de ${lista.size} ${secao.rotuloFeitos}",
                        style = EstiloRotulo,
                        color = TintaSuave,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).background(Tinta.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxWidth(fracaoAnimada).height(3.dp).background(Ouro))
                    }
                }
                BotaoSite(
                    texto = "Adicionar ${secao.singular}",
                    aoClicar = { novaAberta = true },
                    primario = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FiltroSite(texto = "Capas", ativo = modo == "capas", aoClicar = { modo = "capas" })
                    FiltroSite(texto = "Lista", ativo = modo == "lista", aoClicar = { modo = "lista" })
                    FiltroSite(texto = "Carrossel", ativo = modo == "destaque", aoClicar = { modo = "destaque" })
                }
                Text("Mostrar", style = EstiloRotulo, color = TintaSuave)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    secao.statusOpcoes.forEach { (valor, rotulo) ->
                        FiltroSite(texto = rotulo, ativo = statusEscolhido == valor, aoClicar = { statusEscolhido = valor })
                    }
                }
                if (generos.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FiltroSite(texto = "Todos os gêneros", ativo = generoEscolhido == "todos", aoClicar = { generoEscolhido = "todos" })
                        generos.forEach { g ->
                            FiltroSite(texto = g, ativo = generoEscolhido == g, aoClicar = { generoEscolhido = g })
                        }
                    }
                }
            }
        }

        if (visiveis.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (lista.isEmpty()) "Ainda não há ${secao.plural} na lista. Toque em Adicionar ${secao.singular}."
                    else "Nada com este filtro.",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        } else if (modo == "destaque") {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // Trocar de filtro recria o carrossel, que volta ao primeiro item.
                key(secao, statusEscolhido, generoEscolhido) {
                    DestaqueFilmes(visiveis = visiveis, secao = secao, aoAbrir = { chaveAberta = it })
                }
            }
        } else if (modo == "capas") {
            items(visiveis, key = { it.chave }) { midia ->
                CartaoMidia(
                    item = midia,
                    rotuloFeito = secao.rotuloFeito(),
                    aoAbrir = { chaveAberta = midia.chave },
                    modifier = Modifier.animateItem()
                )
            }
        } else {
            visiveis.forEach { midia ->
                item(key = midia.chave, span = { GridItemSpan(maxLineSpan) }) {
                    LinhaMidia(
                        item = midia,
                        situacao = secao.situacaoDe(midia),
                        aoAbrir = { chaveAberta = midia.chave },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

private val UNIDADES = listOf("dias" to "dias", "meses" to "meses", "anos" to "anos")

private fun Tela.botoesUnidade(y: Float, altura: Float, unidadeAtual: String, larguraTotal: Float, x0: Float) {
    val espaco = 6f
    val largura = (larguraTotal - espaco * (UNIDADES.size - 1)) / UNIDADES.size
    val tamFonte = (altura * 0.36f).coerceIn(9.5f, 11f)
    UNIDADES.forEachIndexed { i, (chave, rotulo) ->
        val x = x0 + i * (largura + espaco)
        val ativa = chave == unidadeAtual
        retangulo(x, y, largura, altura, 9f, if (ativa) BotaoAtivoPastel else BotaoInativoPastel)
        if (!ativa) {
            retangulo(x, y, largura, altura, 9f, BordaOuroPastel.copy(alpha = 0.7f), contorno = true, espessura = 0.7f)
        }
        val pincel = fonte(if (ativa) CremePastel else TintaPastel, R.font.montserrat_semibold, tamFonte)
        texto(rotulo, x + largura / 2f, baselineCentro(y + altura / 2f, pincel), pincel, largura - 6f, Paint.Align.CENTER)
        zona(x, y, largura, altura, actionRunCallback<SetarUnidadeCallback>(actionParametersOf(ChaveUnidade to chave)))
    }
}
