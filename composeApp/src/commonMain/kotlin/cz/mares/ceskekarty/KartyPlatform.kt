package cz.mares.ceskekarty

import kotlin.random.Random

data class SitovaOdpoved(
    val stavovyKod: Int,
    val telo: String,
    val uspesne: Boolean
)

expect fun ziskejAktualniCasMs(): Long

expect fun ulozTextDoUloziste(klic: String, hodnota: String)

expect fun nactiTextZUloziste(klic: String, vychozi: String = ""): String

expect fun jemnaHaptika()

expect suspend fun provedSitovyDotaz(
    metoda: String,
    cilovaUrl: String,
    jsonTelo: String? = null
): SitovaOdpoved

private const val KLIC_ID_ZARIZENI = "ck_device_id_v1"
private const val KLIC_PREZDIVKA = "ck_saved_nick_v1"
private const val KLIC_ELO = "ck_local_elo_v1"

fun ziskejUnikatniIdZarizeni(): String {
    val ulozene = nactiTextZUloziste(KLIC_ID_ZARIZENI, "")
    if (ulozene.startsWith("dev_") && ulozene.length == 24) {
        return ulozene
    }
    val abeceda = "abcdefghijklmnopqrstuvwxyz0123456789"
    val sb = StringBuilder("dev_")
    repeat(20) {
        sb.append(abeceda[Random.nextInt(abeceda.length)])
    }
    val noveId = sb.toString()
    ulozTextDoUloziste(KLIC_ID_ZARIZENI, noveId)
    return noveId
}

fun nactiUlozenouPrezdivku(): String = nactiTextZUloziste(KLIC_PREZDIVKA, "").trim()

fun ulozPrezdivkuTrvale(prezdivka: String) {
    ulozTextDoUloziste(KLIC_PREZDIVKA, prezdivka.trim())
}

fun nactiLokalniElo(): Int = nactiTextZUloziste(KLIC_ELO, "1000").toIntOrNull() ?: 1000

fun ulozLokalniElo(noveElo: Int) {
    val omezene = noveElo.coerceIn(100, 9999)
    ulozTextDoUloziste(KLIC_ELO, omezene.toString())
}
