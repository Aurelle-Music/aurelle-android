package com.aurelle.music.settings

import com.aurelle.music.download.DownloadQuality
import com.aurelle.music.download.DownloadTarget
import com.aurelle.music.ui.library.LibrarySort

/** Variações da paleta roxo + dourado do Aurelle (o app é sempre escuro). */
enum class AppStyle { CLASSIC, AMETHYST, MIDNIGHT, GRAPHITE }

/** Ícone do app na tela inicial; cada um é um `activity-alias` do manifesto (ver [IconSwitcher]). */
enum class AppIcon(val alias: String) {
    CLASSIC(".IconClassic"),
    AMETHYST(".IconAmethyst"),
    MIDNIGHT(".IconMidnight"),
    GRAPHITE(".IconGraphite"),
}

/** Tudo o que a aba Conta configura. Os padrões são o comportamento das versões anteriores. */
data class AppSettings(
    /** Destino/qualidade que a folha de download já abre selecionados (ela guarda a última escolha aqui). */
    val downloadTarget: DownloadTarget = DownloadTarget.DEVICE,
    val downloadQuality: DownloadQuality = DownloadQuality.MAX,
    /** Aba da Biblioteca (Dispositivo/App) e ordenação com que as listas abrem. */
    val librarySource: DownloadTarget = DownloadTarget.DEVICE,
    val librarySort: LibrarySort = LibrarySort.RECENT,
    val style: AppStyle = AppStyle.CLASSIC,
    val icon: AppIcon = AppIcon.CLASSIC,
)
