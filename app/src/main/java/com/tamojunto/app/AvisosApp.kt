package com.tamojunto.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

// Canal dos avisos: o servidor manda com este mesmo id (channel_id).
internal const val CANAL_AVISOS = "tamojunto_avisos"

// Ordem em que os tipos aparecem nas Configurações. Os rótulos vêm do servidor.
internal val ORDEM_AVISOS = listOf(
    "perguntas", "cartas", "filmes", "coisinhas", "viagens", "notas",
    "calendario", "missoes", "fotos", "humor", "grupos", "casal"
)

private val escopoAvisos = CoroutineScope(SupervisorJob() + Dispatchers.IO)

object AvisosApp {
    fun criarCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gerenciador = contexto.getSystemService(NotificationManager::class.java) ?: return
        if (gerenciador.getNotificationChannel(CANAL_AVISOS) != null) return
        val canal = NotificationChannel(CANAL_AVISOS, "Avisos do casal", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Quando alguém faz algo no seu casal, duo ou grupo"
            enableVibration(true)
        }
        gerenciador.createNotificationChannel(canal)
    }

    fun permissaoConcedida(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // Pega o token deste aparelho e registra no servidor. Feito quando a pessoa já entrou.
    suspend fun registrarEste() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            AvisosServidor.registrar(token)
        }
    }

    // Depois de gravar no banco, avisa o servidor para os outros membros. Não bloqueia a tela.
    fun depoisDeGravar(colecao: String, acao: String, titulo: String) {
        escopoAvisos.launch { AvisosServidor.atividade(colecao, acao, titulo) }
    }

    // Mostra o aviso com o visual do app: ícone de laço, cor de tinta e texto completo.
    fun mostrar(contexto: Context, titulo: String, corpo: String, rota: String) {
        if (!permissaoConcedida(contexto)) return
        criarCanal(contexto)
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("rota", rota)
        }
        val pendente = PendingIntent.getActivity(
            contexto,
            rota.hashCode(),
            abrir,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val aviso = NotificationCompat.Builder(contexto, CANAL_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(ContextCompat.getColor(contexto, R.color.aviso_cor))
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendente)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(contexto).notify(System.currentTimeMillis().toInt(), aviso)
    }
}

// Recebe os avisos. Com o app aberto, o sistema não mostra sozinho: então mostramos aqui.
// Com o app fechado, o próprio sistema mostra usando a configuração que o servidor mandou.
class AvisosService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        escopoAvisos.launch { AvisosServidor.registrar(token) }
    }

    override fun onMessageReceived(mensagem: RemoteMessage) {
        val titulo = mensagem.notification?.title ?: mensagem.data["titulo"].orEmpty()
        val corpo = mensagem.notification?.body ?: mensagem.data["corpo"].orEmpty()
        if (titulo.isBlank()) return
        AvisosApp.mostrar(this, titulo, corpo, mensagem.data["rota"] ?: "/")
    }
}

object AvisosServidor {
    suspend fun registrar(token: String) {
        Servidor.postJson("/api/avisos/token", JSONObject().put("token", token))
    }

    // Preferências por tipo e os rótulos, ou null se o servidor não respondeu.
    suspend fun carregarPreferencias(): Pair<Map<String, Boolean>, Map<String, String>>? {
        val r = Servidor.obter("/api/avisos/preferencias")
        val j = r.corpo ?: return null
        if (!r.ok) return null
        val tipos = j.optJSONObject("tipos") ?: return null
        val rotulos = j.optJSONObject("rotulos") ?: JSONObject()
        val ligados = tipos.keys().asSequence().associateWith { tipos.optBoolean(it, true) }
        val nomes = rotulos.keys().asSequence().associateWith { rotulos.optString(it) }
        return ligados to nomes
    }

    // Devolve vazio se salvou, ou a mensagem do servidor.
    suspend fun salvarPreferencias(tipos: Map<String, Boolean>): String {
        val json = JSONObject()
        tipos.forEach { (chave, ligado) -> json.put(chave, ligado) }
        val r = Servidor.postJson("/api/avisos/preferencias", JSONObject().put("tipos", json))
        return if (r.ok) "" else r.msg.ifBlank { "Não consegui salvar os avisos." }
    }

    // Quem grava no banco avisa o servidor. Cartinhas não mandam o texto: o aviso só diz que chegou algo.
    suspend fun atividade(colecao: String, acao: String, titulo: String) {
        val semTexto = colecao == "cartinhas"
        Servidor.postJson(
            "/api/avisos/atividade",
            JSONObject()
                .put("colecao", colecao)
                .put("acao", acao)
                .put("titulo", if (semTexto) "" else titulo)
        )
    }
}

// Liga o registro do aparelho quando a pessoa entra: pede a permissão (Android 13+) e envia o token.
@Composable
internal fun PrepararAvisos() {
    val contexto = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !AvisosApp.permissaoConcedida(contexto)) {
            pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        AvisosApp.registrarEste()
    }
}

// Controle dos avisos nas Configurações: um interruptor por tipo e o botão de permissão quando falta.
@Composable
internal fun SecaoAvisosConfig() {
    val escopo = rememberCoroutineScope()
    val contexto = LocalContext.current
    var tipos by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var rotulos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var carregou by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }
    var permitido by remember { mutableStateOf(AvisosApp.permissaoConcedida(contexto)) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permitido = AvisosApp.permissaoConcedida(contexto)
    }

    LaunchedEffect(Unit) {
        AvisosServidor.carregarPreferencias()?.let { (ligados, nomes) ->
            tipos = ligados
            rotulos = nomes
        }
        carregou = true
    }

    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text("Avisos no celular", style = EstiloTituloSecao, color = Tinta)
        Text(
            "Quando alguém faz algo no seu casal, duo ou grupo, você recebe um aviso.",
            style = EstiloApoio,
            color = TintaSuave,
            modifier = Modifier.padding(top = 4.dp)
        )
        Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!permitido) {
                BotaoSite(
                    texto = "Permitir avisos",
                    primario = true,
                    modifier = Modifier.fillMaxWidth(),
                    aoClicar = {
                        if (Build.VERSION.SDK_INT >= 33) pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
                Text(
                    "Os avisos estão desligados neste aparelho.",
                    style = EstiloRotulo,
                    color = Ocre,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            when {
                !carregou -> Text("Carregando…", style = EstiloApoio, color = TintaSuave)
                rotulos.isEmpty() -> Text("Não consegui carregar os avisos agora.", style = EstiloApoio, color = TintaSuave)
                else -> ORDEM_AVISOS.forEach { chave ->
                    val ligado = tipos[chave] ?: true
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            rotulos[chave] ?: chave,
                            style = EstiloApoio,
                            color = Tinta,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = ligado,
                            onCheckedChange = { novo ->
                                val antes = tipos
                                tipos = tipos + (chave to novo)
                                escopo.launch {
                                    val erro = AvisosServidor.salvarPreferencias(mapOf(chave to novo))
                                    if (erro.isNotBlank()) {
                                        tipos = antes
                                        aviso = erro
                                    } else {
                                        aviso = ""
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Papel,
                                checkedTrackColor = Tinta,
                                uncheckedThumbColor = TintaSuave,
                                uncheckedTrackColor = Creme
                            )
                        )
                    }
                }
            }
            if (aviso.isNotBlank()) Text(aviso, style = EstiloRotulo, color = Ocre)
        }
    }
}
