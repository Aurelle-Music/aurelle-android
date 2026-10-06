package com.aurelle.music

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.aurelle.music.ui.AurelleApp
import com.aurelle.music.ui.theme.AurelleTheme

class MainActivity : ComponentActivity() {

    /** Sobe a cada toque na notificação de mídia; a UI abre o player completo quando muda. */
    private var openPlayerSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            AurelleTheme {
                AurelleApp(openPlayerSignal = openPlayerSignal)
            }
        }
    }

    // launchMode="singleTop": tocar na notificação com o app aberto chega aqui, sem criar outra tela.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_PLAYER) {
            openPlayerSignal++
            intent.action = null // não reabrir o player em recriações da tela (rotação etc.)
        }
    }

    companion object {
        const val ACTION_OPEN_PLAYER = "com.aurelle.music.action.OPEN_PLAYER"
    }
}
