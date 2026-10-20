package com.cinesakura.app

import android.app.Application
import com.cinesakura.app.domain.repository.MovieRepository
import com.cinesakura.app.domain.repository.WatchlistRepository

class CineSakuraApp : Application() {

    lateinit var movieRepository: MovieRepository
        private set

    lateinit var watchlistRepository: WatchlistRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        movieRepository = MovieRepository()
        watchlistRepository = WatchlistRepository(this)
    }

    companion object {
        lateinit var instance: CineSakuraApp
            private set
    }
}
