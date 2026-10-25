package id.vixnihon.nativeapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** UI palette used by Home and Settings; dynamic getters recompose on theme change. */
internal object NihongoPalette {
    var lightMode by mutableStateOf(false)
    val background get() = if (lightMode) Color(0xFFF6F7FB) else Color(0xFF07070D)
    val surface get() = if (lightMode) Color.White else Color(0xFF0F0F17)
    val surface2 get() = if (lightMode) Color(0xFFF2F3F8) else Color(0xFF161621)
    val surface3 get() = if (lightMode) Color(0xFFE7E9F1) else Color(0xFF1E1E2C)
    val red = Color(0xFFC42B2B)
    val redLight = Color(0xFFE03535)
    val redSoft get() = if (lightMode) Color(0xFFCE3333) else Color(0xFFFF5A5A)
    val gold get() = if (lightMode) Color(0xFFAF802A) else Color(0xFFC49A3C)
    val white get() = if (lightMode) Color(0xFF141521) else Color(0xFFEAEAF3)
    val dim get() = if (lightMode) Color(0xFF6E6882) else Color(0xFF8984AA)
    val dim2 get() = if (lightMode) Color(0xFF78728A) else Color(0xFFA7A0C8)
    val border get() = if (lightMode) Color(0x1C172034) else Color(0x1AFFFFFF)
    val borderStrong get() = if (lightMode) Color(0x27172034) else Color(0x26FFFFFF)
    val green = Color(0xFF2DB36A)
}
