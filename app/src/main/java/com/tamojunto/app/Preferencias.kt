package com.tamojunto.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Cartões da página inicial, na ordem padrão. A chave é o que fica salvo no banco.
internal val CARTOES_INICIO = listOf(
    "hoje" to "Hoje",
    "perguntas" to "Pergunta do dia",
    "proximas" to "Próximas datas",
    "fotos" to "Fotos",
    "laco" to "Laço",
    "clima" to "Como vocês estão"
)

// Ordem e cartões escondidos. Vale para o casal inteiro e é salvo no banco do casal.
data class PrefInicio(val ordem: List<String>, val ocultos: Set<String>)

internal val PREF_INICIO_PADRAO = PrefInicio(CARTOES_INICIO.map { it.first }, emptySet())

// Lê e grava as preferências do espaço (casais/<id> ou grupos/<gid>) em preferenciasApp.
object PreferenciasApp {
    private fun referencia(caminho: String) =
        FirebaseDatabase.getInstance().getReference("$caminho/preferenciasApp/inicio")

    // Acompanha as mudanças ao vivo: quando um dos dois muda a página, o outro vê na hora.
    // Devolve a função que para de ouvir.
    fun observarInicio(caminho: String, aoMudar: (PrefInicio) -> Unit): () -> Unit {
        val ref = referencia(caminho)
        val ouvinte = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                aoMudar(lerInicio(snapshot))
            }

            override fun onCancelled(erro: DatabaseError) {
                // Sem conexão a página segue com a última ordem conhecida.
            }
        }
        ref.addValueEventListener(ouvinte)
        return { ref.removeEventListener(ouvinte) }
    }

    // Ignora chaves desconhecidas e completa com as que faltam, para nada sumir por engano.
    private fun lerInicio(snapshot: DataSnapshot): PrefInicio {
        val salvos = snapshot.child("ordem").children.mapNotNull { it.getValue(String::class.java) }
        val conhecidas = CARTOES_INICIO.map { it.first }
        val ordem = (salvos + conhecidas).filter { it in conhecidas }.distinct()
        val ocultos = snapshot.child("ocultos").children
            .mapNotNull { it.getValue(String::class.java) }
            .filter { it in conhecidas }
            .toSet()
        return PrefInicio(ordem, ocultos)
    }

    fun salvarInicio(caminho: String, pref: PrefInicio) {
        referencia(caminho).setValue(
            mapOf("ordem" to pref.ordem, "ocultos" to pref.ocultos.toList())
        )
    }
}

private fun trocarPosicao(lista: List<String>, de: Int, para: Int): List<String> {
    val nova = lista.toMutableList()
    val item = nova.removeAt(de)
    nova.add(para, item)
    return nova
}

// Folha "Organizar a página": escolhe o que aparece e a ordem. Cada mudança já é salva.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrganizarInicioFolha(
    pref: PrefInicio,
    aoMudar: (PrefInicio) -> Unit,
    aoFechar: () -> Unit
) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nomes = CARTOES_INICIO.toMap()
    ModalBottomSheet(onDismissRequest = aoFechar, sheetState = estado) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Sua página inicial", style = EstiloTituloSecao, color = Tinta, textAlign = TextAlign.Center)
            Text(
                "Escolha o que aparece e a ordem. Vale para o casal.",
                style = EstiloRotulo,
                color = TintaSuave,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                itemsIndexed(pref.ordem, key = { _, chave -> chave }) { indice, chave ->
                    val visivel = chave !in pref.ocultos
                    Column(Modifier.fillMaxWidth().animateItem()) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                nomes[chave] ?: chave,
                                style = EstiloApoio,
                                color = if (visivel) Tinta else TintaSuave,
                                modifier = Modifier.weight(1f)
                            )
                            SetaOrdem(
                                caminho = "M18 15l-6-6-6 6",
                                habilitada = indice > 0,
                                aoClicar = { aoMudar(pref.copy(ordem = trocarPosicao(pref.ordem, indice, indice - 1))) }
                            )
                            SetaOrdem(
                                caminho = "M6 9l6 6 6-6",
                                habilitada = indice < pref.ordem.lastIndex,
                                aoClicar = { aoMudar(pref.copy(ordem = trocarPosicao(pref.ordem, indice, indice + 1))) }
                            )
                            Switch(
                                checked = visivel,
                                onCheckedChange = { ligar ->
                                    aoMudar(
                                        pref.copy(
                                            ocultos = if (ligar) pref.ocultos - chave else pref.ocultos + chave
                                        )
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Papel,
                                    checkedTrackColor = Tinta,
                                    uncheckedThumbColor = TintaSuave,
                                    uncheckedTrackColor = Creme
                                )
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Tinta.copy(alpha = 0.08f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetaOrdem(caminho: String, habilitada: Boolean, aoClicar: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .alpha(if (habilitada) 1f else 0.3f)
            .clip(CircleShape)
            .clickable(enabled = habilitada, onClick = aoClicar),
        contentAlignment = Alignment.Center
    ) {
        IconeLinha(caminhos = listOf(caminho), tamanho = 18.dp, cor = Tinta)
    }
}
