package com.tamojunto.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
    val notas: String
) {
    companion object {
        fun de(chave: String, s: DataSnapshot, secao: String): Item {
            fun texto(campo: String): String = s.child(campo).value?.toString() ?: ""
            val feito = when (secao) {
                // Mesma regra do site (filmes/comum.js)
                "filmes" -> s.child("watched").value.let { it == true || it == "true" }
                "series" -> texto("status") == "concluida"
                "jogos" -> texto("status") == "zerado"
                else -> s.child("done").value == true
            }
            return Item(
                chave = chave,
                titulo = texto("title"),
                ano = texto("year"),
                genero = texto("genre"),
                quando = dataBr(texto("targetDate")),
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

    fun alternarCoisinha(item: Item) {
        val id = casalId ?: return
        banco.getReference("casais/$id/coisinhas/${item.chave}/done").setValue(!item.feito)
    }

    // Grava uma nota do tipo texto ou lista. Devolve null se ainda não dá para salvar (sem casal ou slot).
    fun criarNota(titulo: String, conteudo: String, tipo: String): String? {
        val id = casalId ?: return null
        val slot = meuSlot ?: return null
        val ref = banco.getReference("casais/$id/bloco").push()
        ref.setValue(
            mapOf(
                "title" to titulo,
                "type" to tipo,
                "author" to slot,
                "content" to conteudo,
                "answer" to "",
                "createdDate" to LocalDate.now(ZoneId.of("America/Belem")).toString(),
                "createdBy" to (FirebaseAuth.getInstance().currentUser?.email ?: ""),
                "createdByUid" to uid,
                "createdAt" to ServerValue.TIMESTAMP
            )
        )
        return ref.key
    }

    private fun trocarCasal(id: String?) {
        removeresCasal.forEach { it() }
        removeresCasal.clear()
        casalId = id
        filmes.clear(); series.clear(); jogos.clear(); coisinhas.clear(); notas.clear()
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
