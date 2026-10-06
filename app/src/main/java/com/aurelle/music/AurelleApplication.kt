package com.aurelle.music

import android.app.Application
import com.aurelle.music.data.MusicRepository
import com.aurelle.music.data.youtube.NewPipeDownloader
import com.aurelle.music.data.youtube.NewPipeMusicRepository
import com.aurelle.music.download.DownloadManager
import com.aurelle.music.download.DownloadNotifications
import com.aurelle.music.download.DownloadPreferences
import com.aurelle.music.download.DownloadRepository
import com.aurelle.music.settings.SettingsRepository
import com.aurelle.music.ui.theme.ActivePalette
import com.aurelle.music.ui.theme.Palettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

class AurelleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val locale = Locale.getDefault()
        NewPipe.init(
            NewPipeDownloader.create(),
            Localization.fromLocale(locale),
            ContentCountry(locale.country.ifEmpty { "US" }),
        )
        repository = NewPipeMusicRepository()

        // Etapa 6: configurações; o estilo escolhido vale desde o primeiro quadro e acompanha as mudanças.
        settings = SettingsRepository(this)
        ActivePalette.current = Palettes.of(settings.current.style)
        settings.state.map { it.style }.distinctUntilChanged()
            .onEach { ActivePalette.current = Palettes.of(it) }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Default))

        // Etapa 4: downloads
        DownloadNotifications.ensureChannel(this)
        downloadRepository = DownloadRepository(this)
        downloads = DownloadManager(this, downloadRepository, DownloadPreferences(settings))
    }

    companion object {
        /** Localizador simples de dependências (pode virar Hilt/Koin numa etapa futura). */
        lateinit var repository: MusicRepository
            private set

        /** Configurações do app (DataStore). */
        lateinit var settings: SettingsRepository
            private set

        /** Registro do que foi baixado (banco Room). */
        lateinit var downloadRepository: DownloadRepository
            private set

        /** Fila e estado dos downloads. */
        lateinit var downloads: DownloadManager
            private set
    }
}
