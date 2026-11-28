@file:OptIn(ExperimentalMaterial3Api::class)

package com.tamojunto.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// SHA-256 em hexadecimal, igual ao que o site calcula antes de mandar a senha do Bloco.
fun sha256Hex(texto: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(texto.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

// Lê a foto escolhida: os bytes e o tipo (image/jpeg, por exemplo).
suspend fun lerFoto(contexto: Context, uri: Uri): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
    val bytes = contexto.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
    bytes to (contexto.contentResolver.getType(uri) ?: "image/jpeg")
}

// Mensagem de uma ação: verde-suave quando deu certo, ocre quando o servidor recusou.
private data class Aviso(val texto: String, val erro: Boolean)

private fun avisoDe(r: RespostaServidor, sucesso: String): Aviso =
    if (r.ok) Aviso(r.msg.ifBlank { sucesso }, false)
    else Aviso(r.msg.ifBlank { "Não deu certo. Tente de novo." }, true)

@Composable
private fun AvisoTexto(aviso: Aviso?) {
    if (aviso == null) return
    Text(
        aviso.texto,
        style = EstiloRotulo,
        color = if (aviso.erro) Ocre else TintaSuave,
        modifier = Modifier.padding(top = 4.dp)
    )
}

// Bloco de uma seção: título em itálico serifado, apoio pequeno e o conteúdo com espaço entre os itens.
@Composable
private fun Secao(titulo: String, apoio: String? = null, conteudo: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text(titulo, style = EstiloTituloSecao, color = Tinta)
        if (apoio != null) {
            Text(apoio, style = EstiloApoio, color = TintaSuave, modifier = Modifier.padding(top = 4.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = conteudo
        )
    }
}

// Configurações do site, na mesma ordem: perfil, casal (data, convite, fotos, senha) e conta.
@Composable
fun TelaConfiguracoes(dados: DadosDoCasal, usuario: FirebaseUser) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val areaTransferencia = LocalClipboardManager.current

    // Quando isto muda, a tela busca o perfil e as fotos de novo.
    var recarregar by remember { mutableStateOf(0) }

    var perfilCarregado by rememberSaveable { mutableStateOf(false) }
    var nome by rememberSaveable { mutableStateOf(usuario.displayName ?: "") }
    var handle by rememberSaveable { mutableStateOf("") }
    var fotoPerfil by remember { mutableStateOf("") }
    var avisoNome by remember { mutableStateOf<Aviso?>(null) }
    var avisoHandle by remember { mutableStateOf<Aviso?>(null) }
    var avisoFotoPerfil by remember { mutableStateOf<Aviso?>(null) }

    var avisoData by remember { mutableStateOf<Aviso?>(null) }
    var escolhendoData by remember { mutableStateOf(false) }
    var codigoConvite by remember { mutableStateOf<String?>(null) }
    var avisoConvite by remember { mutableStateOf<Aviso?>(null) }
    var fotosCasal by remember { mutableStateOf<List<FotoCasal>>(emptyList()) }
    var avisoFotosCasal by remember { mutableStateOf<Aviso?>(null) }
    var fotoParaRemover by remember { mutableStateOf<FotoCasal?>(null) }

    var senhaAtual by remember { mutableStateOf("") }
    var senhaNova by remember { mutableStateOf("") }
    var senhaRepetida by remember { mutableStateOf("") }
    var avisoSenha by remember { mutableStateOf<Aviso?>(null) }

    var pedindoSair by remember { mutableStateOf(false) }

    LaunchedEffect(recarregar) {
        val perfil = AmizadesServidor.carregarPerfil()
        if (perfil != null) {
            // Preenche nome e @ só na primeira vez, para não apagar o que a pessoa está digitando.
            if (!perfilCarregado) {
                nome = perfil.nome.takeIf { it != "Sem nome" } ?: nome
                handle = perfil.handle
                perfilCarregado = true
            }
            fotoPerfil = perfil.foto
        }
        if (dados.casalId != null) {
            fotosCasal = InicioServidorCasal.carregar().first?.fotos.orEmpty()
        }
    }

    val escolherFotoPerfil = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) escopo.launch {
            avisoFotoPerfil = Aviso("Enviando foto...", false)
            val arquivo = lerFoto(contexto, uri)
            if (arquivo == null) {
                avisoFotoPerfil = Aviso("Não consegui ler essa foto.", true)
            } else {
                val r = Servidor.enviarFoto("/api/config/foto-perfil", arquivo.first, arquivo.second)
                avisoFotoPerfil = avisoDe(r, "Foto salva.")
                recarregar++
            }
        }
    }

    val escolherFotoCasal = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) escopo.launch {
            avisoFotosCasal = Aviso("Enviando foto...", false)
            val arquivo = lerFoto(contexto, uri)
            if (arquivo == null) {
                avisoFotosCasal = Aviso("Não consegui ler essa foto.", true)
            } else {
                val r = Servidor.enviarFoto("/api/config/fotos", arquivo.first, arquivo.second)
                avisoFotosCasal = avisoDe(r, "Foto adicionada.")
                recarregar++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .papelDoSite()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Configurações", style = EstiloTituloSecao, color = Tinta)
        Text(
            if (dados.casalId == null) "Sua conta." else "Sua conta e o casal.",
            style = EstiloApoio,
            color = TintaSuave,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Perfil
        Secao("Seu perfil", "É assim que as outras pessoas veem você.") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AvatarPessoa(nome = nome.ifBlank { "?" }, foto = fotoPerfil, tamanho = 72.dp)
                Text(usuario.email.orEmpty(), style = EstiloRotulo, color = TintaSuave)
            }
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it.take(40) },
                label = { Text("Seu nome") },
                supportingText = { Text("Até 40 letras.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Salvar nome",
                primario = true,
                modifier = Modifier.fillMaxWidth(),
                aoClicar = {
                    escopo.launch {
                        val r = Servidor.postJson("/api/config/perfil", JSONObject().put("nome", nome.trim()))
                        avisoNome = avisoDe(r, "Nome salvo.")
                        if (r.ok) recarregar++
                    }
                }
            )
            AvisoTexto(avisoNome)

            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it.replace(" ", "").removePrefix("@") },
                label = { Text("Seu @") },
                prefix = { Text("@") },
                supportingText = { Text("Amigos encontram você por esse @.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            BotaoSite(
                texto = "Salvar @",
                primario = true,
                modifier = Modifier.fillMaxWidth(),
                aoClicar = {
                    escopo.launch {
                        val msg = AmizadesServidor.definirHandle(handle.trim())
                        // definirHandle devolve a mensagem de sucesso fixa quando deu certo.
                        avisoHandle = Aviso(msg, msg != "Seu @ foi salvo.")
                        if (!avisoHandle!!.erro) recarregar++
                    }
                }
            )
            AvisoTexto(avisoHandle)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                BotaoSite(
                    texto = "Trocar foto",
                    modifier = Modifier.weight(1f),
                    aoClicar = {
                        escolherFotoPerfil.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                BotaoSite(
                    texto = "Foto do Google",
                    modifier = Modifier.weight(1f),
                    aoClicar = {
                        escopo.launch {
                            val r = Servidor.apagar("/api/config/foto-perfil")
                            avisoFotoPerfil = avisoDe(r, "Agora você usa a foto da conta Google.")
                            if (r.ok) recarregar++
                        }
                    }
                )
            }
            AvisoTexto(avisoFotoPerfil)
        }

        if (dados.casalId != null) {
            // Casal
            Secao("O casal", "Data, convite e as fotos da página inicial.") {
                Text("Data de início", style = EstiloRotulo, color = TintaSuave)
                Text(
                    dados.dataNamoro?.let { dataBr(it) } ?: "Não definida",
                    style = EstiloApoio,
                    color = Tinta
                )
                BotaoSite(texto = "Escolher data", aoClicar = { escolhendoData = true })
                AvisoTexto(avisoData)

                Text("Convidar a outra pessoa", style = EstiloRotulo, color = TintaSuave, modifier = Modifier.padding(top = 8.dp))
                Text("O código vale 7 dias e só pode ser usado uma vez.", style = EstiloApoio, color = TintaSuave)
                BotaoSite(
                    texto = "Gerar código",
                    aoClicar = {
                        escopo.launch {
                            val r = Servidor.postJson("/api/config/convite", JSONObject())
                            codigoConvite = if (r.ok) r.corpo?.optString("codigo") else null
                            avisoConvite = if (r.ok) null else avisoDe(r, "")
                        }
                    }
                )
                codigoConvite?.let { codigo ->
                    Text(codigo, style = EstiloTituloSecao, color = Tinta)
                    BotaoSite(
                        texto = "Copiar código",
                        aoClicar = { areaTransferencia.setText(AnnotatedString(codigo)) }
                    )
                }
                AvisoTexto(avisoConvite)

                Text("Fotos da página inicial", style = EstiloRotulo, color = TintaSuave, modifier = Modifier.padding(top = 8.dp))
                if (fotosCasal.isEmpty()) {
                    Text("Nenhuma foto ainda.", style = EstiloApoio, color = TintaSuave)
                }
                fotosCasal.forEach { foto ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(
                            model = foto.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        BotaoSite(texto = "Remover", aoClicar = { fotoParaRemover = foto })
                    }
                }
                BotaoSite(
                    texto = "Adicionar foto",
                    aoClicar = {
                        escolherFotoCasal.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                AvisoTexto(avisoFotosCasal)
            }

            // Senha do Bloco de Notas
            Secao("Senha do Bloco de Notas", "A mesma senha vale para o casal inteiro. Para trocar, confirme a senha atual.") {
                OutlinedTextField(
                    value = senhaAtual,
                    onValueChange = { senhaAtual = it },
                    label = { Text("Senha atual") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = senhaNova,
                    onValueChange = { senhaNova = it },
                    label = { Text("Nova senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = senhaRepetida,
                    onValueChange = { senhaRepetida = it },
                    label = { Text("Repita a nova senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSite(
                    texto = "Trocar senha",
                    primario = true,
                    modifier = Modifier.fillMaxWidth(),
                    aoClicar = {
                        when {
                            senhaNova.length < 4 -> avisoSenha = Aviso("A nova senha precisa ter pelo menos 4 caracteres.", true)
                            senhaNova != senhaRepetida -> avisoSenha = Aviso("As duas senhas novas não batem.", true)
                            else -> escopo.launch {
                                // Como no site: manda o SHA-256 das senhas, nunca a senha em texto.
                                val r = Servidor.postJson(
                                    "/api/config/senha-bloco",
                                    JSONObject()
                                        .put("atual", sha256Hex(senhaAtual))
                                        .put("nova", sha256Hex(senhaNova))
                                )
                                avisoSenha = avisoDe(r, "Senha trocada.")
                                if (r.ok) {
                                    senhaAtual = ""
                                    senhaNova = ""
                                    senhaRepetida = ""
                                }
                            }
                        }
                    }
                )
                AvisoTexto(avisoSenha)
            }
        }

        // Conta
        Secao("Conta", "Sair encerra a sessão neste aparelho.") {
            BotaoSite(texto = "Sair da conta", aoClicar = { pedindoSair = true })
        }

        Spacer(Modifier.height(LocalAlturaDock.current))
    }

    if (escolhendoData && dados.casalId != null) {
        DialogoDataInicio(dados) { data ->
            escolhendoData = false
            if (data != null) escopo.launch {
                val r = Servidor.postJson("/api/config/data-namoro", JSONObject().put("data_namoro", data))
                avisoData = avisoDe(r, "Data salva.")
                if (r.ok) recarregar++
            }
        }
    }

    fotoParaRemover?.let { foto ->
        DialogoConfirmar(
            titulo = "Remover foto?",
            texto = "Ela some da página inicial de vocês.",
            rotuloConfirmar = "Remover",
            aoConfirmar = {
                escopo.launch {
                    val r = Servidor.apagar("/api/config/fotos/" + Uri.encode(foto.nome))
                    avisoFotosCasal = avisoDe(r, "Foto removida.")
                    if (r.ok) recarregar++
                }
            },
            aoFechar = { fotoParaRemover = null }
        )
    }

    if (pedindoSair) {
        DialogoConfirmar(
            titulo = "Sair da conta?",
            texto = "Você vai precisar entrar de novo com o Google.",
            rotuloConfirmar = "Sair",
            aoConfirmar = {
                Servidor.encerrar()
                FirebaseAuth.getInstance().signOut()
            },
            aoFechar = { pedindoSair = false }
        )
    }
}

private fun inicioDoCasalMillis(iso: String?): Long? =
    iso?.let { runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() }

// Diálogo de data usado em "Data de início": salva no site e o banco atualiza a tela sozinho.
@Composable
fun DialogoDataInicio(dados: DadosDoCasal, aoFechar: (String?) -> Unit) {
    val estado = rememberDatePickerState(initialSelectedDateMillis = inicioDoCasalMillis(dados.dataNamoro))
    DatePickerDialog(
        onDismissRequest = { aoFechar(null) },
        confirmButton = {
            TextButton(onClick = {
                val millis = estado.selectedDateMillis
                if (millis != null) {
                    aoFechar(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                } else {
                    aoFechar(null)
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = { aoFechar(null) }) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = estado)
    }
}
