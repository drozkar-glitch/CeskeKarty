package cz.mares.ceskekarty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inicializujAndroidPlatformu(applicationContext)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}
