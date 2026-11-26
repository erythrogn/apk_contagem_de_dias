package com.tamojunto.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Um item de filme, série, jogo ou coisinha, lido do Realtime Database (mesmos campos do site).
data class Item(
    val chave: String,
    val titulo: String,
    val ano: String,
    val genero: String,
    val quando: String,
    val quem: String,
    val feito: Boolean,
    val status: String,
    val categoria: String,
    val notas: String,
    val dataIso: String = ""
) {
    companion object {
        fun de(chave: String, s: DataSnapshot, secao: String): Item {
            fun texto(campo: String): String = s.child(campo).value?.toString() ?: ""
            val feito = when (secao) {
                // Mesma regra do site (filmes/comum.js)
                "filmes" -> s.child("watched").value.let { it == true || it == "true" }
                "series" -> texto("status") == "concluida"
                "jogos" -> texto("status") == "zerado"
                "viagens" -> texto("status") == "feita"
                else -> s.child("done").value == true
            }
            val dataIso = texto("targetDate").ifBlank { texto("date") }
            return Item(
                chave = chave,
                titulo = texto("title").ifBlank { texto("destiny").ifBlank { texto("dest") } },
                ano = texto("year"),
                genero = texto("genre"),
                quando = dataBr(dataIso),
                dataIso = dataIso,
                quem = texto("who"),
                feito = feito,
                status = texto("status"),
                categoria = texto("category"),
                notas = texto("notes")
            )
        }
    }
}

// Uma nota do Bloco de Notas. `autor` é o slot da pessoa: "pessoa1" ou "pessoa2".
data class Nota(
    val chave: String,
    val titulo: String,
    val tipo: String,
    val autor: String,
    val conteudo: String,
    val resposta: String,
    val criadoEm: Long,
    val data: String
) {
    companion object {
        fun de(chave: String, s: DataSnapshot): Nota {
            fun texto(campo: String): String = s.child(campo).value?.toString() ?: ""
            val criado = s.child("createdAt").getValue(Long::class.java)
                ?: s.child("timestamp").getValue(Long::class.java)
                ?: 0L
            return Nota(
                chave = chave,
                titulo = texto("title"),
                tipo = texto("type").ifBlank { "texto" },
                autor = texto("author"),
                conteudo = texto("content"),
                resposta = texto("answer"),
                criadoEm = criado,
                data = texto("createdDate")
            )
        }
    }
}

// "2024-05-01" -> "01/05/2024". Se não for uma data ISO, devolve o texto original.
fun dataBr(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
} catch (e: Exception) {
    iso
}

data class Humor(val id: String, val em: Long)
data class Localizacao(val lat: Double, val lng: Double, val ativo: Boolean, val em: Long)
data class Cartinha(val chave: String, val de: String, val para: String, val texto: String, val em: Long)

// Lê o casal do usuário e as listas do casal em tempo real.
class DadosDoCasal(private val uid: String) {
    private val banco = FirebaseDatabase.getInstance()

    var carregando by mutableStateOf(true)
        private set
    var casalId by mutableStateOf<String?>(null)
        private set
    var dataNamoro by mutableStateOf<String?>(null)
        private set
    // Slot desta pessoa no casal ("pessoa1" ou "pessoa2"), usado como autor das notas.
    var meuSlot by mutableStateOf<String?>(null)
        private set

    val filmes: SnapshotStateList<Item> = mutableStateListOf()
    val series: SnapshotStateList<Item> = mutableStateListOf()
    val jogos: SnapshotStateList<Item> = mutableStateListOf()
    val coisinhas: SnapshotStateList<Item> = mutableStateListOf()
    val notas: SnapshotStateList<Nota> = mutableStateListOf()
    val viagens: SnapshotStateList<Item> = mutableStateListOf()
    val planos: SnapshotStateList<Item> = mutableStateListOf()
    val humores = mutableStateMapOf<String, Humor>()
    val localizacoes = mutableStateMapOf<String, Localizacao>()
    val cartinhas: SnapshotStateList<Cartinha> = mutableStateListOf()

    private var removerUsuario: (() -> Unit)? = null
    private val removeresCasal = mutableListOf<() -> Unit>()

    fun iniciar() {
        removerUsuario = ouvir("usuarios/$uid") { s ->
            val id = s.child("casal_id").getValue(String::class.java)
            carregando = false
            if (id != casalId) trocarCasal(id)
        }
    }

    fun parar() {
        removerUsuario?.invoke()
        removeresCasal.forEach { it() }
        removeresCasal.clear()
    }

    // Marca ou desmarca um item. Cada secao tem o seu campo, como no site.
    fun alternarFeito(secao: String, item: Item, aoErro: (Exception) -> Unit) {
        val campos: Map<String, Any?> = when (secao) {
            "filmes" -> mapOf("watched" to !item.feito)
            "series" -> mapOf("status" to if (item.feito) "quero ver" else "concluida")
            "jogos" -> mapOf("status" to if (item.feito) "quero jogar" else "zerado")
            else -> mapOf("done" to !item.feito)
        }
        atualizarItem(secao, item.chave, campos, item.titulo, aoErro)
    }

    fun criarItem(colecao: String, campos: Map<String, Any?>, resumo: String, aoErro: (Exception) -> Unit): Boolean {
        val id = casalId ?: return false
        val usuario = FirebaseAuth.getInstance().currentUser ?: return false
        return Registro.criar(id, usuario, colecao, campos, resumo, aoErro) != null
    }

    fun atualizarItem(colecao: String, chave: String, campos: Map<String, Any?>, resumo: String, aoErro: (Exception) -> Unit) {
        val id = casalId ?: return
        val usuario = FirebaseAuth.getInstance().currentUser ?: return
        Registro.atualizar(id, usuario, colecao, chave, campos, resumo, aoErro)
    }

    fun apagarItem(colecao: String, chave: String, resumo: String, aoErro: (Exception) -> Unit) {
        val id = casalId ?: return
        val usuario = FirebaseAuth.getInstance().currentUser ?: return
        Registro.apagar(id, usuario, colecao, chave, resumo, aoErro)
    }

    // Grava uma nota do tipo texto ou lista. Devolve null se ainda não dá para salvar (sem casal ou slot).
    fun criarNota(titulo: String, conteudo: String, tipo: String): String? {
        val id = casalId ?: return null
        val usuario = FirebaseAuth.getInstance().currentUser ?: return null
        val slot = meuSlot ?: return null
        return Registro.criar(
            id,
            usuario,
            "bloco",
            mapOf(
                "title" to titulo,
                "type" to tipo,
                "author" to slot,
                "content" to conteudo,
                "answer" to "",
                "createdDate" to LocalDate.now(ZoneId.of("America/Belem")).toString()
            ),
            titulo.ifBlank { "Nota" }
        ) {}
    }

    // Humor, localização e cartinhas: gravados direto, como o site faz.
    fun definirHumor(id: String) {
        val casal = casalId ?: return
        val slot = meuSlot ?: return
        banco.getReference("casais/$casal/humor/$slot")
            .setValue(mapOf("id" to id, "em" to ServerValue.TIMESTAMP))
    }

    fun atualizarLocalizacao(lat: Double, lng: Double) {
        val casal = casalId ?: return
        val slot = meuSlot ?: return
        banco.getReference("casais/$casal/localizacoes/$slot")
            .setValue(mapOf("lat" to lat, "lng" to lng, "ativo" to true, "em" to ServerValue.TIMESTAMP))
    }

    fun enviarCartinha(texto: String) {
        val casal = casalId ?: return
        val de = meuSlot ?: return
        val para = if (de == "pessoa1") "pessoa2" else "pessoa1"
        banco.getReference("casais/$casal/cartinhas").push()
            .setValue(mapOf("de" to de, "para" to para, "texto" to texto, "em" to ServerValue.TIMESTAMP))
    }

    // Desliga o compartilhamento: o site guarda ativo = false e a última posição.
    fun pararLocalizacao() {
        val casal = casalId ?: return
        val slot = meuSlot ?: return
        banco.getReference("casais/$casal/localizacoes/$slot/ativo").setValue(false)
    }

    // Quem lê a cartinha a apaga, como no site.
    fun lerCartinha(chave: String) {
        val casal = casalId ?: return
        banco.getReference("casais/$casal/cartinhas/$chave").removeValue()
    }

    private fun trocarCasal(id: String?) {
        removeresCasal.forEach { it() }
        removeresCasal.clear()
        casalId = id
        filmes.clear(); series.clear(); jogos.clear(); coisinhas.clear(); notas.clear()
        viagens.clear(); planos.clear(); humores.clear(); localizacoes.clear(); cartinhas.clear()
        dataNamoro = null
        meuSlot = null
        if (id == null) return

        removeresCasal += ouvir("casais/$id/data_namoro") { s ->
            dataNamoro = s.getValue(String::class.java)
        }
        removeresCasal += ouvir("casais/$id/pessoas") { s ->
            meuSlot = s.child(uid).getValue(String::class.java)
        }
        removeresCasal += ouvirLista("casais/$id/filmes", filmes, "filmes")
        removeresCasal += ouvirLista("casais/$id/series", series, "series")
        removeresCasal += ouvirLista("casais/$id/jogos", jogos, "jogos")
        removeresCasal += ouvirLista("casais/$id/coisinhas", coisinhas, "coisinhas")
        removeresCasal += ouvirLista("casais/$id/viagens", viagens, "viagens")
        removeresCasal += ouvirLista("casais/$id/planos", planos, "planos")
        removeresCasal += ouvir("casais/$id/humor") { s ->
            humores.clear()
            s.children.forEach { f ->
                humores[f.key ?: ""] = Humor(
                    f.child("id").getValue(String::class.java) ?: "",
                    f.child("em").getValue(Long::class.java) ?: 0L
                )
            }
        }
        removeresCasal += ouvir("casais/$id/localizacoes") { s ->
            localizacoes.clear()
            s.children.forEach { f ->
                localizacoes[f.key ?: ""] = Localizacao(
                    f.child("lat").getValue(Double::class.java) ?: 0.0,
                    f.child("lng").getValue(Double::class.java) ?: 0.0,
                    f.child("ativo").getValue(Boolean::class.java) ?: false,
                    f.child("em").getValue(Long::class.java) ?: 0L
                )
            }
        }
        removeresCasal += ouvir("casais/$id/cartinhas") { s ->
            cartinhas.clear()
            s.children.forEach { f ->
                cartinhas.add(
                    Cartinha(
                        f.key ?: "",
                        f.child("de").getValue(String::class.java) ?: "",
                        f.child("para").getValue(String::class.java) ?: "",
                        f.child("texto").getValue(String::class.java) ?: "",
                        f.child("em").getValue(Long::class.java) ?: 0L
                    )
                )
            }
            cartinhas.sortByDescending { it.em }
        }
        removeresCasal += ouvir("casais/$id/bloco") { s ->
            notas.clear()
            s.children.forEach { filho -> notas.add(Nota.de(filho.key ?: "", filho)) }
            notas.sortByDescending { it.criadoEm }
        }
    }

    private fun ouvirLista(caminho: String, destino: SnapshotStateList<Item>, secao: String): () -> Unit =
        ouvir(caminho) { s ->
            destino.clear()
            s.children.forEach { filho -> destino.add(Item.de(filho.key ?: "", filho, secao)) }
        }

    private fun ouvir(caminho: String, aoMudar: (DataSnapshot) -> Unit): () -> Unit {
        val ref = banco.getReference(caminho)
        val ouvinte = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) = aoMudar(s)
            override fun onCancelled(erro: DatabaseError) {}
        }
        ref.addValueEventListener(ouvinte)
        return { ref.removeEventListener(ouvinte) }
    }
}
