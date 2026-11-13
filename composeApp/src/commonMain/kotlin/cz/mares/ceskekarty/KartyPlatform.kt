package cz.mares.ceskekarty

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

data class SitovaOdpoved(
    val stavovyKod: Int,
    val telo: String,
    val uspesne: Boolean
)

enum class EfektStolu {
    KARTA_NA_STUL,
    SLAVNOSTNI_CINK
}

expect fun ziskejAktualniCasMs(): Long

expect fun ulozTextDoUloziste(klic: String, hodnota: String)

expect fun nactiTextZUloziste(klic: String, vychozi: String = ""): String

expect fun jemnaHaptika()

expect fun silnaHaptika()

expect fun prehrajHospodaKlipPlatforma(idKlipu: String): Boolean

expect fun vyslovCeskyTextPlatforma(text: String, vyskaHlasu: Float, rychlostHlasu: Float)

expect fun zastavMluveniPlatforma()

expect fun prehrajZvukovyEfektPlatforma(efekt: EfektStolu)

expect suspend fun provedSitovyDotaz(
    metoda: String,
    cilovaUrl: String,
    jsonTelo: String? = null
): SitovaOdpoved

expect fun nactiArchKaretBitmap(): ImageBitmap?

fun dekodujBase64Bloky(bloky: Array<String>): ByteArray {
    val celkovaDelka = bloky.sumOf { it.length }
    if (celkovaDelka == 0) return ByteArray(0)

    val tabulka = IntArray(256) { -1 }
    val abeceda = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    for (i in abeceda.indices) {
        tabulka[abeceda[i].code] = i
    }

    val odhad = (celkovaDelka * 3) / 4 + 4
    val vystup = ByteArray(odhad)
    var outPos = 0
    var buffer = 0
    var bity = 0

    for (blok in bloky) {
        for (i in blok.indices) {
            val c = blok[i].code
            if (c == '='.code) {
                return if (outPos == vystup.size) vystup else vystup.copyOf(outPos)
            }
            val hodnota = if (c in 0..255) tabulka[c] else -1
            if (hodnota < 0) continue
            buffer = ((buffer shl 6) or hodnota) and 0xFFFF
            bity += 6
            if (bity >= 8) {
                bity -= 8
                vystup[outPos++] = ((buffer ushr bity) and 0xFF).toByte()
            }
        }
    }
    return if (outPos == vystup.size) vystup else vystup.copyOf(outPos)
}

fun dekodujArchKaretBajty(): ByteArray = dekodujBase64Bloky(BOHEMIAN_DECK_B64_CHUNKS)

private var kesovanyArchKaret: ImageBitmap? = null

fun ziskejArchJednohlavychKaret(): ImageBitmap? {
    kesovanyArchKaret?.let { return it }
    val nacteny = nactiArchKaretBitmap()
    if (nacteny != null) {
        kesovanyArchKaret = nacteny
    }
    return nacteny
}

private val IMA_INDEX_TABLE: IntArray = intArrayOf(
    -1, -1, -1, -1, 2, 4, 6, 8,
    -1, -1, -1, -1, 2, 4, 6, 8
)

private val IMA_STEP_TABLE: IntArray = intArrayOf(
    7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
    19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
    50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
    130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
    337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
    876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
    2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
    5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
    15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
)

fun dekodujImaAdpcmNaPcm16(adpcmBajty: ByteArray): ShortArray {
    if (adpcmBajty.isEmpty()) return ShortArray(0)
    val vystup = ShortArray(adpcmBajty.size * 2)
    var prediktor = 0
    var indexKroku = 0
    var outIdx = 0

    for (i in adpcmBajty.indices) {
        val b = adpcmBajty[i].toInt() and 0xFF
        for (posun in 0..4 step 4) {
            val nibble = (b ushr posun) and 0x0F
            val krok = IMA_STEP_TABLE[indexKroku]
            var rozdil = krok shr 3
            if ((nibble and 4) != 0) rozdil += krok
            if ((nibble and 2) != 0) rozdil += (krok shr 1)
            if ((nibble and 1) != 0) rozdil += (krok shr 2)
            if ((nibble and 8) != 0) {
                prediktor -= rozdil
            } else {
                prediktor += rozdil
            }
            if (prediktor > 32767) prediktor = 32767
            else if (prediktor < -32768) prediktor = -32768

            indexKroku += IMA_INDEX_TABLE[nibble]
            if (indexKroku < 0) indexKroku = 0
            else if (indexKroku > 88) indexKroku = 88

            vystup[outIdx++] = prediktor.toShort()
        }
    }
    return vystup
}

private val kesovaneHospodaPcm = HashMap<String, ShortArray>()

fun ziskejHospodaKlipPcm16(idKlipu: String): ShortArray? {
    kesovaneHospodaPcm[idKlipu]?.let { return it }
    val bloky = ziskejHospodaKlipB64(idKlipu) ?: return null
    val adpcmBajty = dekodujBase64Bloky(bloky)
    if (adpcmBajty.isEmpty()) return null
    val pcm = dekodujImaAdpcmNaPcm16(adpcmBajty)
    if (pcm.isNotEmpty()) {
        kesovaneHospodaPcm[idKlipu] = pcm
    }
    return pcm
}

fun vygenerujPcm16Efektu(efekt: EfektStolu, vzorkovaciFrekvence: Int = 22050): ShortArray {
    return when (efekt) {
        EfektStolu.KARTA_NA_STUL -> {
            val pocetVzorku = (vzorkovaciFrekvence * 0.075).toInt()
            val vzorky = ShortArray(pocetVzorku)
            var stavSumu = 0x5A3C91
            var dolniPropust = 0.0
            for (i in 0 until pocetVzorku) {
                val t = i.toDouble() / vzorkovaciFrekvence
                stavSumu = (stavSumu * 1103515245 + 12345) and 0x7FFFFFFF
                val bilySum = (stavSumu.toDouble() / 1073741824.0) - 1.0
                dolniPropust = dolniPropust * 0.58 + bilySum * 0.42
                val obalkaSumu = exp(-t * 52.0)
                val obalkaNarazu = exp(-t * 95.0)
                val obalkaLupnuti = exp(-t * 210.0)
                val drevenyStul = sin(2.0 * PI * (165.0 - t * 620.0).coerceAtLeast(85.0) * t) * obalkaNarazu * 0.72
                val lupnutiKarty = sin(2.0 * PI * 980.0 * t) * obalkaLupnuti * 0.34
                val svistPapiru = dolniPropust * obalkaSumu * 0.65
                val hodnota = ((drevenyStul + lupnutiKarty + svistPapiru) * 26500.0)
                    .coerceIn(-32000.0, 32000.0)
                vzorky[i] = hodnota.toInt().toShort()
            }
            vzorky
        }

        EfektStolu.SLAVNOSTNI_CINK -> {
            val pocetVzorku = (vzorkovaciFrekvence * 0.24).toInt()
            val vzorky = ShortArray(pocetVzorku)
            for (i in 0 until pocetVzorku) {
                val t = i.toDouble() / vzorkovaciFrekvence
                val obalka1 = exp(-t * 14.0)
                val ton1 = sin(2.0 * PI * 1046.5 * t) * obalka1 * 0.44
                val ton2 = if (t >= 0.065) {
                    val t2 = t - 0.065
                    sin(2.0 * PI * 1318.5 * t2) * exp(-t2 * 11.0) * 0.50
                } else {
                    0.0
                }
                val hodnota = ((ton1 + ton2) * 24500.0).coerceIn(-32000.0, 32000.0)
                vzorky[i] = hodnota.toInt().toShort()
            }
            vzorky
        }
    }
}

fun pcm16NaWavBajty(pcm: ShortArray, vzorkovaciFrekvence: Int): ByteArray {
    val velikostDat = pcm.size * 2
    val celkovaVelikost = 44 + velikostDat
    val wav = ByteArray(celkovaVelikost)

    fun zapisAscii(pos: Int, txt: String) {
        for (i in txt.indices) {
            wav[pos + i] = txt[i].code.toByte()
        }
    }

    fun zapisInt32LE(pos: Int, hodnota: Int) {
        wav[pos] = (hodnota and 0xFF).toByte()
        wav[pos + 1] = ((hodnota ushr 8) and 0xFF).toByte()
        wav[pos + 2] = ((hodnota ushr 16) and 0xFF).toByte()
        wav[pos + 3] = ((hodnota ushr 24) and 0xFF).toByte()
    }

    fun zapisInt16LE(pos: Int, hodnota: Int) {
        wav[pos] = (hodnota and 0xFF).toByte()
        wav[pos + 1] = ((hodnota ushr 8) and 0xFF).toByte()
    }

    zapisAscii(0, "RIFF")
    zapisInt32LE(4, 36 + velikostDat)
    zapisAscii(8, "WAVE")
    zapisAscii(12, "fmt ")
    zapisInt32LE(16, 16)
    zapisInt16LE(20, 1)
    zapisInt16LE(22, 1)
    zapisInt32LE(24, vzorkovaciFrekvence)
    zapisInt32LE(28, vzorkovaciFrekvence * 2)
    zapisInt16LE(32, 2)
    zapisInt16LE(34, 16)
    zapisAscii(36, "data")
    zapisInt32LE(40, velikostDat)

    var p = 44
    for (vzorek in pcm) {
        val v = vzorek.toInt()
        wav[p] = (v and 0xFF).toByte()
        wav[p + 1] = ((v ushr 8) and 0xFF).toByte()
        p += 2
    }
    return wav
}

fun vygenerujWavBajtyEfektu(efekt: EfektStolu, vzorkovaciFrekvence: Int = 22050): ByteArray {
    return pcm16NaWavBajty(vygenerujPcm16Efektu(efekt, vzorkovaciFrekvence), vzorkovaciFrekvence)
}

fun ziskejHospodaKlipWavBajty(idKlipu: String): ByteArray? {
    val pcm = ziskejHospodaKlipPcm16(idKlipu) ?: return null
    if (pcm.isEmpty()) return null
    return pcm16NaWavBajty(pcm, HOSPODA_AUDIO_VZORKOVANI_HZ)
}

const val STARTOVNI_TEREZIANY = 1000
const val DENNI_ODMENA_TEREZIANY = 100
const val HRANICE_PODPORY_TEREZIANY = 100
const val DOROVNANI_PODPORY_TEREZIANY = 200

private const val KLIC_ID_ZARIZENI = "ck_device_id_v1"
private const val KLIC_PREZDIVKA = "ck_saved_nick_v1"
private const val KLIC_PIN_OTISK = "ck_saved_pin_hash_v1"
private const val KLIC_TEREZIANY = "ck_local_elo_v1"
private const val KLIC_DENNI_ODMENA_DEN = "ck_daily_reward_day_v1"
private const val KLIC_ZVUK_HLASKY = "ck_sound_voice_enabled_v1"

fun jeZvukAHlaskyZapnuty(): Boolean {
    return nactiTextZUloziste(KLIC_ZVUK_HLASKY, "1") != "0"
}

fun nastavZvukAHlasky(zapnuto: Boolean) {
    ulozTextDoUloziste(KLIC_ZVUK_HLASKY, if (zapnuto) "1" else "0")
    if (!zapnuto) {
        zastavMluveniPlatforma()
    }
}

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

fun ulozObnoveneIdZarizeni(obnoveneDeviceId: String) {
    val ciste = obnoveneDeviceId.trim()
    if (ciste.startsWith("dev_") && ciste.length >= 8) {
        ulozTextDoUloziste(KLIC_ID_ZARIZENI, ciste)
    }
}

fun nactiUlozenouPrezdivku(): String = nactiTextZUloziste(KLIC_PREZDIVKA, "").trim()

fun ulozPrezdivkuTrvale(prezdivka: String) {
    ulozTextDoUloziste(KLIC_PREZDIVKA, prezdivka.trim())
}

fun nactiUlozenyPinOtisk(): String = nactiTextZUloziste(KLIC_PIN_OTISK, "").trim()

fun ulozPinOtiskTrvale(otisk: String) {
    ulozTextDoUloziste(KLIC_PIN_OTISK, otisk.trim())
}

fun nactiLokalniTereziany(): Int {
    val ulozene = nactiTextZUloziste(KLIC_TEREZIANY, STARTOVNI_TEREZIANY.toString()).toIntOrNull()
        ?: STARTOVNI_TEREZIANY
    return ulozene.coerceIn(0, 999_999)
}

fun ulozLokalniTereziany(novaCastka: Int) {
    val omezene = novaCastka.coerceIn(0, 999_999)
    ulozTextDoUloziste(KLIC_TEREZIANY, omezene.toString())
}

fun aktualniCisloDne(): Int {
    return ((ziskejAktualniCasMs() + 3_600_000L) / 86_400_000L).toInt()
}

fun nactiPosledniDenOdmeny(): Int {
    return nactiTextZUloziste(KLIC_DENNI_ODMENA_DEN, "0").toIntOrNull() ?: 0
}

fun ulozPosledniDenOdmeny(den: Int) {
    ulozTextDoUloziste(KLIC_DENNI_ODMENA_DEN, den.toString())
}

fun muzeVyzvednoutDenniOdmenu(): Boolean {
    return aktualniCisloDne() > nactiPosledniDenOdmeny()
}

fun nazevMenaTerezian(pocet: Int): String {
    val absolutni = abs(pocet)
    return when {
        absolutni == 1 -> "terezián"
        absolutni in 2..4 -> "tereziány"
        else -> "tereziánů"
    }
}

fun formatujTereziany(pocet: Int, seZnaminkem: Boolean = false): String {
    val znamenko = if (seZnaminkem && pocet > 0) "+" else ""
    return "$znamenko$pocet ${nazevMenaTerezian(pocet)}"
}

fun vypoctiOtiskPinu(prezdivka: String, pin: String): String {
    val normalizovanyNick = prezdivka.trim().lowercase()
    val cistyPin = pin.trim()
    return vypoctiSha256Hex("ceske_karty_pin_v1:$normalizovanyNick:$cistyPin")
}

private val SHA256_K: IntArray = intArrayOf(
    0x428a2f98L.toInt(), 0x71374491L.toInt(), 0xb5c0fbcfL.toInt(), 0xe9b5dba5L.toInt(),
    0x3956c25bL.toInt(), 0x59f111f1L.toInt(), 0x923f82a4L.toInt(), 0xab1c5ed5L.toInt(),
    0xd807aa98L.toInt(), 0x12835b01L.toInt(), 0x243185beL.toInt(), 0x550c7dc3L.toInt(),
    0x72be5d74L.toInt(), 0x80deb1feL.toInt(), 0x9bdc06a7L.toInt(), 0xc19bf174L.toInt(),
    0xe49b69c1L.toInt(), 0xefbe4786L.toInt(), 0x0fc19dc6L.toInt(), 0x240ca1ccL.toInt(),
    0x2de92c6fL.toInt(), 0x4a7484aaL.toInt(), 0x5cb0a9dcL.toInt(), 0x76f988daL.toInt(),
    0x983e5152L.toInt(), 0xa831c66dL.toInt(), 0xb00327c8L.toInt(), 0xbf597fc7L.toInt(),
    0xc6e00bf3L.toInt(), 0xd5a79147L.toInt(), 0x06ca6351L.toInt(), 0x14292967L.toInt(),
    0x27b70a85L.toInt(), 0x2e1b2138L.toInt(), 0x4d2c6dfcL.toInt(), 0x53380d13L.toInt(),
    0x650a7354L.toInt(), 0x766a0abbL.toInt(), 0x81c2c92eL.toInt(), 0x92722c85L.toInt(),
    0xa2bfe8a1L.toInt(), 0xa81a664bL.toInt(), 0xc24b8b70L.toInt(), 0xc76c51a3L.toInt(),
    0xd192e819L.toInt(), 0xd6990624L.toInt(), 0xf40e3585L.toInt(), 0x106aa070L.toInt(),
    0x19a4c116L.toInt(), 0x1e376c08L.toInt(), 0x2748774cL.toInt(), 0x34b0bcb5L.toInt(),
    0x391c0cb3L.toInt(), 0x4ed8aa4aL.toInt(), 0x5b9cca4fL.toInt(), 0x682e6ff3L.toInt(),
    0x748f82eeL.toInt(), 0x78a5636fL.toInt(), 0x84c87814L.toInt(), 0x8cc70208L.toInt(),
    0x90befffaL.toInt(), 0xa4506cebL.toInt(), 0xbef9a3f7L.toInt(), 0xc67178f2L.toInt()
)

private fun rotR(x: Int, n: Int): Int = (x ushr n) or (x shl (32 - n))

fun vypoctiSha256Hex(vstup: String): String {
    val bajty = vstup.encodeToByteArray()
    val delkaBitu: Long = bajty.size.toLong() * 8L
    val zbytek = (bajty.size + 1) % 64
    val nulNavic = if (zbytek <= 56) (56 - zbytek) else (56 + 64 - zbytek)
    val celkovaDelka = bajty.size + 1 + nulNavic + 8

    val blok = ByteArray(celkovaDelka)
    for (i in bajty.indices) {
        blok[i] = bajty[i]
    }
    blok[bajty.size] = 0x80.toByte()
    for (i in 0 until 8) {
        val posun = (7 - i) * 8
        blok[celkovaDelka - 8 + i] = ((delkaBitu ushr posun) and 0xFFL).toByte()
    }

    var h0 = 0x6a09e667L.toInt()
    var h1 = 0xbb67ae85L.toInt()
    var h2 = 0x3c6ef372L.toInt()
    var h3 = 0xa54ff53aL.toInt()
    var h4 = 0x510e527fL.toInt()
    var h5 = 0x9b05688cL.toInt()
    var h6 = 0x1f83d9abL.toInt()
    var h7 = 0x5be0cd19L.toInt()

    val w = IntArray(64)
    var offset = 0
    while (offset < celkovaDelka) {
        for (i in 0 until 16) {
            val z = offset + i * 4
            w[i] = ((blok[z].toInt() and 0xFF) shl 24) or
                ((blok[z + 1].toInt() and 0xFF) shl 16) or
                ((blok[z + 2].toInt() and 0xFF) shl 8) or
                (blok[z + 3].toInt() and 0xFF)
        }
        for (i in 16 until 64) {
            val s0 = rotR(w[i - 15], 7) xor rotR(w[i - 15], 18) xor (w[i - 15] ushr 3)
            val s1 = rotR(w[i - 2], 17) xor rotR(w[i - 2], 19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7

        for (i in 0 until 64) {
            val sum1 = rotR(e, 6) xor rotR(e, 11) xor rotR(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + sum1 + ch + SHA256_K[i] + w[i]
            val sum0 = rotR(a, 2) xor rotR(a, 13) xor rotR(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = sum0 + maj

            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        h5 += f
        h6 += g
        h7 += h

        offset += 64
    }

    val slova = intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
    val hexZnaky = "0123456789abcdef"
    val vystup = StringBuilder(64)
    for (slovo in slova) {
        for (k in 7 downTo 0) {
            val cislice = (slovo ushr (k * 4)) and 0xF
            vystup.append(hexZnaky[cislice])
        }
    }
    return vystup.toString()
}
