package com.tamojunto.app

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// ─── Dados de duos e grupos, como o site os entrega em /api/grupos/... ───

data class GrupoApp(val gid: String, val nome: String, val tipo: String, val papel: String, val total: Int, val maximo: Int)
data class ConviteGrupo(val gid: String, val nomeGrupo: String, val de: String)
data class MembroGrupo(val uid: String, val nome: String, val handle: String, val foto: String, val papel: String, val eu: Boolean)
data class DetalheGrupo(
    val gid: String,
    val nome: String,
    val tipo: String,
    val maximo: Int,
    val euDono: Boolean,
    val membros: List<MembroGrupo>,
    val temSenhaBloco: Boolean,
    val amigosParaConvidar: List<PessoaAmiga>
)

private fun rotuloTipo(tipo: String): String = if (tipo == "duo") "Duo" else "Grupo"

private fun JSONObject.txt(campo: String): String = optString(campo, "")
private fun JSONArray.objs(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

// Conversa com as rotas de duos e grupos. Cada ação devolve a mensagem do servidor, se houver.
object GruposServidor {
    suspend fun listar(): Pair<List<GrupoApp>, List<ConviteGrupo>>? {
        val r = Servidor.obter("/api/grupos/gerenciar")
        val j = r.corpo ?: return null
        if (!r.ok) return null
        val grupos = j.optJSONArray("grupos")?.objs()?.map {
            GrupoApp(it.txt("gid"), it.txt("nome"), it.txt("tipo"), it.txt("papel"), it.optInt("total"), it.optInt("maximo"))
        }.orEmpty()
        val convites = j.optJSONArray("convites")?.objs()?.map {
            ConviteGrupo(it.txt("gid"), it.txt("nome_grupo"), it.txt("de"))
        }.orEmpty()
        return grupos to convites
    }

    suspend fun detalhes(gid: String): DetalheGrupo? {
        val r = Servidor.obter("/api/grupos/$gid/detalhes")
        val g = r.corpo?.optJSONObject("grupo") ?: return null
        if (!r.ok) return null
        return DetalheGrupo(
            gid = g.txt("gid"),
            nome = g.txt("nome"),
            tipo = g.txt("tipo"),
            maximo = g.optInt("maximo"),
            euDono = g.optBoolean("eu_dono"),
            membros = g.optJSONArray("membros")?.objs()?.map {
                MembroGrupo(
                    uid = it.txt("uid"),
                    nome = it.txt("nome").ifBlank { "Sem nome" },
                    handle = it.txt("handle"),
                    foto = it.txt("foto"),
                    papel = it.txt("papel"),
                    eu = it.optBoolean("eu")
                )
            }.orEmpty(),
            temSenhaBloco = g.optBoolean("tem_senha_bloco"),
            amigosParaConvidar = g.optJSONArray("amigos_para_convidar")?.objs()?.map {
                PessoaAmiga(it.txt("uid"), it.txt("nome").ifBlank { "Sem nome" }, it.txt("handle"), it.txt("foto"))
            }.orEmpty()
        )
    }

    // Devolve (mensagem, gid). O gid só vem quando o grupo foi criado ou entrado.
    suspend fun criar(nome: String, tipo: String): Pair<String, String?> {
        val r = Servidor.postJson("/api/grupos/criar", JSONObject().put("nome", nome).put("tipo", tipo))
        val gid = r.corpo?.optString("gid")?.takeIf { r.ok && it.isNotBlank() }
        return (if (r.ok) "" else respostaDoServidor(r, "Não consegui criar o grupo.")) to gid
    }

    suspend fun entrar(codigo: String): Pair<String, String?> {
        val r = Servidor.postJson("/api/grupos/entrar", JSONObject().put("codigo", codigo))
        val gid = r.corpo?.optString("gid")?.takeIf { r.ok && it.isNotBlank() }
        return (if (r.ok) "" else respostaDoServidor(r, "Não consegui entrar no grupo.")) to gid
    }

    suspend fun convite(gid: String, acao: String): String {
        val r = Servidor.postJson("/api/grupos/convite", JSONObject().put("gid", gid).put("acao", acao))
        return if (r.ok) "" else respostaDoServidor(r, "Não consegui responder ao convite.")
    }

    suspend fun convidar(gid: String, handle: String): String {
        val r = Servidor.postJson("/api/grupos/convidar", JSONObject().put("gid", gid).put("handle", handle))
        return if (r.ok) r.corpo?.optString("msg").orEmpty() else respostaDoServidor(r, "Não consegui enviar o convite.")
    }

    suspend fun link(gid: String): String? {
        val r = Servidor.postJson("/api/grupos/link", JSONObject().put("gid", gid))
        return if (r.ok) r.corpo?.optString("url")?.takeIf { it.isNotBlank() } else null
    }

    // Renomear, remover um membro, sair ou excluir o grupo. Devolve a mensagem de erro, se houver.
    suspend fun gestao(gid: String, acao: String, extra: JSONObject = JSONObject()): String {
        val corpo = JSONObject(extra.toString()).put("gid", gid).put("acao", acao)
        val r = Servidor.postJson("/api/grupos/gestao", corpo)
        return if (r.ok) "" else respostaDoServidor(r, "Não consegui concluir. Tente de novo.")
    }

    private fun respostaDoServidor(r: RespostaServidor, padrao: String): String =
        r.corpo?.optString("msg")?.takeIf { it.isNotBlank() } ?: r.msg.ifBlank { padrao }
}

// Lista de duos e grupos: convites recebidos, entrar por código, criar e abrir cada um.
@Composable
fun TelaGrupos(aoAbrir: (String) -> Unit) {
    var grupos by remember { mutableStateOf<List<GrupoApp>?>(null) }
    var convites by remember { mutableStateOf<List<ConviteGrupo>>(emptyList()) }
    var carregou by remember { mutableStateOf(false) }
    var nomeNovo by rememberSaveable { mutableStateOf("") }
    var tipoNovo by rememberSaveable { mutableStateOf("duo") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    val escopo = rememberCoroutineScope()

    suspend fun recarregar() {
        GruposServidor.listar()?.let { (g, c) ->
            grupos = g
            convites = c
        }
        carregou = true
    }

    LaunchedEffect(Unit) { recarregar() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Duos e grupos", style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        item {
            Text(
                "Um duo só entre vocês dois, ou um grupo de até 10 pessoas.",
                style = EstiloRotulo,
                color = TintaSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (aviso.isNotBlank()) {
            item {
                Text(aviso, style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        if (convites.isNotEmpty()) {
            item { Text("Convites", style = EstiloTituloSecao, color = Tinta) }
            items(convites, key = { it.gid }) { c ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(c.nomeGrupo.ifBlank { "Duo ou grupo" }, style = EstiloApoio, color = Tinta)
                    Text(
                        if (c.de.isNotBlank()) "Convite de ${c.de}" else "Convite recebido",
                        style = EstiloRotulo,
                        color = TintaSuave
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotaoSite(
                            texto = "Aceitar",
                            aoClicar = {
                                escopo.launch {
                                    aviso = GruposServidor.convite(c.gid, "aceitar")
                                    recarregar()
                                }
                            },
                            primario = true,
                            modifier = Modifier.weight(1f)
                        )
                        BotaoSite(
                            texto = "Recusar",
                            aoClicar = {
                                escopo.launch {
                                    aviso = GruposServidor.convite(c.gid, "recusar")
                                    recarregar()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Text("Seus duos e grupos", style = EstiloTituloSecao, color = Tinta) }
        val lista = grupos
        when {
            lista == null -> item {
                Text(
                    if (carregou) "Não consegui carregar seus grupos. Tente de novo." else "Carregando…",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            lista.isEmpty() -> item {
                Text("Você ainda não está em nenhum duo ou grupo.", style = EstiloApoio, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            else -> items(lista, key = { it.gid }) { g ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { aoAbrir(g.gid) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(g.nome, style = EstiloApoio, color = Tinta)
                        Text(
                            "${rotuloTipo(g.tipo)} · ${g.total} de ${g.maximo}" + if (g.papel == "dono") " · você criou" else "",
                            style = EstiloRotulo,
                            color = TintaSuave
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.1f)))
            }
        }

        item { Text("Entrar com um link", style = EstiloTituloSecao, color = Tinta) }
        item {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it },
                    label = { Text("Cole o link ou o código do convite") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSite(
                    texto = "Entrar",
                    aoClicar = {
                        escopo.launch {
                            val (erro, gid) = GruposServidor.entrar(codigo.trim())
                            aviso = erro
                            if (gid != null) {
                                codigo = ""
                                aoAbrir(gid)
                            }
                            recarregar()
                        }
                    },
                    habilitado = codigo.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        item { Text("Criar um duo ou grupo", style = EstiloTituloSecao, color = Tinta) }
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nomeNovo,
                    onValueChange = { nomeNovo = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FiltroSite(texto = "Duo (2 pessoas)", ativo = tipoNovo == "duo", aoClicar = { tipoNovo = "duo" })
                    FiltroSite(texto = "Grupo (até 10)", ativo = tipoNovo == "grupo", aoClicar = { tipoNovo = "grupo" })
                }
                BotaoSite(
                    texto = "Criar",
                    aoClicar = {
                        escopo.launch {
                            val (erro, gid) = GruposServidor.criar(nomeNovo.trim(), tipoNovo)
                            aviso = erro
                            if (gid != null) {
                                nomeNovo = ""
                                aoAbrir(gid)
                            }
                            recarregar()
                        }
                    },
                    habilitado = nomeNovo.isNotBlank(),
                    primario = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Detalhe de um duo ou grupo: membros, convites, link e gestão (renomear, remover, sair, excluir).
@Composable
fun TelaGrupoDetalhe(gid: String, aoSair: () -> Unit) {
    var detalhe by remember { mutableStateOf<DetalheGrupo?>(null) }
    var carregou by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var novoNome by rememberSaveable { mutableStateOf("") }
    var confirmarSair by remember { mutableStateOf(false) }
    var confirmarExcluir by remember { mutableStateOf(false) }
    var removerUid by remember { mutableStateOf<MembroGrupo?>(null) }
    val escopo = rememberCoroutineScope()
    val contexto = LocalContext.current

    suspend fun recarregar() {
        detalhe = GruposServidor.detalhes(gid)
        carregou = true
    }

    LaunchedEffect(gid) { recarregar() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val d = detalhe
        if (d == null) {
            item {
                Text(
                    if (carregou) "Este grupo não está disponível." else "Carregando…",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.nome, style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
                Text(
                    "${rotuloTipo(d.tipo)} · ${d.membros.size} de ${d.maximo}",
                    style = EstiloRotulo,
                    color = TintaSuave
                )
                if (d.tipo != "duo" && !d.temSenhaBloco) {
                    Text(
                        "As Notas deste grupo ainda não têm senha.",
                        style = EstiloRotulo,
                        color = Ocre,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (aviso.isNotBlank()) {
            item {
                Text(aviso, style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        item { Text("Quem está", style = EstiloTituloSecao, color = Tinta) }
        items(d.membros, key = { it.uid }) { m ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarPessoa(nome = m.nome, foto = m.foto, tamanho = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        m.nome + if (m.eu) " (você)" else "",
                        style = EstiloApoio,
                        color = Tinta
                    )
                    Text(
                        if (m.papel == "dono") "Criou o grupo" else "Membro",
                        style = EstiloRotulo,
                        color = TintaSuave
                    )
                }
                if (d.euDono && !m.eu) {
                    Text(
                        "remover",
                        style = EstiloRotulo,
                        color = Ocre,
                        modifier = Modifier.clickable { removerUid = m }
                    )
                }
            }
        }

        if (d.amigosParaConvidar.isNotEmpty() && d.membros.size < d.maximo) {
            item { Text("Convidar um amigo", style = EstiloTituloSecao, color = Tinta) }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    d.amigosParaConvidar.forEach { a ->
                        FiltroSite(
                            texto = a.nome,
                            ativo = false,
                            aoClicar = {
                                escopo.launch {
                                    aviso = GruposServidor.convidar(gid, a.handle)
                                    recarregar()
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            BotaoSite(
                texto = "Gerar link de convite",
                aoClicar = {
                    escopo.launch {
                        val url = GruposServidor.link(gid)
                        if (url == null) {
                            aviso = "Não consegui gerar o link. Tente de novo."
                        } else {
                            val envio = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            contexto.startActivity(Intent.createChooser(envio, "Enviar convite"))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (d.euDono) {
            item { Text("Gerenciar", style = EstiloTituloSecao, color = Tinta) }
            item {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = novoNome,
                        onValueChange = { novoNome = it },
                        label = { Text("Novo nome") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BotaoSite(
                        texto = "Renomear",
                        aoClicar = {
                            escopo.launch {
                                aviso = GruposServidor.gestao(gid, "renomear", JSONObject().put("nome", novoNome.trim()))
                                novoNome = ""
                                recarregar()
                            }
                        },
                        habilitado = novoNome.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
            item {
                BotaoSite(
                    texto = "Excluir o grupo",
                    aoClicar = { confirmarExcluir = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                BotaoSite(
                    texto = "Sair do grupo",
                    aoClicar = { confirmarSair = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmarSair) {
        DialogoConfirmar(
            titulo = "Sair de ${detalhe?.nome.orEmpty()}?",
            texto = "Você deixa de ver as coisas deste duo ou grupo.",
            rotuloConfirmar = "Sair",
            aoConfirmar = {
                escopo.launch {
                    val erro = GruposServidor.gestao(gid, "sair")
                    if (erro.isBlank()) { aoSair() } else { aviso = erro }
                }
            },
            aoFechar = { confirmarSair = false }
        )
    }
    if (confirmarExcluir) {
        DialogoConfirmar(
            titulo = "Excluir ${detalhe?.nome.orEmpty()}?",
            texto = "Isso apaga o grupo para todos os membros.",
            rotuloConfirmar = "Excluir",
            aoConfirmar = {
                escopo.launch {
                    val erro = GruposServidor.gestao(gid, "excluir")
                    if (erro.isBlank()) { aoSair() } else { aviso = erro }
                }
            },
            aoFechar = { confirmarExcluir = false }
        )
    }
    removerUid?.let { alvo ->
        DialogoConfirmar(
            titulo = "Remover ${alvo.nome}?",
            texto = "A pessoa sai do grupo.",
            rotuloConfirmar = "Remover",
            aoConfirmar = {
                escopo.launch {
                    aviso = GruposServidor.gestao(gid, "remover", JSONObject().put("uid", alvo.uid))
                    recarregar()
                }
            },
            aoFechar = { removerUid = null }
        )
    }
}
