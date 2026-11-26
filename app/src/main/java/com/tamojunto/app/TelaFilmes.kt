package com.tamojunto.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Colecao é o caminho no banco; rotulo é o que aparece na tela.
enum class Secao(val rotulo: String, val colecao: String) {
    FILMES("Filmes", "filmes"),
    SERIES("Séries", "series"),
    JOGOS("Jogos", "jogos")
}

enum class Filtro(val rotulo: String) { TODOS("Todos"), FEITOS("Feitos"), PENDENTES("Pendentes") }

// Filmes, séries e jogos em uma tela só, como no site.
@Composable
fun TelaFilmes(dados: DadosDoCasal) {
    var secao by rememberSaveable { mutableStateOf(Secao.FILMES) }
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }
    var novoAberto by rememberSaveable { mutableStateOf(false) }
    var paraApagar by remember { mutableStateOf<Item?>(null) }

    val lista = when (secao) {
        Secao.FILMES -> dados.filmes
        Secao.SERIES -> dados.series
        Secao.JOGOS -> dados.jogos
    }
    val visiveis = lista.filter {
        when (filtro) {
            Filtro.TODOS -> true
            Filtro.FEITOS -> it.feito
            Filtro.PENDENTES -> !it.feito
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Filmes", style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState())
        ) {
            Secao.entries.forEach { s ->
                FilterChip(
                    selected = secao == s,
                    onClick = { secao = s },
                    label = { Text(s.rotulo) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState())
        ) {
            Filtro.entries.forEach { f ->
                FilterChip(
                    selected = filtro == f,
                    onClick = { filtro = f },
                    label = { Text(f.rotulo) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Button(onClick = { novoAberto = true }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Novo em ${secao.rotulo.lowercase()}")
        }

        if (visiveis.isEmpty()) {
            Text("Nada aqui ainda.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(visiveis, key = { it.chave }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.feito,
                                onCheckedChange = { dados.alternarFeito(secao.colecao, item) {} }
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                                val detalhes = listOf(item.ano, item.genero, item.quando)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · ")
                                if (detalhes.isNotBlank()) Text(detalhes, modifier = Modifier.padding(top = 2.dp))
                            }
                            TextButton(onClick = { paraApagar = item }) { Text("Apagar") }
                        }
                    }
                }
            }
        }
    }

    if (novoAberto) DialogoNovoItem(secao, dados, aoFechar = { novoAberto = false })

    paraApagar?.let { item ->
        DialogoConfirmar(
            titulo = "Apagar ${item.titulo}?",
            texto = "Isso remove da lista do casal.",
            rotuloConfirmar = "Apagar",
            aoConfirmar = { dados.apagarItem(secao.colecao, item.chave, item.titulo) {} },
            aoFechar = { paraApagar = null }
        )
    }
}

// Campos extras que o site grava em cada seção ao criar um item.
private fun camposIniciais(secao: Secao): Map<String, Any?> = when (secao) {
    Secao.FILMES -> mapOf("watched" to false)
    Secao.SERIES -> mapOf("status" to "quero ver")
    Secao.JOGOS -> mapOf("status" to "quero jogar", "platform" to "", "where" to "")
}

@Composable
fun DialogoNovoItem(secao: Secao, dados: DadosDoCasal, aoFechar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var ano by rememberSaveable { mutableStateOf("") }
    var genero by rememberSaveable { mutableStateOf("") }
    var quando by rememberSaveable { mutableStateOf("") }
    val dataValida = quando.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(quando.trim())

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Novo em ${secao.rotulo.lowercase()}") },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ano,
                    onValueChange = { ano = it },
                    label = { Text("Ano (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = genero,
                    onValueChange = { genero = it },
                    label = { Text("Gênero (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = quando,
                    onValueChange = { quando = it },
                    label = { Text("Data (opcional)") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    isError = !dataValida,
                    supportingText = { if (!dataValida) Text("Use o formato AAAA-MM-DD") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = titulo.isNotBlank() && dataValida,
                onClick = {
                    val campos = mutableMapOf<String, Any?>(
                        "title" to titulo.trim(),
                        "year" to ano.trim(),
                        "genre" to genero.trim(),
                        "targetDate" to quando.trim(),
                        "who" to ""
                    )
                    campos.putAll(camposIniciais(secao))
                    dados.criarItem(secao.colecao, campos, titulo.trim()) {}
                    aoFechar()
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
