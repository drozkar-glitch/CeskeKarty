@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package cz.mares.ceskekarty

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.coroutines.resume
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import platform.Foundation.*
import platform.UIKit.UIImpactFeedbackGenerator
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

actual fun ziskejAktualniCasMs(): Long {
    return (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}

actual fun ulozTextDoUloziste(klic: String, hodnota: String) {
    NSUserDefaults.standardUserDefaults.setObject(hodnota, forKey = klic)
}

actual fun nactiTextZUloziste(klic: String, vychozi: String): String {
    return NSUserDefaults.standardUserDefaults.stringForKey(klic) ?: vychozi
}

actual fun jemnaHaptika() {
    dispatch_async(dispatch_get_main_queue()) {
        val generator = UIImpactFeedbackGenerator()
        generator.prepare()
        generator.impactOccurred()
    }
}

private fun retezecNaNsData(text: String): NSData? {
    val bajty = text.encodeToByteArray()
    if (bajty.isEmpty()) return NSData()
    return bajty.usePinned { pripnute ->
        NSData.dataWithBytes(pripnute.addressOf(0), bajty.size.toULong())
    }
}

private fun nsDataNaBajty(data: NSData): ByteArray {
    val delka = data.length.toInt()
    if (delka <= 0) return ByteArray(0)
    val zdroj = data.bytes ?: return ByteArray(0)
    val bajty = ByteArray(delka)
    bajty.usePinned { pripnute ->
        memcpy(pripnute.addressOf(0), zdroj, data.length)
    }
    return bajty
}

private fun nsDataNaRetezec(data: NSData): String = nsDataNaBajty(data).decodeToString()

actual suspend fun provedSitovyDotaz(
    metoda: String,
    cilovaUrl: String,
    jsonTelo: String?
): SitovaOdpoved = suspendCancellableCoroutine { pokracovani ->
    val nsUrl = NSURL.URLWithString(cilovaUrl)
    if (nsUrl == null) {
        pokracovani.resume(SitovaOdpoved(0, "Neplatna URL", false))
        return@suspendCancellableCoroutine
    }

    val pozadavek = NSMutableURLRequest(
        nsUrl,
        NSURLRequestReloadIgnoringLocalCacheData,
        10.0
    )
    pozadavek.setHTTPMethod(metoda.uppercase())
    pozadavek.setValue("application/json", forHTTPHeaderField = "Accept")
    pozadavek.setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")

    if (jsonTelo != null && metoda.uppercase() != "GET" && metoda.uppercase() != "DELETE") {
        pozadavek.setHTTPBody(retezecNaNsData(jsonTelo))
    }

    val uloha = NSURLSession.sharedSession.dataTaskWithRequest(pozadavek) { data, odpoved, chyba ->
        if (!pokracovani.isActive) return@dataTaskWithRequest
        if (chyba != null) {
            pokracovani.resume(
                SitovaOdpoved(
                    stavovyKod = 0,
                    telo = chyba.localizedDescription,
                    uspesne = false
                )
            )
            return@dataTaskWithRequest
        }
        val httpOdpoved = odpoved as? NSHTTPURLResponse
        val kod = httpOdpoved?.statusCode?.toInt() ?: 200
        val textOdpovedi = if (data != null) nsDataNaRetezec(data) else ""
        pokracovani.resume(
            SitovaOdpoved(
                stavovyKod = kod,
                telo = textOdpovedi,
                uspesne = kod in 200..299
            )
        )
    }

    pokracovani.invokeOnCancellation {
        uloha.cancel()
    }
    uloha.resume()
}

actual fun nactiArchKaretBitmap(): ImageBitmap? {
    return try {
        val bajty = dekodujArchKaretBajty()
        if (bajty.isEmpty()) return null
        Image.makeFromEncoded(bajty).toComposeImageBitmap()
    } catch (_: Throwable) {
        null
    }
}
