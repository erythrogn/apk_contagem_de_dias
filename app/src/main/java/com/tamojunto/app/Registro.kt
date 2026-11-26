package com.tamojunto.app

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

// Escritas no banco com autoria e auditoria, iguais ao registro.js do site.
// As regras do banco exigem o e-mail de quem está logado em createdBy/updatedBy, e cada alteração
// grava uma linha em `auditoria` no mesmo update: o registro e o histórico são salvos juntos.
object Registro {
    private fun banco() = FirebaseDatabase.getInstance()

    private fun entrada(usuario: FirebaseUser, acao: String, caminho: String, resumo: String): Map<String, Any?> =
        mapOf(
            "acao" to acao,
            "caminho" to caminho,
            "resumo" to resumo.take(200),
            "usuario" to (usuario.email ?: ""),
            "uid" to usuario.uid,
            "em" to ServerValue.TIMESTAMP
        )

    private fun chaveAuditoria(casalId: String): String? =
        banco().getReference("casais/$casalId/auditoria").push().key

    // Cria um item na coleção e devolve a chave gerada (null se não der para gerar).
    fun criar(
        casalId: String,
        usuario: FirebaseUser,
        colecao: String,
        campos: Map<String, Any?>,
        resumo: String,
        aoErro: (Exception) -> Unit
    ): String? {
        val chave = banco().getReference("casais/$casalId/$colecao").push().key ?: return null
        val caminho = "$colecao/$chave"
        val log = chaveAuditoria(casalId) ?: return null
        val item = campos + mapOf(
            "createdBy" to (usuario.email ?: ""),
            "createdByUid" to usuario.uid,
            "createdAt" to ServerValue.TIMESTAMP
        )
        banco().getReference("casais/$casalId")
            .updateChildren(mapOf(caminho to item, "auditoria/$log" to entrada(usuario, "criar", caminho, resumo)))
            .addOnFailureListener { aoErro(it) }
        return chave
    }

    // Altera só os campos dados. Campos com barra são caminhos aninhados (ex.: "ratings/uid").
    fun atualizar(
        casalId: String,
        usuario: FirebaseUser,
        colecao: String,
        chave: String,
        campos: Map<String, Any?>,
        resumo: String,
        aoErro: (Exception) -> Unit
    ) {
        val caminho = "$colecao/$chave"
        val log = chaveAuditoria(casalId) ?: return
        val mudancas = mutableMapOf<String, Any?>()
        campos.forEach { (campo, valor) -> mudancas["$caminho/$campo"] = valor }
        mudancas["$caminho/updatedBy"] = usuario.email ?: ""
        mudancas["$caminho/updatedAt"] = ServerValue.TIMESTAMP
        mudancas["auditoria/$log"] = entrada(usuario, "atualizar", caminho, resumo)
        banco().getReference("casais/$casalId").updateChildren(mudancas).addOnFailureListener { aoErro(it) }
    }

    // Apaga um item. A auditoria registra quem apagou.
    fun apagar(
        casalId: String,
        usuario: FirebaseUser,
        colecao: String,
        chave: String,
        resumo: String,
        aoErro: (Exception) -> Unit
    ) {
        val caminho = "$colecao/$chave"
        val log = chaveAuditoria(casalId) ?: return
        banco().getReference("casais/$casalId")
            .updateChildren(mapOf(caminho to null, "auditoria/$log" to entrada(usuario, "apagar", caminho, resumo)))
            .addOnFailureListener { aoErro(it) }
    }
}
