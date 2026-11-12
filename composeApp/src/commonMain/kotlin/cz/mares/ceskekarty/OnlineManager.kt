package cz.mares.ceskekarty

import kotlin.math.max
import kotlin.random.Random
import kotlinx.coroutines.delay

const val FIREBASE_DB_ZAKLAD = "https://mares-games-default-rtdb.europe-west1.firebasedatabase.app"
const val CLOUDFLARE_WORKER_URL = "https://ceske-karty.drozkar.workers.dev"
private const val ZEBRICEK_GAME_ID = "ceske_karty"

const val LIMIT_ZELENA_MS = 10_000L
const val LIMIT_ODPOJENI_MS = 60_000L

enum class StavPripojeniHrace {
    ONLINE_ZELENA,
    VYPADEK_ZLUTA,
    ODPOJEN_CERVENA
}

data class InfoPripojeniHrace(
    val stav: StavPripojeniHrace,
    val zbyvaSekundDoUkonceni: Int
)

data class TepHrace(
    val deviceId: String,
    val casMs: Long
)

data class HracUStolu(
    val deviceId: String,
    val prezdivka: String,
    val elo: Int
)

enum class StavMistnosti {
    CEKANI,
    HRAJE_SE,
    DOHRANO,
    UKONCENO_VYPADKEM
}

data class OnlineStul(
    val roomId: String,
    val nazevStolu: String,
    val typHry: TypHry,
    val maxHracu: Int,
    val pinKod: String,
    val hostDeviceId: String,
    val stavMistnosti: StavMistnosti,
    val hraci: List<HracUStolu>,
    val serializovanyStavHry: String,
    val verzeStavu: Int,
    val zacatekTahuMs: Long,
    val vytvorenoMs: Long,
    val ukoncenoKym: String,
    val turnajId: String,
    val slotZapasu: Int,
    val cisloRozdani: Int,
    val celkemRozdani: Int,
    val soucetBoduHracu: List<Int>,
    val tepyHracu: List<TepHrace>
) {
    val jeSoukromy: Boolean
        get() = pinKod.isNotEmpty()

    fun indexLokalnihoHrace(mojeDeviceId: String): Int {
        return hraci.indexOfFirst { it.deviceId == mojeDeviceId }
    }

    fun infoPripojeni(deviceId: String, nyniMs: Long): InfoPripojeniHrace {
        val zaznam = tepyHracu.firstOrNull { it.deviceId == deviceId }
        val posledniMs = zaznam?.casMs ?: vytvorenoMs
        val prodleva = max(0L, nyniMs - posledniMs)
        val zbyvaSekund = max(0L, (LIMIT_ODPOJENI_MS - prodleva + 999L) / 1000L).toInt()
        val stav = when {
            prodleva <= LIMIT_ZELENA_MS -> StavPripojeniHrace.ONLINE_ZELENA
            prodleva < LIMIT_ODPOJENI_MS -> StavPripojeniHrace.VYPADEK_ZLUTA
            else -> StavPripojeniHrace.ODPOJEN_CERVENA
        }
        return InfoPripojeniHrace(stav = stav, zbyvaSekundDoUkonceni = zbyvaSekund)
    }
}

data class ZapasTurnaje(
    val slot: Int,
    val nazevKola: String,
    val hrac1Id: String,
    val hrac1Jmeno: String,
    val hrac2Id: String,
    val hrac2Jmeno: String,
    val vitezId: String,
    val vitezJmeno: String,
    val roomId: String
)

data class OnlineTurnaj(
    val turnajId: String,
    val nazev: String,
    val typHry: TypHry,
    val kapacita: Int,
    val stav: String,
    val ucastnici: List<HracUStolu>,
    val zapasy: List<ZapasTurnaje>,
    val mariasRoomId: String,
    val celkovyVitez: String,
    val vytvorenoMs: Long
)

data class ZaznamZebricku(
    val poradi: Int,
    val prezdivka: String,
    val skoreElo: Int,
    val jeLokalniHrac: Boolean
)

sealed class VysledekRegistraceNicku {
    data class Uspech(val zprava: String = "") : VysledekRegistraceNicku()
    data class Chyba(val zprava: String) : VysledekRegistraceNicku()
}

sealed class JsonPrvek {
    data class Objekt(val polozky: List<Pair<String, JsonPrvek>>) : JsonPrvek() {
        fun pole(klic: String): JsonPrvek? = polozky.firstOrNull { it.first == klic }?.second
        fun retezec(klic: String, vychozi: String = ""): String =
            (pole(klic) as? Retezec)?.hodnota ?: vychozi
        fun cisloLong(klic: String, vychozi: Long = 0L): Long =
            (pole(klic) as? Cislo)?.hodnota?.toLong() ?: vychozi
        fun cisloInt(klic: String, vychozi: Int = 0): Int =
            (pole(klic) as? Cislo)?.hodnota?.toInt() ?: vychozi
    }

    data class Pole(val prvky: List<JsonPrvek>) : JsonPrvek()
    data class Retezec(val hodnota: String) : JsonPrvek()
    data class Cislo(val hodnota: Double) : JsonPrvek()
    data class Logicka(val hodnota: Boolean) : JsonPrvek()
    data object Prazdno : JsonPrvek()
}

private class JsonCtecka(private val zdroj: String) {
    private var pozice = 0

    fun parsuj(): JsonPrvek {
        preskocBileZnaky()
        if (pozice >= zdroj.length) return JsonPrvek.Prazdno
        return parsujHodnotu()
    }

    private fun preskocBileZnaky() {
        while (pozice < zdroj.length && zdroj[pozice].isWhitespace()) {
            pozice++
        }
    }

    private fun parsujHodnotu(): JsonPrvek {
        preskocBileZnaky()
        if (pozice >= zdroj.length) return JsonPrvek.Prazdno
        return when (zdroj[pozice]) {
            '{' -> parsujObjekt()
            '[' -> parsujPole()
            '"' -> JsonPrvek.Retezec(parsujRetezec())
            't', 'f' -> parsujBool()
            'n' -> parsujNull()
            else -> parsujCislo()
        }
    }

    private fun parsujObjekt(): JsonPrvek.Objekt {
        pozice++
        val seznam = ArrayList<Pair<String, JsonPrvek>>()
        preskocBileZnaky()
        if (pozice < zdroj.length && zdroj[pozice] == '}') {
            pozice++
            return JsonPrvek.Objekt(seznam)
        }
        while (pozice < zdroj.length) {
            preskocBileZnaky()
            if (pozice >= zdroj.length || zdroj[pozice] != '"') break
            val klic = parsujRetezec()
            preskocBileZnaky()
            if (pozice < zdroj.length && zdroj[pozice] == ':') pozice++
            val hodnota = parsujHodnotu()
            seznam.add(klic to hodnota)
            preskocBileZnaky()
            if (pozice < zdroj.length && zdroj[pozice] == ',') {
                pozice++
            } else if (pozice < zdroj.length && zdroj[pozice] == '}') {
                pozice++
                break
            } else {
                break
            }
        }
        return JsonPrvek.Objekt(seznam)
    }

    private fun parsujPole(): JsonPrvek.Pole {
        pozice++
        val seznam = ArrayList<JsonPrvek>()
        preskocBileZnaky()
        if (pozice < zdroj.length && zdroj[pozice] == ']') {
            pozice++
            return JsonPrvek.Pole(seznam)
        }
        while (pozice < zdroj.length) {
            seznam.add(parsujHodnotu())
            preskocBileZnaky()
            if (pozice < zdroj.length && zdroj[pozice] == ',') {
                pozice++
            } else if (pozice < zdroj.length && zdroj[pozice] == ']') {
                pozice++
                break
            } else {
                break
            }
        }
        return JsonPrvek.Pole(seznam)
    }

    private fun parsujRetezec(): String {
        if (pozice >= zdroj.length || zdroj[pozice] != '"') return ""
        pozice++
        val sb = StringBuilder()
        while (pozice < zdroj.length) {
            val c = zdroj[pozice++]
            if (c == '"') break
            if (c == '\\' && pozice < zdroj.length) {
                when (val esc = zdroj[pozice++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        if (pozice + 4 <= zdroj.length) {
                            val hex = zdroj.substring(pozice, pozice + 4)
                            val kod = hex.toIntOrNull(16)
                            if (kod != null) sb.append(kod.toChar())
                            pozice += 4
                        }
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parsujBool(): JsonPrvek {
        return if (zdroj.startsWith("true", pozice)) {
            pozice += 4
            JsonPrvek.Logicka(true)
        } else if (zdroj.startsWith("false", pozice)) {
            pozice += 5
            JsonPrvek.Logicka(false)
        } else {
            pozice++
            JsonPrvek.Prazdno
        }
    }

    private fun parsujNull(): JsonPrvek {
        if (zdroj.startsWith("null", pozice)) {
            pozice += 4
        } else {
            pozice++
        }
        return JsonPrvek.Prazdno
    }

    private fun parsujCislo(): JsonPrvek {
        val start = pozice
        while (pozice < zdroj.length && zdroj[pozice] in "0123456789+-eE.") {
            pozice++
        }
        val vyrez = zdroj.substring(start, pozice)
        val d = vyrez.toDoubleOrNull()
        return if (d != null) JsonPrvek.Cislo(d) else JsonPrvek.Prazdno
    }
}

fun parsujJsonText(text: String): JsonPrvek = JsonCtecka(text).parsuj()

fun jsonEscape(text: String): String {
    val sb = StringBuilder(text.length + 8)
    sb.append('"')
    for (c in text) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> {
                if (c.code < 32) {
                    val hex = c.code.toString(16).padStart(4, '0')
                    sb.append("\\u").append(hex)
                } else {
                    sb.append(c)
                }
            }
        }
    }
    sb.append('"')
    return sb.toString()
}

fun urlKodujKomponentu(text: String): String {
    val povolene = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.~"
    val bajty = text.encodeToByteArray()
    val sb = StringBuilder(bajty.size * 2)
    for (b in bajty) {
        val neznam = b.toInt() and 0xFF
        val znak = neznam.toChar()
        if (znak in povolene) {
            sb.append(znak)
        } else {
            val hex = neznam.toString(16).uppercase().padStart(2, '0')
            sb.append('%').append(hex)
        }
    }
    return sb.toString()
}

object OnlineManager {

    private suspend fun dotazSOpakovanim(
        metoda: String,
        url: String,
        jsonTelo: String? = null,
        pokusy: Int = 3
    ): SitovaOdpoved {
        var posledni = SitovaOdpoved(0, "", false)
        for (pokus in 0 until pokusy) {
            posledni = provedSitovyDotaz(metoda, url, jsonTelo)
            if (posledni.uspesne) return posledni
            if (pokus < pokusy - 1) {
                delay(350L * (pokus + 1))
            }
        }
        return posledni
    }

    suspend fun overAUlozPrezdivku(kandidat: String, pin: String = ""): VysledekRegistraceNicku {
        val cista = kandidat.trim()
            .replace("/", "")
            .replace(".", "")
            .replace("#", "")
            .replace("$", "")
            .replace("[", "")
            .replace("]", "")
        if (cista.length < 3) {
            return VysledekRegistraceNicku.Chyba("Přezdívka musí mít alespoň 3 znaky.")
        }
        if (cista.length > 18) {
            return VysledekRegistraceNicku.Chyba("Přezdívka může mít nejvýše 18 znaků.")
        }

        val cistyPin = pin.trim()
        if (cistyPin.length != 4 || !cistyPin.all { it.isDigit() }) {
            return VysledekRegistraceNicku.Chyba("Zadej prosím přesně 4 číslice PIN kódu.")
        }

        val zadanyOtisk = vypoctiOtiskPinu(cista, cistyPin)
        val mojeId = ziskejUnikatniIdZarizeni()
        val kodovana = urlKodujKomponentu(cista)
        val urlHs = "$FIREBASE_DB_ZAKLAD/high_scores/$ZEBRICEK_GAME_ID/$kodovana.json"
        val urlUcet = "$FIREBASE_DB_ZAKLAD/card_accounts/$kodovana.json"

        val odpovedUcet = dotazSOpakovanim("GET", urlUcet, pokusy = 2)
        val odpovedHs = dotazSOpakovanim("GET", urlHs, pokusy = 2)

        val ucetObj = if (odpovedUcet.uspesne && odpovedUcet.telo.isNotBlank() && odpovedUcet.telo.trim() != "null") {
            parsujJsonText(odpovedUcet.telo) as? JsonPrvek.Objekt
        } else null

        val hsPrvek = if (odpovedHs.uspesne && odpovedHs.telo.isNotBlank() && odpovedHs.telo.trim() != "null") {
            parsujJsonText(odpovedHs.telo)
        } else JsonPrvek.Prazdno

        val hsSkore = when (hsPrvek) {
            is JsonPrvek.Objekt -> hsPrvek.cisloInt("score", 0)
            is JsonPrvek.Cislo -> hsPrvek.hodnota.toInt()
            else -> 0
        }
        val hsDeviceId = (hsPrvek as? JsonPrvek.Objekt)?.retezec("deviceId", "") ?: ""

        if (ucetObj != null) {
            val ulozenyOtisk = ucetObj.retezec("pinHash", "")
            if (ulozenyOtisk.isNotEmpty()) {
                if (ulozenyOtisk != zadanyOtisk) {
                    return VysledekRegistraceNicku.Chyba("Přezdívka již existuje — zadaný PIN nesouhlasí.")
                }
                val puvodniDeviceId = ucetObj.retezec("deviceId", "").ifEmpty {
                    hsDeviceId.ifEmpty { mojeId }
                }
                val ucetSkore = ucetObj.cisloInt("score", 1000)
                val obnoveneElo = (if (hsSkore > 0) hsSkore else ucetSkore).coerceIn(100, 9999)

                ulozObnoveneIdZarizeni(puvodniDeviceId)
                ulozPrezdivkuTrvale(cista)
                ulozPinOtiskTrvale(zadanyOtisk)
                ulozLokalniElo(obnoveneElo)

                val nyni = ziskejAktualniCasMs()
                val teloUcet = """{"nick":${jsonEscape(cista)},"pinHash":${jsonEscape(zadanyOtisk)},"deviceId":${jsonEscape(puvodniDeviceId)},"score":$obnoveneElo,"updatedAt":$nyni}"""
                val teloHs = """{"score":$obnoveneElo,"deviceId":${jsonEscape(puvodniDeviceId)},"updatedAt":$nyni}"""
                dotazSOpakovanim("PUT", urlUcet, teloUcet, pokusy = 2)
                dotazSOpakovanim("PUT", urlHs, teloHs, pokusy = 2)

                return VysledekRegistraceNicku.Uspech("Účet $cista (ELO $obnoveneElo) byl úspěšně obnoven!")
            }
        }

        if (hsPrvek is JsonPrvek.Objekt || hsPrvek is JsonPrvek.Cislo) {
            if (hsDeviceId.isNotEmpty() && hsDeviceId != mojeId) {
                return VysledekRegistraceNicku.Chyba("Tato přezdívka je již obsazena jiným hráčem.")
            }
            if (hsPrvek is JsonPrvek.Cislo) {
                return VysledekRegistraceNicku.Chyba("Tato přezdívka je již obsazena v žebříčku.")
            }
        }

        val startElo = (if (hsSkore > 0) hsSkore else nactiLokalniElo()).coerceAtLeast(1000)
        val nyni = ziskejAktualniCasMs()
        val teloUcet = """{"nick":${jsonEscape(cista)},"pinHash":${jsonEscape(zadanyOtisk)},"deviceId":${jsonEscape(mojeId)},"score":$startElo,"updatedAt":$nyni}"""
        val teloHs = """{"score":$startElo,"deviceId":${jsonEscape(mojeId)},"updatedAt":$nyni}"""

        dotazSOpakovanim("PUT", urlUcet, teloUcet, pokusy = 2)
        dotazSOpakovanim("PUT", urlHs, teloHs, pokusy = 2)

        ulozPrezdivkuTrvale(cista)
        ulozPinOtiskTrvale(zadanyOtisk)
        ulozLokalniElo(startElo)
        return VysledekRegistraceNicku.Uspech("Přezdívka $cista byla uložena a zabezpečena PINem.")
    }

    suspend fun doplnPinKeStavajiciPrezdivce(pin: String): VysledekRegistraceNicku {
        val aktualniNick = nactiUlozenouPrezdivku()
        if (aktualniNick.isEmpty()) {
            return VysledekRegistraceNicku.Chyba("Nejprve si zvol přezdívku.")
        }
        return overAUlozPrezdivku(aktualniNick, pin)
    }

    suspend fun odesliAktualniEloDoZebricku(noveElo: Int) {
        val cisteElo = noveElo.coerceIn(100, 9999)
        ulozLokalniElo(cisteElo)
        val nick = nactiUlozenouPrezdivku()
        if (nick.isBlank() || cisteElo <= 0) return

        val mojeId = ziskejUnikatniIdZarizeni()
        val kodovana = urlKodujKomponentu(nick)
        val nyni = ziskejAktualniCasMs()

        val urlHs = "$FIREBASE_DB_ZAKLAD/high_scores/$ZEBRICEK_GAME_ID/$kodovana.json"
        val teloHs = """{"score":$cisteElo,"deviceId":${jsonEscape(mojeId)},"updatedAt":$nyni}"""
        dotazSOpakovanim("PUT", urlHs, teloHs, pokusy = 2)

        val pinOtisk = nactiUlozenyPinOtisk()
        if (pinOtisk.isNotEmpty()) {
            val urlUcet = "$FIREBASE_DB_ZAKLAD/card_accounts/$kodovana.json"
            val teloUcet = """{"nick":${jsonEscape(nick)},"pinHash":${jsonEscape(pinOtisk)},"deviceId":${jsonEscape(mojeId)},"score":$cisteElo,"updatedAt":$nyni}"""
            dotazSOpakovanim("PUT", urlUcet, teloUcet, pokusy = 2)
        }
    }

    suspend fun nactiZebricek(includeZeroScores: Boolean = false): List<ZaznamZebricku> {
        val url = "$FIREBASE_DB_ZAKLAD/high_scores/$ZEBRICEK_GAME_ID.json"
        val odpoved = dotazSOpakovanim("GET", url, pokusy = 2)
        if (!odpoved.uspesne || odpoved.telo.isBlank() || odpoved.telo.trim() == "null") {
            return emptyList()
        }
        val koren = parsujJsonText(odpoved.telo) as? JsonPrvek.Objekt ?: return emptyList()
        val mujNick = nactiUlozenouPrezdivku()

        val nactene = ArrayList<Pair<String, Int>>()
        for ((nick, hodnota) in koren.polozky) {
            val skore = when (hodnota) {
                is JsonPrvek.Objekt -> hodnota.cisloInt("score", 0)
                is JsonPrvek.Cislo -> hodnota.hodnota.toInt()
                else -> 0
            }
            if (includeZeroScores || skore > 0) {
                nactene.add(nick to skore)
            }
        }

        val serazene = nactene.sortedByDescending { it.second }
        return serazene.mapIndexed { index, (nick, skore) ->
            ZaznamZebricku(
                poradi = index + 1,
                prezdivka = nick,
                skoreElo = skore,
                jeLokalniHrac = nick.equals(mujNick, ignoreCase = true)
            )
        }
    }

    private fun serializujSeznamHracu(hraci: List<HracUStolu>): String {
        return hraci.joinToString(";") { h ->
            val jmeno = h.prezdivka.replace("~", " ").replace(";", " ").replace("|", " ")
            "${h.deviceId}~${jmeno}~${h.elo}"
        }
    }

    private fun deserializujSeznamHracu(text: String): List<HracUStolu> {
        if (text.isBlank()) return emptyList()
        return text.split(";").mapNotNull { polozka ->
            val casti = polozka.split("~")
            if (casti.size < 3) return@mapNotNull null
            HracUStolu(
                deviceId = casti[0],
                prezdivka = casti[1],
                elo = casti[2].toIntOrNull() ?: 1000
            )
        }
    }

    private fun serializujMetaStolu(stul: OnlineStul): String {
        return serializujMapuPoli(
            listOf(
                "id" to stul.roomId,
                "naz" to stul.nazevStolu,
                "typ" to stul.typHry.idHry,
                "max" to stul.maxHracu.toString(),
                "pin" to stul.pinKod,
                "host" to stul.hostDeviceId,
                "stav" to stul.stavMistnosti.name,
                "hr" to serializujSeznamHracu(stul.hraci),
                "vyt" to stul.vytvorenoMs.toString(),
                "uk" to stul.ukoncenoKym,
                "tid" to stul.turnajId,
                "slot" to stul.slotZapasu.toString(),
                "cr" to stul.cisloRozdani.toString(),
                "celr" to stul.celkemRozdani.toString(),
                "sb" to stul.soucetBoduHracu.joinToString(",")
            )
        )
    }

    private fun parsujStulZJsonObjektu(fallbackId: String, obj: JsonPrvek.Objekt): OnlineStul? {
        val metaText = obj.retezec("meta", "")
        if (metaText.isBlank()) return null
        val r = PoleStavuReader(metaText)
        val roomId = r.text("id", fallbackId)
        val typHry = TypHry.dleId(r.text("typ", "PRSI"))
        val stavText = obj.retezec("statusOverride", r.text("stav", "CEKANI"))
        val stavMistnosti = when (stavText) {
            "ABORTED_TIMEOUT", "UKONCENO_VYPADKEM" -> StavMistnosti.UKONCENO_VYPADKEM
            "PLAYING", "HRAJE_SE" -> StavMistnosti.HRAJE_SE
            "FINISHED", "DOHRANO" -> StavMistnosti.DOHRANO
            else -> StavMistnosti.CEKANI
        }
        val hraci = deserializujSeznamHracu(r.text("hr"))
        if (hraci.isEmpty()) return null

        val tepy = ArrayList<TepHrace>()
        val hbObj = obj.pole("heartbeats") as? JsonPrvek.Objekt
        if (hbObj != null) {
            for ((devId, prvekCasu) in hbObj.polozky) {
                val ts = (prvekCasu as? JsonPrvek.Cislo)?.hodnota?.toLong() ?: 0L
                if (ts > 0L) tepy.add(TepHrace(devId, ts))
            }
        }

        val souctyText = r.text("sb", "")
        val soucty = if (souctyText.isBlank()) {
            List(hraci.size.coerceAtLeast(r.cislo("max", 2))) { 0 }
        } else {
            souctyText.split(",").map { it.toIntOrNull() ?: 0 }
        }

        val ukoncenoOverride = obj.retezec("abortedBy", r.text("uk", ""))

        return OnlineStul(
            roomId = roomId,
            nazevStolu = r.text("naz", "Stůl"),
            typHry = typHry,
            maxHracu = r.cislo("max", typHry.povolenePoctyHracu.first()),
            pinKod = r.text("pin", ""),
            hostDeviceId = r.text("host", hraci.first().deviceId),
            stavMistnosti = stavMistnosti,
            hraci = hraci,
            serializovanyStavHry = obj.retezec("state", ""),
            verzeStavu = obj.cisloInt("version", 0),
            zacatekTahuMs = obj.cisloLong("turnStartedAt", r.text("vyt").toLongOrNull() ?: 0L),
            vytvorenoMs = r.text("vyt").toLongOrNull() ?: 0L,
            ukoncenoKym = ukoncenoOverride,
            turnajId = r.text("tid", ""),
            slotZapasu = r.cislo("slot", -1),
            cisloRozdani = r.cislo("cr", 1),
            celkemRozdani = r.cislo("celr", 1),
            soucetBoduHracu = soucty,
            tepyHracu = tepy
        )
    }

    suspend fun nactiZiveStoly(): List<OnlineStul> {
        val url = "$FIREBASE_DB_ZAKLAD/card_rooms.json"
        val odpoved = dotazSOpakovanim("GET", url, pokusy = 2)
        if (!odpoved.uspesne || odpoved.telo.isBlank() || odpoved.telo.trim() == "null") {
            return emptyList()
        }
        val koren = parsujJsonText(odpoved.telo) as? JsonPrvek.Objekt ?: return emptyList()
        val nyni = ziskejAktualniCasMs()
        val stoly = ArrayList<OnlineStul>()

        for ((id, hodnota) in koren.polozky) {
            val obj = hodnota as? JsonPrvek.Objekt ?: continue
            val stul = parsujStulZJsonObjektu(id, obj) ?: continue

            val nejnovejsiTep = stul.tepyHracu.maxOfOrNull { it.casMs } ?: stul.vytvorenoMs
            if (nyni - nejnovejsiTep > 20 * 60_000L || stul.stavMistnosti == StavMistnosti.DOHRANO) {
                smazStul(stul.roomId)
                continue
            }
            stoly.add(stul)
        }
        return stoly.sortedByDescending { it.vytvorenoMs }
    }

    suspend fun nactiKonkretniStul(roomId: String): OnlineStul? {
        val url = "$FIREBASE_DB_ZAKLAD/card_rooms/$roomId.json"
        val odpoved = dotazSOpakovanim("GET", url, pokusy = 2)
        if (!odpoved.uspesne || odpoved.telo.isBlank() || odpoved.telo.trim() == "null") {
            return null
        }
        val obj = parsujJsonText(odpoved.telo) as? JsonPrvek.Objekt ?: return null
        return parsujStulZJsonObjektu(roomId, obj)
    }

    suspend fun vytvorStul(
        nazevStolu: String,
        typHry: TypHry,
        pocetHracu: Int,
        pinKod: String = "",
        turnajId: String = "",
        slotZapasu: Int = -1,
        celkemRozdani: Int = 1,
        predvyplneniHraci: List<HracUStolu>? = null
    ): OnlineStul? {
        val mojeId = ziskejUnikatniIdZarizeni()
        val mujNick = nactiUlozenouPrezdivku().ifBlank { "Hráč" }
        val mojeElo = nactiLokalniElo()
        val nyni = ziskejAktualniCasMs()
        val nahoda = Random.nextInt(1000, 9999)
        val roomId = "stul_${nyni}_$nahoda"

        val seznamHracu = predvyplneniHraci ?: listOf(HracUStolu(mojeId, mujNick, mojeElo))
        val stul = OnlineStul(
            roomId = roomId,
            nazevStolu = nazevStolu.ifBlank { "${typHry.nazev} — $mujNick" },
            typHry = typHry,
            maxHracu = pocetHracu,
            pinKod = pinKod.trim(),
            hostDeviceId = seznamHracu.first().deviceId,
            stavMistnosti = StavMistnosti.CEKANI,
            hraci = seznamHracu,
            serializovanyStavHry = "",
            verzeStavu = 0,
            zacatekTahuMs = nyni,
            vytvorenoMs = nyni,
            ukoncenoKym = "",
            turnajId = turnajId,
            slotZapasu = slotZapasu,
            cisloRozdani = 1,
            celkemRozdani = celkemRozdani,
            soucetBoduHracu = List(pocetHracu) { 0 },
            tepyHracu = seznamHracu.map { TepHrace(it.deviceId, nyni) }
        )

        val meta = serializujMetaStolu(stul)
        val hbPolo = seznamHracu.joinToString(",") { "${jsonEscape(it.deviceId)}:$nyni" }
        val telo = """{"meta":${jsonEscape(meta)},"state":"","version":0,"turnStartedAt":$nyni,"heartbeats":{$hbPolo}}"""

        val odpoved = dotazSOpakovanim("PUT", "$FIREBASE_DB_ZAKLAD/card_rooms/$roomId.json", telo)
        if (!odpoved.uspesne) return null

        return if (seznamHracu.size == pocetHracu) {
            rozdejASpustStul(stul, zacinajiciIndex = 0) ?: stul
        } else {
            stul
        }
    }

    suspend fun prisedniKeStolu(roomId: String, zadanyPin: String = ""): Result<OnlineStul> {
        val aktualni = nactiKonkretniStul(roomId)
            ?: return Result.failure(Exception("Stůl již neexistuje."))

        val mojeId = ziskejUnikatniIdZarizeni()
        if (aktualni.hraci.any { it.deviceId == mojeId }) {
            odesliTepSpojeni(roomId)
            return Result.success(aktualni)
        }

        if (aktualni.stavMistnosti != StavMistnosti.CEKANI) {
            return Result.failure(Exception("Hra u tohoto stolu již probíhá."))
        }
        if (aktualni.hraci.size >= aktualni.maxHracu) {
            return Result.failure(Exception("Stůl je již plně obsazen."))
        }
        if (aktualni.jeSoukromy && aktualni.pinKod != zadanyPin.trim()) {
            return Result.failure(Exception("Nesprávný 4místný PIN soukromého stolu."))
        }

        val mujNick = nactiUlozenouPrezdivku().ifBlank { "Hráč" }
        val mojeElo = nactiLokalniElo()
        val noviHraci = aktualni.hraci + HracUStolu(mojeId, mujNick, mojeElo)
        val upraveny = aktualni.copy(hraci = noviHraci)

        val meta = serializujMetaStolu(upraveny)
        val patchTelo = """{"meta":${jsonEscape(meta)}}"""
        val zapis = dotazSOpakovanim("PATCH", "$FIREBASE_DB_ZAKLAD/card_rooms/$roomId.json", patchTelo)
        if (!zapis.uspesne) {
            return Result.failure(Exception("Nepodařilo se přisednout — zkontroluj připojení."))
        }
        odesliTepSpojeni(roomId)

        if (noviHraci.size == upraveny.maxHracu) {
            val spusteny = rozdejASpustStul(upraveny, zacinajiciIndex = 0)
            if (spusteny != null) return Result.success(spusteny)
        }

        return Result.success(upraveny)
    }

    private suspend fun vygenerujBalicekPresWorkerNeboLokalne(
        roomId: String,
        typHry: TypHry,
        hraci: List<HracUStolu>
    ): List<Karta> {
        val idsJson = hraci.joinToString(",") { jsonEscape(it.deviceId) }
        val req = """{"roomId":${jsonEscape(roomId)},"gameType":${jsonEscape(typHry.idHry)},"playerIds":[$idsJson]}"""
        val workerRes = provedSitovyDotaz("POST", "$CLOUDFLARE_WORKER_URL/deal", req)
        if (workerRes.uspesne) {
            val dealRes = provedSitovyDotaz("GET", "$FIREBASE_DB_ZAKLAD/card_rooms/$roomId/deal.json")
            if (dealRes.uspesne && dealRes.telo.isNotBlank() && dealRes.telo.trim() != "null") {
                val dealObj = parsujJsonText(dealRes.telo) as? JsonPrvek.Objekt
                val handsObj = dealObj?.pole("hands") as? JsonPrvek.Objekt
                if (dealObj != null && handsObj != null) {
                    val slozeny = ArrayList<Karta>(32)
                    for (h in hraci) {
                        slozeny.addAll(retezecNaKarty(handsObj.retezec(h.deviceId, "")))
                    }
                    val top = Karta.zKodu(dealObj.retezec("topCard", ""))
                    if (top != null) slozeny.add(top)
                    slozeny.addAll(retezecNaKarty(dealObj.retezec("stock", "")))
                    if (slozeny.size == 32) {
                        return slozeny
                    }
                }
            }
        }
        return Karta.vytvorZamichanyBalicek()
    }

    suspend fun rozdejASpustStul(
        stul: OnlineStul,
        zacinajiciIndex: Int,
        noveCisloRozdani: Int = stul.cisloRozdani,
        noveSouctyBodu: List<Int> = stul.soucetBoduHracu
    ): OnlineStul? {
        val jmena = stul.hraci.map { it.prezdivka }
        val balicek = vygenerujBalicekPresWorkerNeboLokalne(stul.roomId, stul.typHry, stul.hraci)

        val stavRetezec = when (stul.typHry) {
            TypHry.PRSI -> PrsiEngine.novaHra(jmena, balicek, zacinajiciIndex).doKompaktnihoRetezce()
            TypHry.SEDMA -> SedmaEngine.novaHra(jmena, balicek, zacinajiciIndex).doKompaktnihoRetezce()
            TypHry.MARIAS -> MariasEngine.novaHra(jmena, zacinajiciIndex, balicek).doKompaktnihoRetezce()
        }

        val nyni = ziskejAktualniCasMs()
        val novyStul = stul.copy(
            stavMistnosti = StavMistnosti.HRAJE_SE,
            serializovanyStavHry = stavRetezec,
            verzeStavu = stul.verzeStavu + 1,
            zacatekTahuMs = nyni,
            cisloRozdani = noveCisloRozdani,
            soucetBoduHracu = noveSouctyBodu
        )
        val meta = serializujMetaStolu(novyStul)
        val patch = """{"meta":${jsonEscape(meta)},"state":${jsonEscape(stavRetezec)},"version":${novyStul.verzeStavu},"turnStartedAt":$nyni,"statusOverride":"HRAJE_SE"}"""
        val res = dotazSOpakovanim("PATCH", "$FIREBASE_DB_ZAKLAD/card_rooms/${stul.roomId}.json", patch)
        return if (res.uspesne) novyStul else null
    }

    suspend fun odesliTahHry(
        stul: OnlineStul,
        novySerializovanyStav: String
    ): OnlineStul? {
        val nyni = ziskejAktualniCasMs()
        val novaVerze = stul.verzeStavu + 1
        val patch = """{"state":${jsonEscape(novySerializovanyStav)},"version":$novaVerze,"turnStartedAt":$nyni}"""
        val odpoved = dotazSOpakovanim(
            metoda = "PATCH",
            url = "$FIREBASE_DB_ZAKLAD/card_rooms/${stul.roomId}.json",
            jsonTelo = patch,
            pokusy = 3
        )
        return if (odpoved.uspesne) {
            stul.copy(
                serializovanyStavHry = novySerializovanyStav,
                verzeStavu = novaVerze,
                zacatekTahuMs = nyni
            )
        } else {
            null
        }
    }

    suspend fun odesliTepSpojeni(roomId: String) {
        val mojeId = ziskejUnikatniIdZarizeni()
        val nyni = ziskejAktualniCasMs()
        provedSitovyDotaz(
            metoda = "PUT",
            cilovaUrl = "$FIREBASE_DB_ZAKLAD/card_rooms/$roomId/heartbeats/$mojeId.json",
            jsonTelo = nyni.toString()
        )
    }

    suspend fun ukonciStulProVypadek(stul: OnlineStul, jmenoOdpojeneho: String): OnlineStul {
        provedSitovyDotaz(
            "POST",
            "$CLOUDFLARE_WORKER_URL/check-timeout",
            """{"roomId":${jsonEscape(stul.roomId)}}"""
        )
        val upraveny = stul.copy(
            stavMistnosti = StavMistnosti.UKONCENO_VYPADKEM,
            ukoncenoKym = jmenoOdpojeneho
        )
        val meta = serializujMetaStolu(upraveny)
        val patch = """{"meta":${jsonEscape(meta)},"statusOverride":"ABORTED_TIMEOUT","abortedBy":${jsonEscape(jmenoOdpojeneho)}}"""
        dotazSOpakovanim("PATCH", "$FIREBASE_DB_ZAKLAD/card_rooms/${stul.roomId}.json", patch, pokusy = 2)
        return upraveny
    }

    suspend fun smazStul(roomId: String) {
        if (roomId.isBlank()) return
        provedSitovyDotaz("DELETE", "$FIREBASE_DB_ZAKLAD/card_rooms/$roomId.json")
    }

    private fun serializujZapasy(zapasy: List<ZapasTurnaje>): String {
        return zapasy.joinToString(";") { z ->
            listOf(
                z.slot.toString(),
                z.nazevKola.replace("~", " ").replace(";", " "),
                z.hrac1Id,
                z.hrac1Jmeno.replace("~", " ").replace(";", " "),
                z.hrac2Id,
                z.hrac2Jmeno.replace("~", " ").replace(";", " "),
                z.vitezId,
                z.vitezJmeno.replace("~", " ").replace(";", " "),
                z.roomId
            ).joinToString("~")
        }
    }

    private fun deserializujZapasy(text: String): List<ZapasTurnaje> {
        if (text.isBlank()) return emptyList()
        return text.split(";").mapNotNull { radek ->
            val c = radek.split("~")
            if (c.size < 9) return@mapNotNull null
            ZapasTurnaje(
                slot = c[0].toIntOrNull() ?: 0,
                nazevKola = c[1],
                hrac1Id = c[2],
                hrac1Jmeno = c[3],
                hrac2Id = c[4],
                hrac2Jmeno = c[5],
                vitezId = c[6],
                vitezJmeno = c[7],
                roomId = c[8]
            )
        }
    }

    private fun serializujTurnaj(t: OnlineTurnaj): String {
        return serializujMapuPoli(
            listOf(
                "tid" to t.turnajId,
                "naz" to t.nazev,
                "typ" to t.typHry.idHry,
                "kap" to t.kapacita.toString(),
                "st" to t.stav,
                "uc" to serializujSeznamHracu(t.ucastnici),
                "zap" to serializujZapasy(t.zapasy),
                "mrid" to t.mariasRoomId,
                "vit" to t.celkovyVitez,
                "vyt" to t.vytvorenoMs.toString()
            )
        )
    }

    private fun deserializujTurnaj(data: String): OnlineTurnaj? {
        if (data.isBlank()) return null
        val r = PoleStavuReader(data)
        val tid = r.text("tid")
        if (tid.isBlank()) return null
        val typ = TypHry.dleId(r.text("typ", "PRSI"))
        return OnlineTurnaj(
            turnajId = tid,
            nazev = r.text("naz", "Turnaj"),
            typHry = typ,
            kapacita = r.cislo("kap", if (typ == TypHry.MARIAS) 3 else 4),
            stav = r.text("st", "REGISTRACE"),
            ucastnici = deserializujSeznamHracu(r.text("uc")),
            zapasy = deserializujZapasy(r.text("zap")),
            mariasRoomId = r.text("mrid", ""),
            celkovyVitez = r.text("vit", ""),
            vytvorenoMs = r.text("vyt").toLongOrNull() ?: 0L
        )
    }

    suspend fun nactiTurnaje(): List<OnlineTurnaj> {
        val odpoved = dotazSOpakovanim("GET", "$FIREBASE_DB_ZAKLAD/card_tournaments.json", pokusy = 2)
        if (!odpoved.uspesne || odpoved.telo.isBlank() || odpoved.telo.trim() == "null") {
            return emptyList()
        }
        val koren = parsujJsonText(odpoved.telo) as? JsonPrvek.Objekt ?: return emptyList()
        val nyni = ziskejAktualniCasMs()
        val seznam = ArrayList<OnlineTurnaj>()
        for ((id, prvek) in koren.polozky) {
            val obj = prvek as? JsonPrvek.Objekt ?: continue
            val turnaj = deserializujTurnaj(obj.retezec("payload", "")) ?: continue
            if (nyni - turnaj.vytvorenoMs > 3 * 3600_000L) {
                provedSitovyDotaz("DELETE", "$FIREBASE_DB_ZAKLAD/card_tournaments/$id.json")
                continue
            }
            seznam.add(turnaj)
        }
        return seznam.sortedByDescending { it.vytvorenoMs }
    }

    suspend fun vytvorTurnaj(nazev: String, typHry: TypHry): OnlineTurnaj? {
        val mojeId = ziskejUnikatniIdZarizeni()
        val mujNick = nactiUlozenouPrezdivku().ifBlank { "Hráč" }
        val mojeElo = nactiLokalniElo()
        val nyni = ziskejAktualniCasMs()
        val tid = "tur_${nyni}_${Random.nextInt(100, 999)}"
        val kapacita = if (typHry == TypHry.MARIAS) 3 else 4

        val turnaj = OnlineTurnaj(
            turnajId = tid,
            nazev = nazev.ifBlank { "Pohár: ${typHry.nazev}" },
            typHry = typHry,
            kapacita = kapacita,
            stav = "REGISTRACE",
            ucastnici = listOf(HracUStolu(mojeId, mujNick, mojeElo)),
            zapasy = emptyList(),
            mariasRoomId = "",
            celkovyVitez = "",
            vytvorenoMs = nyni
        )
        return ulozTurnaj(turnaj)
    }

    private suspend fun ulozTurnaj(turnaj: OnlineTurnaj): OnlineTurnaj? {
        val payload = serializujTurnaj(turnaj)
        val telo = """{"payload":${jsonEscape(payload)},"updatedAt":${ziskejAktualniCasMs()}}"""
        val res = dotazSOpakovanim("PUT", "$FIREBASE_DB_ZAKLAD/card_tournaments/${turnaj.turnajId}.json", telo)
        return if (res.uspesne) turnaj else null
    }

    suspend fun prihlasDoTurnaje(turnajId: String): OnlineTurnaj? {
        val vsechny = nactiTurnaje()
        val turnaj = vsechny.firstOrNull { it.turnajId == turnajId } ?: return null
        val mojeId = ziskejUnikatniIdZarizeni()
        if (turnaj.ucastnici.any { it.deviceId == mojeId }) return turnaj
        if (turnaj.stav != "REGISTRACE" || turnaj.ucastnici.size >= turnaj.kapacita) return turnaj

        val novi = turnaj.ucastnici + HracUStolu(
            deviceId = mojeId,
            prezdivka = nactiUlozenouPrezdivku().ifBlank { "Hráč" },
            elo = nactiLokalniElo()
        )

        if (novi.size < turnaj.kapacita) {
            return ulozTurnaj(turnaj.copy(ucastnici = novi))
        }

        return if (turnaj.typHry == TypHry.MARIAS) {
            val stul = vytvorStul(
                nazevStolu = "${turnaj.nazev} (Série 3 rozdání)",
                typHry = TypHry.MARIAS,
                pocetHracu = 3,
                turnajId = turnaj.turnajId,
                slotZapasu = 0,
                celkemRozdani = 3,
                predvyplneniHraci = novi
            )
            ulozTurnaj(
                turnaj.copy(
                    stav = "PROBIHA",
                    ucastnici = novi,
                    mariasRoomId = stul?.roomId ?: ""
                )
            )
        } else {
            val zamichani = novi.shuffled()
            val stulA = vytvorStul(
                nazevStolu = "${turnaj.nazev} — Semifinále A",
                typHry = turnaj.typHry,
                pocetHracu = 2,
                turnajId = turnaj.turnajId,
                slotZapasu = 0,
                predvyplneniHraci = listOf(zamichani[0], zamichani[1])
            )
            val stulB = vytvorStul(
                nazevStolu = "${turnaj.nazev} — Semifinále B",
                typHry = turnaj.typHry,
                pocetHracu = 2,
                turnajId = turnaj.turnajId,
                slotZapasu = 1,
                predvyplneniHraci = listOf(zamichani[2], zamichani[3])
            )
            val zapasy = listOf(
                ZapasTurnaje(
                    slot = 0,
                    nazevKola = "Semifinále A",
                    hrac1Id = zamichani[0].deviceId,
                    hrac1Jmeno = zamichani[0].prezdivka,
                    hrac2Id = zamichani[1].deviceId,
                    hrac2Jmeno = zamichani[1].prezdivka,
                    vitezId = "",
                    vitezJmeno = "",
                    roomId = stulA?.roomId ?: ""
                ),
                ZapasTurnaje(
                    slot = 1,
                    nazevKola = "Semifinále B",
                    hrac1Id = zamichani[2].deviceId,
                    hrac1Jmeno = zamichani[2].prezdivka,
                    hrac2Id = zamichani[3].deviceId,
                    hrac2Jmeno = zamichani[3].prezdivka,
                    vitezId = "",
                    vitezJmeno = "",
                    roomId = stulB?.roomId ?: ""
                ),
                ZapasTurnaje(
                    slot = 2,
                    nazevKola = "Finále",
                    hrac1Id = "",
                    hrac1Jmeno = "Vítěz SF A",
                    hrac2Id = "",
                    hrac2Jmeno = "Vítěz SF B",
                    vitezId = "",
                    vitezJmeno = "",
                    roomId = ""
                )
            )
            ulozTurnaj(turnaj.copy(stav = "PROBIHA", ucastnici = novi, zapasy = zapasy))
        }
    }

    suspend fun zapisVysledekTurnajovehoZapasu(
        turnajId: String,
        slotZapasu: Int,
        vitezDeviceId: String,
        vitezJmeno: String
    ) {
        if (turnajId.isBlank()) return
        val turnaj = nactiTurnaje().firstOrNull { it.turnajId == turnajId } ?: return
        if (turnaj.stav == "DOKONCENO") return

        if (turnaj.typHry == TypHry.MARIAS) {
            ulozTurnaj(turnaj.copy(stav = "DOKONCENO", celkovyVitez = vitezJmeno))
            return
        }

        val upraveneZapasy = turnaj.zapasy.map { z ->
            if (z.slot == slotZapasu && z.vitezId.isEmpty()) {
                z.copy(vitezId = vitezDeviceId, vitezJmeno = vitezJmeno)
            } else {
                z
            }
        }.toMutableList()

        if (slotZapasu == 2) {
            ulozTurnaj(
                turnaj.copy(
                    stav = "DOKONCENO",
                    zapasy = upraveneZapasy,
                    celkovyVitez = vitezJmeno
                )
            )
            return
        }

        val sfA = upraveneZapasy.firstOrNull { it.slot == 0 }
        val sfB = upraveneZapasy.firstOrNull { it.slot == 1 }
        val finale = upraveneZapasy.firstOrNull { it.slot == 2 }

        if (sfA != null && sfB != null && finale != null) {
            var noveFinale = finale.copy(
                hrac1Id = sfA.vitezId.ifEmpty { finale.hrac1Id },
                hrac1Jmeno = sfA.vitezJmeno.ifEmpty { finale.hrac1Jmeno },
                hrac2Id = sfB.vitezId.ifEmpty { finale.hrac2Id },
                hrac2Jmeno = sfB.vitezJmeno.ifEmpty { finale.hrac2Jmeno }
            )
            if (sfA.vitezId.isNotEmpty() && sfB.vitezId.isNotEmpty() && noveFinale.roomId.isEmpty()) {
                val h1 = turnaj.ucastnici.firstOrNull { it.deviceId == sfA.vitezId }
                    ?: HracUStolu(sfA.vitezId, sfA.vitezJmeno, 1000)
                val h2 = turnaj.ucastnici.firstOrNull { it.deviceId == sfB.vitezId }
                    ?: HracUStolu(sfB.vitezId, sfB.vitezJmeno, 1000)
                val finalovyStul = vytvorStul(
                    nazevStolu = "${turnaj.nazev} — FINÁLE",
                    typHry = turnaj.typHry,
                    pocetHracu = 2,
                    turnajId = turnaj.turnajId,
                    slotZapasu = 2,
                    predvyplneniHraci = listOf(h1, h2)
                )
                noveFinale = noveFinale.copy(roomId = finalovyStul?.roomId ?: "")
            }
            val idxFinale = upraveneZapasy.indexOfFirst { it.slot == 2 }
            if (idxFinale >= 0) upraveneZapasy[idxFinale] = noveFinale
        }

        ulozTurnaj(turnaj.copy(zapasy = upraveneZapasy))
    }
}
