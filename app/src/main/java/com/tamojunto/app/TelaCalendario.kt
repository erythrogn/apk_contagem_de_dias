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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

// Imagem padrão de planos que viram coisinha, como no site.
internal const val CAPA_SEM_FOTO = "https://placehold.co/600x900/d4e6f6/3f6fa6?text=Sem+Capa"

private val MESES = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
)
private val SEMANA = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")
private val DIAS_CURTOS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

private val ROTULO_TIPO = linkedMapOf(
    "anniv" to "Mesversário",
    "viagem" to "Viagem",
    "filme" to "Filme",
    "coisinha" to "Coisinha",
    "plano" to "Plano"
)
private val ROTULO_PLANO = mapOf(
    "encontro" to "Encontro",
    "data" to "Data especial",
    "outro" to "Outro",
    "filme" to "Filme",
    "coisinha" to "Coisinha"
)

private fun corDoTipo(tipo: String): Color = when (tipo) {
    "anniv" -> Ouro
    "viagem" -> Tinta
    "filme" -> Ocre
    "coisinha" -> Color(0xFFB08A2E)
    else -> TintaSuave
}

// Um compromisso no calendário. Planos guardam a chave e o tipo (kind) para editar.
private data class Evento(
    val data: String,
    val tipo: String,
    val titulo: String,
    val sub: String = "",
    val nota: String = "",
    val chave: String? = null,
    val kind: String = ""
)

private fun eventosCadastrados(dados: DadosDoCasal): List<Evento> {
    val lista = mutableListOf<Evento>()
    dados.viagens.filter { it.dataIso.isNotBlank() && it.status != "feita" }.forEach {
        lista += Evento(it.dataIso, "viagem", it.titulo, it.status)
    }
    dados.filmes.filter { it.dataIso.isNotBlank() && !it.feito }.forEach {
        lista += Evento(it.dataIso, "filme", it.titulo, if (it.quem.isNotBlank()) "indicação: ${it.quem}" else "")
    }
    dados.coisinhas.filter { it.dataIso.isNotBlank() && !it.feito }.forEach {
        lista += Evento(it.dataIso, "coisinha", it.titulo)
    }
    dados.planos.filter { it.dataIso.isNotBlank() }.forEach {
        lista += Evento(
            data = it.dataIso,
            tipo = "plano",
            titulo = it.titulo,
            sub = ROTULO_PLANO[it.tipo] ?: "",
            nota = it.notas,
            chave = it.chave,
            kind = it.tipo.ifBlank { "outro" }
        )
    }
    return lista
}

// Mesversário: cai no mesmo dia do mês em que o casal começou (mês 1 a 12).
private fun mesversario(inicio: LocalDate?, ano: Int, mes: Int): List<Evento> {
    if (inicio == null) return emptyList()
    val dia = inicio.dayOfMonth
    if (YearMonth.of(ano, mes).lengthOfMonth() < dia) return emptyList()
    val data = LocalDate.of(ano, mes, dia)
    if (data.isBefore(inicio)) return emptyList()
    val n = ChronoUnit.MONTHS.between(inicio.withDayOfMonth(1), data.withDayOfMonth(1)).toInt()
    val titulo = if (n == 0) "Início da nossa história" else "${n}º mesversário"
    return listOf(Evento(data.toString(), "anniv", titulo, "todo dia $dia"))
}

private fun faltam(hoje: LocalDate, iso: String): String {
    val n = ChronoUnit.DAYS.between(hoje, LocalDate.parse(iso))
    return when (n) {
        0L -> "é hoje"
        1L -> "amanhã"
        else -> "faltam $n dias"
    }
}

private fun nomeDoMes(ano: Int, mes: Int): String = "${MESES[mes - 1].replaceFirstChar { it.uppercase() }} de $ano"

@Composable
private fun TagTipo(tipo: String) {
    val cor = corDoTipo(tipo)
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(cor.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(ROTULO_TIPO[tipo] ?: tipo, style = EstiloRotulo, color = Tinta)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCalendario(dados: DadosDoCasal) {
    val hoje = remember { LocalDate.now() }
    var ano by rememberSaveable { mutableStateOf(hoje.year) }
    var mes by rememberSaveable { mutableStateOf(hoje.monthValue) }
    var vista by rememberSaveable { mutableStateOf("mes") }
    var filtro by rememberSaveable { mutableStateOf("todos") }
    var diaEscolhido by rememberSaveable { mutableStateOf<String?>(null) }
    var inicioCasal by remember { mutableStateOf<LocalDate?>(null) }
    var formAberto by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Evento?>(null) }
    var dataNovo by remember { mutableStateOf(hoje.toString()) }

    LaunchedEffect(dados.casalId) {
        if (dados.casalId != null) {
            val (inicio, _) = InicioServidorCasal.carregar()
            inicioCasal = inicio?.dataIso?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }
    }

    // Datas gravadas antes da validação estrita (ex.: 2026-02-30) são ignoradas, para não derrubar a tela.
    val cadastrados = eventosCadastrados(dados).filter { dataIsoOuNull(it.data) != null }
    val prefixoMes = "%d-%02d-".format(ano, mes)
    val doMes = (cadastrados + mesversario(inicioCasal, ano, mes))
        .filter { it.data.startsWith(prefixoMes) && (filtro == "todos" || it.tipo == filtro) }
        .sortedBy { it.data }
    val proximo = (cadastrados + (0L..3L).flatMap { k ->
        val d = hoje.plusMonths(k)
        mesversario(inicioCasal, d.year, d.monthValue)
    }).filter { it.data >= hoje.toString() }.minByOrNull { it.data }

    fun abrirNovo(data: String) {
        editando = null
        dataNovo = data
        formAberto = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nossa Programação", style = EstiloTituloSecao, textAlign = TextAlign.Center)
                Text(
                    "voce muda todos os dias da minha vida",
                    style = EstiloApoio,
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            if (proximo == null) {
                Text(
                    "Nada marcado ainda. Que tal um plano novo?",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val d = LocalDate.parse(proximo.data)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
                        Text("${d.dayOfMonth}", style = EstiloNumero.copy(fontSize = 52.sp, lineHeight = 52.sp), color = Tinta)
                        Text(MESES[d.monthValue - 1].take(3), style = EstiloRotulo, color = TintaSuave)
                    }
                    Column(Modifier.weight(1f)) {
                        TagTipo(proximo.tipo)
                        Text(proximo.titulo, style = EstiloTituloSecao, color = Tinta)
                        Text(faltam(hoje, proximo.data), style = EstiloRotulo, color = TintaSuave)
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AbaSite(texto = "Mês", ativa = vista == "mes", aoClicar = { vista = "mes" })
                AbaSite(texto = "Lista", ativa = vista == "lista", aoClicar = { vista = "lista" })
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroSite(texto = "Todos", ativo = filtro == "todos", aoClicar = { filtro = "todos" })
                ROTULO_TIPO.forEach { (chave, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = filtro == chave, aoClicar = { filtro = chave })
                }
            }
        }

        if (vista == "mes") {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SetaMes(
                            direcao = -1,
                            aoClicar = {
                                val anterior = YearMonth.of(ano, mes).minusMonths(1)
                                ano = anterior.year
                                mes = anterior.monthValue
                            }
                        )
                        Text(
                            nomeDoMes(ano, mes),
                            style = EstiloApoio,
                            color = Tinta,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        SetaMes(
                            direcao = 1,
                            aoClicar = {
                                val proxima = YearMonth.of(ano, mes).plusMonths(1)
                                ano = proxima.year
                                mes = proxima.monthValue
                            }
                        )
                    }
                    Text(
                        "ir para hoje",
                        style = EstiloRotulo,
                        color = Ocre,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ano = hoje.year
                                mes = hoje.monthValue
                                diaEscolhido = hoje.toString()
                            }
                            .padding(vertical = 6.dp)
                    )
                    Row(Modifier.fillMaxWidth()) {
                        DIAS_CURTOS.forEach { nome ->
                            Text(
                                nome,
                                style = EstiloRotulo,
                                color = TintaSuave,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            item {
                Grade(
                    ano = ano,
                    mes = mes,
                    eventos = (cadastrados + mesversario(inicioCasal, ano, mes)).filter {
                        it.data.startsWith(prefixoMes) && (filtro == "todos" || it.tipo == filtro)
                    },
                    hoje = hoje,
                    selecionado = diaEscolhido,
                    aoEscolher = { diaEscolhido = it }
                )
            }
            item {
                PainelDoDia(
                    dia = diaEscolhido,
                    eventos = doMes.filter { it.data == diaEscolhido },
                    aoEditar = { editando = it; formAberto = true },
                    aoMarcar = { abrirNovo(diaEscolhido ?: hoje.toString()) }
                )
            }
        } else {
            if (doMes.isEmpty()) {
                item {
                    Text(
                        "Nada marcado neste mês.",
                        style = EstiloApoio,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            doMes.groupBy { it.data }.forEach { (iso, lista) ->
                item {
                    val d = LocalDate.parse(iso)
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "${d.dayOfMonth}  ${SEMANA[(d.dayOfWeek.value + 6) % 7]}",
                            style = EstiloTituloSecao,
                            color = Tinta
                        )
                        lista.forEach { evento ->
                            LinhaEvento(evento, aoEditar = { editando = it; formAberto = true })
                        }
                    }
                }
            }
        }

        item {
            BotaoSite(
                texto = "Novo plano",
                aoClicar = { abrirNovo(diaEscolhido ?: hoje.toString()) },
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (formAberto) {
        FormularioPlano(
            dados = dados,
            editando = editando,
            dataInicial = if (editando == null) dataNovo else "",
            aoFechar = { formAberto = false; editando = null }
        )
    }
}

// Seta de troca de mês: chevron de traço, como nas outras telas.
@Composable
private fun SetaMes(direcao: Int, aoClicar: () -> Unit) {
    val caminho = if (direcao < 0) "M15 19l-7-7 7-7" else "M9 5l7 7-7 7"
    Box(
        Modifier
            .size(40.dp)
            .clickable(onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        IconeLinha(caminhos = listOf(caminho), tamanho = 18.dp, cor = Tinta, espessura = 1.5f)
    }
}

@Composable
private fun Grade(
    ano: Int,
    mes: Int,
    eventos: List<Evento>,
    hoje: LocalDate,
    selecionado: String?,
    aoEscolher: (String) -> Unit
) {
    val primeiro = LocalDate.of(ano, mes, 1)
    val deslocamento = primeiro.dayOfWeek.value % 7
    val total = primeiro.lengthOfMonth()
    val celulas: List<Int?> = List(deslocamento) { null } + (1..total).toList()
    val semanas = celulas.chunked(7).map { semana -> semana + List(7 - semana.size) { null } }
    val porDia = eventos.groupBy { it.data }

    Column(Modifier.fillMaxWidth()) {
        semanas.forEach { semana ->
            Row(Modifier.fillMaxWidth()) {
                semana.forEach { dia ->
                    if (dia == null) {
                        Box(Modifier.weight(1f).height(50.dp))
                    } else {
                        val iso = "%d-%02d-%02d".format(ano, mes, dia)
                        val evs = porDia[iso].orEmpty()
                        val ehHoje = iso == hoje.toString()
                        val ehSelecionado = iso == selecionado
                        Column(
                            Modifier
                                .weight(1f)
                                .height(50.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (ehSelecionado) Ouro.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (ehHoje) Tinta else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { aoEscolher(iso) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("$dia", style = EstiloChip, color = Tinta)
                            Row(horizontalArrangement = Arrangement.Center) {
                                evs.take(3).forEach { e ->
                                    Box(
                                        Modifier
                                            .padding(horizontal = 1.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(corDoTipo(e.tipo))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PainelDoDia(
    dia: String?,
    eventos: List<Evento>,
    aoEditar: (Evento) -> Unit,
    aoMarcar: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (dia == null) {
            Text(
                "Toque em um dia para ver o que está marcado.",
                style = EstiloRotulo,
                color = TintaSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            return@Column
        }
        val d = LocalDate.parse(dia)
        Text(
            "${d.dayOfMonth} de ${MESES[d.monthValue - 1]}, ${SEMANA[(d.dayOfWeek.value + 6) % 7]}",
            style = EstiloTituloSecao,
            color = Tinta
        )
        if (eventos.isEmpty()) {
            Text("Nada marcado para este dia.", style = EstiloRotulo, color = TintaSuave)
        } else {
            eventos.forEach { LinhaEvento(it, aoEditar) }
        }
        BotaoSite(texto = "Marcar algo neste dia", aoClicar = aoMarcar, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LinhaEvento(evento: Evento, aoEditar: ((Evento) -> Unit)?) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
            TagTipo(evento.tipo)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(evento.titulo, style = EstiloApoio, color = Tinta)
                if (evento.sub.isNotBlank()) Text(evento.sub, style = EstiloRotulo, color = TintaSuave)
                if (evento.nota.isNotBlank()) Text(evento.nota, style = EstiloRotulo, color = TintaSuave)
                if (aoEditar != null && evento.chave != null) {
                    Text(
                        "editar",
                        style = EstiloRotulo,
                        color = Ocre,
                        modifier = Modifier.padding(top = 4.dp).clickable { aoEditar(evento) }
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.1f)))
    }
}

// Plano próprio do casal (encontro, data especial ou outro). Filme e coisinha vão para a página certa.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioPlano(
    dados: DadosDoCasal,
    editando: Evento?,
    dataInicial: String,
    aoFechar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titulo by remember { mutableStateOf(editando?.titulo ?: "") }
    var data by remember { mutableStateOf(editando?.data ?: dataInicial) }
    var tipo by remember { mutableStateOf(editando?.kind?.ifBlank { "outro" } ?: "encontro") }
    var nota by remember { mutableStateOf(editando?.nota ?: "") }
    var confirmarApagar by remember { mutableStateOf(false) }
    val dataValida = dataIsoOuNull(data) != null

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
                if (editando == null) "Novo plano" else "Editar plano",
                style = EstiloTituloSecao,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("O que é") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = data,
                onValueChange = { data = it },
                label = { Text("Quando") },
                placeholder = { Text("AAAA-MM-DD") },
                singleLine = true,
                isError = !dataValida,
                supportingText = { if (!dataValida) Text("Use o formato AAAA-MM-DD") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ROTULO_PLANO.forEach { (chave, rotulo) ->
                    FiltroSite(texto = rotulo, ativo = tipo == chave, aoClicar = { tipo = chave })
                }
            }
            if (tipo == "filme" || tipo == "coisinha") {
                Text(
                    if (tipo == "filme") "Entra na lista de Filmes com esta data e aparece aqui também."
                    else "Entra no cardápio de Coisinhas com esta data e aparece aqui também.",
                    style = EstiloRotulo,
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
            }
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it },
                label = { Text("Observação (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            BotaoSite(
                texto = "Salvar",
                aoClicar = {
                    val nome = titulo.trim()
                    val dia = data.trim()
                    val antigo = editando?.chave
                    when (tipo) {
                        "filme" -> {
                            dados.criarItem(
                                "filmes",
                                mapOf(
                                    "title" to nome, "year" to "", "poster" to "", "backdrop" to "",
                                    "targetDate" to dia, "genre" to "", "who" to "", "where" to "",
                                    "watched" to false, "notes" to nota.trim()
                                ),
                                nome
                            ) {}
                            antigo?.let { dados.apagarItem("planos", it, nome) {} }
                        }
                        "coisinha" -> {
                            dados.criarItem(
                                "coisinhas",
                                mapOf(
                                    "title" to nome, "category" to "outro", "targetDate" to dia,
                                    "notes" to nota.trim(), "poster" to CAPA_SEM_FOTO, "done" to false
                                ),
                                nome
                            ) {}
                            antigo?.let { dados.apagarItem("planos", it, nome) {} }
                        }
                        else -> {
                            val campos = mapOf("title" to nome, "date" to dia, "kind" to tipo, "note" to nota.trim())
                            if (antigo == null) dados.criarItem("planos", campos, nome) {}
                            else dados.atualizarItem("planos", antigo, campos, nome) {}
                        }
                    }
                    aoFechar()
                },
                habilitado = titulo.isNotBlank() && dataValida,
                primario = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (editando?.chave != null) {
                TextButton(onClick = { confirmarApagar = true }) { Text("Apagar plano", style = EstiloBotao, color = Ocre) }
            }
        }
    }

    if (confirmarApagar) {
        DialogoConfirmar(
            titulo = "Apagar ${editando?.titulo.orEmpty()}?",
            texto = "Isso remove o plano do calendário.",
            rotuloConfirmar = "Apagar",
            aoConfirmar = {
                editando?.chave?.let { dados.apagarItem("planos", it, editando.titulo) {} }
                aoFechar()
            },
            aoFechar = { confirmarApagar = false }
        )
    }
}
