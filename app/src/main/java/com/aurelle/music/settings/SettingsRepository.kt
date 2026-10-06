package com.aurelle.music.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aurelle.music.download.DownloadQuality
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.ui.library.LibrarySort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "aurelle_settings",
    // As chaves "target" e "quality" são as mesmas do antigo SharedPreferences "aurelle_downloads":
    // a primeira abertura copia a última escolha de download para cá.
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, "aurelle_downloads")) },
)

/**
 * Configurações do app (DataStore Preferences). [state] sempre tem o valor atual, então quem não pode
 * esperar (tema, folha de download) lê [AppSettings] na hora; as mudanças são gravadas em segundo plano.
 */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(
        // Leitura única de um arquivo minúsculo, no começo: evita abrir com o tema errado por um instante.
        runBlocking { store.data.first().toSettings() }
    )
    val state: StateFlow<AppSettings> = _state

    val current: AppSettings get() = _state.value

    init {
        scope.launch { store.data.collect { _state.value = it.toSettings() } }
    }

    /**
     * Aplica [transform] ao valor gravado, de forma atômica (toques rápidos em sequência não se atropelam).
     * O novo valor chega em [state] em poucos milissegundos, pelo próprio DataStore.
     */
    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch { store.edit { it.write(transform(it.toSettings())) } }
    }

    private fun Preferences.toSettings() = AppSettings(
        downloadTarget = enumOf(TARGET, DownloadTarget.DEVICE),
        downloadQuality = enumOf(QUALITY, DownloadQuality.MAX),
        librarySource = enumOf(LIBRARY_SOURCE, DownloadTarget.DEVICE),
        librarySort = enumOf(LIBRARY_SORT, LibrarySort.RECENT),
        style = enumOf(STYLE, AppStyle.CLASSIC),
        icon = enumOf(ICON, AppIcon.CLASSIC),
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.write(s: AppSettings) {
        this[TARGET] = s.downloadTarget.name
        this[QUALITY] = s.downloadQuality.name
        this[LIBRARY_SOURCE] = s.librarySource.name
        this[LIBRARY_SORT] = s.librarySort.name
        this[STYLE] = s.style.name
        this[ICON] = s.icon.name
    }

    private inline fun <reified E : Enum<E>> Preferences.enumOf(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val TARGET = stringPreferencesKey("target")
        val QUALITY = stringPreferencesKey("quality")
        val LIBRARY_SOURCE = stringPreferencesKey("library_source")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val STYLE = stringPreferencesKey("style")
        val ICON = stringPreferencesKey("icon")
    }
}
