package com.aurelle.music.download

import com.aurelle.music.settings.SettingsRepository

/**
 * Destino/qualidade que a folha de download já abre selecionados. Desde a Etapa 6 vive no DataStore das
 * Configurações (a aba Conta edita os mesmos valores); a interface é a mesma de antes.
 */
class DownloadPreferences(private val settings: SettingsRepository) {

    fun load(): DownloadChoice = settings.current.let { DownloadChoice(it.downloadTarget, it.downloadQuality) }

    fun save(choice: DownloadChoice) {
        settings.update { it.copy(downloadTarget = choice.target, downloadQuality = choice.quality) }
    }
}
