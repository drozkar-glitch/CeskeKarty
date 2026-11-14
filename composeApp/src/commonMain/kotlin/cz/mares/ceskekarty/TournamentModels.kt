package cz.mares.ceskekarty

import kotlin.math.max

const val CIL_VITEZNYCH_BITEV_DUELU = 3
const val MAX_HER_DUEL_SERIE = 5
const val POCET_ROZDANI_MARIAS_TURNAJ = 6

fun jeBotDeviceId(deviceId: String): Boolean = deviceId.startsWith("bot_")

val TURNAJOVI_BOTI: List<Pair<String, String>> = listOf(
    "bot_karel" to "Karel (Bot)",
    "bot_pepa" to "Pepa (Bot)",
    "bot_franta" to "Franta (Bot)",
    "bot_vasek" to "Vašek (Bot)",
    "bot_tonda" to "Tonda (Bot)",
    "bot_honza" to "Honza (Bot)",
    "bot_lojza" to "Lojza (Bot)",
    "bot_standa" to "Standa (Bot)",
    "bot_venca" to "Venca (Bot)"
)

data class PrahyCas(
    val rok: Int,
    val mesic: Int,
    val den: Int,
    val hodina: Int,
    val minuta: Int,
    val sekunda: Int,
    val denVTydnu: Int,
    val epochDen: Long
)

data class SablonaDennihoTurnaje(
    val kodSlotu: String,
    val hodina: Int,
    val minuta: Int,
    val nazev: String,
    val typHry: TypHry,
    val kapacita: Int,
    val vkladTereziany: Int,
    val dotaceSine: Int
)

val DENNI_ROZVRH_TURNAJU: List<SablonaDennihoTurnaje> = listOf(
    SablonaDennihoTurnaje(
        kodSlotu = "1900_PRSI",
        hodina = 19,
        minuta = 0,
        nazev = "Prší • Večerní pohár",
        typHry = TypHry.PRSI,
        kapacita = 8,
        vkladTereziany = 40,
        dotaceSine = 160
    ),
    SablonaDennihoTurnaje(
        kodSlotu = "2000_SEDMA",
        hodina = 20,
        minuta = 0,
        nazev = "Sedma • Večerní pohár",
        typHry = TypHry.SEDMA,
        kapacita = 8,
        vkladTereziany = 50,
        dotaceSine = 200
    ),
    SablonaDennihoTurnaje(
        kodSlotu = "2030_MARIAS",
        hodina = 20,
        minuta = 30,
        nazev = "Mariáš • Královský pohár",
        typHry = TypHry.MARIAS,
        kapacita = 9,
        vkladTereziany = 60,
        dotaceSine = 240
    )
)

fun normalizujNazevTurnaje(puvodni: String, typHry: TypHry): String {
    val cisty = puvodni.trim()
    return when {
        cisty.startsWith("Večerní pohár: Prší") -> "Prší • Večerní pohár"
        cisty.startsWith("Večerní pohár: Sedma") -> "Sedma • Večerní pohár"
        cisty.startsWith("Královský pohár:") -> "Mariáš • Královský pohár"
        cisty.isEmpty() -> when (typHry) {
            TypHry.PRSI -> "Prší • Večerní pohár"
            TypHry.SEDMA -> "Sedma • Večerní pohár"
            TypHry.MARIAS -> "Mariáš • Královský pohár"
        }
        else -> cisty
    }
}

fun formatujSkoreDoPavouka(typHry: TypHry, hodnota: Int): String {
    return if (typHry == TypHry.MARIAS) {
        if (hodnota > 0) "+${hodnota}b" else "${hodnota}b"
    } else {
        hodnota.toString()
    }
}

data class HracVeSlotuPavouka(
    val deviceId: String,
    val prezdivka: String,
    val jeBot: Boolean = jeBotDeviceId(deviceId),
    val jeVolneMisto: Boolean = deviceId.isEmpty(),
    val skoreText: String = ""
)

data class UzelZapasuPavouka(
    val slot: Int,
    val kolo: Int,
    val nazevZapasu: String,
    val planovanyCasMs: Long,
    val hraci: List<HracVeSlotuPavouka>,
    val vitezId: String,
    val vitezJmeno: String,
    val roomId: String,
    val stavZapasu: String
) {
    fun obsahujeHrace(deviceId: String): Boolean {
        return deviceId.isNotEmpty() && hraci.any { it.deviceId == deviceId }
    }
}

data class PlanovanyTurnaj(
    val turnajId: String,
    val nazev: String,
    val typHry: TypHry,
    val variantaPravidel: VariantaPravidel,
    val kapacita: Int,
    val casStartuMs: Long,
    val vkladTereziany: Int,
    val dotaceSine: Int,
    val stav: String,
    val ucastnici: List<HracUStolu>,
    val uzlyPavouka: List<UzelZapasuPavouka>,
    val celkovyVitezId: String,
    val celkovyVitezJmeno: String,
    val druheMistoId: String,
    val druheMistoJmeno: String,
    val vyplaceneIdHracu: List<String>,
    val vytvorenoMs: Long
) {
    val celkovyBankTerezianu: Int
        get() = dotaceSine + ucastnici.count { !jeBotDeviceId(it.deviceId) } * vkladTereziany

    val odmenaPrvniMisto: Int
        get() = ((celkovyBankTerezianu * 65) / 100).coerceAtLeast(vkladTereziany * 2)

    val odmenaDruheMisto: Int
        get() = ((celkovyBankTerezianu * 35) / 100).coerceAtLeast(vkladTereziany)

    fun jeHracPrihlasen(deviceId: String): Boolean {
        return deviceId.isNotEmpty() && ucastnici.any { it.deviceId == deviceId }
    }

    fun aktivniZapasHrace(deviceId: String): UzelZapasuPavouka? {
        if (stav != "PROBIHA" || deviceId.isEmpty()) return null
        return uzlyPavouka.firstOrNull { uzel ->
            uzel.obsahujeHrace(deviceId) &&
                uzel.vitezId.isEmpty() &&
                uzel.roomId.isNotEmpty()
        }
    }

    fun ziskejUzlyProZobrazeni(): List<UzelZapasuPavouka> {
        return if (uzlyPavouka.isNotEmpty()) {
            uzlyPavouka
        } else {
            vygenerujNahledPavoukaPredStartem(this)
        }
    }
}

object TurnajKalendar {

    fun datumNaEpochDen(rok: Int, mesic: Int, den: Int): Long {
        val y = (if (mesic <= 2) rok - 1 else rok).toLong()
        val era = if (y >= 0L) y / 400L else (y - 399L) / 400L
        val yoe = y - era * 400L
        val m = mesic.toLong()
        val doy = (153L * (if (m > 2L) m - 3L else m + 9L) + 2L) / 5L + den.toLong() - 1L
        val doe = yoe * 365L + yoe / 4L - yoe / 100L + doy
        return era * 146097L + doe - 719468L
    }

    fun epochDenNaDatum(zIn: Long): Triple<Int, Int, Int> {
        val z = zIn + 719468L
        val era = if (z >= 0L) z / 146097L else (z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        val y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = if (mp < 10L) mp + 3L else mp - 9L
        val rok = (if (m <= 2L) y + 1L else y).toInt()
        return Triple(rok, m.toInt(), d.toInt())
    }

    private fun posledniNedeleVMesici(rok: Int, mesic: Int): Int {
        val epoch31 = datumNaEpochDen(rok, mesic, 31)
        val dow = ((epoch31 + 3L).mod(7L) + 1L).toInt()
        return if (dow == 7) 31 else 31 - dow
    }

    fun prahaPosunUtcMs(utcMs: Long): Long {
        val pribliznyDen = if (utcMs >= 0L) utcMs / 86_400_000L else (utcMs - 86_399_999L) / 86_400_000L
        val (rok, _, _) = epochDenNaDatum(pribliznyDen)
        val nedeleBrezen = posledniNedeleVMesici(rok, 3)
        val nedeleRijen = posledniNedeleVMesici(rok, 10)
        val zacatekLetnihoUtc = datumNaEpochDen(rok, 3, nedeleBrezen) * 86_400_000L + 3_600_000L
        val konecLetnihoUtc = datumNaEpochDen(rok, 10, nedeleRijen) * 86_400_000L + 3_600_000L
        return if (utcMs in zacatekLetnihoUtc until konecLetnihoUtc) 7_200_000L else 3_600_000L
    }

    fun utcMsNaPrahaCas(utcMs: Long): PrahyCas {
        val mistniMs = utcMs + prahaPosunUtcMs(utcMs)
        val epochDen = if (mistniMs >= 0L) mistniMs / 86_400_000L else (mistniMs - 86_399_999L) / 86_400_000L
        val zbytekDneMs = (mistniMs - epochDen * 86_400_000L).coerceAtLeast(0L)
        val (rok, mesic, den) = epochDenNaDatum(epochDen)
        val celkemSekund = (zbytekDneMs / 1000L).toInt()
        val hodina = celkemSekund / 3600
        val minuta = (celkemSekund % 3600) / 60
        val sekunda = celkemSekund % 60
        val denVTydnu = ((epochDen + 3L).mod(7L) + 1L).toInt()
        return PrahyCas(
            rok = rok,
            mesic = mesic,
            den = den,
            hodina = hodina,
            minuta = minuta,
            sekunda = sekunda,
            denVTydnu = denVTydnu,
            epochDen = epochDen
        )
    }

    fun prahaCasNaUtcMs(rok: Int, mesic: Int, den: Int, hodina: Int, minuta: Int): Long {
        val denMs = datumNaEpochDen(rok, mesic, den) * 86_400_000L
        val casMs = (hodina * 3600L + minuta * 60L) * 1000L
        val odhadZimniUtc = denMs + casMs - 3_600_000L
        val posun = prahaPosunUtcMs(odhadZimniUtc)
        return denMs + casMs - posun
    }

    fun nazevDneVTydnu(denVTydnu: Int): String = when (denVTydnu) {
        1 -> "Po"
        2 -> "Út"
        3 -> "St"
        4 -> "Čt"
        5 -> "Pá"
        6 -> "So"
        else -> "Ne"
    }

    fun formatujHodinuMinutu(utcMs: Long): String {
        val p = utcMsNaPrahaCas(utcMs)
        return "${p.hodina.toString().padStart(2, '0')}:${p.minuta.toString().padStart(2, '0')}"
    }

    fun formatujTerminTurnaje(startMs: Long, nyniMs: Long): String {
        val startP = utcMsNaPrahaCas(startMs)
        val nyniP = utcMsNaPrahaCas(nyniMs)
        val casTxt = "${startP.hodina.toString().padStart(2, '0')}:${startP.minuta.toString().padStart(2, '0')}"
        val datumTxt = "${startP.den}.${startP.mesic}."
        return when (startP.epochDen - nyniP.epochDen) {
            0L -> "Dnes $datumTxt v $casTxt"
            1L -> "Zítra $datumTxt v $casTxt"
            -1L -> "Včera $datumTxt v $casTxt"
            else -> "${nazevDneVTydnu(startP.denVTydnu)} $datumTxt v $casTxt"
        }
    }

    fun formatujOdpocetDoStartu(zbyvaMs: Long): String {
        val sek = max(0L, zbyvaMs / 1000L)
        val hod = sek / 3600L
        val min = (sek % 3600L) / 60L
        val s = sek % 60L
        return when {
            hod > 0L -> "${hod}h ${min.toString().padStart(2, '0')}m"
            min > 0L -> "${min}m ${s.toString().padStart(2, '0')}s"
            else -> "${s}s"
        }
    }

    fun variantaProEpochDen(epochDen: Long, indexSlotu: Int): VariantaPravidel {
        return if ((epochDen + indexSlotu).mod(3L) == 2L) {
            VariantaPravidel.SVAZOVA
        } else {
            VariantaPravidel.HOSPODSKA
        }
    }
}

private val NASAZENI_8_HRACU = intArrayOf(0, 4, 2, 6, 1, 5, 3, 7)
private val NASAZENI_9_MARIAS = intArrayOf(0, 3, 6, 1, 4, 7, 2, 5, 8)

fun vygenerujNahledPavoukaPredStartem(turnaj: PlanovanyTurnaj): List<UzelZapasuPavouka> {
    if (turnaj.typHry == TypHry.MARIAS) {
        val sedadla = Array(9) { HracVeSlotuPavouka("", "Volné místo (Bot)", jeBot = false, jeVolneMisto = true) }
        turnaj.ucastnici.take(9).forEachIndexed { idx, hrac ->
            val cil = NASAZENI_9_MARIAS[idx]
            sedadla[cil] = HracVeSlotuPavouka(
                deviceId = hrac.deviceId,
                prezdivka = hrac.prezdivka,
                jeBot = jeBotDeviceId(hrac.deviceId),
                jeVolneMisto = false
            )
        }
        val stoly = listOf("Semifinále A", "Semifinále B", "Semifinále C")
        val prvniKolo = (0..2).map { s ->
            UzelZapasuPavouka(
                slot = s,
                kolo = 0,
                nazevZapasu = stoly[s],
                planovanyCasMs = turnaj.casStartuMs,
                hraci = listOf(sedadla[s * 3], sedadla[s * 3 + 1], sedadla[s * 3 + 2]),
                vitezId = "",
                vitezJmeno = "",
                roomId = "",
                stavZapasu = "CEKA"
            )
        }
        val finale = UzelZapasuPavouka(
            slot = 3,
            kolo = 1,
            nazevZapasu = "Finálový stůl",
            planovanyCasMs = turnaj.casStartuMs + 25 * 60_000L,
            hraci = listOf(
                HracVeSlotuPavouka("", "Vítěz SF A", jeBot = false, jeVolneMisto = true),
                HracVeSlotuPavouka("", "Vítěz SF B", jeBot = false, jeVolneMisto = true),
                HracVeSlotuPavouka("", "Vítěz SF C", jeBot = false, jeVolneMisto = true)
            ),
            vitezId = "",
            vitezJmeno = "",
            roomId = "",
            stavZapasu = "CEKA"
        )
        return prvniKolo + finale
    }

    val sedadla = Array(8) { HracVeSlotuPavouka("", "Volné místo (Bot)", jeBot = false, jeVolneMisto = true) }
    turnaj.ucastnici.take(8).forEachIndexed { idx, hrac ->
        val cil = NASAZENI_8_HRACU[idx]
        sedadla[cil] = HracVeSlotuPavouka(
            deviceId = hrac.deviceId,
            prezdivka = hrac.prezdivka,
            jeBot = jeBotDeviceId(hrac.deviceId),
            jeVolneMisto = false
        )
    }

    val ctvrtfinale = (0..3).map { s ->
        UzelZapasuPavouka(
            slot = s,
            kolo = 0,
            nazevZapasu = "Čtvrtfinále ${s + 1}",
            planovanyCasMs = turnaj.casStartuMs,
            hraci = listOf(sedadla[s * 2], sedadla[s * 2 + 1]),
            vitezId = "",
            vitezJmeno = "",
            roomId = "",
            stavZapasu = "CEKA"
        )
    }
    val semifinale = (0..1).map { s ->
        UzelZapasuPavouka(
            slot = 4 + s,
            kolo = 1,
            nazevZapasu = "Semifinále ${s + 1}",
            planovanyCasMs = turnaj.casStartuMs + 20 * 60_000L,
            hraci = listOf(
                HracVeSlotuPavouka("", "Vítěz ČF ${s * 2 + 1}", jeBot = false, jeVolneMisto = true),
                HracVeSlotuPavouka("", "Vítěz ČF ${s * 2 + 2}", jeBot = false, jeVolneMisto = true)
            ),
            vitezId = "",
            vitezJmeno = "",
            roomId = "",
            stavZapasu = "CEKA"
        )
    }
    val finale = UzelZapasuPavouka(
        slot = 6,
        kolo = 2,
        nazevZapasu = "Finále",
        planovanyCasMs = turnaj.casStartuMs + 40 * 60_000L,
        hraci = listOf(
            HracVeSlotuPavouka("", "Vítěz SF 1", jeBot = false, jeVolneMisto = true),
            HracVeSlotuPavouka("", "Vítěz SF 2", jeBot = false, jeVolneMisto = true)
        ),
        vitezId = "",
        vitezJmeno = "",
        roomId = "",
        stavZapasu = "CEKA"
    )
    return ctvrtfinale + semifinale + finale
}

fun sestavStartovniUzlySHraciABoty(turnaj: PlanovanyTurnaj): Pair<List<HracUStolu>, List<UzelZapasuPavouka>> {
    val zivi = turnaj.ucastnici.filterNot { jeBotDeviceId(it.deviceId) }
    val dostupniBoti = TURNAJOVI_BOTI.map { (id, jm) -> HracUStolu(id, jm, STARTOVNI_TEREZIANY) }
    var botIdx = 0

    if (turnaj.typHry == TypHry.MARIAS) {
        val plnych9 = Array<HracUStolu?>(9) { null }
        zivi.take(9).forEachIndexed { idx, h ->
            plnych9[NASAZENI_9_MARIAS[idx]] = h
        }
        for (i in 0 until 9) {
            if (plnych9[i] == null) {
                plnych9[i] = dostupniBoti[botIdx % dostupniBoti.size]
                botIdx++
            }
        }
        val vsichni = plnych9.filterNotNull()
        val stoly = listOf("Semifinále A", "Semifinále B", "Semifinále C")
        val prvniKolo = (0..2).map { s ->
            val h0 = vsichni[s * 3]
            val h1 = vsichni[s * 3 + 1]
            val h2 = vsichni[s * 3 + 2]
            UzelZapasuPavouka(
                slot = s,
                kolo = 0,
                nazevZapasu = stoly[s],
                planovanyCasMs = turnaj.casStartuMs,
                hraci = listOf(
                    HracVeSlotuPavouka(h0.deviceId, h0.prezdivka),
                    HracVeSlotuPavouka(h1.deviceId, h1.prezdivka),
                    HracVeSlotuPavouka(h2.deviceId, h2.prezdivka)
                ),
                vitezId = "",
                vitezJmeno = "",
                roomId = "",
                stavZapasu = "CEKA"
            )
        }
        val finale = UzelZapasuPavouka(
            slot = 3,
            kolo = 1,
            nazevZapasu = "Finálový stůl",
            planovanyCasMs = turnaj.casStartuMs + 25 * 60_000L,
            hraci = listOf(
                HracVeSlotuPavouka("", "Vítěz SF A", jeBot = false, jeVolneMisto = true),
                HracVeSlotuPavouka("", "Vítěz SF B", jeBot = false, jeVolneMisto = true),
                HracVeSlotuPavouka("", "Vítěz SF C", jeBot = false, jeVolneMisto = true)
            ),
            vitezId = "",
            vitezJmeno = "",
            roomId = "",
            stavZapasu = "CEKA"
        )
        return vsichni to (prvniKolo + finale)
    }

    val plnych8 = Array<HracUStolu?>(8) { null }
    zivi.take(8).forEachIndexed { idx, h ->
        plnych8[NASAZENI_8_HRACU[idx]] = h
    }
    for (i in 0 until 8) {
        if (plnych8[i] == null) {
            plnych8[i] = dostupniBoti[botIdx % dostupniBoti.size]
            botIdx++
        }
    }
    val vsichni = plnych8.filterNotNull()
    val ctvrtfinale = (0..3).map { s ->
        val h0 = vsichni[s * 2]
        val h1 = vsichni[s * 2 + 1]
        UzelZapasuPavouka(
            slot = s,
            kolo = 0,
            nazevZapasu = "Čtvrtfinále ${s + 1}",
            planovanyCasMs = turnaj.casStartuMs,
            hraci = listOf(
                HracVeSlotuPavouka(h0.deviceId, h0.prezdivka),
                HracVeSlotuPavouka(h1.deviceId, h1.prezdivka)
            ),
            vitezId = "",
            vitezJmeno = "",
            roomId = "",
            stavZapasu = "CEKA"
        )
    }
    val semifinale = (0..1).map { s ->
        UzelZapasuPavouka(
            slot = 4 + s,
            kolo = 1,
            nazevZapasu = "Semifinále ${s + 1}",
            planovanyCasMs = turnaj.casStartuMs + 20 * 60_000L,
            hraci = listOf(
                HracVeSlotuPavouka("", "Vítěz ČF ${s * 2 + 1}", jeBot = false, jeVolneMisto = true),
                HracVeSlotuPavouka("", "Vítěz ČF ${s * 2 + 2}", jeBot = false, jeVolneMisto = true)
            ),
            vitezId = "",
            vitezJmeno = "",
            roomId = "",
            stavZapasu = "CEKA"
        )
    }
    val finale = UzelZapasuPavouka(
        slot = 6,
        kolo = 2,
        nazevZapasu = "Finále",
        planovanyCasMs = turnaj.casStartuMs + 40 * 60_000L,
        hraci = listOf(
            HracVeSlotuPavouka("", "Vítěz SF 1", jeBot = false, jeVolneMisto = true),
            HracVeSlotuPavouka("", "Vítěz SF 2", jeBot = false, jeVolneMisto = true)
        ),
        vitezId = "",
        vitezJmeno = "",
        roomId = "",
        stavZapasu = "CEKA"
    )
    return vsichni to (ctvrtfinale + semifinale + finale)
}

private fun ocistiOddelovaceTurnaje(text: String): String {
    return text
        .replace("~", " ")
        .replace(";", " ")
        .replace("^", " ")
        .replace("!", " ")
}

fun serializujPlanovanyTurnaj(t: PlanovanyTurnaj): String {
    val ucStr = t.ucastnici.joinToString(";") { h ->
        "${ocistiOddelovaceTurnaje(h.deviceId)}~${ocistiOddelovaceTurnaje(h.prezdivka)}~${h.tereziany}"
    }
    val uzlyStr = t.uzlyPavouka.joinToString(";") { u ->
        val hrStr = u.hraci.joinToString("^") { hs ->
            listOf(
                ocistiOddelovaceTurnaje(hs.deviceId),
                ocistiOddelovaceTurnaje(hs.prezdivka),
                if (hs.jeVolneMisto) "1" else "0",
                ocistiOddelovaceTurnaje(hs.skoreText)
            ).joinToString("!")
        }
        listOf(
            u.slot.toString(),
            u.kolo.toString(),
            ocistiOddelovaceTurnaje(u.nazevZapasu),
            u.planovanyCasMs.toString(),
            hrStr,
            ocistiOddelovaceTurnaje(u.vitezId),
            ocistiOddelovaceTurnaje(u.vitezJmeno),
            ocistiOddelovaceTurnaje(u.roomId),
            ocistiOddelovaceTurnaje(u.stavZapasu)
        ).joinToString("~")
    }
    return serializujMapuPoli(
        listOf(
            "ver" to "2",
            "tid" to t.turnajId,
            "naz" to normalizujNazevTurnaje(t.nazev, t.typHry),
            "typ" to t.typHry.idHry,
            "vp" to t.variantaPravidel.kod,
            "kap" to t.kapacita.toString(),
            "cs" to t.casStartuMs.toString(),
            "vkl" to t.vkladTereziany.toString(),
            "dot" to t.dotaceSine.toString(),
            "st" to t.stav,
            "uc" to ucStr,
            "uz" to uzlyStr,
            "vid" to t.celkovyVitezId,
            "vjm" to t.celkovyVitezJmeno,
            "d2id" to t.druheMistoId,
            "d2jm" to t.druheMistoJmeno,
            "vyp" to t.vyplaceneIdHracu.joinToString(","),
            "vyt" to t.vytvorenoMs.toString()
        )
    )
}

fun deserializujPlanovanyTurnaj(data: String): PlanovanyTurnaj? {
    if (data.isBlank()) return null
    val r = PoleStavuReader(data)
    val verze = r.text("ver")
    if (verze != "2" && verze != "3") return null
    val tid = r.text("tid")
    if (tid.isBlank()) return null
    val typ = TypHry.dleId(r.text("typ", "PRSI"))
    val ucText = r.text("uc")
    val ucastnici = if (ucText.isBlank()) {
        emptyList()
    } else {
        ucText.split(";").mapNotNull { pol ->
            val c = pol.split("~")
            if (c.size < 3) return@mapNotNull null
            HracUStolu(c[0], c[1], c[2].toIntOrNull() ?: STARTOVNI_TEREZIANY)
        }
    }
    val uzText = r.text("uz")
    val uzly = if (uzText.isBlank()) {
        emptyList()
    } else {
        uzText.split(";").mapNotNull { uPol ->
            val c = uPol.split("~")
            if (c.size < 9) return@mapNotNull null
            val hraciUzlu = if (c[4].isBlank()) {
                emptyList()
            } else {
                c[4].split("^").mapNotNull { hp ->
                    val hc = hp.split("!")
                    if (hc.size < 3) return@mapNotNull null
                    val cisteJmeno = if (hc[1] == "Volné místo (nebo Bot)") "Volné místo (Bot)" else hc[1]
                    HracVeSlotuPavouka(
                        deviceId = hc[0],
                        prezdivka = cisteJmeno,
                        jeBot = jeBotDeviceId(hc[0]),
                        jeVolneMisto = hc[2] == "1" || hc[0].isEmpty(),
                        skoreText = hc.getOrElse(3) { "" }
                    )
                }
            }
            val zkracenyNazevUzlu = c[2]
                .replace("Semifinále — Stůl ", "Semifinále ")
            UzelZapasuPavouka(
                slot = c[0].toIntOrNull() ?: 0,
                kolo = c[1].toIntOrNull() ?: 0,
                nazevZapasu = zkracenyNazevUzlu,
                planovanyCasMs = c[3].toLongOrNull() ?: 0L,
                hraci = hraciUzlu,
                vitezId = c[5],
                vitezJmeno = c[6],
                roomId = c[7],
                stavZapasu = c[8]
            )
        }
    }
    val vypText = r.text("vyp")
    val vyplacene = if (vypText.isBlank()) emptyList() else vypText.split(",").filter { it.isNotBlank() }

    return PlanovanyTurnaj(
        turnajId = tid,
        nazev = normalizujNazevTurnaje(r.text("naz", ""), typ),
        typHry = typ,
        variantaPravidel = VariantaPravidel.dleKodu(r.text("vp", VariantaPravidel.HOSPODSKA.kod)),
        kapacita = r.cislo("kap", if (typ == TypHry.MARIAS) 9 else 8),
        casStartuMs = r.text("cs").toLongOrNull() ?: 0L,
        vkladTereziany = r.cislo("vkl", 40),
        dotaceSine = r.cislo("dot", 160),
        stav = r.text("st", "REGISTRACE"),
        ucastnici = ucastnici,
        uzlyPavouka = uzly,
        celkovyVitezId = r.text("vid"),
        celkovyVitezJmeno = r.text("vjm"),
        druheMistoId = r.text("d2id"),
        druheMistoJmeno = r.text("d2jm"),
        vyplaceneIdHracu = vyplacene,
        vytvorenoMs = r.text("vyt").toLongOrNull() ?: 0L
    )
}
