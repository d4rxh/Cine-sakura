package com.cinesakura.app.domain.repository

import com.cinesakura.app.domain.model.Episode
import com.cinesakura.app.domain.model.MediaDetail
import com.cinesakura.app.domain.model.MediaItem
import com.cinesakura.app.domain.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MovieRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cinemetaBase = "https://v3-cinemeta.strem.io"

    suspend fun getTrendingMovies(): List<MediaItem> = withContext(Dispatchers.IO) {
        val url = "$cinemetaBase/catalog/movie/top.json"
        fetchCatalog(url, "movie")
    }

    suspend fun getPopularSeries(): List<MediaItem> = withContext(Dispatchers.IO) {
        val url = "$cinemetaBase/catalog/series/top.json"
        fetchCatalog(url, "series")
    }

    suspend fun searchMedia(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val movieUrl = "$cinemetaBase/catalog/movie/top/search=$encoded.json"
        val seriesUrl = "$cinemetaBase/catalog/series/top/search=$encoded.json"

        val movies = fetchCatalog(movieUrl, "movie")
        val series = fetchCatalog(seriesUrl, "series")
        (movies + series).distinctBy { it.imdbId }
    }

    suspend fun getMediaDetails(id: String, type: String): MediaDetail = withContext(Dispatchers.IO) {
        val cleanType = if (type.equals("tv", ignoreCase = true) || type.equals("series", ignoreCase = true)) "series" else "movie"
        val url = "$cinemetaBase/meta/$cleanType/$id.json"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("API error: ${response.code}")
            val body = response.body?.string() ?: throw IllegalStateException("Empty body")
            val json = JSONObject(body)
            val meta = json.getJSONObject("meta")

            val imdbId = meta.optString("imdb_id", meta.optString("id", id))
            val title = meta.optString("name", "Unknown Title")
            val poster = meta.optString("poster", "")
            val banner = meta.optString("background", poster)
            val year = meta.optString("releaseInfo", meta.optString("year", "N/A"))
            val rating = meta.optString("imdbRating", "8.0")
            val description = meta.optString("description", "No description available.")

            val genresList = mutableListOf<String>()
            val genreArr = meta.optJSONArray("genre")
            if (genreArr != null) {
                for (i in 0 until genreArr.length()) {
                    genresList.add(genreArr.getString(i))
                }
            }

            val castList = mutableListOf<String>()
            val castArr = meta.optJSONArray("cast")
            if (castArr != null) {
                for (i in 0 until castArr.length()) {
                    castList.add(castArr.getString(i))
                }
            }

            val directorList = mutableListOf<String>()
            val dirArr = meta.optJSONArray("director")
            if (dirArr != null) {
                for (i in 0 until dirArr.length()) {
                    directorList.add(dirArr.getString(i))
                }
            }

            val runtime = meta.optString("runtime", "")

            // Episodes for Series
            val episodesList = mutableListOf<Episode>()
            val videosArr = meta.optJSONArray("videos")
            var maxSeason = 1
            if (videosArr != null && cleanType == "series") {
                for (i in 0 until videosArr.length()) {
                    val epObj = videosArr.getJSONObject(i)
                    val sNum = epObj.optInt("season", 1)
                    val eNum = epObj.optInt("number", epObj.optInt("episode", 1))
                    if (sNum > maxSeason) maxSeason = sNum

                    // skip special/extras season 0 if regular episodes exist
                    episodesList.add(
                        Episode(
                            id = epObj.optString("id", "$imdbId:$sNum:$eNum"),
                            title = epObj.optString("name", epObj.optString("title", "Episode $eNum")),
                            seasonNumber = sNum,
                            episodeNumber = eNum,
                            overview = epObj.optString("overview", ""),
                            thumbnail = epObj.optString("thumbnail", null)
                        )
                    )
                }
            }

            MediaDetail(
                id = id,
                imdbId = imdbId,
                title = title,
                type = cleanType,
                poster = poster,
                banner = banner,
                releaseYear = year,
                rating = rating,
                description = description,
                genres = genresList,
                cast = castList,
                directors = directorList,
                runtime = runtime,
                episodes = episodesList,
                seasonsCount = maxSeason.coerceAtLeast(1)
            )
        }
    }

    fun getStreamServers(imdbId: String, isTv: Boolean, season: Int = 1, episode: Int = 1): List<StreamServer> {
        val s = season.toString()
        val e = episode.toString()

        return if (isTv) {
            listOf(
                StreamServer(
                    name = "VidLink Pro (Multi-Audio)",
                    serverKey = "vidlink",
                    embedUrl = "https://vidlink.pro/tv/$imdbId/$s/$e",
                    badge = "Ultra HD 4K"
                ),
                StreamServer(
                    name = "VidSrc Prime (Fast)",
                    serverKey = "vidsrc",
                    embedUrl = "https://vidsrc.me/embed/tv?imdb=$imdbId&season=$s&episode=$e",
                    badge = "Full HD"
                ),
                StreamServer(
                    name = "2Embed Multi",
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
                    name = "VidLink Pro (Multi-Audio)",
                    serverKey = "vidlink",
                    embedUrl = "https://vidlink.pro/movie/$imdbId",
                    badge = "Ultra HD 4K"
                ),
                StreamServer(
                    name = "VidSrc Prime (Fast)",
                    serverKey = "vidsrc",
                    embedUrl = "https://vidsrc.me/embed/movie?imdb=$imdbId",
                    badge = "Full HD"
                ),
                StreamServer(
                    name = "2Embed Multi",
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

    private fun fetchCatalog(url: String, defaultType: String): List<MediaItem> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
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
                    if (imdbId.isEmpty()) continue

                    val title = m.optString("name", "Unknown Title")
                    val poster = m.optString("poster", "")
                    val banner = m.optString("background", poster)
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
}
