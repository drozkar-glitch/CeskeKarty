package cz.mares.ceskekarty

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private var appCtx: Context? = null

fun inicializujAndroidKontext(context: Context) {
    appCtx = context.applicationContext
}

actual fun ziskejAktualniCasMs(): Long = System.currentTimeMillis()

actual fun ulozTextDoUloziste(klic: String, hodnota: String) {
    val ctx = appCtx ?: return
    ctx.getSharedPreferences("ceske_karty_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString(klic, hodnota)
        .apply()
}

actual fun nactiTextZUloziste(klic: String, vychozi: String): String {
    val ctx = appCtx ?: return vychozi
    return ctx.getSharedPreferences("ceske_karty_prefs", Context.MODE_PRIVATE)
        .getString(klic, vychozi) ?: vychozi
}

actual fun jemnaHaptika() {
    val ctx = appCtx ?: return
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18L)
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
        val urlObj = URL(cilovaUrl)
        spojeni = (urlObj.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (metoda.equals("PATCH", ignoreCase = true)) {
                requestMethod = "POST"
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                requestMethod = metoda.uppercase()
            }
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
        val vstupniProud = if (kod in 200..299) spojeni.inputStream else spojeni.errorStream
        val prectenyText = if (vstupniProud != null) {
            BufferedReader(InputStreamReader(vstupniProud, Charsets.UTF_8)).use { it.readText() }
        } else {
            ""
        }
        SitovaOdpoved(
            stavovyKod = kod,
            telo = prectenyText,
            uspesne = kod in 200..299
        )
    } catch (chyba: Throwable) {
        SitovaOdpoved(
            stavovyKod = 0,
            telo = chyba.message ?: "Chyba spojeni",
            uspesne = false
        )
    } finally {
        spojeni?.disconnect()
    }
}
