package com.tamojunto.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class Secao(val rotulo: String) { FILMES("Filmes"), SERIES("Séries"), JOGOS("Jogos") }

enum class Filtro(val rotulo: String) { TODOS("Todos"), FEITOS("Feitos"), PENDENTES("Pendentes") }

// Filmes, séries e jogos em uma tela só, como no site. Por enquanto só leitura.
@Composable
fun TelaFilmes(dados: DadosDoCasal) {
    var secao by rememberSaveable { mutableStateOf(Secao.FILMES) }
    var filtro by rememberSaveable { mutableStateOf(Filtro.TODOS) }

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

        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Secao.entries.forEach { s ->
                FilterChip(
                    selected = secao == s,
                    onClick = { secao = s },
                    label = { Text(s.rotulo) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Filtro.entries.forEach { f ->
                FilterChip(
                    selected = filtro == f,
                    onClick = { filtro = f },
                    label = { Text(f.rotulo) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        if (visiveis.isEmpty()) {
            Text("Nada por aqui ainda.", modifier = Modifier.padding(top = 24.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
                items(visiveis, key = { it.chave }) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                            val detalhes = listOf(item.ano, item.genero).filter { it.isNotBlank() }.joinToString(" · ")
                            if (detalhes.isNotBlank()) Text(detalhes, modifier = Modifier.padding(top = 4.dp))
                            val quem = listOf(item.quem, item.quando).filter { it.isNotBlank() }.joinToString(" · ")
                            if (quem.isNotBlank()) Text(quem, modifier = Modifier.padding(top = 2.dp))
                            Text(
                                if (item.feito) "Feito" else "Pendente",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
