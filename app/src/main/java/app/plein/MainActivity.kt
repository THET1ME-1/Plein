package app.plein

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.plein.data.AppRepository
import app.plein.data.Folders
import app.plein.data.Prefs
import app.plein.ui.home.HomeScreen
import app.plein.ui.theme.DefaultSeed
import app.plein.ui.theme.PleinTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = AppRepository(this)
        prefs = Prefs(this)
        repository.start()

        setContent {
            val apps by repository.apps.collectAsState()
            val folders = remember(apps) { Folders.build(apps) }
            val dark = if (prefs.followSystemTheme) isSystemInDarkTheme() else prefs.darkTheme

            // Палитра приложения приезжает из кадра: в этом вся идея Plein.
            var seed by remember { mutableStateOf(DefaultSeed) }

            LaunchedEffect(Unit) { repository.refresh() }

            PleinTheme(dark = dark, seed = seed) {
                HomeScreen(
                    folders = folders,
                    repository = repository,
                    prefs = prefs,
                    backdropAuthor = "Alexey Topolyanskiy",
                    onShuffleBackdrop = {},
                    onSeedExtracted = { seed = it },
                    onOpenSearch = {},
                    onAppMenu = {},
                    onOpenSettings = {},
                )
            }
        }
    }

    override fun onDestroy() {
        repository.stop()
        super.onDestroy()
    }
}
