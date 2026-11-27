package com.tamojunto.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.min

// Capa e ficha de um filme, série ou jogo, das mesmas fontes do site: TMDB (filmes e séries) e Steam (jogos).
data class FichaMidia(
    val poster: String,
    val diretor: String,
    val elenco: String,
    val trailer: String,
    val dataEstreia: String
)

object BuscaMidia {
    // Chave pública usada também pelo site (static/js/filmes/dados.js).
    private const val TMDB_KEY = "d39bbf74a96f6cca570dcb69c8f3059f"

    private val cliente = OkHttpClient.Builder()
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    // Null quando a rede falha ou a fonte não responde: a tela segue sem capa, sem travar.
    private fun pegar(url: String): JSONObject? = try {
        cliente.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) null else JSONObject(r.body?.string().orEmpty())
        }
    } catch (e: Exception) {
        null
    }

    private fun codificar(texto: String): String = URLEncoder.encode(texto, "UTF-8")

    // Filme (serie = false) ou série (serie = true). Usa o primeiro resultado, como o site.
    suspend fun filmeOuSerie(serie: Boolean, titulo: String, ano: String): FichaMidia? = withContext(Dispatchers.IO) {
        if (titulo.isBlank()) return@withContext null
        val tipo = if (serie) "tv" else "movie"
        val filtroAno = when {
            ano.isBlank() -> ""
            serie -> "&first_air_date_year=${codificar(ano)}"
            else -> "&primary_release_year=${codificar(ano)}"
        }
        val busca = "https://api.themoviedb.org/3/search/$tipo?api_key=$TMDB_KEY&query=${codificar(titulo)}$filtroAno"
        val primeiro = pegar("$busca&language=pt-BR")?.optJSONArray("results")?.optJSONObject(0)
            ?: pegar("$busca&language=en-US")?.optJSONArray("results")?.optJSONObject(0)
            ?: return@withContext null

        val caminhoPoster = primeiro.optString("poster_path")
        val poster = if (caminhoPoster.isBlank() || caminhoPoster == "null") "" else "https://image.tmdb.org/t/p/w500$caminhoPoster"
        val detalhes = pegar(
            "https://api.themoviedb.org/3/$tipo/${primeiro.optInt("id")}?api_key=$TMDB_KEY&language=pt-BR&append_to_response=credits,videos"
        )
        val credito = detalhes?.optJSONObject("credits")
        val diretor = if (serie) {
            nomes(detalhes?.optJSONArray("created_by"), 2)
        } else {
            diretores(credito?.optJSONArray("crew"))
        }
        FichaMidia(
            poster = poster,
            diretor = diretor,
            elenco = nomes(credito?.optJSONArray("cast"), 4),
            trailer = trailerDe(detalhes?.optJSONObject("videos")),
            dataEstreia = detalhes?.optString(if (serie) "first_air_date" else "release_date").orEmpty()
        )
    }

    // Jogo: a capa vem da loja da Steam, como no site.
    suspend fun jogo(titulo: String): FichaMidia? = withContext(Dispatchers.IO) {
        if (titulo.isBlank()) return@withContext null
        val busca = "https://store.steampowered.com/api/storesearch/?term=${codificar(titulo)}&l=portuguese&cc=BR"
        val item = pegar(busca)?.optJSONArray("items")?.optJSONObject(0) ?: return@withContext null
        FichaMidia(
            poster = "https://cdn.cloudflare.steamstatic.com/steam/apps/${item.optInt("id")}/library_600x900_2x.jpg",
            diretor = "",
            elenco = "",
            trailer = "",
            dataEstreia = ""
        )
    }

    private fun nomes(lista: JSONArray?, limite: Int): String {
        if (lista == null) return ""
        return (0 until min(lista.length(), limite))
            .mapNotNull { lista.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }
            .joinToString(", ")
    }

    private fun diretores(equipe: JSONArray?): String {
        if (equipe == null) return ""
        return (0 until equipe.length())
            .mapNotNull { equipe.optJSONObject(it) }
            .filter { it.optString("job") == "Director" }
            .mapNotNull { it.optString("name").takeIf { n -> n.isNotBlank() } }
            .take(2)
            .joinToString(", ")
    }

    // Trailer oficial no YouTube; sem trailer, o teaser.
    private fun trailerDe(videos: JSONObject?): String {
        val lista = videos?.optJSONArray("results") ?: return ""
        val youtube = (0 until lista.length())
            .mapNotNull { lista.optJSONObject(it) }
            .filter { it.optString("site") == "YouTube" && it.optString("key").isNotBlank() }
        val trailers = youtube.filter { it.optString("type") == "Trailer" }
        val escolhido = trailers.firstOrNull { it.optBoolean("official") }
            ?: trailers.firstOrNull()
            ?: youtube.firstOrNull { it.optString("type") == "Teaser" }
        return escolhido?.let { "https://www.youtube.com/watch?v=${it.optString("key")}" }.orEmpty()
    }
}
