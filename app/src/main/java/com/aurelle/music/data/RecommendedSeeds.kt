package com.aurelle.music.data

import java.util.Locale

/**
 * Buscas usadas para preencher "Recommended Artists/Songs" quando a busca está vazia.
 * Solução provisória: recomendações pessoais reais ficam para a etapa do login (Etapa 7).
 * Para mudar o que aparece na Home, é só editar os textos abaixo.
 */
object RecommendedSeeds {
    fun queryFor(section: Section, locale: Locale = Locale.getDefault()): String {
        val brazil = locale.country.equals("BR", ignoreCase = true)
        return when (section) {
            Section.ARTISTS -> if (brazil) "artistas brasileiros populares" else "popular artists"
            Section.SONGS -> if (brazil) "músicas mais tocadas" else "top hits"
        }
    }
}
