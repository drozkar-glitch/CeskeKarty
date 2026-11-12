@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class
)

package cz.mares.ceskekarty

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

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
        val generator = UIImpactFeedbackGenerator(
            style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        )
        generator.prepare()
        generator.impactOccurred()
    }
}

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

    val pozadavek = NSMutableURLRequest(nsUrl)
    pozadavek.timeoutInterval = 10.0
    pozadavek.setHTTPMethod(metoda.uppercase())
    pozadavek.setValue("application/json", forHTTPHeaderField = "Accept")
    pozadavek.setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")

    if (jsonTelo != null && metoda.uppercase() != "GET" && metoda.uppercase() != "DELETE") {
        val dataTela: NSData? = NSString.create(string = jsonTelo).dataUsingEncoding(NSUTF8StringEncoding)
        pozadavek.setHTTPBody(dataTela)
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
        val textOdpovedi = if (data != null) {
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString() ?: ""
        } else {
            ""
        }
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
