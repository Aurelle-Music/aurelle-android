plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Etapa 5: processador de anotações do Room (a versão do KSP acompanha a do Kotlin)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
