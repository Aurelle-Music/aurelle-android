package com.aurelle.music.data

/** 1_250 -> "1.3K", 3_400_000 -> "3.4M". */
fun compactCount(value: Long): String = when {
    value >= 1_000_000_000 -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}.replace(".0", "")

/** 3_400_000 -> "3,2 MB" (usa o separador decimal do idioma do aparelho). */
fun formatSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024L -> "%d KB".format(bytes / 1024)
    else -> "$bytes B"
}
