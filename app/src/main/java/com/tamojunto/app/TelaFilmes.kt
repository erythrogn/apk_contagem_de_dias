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

// Carrossel de destaque, como no site: a capa do centro em tamanho cheio, as vizinhas menores
// e esmaecidas. Desliza com o dedo, tem setas, contador, faixa de miniaturas e o título embaixo.
@Composable
private fun DestaqueFilmes(visiveis: List<Item>, secao: Secao, aoAbrir: (String) -> Unit) {
    val total = visiveis.size
    val pagerState = rememberPagerState(pageCount = { total })
    val escopo = rememberCoroutineScope()
    val atual = pagerState.currentPage
    val listaMiniaturas = rememberLazyListState()

    // A faixa de miniaturas acompanha o item do centro.
    LaunchedEffect(atual) { listaMiniaturas.animateScrollToItem(atual) }

    // O progresso da barra desliza até a posição nova.
    val progresso by animateFloatAsState(
        targetValue = (atual + 1).toFloat() / total,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progressoCarrossel"
    )

    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(430.dp),
            contentPadding = PaddingValues(horizontal = 58.dp),
            pageSpacing = 14.dp
        ) { pagina ->
            val midia = visiveis[pagina]
            Box(
                Modifier
                    .fillMaxSize()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(4.dp), ambientColor = Tinta, spotColor = Tinta)
                    // Quanto mais perto do centro, maior e mais nítida. A escala é lida no desenho, sem recompor.
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
                    modifier = Modifier.fillMaxSize().border(1.dp, Ouro.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                )
            }
        }

        // Setas nas pontas e o contador no meio, com a barra de progresso embaixo.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotaoSeta(
                caminho = "M15 19l-7-7 7-7",
                habilitado = atual > 0,
                aoClicar = { escopo.launch { pagerState.animateScrollToPage(atual - 1) } }
            )
            Column(Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${atual + 1} de $total", style = EstiloRotulo, color = TintaSuave)
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(2.dp).background(Tinta.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(progresso).height(2.dp).background(Ouro))
                }
            }
            BotaoSeta(
                caminho = "M9 18l6-6-6-6",
                habilitado = atual < total - 1,
                aoClicar = { escopo.launch { pagerState.animateScrollToPage(atual + 1) } }
            )
        }

        // Título e a ficha. Ao trocar de item, o texto dá uma leve transição.
        Crossfade(targetState = atual, label = "textoCarrossel") { pagina ->
            visiveis.getOrNull(pagina)?.let { midia ->
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        midia.titulo.ifBlank { "Sem título" },
                        style = EstiloTituloSecao,
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

        // Faixa de miniaturas: o item do centro fica destacado; tocar numa miniatura leva até ela.
        LazyRow(
            state = listaMiniaturas,
            contentPadding = PaddingValues(horizontal = 20.dp),
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
                        .width(54.dp)
                        .aspectRatio(2f / 3f)
                        .graphicsLayer {
                            scaleX = escala
                            scaleY = escala
                            alpha = if (selecionada) 1f else 0.7f
                        }
                        .then(if (selecionada) Modifier.border(2.dp, Ouro, RoundedCornerShape(4.dp)) else Modifier)
                        .clickable { escopo.launch { pagerState.animateScrollToPage(indice) } }
                )
            }
        }
    }
}

// Seta redonda do carrossel: some para o tamanho do toque quando não há mais para onde ir.
@Composable
private fun BotaoSeta(caminho: String, habilitado: Boolean, aoClicar: () -> Unit) {
    val opacidade by animateFloatAsState(
        targetValue = if (habilitado) 1f else 0.35f,
        label = "opacidadeSeta"
    )
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { alpha = opacidade }
            .clip(RoundedCornerShape(4.dp))
            .background(Papel)
            .border(1.dp, Ouro, RoundedCornerShape(4.dp))
            .clickable(enabled = habilitado, onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        IconeLinha(caminhos = listOf(caminho), tamanho = 20.dp, cor = Tinta)
    }
}

// Escala suave ao toque: a capa encolhe um pouco enquanto o dedo está sobre ela.
@Composable
private fun Modifier.toqueSuave(fonte: MutableInteractionSource): Modifier {
    val pressionado by fonte.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pressionado) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "toque"
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

// Uma capa na grade: a imagem, o selo de "feito" (surge com animação) e o título embaixo.
@Composable
private fun CartaoMidia(item: Item, rotuloFeito: String, aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val fonte = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .toqueSuave(fonte)
            .clickable(interactionSource = fonte, indication = null, onClick = aoAbrir)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            CapaMidia(poster = item.poster, titulo = item.titulo, modifier = Modifier.fillMaxSize())
            // O selo cresce com um pequeno salto quando o item vira "feito" e encolhe ao desmarcar.
            val escalaSelo by animateFloatAsState(
                targetValue = if (item.feito) 1f else 0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "selo"
            )
            if (escalaSelo > 0.01f) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .graphicsLayer {
                            scaleX = escalaSelo
                            scaleY = escalaSelo
                            alpha = escalaSelo.coerceIn(0f, 1f)
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Ouro)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(rotuloFeito, style = EstiloRotulo, color = Tinta)
                }
            }
        }
        Text(
            item.titulo.ifBlank { "Sem título" },
            style = EstiloApoio,
            color = Tinta,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        val meta = listOf(item.ano, item.genero).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, style = EstiloRotulo, color = TintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Uma linha da lista: miniatura, título, ano e gênero, e a situação à direita.
@Composable
private fun LinhaMidia(item: Item, situacao: String, aoAbrir: () -> Unit, modifier: Modifier = Modifier) {
    val fonte = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .toqueSuave(fonte)
            .clickable(interactionSource = fonte, indication = null, onClick = aoAbrir)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CapaMidia(poster = item.poster, titulo = item.titulo, modifier = Modifier.width(52.dp).aspectRatio(2f / 3f))
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                item.titulo.ifBlank { "Sem título" },
                style = EstiloApoio,
                color = Tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = listOf(item.ano, item.genero).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, style = EstiloRotulo, color = TintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(situacao, style = EstiloRotulo, color = if (item.feito) Ocre else TintaSuave, textAlign = TextAlign.End)
    }
}

// Ficha do item em painel inferior. Tudo o que se faz com um item está aqui, com nome claro:
// situação, notas, onde assistir, trailer e apagar.
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
    var onde by remember(item.chave) { mutableStateOf(item.onde) }
    var confirmarApagar by remember { mutableStateOf(false) }
    val (minha, dela) = notasDoCasal(item, eu)
    val statusAtual = if (secao == Secao.FILMES) (if (item.feito) "assistido" else "assistir") else item.status

    fun mudarStatus(valor: String) {
        if (secao == Secao.FILMES) {
            dados.atualizarItem("filmes", item.chave, mapOf("watched" to (valor == "assistido")), item.titulo) {}
        } else {
            dados.atualizarItem(secao.colecao, item.chave, mapOf("status" to valor), item.titulo) {}
        }
    }

    fun salvarNota(nota: Int) {
        val campo = campoNota(eu) ?: return
        dados.atualizarItem(secao.colecao, item.chave, mapOf(campo to nota), item.titulo) {}
    }

    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = estado) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CapaMidia(poster = item.poster, titulo = item.titulo, modifier = Modifier.width(170.dp).aspectRatio(2f / 3f))
            Text(item.titulo.ifBlank { "Sem título" }, style = EstiloTituloSecao, textAlign = TextAlign.Center)
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
                Text("Indicado por ${item.quem}", style = EstiloRotulo, color = TintaSuave)
            }
            if (secao == Secao.JOGOS && item.plataforma.isNotBlank()) {
                Text("Plataforma: ${item.plataforma}", style = EstiloRotulo, color = TintaSuave)
            }

            // Situação: um toque troca. O item marcado fica destacado.
            Text("Situação", style = EstiloTituloSecao, color = Tinta)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secao.statusOpcoes.drop(1).forEach { (valor, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = statusAtual == valor, aoClicar = { mudarStatus(valor) })
                }
            }

            // Notas: cada pessoa dá a sua.
            Text("Notas", style = EstiloTituloSecao, color = Tinta)
            if (eu == null) {
                Text("Entre no casal para dar sua nota.", style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nomeEu, style = EstiloRotulo, color = Tinta, modifier = Modifier.width(90.dp))
                Estrelas(valor = minha, aoMudar = if (eu != null) ({ nota: Int -> salvarNota(nota) }) else null, tamanho = 26.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nomeOutra, style = EstiloRotulo, color = Tinta, modifier = Modifier.width(90.dp))
                Estrelas(valor = dela, tamanho = 26.dp)
            }

            // Onde assistir: salvo só quando a pessoa aperta o botão.
            OutlinedTextField(
                value = onde,
                onValueChange = { onde = it },
                label = { Text("Onde assistir ou jogar") },
                singleLine = true,
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

// Cadastro de um item. A capa e a ficha são buscadas enquanto a pessoa digita o título.
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

    // Busca automática da capa: espera a pessoa parar de digitar.
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

    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = estado) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Novo ${secao.singular}", style = EstiloTituloSecao, textAlign = TextAlign.Center)
            CapaMidia(
                poster = ficha?.poster.orEmpty(),
                titulo = titulo,
                modifier = Modifier.width(130.dp).aspectRatio(2f / 3f)
            )
            if (buscando) {
                Text("Procurando a capa…", style = EstiloRotulo, color = TintaSuave)
            } else if (ficha != null && ficha?.poster.isNullOrBlank()) {
                Text("Capa não encontrada. Você pode salvar mesmo assim.", style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
            }

            OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ano, onValueChange = { ano = it }, label = { Text("Ano (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = genero, onValueChange = { genero = it }, label = { Text("Gênero (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = indicado, onValueChange = { indicado = it }, label = { Text("Quem indicou (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = quando,
                onValueChange = { quando = it },
                label = { Text("Data (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                isError = !dataValida,
                supportingText = { if (!dataValida) Text("Use o formato AAAA-MM-DD") },
                modifier = Modifier.fillMaxWidth()
            )
            if (secao == Secao.JOGOS) {
                OutlinedTextField(value = plataforma, onValueChange = { plataforma = it }, label = { Text("Plataforma (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (secao != Secao.FILMES) {
                Text("Situação", style = EstiloRotulo, color = TintaSuave)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
