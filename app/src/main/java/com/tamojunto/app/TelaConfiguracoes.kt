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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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

// Configurações do site, na mesma ordem: perfil, casal, fotos da página inicial, senha do Bloco e sair.
@Composable
fun TelaConfiguracoes(dados: DadosDoCasal, usuario: FirebaseUser) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()
    val areaTransferencia = LocalClipboardManager.current

    var nome by rememberSaveable { mutableStateOf(usuario.displayName ?: "") }
    var avisoPerfil by remember { mutableStateOf("") }
    var avisoFotoPerfil by remember { mutableStateOf("") }
    var avisoData by remember { mutableStateOf("") }
    var escolhendoData by remember { mutableStateOf(false) }
    var codigoConvite by remember { mutableStateOf<String?>(null) }
    var avisoConvite by remember { mutableStateOf("") }
    var avisoFotoCasal by remember { mutableStateOf("") }
    var senhaAtual by remember { mutableStateOf("") }
    var senhaNova by remember { mutableStateOf("") }
    var senhaRepetida by remember { mutableStateOf("") }
    var avisoSenha by remember { mutableStateOf("") }

    val escolherFotoPerfil = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) escopo.launch {
            avisoFotoPerfil = "Enviando foto..."
            val arquivo = lerFoto(contexto, uri)
            avisoFotoPerfil = if (arquivo == null) {
                "Não consegui ler essa foto."
            } else {
                Servidor.enviarFoto("/api/config/foto-perfil", arquivo.first, arquivo.second).msg
            }
        }
    }

    val escolherFotoCasal = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) escopo.launch {
            avisoFotoCasal = "Enviando foto..."
            val arquivo = lerFoto(contexto, uri)
            avisoFotoCasal = if (arquivo == null) {
                "Não consegui ler essa foto."
            } else {
                Servidor.enviarFoto("/api/config/fotos", arquivo.first, arquivo.second).msg
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Configurações", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (dados.casalId == null) "Sua conta." else "Sua conta e o casal.",
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Perfil
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Seu perfil", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it.take(40) },
                    label = { Text("Seu nome") },
                    supportingText = { Text("Até 40 letras. É assim que as outras pessoas veem você.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Button(
                    onClick = {
                        escopo.launch {
                            val r = Servidor.postJson("/api/config/perfil", JSONObject().put("nome", nome.trim()))
                            avisoPerfil = r.msg
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Salvar nome") }
                if (avisoPerfil.isNotBlank()) Text(avisoPerfil, modifier = Modifier.padding(top = 4.dp))

                Row(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedButton(onClick = {
                        escolherFotoPerfil.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Trocar foto") }
                    TextButton(onClick = {
                        escopo.launch {
                            avisoFotoPerfil = Servidor.apagar("/api/config/foto-perfil").msg
                        }
                    }) { Text("Usar a foto da conta Google") }
                }
                if (avisoFotoPerfil.isNotBlank()) Text(avisoFotoPerfil)
            }
        }

        // Casal
        if (dados.casalId != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("O casal", style = MaterialTheme.typography.titleMedium)

                    Text(
                        "Data de início: " + (dados.dataNamoro?.let { dataBr(it) } ?: "não definida"),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedButton(onClick = { escolhendoData = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Escolher data")
                    }
                    if (avisoData.isNotBlank()) Text(avisoData, modifier = Modifier.padding(top = 4.dp))

                    Text("Convidar a outra pessoa", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                    Text("O código vale 7 dias e só pode ser usado uma vez.", modifier = Modifier.padding(top = 4.dp))
                    Button(
                        onClick = {
                            escopo.launch {
                                val r = Servidor.postJson("/api/config/convite", JSONObject())
                                codigoConvite = if (r.ok) r.corpo?.optString("codigo") else null
                                avisoConvite = if (r.ok) "" else r.msg
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Gerar código") }
                    codigoConvite?.let { codigo ->
                        Text(codigo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
                        OutlinedButton(onClick = { areaTransferencia.setText(AnnotatedString(codigo)) }) {
                            Text("Copiar código")
                        }
                    }
                    if (avisoConvite.isNotBlank()) Text(avisoConvite, modifier = Modifier.padding(top = 4.dp))

                    Text("Fotos da página inicial", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
                    Text("Por enquanto o app só adiciona fotos. Ver e apagar fica para depois.", modifier = Modifier.padding(top = 4.dp))
                    OutlinedButton(
                        onClick = {
                            escolherFotoCasal.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Adicionar foto") }
                    if (avisoFotoCasal.isNotBlank()) Text(avisoFotoCasal, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Senha do Bloco de Notas
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Senha do Bloco de Notas", style = MaterialTheme.typography.titleMedium)
                    Text("A mesma senha vale para o casal inteiro. Para trocar, confirme a senha atual.", modifier = Modifier.padding(top = 4.dp))
                    OutlinedTextField(
                        value = senhaAtual,
                        onValueChange = { senhaAtual = it },
                        label = { Text("Senha atual") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = senhaNova,
                        onValueChange = { senhaNova = it },
                        label = { Text("Nova senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = senhaRepetida,
                        onValueChange = { senhaRepetida = it },
                        label = { Text("Repita a nova senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    Button(
                        onClick = {
                            when {
                                senhaNova.length < 4 -> avisoSenha = "A nova senha precisa ter pelo menos 4 caracteres."
                                senhaNova != senhaRepetida -> avisoSenha = "As duas senhas novas não batem."
                                else -> escopo.launch {
                                    // Como no site: manda o SHA-256 das senhas, nunca a senha em texto.
                                    val r = Servidor.postJson(
                                        "/api/config/senha-bloco",
                                        JSONObject()
                                            .put("atual", sha256Hex(senhaAtual))
                                            .put("nova", sha256Hex(senhaNova))
                                    )
                                    avisoSenha = r.msg
                                    if (r.ok) {
                                        senhaAtual = ""
                                        senhaNova = ""
                                        senhaRepetida = ""
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("Trocar senha") }
                    if (avisoSenha.isNotBlank()) Text(avisoSenha, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        if (escolhendoData && dados.casalId != null) {
            DialogoDataInicio(dados) { data ->
                escolhendoData = false
                if (data != null) escopo.launch {
                    avisoData = Servidor.postJson("/api/config/data-namoro", JSONObject().put("data_namoro", data)).msg
                }
            }
        }

        // Sair
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Conta", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = {
                        Servidor.encerrar()
                        FirebaseAuth.getInstance().signOut()
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text("Sair da conta") }
            }
        }
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
