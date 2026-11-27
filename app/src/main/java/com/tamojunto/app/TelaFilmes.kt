package com.tamojunto.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Cada seção tem a coleção no banco, o rótulo e os status que o site usa em cada filtro.
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

// Filtro de status, como no site: "assistir" e "assistido" usam o campo watched; o resto usa status.
private fun Item.combinaStatus(filtro: String): Boolean = when (filtro) {
    "todos" -> true
    "assistir" -> !feito
    "assistido" -> feito
    else -> status == filtro
}

private fun Secao.rotuloMarcar(feito: Boolean): String = when (this) {
    Secao.FILMES -> if (feito) "Desmarcar como assistido" else "Marcar como assistido"
    Secao.SERIES -> if (feito) "Voltar para assistindo" else "Marcar como concluída"
    Secao.JOGOS -> if (feito) "Voltar a jogar" else "Marcar como zerado"
}

private fun Secao.rotuloFeito(): String = when (this) {
    Secao.FILMES -> "Assistido"
    Secao.SERIES -> "Concluída"
    Secao.JOGOS -> "Zerado"
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

// Capa do filme. Sem capa, mostra o título no lugar, no papel do site.
@Composable
private fun CapaMidia(poster: String, titulo: String, largura: Dp, aoClicar: (() -> Unit)? = null) {
    val base = Modifier
        .width(largura)
        .aspectRatio(2f / 3f)
        .clip(RoundedCornerShape(4.dp))
        .background(Tinta.copy(alpha = 0.08f))
    val modificador = if (aoClicar != null) base.clickable(onClick = aoClicar) else base
    Box(modificador, contentAlignment = Alignment.Center) {
        if (poster.isNotBlank()) {
            AsyncImage(
                model = poster,
                contentDescription = titulo,
                contentScale = ContentScale.Crop,
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

// Tela de Filmes, Séries e Jogos, pensada para o celular: carrossel de capas e lista logo abaixo.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFilmes(dados: DadosDoCasal) {
    val contexto = LocalContext.current
    var secao by rememberSaveable { mutableStateOf(Secao.FILMES) }
    var statusEscolhido by rememberSaveable { mutableStateOf("todos") }
    var generoEscolhido by rememberSaveable { mutableStateOf("todos") }
    var novaAberta by rememberSaveable { mutableStateOf(false) }
    var chaveAberta by rememberSaveable { mutableStateOf<String?>(null) }
    var nomesPessoas by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var aviso by remember { mutableStateOf("") }

    // Nomes das pessoas vêm do servidor, como no Início.
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

    val lista = when (secao) {
        Secao.FILMES -> dados.filmes
        Secao.SERIES -> dados.series
        Secao.JOGOS -> dados.jogos
    }
    val generos = lista.flatMap { generosDe(it.genero) }.distinct().sorted()
    val visiveis = lista.filter {
        it.combinaStatus(statusEscolhido) &&
            (generoEscolhido == "todos" || generoEscolhido in generosDe(it.genero))
    }
    val feitos = lista.count { it.feito }
    val itemAberto = chaveAberta?.let { chave -> lista.firstOrNull { it.chave == chave } }
    val pagina = rememberPagerState(pageCount = { visiveis.size })

    fun trocarSecao(nova: Secao) {
        secao = nova
        statusEscolhido = "todos"
        generoEscolhido = "todos"
    }

    fun salvarNota(item: Item, nota: Int) {
        val campo = campoNota(eu) ?: return
        dados.atualizarItem(secao.colecao, item.chave, mapOf(campo to nota), item.titulo) {}
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "nossos ${secao.plural}",
                    style = EstiloTituloSecao,
                    textAlign = TextAlign.Center
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Secao.entries.forEach { s ->
                        AbaSite(texto = s.rotulo, ativa = secao == s, aoClicar = { trocarSecao(s) })
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "$feitos de ${lista.size} ${secao.rotuloFeitos}",
                    style = EstiloRotulo,
                    color = TintaSuave,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                val fracao = if (lista.isEmpty()) 0f else feitos.toFloat() / lista.size
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Tinta.copy(alpha = 0.12f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fracao)
                            .height(3.dp)
                            .background(Ouro)
                    )
                }
            }
        }

        item {
            BotaoSite(
                texto = "Adicionar ${secao.singular}",
                aoClicar = { novaAberta = true },
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                secao.statusOpcoes.forEach { (valor, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = statusEscolhido == valor, aoClicar = { statusEscolhido = valor })
                }
            }
        }

        if (generos.isNotEmpty()) {
            item {
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

        if (visiveis.isEmpty()) {
            item {
                Text(
                    "Nada aqui ainda. Adicione ${secao.singular} ou mude o filtro.",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        } else {
            // Carrossel de capas: cada página mostra a capa, as notas e o botão de marcar.
            item {
                HorizontalPager(
                    state = pagina,
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    pageSpacing = 14.dp,
                    modifier = Modifier.fillMaxWidth().height(560.dp)
                ) { indice ->
                    val item = visiveis.getOrNull(indice) ?: return@HorizontalPager
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapaMidia(
                            poster = item.poster,
                            titulo = item.titulo,
                            largura = 180.dp,
                            aoClicar = { chaveAberta = item.chave }
                        )
                        Text(
                            item.titulo.ifBlank { "Sem título" },
                            style = EstiloTituloSecao,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val meta = listOf(item.ano, item.genero, item.quando).filter { it.isNotBlank() }
                        if (meta.isNotEmpty()) {
                            Text(
                                meta.joinToString(" · "),
                                style = EstiloRotulo,
                                color = TintaSuave,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val (minha, dela) = notasDoCasal(item, eu)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nomeDe(eu ?: "pessoa1"), style = EstiloRotulo, color = Tinta, modifier = Modifier.width(78.dp))
                            Estrelas(
                                valor = minha,
                                aoMudar = if (eu != null) ({ nota -> salvarNota(item, nota) }) else null,
                                tamanho = 20.dp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(nomeDe(outra), style = EstiloRotulo, color = Tinta, modifier = Modifier.width(78.dp))
                            Estrelas(valor = dela, tamanho = 20.dp)
                        }
                        BotaoSite(
                            texto = secao.rotuloMarcar(item.feito),
                            aoClicar = { dados.alternarFeito(secao.colecao, item) { aviso = "Não consegui salvar. Tente de novo." } },
                            primario = !item.feito,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (visiveis.isNotEmpty()) {
            item {
                Text(
                    "Todos",
                    style = EstiloTituloSecao,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(visiveis, key = { it.chave }) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { chaveAberta = item.chave },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CapaMidia(poster = item.poster, titulo = item.titulo, largura = 46.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.titulo.ifBlank { "Sem título" },
                            style = EstiloApoio,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val meta = listOf(item.ano, item.genero).filter { it.isNotBlank() }
                        Text(
                            if (item.feito) secao.rotuloFeito() else meta.joinToString(" · ").ifBlank { "Na lista" },
                            style = EstiloRotulo,
                            color = TintaSuave,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (aviso.isNotBlank()) {
            item {
                Text(aviso, style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (novaAberta) {
        FormularioNovaMidia(secao = secao, dados = dados, aoFechar = { novaAberta = false })
    }

    itemAberto?.let { item ->
        FichaMidiaPainel(
            secao = secao,
            item = item,
            eu = eu,
            nomeOutra = nomeDe(outra),
            aoNota = { nota -> salvarNota(item, nota) },
            aoMarcar = { dados.alternarFeito(secao.colecao, item) {} },
            aoAbrirTrailer = { url ->
                contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            aoSalvarOnde = { onde -> dados.atualizarItem(secao.colecao, item.chave, mapOf("where" to onde), item.titulo) {} },
            aoApagar = {
                dados.apagarItem(secao.colecao, item.chave, item.titulo) {}
                chaveAberta = null
            },
            aoFechar = { chaveAberta = null }
        )
    }
}

// Ficha de um item em painel inferior: capa, elenco, notas, onde assistir, trailer e apagar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FichaMidiaPainel(
    secao: Secao,
    item: Item,
    eu: String?,
    nomeOutra: String,
    aoNota: (Int) -> Unit,
    aoMarcar: () -> Unit,
    aoAbrirTrailer: (String) -> Unit,
    aoSalvarOnde: (String) -> Unit,
    aoApagar: () -> Unit,
    aoFechar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var onde by remember(item.chave) { mutableStateOf(item.onde) }
    var confirmarApagar by remember { mutableStateOf(false) }
    val (minha, dela) = notasDoCasal(item, eu)

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
            CapaMidia(poster = item.poster, titulo = item.titulo, largura = 200.dp)
            Text(
                item.titulo.ifBlank { "Sem título" },
                style = EstiloTituloSecao,
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
                Text("Indicação: ${item.quem}", style = EstiloRotulo, color = TintaSuave)
            }
            if (secao == Secao.JOGOS && item.plataforma.isNotBlank()) {
                Text("Plataforma: ${item.plataforma}", style = EstiloRotulo, color = TintaSuave)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Você", style = EstiloRotulo, color = Tinta, modifier = Modifier.width(78.dp))
                Estrelas(valor = minha, aoMudar = if (eu != null) aoNota else null, tamanho = 24.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(nomeOutra, style = EstiloRotulo, color = Tinta, modifier = Modifier.width(78.dp))
                Estrelas(valor = dela, tamanho = 24.dp)
            }

            OutlinedTextField(
                value = onde,
                onValueChange = { onde = it },
                label = { Text("Onde assistir") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Salvar onde assistir",
                aoClicar = { aoSalvarOnde(onde.trim()) },
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
            BotaoSite(
                texto = secao.rotuloMarcar(item.feito),
                aoClicar = aoMarcar,
                primario = !item.feito,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { confirmarApagar = true }) { Text("Apagar da lista") }
        }
    }

    if (confirmarApagar) {
        DialogoConfirmar(
            titulo = "Apagar ${item.titulo}?",
            texto = "Isso remove da lista do casal.",
            rotuloConfirmar = "Apagar",
            aoConfirmar = aoApagar,
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
    var statusNovo by rememberSaveable {
        mutableStateOf(secao.statusOpcoes.getOrNull(1)?.first ?: "")
    }
    var ficha by remember { mutableStateOf<FichaMidia?>(null) }
    var buscando by remember { mutableStateOf(false) }
    val dataValida = quando.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(quando.trim())

    // Busca automática da capa: espera a pessoa parar de digitar.
    LaunchedEffect(titulo, ano, secao) {
        ficha = null
        if (titulo.trim().length >= 2) {
            delay(700)
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
            CapaMidia(poster = ficha?.poster.orEmpty(), titulo = titulo, largura = 140.dp)
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
