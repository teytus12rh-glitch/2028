package com.leandro.launcher

import android.content.Intent
import android.graphics.drawable.Drawable

/** Um app que aparece nas fileiras do launcher. */
class AppEntry(
    val label: String,
    val packageName: String,
    val launchIntent: Intent,
    val image: Drawable,
    /** true = banner de TV (preenche o cartão); false = ícone comum (centralizado). */
    val isBanner: Boolean
)

/** Um atalho do sistema (botões do topo). */
class ShortcutEntry(
    val label: String,
    val action: () -> Unit
)
