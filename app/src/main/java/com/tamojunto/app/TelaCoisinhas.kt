package com.tamojunto.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

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

// Mesmas categorias do roteiro.js do site.
val CATEGORIAS_COISINHAS = listOf(
    "visitar" to "Visitar Local",
    "comida" to "Comida",
    "receita" to "Fazer Receita",
    "drink" to "Experimentar Drink",
    "outro" to "Outro"
)

fun rotuloCategoria(chave: String): String = CATEGORIAS_COISINHAS.firstOrNull { it.first == chave }?.second ?: chave

// Coisinhas do casal: marcar como feita funciona nos dois aparelhos ao mesmo tempo.
@Composable
fun TelaCoisinhas(dados: DadosDoCasal) {
    var novaAberta by rememberSaveable { mutableStateOf(false) }
    var paraApagar by remember { mutableStateOf<Item?>(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Coisinhas", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = { novaAberta = true }, modifier = Modifier.padding(top = 12.dp)) {
            Text("Nova coisinha")
        }

        if (dados.coisinhas.isEmpty()) {
            Text("Nenhuma coisinha ainda.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(dados.coisinhas, key = { it.chave }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.feito,
                                onCheckedChange = { dados.alternarFeito("coisinhas", item) {} }
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                                val detalhes = listOf(rotuloCategoria(item.categoria), item.quando)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · ")
                                if (detalhes.isNotBlank()) Text(detalhes, modifier = Modifier.padding(top = 2.dp))
                                if (item.notas.isNotBlank()) Text(item.notas, modifier = Modifier.padding(top = 4.dp))
                            }
                            TextButton(onClick = { paraApagar = item }) { Text("Apagar") }
                        }
                    }
                }
            }
        }
    }

    if (novaAberta) DialogoNovaCoisinha(dados, aoFechar = { novaAberta = false })

    paraApagar?.let { item ->
        DialogoConfirmar(
            titulo = "Apagar coisinha?",
            texto = item.titulo,
            rotuloConfirmar = "Apagar",
            aoConfirmar = { dados.apagarItem("coisinhas", item.chave, item.titulo) {} },
            aoFechar = { paraApagar = null }
        )
    }
}

@Composable
fun DialogoNovaCoisinha(dados: DadosDoCasal, aoFechar: () -> Unit) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("outro") }
    var quando by rememberSaveable { mutableStateOf("") }
    var notas by rememberSaveable { mutableStateOf("") }
    val dataValida = quando.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(quando.trim())

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Nova coisinha") },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("O que é") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).horizontalScroll(rememberScrollState())
                ) {
                    CATEGORIAS_COISINHAS.forEach { (chave, rotulo) ->
                        FilterChip(
                            selected = categoria == chave,
                            onClick = { categoria = chave },
                            label = { Text(rotulo) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
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
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = titulo.isNotBlank() && dataValida,
                onClick = {
                    dados.criarItem(
                        "coisinhas",
                        mapOf(
                            "category" to categoria,
                            "title" to titulo.trim(),
                            "targetDate" to quando.trim(),
                            "notes" to notas.trim(),
                            "done" to false
                        ),
                        titulo.trim()
                    ) {}
                    aoFechar()
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
