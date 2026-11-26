package com.tamojunto.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Notas ainda não foram portadas: a senha do bloco é conferida no servidor pelo site.
@Composable
fun TelaNotas() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bloco de Notas", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Em breve no app. Por enquanto, use o site.",
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
