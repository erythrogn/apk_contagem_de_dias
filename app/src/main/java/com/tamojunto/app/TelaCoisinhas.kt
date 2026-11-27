package com.tamojunto.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val CATEGORIAS_COISINHAS = listOf(
    "visitar" to "Visitar Local",
    "comida" to "Comida",
    "receita" to "Fazer Receita",
    "drink" to "Experimentar Drink",
    "outro" to "Outro"
)

private val CATEGORIAS_FILTRO = listOf("todos" to "Todos") + CATEGORIAS_COISINHAS

fun rotuloCategoria(chave: String): String =
    CATEGORIAS_COISINHAS.firstOrNull { it.first == chave }?.second ?: chave

private val FonteSerifaMenu = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold)
)

private val CartaoTopo = Color(0xFFFBF8EC)
private val CartaoBase = Color(0xFFF3EFD9)
private val AzulPill = Color(0xFFC7DAEC)
private val TintaAcento = Color(0xFF3F6FA6)
private val BordaDourada = Color(0xFFD6BE7A)
private val DestaqueAmarelo = Color(0xFFF6E27E)
private val PerigoCor = Color(0xFFB44343)

private data class IngredienteCompra(
    val texto: String,
    val feito: Boolean = false
)

private data class ExtraCoisinha(
    val tripId: String = "",
    val shoppingList: List<IngredienteCompra> = emptyList()
)

private enum class OrdenacaoMenu(val chave: String, val rotulo: String) {
    RECENTES("recentes", "Mais recentes"),
    DATA("data", "Data planejada"),
    AZ("az", "A–Z")
}

@Composable
fun TelaCoisinhas(dados: DadosDoCasal) {
    val escopo = rememberCoroutineScope()
    var novaAberta by rememberSaveable { mutableStateOf(false) }
    var itemEditando by remember { mutableStateOf<Item?>(null) }
    var paraApagar by remember { mutableStateOf<Item?>(null) }

    var filtroCategoria by rememberSaveable { mutableStateOf("todos") }
    var busca by rememberSaveable { mutableStateOf("") }
    var ordenacao by rememberSaveable { mutableStateOf(OrdenacaoMenu.RECENTES) }
    var ocultarFeitas by rememberSaveable { mutableStateOf(false) }

    var itemExpandidoChave by rememberSaveable { mutableStateOf<String?>(null) }
    var itemGirandoChave by remember { mutableStateOf<String?>(null) }
    var itemDestaqueChave by remember { mutableStateOf<String?>(null) }
    var sorteando by remember { mutableStateOf(false) }

    val extrasPorChave = remember { mutableStateListOf<Pair<String, ExtraCoisinha>>() }

    // Lê campos complementares do site (roteiro de viagem e lista de compras de receitas) sem alterar o schema web.
    DisposableEffect(dados.casalId) {
        val casalId = dados.casalId
        if (casalId.isNullOrBlank()) {
            onDispose { }
        } else {
            val ref = FirebaseDatabase.getInstance().getReference("casais/$casalId/coisinhas")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val mapa = mutableListOf<Pair<String, ExtraCoisinha>>()
                    for (child in snapshot.children) {
                        val chave = child.key ?: continue
                        val tripId = child.child("tripId").getValue(String::class.java).orEmpty()
                        val comprasNode = child.child("shoppingList")
                        val lista = mutableListOf<IngredienteCompra>()
                        for (itemNode in comprasNode.children) {
                            when (val valor = itemNode.value) {
                                is String -> if (valor.isNotBlank()) lista.add(IngredienteCompra(valor, false))
                                is Map<*, *> -> {
                                    val t = (valor["text"] ?: valor["title"] ?: valor["name"])?.toString().orEmpty()
                                    val d = (valor["done"] as? Boolean) ?: false
                                    if (t.isNotBlank()) lista.add(IngredienteCompra(t, d))
                                }
                            }
                        }
                        if (tripId.isNotBlank() || lista.isNotEmpty()) {
                            mapa.add(chave to ExtraCoisinha(tripId = tripId, shoppingList = lista))
                        }
                    }
                    extrasPorChave.clear()
                    extrasPorChave.addAll(mapa)
                }

                override fun onCancelled(error: DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            onDispose { ref.removeEventListener(listener) }
        }
    }

    LaunchedEffect(itemDestaqueChave) {
        if (itemDestaqueChave != null) {
            delay(1800)
            itemDestaqueChave = null
        }
    }

    val todas = dados.coisinhas.toList()
    val total = todas.size
    val concluidas = todas.count { it.feito }
    val progressoAlvo = if (total > 0) concluidas.toFloat() / total.toFloat() else 0f
    val progressoAnimado by animateFloatAsState(
        targetValue = progressoAlvo,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "progressoCoisinhas"
    )

    val filtradas = remember(todas, filtroCategoria, busca, ordenacao, ocultarFeitas) {
        val termo = busca.trim().lowercase()
        val base = todas.filter { item ->
            val catOk = filtroCategoria == "todos" || item.categoria == filtroCategoria
            val feitaOk = !ocultarFeitas || !item.feito
            val buscaOk = termo.isEmpty() ||
                item.titulo.lowercase().contains(termo) ||
                item.notas.lowercase().contains(termo) ||
                rotuloCategoria(item.categoria).lowercase().contains(termo)
            catOk && feitaOk && buscaOk
        }
        when (ordenacao) {
            OrdenacaoMenu.RECENTES -> base.reversed()
            OrdenacaoMenu.DATA -> base.sortedWith(
                compareBy<Item> { it.dataIso.ifBlank { "9999-12-31" } }.thenBy { it.titulo.lowercase() }
            )
            OrdenacaoMenu.AZ -> base.sortedBy { it.titulo.lowercase() }
        }
    }

    val gruposCardapio = remember(filtradas, filtroCategoria) {
        if (filtroCategoria != "todos") {
            if (filtradas.isEmpty()) emptyList()
            else listOf(rotuloCategoria(filtroCategoria) to filtradas)
        } else {
            val ordemChaves = CATEGORIAS_COISINHAS.map { it.first }
            val agrupado = filtradas.groupBy { it.categoria.ifBlank { "outro" } }
            val resultado = mutableListOf<Pair<String, List<Item>>>()
            ordemChaves.forEach { cat ->
                val lista = agrupado[cat].orEmpty()
                if (lista.isNotEmpty()) resultado.add(rotuloCategoria(cat) to lista)
            }
            agrupado.forEach { (cat, lista) ->
                if (cat !in ordemChaves && lista.isNotEmpty()) {
                    resultado.add(rotuloCategoria(cat) to lista)
                }
            }
            resultado
        }
    }

    val pendentesParaSorteio = remember(filtradas) { filtradas.filter { !it.feito } }

    fun sortearCoisinha() {
        if (sorteando || pendentesParaSorteio.isEmpty()) return
        escopo.launch {
            sorteando = true
            val candidatos = pendentesParaSorteio
            val escolhido = candidatos.random()
            if (candidatos.size > 1) {
                val voltas = minOf(12, maxOf(6, candidatos.size * 2))
                for (i in 0 until voltas) {
                    itemGirandoChave = candidatos[i % candidatos.size].chave
                    delay(75L + i * 14L)
                }
            }
            itemGirandoChave = null
            itemDestaqueChave = escolhido.chave
            itemExpandidoChave = escolhido.chave
            sorteando = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Papel),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabeçalho editorial idêntico ao site
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOSSO GRIMÓRIO DE MEMÓRIAS",
                    style = TextStyle(
                        fontFamily = Sem,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 2.6.sp
                    ),
                    color = Ocre
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Coisinhas ",
                        style = TextStyle(
                            fontFamily = FonteSerifaMenu,
                            fontWeight = FontWeight.Normal,
                            fontSize = 34.sp,
                            lineHeight = 38.sp
                        ),
                        color = Tinta
                    )
                    Text(
                        text = "para fazer",
                        style = TextStyle(
                            fontFamily = FonteSerifaMenu,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            fontSize = 34.sp,
                            lineHeight = 38.sp
                        ),
                        color = TintaAcento
                    )
                }
                Text(
                    text = "quero viver tudo com voce pq é com voce",
                    style = TextStyle(
                        fontFamily = FonteSerifaMenu,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp
                    ),
                    color = TintaSuave,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Barra de progresso das coisinhas
        item(key = "progresso") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CartaoTopo.copy(alpha = 0.72f))
                    .border(1.dp, BordaDourada.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONCLUÍDAS",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.6.sp
                        ),
                        color = TintaSuave
                    )
                    Text(
                        text = "$concluidas / $total",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = Tinta
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(AzulPill.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressoAnimado.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(TintaAcento, Ouro)
                                )
                            )
                    )
                }
            }
        }

        // Botão Adicionar ao Menu + Filtros de Categoria
        item(key = "acoes_filtros") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { novaAberta = true },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AzulPill.copy(alpha = 0.92f),
                        contentColor = Tinta
                    ),
                    border = BorderStroke(1.dp, BordaDourada.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(
                        text = "+ ADICIONAR AO MENU",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIAS_FILTRO.forEach { (chave, rotulo) ->
                        val contagem = if (chave == "todos") total else todas.count { it.categoria == chave }
                        val selecionado = filtroCategoria == chave
                        FilterChip(
                            selected = selecionado,
                            onClick = { filtroCategoria = chave },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CartaoTopo.copy(alpha = 0.7f),
                                labelColor = TintaSuave,
                                selectedContainerColor = TintaAcento,
                                selectedLabelColor = Creme
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selecionado,
                                borderColor = BordaDourada.copy(alpha = 0.6f),
                                selectedBorderColor = TintaAcento
                            ),
                            label = {
                                Text(
                                    text = "$rotulo ($contagem)",
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Ferramentas: busca, ordenação, ocultar feitas e sorteio
        item(key = "ferramentas") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    placeholder = {
                        Text(
                            "Buscar no cardápio…",
                            style = TextStyle(fontFamily = Sem, fontSize = 13.sp, color = TintaSuave)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.78f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
                        focusedBorderColor = TintaAcento,
                        unfocusedBorderColor = BordaDourada.copy(alpha = 0.7f),
                        focusedTextColor = Tinta,
                        unfocusedTextColor = Tinta
                    ),
                    trailingIcon = {
                        if (busca.isNotEmpty()) {
                            TextButton(onClick = { busca = "" }) {
                                Text("Limpar", fontSize = 11.sp, color = Ocre)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OrdenacaoMenu.entries.forEach { opcao ->
                        val ativo = ordenacao == opcao
                        FilterChip(
                            selected = ativo,
                            onClick = { ordenacao = opcao },
                            shape = RoundedCornerShape(6.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AzulPill.copy(alpha = 0.65f),
                                labelColor = Tinta,
                                selectedContainerColor = Tinta,
                                selectedLabelColor = Creme
                            ),
                            label = {
                                Text(
                                    text = opcao.rotulo.uppercase(),
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                            }
                        )
                    }

                    FilterChip(
                        selected = ocultarFeitas,
                        onClick = { ocultarFeitas = !ocultarFeitas },
                        shape = RoundedCornerShape(6.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = AzulPill.copy(alpha = 0.65f),
                            labelColor = Tinta,
                            selectedContainerColor = Ocre,
                            selectedLabelColor = Creme
                        ),
                        label = {
                            Text(
                                text = "OCULTAR FEITAS",
                                style = TextStyle(
                                    fontFamily = Sem,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                )
                            )
                        }
                    )
                }

                Button(
                    onClick = { sortearCoisinha() },
                    enabled = !sorteando && pendentesParaSorteio.isNotEmpty(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TintaAcento,
                        contentColor = Creme,
                        disabledContainerColor = TintaSuave.copy(alpha = 0.35f),
                        disabledContentColor = Creme.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Text(
                        text = if (sorteando) "SORTEANDO…" else "SORTEAR UMA PRA FAZER",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp
                        )
                    )
                }
            }
        }

        // Cartão de Cardápio ("Nossas coisinhas") com moldura dupla dourada
        item(key = "cardapio") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = Tinta.copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.verticalGradient(listOf(CartaoTopo, CartaoBase)))
                    .border(1.dp, BordaDourada, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.8.dp, Ocre.copy(alpha = 0.35f), RoundedCornerShape(5.dp))
                        .padding(horizontal = 16.dp, vertical = 22.dp)
                ) {
                    // Cabeçalho do cardápio
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "· CARDÁPIO ·",
                            style = TextStyle(
                                fontFamily = Sem,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 3.sp
                            ),
                            color = Ocre
                        )
                        Text(
                            text = "Nossas coisinhas",
                            style = TextStyle(
                                fontFamily = FonteSerifaMenu,
                                fontWeight = FontWeight.Normal,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            ),
                            color = Tinta,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "tudo o que queremos viver juntos",
                            style = TextStyle(
                                fontFamily = FonteSerifaMenu,
                                fontStyle = FontStyle.Italic,
                                fontSize = 15.sp
                            ),
                            color = TintaSuave,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        DivisorCardapio(modifier = Modifier.padding(top = 12.dp, bottom = 18.dp))
                    }

                    if (gruposCardapio.isEmpty()) {
                        Text(
                            text = if (todas.isEmpty()) "Aguardando memórias..." else "Nenhuma coisinha encontrada nesse filtro.",
                            style = TextStyle(
                                fontFamily = FonteSerifaMenu,
                                fontStyle = FontStyle.Italic,
                                fontSize = 17.sp
                            ),
                            color = TintaSuave,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp)
                        )
                    } else {
                        gruposCardapio.forEachIndexed { grupoIndex, (tituloGrupo, itensGrupo) ->
                            if (grupoIndex > 0) {
                                Spacer(modifier = Modifier.height(22.dp))
                            }
                            CabecalhoSecaoCardapio(titulo = tituloGrupo, quantidade = itensGrupo.size)
                            Spacer(modifier = Modifier.height(8.dp))

                            itensGrupo.forEach { item ->
                                val extra = extrasPorChave.firstOrNull { it.first == item.chave }?.second
                                val nomeViagem = if (!extra?.tripId.isNullOrBlank()) {
                                    dados.viagens.firstOrNull { it.chave == extra?.tripId }?.titulo.orEmpty()
                                } else {
                                    ""
                                }
                                LinhaItemCardapio(
                                    item = item,
                                    extra = extra,
                                    nomeViagem = nomeViagem,
                                    expandido = itemExpandidoChave == item.chave,
                                    girando = itemGirandoChave == item.chave,
                                    destacado = itemDestaqueChave == item.chave,
                                    aoAlternarExpandir = {
                                        itemExpandidoChave = if (itemExpandidoChave == item.chave) null else item.chave
                                    },
                                    aoAlternarFeito = {
                                        dados.alternarFeito("coisinhas", item) {}
                                    },
                                    aoAlternarIngrediente = { indiceIngrediente ->
                                        val casalId = dados.casalId
                                        val listaAtual = extra?.shoppingList ?: return@LinhaItemCardapio
                                        if (!casalId.isNullOrBlank() && indiceIngrediente in listaAtual.indices) {
                                            val atualizada = listaAtual.mapIndexed { idx, ing ->
                                                if (idx == indiceIngrediente) {
                                                    mapOf("text" to ing.texto, "done" to !ing.feito)
                                                } else {
                                                    mapOf("text" to ing.texto, "done" to ing.feito)
                                                }
                                            }
                                            FirebaseDatabase.getInstance()
                                                .getReference("casais/$casalId/coisinhas/${item.chave}/shoppingList")
                                                .setValue(atualizada)
                                        }
                                    },
                                    aoEditar = { itemEditando = item },
                                    aoApagar = { paraApagar = item }
                                )
                            }
                        }
                    }

                    Text(
                        text = "— Contagem de Dias —",
                        style = TextStyle(
                            fontFamily = FonteSerifaMenu,
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        ),
                        color = Ocre,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    )
                }
            }
        }
    }

    if (novaAberta) {
        DialogoNovaCoisinha(
            dados = dados,
            aoFechar = { novaAberta = false },
            aoCriado = { tituloCriado ->
                val recemCriado = dados.coisinhas.lastOrNull { it.titulo == tituloCriado }
                if (recemCriado != null) {
                    itemDestaqueChave = recemCriado.chave
                }
            }
        )
    }

    itemEditando?.let { item ->
        val extraAtual = extrasPorChave.firstOrNull { it.first == item.chave }?.second
        DialogoEditarCoisinha(
            dados = dados,
            item = item,
            extra = extraAtual,
            aoFechar = { itemEditando = null }
        )
    }

    paraApagar?.let { item ->
        DialogoConfirmar(
            titulo = "Apagar coisinha?",
            texto = item.titulo,
            rotuloConfirmar = "Apagar",
            aoConfirmar = {
                if (itemExpandidoChave == item.chave) itemExpandidoChave = null
                dados.apagarItem("coisinhas", item.chave, item.titulo) {}
            },
            aoFechar = { paraApagar = null }
        )
    }
}

@Composable
private fun DivisorCardapio(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 180.dp, height = 14.dp)) {
        val cor = Ocre
        val centroY = size.height / 2f
        val centroX = size.width / 2f
        drawLine(cor, Offset(0f, centroY), Offset(centroX - 20.dp.toPx(), centroY), strokeWidth = 1.dp.toPx())
        drawLine(cor, Offset(centroX + 20.dp.toPx(), centroY), Offset(size.width, centroY), strokeWidth = 1.dp.toPx())

        val losango = Path().apply {
            moveTo(centroX, 1.dp.toPx())
            lineTo(centroX + 6.dp.toPx(), centroY)
            lineTo(centroX, size.height - 1.dp.toPx())
            lineTo(centroX - 6.dp.toPx(), centroY)
            close()
        }
        drawPath(losango, color = cor)
        drawCircle(cor, radius = 1.8.dp.toPx(), center = Offset(centroX - 12.dp.toPx(), centroY))
        drawCircle(cor, radius = 1.8.dp.toPx(), center = Offset(centroX + 12.dp.toPx(), centroY))
    }
}

@Composable
private fun CabecalhoSecaoCardapio(titulo: String, quantidade: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BordaDourada.copy(alpha = 0.8f))
        )
        Text(
            text = titulo,
            style = TextStyle(
                fontFamily = FonteSerifaMenu,
                fontStyle = FontStyle.Italic,
                fontSize = 21.sp
            ),
            color = Tinta
        )
        Text(
            text = "$quantidade",
            style = TextStyle(
                fontFamily = Sem,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            ),
            color = Ocre
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(BordaDourada.copy(alpha = 0.8f))
        )
    }
}

@Composable
private fun LinhaItemCardapio(
    item: Item,
    extra: ExtraCoisinha?,
    nomeViagem: String,
    expandido: Boolean,
    girando: Boolean,
    destacado: Boolean,
    aoAlternarExpandir: () -> Unit,
    aoAlternarFeito: () -> Unit,
    aoAlternarIngrediente: (Int) -> Unit,
    aoEditar: () -> Unit,
    aoApagar: () -> Unit
) {
    val pulsoCheck by animateFloatAsState(
        targetValue = if (item.feito) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "pulsoCheck"
    )
    val corFundoLinha by animateColorAsState(
        targetValue = when {
            girando -> DestaqueAmarelo.copy(alpha = 0.45f)
            destacado -> DestaqueAmarelo.copy(alpha = 0.52f)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 280),
        label = "fundoItemCardapio"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(corFundoLinha)
            .padding(vertical = 7.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { aoAlternarExpandir() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.titulo,
                        style = TextStyle(
                            fontFamily = FonteSerifaMenu,
                            fontSize = 19.sp,
                            textDecoration = if (item.feito) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.feito) Tinta.copy(alpha = 0.55f) else Tinta,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (item.quando.isNotBlank()) {
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                        ) {
                            drawLine(
                                color = Tinta.copy(alpha = 0.28f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                            )
                        }
                        Text(
                            text = item.quando,
                            style = TextStyle(
                                fontFamily = Sem,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp
                            ),
                            color = TintaSuave.copy(alpha = if (item.feito) 0.55f else 1f)
                        )
                    }
                }

                if (item.notas.isNotBlank()) {
                    Text(
                        text = item.notas,
                        style = TextStyle(
                            fontFamily = FonteSerifaMenu,
                            fontStyle = FontStyle.Italic,
                            fontSize = 14.sp
                        ),
                        color = TintaSuave.copy(alpha = if (item.feito) 0.55f else 1f),
                        maxLines = if (expandido) 6 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (nomeViagem.isNotBlank()) {
                    Text(
                        text = "ROTEIRO: $nomeViagem",
                        style = TextStyle(
                            fontFamily = Sem,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.sp
                        ),
                        color = Ocre.copy(alpha = if (item.feito) 0.6f else 1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            // Checkbox quadrado editorial com alvo de toque acessível (44dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { aoAlternarFeito() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = pulsoCheck
                            scaleY = pulsoCheck
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (item.feito) TintaAcento else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (item.feito) TintaAcento else TintaSuave,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.feito) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val check = Path().apply {
                                moveTo(size.width * 0.18f, size.height * 0.52f)
                                lineTo(size.width * 0.42f, size.height * 0.76f)
                                lineTo(size.width * 0.84f, size.height * 0.24f)
                            }
                            drawPath(
                                path = check,
                                color = Creme,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }

        // Detalhes expandidos (mesma animação detalhe-abre do movimento.css)
        AnimatedVisibility(
            visible = expandido,
            enter = fadeIn(tween(220)) + expandVertically(tween(240)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(200))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Text(
                    text = rotuloCategoria(item.categoria).uppercase(),
                    style = TextStyle(
                        fontFamily = Sem,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp
                    ),
                    color = Ocre
                )

                val ingredientes = extra?.shoppingList.orEmpty()
                if (ingredientes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AzulPill.copy(alpha = 0.42f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lista de compras",
                            style = TextStyle(
                                fontFamily = Sem,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = Tinta
                        )
                        ingredientes.forEachIndexed { idx, ing ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { aoAlternarIngrediente(idx) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (ing.feito) TintaAcento else Color.Transparent)
                                        .border(1.2.dp, TintaAcento, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ing.feito) {
                                        Canvas(modifier = Modifier.size(10.dp)) {
                                            val p = Path().apply {
                                                moveTo(size.width * 0.2f, size.height * 0.5f)
                                                lineTo(size.width * 0.45f, size.height * 0.75f)
                                                lineTo(size.width * 0.85f, size.height * 0.25f)
                                            }
                                            drawPath(p, Creme, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
                                        }
                                    }
                                }
                                Text(
                                    text = ing.texto,
                                    style = TextStyle(
                                        fontFamily = Sem,
                                        fontSize = 12.sp,
                                        textDecoration = if (ing.feito) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (ing.feito) TintaSuave else Tinta
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = aoEditar,
                        shape = RoundedCornerShape(4.dp),
                        color = AzulPill.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "EDITAR",
                            style = TextStyle(
                                fontFamily = Sem,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = Tinta,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    Surface(
                        onClick = aoApagar,
                        shape = RoundedCornerShape(4.dp),
                        color = AzulPill.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "APAGAR",
                            style = TextStyle(
                                fontFamily = Sem,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            ),
                            color = PerigoCor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoNovaCoisinha(
    dados: DadosDoCasal,
    aoFechar: () -> Unit,
    aoCriado: ((String) -> Unit)? = null
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("visitar") }
    var quando by rememberSaveable { mutableStateOf("") }
    var tripId by rememberSaveable { mutableStateOf("") }
    var notas by rememberSaveable { mutableStateOf("") }
    var ingredienteTexto by rememberSaveable { mutableStateOf("") }
    val ingredientes = remember { mutableStateListOf<String>() }

    val dataValida = quando.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(quando.trim())

    AlertDialog(
        onDismissRequest = aoFechar,
        containerColor = CartaoTopo,
        title = {
            Text(
                text = "Adicionar ao Menu",
                style = TextStyle(fontFamily = FonteSerifaMenu, fontSize = 24.sp, color = Tinta)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("O que vamos fazer?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Categoria",
                    style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Ocre)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIAS_COISINHAS.forEach { (chave, rotulo) ->
                        FilterChip(
                            selected = categoria == chave,
                            onClick = { categoria = chave },
                            label = { Text(rotulo, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = quando,
                    onValueChange = { quando = it },
                    label = { Text("Quando vamos fazer? (opcional)") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    isError = !dataValida,
                    supportingText = { if (!dataValida) Text("Use o formato AAAA-MM-DD") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (dados.viagens.isNotEmpty()) {
                    Text(
                        text = "Vai no roteiro de",
                        style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Ocre)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tripId.isBlank(),
                            onClick = { tripId = "" },
                            label = { Text("Nenhuma", fontSize = 11.sp) }
                        )
                        dados.viagens.forEach { viagem ->
                            FilterChip(
                                selected = tripId == viagem.chave,
                                onClick = { tripId = viagem.chave },
                                label = { Text(viagem.titulo, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (categoria == "receita") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFDF7DF).copy(alpha = 0.75f))
                            .border(1.dp, BordaDourada, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Lista de compras",
                            style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TintaAcento)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ingredienteTexto,
                                onValueChange = { ingredienteTexto = it },
                                placeholder = { Text("ex: 2 ovos", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    val limpo = ingredienteTexto.trim()
                                    if (limpo.isNotEmpty()) {
                                        ingredientes.add(limpo)
                                        ingredienteTexto = ""
                                    }
                                }
                            ) {
                                Text("Adicionar", fontSize = 11.sp)
                            }
                        }
                        ingredientes.forEachIndexed { idx, ing ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("· $ing", fontSize = 12.sp, color = Tinta)
                                TextButton(onClick = { ingredientes.removeAt(idx) }) {
                                    Text("Remover", fontSize = 11.sp, color = PerigoCor)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas e detalhes (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = titulo.isNotBlank() && dataValida,
                onClick = {
                    val tituloLimpo = titulo.trim()
                    val payload = mutableMapOf<String, Any>(
                        "category" to categoria,
                        "title" to tituloLimpo,
                        "targetDate" to quando.trim(),
                        "notes" to notas.trim(),
                        "done" to false
                    )
                    if (tripId.isNotBlank()) {
                        payload["tripId"] = tripId
                    }
                    if (categoria == "receita" && ingredientes.isNotEmpty()) {
                        payload["shoppingList"] = ingredientes.map { mapOf("text" to it, "done" to false) }
                    }
                    dados.criarItem("coisinhas", payload, tituloLimpo) {
                        aoCriado?.invoke(tituloLimpo)
                    }
                    aoFechar()
                }
            ) {
                Text("Adicionar Experiência", color = TintaAcento, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar", color = TintaSuave) }
        }
    )
}

@Composable
private fun DialogoEditarCoisinha(
    dados: DadosDoCasal,
    item: Item,
    extra: ExtraCoisinha?,
    aoFechar: () -> Unit
) {
    var titulo by rememberSaveable(item.chave) { mutableStateOf(item.titulo) }
    var categoria by rememberSaveable(item.chave) { mutableStateOf(item.categoria.ifBlank { "outro" }) }
    var quando by rememberSaveable(item.chave) { mutableStateOf(item.dataIso) }
    var tripId by rememberSaveable(item.chave) { mutableStateOf(extra?.tripId.orEmpty()) }
    var notas by rememberSaveable(item.chave) { mutableStateOf(item.notas) }
    var ingredienteTexto by rememberSaveable(item.chave) { mutableStateOf("") }
    val ingredientes = remember(item.chave) {
        mutableStateListOf<IngredienteCompra>().apply {
            addAll(extra?.shoppingList.orEmpty())
        }
    }

    val dataValida = quando.isBlank() || Regex("\\d{4}-\\d{2}-\\d{2}").matches(quando.trim())

    AlertDialog(
        onDismissRequest = aoFechar,
        containerColor = CartaoTopo,
        title = {
            Text(
                text = "Editar Coisinha",
                style = TextStyle(fontFamily = FonteSerifaMenu, fontSize = 24.sp, color = Tinta)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("O que vamos fazer?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CATEGORIAS_COISINHAS.forEach { (chave, rotulo) ->
                        FilterChip(
                            selected = categoria == chave,
                            onClick = { categoria = chave },
                            label = { Text(rotulo, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = quando,
                    onValueChange = { quando = it },
                    label = { Text("Data planejada") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    isError = !dataValida,
                    supportingText = { if (!dataValida) Text("Use o formato AAAA-MM-DD") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (dados.viagens.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = tripId.isBlank(),
                            onClick = { tripId = "" },
                            label = { Text("Sem roteiro", fontSize = 11.sp) }
                        )
                        dados.viagens.forEach { viagem ->
                            FilterChip(
                                selected = tripId == viagem.chave,
                                onClick = { tripId = viagem.chave },
                                label = { Text(viagem.titulo, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (categoria == "receita") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFDF7DF).copy(alpha = 0.75f))
                            .border(1.dp, BordaDourada, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Lista de compras",
                            style = TextStyle(fontFamily = Sem, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TintaAcento)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ingredienteTexto,
                                onValueChange = { ingredienteTexto = it },
                                placeholder = { Text("ex: 2 ovos", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    val limpo = ingredienteTexto.trim()
                                    if (limpo.isNotEmpty()) {
                                        ingredientes.add(IngredienteCompra(limpo, false))
                                        ingredienteTexto = ""
                                    }
                                }
                            ) {
                                Text("Adicionar", fontSize = 11.sp)
                            }
                        }
                        ingredientes.forEachIndexed { idx, ing ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("· ${ing.texto}", fontSize = 12.sp, color = Tinta)
                                TextButton(onClick = { ingredientes.removeAt(idx) }) {
                                    Text("Remover", fontSize = 11.sp, color = PerigoCor)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas e detalhes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = titulo.isNotBlank() && dataValida,
                onClick = {
                    val casalId = dados.casalId
                    if (!casalId.isNullOrBlank()) {
                        val updates = mutableMapOf<String, Any?>(
                            "title" to titulo.trim(),
                            "category" to categoria,
                            "targetDate" to quando.trim(),
                            "notes" to notas.trim(),
                            "tripId" to tripId.ifBlank { null },
                            "shoppingList" to if (categoria == "receita" && ingredientes.isNotEmpty()) {
                                ingredientes.map { mapOf("text" to it.texto, "done" to it.feito) }
                            } else {
                                null
                            }
                        )
                        FirebaseDatabase.getInstance()
                            .getReference("casais/$casalId/coisinhas/${item.chave}")
                            .updateChildren(updates)
                    }
                    aoFechar()
                }
            ) {
                Text("Salvar Alterações", color = TintaAcento, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar", color = TintaSuave) }
        }
    )
}