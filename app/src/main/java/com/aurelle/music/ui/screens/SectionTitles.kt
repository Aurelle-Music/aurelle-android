package com.aurelle.music.ui.screens

import androidx.annotation.StringRes
import com.aurelle.music.R
import com.aurelle.music.data.Section

@StringRes
fun Section.titleRes(recommended: Boolean): Int = when (this) {
    Section.ARTISTS ->
        if (recommended) R.string.section_recommended_artists else R.string.section_artists
    Section.SONGS ->
        if (recommended) R.string.section_recommended_songs else R.string.section_songs
}
