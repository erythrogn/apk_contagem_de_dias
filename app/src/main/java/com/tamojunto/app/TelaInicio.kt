package com.tamojunto.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Início: quanto tempo o casal está junto. Conta a partir do dia do namoro, no fuso de Belém (como o site).
@Composable
fun TelaInicio(dados: DadosDoCasal, usuario: FirebaseUser) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val nome = usuario.displayName?.substringBefore(" ") ?: ""
        val inicio = dados.dataNamoro?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        when {
            dados.carregando -> CircularProgressIndicator()
            dados.casalId == null -> {
                Text("Olá, $nome", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Você ainda não tem um casal. Criar ou entrar em um casal ainda não está no app.",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            inicio == null -> {
                Text("Olá, $nome", style = MaterialTheme.typography.headlineMedium)
                Text("Falta a data do namoro. Defina no site, em Configurações.", modifier = Modifier.padding(top = 12.dp))
            }
            else -> {
                val agora = LocalDateTime.now(ZoneId.of("America/Belem"))
                val dias = Duration.between(LocalDateTime.of(inicio, LocalTime.of(0, 1)), agora).toDays()
                Text("Contagem de Dias", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "$dias",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    if (dias == 1L) "dia juntos" else "dias juntos",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Desde ${dataBr(inicio.toString())}",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
