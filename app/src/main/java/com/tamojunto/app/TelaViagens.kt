package com.tamojunto.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val CATEGORIAS_PARADA = linkedMapOf(
    "visitar" to "Visitar local",
    "comida" to "Comida",
    "drink" to "Drink",
    "receita" to "Receita",
    "outro" to "Outro"
)

private val FILTROS_VIAGEM = linkedMapOf(
    "todas" to "Todas",
    "sonho" to "Sonhos",
    "planejada" to "Planejadas",
    "feita" to "Realizadas"
)

private fun situacaoDe(v: Item): String = when {
    v.status == "feita" -> "feita"
    v.dataIso.isNotBlank() -> "planejada"
    else -> "sonho"
}

// Datas aceitas nos campos: AAAA-MM-DD ou vazio.
private fun dataValidaOuVazia(texto: String): Boolean =
    texto.isBlank() || dataIsoOuNull(texto) != null

private fun dataOuNull(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()

private fun periodo(v: Item): String = when {
    v.dataIso.isBlank() -> "sem data ainda"
    v.fim.isNotBlank() && v.fim != v.dataIso -> "${dataBr(v.dataIso)} a ${dataBr(v.fim)}"
    else -> dataBr(v.dataIso)
}

private fun faltamDias(iso: String): String {
    val d = dataOuNull(iso) ?: return ""
    val n = ChronoUnit.DAYS.between(LocalDate.now(), d)
    return when {
        n == 0L -> "é hoje"
        n == 1L -> "amanhã"
        n > 1L -> "faltam $n dias"
        else -> ""
    }
}

// Rótulo do dia do roteiro: "Dia 1" é o dia da ida; antes da ida, "Antes de partir".
private fun diaDoRoteiro(v: Item, iso: String): String {
    if (iso.isBlank()) return "Sem dia"
    val inicio = dataOuNull(v.dataIso) ?: return dataBr(iso)
    val d = dataOuNull(iso) ?: return dataBr(iso)
    val n = ChronoUnit.DAYS.between(inicio, d) + 1
    return if (n < 1) "Antes de partir" else "Dia $n · ${dataBr(iso)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaViagens(dados: DadosDoCasal) {
    var filtro by rememberSaveable { mutableStateOf("todas") }
    var ordem by rememberSaveable { mutableStateOf("recentes") }
    var busca by rememberSaveable { mutableStateOf("") }
    var ocultarFeitas by rememberSaveable { mutableStateOf(false) }
    var aberta by remember { mutableStateOf<String?>(null) }
    var formAberto by remember { mutableStateOf(false) }
    var editandoChave by remember { mutableStateOf<String?>(null) }
    var sorteada by remember { mutableStateOf<String?>(null) }

    val viagens = dados.viagens.toList()
    val feitas = viagens.count { situacaoDe(it) == "feita" }
    val hoje = LocalDate.now().toString()
    val proxima = viagens
        .filter { situacaoDe(it) == "planejada" && it.dataIso >= hoje }
        .minByOrNull { it.dataIso }

    val visiveis = viagens
        .filter { filtro == "todas" || situacaoDe(it) == filtro }
        .filter { !ocultarFeitas || situacaoDe(it) != "feita" }
        .filter {
            busca.isBlank() ||
                it.titulo.contains(busca.trim(), ignoreCase = true) ||
                it.notas.contains(busca.trim(), ignoreCase = true)
        }
        .let { lista ->
            when (ordem) {
                "data" -> lista.sortedBy { it.dataIso.ifBlank { "9999-12-31" } }
                "az" -> lista.sortedBy { it.titulo.lowercase() }
                else -> lista.reversed()
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nossas viagens", style = EstiloTituloSecao, textAlign = TextAlign.Center)
                Text(
                    "quero viver cada lugar com voce",
                    style = EstiloApoio,
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (proxima != null) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, Tinta.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Próxima viagem", style = EstiloRotulo, color = TintaSuave)
                    Text(proxima.titulo, style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
                    Text(periodo(proxima), style = EstiloRotulo, color = TintaSuave)
                    val dias = dataOuNull(proxima.dataIso)?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) } ?: 0L
                    Text(
                        if (dias <= 0L) faltamDias(proxima.dataIso) else "$dias",
                        style = EstiloNumero.copy(fontSize = 52.sp, lineHeight = 52.sp),
                        color = Tinta
                    )
                    if (dias > 0L) Text("dias", style = EstiloRotulo, color = TintaSuave)
                    BotaoSite(
                        texto = "Ver roteiro",
                        aoClicar = { aberta = proxima.chave },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "$feitas de ${viagens.size} realizadas",
                    style = EstiloRotulo,
                    color = TintaSuave,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                val fracao = if (viagens.isEmpty()) 0f else feitas.toFloat() / viagens.size
                Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).background(Tinta.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(fracao).height(3.dp).background(Ouro))
                }
            }
        }

        item {
            BotaoSite(
                texto = "Nova viagem",
                aoClicar = { editandoChave = null; formAberto = true },
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FILTROS_VIAGEM.forEach { (chave, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = filtro == chave, aoClicar = { filtro = chave })
                }
            }
        }

        item {
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Buscar viagem") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroSite(texto = "Mais recentes", ativo = ordem == "recentes", aoClicar = { ordem = "recentes" })
                FiltroSite(texto = "Data planejada", ativo = ordem == "data", aoClicar = { ordem = "data" })
                FiltroSite(texto = "A a Z", ativo = ordem == "az", aoClicar = { ordem = "az" })
                FiltroSite(
                    texto = "Ocultar realizadas",
                    ativo = ocultarFeitas,
                    aoClicar = { ocultarFeitas = !ocultarFeitas }
                )
            }
        }

        item {
            BotaoSite(
                texto = "Sortear um destino",
                aoClicar = {
                    val candidatas = visiveis.filter { situacaoDe(it) != "feita" }
                    sorteada = candidatas.randomOrNull()?.chave
                    aberta = sorteada
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (visiveis.isEmpty()) {
            item {
                Text(
                    if (viagens.isEmpty()) "Ainda não há viagens. Quando quiserem, adicionem a primeira."
                    else "Nenhuma viagem com este filtro.",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(visiveis, key = { it.chave }) { viagem ->
                LinhaViagem(viagem, aoAbrir = { aberta = viagem.chave })
            }
        }
    }

    val chaveAberta = aberta
    if (chaveAberta != null) {
        if (dados.viagens.any { it.chave == chaveAberta }) {
            FichaViagem(
                dados = dados,
                chave = chaveAberta,
                aoEditar = { editandoChave = chaveAberta; formAberto = true },
                aoFechar = { aberta = null }
            )
        } else {
            aberta = null
        }
    }

    if (formAberto) {
        FormularioViagem(
            dados = dados,
            editando = editandoChave?.let { c -> dados.viagens.firstOrNull { it.chave == c } },
            aoFechar = { formAberto = false; editandoChave = null }
        )
    }
}

@Composable
private fun LinhaViagem(v: Item, aoAbrir: () -> Unit) {
    val situacao = situacaoDe(v)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = aoAbrir)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CapaViagem(imagem = v.imagem, titulo = v.titulo, largura = 72.dp)
            Column(Modifier.weight(1f)) {
                Text(v.titulo, style = EstiloApoio, color = Tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(periodo(v), style = EstiloRotulo, color = TintaSuave, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when (situacao) {
                        "feita" -> "realizada"
                        "planejada" -> faltamDias(v.dataIso).ifBlank { "planejada" }
                        else -> "sonho"
                    },
                    style = EstiloRotulo,
                    color = Ocre
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.1f)))
    }
}

@Composable
private fun CapaViagem(imagem: String, titulo: String, largura: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .width(largura)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(4.dp))
            .background(Tinta.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        if (imagem.isNotBlank()) {
            AsyncImage(
                model = imagem,
                contentDescription = titulo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                titulo.firstOrNull()?.uppercase() ?: "",
                style = EstiloTituloSecao,
                color = TintaSuave
            )
        }
    }
}

// Roteiro da viagem em painel inferior: preparativos, paradas por dia e ações.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FichaViagem(
    dados: DadosDoCasal,
    chave: String,
    aoEditar: () -> Unit,
    aoFechar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val v = dados.viagens.firstOrNull { it.chave == chave } ?: return
    val paradas = dados.coisinhas
        .filter { it.viagemKey == chave }
        .sortedBy { it.dataIso.ifBlank { "9999-12-31" } }
    val livres = dados.coisinhas.filter { it.viagemKey.isBlank() && !it.feito }
    val porDia = paradas.groupBy { diaDoRoteiro(v, it.dataIso) }
    val situacao = situacaoDe(v)
    var confirmarApagar by remember { mutableStateOf(false) }
    var nomeParada by remember { mutableStateOf("") }
    var categoriaParada by remember { mutableStateOf("visitar") }
    var diaParada by remember { mutableStateOf("") }
    var notaParada by remember { mutableStateOf("") }
    var nomePasso by remember { mutableStateOf("") }
    val paradaValida = nomeParada.isNotBlank() && dataValidaOuVazia(diaParada)

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
            if (v.imagem.isNotBlank()) {
                AsyncImage(
                    model = v.imagem,
                    contentDescription = v.titulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(4.dp))
                )
            }
            Text(
                when (situacao) {
                    "feita" -> "realizada"
                    "planejada" -> faltamDias(v.dataIso).ifBlank { "planejada" }
                    else -> "sonho"
                },
                style = EstiloRotulo,
                color = Ocre
            )
            Text(v.titulo, style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
            Text(periodo(v), style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
            if (v.notas.isNotBlank()) {
                Text("Ideias: ${v.notas}", style = EstiloApoio, color = Tinta, textAlign = TextAlign.Center)
            }

            // Preparativos: cada passo pode ser marcado ou tirado da lista.
            Text("Preparativos", style = EstiloTituloSecao, color = Tinta)
            v.checklist.forEachIndexed { indice, passo ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Tinta, RoundedCornerShape(4.dp))
                            .background(if (passo.feito) Ouro else Color.Transparent)
                            .clickable {
                                val nova = v.checklist.mapIndexed { i, p ->
                                    if (i == indice) mapOf("name" to p.nome, "done" to !p.feito)
                                    else mapOf("name" to p.nome, "done" to p.feito)
                                }
                                dados.atualizarItem("viagens", chave, mapOf("checklist" to nova), v.titulo) {}
                            }
                    )
                    Text(
                        passo.nome,
                        style = EstiloApoio,
                        color = if (passo.feito) TintaSuave else Tinta,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "tirar",
                        style = EstiloRotulo,
                        color = Ocre,
                        modifier = Modifier.clickable {
                            val nova = v.checklist.filterIndexed { i, _ -> i != indice }
                                .map { mapOf("name" to it.nome, "done" to it.feito) }
                            dados.atualizarItem("viagens", chave, mapOf("checklist" to nova.ifEmpty { null }), v.titulo) {}
                        }
                    )
                }
            }
            OutlinedTextField(
                value = nomePasso,
                onValueChange = { nomePasso = it },
                label = { Text("Ex.: passaporte") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Adicionar preparativo",
                aoClicar = {
                    val nova = v.checklist.map { mapOf("name" to it.nome, "done" to it.feito) } +
                        mapOf("name" to nomePasso.trim(), "done" to false)
                    dados.atualizarItem("viagens", chave, mapOf("checklist" to nova), v.titulo) {}
                    nomePasso = ""
                },
                habilitado = nomePasso.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            // Roteiro: as coisinhas ligadas a esta viagem, na ordem dos dias.
            Text("Roteiro", style = EstiloTituloSecao, color = Tinta)
            if (paradas.isEmpty()) {
                Text("O roteiro ainda está vazio.", style = EstiloRotulo, color = TintaSuave)
            }
            porDia.forEach { (dia, lista) ->
                Text(dia, style = EstiloRotulo, color = Ocre)
                lista.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.titulo,
                                style = EstiloApoio,
                                color = if (p.feito) TintaSuave else Tinta
                            )
                            Text(
                                listOfNotNull(CATEGORIAS_PARADA[p.categoria], p.notas.takeIf { it.isNotBlank() })
                                    .joinToString(" · "),
                                style = EstiloRotulo,
                                color = TintaSuave
                            )
                        }
                        Text(
                            if (p.feito) "desmarcar" else "feita",
                            style = EstiloRotulo,
                            color = Ocre,
                            modifier = Modifier.clickable {
                                dados.atualizarItem("coisinhas", p.chave, mapOf("done" to !p.feito), p.titulo) {}
                            }
                        )
                        Text(
                            "tirar",
                            style = EstiloRotulo,
                            color = Ocre,
                            modifier = Modifier.clickable {
                                dados.atualizarItem("coisinhas", p.chave, mapOf("viagemKey" to null), p.titulo) {}
                            }
                        )
                    }
                }
            }

            if (livres.isNotEmpty()) {
                Text("Ou pegue do cardápio", style = EstiloRotulo, color = TintaSuave)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    livres.forEach { c ->
                        FiltroSite(
                            texto = c.titulo,
                            ativo = false,
                            aoClicar = { dados.atualizarItem("coisinhas", c.chave, mapOf("viagemKey" to chave), c.titulo) {} }
                        )
                    }
                }
            }

            Text("Adicionar ao roteiro", style = EstiloRotulo, color = TintaSuave)
            OutlinedTextField(
                value = nomeParada,
                onValueChange = { nomeParada = it },
                label = { Text("O que visitar ou fazer?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIAS_PARADA.forEach { (c, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = categoriaParada == c, aoClicar = { categoriaParada = c })
                }
            }
            OutlinedTextField(
                value = diaParada,
                onValueChange = { diaParada = it },
                label = { Text("Dia (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                isError = !dataValidaOuVazia(diaParada),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notaParada,
                onValueChange = { notaParada = it },
                label = { Text("Horário, endereço ou detalhe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Adicionar ao roteiro",
                aoClicar = {
                    dados.criarItem(
                        "coisinhas",
                        mapOf(
                            "title" to nomeParada.trim(),
                            "category" to categoriaParada,
                            "targetDate" to diaParada.trim(),
                            "notes" to notaParada.trim(),
                            "poster" to CAPA_SEM_FOTO,
                            "done" to false,
                            "viagemKey" to chave
                        ),
                        nomeParada.trim()
                    ) {}
                    nomeParada = ""
                    diaParada = ""
                    notaParada = ""
                },
                habilitado = paradaValida,
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )

            BotaoSite(
                texto = if (situacao == "feita") "Marcar como pendente" else "Marcar como feita",
                aoClicar = {
                    dados.atualizarItem(
                        "viagens", chave,
                        mapOf("status" to if (situacao == "feita") "pendente" else "feita"),
                        v.titulo
                    ) {}
                },
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(texto = "Editar viagem", aoClicar = aoEditar, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { confirmarApagar = true }) {
                Text("Excluir viagem", style = EstiloBotao, color = Ocre)
            }
        }
    }

    if (confirmarApagar) {
        DialogoConfirmar(
            titulo = "Excluir ${v.titulo}?",
            texto = "A viagem sai da lista. As coisinhas do roteiro continuam no cardápio.",
            rotuloConfirmar = "Excluir",
            aoConfirmar = {
                dados.apagarItem("viagens", chave, v.titulo) {}
                aoFechar()
            },
            aoFechar = { confirmarApagar = false }
        )
    }
}

// Cadastro e edição de viagem. A lista de preparativos é salva junto com a viagem.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioViagem(dados: DadosDoCasal, editando: Item?, aoFechar: () -> Unit) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var destino by remember { mutableStateOf(editando?.titulo ?: "") }
    var saida by remember { mutableStateOf(editando?.dataIso ?: "") }
    var chegada by remember { mutableStateOf(editando?.fim ?: "") }
    var status by remember { mutableStateOf(if (editando?.status == "feita") "feita" else "pendente") }
    var imagem by remember { mutableStateOf(editando?.imagem ?: "") }
    var notas by remember { mutableStateOf(editando?.notas ?: "") }
    val preparativos = remember {
        mutableStateListOf<Passo>().apply { addAll(editando?.checklist ?: emptyList()) }
    }
    var nomePasso by remember { mutableStateOf("") }
    val periodoOk = chegada.isBlank() || saida.isBlank() || chegada >= saida
    val valida = destino.isNotBlank() && dataValidaOuVazia(saida) && dataValidaOuVazia(chegada) && periodoOk

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
            Text(
                if (editando == null) "Nova viagem" else "Editar viagem",
                style = EstiloTituloSecao,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = destino,
                onValueChange = { destino = it },
                label = { Text("Para onde vamos?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = saida,
                onValueChange = { saida = it },
                label = { Text("Quando vamos? (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                isError = !dataValidaOuVazia(saida),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = chegada,
                onValueChange = { chegada = it },
                label = { Text("Até quando? (opcional)") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                isError = !dataValidaOuVazia(chegada) || !periodoOk,
                supportingText = {
                    if (!periodoOk) Text("A data final não pode ser antes da inicial")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroSite(texto = "Ainda não foi", ativo = status == "pendente", aoClicar = { status = "pendente" })
                FiltroSite(texto = "Já foi", ativo = status == "feita", aoClicar = { status = "feita" })
            }
            OutlinedTextField(
                value = imagem,
                onValueChange = { imagem = it },
                label = { Text("Link de uma foto (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text("Ideias para a viagem") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Preparativos", style = EstiloApoio, color = Tinta)
            preparativos.forEachIndexed { indice, passo ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(passo.nome, style = EstiloApoio, color = Tinta, modifier = Modifier.weight(1f))
                    Text(
                        "tirar",
                        style = EstiloRotulo,
                        color = Ocre,
                        modifier = Modifier.clickable { preparativos.removeAt(indice) }
                    )
                }
            }
            OutlinedTextField(
                value = nomePasso,
                onValueChange = { nomePasso = it },
                label = { Text("Ex.: passaporte") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Adicionar preparativo",
                aoClicar = {
                    preparativos.add(Passo(nomePasso.trim(), false))
                    nomePasso = ""
                },
                habilitado = nomePasso.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            BotaoSite(
                texto = "Salvar",
                aoClicar = {
                    val nome = destino.trim()
                    val lista = preparativos.map { mapOf<String, Any?>("name" to it.nome, "done" to it.feito) }
                    val campos = mutableMapOf<String, Any?>(
                        "destiny" to nome,
                        "dest" to nome,
                        "targetDate" to saida.trim(),
                        "date" to saida.trim(),
                        "fim" to chegada.trim(),
                        "status" to status,
                        "image" to imagem.trim(),
                        "notes" to notas.trim()
                    )
                    if (editando == null) {
                        if (lista.isNotEmpty()) campos["checklist"] = lista
                        dados.criarItem("viagens", campos, nome) {}
                    } else {
                        campos["checklist"] = lista.ifEmpty { null }
                        dados.atualizarItem("viagens", editando.chave, campos, nome) {}
                    }
                    aoFechar()
                },
                habilitado = valida,
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
