package id.vixnihon.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import id.vixnihon.nativeapp.data.AuthRepository
import id.vixnihon.nativeapp.data.SessionStore
import id.vixnihon.nativeapp.ui.NihongoRoot

class MainActivity : ComponentActivity() {
    private val repository by lazy { AuthRepository() }
    private val sessionStore by lazy { SessionStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.rgb(7, 7, 13)
        window.navigationBarColor = android.graphics.Color.rgb(7, 7, 13)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFE03535),
                    onPrimary = Color.White,
                    secondary = Color(0xFFDEB55A),
                    background = Color(0xFF07070D),
                    onBackground = Color(0xFFEAEAF3),
                    surface = Color(0xFF0F0F17),
                    onSurface = Color(0xFFEAEAF3),
                    error = Color(0xFFFF6868)
                )
            ) {
                NihongoRoot(
                    activity = this,
                    repository = repository,
                    sessionStore = sessionStore
                )
            }
        }
    }
}
