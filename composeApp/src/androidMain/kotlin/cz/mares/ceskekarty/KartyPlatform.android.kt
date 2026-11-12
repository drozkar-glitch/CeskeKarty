package cz.mares.ceskekarty

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private var ulozenyAndroidKontext: Context? = null

fun inicializujAndroidPlatformu(kontext: Context) {
    ulozenyAndroidKontext = kontext.applicationContext
}

private fun ziskejAndroidKontext(): Context? {
    ulozenyAndroidKontext?.let { return it }
    return try {
        val trida = Class.forName("android.app.ActivityThread")
        val metoda = trida.getMethod("currentApplication")
        val app = metoda.invoke(null) as? Context
        if (app != null) ulozenyAndroidKontext = app
        app
    } catch (_: Throwable) {
        null
    }
}

actual fun ziskejAktualniCasMs(): Long = System.currentTimeMillis()

actual fun ulozTextDoUloziste(klic: String, hodnota: String) {
    val ctx = ziskejAndroidKontext() ?: return
    ctx.getSharedPreferences("ceske_karty_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString(klic, hodnota)
        .apply()
}

actual fun nactiTextZUloziste(klic: String, vychozi: String): String {
    val ctx = ziskejAndroidKontext() ?: return vychozi
    return ctx.getSharedPreferences("ceske_karty_prefs", Context.MODE_PRIVATE)
        .getString(klic, vychozi) ?: vychozi
}

actual fun jemnaHaptika() {
    try {
        val ctx = ziskejAndroidKontext() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(18L)
            }
        }
    } catch (_: Throwable) {
    }
}

actual suspend fun provedSitovyDotaz(
    metoda: String,
    cilovaUrl: String,
    jsonTelo: String?
): SitovaOdpoved = withContext(Dispatchers.IO) {
    var spojeni: HttpURLConnection? = null
    try {
        val url = URL(cilovaUrl)
        spojeni = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            val m = metoda.uppercase()
            if (m == "PATCH") {
                try {
                    requestMethod = "PATCH"
                } catch (_: ProtocolException) {
                    requestMethod = "POST"
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                }
            } else {
                requestMethod = m
            }
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        if (jsonTelo != null && metoda.uppercase() != "GET" && metoda.uppercase() != "DELETE") {
            spojeni.doOutput = true
            val bajty = jsonTelo.toByteArray(Charsets.UTF_8)
            spojeni.outputStream.use { proud ->
                proud.write(bajty)
                proud.flush()
            }
        }

        val kod = spojeni.responseCode
        val proudOdpovedi = if (kod in 200..299) spojeni.inputStream else spojeni.errorStream
        val textOdpovedi = if (proudOdpovedi != null) {
            BufferedReader(InputStreamReader(proudOdpovedi, Charsets.UTF_8)).use { it.readText() }
        } else {
            ""
        }
        SitovaOdpoved(
            stavovyKod = kod,
            telo = textOdpovedi,
            uspesne = kod in 200..299
        )
    } catch (e: Throwable) {
        SitovaOdpoved(
            stavovyKod = 0,
            telo = e.message ?: "Chyba site",
            uspesne = false
        )
    } finally {
        spojeni?.disconnect()
    }
}

actual fun nactiArchKaretBitmap(): ImageBitmap? {
    return try {
        val bajty = dekodujArchKaretBajty()
        if (bajty.isNotEmpty()) {
            BitmapFactory.decodeByteArray(bajty, 0, bajty.size)?.asImageBitmap()
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    }
}
