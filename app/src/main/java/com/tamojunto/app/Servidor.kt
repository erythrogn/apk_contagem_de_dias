package com.tamojunto.app

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class RespostaServidor(val ok: Boolean, val msg: String, val corpo: JSONObject?)

// Conversa com as rotas /api do site. O login é o mesmo do site: o token do Firebase vai para
// /api/auth/session e o servidor devolve o cookie de sessão, guardado só na memória deste app.
object Servidor {
    private val cookies = mutableListOf<Cookie>()

    private val guardaCookies = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookieList: List<Cookie>) {
            synchronized(cookies) {
                cookieList.forEach { novo ->
                    cookies.removeAll { it.name == novo.name }
                    cookies.add(novo)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            synchronized(cookies) { cookies.filter { it.matches(url) } }
    }

    private val cliente = OkHttpClient.Builder().cookieJar(guardaCookies).build()
    private val base = BuildConfig.SITE_URL.trimEnd('/')
    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var logado = false

    suspend fun entrar(): Boolean = withContext(Dispatchers.IO) {
        try {
            val usuario = FirebaseAuth.getInstance().currentUser ?: return@withContext false
            val token = usuario.getIdToken(false).await().token ?: return@withContext false
            val corpo = JSONObject().put("idToken", token).toString().toRequestBody(tipoJson)
            val pedido = Request.Builder().url("$base/api/auth/session").post(corpo).build()
            logado = cliente.newCall(pedido).execute().use { it.isSuccessful }
            logado
        } catch (e: IOException) {
            logado = false
            false
        }
    }

    fun encerrar() {
        logado = false
        synchronized(cookies) { cookies.clear() }
    }

    // `repetirSeSessaoExpirou = false` evita nova tentativa em rotas de senha (cada tentativa conta para o bloqueio).
    suspend fun postJson(caminho: String, dados: JSONObject, repetirSeSessaoExpirou: Boolean = true): RespostaServidor =
        enviar(caminho, repetirSeSessaoExpirou) { it.post(dados.toString().toRequestBody(tipoJson)) }

    suspend fun apagar(caminho: String): RespostaServidor = enviar(caminho) { it.delete() }

    suspend fun enviarFoto(caminho: String, bytes: ByteArray, tipo: String): RespostaServidor = enviar(caminho) {
        val corpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("foto", "foto", bytes.toRequestBody(tipo.toMediaType()))
            .build()
        it.post(corpo)
    }

    private suspend fun enviar(
        caminho: String,
        repetir: Boolean = true,
        montar: (Request.Builder) -> Request.Builder
    ): RespostaServidor =
        withContext(Dispatchers.IO) {
            try {
                if (!logado && !entrar()) return@withContext RespostaServidor(false, "Entre de novo para continuar.", null)
                var resultado = executar(caminho, montar)
                if (resultado.first == 401 && repetir) {
                    // A sessão expirou: entra de novo com um token novo e tenta mais uma vez.
                    logado = false
                    if (entrar()) resultado = executar(caminho, montar)
                }
                resultado.second
            } catch (e: IOException) {
                RespostaServidor(false, "Sem conexão com o site. Tente de novo.", null)
            }
        }

    private fun executar(caminho: String, montar: (Request.Builder) -> Request.Builder): Pair<Int, RespostaServidor> {
        val pedido = montar(Request.Builder().url(base + caminho)).build()
        cliente.newCall(pedido).execute().use { r ->
            val json = runCatching { JSONObject(r.body?.string().orEmpty()) }.getOrNull()
            val ok = r.isSuccessful && json?.optBoolean("ok") == true
            val msg = json?.optString("msg").orEmpty().ifBlank {
                if (ok) "Pronto." else "Não consegui concluir. Tente de novo."
            }
            return r.code to RespostaServidor(ok, msg, json)
        }
    }
}
