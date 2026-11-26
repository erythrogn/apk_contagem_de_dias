package com.tamojunto.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Coisinhas do casal: marcar como feita funciona nos dois aparelhos ao mesmo tempo.
@Composable
fun TelaCoisinhas(dados: DadosDoCasal) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Coisinhas", style = MaterialTheme.typography.headlineMedium)

        if (dados.coisinhas.isEmpty()) {
            Text("Nenhuma coisinha ainda.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(dados.coisinhas, key = { it.chave }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = item.feito, onCheckedChange = { dados.alternarCoisinha(item) })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                                val detalhes = listOf(item.categoria, item.quando).filter { it.isNotBlank() }.joinToString(" · ")
                                if (detalhes.isNotBlank()) Text(detalhes, modifier = Modifier.padding(top = 2.dp))
                                if (item.notas.isNotBlank()) Text(item.notas, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
