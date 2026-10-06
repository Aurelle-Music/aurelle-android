package com.aurelle.music.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Troca o ícone do app ligando um `activity-alias` e desligando os outros (só um tem o filtro LAUNCHER ligado).
 * O launcher pode levar alguns segundos para mostrar o novo ícone; alguns fecham o app ao trocar.
 */
object IconSwitcher {
    fun apply(context: Context, icon: AppIcon) {
        val pm = context.packageManager
        // Liga o novo primeiro: nunca fica sem ícone na tela inicial.
        AppIcon.entries.sortedByDescending { it == icon }.forEach { option ->
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, "${context.packageName}${option.alias}"),
                if (option == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
