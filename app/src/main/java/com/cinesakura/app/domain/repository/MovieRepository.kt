package com.cinesakura.app.domain.repository

import com.cinesakura.app.domain.model.Episode
import com.cinesakura.app.domain.model.MediaDetail
import com.cinesakura.app.domain.model.MediaItem
import com.cinesakura.app.domain.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MovieRepository {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    suspend fun getTrendingMovies(): List<MediaItem> = withContext(Dispatchers.IO) {
        // Try Cinemeta direct catalogs
        val urls = listOf(
            "https://cinemeta-catalogs.strem.io/top/catalog/movie/top.json",
            "https://v3-cinemeta.strem.io/catalog/movie/top.json"
        )

        for (url in urls) {
            val list = fetchCinemetaCatalog(url, "movie")
            if (list.isNotEmpty()) return@withContext list
        }

        // Resilient fallback: search top blockbuster movies via IMDb suggestions
        val fallbackQueries = listOf("Avengers", "Deadpool", "Oppenheimer", "Spider-Man", "Batman", "Avatar")
        fetchFromImdbQueries(fallbackQueries, "movie")
    }

    suspend fun getPopularSeries(): List<MediaItem> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://cinemeta-catalogs.strem.io/top/catalog/series/top.json",
            "https://v3-cinemeta.strem.io/catalog/series/top.json"
        )

        for (url in urls) {
            val list = fetchCinemetaCatalog(url, "series")
            if (list.isNotEmpty()) return@withContext list
        }

        // Resilient fallback: top binge series via IMDb suggestions
        val fallbackQueries = listOf("Stranger Things", "Wednesday", "Squid Game", "Breaking Bad", "The Boys", "Naruto")
        fetchFromImdbQueries(fallbackQueries, "series")
    }

    suspend fun searchMedia(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext emptyList()

        // 1. Primary High-Speed Search: Official IMDb Suggestions API
        val imdbResults = searchImdbSuggestions(clean)
        if (imdbResults.isNotEmpty()) {
            return@withContext imdbResults
        }

        // 2. Secondary Search: Cinemeta catalog search
        val encoded = URLEncoder.encode(clean, "UTF-8")
        val movieUrl = "https://v3-cinemeta.strem.io/catalog/movie/top/search=$encoded.json"
        val seriesUrl = "https://v3-cinemeta.strem.io/catalog/series/top/search=$encoded.json"

        val movies = fetchCinemetaCatalog(movieUrl, "movie")
        val series = fetchCinemetaCatalog(seriesUrl, "series")
        (movies + series).distinctBy { it.imdbId }
    }

    private fun searchImdbSuggestions(query: String): List<MediaItem> {
        return try {
            val firstChar = query.firstOrNull()?.lowercaseChar() ?: 'a'
            val encodedQuery = URLEncoder.encode(query.lowercase().replace(" ", "_"), "UTF-8")
            val url = "https://v3.sg.media-imdb.com/suggestion/$firstChar/$encodedQuery.json"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val dataArr = json.optJSONArray("d") ?: return emptyList()

                val items = mutableListOf<MediaItem>()
                for (i in 0 until dataArr.length()) {
                    val obj = dataArr.getJSONObject(i)
                    val id = obj.optString("id", "")
                    if (!id.startsWith("tt")) continue

                    val title = obj.optString("l", "Unknown Title")
                    val q = obj.optString("q", "").lowercase()
                    val isTv = q.contains("series") || q.contains("tv")
                    val type = if (isTv) "series" else "movie"

                    val imgObj = obj.optJSONObject("i")
                    val poster = imgObj?.optString("imageUrl", "") ?: ""
                    val year = obj.optString("yr", obj.optString("y", ""))
                    val actors = obj.optString("s", "")
                    val desc = if (actors.isNotEmpty()) "Starring: $actors" else ""

                    items.add(
                        MediaItem(
                            id = id,
                            imdbId = id,
                            title = title,
                            type = type,
                            poster = poster,
                            banner = poster,
                            releaseYear = year,
                            description = desc,
                            rating = "8.0",
                            genres = if (isTv) listOf("TV Series") else listOf("Movie")
                        )
                    )
                }
                items
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchFromImdbQueries(queries: List<String>, filterType: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val results = queries.map { q ->
            async { searchImdbSuggestions(q) }
        }.awaitAll().flatten()

        results.filter {
            if (filterType == "series") it.type == "series" else it.type == "movie"
        }.distinctBy { it.imdbId }
    }

    private fun fetchCinemetaCatalog(url: String, defaultType: String): List<MediaItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val metas = json.optJSONArray("metas") ?: return emptyList()

                val items = mutableListOf<MediaItem>()
                for (i in 0 until metas.length()) {
                    val m = metas.getJSONObject(i)
                    val id = m.optString("id", "")
                    val imdbId = m.optString("imdb_id", id)
                    if (imdbId.isEmpty() || !imdbId.startsWith("tt")) continue

                    val title = m.optString("name", "Unknown Title")
                    var poster = m.optString("poster", "")
                    // Fix metahub.space redirect by pointing directly to live.metahub.space
                    if (poster.contains("images.metahub.space")) {
                        poster = poster.replace("images.metahub.space", "live.metahub.space")
                    }

                    var banner = m.optString("background", poster)
                    if (banner.contains("images.metahub.space")) {
                        banner = banner.replace("images.metahub.space", "live.metahub.space")
                    }

                    val year = m.optString("releaseInfo", m.optString("year", ""))
                    val desc = m.optString("description", "")
                    val type = m.optString("type", defaultType)

                    val genresList = mutableListOf<String>()
                    val genreArr = m.optJSONArray("genres") ?: m.optJSONArray("genre")
                    if (genreArr != null) {
                        for (g in 0 until genreArr.length()) {
                            genresList.add(genreArr.getString(g))
                        }
                    }

                    items.add(
                        MediaItem(
                            id = id,
                            imdbId = imdbId,
                            title = title,
                            type = type,
                            poster = poster,
                            banner = banner,
                            releaseYear = year,
                            description = desc,
                            genres = genresList
                        )
                    )
                }
                items
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getMediaDetails(id: String, type: String): MediaDetail = withContext(Dispatchers.IO) {
        val cleanType = if (type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true)) "series" else "movie"
        val url = "https://v3-cinemeta.strem.io/meta/$cleanType/$id.json"

        var title = "Unknown Title"
        var poster = ""
        var banner = ""
        var year = ""
        var rating = "8.2"
        var description = ""
        val genresList = mutableListOf<String>()
        val castList = mutableListOf<String>()
        val directorList = mutableListOf<String>()
        val episodesList = mutableListOf<Episode>()
        var seasonsCount = 1

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isNotEmpty()) {
                        val json = JSONObject(body)
                        val meta = json.optJSONObject("meta")
                        if (meta != null) {
                            title = meta.optString("name", title)
                            poster = meta.optString("poster", "")
                            banner = meta.optString("background", poster)
                            year = meta.optString("releaseInfo", meta.optString("year", ""))
                            rating = meta.optString("imdbRating", "8.2")
                            description = meta.optString("description", "")

                            val gArr = meta.optJSONArray("genre") ?: meta.optJSONArray("genres")
                            if (gArr != null) {
                                for (i in 0 until gArr.length()) genresList.add(gArr.getString(i))
                            }

                            val cArr = meta.optJSONArray("cast")
                            if (cArr != null) {
                                for (i in 0 until cArr.length()) castList.add(cArr.getString(i))
                            }

                            val dArr = meta.optJSONArray("director")
                            if (dArr != null) {
                                for (i in 0 until dArr.length()) directorList.add(dArr.getString(i))
                            }

                            val videosArr = meta.optJSONArray("videos")
                            if (videosArr != null && cleanType == "series") {
                                for (i in 0 until videosArr.length()) {
                                    val epObj = videosArr.getJSONObject(i)
                                    val sNum = epObj.optInt("season", 1)
                                    val eNum = epObj.optInt("number", epObj.optInt("episode", 1))
                                    if (sNum > seasonsCount) seasonsCount = sNum

                                    episodesList.add(
                                        Episode(
                                            id = epObj.optString("id", "$id:$sNum:$eNum"),
                                            title = epObj.optString("name", epObj.optString("title", "Episode $eNum")),
                                            seasonNumber = sNum,
                                            episodeNumber = eNum,
                                            overview = epObj.optString("overview", epObj.optString("description", "")),
                                            thumbnail = epObj.optString("thumbnail", null)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback for poster & title from IMDb suggestion if missing or empty
        if (poster.isEmpty() || title == "Unknown Title") {
            val imdbItem = getImdbItemDirect(id)
            if (imdbItem != null) {
                if (title == "Unknown Title") title = imdbItem.title
                if (poster.isEmpty()) poster = imdbItem.poster
                if (banner.isEmpty()) banner = imdbItem.poster
                if (year.isEmpty()) year = imdbItem.releaseYear
                if (description.isEmpty()) description = imdbItem.description
            }
        }

        // If it's a TV series with no episodes listed, provide seasons 1-3 with 12 episodes each
        if (cleanType == "series" && episodesList.isEmpty()) {
            seasonsCount = 3
            for (s in 1..3) {
                for (e in 1..12) {
                    episodesList.add(
                        Episode(
                            id = "$id:$s:$e",
                            title = "Episode $e",
                            seasonNumber = s,
                            episodeNumber = e,
                            overview = "Stream Season $s Episode $e of $title in high definition."
                        )
                    )
                }
            }
        }

        MediaDetail(
            id = id,
            imdbId = id,
            title = title,
            type = cleanType,
            poster = poster,
            banner = banner.ifEmpty { poster },
            releaseYear = year,
            rating = rating,
            description = description.ifEmpty { "Enjoy streaming $title in high definition on Cine-Sakura." },
            genres = genresList.ifEmpty { listOf("Entertainment", if (cleanType == "series") "TV Series" else "Movie") },
            cast = castList,
            directors = directorList,
            runtime = "",
            trailerUrl = null,
            episodes = episodesList,
            seasonsCount = seasonsCount
        )
    }

    private fun getImdbItemDirect(imdbId: String): MediaItem? {
        return try {
            val url = "https://v3.sg.media-imdb.com/suggestion/t/$imdbId.json"
            val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val dataArr = json.optJSONArray("d") ?: return null
                if (dataArr.length() == 0) return null
                val obj = dataArr.getJSONObject(0)
                val title = obj.optString("l", "Unknown Title")
                val imgObj = obj.optJSONObject("i")
                val poster = imgObj?.optString("imageUrl", "") ?: ""
                val year = obj.optString("yr", obj.optString("y", ""))
                val s = obj.optString("s", "")
                MediaItem(
                    id = imdbId,
                    imdbId = imdbId,
                    title = title,
                    type = "movie",
                    poster = poster,
                    banner = poster,
                    releaseYear = year,
                    description = if (s.isNotEmpty()) "Starring: $s" else ""
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getStreamServers(imdbId: String, isTv: Boolean, season: Int = 1, episode: Int = 1): List<StreamServer> {
        val s = season
        val e = episode

        return if (isTv) {
            listOf(
                StreamServer(
                    name = "VidLink Pro (Multi-Audio & Subtitles)",
                    serverKey = "vidlink",
                    embedUrl = "https://vidlink.pro/tv/$imdbId/$s/$e",
                    badge = "Ultra HD 4K"
                ),
                StreamServer(
                    name = "VidSrc Prime (Fast Load)",
                    serverKey = "vidsrc",
                    embedUrl = "https://vidsrc.me/embed/tv?imdb=$imdbId&season=$s&episode=$e",
                    badge = "Full HD"
                ),
                StreamServer(
                    name = "2Embed TV (Reliable)",
                    serverKey = "2embed",
                    embedUrl = "https://www.2embed.cc/embedtv/$imdbId&s=$s&e=$e",
                    badge = "HD"
                ),
                StreamServer(
                    name = "111Movies (Hindi Dubbed)",
                    serverKey = "111movies",
                    embedUrl = "https://111movies.net/tv/$imdbId/$s/$e",
                    badge = "Hindi Dub",
                    isHindiDubAvailable = true
                ),
                StreamServer(
                    name = "VidSrc PM",
                    serverKey = "vidsrc_pm",
                    embedUrl = "https://vidsrc.pm/embed/tv?imdb=$imdbId&season=$s&episode=$e",
                    badge = "1080p"
                ),
                StreamServer(
                    name = "EmbedSU",
                    serverKey = "embedsu",
                    embedUrl = "https://embed.su/embed/tv/$imdbId/$s/$e",
                    badge = "Multi Sub"
                )
            )
        } else {
            listOf(
                StreamServer(
                    name = "VidLink Pro (Multi-Audio & Subtitles)",
                    serverKey = "vidlink",
                    embedUrl = "https://vidlink.pro/movie/$imdbId",
                    badge = "Ultra HD 4K"
                ),
                StreamServer(
                    name = "VidSrc Prime (Fast Load)",
                    serverKey = "vidsrc",
                    embedUrl = "https://vidsrc.me/embed/movie?imdb=$imdbId",
                    badge = "Full HD"
                ),
                StreamServer(
                    name = "2Embed Multi (Reliable)",
                    serverKey = "2embed",
                    embedUrl = "https://www.2embed.cc/embed/$imdbId",
                    badge = "HD"
                ),
                StreamServer(
                    name = "111Movies (Hindi Dubbed)",
                    serverKey = "111movies",
                    embedUrl = "https://111movies.net/movie/$imdbId",
                    badge = "Hindi Dub",
                    isHindiDubAvailable = true
                ),
                StreamServer(
                    name = "VidSrc PM",
                    serverKey = "vidsrc_pm",
                    embedUrl = "https://vidsrc.pm/embed/movie?imdb=$imdbId",
                    badge = "1080p"
                ),
                StreamServer(
                    name = "EmbedSU",
                    serverKey = "embedsu",
                    embedUrl = "https://embed.su/embed/movie/$imdbId",
                    badge = "Multi Sub"
                )
            )
        }
    }
}
