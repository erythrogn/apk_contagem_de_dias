package com.tamojunto.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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

// Botão do site: contorno fino, cantos retos e texto centralizado. O primário preenche em azul-tinta.
@Composable
internal fun BotaoSite(
    texto: String,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    primario: Boolean = false
) {
    Box(
        modifier
            .alpha(if (habilitado) 1f else 0.4f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (primario) Tinta else Papel)
            .border(1.dp, Tinta, RoundedCornerShape(4.dp))
            .clickable(enabled = habilitado, onClick = aoClicar)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            style = EstiloBotao,
            color = if (primario) Papel else Tinta,
            textAlign = TextAlign.Center
        )
    }
}

// Filtro ou escolha (espaço, humor, destinatário): cabe em uma linha e fica destacado em dourado quando ativo.
@Composable
internal fun FiltroSite(
    texto: String,
    ativo: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
    icone: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (ativo) Ouro.copy(alpha = 0.25f) else Papel)
            .border(1.dp, if (ativo) Tinta else Tinta.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .clickable(onClick = aoClicar)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icone != null) {
            icone()
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, style = EstiloChip, color = Tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// Aba de seção (Filmes, Séries, Jogos): texto em itálico com o filete dourado embaixo quando ativa.
@Composable
internal fun AbaSite(texto: String, ativa: Boolean, aoClicar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.clickable(onClick = aoClicar).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(texto, style = EstiloApoio, color = if (ativa) Tinta else TintaSuave)
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(32.dp)
                .height(1.dp)
                .background(if (ativa) Ouro else Color.Transparent)
        )
    }
}
