package com.tamojunto.app

import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.json.JSONObject

// Pessoa pública de Amigos: nome, @ e foto, como na página /amigos do site.
data class PessoaAmiga(val uid: String, val nome: String, val handle: String, val foto: String)

data class RelacoesAmigos(
    val amigos: List<PessoaAmiga>,
    val pedidosRecebidos: List<PessoaAmiga>,
    val pedidosEnviados: List<PessoaAmiga>,
    val bloqueados: List<PessoaAmiga>
)

data class PerfilApp(
    val nome: String,
    val handle: String,
    val foto: String,
    val qtdAmigos: Int,
    val pedidosAmigos: Int,
    val qtdGrupos: Int,
    val convitesGrupos: Int
)

private fun JSONObject.texto(campo: String): String = optString(campo, "")

private fun lerPessoas(lista: org.json.JSONArray?): List<PessoaAmiga> {
    if (lista == null) return emptyList()
    return (0 until lista.length()).mapNotNull { i ->
        lista.optJSONObject(i)?.let { p ->
            PessoaAmiga(p.texto("uid"), p.texto("nome").ifBlank { "Sem nome" }, p.texto("handle"), p.texto("foto"))
        }
    }
}

// Conversa com /api/perfil e /api/amigos. As ações de escrita já existem no site.
object AmizadesServidor {
    suspend fun carregarPerfil(): PerfilApp? {
        val r = Servidor.obter("/api/perfil")
        val j = r.corpo ?: return null
        if (!r.ok) return null
        val eu = j.optJSONObject("eu") ?: JSONObject()
        return PerfilApp(
            nome = eu.texto("nome").ifBlank { "Sem nome" },
            handle = eu.texto("handle"),
            foto = eu.texto("foto"),
            qtdAmigos = j.optInt("qtd_amigos", 0),
            pedidosAmigos = j.optInt("pedidos_amigos", 0),
            qtdGrupos = j.optInt("qtd_grupos", 0),
            convitesGrupos = j.optInt("convites_grupos", 0)
        )
    }

    // Devolve o @ próprio e as relações, ou null quando o servidor não responde.
    suspend fun carregarAmigos(): Pair<String, RelacoesAmigos>? {
        val r = Servidor.obter("/api/amigos")
        val j = r.corpo ?: return null
        if (!r.ok) return null
        val relacoes = j.optJSONObject("relacoes") ?: JSONObject()
        return j.texto("handle") to RelacoesAmigos(
            amigos = lerPessoas(relacoes.optJSONArray("amigos")),
            pedidosRecebidos = lerPessoas(relacoes.optJSONArray("pedidos_recebidos")),
            pedidosEnviados = lerPessoas(relacoes.optJSONArray("pedidos_enviados")),
            bloqueados = lerPessoas(relacoes.optJSONArray("bloqueados"))
        )
    }

    // Retorna a mensagem do servidor (sucesso ou motivo da recusa).
    suspend fun definirHandle(handle: String): String {
        val r = Servidor.postJson("/api/amigos/handle", JSONObject().put("handle", handle))
        return if (r.ok) "Seu @ foi salvo." else r.msg.ifBlank { "Não consegui salvar o @." }
    }

    suspend fun pedirAmizade(handle: String): String {
        val r = Servidor.postJson("/api/amigos/pedido", JSONObject().put("handle", handle))
        return r.corpo?.optString("msg")?.takeIf { it.isNotBlank() }
            ?: if (r.ok) "Pedido enviado." else r.msg.ifBlank { "Não consegui enviar o pedido." }
    }

    suspend fun acao(acao: String, uid: String): String {
        val r = Servidor.postJson("/api/amigos/acao", JSONObject().put("acao", acao).put("uid", uid))
        return if (r.ok) "" else r.msg.ifBlank { "Não consegui concluir. Tente de novo." }
    }
}

// Avatar redondo: a foto do perfil, ou a inicial do nome quando não há foto.
@Composable
internal fun AvatarPessoa(nome: String, foto: String, tamanho: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(Tinta.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (foto.isNotBlank()) {
            AsyncImage(
                model = foto,
                contentDescription = nome,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(nome.firstOrNull()?.uppercase() ?: "", style = EstiloTituloSecao, color = Tinta)
        }
    }
}

// Perfil: quem você é no tamo junto e o que está esperando resposta. Leva a Amigos.
@Composable
fun TelaPerfil(aoAbrir: (String) -> Unit) {
    var perfil by remember { mutableStateOf<PerfilApp?>(null) }
    var carregou by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        perfil = AmizadesServidor.carregarPerfil()
        carregou = true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = LocalAlturaDock.current),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val p = perfil
        if (p == null) {
            item {
                Text(
                    if (carregou) "Não consegui carregar o perfil. Tente de novo." else "Carregando…",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarPessoa(nome = p.nome, foto = p.foto, tamanho = 84.dp)
                Text(p.nome, style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
                Text(
                    if (p.handle.isNotBlank()) "@${p.handle}" else "Defina um @ para ser encontrado por amigos",
                    style = EstiloRotulo,
                    color = TintaSuave,
                    textAlign = TextAlign.Center
                )
                BotaoSite(
                    texto = "Editar perfil",
                    aoClicar = { aoAbrir("config-perfil") },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        if (p.pedidosAmigos > 0 || p.convitesGrupos > 0) {
            item {
                Text("Esperando você", style = EstiloTituloSecao, color = Tinta)
            }
            if (p.pedidosAmigos > 0) item {
                LinhaHub(
                    titulo = "Pedidos de amizade",
                    apoio = "Aceite ou recuse quem quer ser seu amigo.",
                    info = "${p.pedidosAmigos} ${if (p.pedidosAmigos == 1) "novo" else "novos"}",
                    aoClicar = { aoAbrir("amigos") }
                )
            }
            if (p.convitesGrupos > 0) item {
                LinhaHub(
                    titulo = "Convites para duos e grupos",
                    apoio = "Alguém chamou você para um duo ou um grupo.",
                    info = "${p.convitesGrupos} ${if (p.convitesGrupos == 1) "novo" else "novos"}",
                    aoClicar = { aoAbrir("grupos") }
                )
            }
        }

        item {
            Text("Conexões", style = EstiloTituloSecao, color = Tinta)
        }
        item {
            LinhaHub(
                titulo = "Amigos",
                apoio = "Procure pelo @ e troque pedidos de amizade.",
                info = "${p.qtdAmigos} ${if (p.qtdAmigos == 1) "amigo" else "amigos"}",
                aoClicar = { aoAbrir("amigos") }
            )
        }
        item {
            LinhaHub(
                titulo = "Duos e grupos",
                apoio = "Um duo só entre vocês dois, ou um grupo de até 10 pessoas.",
                info = "${p.qtdGrupos} ${if (p.qtdGrupos == 1) "duo ou grupo" else "duos e grupos"}",
                aoClicar = { aoAbrir("grupos") }
            )
        }
    }
}

// Linha de menu com título, apoio e um valor à direita: usada em Perfil.
@Composable
internal fun LinhaHub(titulo: String, apoio: String, info: String, aoClicar: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = aoClicar)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = EstiloApoio, color = Tinta)
                Text(apoio, style = EstiloRotulo, color = TintaSuave)
            }
            Text(info, style = EstiloRotulo, color = Ocre, modifier = Modifier.padding(start = 8.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.12f)))
    }
}

// Amigos: definir o @, pedir amizade por @ e cuidar das relações (aceitar, recusar, remover, bloquear).
@Composable
fun TelaAmigos() {
    var handle by remember { mutableStateOf("") }
    var relacoes by remember { mutableStateOf<RelacoesAmigos?>(null) }
    var carregou by remember { mutableStateOf(false) }
    var campoHandle by remember { mutableStateOf("") }
    var campoPedido by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    val escopo = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun recarregar() {
        val lido = AmizadesServidor.carregarAmigos()
        if (lido != null) {
            handle = lido.first
            relacoes = lido.second
        }
        carregou = true
    }

    LaunchedEffect(Unit) { recarregar() }

    fun agir(acao: String, uid: String) {
        escopo.launch {
            val erro = AmizadesServidor.acao(acao, uid)
            aviso = erro
            recarregar()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp).papelDoSite().padding(20.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = LocalAlturaDock.current),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Amigos", style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        val r = relacoes
        if (r == null) {
            item {
                Text(
                    if (carregou) "Não consegui carregar seus amigos. Tente de novo." else "Carregando…",
                    style = EstiloApoio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return@LazyColumn
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Seu @", style = EstiloRotulo, color = TintaSuave)
                if (handle.isNotBlank()) {
                    Text("@$handle", style = EstiloApoio, color = Tinta)
                } else {
                    Text("Escolha um @ para ser encontrado por amigos.", style = EstiloRotulo, color = TintaSuave)
                }
                OutlinedTextField(
                    value = campoHandle,
                    onValueChange = { campoHandle = it },
                    label = { Text(if (handle.isBlank()) "Seu @" else "Trocar o @") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSite(
                    texto = "Salvar @",
                    aoClicar = {
                        escopo.launch {
                            aviso = AmizadesServidor.definirHandle(campoHandle.trim())
                            campoHandle = ""
                            recarregar()
                        }
                    },
                    habilitado = campoHandle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = campoPedido,
                    onValueChange = { campoPedido = it },
                    label = { Text("@ de quem você procura") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSite(
                    texto = "Enviar pedido",
                    aoClicar = {
                        escopo.launch {
                            aviso = AmizadesServidor.pedirAmizade(campoPedido.trim())
                            campoPedido = ""
                            recarregar()
                        }
                    },
                    habilitado = campoPedido.isNotBlank(),
                    primario = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        if (aviso.isNotBlank()) {
            item {
                Text(aviso, style = EstiloRotulo, color = Ocre, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        secaoPessoas("Pedidos recebidos", r.pedidosRecebidos, listOf("aceitar" to "Aceitar", "recusar" to "Recusar"), ::agir)
        secaoPessoas("Pedidos enviados", r.pedidosEnviados, listOf("cancelar" to "Cancelar pedido"), ::agir)
        secaoPessoas("Amigos", r.amigos, listOf("remover" to "Remover", "bloquear" to "Bloquear"), ::agir)
        secaoPessoas("Bloqueados", r.bloqueados, listOf("desbloquear" to "Desbloquear"), ::agir)
    }
}

// Uma seção da lista de amigos: título e, em cada pessoa, as ações disponíveis.
private fun androidx.compose.foundation.lazy.LazyListScope.secaoPessoas(
    titulo: String,
    pessoas: List<PessoaAmiga>,
    acoes: List<Pair<String, String>>,
    agir: (String, String) -> Unit
) {
    if (pessoas.isEmpty()) return
    item {
        Text(titulo, style = EstiloTituloSecao, color = Tinta)
    }
    items(pessoas, key = { it.uid }) { pessoa ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvatarPessoa(nome = pessoa.nome, foto = pessoa.foto, tamanho = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(pessoa.nome, style = EstiloApoio, color = Tinta)
                if (pessoa.handle.isNotBlank()) {
                    Text("@${pessoa.handle}", style = EstiloRotulo, color = TintaSuave)
                }
            }
            acoes.forEach { (acao, rotulo) ->
                Text(
                    rotulo,
                    style = EstiloRotulo,
                    color = Ocre,
                    modifier = Modifier.clickable { agir(acao, pessoa.uid) }.padding(start = 6.dp)
                )
            }
        }
    }
}
