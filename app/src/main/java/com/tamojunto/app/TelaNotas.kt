@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tamojunto.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

// Bloco de Notas: pede a senha do casal (conferida no servidor, como no site) e depois lista as notas.
@Composable
fun TelaNotas(dados: DadosDoCasal) {
    if (dados.casalId == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Você ainda não tem um casal. O Bloco de Notas fica no casal.")
        }
        return
    }

    var liberado by rememberSaveable { mutableStateOf(false) }
    if (liberado) {
        ListaNotas(dados)
    } else {
        GateNotas(aoLiberar = { liberado = true })
    }
}

@Composable
private fun GateNotas(aoLiberar: () -> Unit) {
    val escopo = rememberCoroutineScope()
    var senha by rememberSaveable { mutableStateOf("") }
    var aviso by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bloco de Notas", style = MaterialTheme.typography.headlineMedium)
        Text("Digite a senha do casal para abrir.", modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = senha.isNotBlank() && !enviando,
            onClick = {
                enviando = true
                aviso = ""
                escopo.launch {
                    // Como no site: manda o SHA-256 da senha (sem espaços nas pontas).
                    val r = Servidor.postJson(
                        "/api/bloco/auth",
                        JSONObject().put("hash", sha256Hex(senha.trim())),
                        repetirSeSessaoExpirou = false
                    )
                    enviando = false
                    if (r.ok) {
                        senha = ""
                        aoLiberar()
                    } else {
                        aviso = r.msg
                    }
                }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) { Text("Abrir") }
        if (aviso.isNotBlank()) Text(aviso, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ListaNotas(dados: DadosDoCasal) {
    var criando by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Bloco de Notas", style = MaterialTheme.typography.headlineMedium)
            if (dados.notas.isEmpty()) {
                Text("Nenhuma nota ainda.", modifier = Modifier.padding(top = 24.dp))
            } else {
                LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                    items(dados.notas, key = { it.chave }) { nota ->
                        CartaNota(nota, dados.meuSlot)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { criando = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (criando) {
        DialogoNovaNota(dados) { criando = false }
    }
}

@Composable
private fun CartaNota(nota: Nota, meuSlot: String?) {
    val minha = nota.autor == meuSlot
    val quem = if (minha) "por você" else "pela outra pessoa"
    val cabecalho = listOf(quem, nota.data).filter { it.isNotBlank() }.joinToString(" · ")

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(nota.titulo.ifBlank { "Sem título" }, style = MaterialTheme.typography.titleMedium)
            Text(cabecalho, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 2.dp))
            val corpo = when (nota.tipo) {
                "lista" -> nota.conteudo.lines()
                    .filter { it.isNotBlank() }
                    .joinToString("\n") { linha ->
                        if (linha.startsWith("[x]", ignoreCase = true)) "[x] " + linha.drop(3).trim()
                        else "[ ] " + linha.trim()
                    }
                "carta" -> if (minha) nota.conteudo else "Carta lacrada."
                else -> nota.conteudo
            }
            Text(corpo, modifier = Modifier.padding(top = 8.dp))
            if (nota.tipo == "pergunta" && nota.resposta.isNotBlank()) {
                Text("Resposta: ${nota.resposta}", modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun DialogoNovaNota(dados: DadosDoCasal, aoFechar: () -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var conteudo by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("texto") }
    var aviso by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Nova nota") },
        text = {
            Column {
                Row {
                    FilterChip(
                        selected = tipo == "texto",
                        onClick = { tipo = "texto" },
                        label = { Text("Texto") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = tipo == "lista",
                        onClick = { tipo = "lista" },
                        label = { Text("Lista") }
                    )
                }
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it.take(80) },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = conteudo,
                    onValueChange = { conteudo = it },
                    label = { Text(if (tipo == "lista") "Itens, um por linha" else "Conteúdo") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                if (aviso.isNotBlank()) Text(aviso, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (conteudo.isBlank()) {
                    aviso = "Escreva o conteúdo da nota."
                } else if (dados.criarNota(titulo.trim(), conteudo.trim(), tipo) == null) {
                    aviso = "Não consegui salvar agora. Tente de novo."
                } else {
                    aoFechar()
                }
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
        }
    )
}
