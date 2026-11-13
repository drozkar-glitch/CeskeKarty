package cz.mares.ceskekarty

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private var ulozenyAndroidKontext: Context? = null
private var aktivniAktivitaRef: WeakReference<Activity>? = null

private var androidTts: TextToSpeech? = null
private var ttsPripraveno: Boolean = false
private var ttsPodporujeCestinu: Boolean = false
private var ttsInicializaceBezi: Boolean = false
private var cekajiciHlaskaTts: Triple<String, Float, Float>? = null

private val kesovanyPcmKarta: ShortArray by lazy { vygenerujPcm16Efektu(EfektStolu.KARTA_NA_STUL, 22050) }
private val kesovanyPcmCink: ShortArray by lazy { vygenerujPcm16Efektu(EfektStolu.SLAVNOSTNI_CINK, 22050) }

fun inicializujAndroidPlatformu(kontext: Context) {
    ulozenyAndroidKontext = kontext.applicationContext
}

fun zaregistrujHlavniAktivitu(aktivita: Activity) {
    aktivniAktivitaRef = WeakReference(aktivita)
    if (ulozenyAndroidKontext == null) {
        ulozenyAndroidKontext = aktivita.applicationContext
    }
}

fun odregistrujHlavniAktivitu(aktivita: Activity) {
    if (aktivniAktivitaRef?.get() === aktivita) {
        aktivniAktivitaRef = null
    }
}

private fun ziskejAndroidKontext(): Context? {
    ulozenyAndroidKontext?.let { return it }
    aktivniAktivitaRef?.get()?.applicationContext?.let {
        ulozenyAndroidKontext = it
        return it
    }
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

private fun ziskejVibrator(ctx: Context): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}

private fun zavibrujEfekt(efekt: VibrationEffect?, fallbackMs: Long) {
    val ctx = ziskejAndroidKontext() ?: return
    val vib = ziskejVibrator(ctx) ?: return
    if (!vib.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && efekt != null) {
        val attr = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
        vib.vibrate(efekt, attr)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && efekt != null) {
        @Suppress("DEPRECATION")
        val audioAttr = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build()
        @Suppress("DEPRECATION")
        vib.vibrate(efekt, audioAttr)
    } else {
        @Suppress("DEPRECATION")
        vib.vibrate(fallbackMs)
    }
}

actual fun jemnaHaptika() {
    try {
        val oknoView = aktivniAktivitaRef?.get()?.window?.decorView
        val provedenoPresView = oknoView?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP
        ) == true

        if (!provedenoPresView) {
            val efekt = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                    VibrationEffect.createOneShot(26L, 155)
                else -> null
            }
            zavibrujEfekt(efekt, 26L)
        }
    } catch (_: Throwable) {
    }
}

actual fun silnaHaptika() {
    try {
        val oknoView = aktivniAktivitaRef?.get()?.window?.decorView
        oknoView?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val efekt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(
                longArrayOf(0L, 42L, 48L, 62L),
                intArrayOf(0, 215, 0, 255),
                -1
            )
        } else {
            null
        }
        zavibrujEfekt(efekt, 75L)
    } catch (_: Throwable) {
    }
}

private fun zajistiInicializaciTts(ctx: Context) {
    if (androidTts != null || ttsInicializaceBezi) return
    ttsInicializaceBezi = true
    try {
        androidTts = TextToSpeech(ctx.applicationContext) { status ->
            ttsInicializaceBezi = false
            val engine = androidTts
            if (status == TextToSpeech.SUCCESS && engine != null) {
                val czLocale = Locale("cs", "CZ")
                val vysledek = engine.setLanguage(czLocale)
                val maCz = vysledek != TextToSpeech.LANG_MISSING_DATA &&
                    vysledek != TextToSpeech.LANG_NOT_SUPPORTED
                if (!maCz) {
                    val altVys = engine.setLanguage(Locale("cs"))
                    ttsPodporujeCestinu = altVys != TextToSpeech.LANG_MISSING_DATA &&
                        altVys != TextToSpeech.LANG_NOT_SUPPORTED
                } else {
                    ttsPodporujeCestinu = true
                }
                ttsPripraveno = true
                val cekajici = cekajiciHlaskaTts
                cekajiciHlaskaTts = null
                if (cekajici != null && ttsPodporujeCestinu && jeZvukAHlaskyZapnuty()) {
                    odehrajTtsInterni(engine, cekajici.first, cekajici.second, cekajici.third)
                }
            } else {
                ttsPripraveno = false
                ttsPodporujeCestinu = false
            }
        }
    } catch (_: Throwable) {
        ttsInicializaceBezi = false
    }
}

private fun odehrajTtsInterni(
    engine: TextToSpeech,
    text: String,
    vyska: Float,
    rychlost: Float
) {
    try {
        engine.setPitch(vyska.coerceIn(0.65f, 1.45f))
        engine.setSpeechRate(rychlost.coerceIn(0.75f, 1.35f))
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ck_${System.currentTimeMillis()}")
    } catch (_: Throwable) {
    }
}

actual fun vyslovCeskyTextPlatforma(text: String, vyskaHlasu: Float, rychlostHlasu: Float) {
    if (text.isBlank()) return
    val ctx = ziskejAndroidKontext() ?: return
    zajistiInicializaciTts(ctx)
    val engine = androidTts
    if (ttsPripraveno && engine != null) {
        if (ttsPodporujeCestinu) {
            odehrajTtsInterni(engine, text, vyskaHlasu, rychlostHlasu)
        }
    } else {
        cekajiciHlaskaTts = Triple(text, vyskaHlasu, rychlostHlasu)
    }
}

actual fun zastavMluveniPlatforma() {
    cekajiciHlaskaTts = null
    try {
        androidTts?.stop()
    } catch (_: Throwable) {
    }
}

actual fun prehrajZvukovyEfektPlatforma(efekt: EfektStolu) {
    val pcm = when (efekt) {
        EfektStolu.KARTA_NA_STUL -> kesovanyPcmKarta
        EfektStolu.SLAVNOSTNI_CINK -> kesovanyPcmCink
    }
    if (pcm.isEmpty()) return

    Thread {
        var stopa: AudioTrack? = null
        try {
            val vzorkovani = 22050
            val velikostBajty = pcm.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                vzorkovani,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(velikostBajty)

            val atributy = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(vzorkovani)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            stopa = AudioTrack(
                atributy,
                format,
                minBuf,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )
            stopa.write(pcm, 0, pcm.size)
            stopa.play()
            val dobaMs = ((pcm.size * 1000L) / vzorkovani) + 80L
            Thread.sleep(dobaMs)
        } catch (_: Throwable) {
        } finally {
            try {
                stopa?.stop()
            } catch (_: Throwable) {
            }
            try {
                stopa?.release()
            } catch (_: Throwable) {
            }
        }
    }.start()
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
