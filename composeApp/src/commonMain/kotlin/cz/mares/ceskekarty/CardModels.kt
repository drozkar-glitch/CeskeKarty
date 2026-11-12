package cz.mares.ceskekarty

import kotlin.random.Random

enum class TypHry(
    val idHry: String,
    val nazev: String,
    val povolenePoctyHracu: List<Int>,
    val kratkyPopis: String
) {
    MARIAS(
        idHry = "MARIAS",
        nazev = "Volený Mariáš",
        povolenePoctyHracu = listOf(3),
        kratkyPopis = "Klasika pro 3 hráče s volbou trumfů, hláškami 20/40, Sedmou, Betlem, Durchem a flekováním."
    ),
    PRSI(
        idHry = "PRSI",
        nazev = "Prší",
        povolenePoctyHracu = listOf(2, 3, 4),
        kratkyPopis = "Vykládací hra s přebíjením sedmiček i es a změnou barvy svrškem."
    ),
    SEDMA(
        idHry = "SEDMA",
        nazev = "Sedma",
        povolenePoctyHracu = listOf(2, 4),
        kratkyPopis = "Štychová hra 1 na 1 nebo čtyřhra 2 na 2 do kříže s divokými sedmičkami a spálenou."
    );

    companion object {
        fun dleId(id: String): TypHry {
            return entries.firstOrNull { it.idHry.equals(id, ignoreCase = true) } ?: PRSI
        }
    }
}

enum class BarvaKarty(
    val ceskyNazev: String,
    val privlastekZensky: String,
    val privlastekMuzsky: String,
    val poradiRuky: Int
) {
    CERVENE("Červené", "Červená", "Červený", 0),
    KULE("Kule", "Kulová", "Kulový", 1),
    ZALUDY("Žaludy", "Žaludská", "Žaludský", 2),
    ZELENE("Zelené", "Zelená", "Zelený", 3);

    companion object {
        fun dleNazvu(text: String): BarvaKarty? {
            return entries.firstOrNull { it.name.equals(text.trim(), ignoreCase = true) }
        }
    }
}

enum class HodnotaKarty(
    val kratkyZnak: String,
    val ceskyNazev: String,
    val muzskyRod: Boolean,
    val bodyMariasSedma: Int,
    val silaMarias: Int,
    val silaBetlDurch: Int
) {
    R7("7", "sedma", false, 0, 1, 1),
    R8("8", "osma", false, 0, 2, 2),
    R9("9", "devítka", false, 0, 3, 3),
    R10("10", "desítka", false, 10, 7, 4),
    SPODEK("Sp", "spodek", true, 0, 4, 5),
    SVRSEK("Sv", "svršek", true, 0, 5, 6),
    KRAL("Kr", "král", true, 0, 6, 7),
    ESO("A", "eso", false, 10, 8, 8);

    companion object {
        fun dleNazvu(text: String): HodnotaKarty? {
            return entries.firstOrNull { it.name.equals(text.trim(), ignoreCase = true) }
        }
    }
}

data class Karta(
    val barva: BarvaKarty,
    val hodnota: HodnotaKarty
) {
    val kod: String
        get() = "${barva.name}_${hodnota.name}"

    fun silaProMarias(betlNeboDurch: Boolean): Int {
        return if (betlNeboDurch) hodnota.silaBetlDurch else hodnota.silaMarias
    }

    fun celyNazev(): String {
        return when (hodnota) {
            HodnotaKarty.ESO -> when (barva) {
                BarvaKarty.CERVENE -> "Červené eso"
                BarvaKarty.KULE -> "Kulové eso"
                BarvaKarty.ZALUDY -> "Žaludské eso"
                BarvaKarty.ZELENE -> "Zelené eso"
            }
            else -> {
                val privlastek = if (hodnota.muzskyRod) barva.privlastekMuzsky else barva.privlastekZensky
                "$privlastek ${hodnota.ceskyNazev}"
            }
        }
    }

    companion object {
        fun zKodu(kod: String): Karta? {
            val cisty = kod.trim()
            if (cisty.isEmpty()) return null
            val indexPodtrzitka = cisty.indexOf('_')
            if (indexPodtrzitka <= 0 || indexPodtrzitka >= cisty.length - 1) return null
            val b = BarvaKarty.dleNazvu(cisty.substring(0, indexPodtrzitka)) ?: return null
            val h = HodnotaKarty.dleNazvu(cisty.substring(indexPodtrzitka + 1)) ?: return null
            return Karta(b, h)
        }

        fun vsech32Karet(): List<Karta> {
            val seznam = ArrayList<Karta>(32)
            for (b in BarvaKarty.entries) {
                for (h in HodnotaKarty.entries) {
                    seznam.add(Karta(b, h))
                }
            }
            return seznam
        }

        fun vytvorZamichanyBalicek(): MutableList<Karta> {
            val balicek = vsech32Karet().toMutableList()
            for (i in balicek.lastIndex downTo 1) {
                val j = Random.nextInt(i + 1)
                val docasna = balicek[i]
                balicek[i] = balicek[j]
                balicek[j] = docasna
            }
            return balicek
        }

        fun seradRuku(
            karty: List<Karta>,
            trumf: BarvaKarty? = null,
            betlNeboDurch: Boolean = false
        ): List<Karta> {
            return karty.sortedWith(
                compareBy<Karta>(
                    { if (trumf != null && it.barva == trumf) -1 else it.barva.poradiRuky },
                    { -it.silaProMarias(betlNeboDurch) }
                )
            )
        }
    }
}

fun kartyNaRetezec(karty: List<Karta>): String {
    return karty.joinToString(",") { it.kod }
}

fun retezecNaKarty(retezec: String): List<Karta> {
    if (retezec.isBlank()) return emptyList()
    return retezec.split(",")
        .mapNotNull { Karta.zKodu(it) }
}

data class HranaKartaStolu(
    val indexHrace: Int,
    val jmenoHrace: String,
    val karta: Karta
)

fun stychNaRetezec(stych: List<HranaKartaStolu>): String {
    return stych.joinToString(";") { polozka ->
        val bezpecneJmeno = polozka.jmenoHrace.replace(";", " ").replace(":", " ").replace("|", " ")
        "${polozka.indexHrace}:${bezpecneJmeno}:${polozka.karta.kod}"
    }
}

fun retezecNaStych(retezec: String): List<HranaKartaStolu> {
    if (retezec.isBlank()) return emptyList()
    return retezec.split(";").mapNotNull { zaznam ->
        val casti = zaznam.split(":")
        if (casti.size < 3) return@mapNotNull null
        val idx = casti[0].toIntOrNull() ?: return@mapNotNull null
        val karta = Karta.zKodu(casti[2]) ?: return@mapNotNull null
        HranaKartaStolu(idx, casti[1], karta)
    }
}

data class PolozkaVyuctovani(
    val popis: String,
    val hodnotaText: String,
    val jeZvyraznena: Boolean = false
)

data class ZmenaBoduHrace(
    val indexHrace: Int,
    val jmenoHrace: String,
    val bodyZapasu: Int,
    val zmenaElo: Int,
    val jeVitez: Boolean
)

data class VysledekPartie(
    val typHry: TypHry,
    val nadpis: String,
    val podnadpis: String,
    val polozky: List<PolozkaVyuctovani>,
    val hraci: List<ZmenaBoduHrace>
)

fun zakodujHodnotuPole(text: String): String {
    return text
        .replace("%", "%25")
        .replace("\n", "%0A")
        .replace("=", "%3D")
        .replace("|", "%7C")
}

fun dekodujHodnotuPole(text: String): String {
    return text
        .replace("%7C", "|")
        .replace("%3D", "=")
        .replace("%0A", "\n")
        .replace("%25", "%")
}

fun serializujMapuPoli(pole: List<Pair<String, String>>): String {
    return pole.joinToString("|") { (k, v) ->
        "${k}=${zakodujHodnotuPole(v)}"
    }
}

class PoleStavuReader(serializovanyText: String) {
    private val klice = ArrayList<String>()
    private val hodnoty = ArrayList<String>()

    init {
        if (serializovanyText.isNotBlank()) {
            val zaznamy = serializovanyText.split("|")
            for (z in zaznamy) {
                val idx = z.indexOf('=')
                if (idx > 0) {
                    klice.add(z.substring(0, idx))
                    hodnoty.add(dekodujHodnotuPole(z.substring(idx + 1)))
                }
            }
        }
    }

    fun text(klic: String, vychozi: String = ""): String {
        val i = klice.indexOf(klic)
        return if (i >= 0) hodnoty[i] else vychozi
    }

    fun cislo(klic: String, vychozi: Int = 0): Int {
        return text(klic, "").toIntOrNull() ?: vychozi
    }

    fun pravda(klic: String, vychozi: Boolean = false): Boolean {
        val t = text(klic, "")
        return if (t.isEmpty()) vychozi else t == "1" || t.equals("true", ignoreCase = true)
    }
}
