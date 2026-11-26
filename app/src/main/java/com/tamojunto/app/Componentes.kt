package com.tamojunto.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

// Pergunta antes de apagar. Só chama aoConfirmar se a pessoa tocar no botão de confirmação.
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    rotuloConfirmar: String,
    aoConfirmar: () -> Unit,
    aoFechar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(onClick = {
                aoConfirmar()
                aoFechar()
            }) { Text(rotuloConfirmar) }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } }
    )
}
