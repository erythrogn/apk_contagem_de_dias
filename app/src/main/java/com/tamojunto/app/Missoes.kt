package com.tamojunto.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// ─── Dados do Laço, das Missões e do Céu, como o site os entrega em /api/missoes e /api/ceu ───

data class LacoDetalhe(
    val nivel: Int,
    val total: Int,
    val fase: String,
    val significado: String,
    val xp: Int,
    val proximoNivel: Int?,
    val falta: Int,
    val percentual: Int,
    val prestigio: Boolean,
    val regras: List<RegraLaco>,
    val limitesHoje: List<LimiteHoje>
)

data class MissaoApp(
    val id: String,
    val titulo: String,
    val descricao: String,
    val tipoRotulo: String,
    val categoria: String,
    val xp: Int,
    val compartilhada: Boolean,
    val estadoRotulo: String,
    val expiraTexto: String,
    val quem: String,
    val acoes: List<Pair<String, String>>
)

data class LinhaMissao(val rotulo: String, val missao: MissaoApp?)
data class ConcluidaApp(val titulo: String, val xp: Int, val quem: String)
data class QuadroApp(val linhas: List<LinhaMissao>, val esperando: List<MissaoApp>, val concluidas: List<ConcluidaApp>)

data class EraCeu(val id: String, val rotulo: String, val sub: String)
data class ItemCeu(val id: String, val coluna: String, val titulo: String, val feito: Boolean, val era: String, val dataBr: String)
data class CeuApp(val eras: List<EraCeu>, val tipos: Map<String, String>, val itens: List<ItemCeu>, val vazio: Boolean, val xpHoje: Int)

private fun JSONObject.txt(campo: String): String = optString(campo, "")
private fun JSONArray.objs(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONObject.lacoDetalhe(): LacoDetalhe = LacoDetalhe(
    nivel = optInt("nivel"),
    total = optInt("total"),
    fase = txt("fase"),
    significado = txt("significado"),
    xp = optInt("xp"),
    proximoNivel = if (isNull("proximo_nivel")) null else optInt("proximo_nivel"),
    falta = optInt("falta"),
    percentual = optInt("percentual"),
    prestigio = optBoolean("prestigio"),
    regras = optJSONArray("regras")?.objs()?.map { r ->
        RegraLaco(
            categoria = r.txt("categoria"),
            limite = r.optInt("limite"),
            atividades = r.optJSONArray("atividades")?.objs()?.map { a -> AtividadeLaco(a.txt("rotulo"), a.optInt("xp")) }.orEmpty()
        )
    }.orEmpty(),
    limitesHoje = optJSONArray("limites_hoje")?.objs()?.map { l ->
        LimiteHoje(l.txt("categoria"), l.optInt("usado"), l.optInt("limite"))
    }.orEmpty()
)

private fun JSONObject.missaoApp(): MissaoApp = MissaoApp(
    id = txt("id"),
    titulo = txt("titulo"),
    descricao = txt("descricao"),
    tipoRotulo = txt("tipo_rotulo"),
    categoria = txt("categoria"),
    xp = optInt("xp"),
    compartilhada = optBoolean("compartilhada"),
    estadoRotulo = txt("estado_rotulo"),
    expiraTexto = txt("expira_texto"),
    quem = txt("quem"),
    acoes = optJSONArray("acoes")?.let { lista ->
        (0 until lista.length()).mapNotNull { i ->
            lista.optJSONArray(i)?.let { par -> par.optString(0) to par.optString(1) }
        }
    }.orEmpty()
)

private fun JSONObject.quadroApp(): QuadroApp = QuadroApp(
    linhas = optJSONArray("linhas")?.objs()?.map { l ->
        LinhaMissao(l.txt("rotulo"), if (l.isNull("missao")) null else l.getJSONObject("missao").missaoApp())
    }.orEmpty(),
    esperando = optJSONArray("esperando")?.objs()?.map { it.missaoApp() }.orEmpty(),
    concluidas = optJSONArray("concluidas")?.objs()?.map { c ->
        ConcluidaApp(c.txt("titulo"), c.optInt("xp"), c.txt("quem"))
    }.orEmpty()
)

private fun JSONObject.ceuApp(): CeuApp = CeuApp(
    eras = optJSONArray("eras")?.objs()?.map { e -> EraCeu(e.txt("id"), e.txt("rotulo"), e.txt("sub")) }.orEmpty(),
    tipos = optJSONArray("tipos")?.objs()?.associate { t -> t.txt("id") to t.txt("rotulo") }.orEmpty(),
    itens = optJSONArray("itens")?.objs()?.map { i ->
        ItemCeu(
            id = i.txt("item"),
            coluna = i.txt("coluna"),
            titulo = i.txt("titulo"),
            feito = i.optBoolean("feito"),
            era = i.txt("era"),
            dataBr = i.txt("data_br")
        )
    }.orEmpty(),
    vazio = optBoolean("vazio", true),
    xpHoje = optInt("xp_hoje")
)

// Conversa com /api/missoes e /api/ceu. Quem marca ou confirma uma missão é o servidor.
object MissoesServidor {
    suspend fun carregarMissoes(): Pair<LacoDetalhe, QuadroApp>? {
        val r = Servidor.obter("/api/missoes")
        val j = r.corpo ?: return null
        if (!r.ok) return null
        val laco = j.optJSONObject("laco") ?: return null
        val quadro = j.optJSONObject("quadro") ?: return null
        return laco.lacoDetalhe() to quadro.quadroApp()
    }

    suspend fun carregarCeu(): Pair<LacoDetalhe, CeuApp>? {
        val r = Servidor.obter("/api/ceu")
        val j = r.corpo ?: return null
        if (!r.ok) return null
        val laco = j.optJSONObject("laco") ?: return null
        val ceu = j.optJSONObject("ceu") ?: return null
        return laco.lacoDetalhe() to ceu.ceuApp()
    }

    // Retorna o aviso do servidor (vazio quando deu certo).
    suspend fun agir(id: String, acao: String): String {
        val r = Servidor.postJson("/api/missoes/acao", org.json.JSONObject().put("id", id).put("acao", acao))
        return if (r.ok) r.corpo?.optString("msg").orEmpty() else r.msg.ifBlank { "Não consegui concluir. Tente de novo." }
    }
}

// Cartão do Laço: nível, fase, barra de XP e as regras de como ganhar.
@Composable
internal fun CartaoLaco(laco: LacoDetalhe) {
    var mostrarRegras by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Laço", style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
        Text(
            "Nível ${laco.nivel} de ${laco.total}" + if (laco.prestigio) " · Prestígio" else "",
            style = EstiloApoio,
            color = Tinta,
            textAlign = TextAlign.Center
        )
        Text(laco.fase, style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center)
        Text(laco.significado, style = EstiloRotulo, color = TintaSuave, textAlign = TextAlign.Center)
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).background(Tinta.copy(alpha = 0.12f))) {
            Box(Modifier.fillMaxWidth(laco.percentual.coerceIn(0, 100) / 100f).height(4.dp).background(Ouro))
        }
        Text(
            "${formatarXp(laco.xp)} XP" + if (laco.proximoNivel != null) " · faltam ${formatarXp(laco.falta)} para o nível ${laco.proximoNivel}" else " · continua acumulando",
            style = EstiloRotulo,
            color = TintaSuave,
            textAlign = TextAlign.Center
        )
        Text(
            if (mostrarRegras) "ocultar como ganhar XP" else "como ganhar XP",
            style = EstiloRotulo,
            color = Ocre,
            modifier = Modifier.clickable { mostrarRegras = !mostrarRegras }.padding(top = 4.dp)
        )
        if (mostrarRegras) {
            laco.regras.forEach { grupo ->
                Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("${grupo.categoria} · até ${grupo.limite} XP por dia", style = EstiloRotulo, color = Tinta)
                    grupo.atividades.forEach { atividade ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(atividade.rotulo, style = EstiloRotulo, color = TintaSuave, modifier = Modifier.weight(1f))
                            Text("${atividade.xp} XP", style = EstiloRotulo, color = TintaSuave)
                        }
                    }
                }
            }
            if (laco.limitesHoje.isNotEmpty()) {
                Text(
                    "Hoje: " + laco.limitesHoje.joinToString(" · ") { "${it.categoria} ${it.usado}/${it.limite}" },
                    style = EstiloRotulo,
                    color = TintaSuave,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun formatarXp(valor: Int): String = "%,d".format(valor).replace(',', '.')

@Composable
fun TelaMissoes() {
    var laco by remember { mutableStateOf<LacoDetalhe?>(null) }
    var quadro by remember { mutableStateOf<QuadroApp?>(null) }
    var carregou by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    val escopo = rememberCoroutineScope()

    suspend fun recarregar() {
        MissoesServidor.carregarMissoes()?.let { (l, q) ->
            laco = l
            quadro = q
        }
        carregou = true
    }

    LaunchedEffect(Unit) { recarregar() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = LocalAlturaDock.current),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Missões", style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
                Text(
                    "Coisas para fazer juntos. Cada missão concluída soma XP ao Laço.",
                    style = EstiloRotulo,
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
            }
        }

        val l = laco
        val q = quadro
        if (l == null || q == null) {
            item {
                Text(
                    if (carregou) "Não consegui carregar as missões. Tente de novo." else "Carregando…",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        item { CartaoLaco(l) }

        if (aviso.isNotBlank()) {
            item {
                Text(aviso, style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        if (q.esperando.isNotEmpty()) {
            item { Text("Esperando confirmação", style = EstiloTituloSecao, color = Tinta) }
            items(q.esperando, key = { it.id }) { m ->
                CartaoMissao(rotulo = m.tipoRotulo, missao = m, aoAgir = { acao ->
                    escopo.launch {
                        aviso = MissoesServidor.agir(m.id, acao)
                        recarregar()
                    }
                })
            }
        }

        items(q.linhas, key = { it.rotulo }) { linha ->
            CartaoMissao(
                rotulo = linha.rotulo,
                missao = linha.missao,
                aoAgir = { acao ->
                    val id = linha.missao?.id ?: return@CartaoMissao
                    escopo.launch {
                        aviso = MissoesServidor.agir(id, acao)
                        recarregar()
                    }
                }
            )
        }

        item { Text("Concluídas", style = EstiloTituloSecao, color = Tinta) }
        if (q.concluidas.isEmpty()) {
            item { Text("Nenhuma missão concluída ainda.", style = EstiloRotulo, color = TintaSuave) }
        } else {
            items(q.concluidas) { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.titulo, style = EstiloApoio, color = Tinta)
                        Text(c.quem, style = EstiloRotulo, color = TintaSuave)
                    }
                    Text("+${c.xp} XP", style = EstiloRotulo, color = Ocre)
                }
            }
        }
    }
}

// Cartão de uma missão (ou da vaga, quando não há missão daquele tipo agora).
@Composable
private fun CartaoMissao(rotulo: String, missao: MissaoApp?, aoAgir: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Tinta.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(rotulo, style = EstiloRotulo, color = TintaSuave)
        if (missao == null) {
            Text("Nenhuma missão agora", style = EstiloApoio, color = Tinta)
            Text("A próxima aparece quando esta terminar ou expirar.", style = EstiloRotulo, color = TintaSuave)
            return@Column
        }
        Text(missao.titulo, style = EstiloTituloSecao, color = Tinta)
        Text(missao.descricao, style = EstiloRotulo, color = TintaSuave)
        Text(
            listOfNotNull(
                "+${missao.xp} XP",
                missao.categoria.takeIf { it.isNotBlank() },
                if (missao.compartilhada) "juntos" else "individual",
                missao.expiraTexto.takeIf { it.isNotBlank() }
            ).joinToString(" · "),
            style = EstiloRotulo,
            color = Ocre
        )
        Text(missao.estadoRotulo, style = EstiloRotulo, color = Tinta)
        if (missao.quem.isNotBlank()) Text(missao.quem, style = EstiloRotulo, color = TintaSuave)
        if (missao.acoes.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                missao.acoes.forEach { (acao, rotuloAcao) ->
                    BotaoSite(
                        texto = rotuloAcao,
                        aoClicar = { aoAgir(acao) },
                        primario = acao in listOf("aceitar", "confirmar", "concluir"),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Céu de vocês: cada história vivida ou planejada vira uma estrela, agrupada por época.
// Aqui ela aparece como lista por época; o desenho do céu fica para a versão web.
@Composable
fun TelaCeu() {
    var laco by remember { mutableStateOf<LacoDetalhe?>(null) }
    var ceu by remember { mutableStateOf<CeuApp?>(null) }
    var carregou by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CeuServidorLeitura.carregar()?.let { (l, c) ->
            laco = l
            ceu = c
        }
        carregou = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = LocalAlturaDock.current),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Céu de vocês", style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
                Text(
                    "Cada estrela é algo que vocês viveram juntos.",
                    style = EstiloApoio,
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
            }
        }

        val l = laco
        val c = ceu
        if (l == null || c == null) {
            item {
                Text(
                    if (carregou) "Não consegui carregar o céu. Tente de novo." else "Carregando…",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        item { CartaoLaco(l) }

        if (c.xpHoje > 0) {
            item {
                Text("+${c.xpHoje} XP hoje", style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        if (c.vazio) {
            item {
                Text(
                    "O céu começa com a primeira história. Um filme, uma viagem, um compromisso, uma coisinha ou uma lembrança do Bloco já viram estrela.",
                    style = EstiloApoio,
                    color = Tinta,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        c.eras.forEach { era ->
            val doEra = c.itens.filter { it.era == era.id }
            if (doEra.isEmpty()) return@forEach
            item {
                Text("${era.rotulo} · ${era.sub}", style = EstiloTituloSecao, color = Tinta)
            }
            items(doEra, key = { it.id }) { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (item.feito) Ouro else Color.Transparent)
                            .border(1.dp, Tinta.copy(alpha = if (item.feito) 0.0f else 0.5f), CircleShape)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.titulo, style = EstiloApoio, color = Tinta)
                        Text(
                            listOf(c.tipos[item.coluna] ?: item.coluna, item.dataBr).filter { it.isNotBlank() }.joinToString(" · "),
                            style = EstiloRotulo,
                            color = TintaSuave
                        )
                    }
                }
            }
        }
    }
}

// Leitura do Céu separada da tela, para o mesmo carregamento poder ser reaproveitado.
object CeuServidorLeitura {
    suspend fun carregar(): Pair<LacoDetalhe, CeuApp>? = MissoesServidor.carregarCeu()
}
