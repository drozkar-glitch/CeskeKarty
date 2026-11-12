package cz.mares.ceskekarty

enum class FazeMariase {
    VOLBA_TRUMFU,
    ODKLADANI_TALONU,
    FLEKOVANI,
    SEHRAVKA,
    KONEC
}

enum class ZavazekMariase(
    val nazev: String,
    val jeBezTrumfu: Boolean,
    val hlasiSto: Boolean,
    val hlasiSedmu: Boolean,
    val hlasiDveSedmy: Boolean
) {
    HRA("Hra", jeBezTrumfu = false, hlasiSto = false, hlasiSedmu = false, hlasiDveSedmy = false),
    HRA_A_SEDMA("Hra a sedma", jeBezTrumfu = false, hlasiSto = false, hlasiSedmu = true, hlasiDveSedmy = false),
    HRA_A_DVE_SEDMY("Hra a dvě sedmy", jeBezTrumfu = false, hlasiSto = false, hlasiSedmu = true, hlasiDveSedmy = true),
    STO("Stovka (100)", jeBezTrumfu = false, hlasiSto = true, hlasiSedmu = false, hlasiDveSedmy = false),
    STO_A_SEDMA("Sto a sedma", jeBezTrumfu = false, hlasiSto = true, hlasiSedmu = true, hlasiDveSedmy = false),
    STO_A_DVE_SEDMY("Sto a dvě sedmy", jeBezTrumfu = false, hlasiSto = true, hlasiSedmu = true, hlasiDveSedmy = true),
    BETL("Betl", jeBezTrumfu = true, hlasiSto = false, hlasiSedmu = false, hlasiDveSedmy = false),
    DURCH("Durch", jeBezTrumfu = true, hlasiSto = false, hlasiSedmu = false, hlasiDveSedmy = false);

    companion object {
        fun dleJmena(jmeno: String): ZavazekMariase {
            return entries.firstOrNull { it.name == jmeno } ?: HRA
        }
    }
}

data class TahStychuMariase(
    val indexHrace: Int,
    val jmenoHrace: String,
    val karta: Karta
)

data class PolozkaVyuctovaniMariase(
    val popis: String,
    val hodnotaText: String,
    val jeZvyraznena: Boolean = false
)

data class HracVyuctovaniMariase(
    val indexHrace: Int,
    val jmenoHrace: String,
    val jeVitez: Boolean,
    val bodyZapasu: Int,
    val zmenaTerezianu: Int
)

data class VyuctovaniMariase(
    val nadpis: String,
    val podnadpis: String,
    val polozky: List<PolozkaVyuctovaniMariase>,
    val hraci: List<HracVyuctovaniMariase>
)

data class StavMarias(
    val jmenaHracu: List<String>,
    val ruceHracu: List<List<Karta>>,
    val zbytekProForhonta: List<Karta>,
    val talon: List<Karta>,
    val faze: FazeMariase,
    val indexForhonta: Int,
    val naTahu: Int,
    val trumf: BarvaKarty?,
    val volenoZTalonu: Boolean,
    val zavazek: ZavazekMariase,
    val nasobekFlekHra: Int,
    val nasobekFlekSedma: Int,
    val aktivniFlekHra: Boolean,
    val aktivniFlekSedma: Boolean,
    val flekKrok: Int,
    val cisloStychu: Int,
    val aktualniStych: List<TahStychuMariase>,
    val bodyKaretForhont: Int,
    val bodyKaretObrana: Int,
    val hlaskyForhont: Int,
    val hlaskyObrana: Int,
    val pocetStychuForhont: Int,
    val pocetStychuObrana: Int,
    val stych9VitezIndex: Int,
    val stych9ViteznaKarta: Karta?,
    val stych10VitezIndex: Int,
    val stych10ViteznaKarta: Karta?,
    val stych10TrumfSedmaOdIndexu: Int,
    val posledniHlaska: String,
    val cisloTahu: Int
) {
    val jeBetlNeboDurch: Boolean
        get() = zavazek.jeBezTrumfu

    fun doKompaktnihoRetezce(): String {
        val cisteJmena = jmenaHracu.joinToString(",") {
            it.replace(",", " ").replace("|", " ").replace("=", " ").replace(";", " ")
        }
        val ruceText = ruceHracu.joinToString("/") { MariasEngine.zakodujKarty(it) }
        val stychText = aktualniStych.joinToString(",") { "${it.indexHrace}.${MariasEngine.zakodujJednuKartu(it.karta)}" }
        val cistaHlaska = posledniHlaska.replace("|", "-").replace("=", "-").replace(";", ",")

        return serializujMapuPoli(
            listOf(
                "jm" to cisteJmena,
                "ru" to ruceText,
                "zb" to MariasEngine.zakodujKarty(zbytekProForhonta),
                "ta" to MariasEngine.zakodujKarty(talon),
                "fa" to faze.name,
                "fo" to indexForhonta.toString(),
                "nt" to naTahu.toString(),
                "tr" to (trumf?.name ?: ""),
                "vt" to if (volenoZTalonu) "1" else "0",
                "za" to zavazek.name,
                "fh" to nasobekFlekHra.toString(),
                "fs" to nasobekFlekSedma.toString(),
                "ah" to if (aktivniFlekHra) "1" else "0",
                "as" to if (aktivniFlekSedma) "1" else "0",
                "fk" to flekKrok.toString(),
                "cs" to cisloStychu.toString(),
                "st" to stychText,
                "bf" to bodyKaretForhont.toString(),
                "bo" to bodyKaretObrana.toString(),
                "hf" to hlaskyForhont.toString(),
                "ho" to hlaskyObrana.toString(),
                "sf" to pocetStychuForhont.toString(),
                "so" to pocetStychuObrana.toString(),
                "v9" to stych9VitezIndex.toString(),
                "k9" to (stych9ViteznaKarta?.let { MariasEngine.zakodujJednuKartu(it) } ?: ""),
                "v10" to stych10VitezIndex.toString(),
                "k10" to (stych10ViteznaKarta?.let { MariasEngine.zakodujJednuKartu(it) } ?: ""),
                "ts10" to stych10TrumfSedmaOdIndexu.toString(),
                "hl" to cistaHlaska,
                "ct" to cisloTahu.toString()
            )
        )
    }

    companion object {
        fun zKompaktnihoRetezce(data: String): StavMarias? {
            if (data.isBlank()) return null
            val r = PoleStavuReader(data)
            val jmena = r.text("jm").split(",").filter { it.isNotBlank() }
            if (jmena.size != 3) return null

            val ruceCasti = r.text("ru").split("/")
            val ruce = List(3) { idx ->
                MariasEngine.dekodujKarty(ruceCasti.getOrElse(idx) { "" })
            }
            val trumfText = r.text("tr")
            val trumf = BarvaKarty.entries.firstOrNull { it.name == trumfText }
            val faze = FazeMariase.entries.firstOrNull { it.name == r.text("fa") } ?: FazeMariase.VOLBA_TRUMFU

            val stych = r.text("st").split(",").mapNotNull { polozka ->
                val tecka = polozka.indexOf('.')
                if (tecka <= 0) return@mapNotNull null
                val idx = polozka.substring(0, tecka).toIntOrNull() ?: return@mapNotNull null
                val karta = MariasEngine.dekodujJednuKartu(polozka.substring(tecka + 1)) ?: return@mapNotNull null
                TahStychuMariase(
                    indexHrace = idx,
                    jmenoHrace = jmena.getOrElse(idx) { "Hráč" },
                    karta = karta
                )
            }

            return StavMarias(
                jmenaHracu = jmena,
                ruceHracu = ruce,
                zbytekProForhonta = MariasEngine.dekodujKarty(r.text("zb")),
                talon = MariasEngine.dekodujKarty(r.text("ta")),
                faze = faze,
                indexForhonta = r.cislo("fo", 0).coerceIn(0, 2),
                naTahu = r.cislo("nt", 0).coerceIn(0, 2),
                trumf = trumf,
                volenoZTalonu = r.text("vt") == "1",
                zavazek = ZavazekMariase.dleJmena(r.text("za", "HRA")),
                nasobekFlekHra = r.cislo("fh", 1).coerceIn(1, 32),
                nasobekFlekSedma = r.cislo("fs", 1).coerceIn(1, 32),
                aktivniFlekHra = r.text("ah", "1") == "1",
                aktivniFlekSedma = r.text("as", "0") == "1",
                flekKrok = r.cislo("fk", 0),
                cisloStychu = r.cislo("cs", 1),
                aktualniStych = stych,
                bodyKaretForhont = r.cislo("bf", 0),
                bodyKaretObrana = r.cislo("bo", 0),
                hlaskyForhont = r.cislo("hf", 0),
                hlaskyObrana = r.cislo("ho", 0),
                pocetStychuForhont = r.cislo("sf", 0),
                pocetStychuObrana = r.cislo("so", 0),
                stych9VitezIndex = r.cislo("v9", -1),
                stych9ViteznaKarta = MariasEngine.dekodujJednuKartu(r.text("k9")),
                stych10VitezIndex = r.cislo("v10", -1),
                stych10ViteznaKarta = MariasEngine.dekodujJednuKartu(r.text("k10")),
                stych10TrumfSedmaOdIndexu = r.cislo("ts10", -1),
                posledniHlaska = r.text("hl", ""),
                cisloTahu = r.cislo("ct", 0)
            )
        }
    }
}

object MariasEngine {

    fun zakodujJednuKartu(karta: Karta): String {
        val b = when (karta.barva) {
            BarvaKarty.CERVENE -> 'C'
            BarvaKarty.KULE -> 'K'
            BarvaKarty.ZELENE -> 'Z'
            BarvaKarty.ZALUDY -> 'A'
        }
        val h = when (karta.hodnota) {
            HodnotaKarty.R7 -> '7'
            HodnotaKarty.R8 -> '8'
            HodnotaKarty.R9 -> '9'
            HodnotaKarty.R10 -> 'X'
            HodnotaKarty.SPODEK -> 'J'
            HodnotaKarty.SVRSEK -> 'Q'
            HodnotaKarty.KRAL -> 'K'
            HodnotaKarty.ESO -> 'E'
        }
        return "$b$h"
    }

    fun dekodujJednuKartu(kod: String): Karta? {
        val cisty = kod.trim()
        if (cisty.length != 2) return null
        val barva = when (cisty[0]) {
            'C' -> BarvaKarty.CERVENE
            'K' -> BarvaKarty.KULE
            'Z' -> BarvaKarty.ZELENE
            'A' -> BarvaKarty.ZALUDY
            else -> return null
        }
        val hodnota = when (cisty[1]) {
            '7' -> HodnotaKarty.R7
            '8' -> HodnotaKarty.R8
            '9' -> HodnotaKarty.R9
            'X' -> HodnotaKarty.R10
            'J' -> HodnotaKarty.SPODEK
            'Q' -> HodnotaKarty.SVRSEK
            'K' -> HodnotaKarty.KRAL
            'E' -> HodnotaKarty.ESO
            else -> return null
        }
        return Karta(barva, hodnota)
    }

    fun zakodujKarty(karty: List<Karta>): String {
        if (karty.isEmpty()) return ""
        return karty.joinToString(".") { zakodujJednuKartu(it) }
    }

    fun dekodujKarty(text: String): List<Karta> {
        if (text.isBlank()) return emptyList()
        return text.split(".").mapNotNull { dekodujJednuKartu(it) }
    }

    fun nazevStupneFleku(nasobek: Int): String = when (nasobek) {
        2 -> "Flek (2×)"
        4 -> "Re (4×)"
        8 -> "Tutti (8×)"
        16 -> "Boty (16×)"
        32 -> "Kalhoty (32×)"
        else -> "1×"
    }

    private fun silaKarty(hodnota: HodnotaKarty, bezTrumfu: Boolean): Int {
        return if (bezTrumfu) {
            when (hodnota) {
                HodnotaKarty.R7 -> 1
                HodnotaKarty.R8 -> 2
                HodnotaKarty.R9 -> 3
                HodnotaKarty.R10 -> 4
                HodnotaKarty.SPODEK -> 5
                HodnotaKarty.SVRSEK -> 6
                HodnotaKarty.KRAL -> 7
                HodnotaKarty.ESO -> 8
            }
        } else {
            when (hodnota) {
                HodnotaKarty.R7 -> 1
                HodnotaKarty.R8 -> 2
                HodnotaKarty.R9 -> 3
                HodnotaKarty.SPODEK -> 4
                HodnotaKarty.SVRSEK -> 5
                HodnotaKarty.KRAL -> 6
                HodnotaKarty.R10 -> 7
                HodnotaKarty.ESO -> 8
            }
        }
    }

    private fun bodyKarty(hodnota: HodnotaKarty): Int {
        return if (hodnota == HodnotaKarty.ESO || hodnota == HodnotaKarty.R10) 10 else 0
    }

    private fun seradRuku(karty: List<Karta>, trumf: BarvaKarty?, bezTrumfu: Boolean): List<Karta> {
        return karty.sortedWith(
            compareBy<Karta>(
                { k ->
                    if (!bezTrumfu && trumf != null && k.barva == trumf) -1
                    else when (k.barva) {
                        BarvaKarty.CERVENE -> 0
                        BarvaKarty.KULE -> 1
                        BarvaKarty.ZELENE -> 2
                        BarvaKarty.ZALUDY -> 3
                    }
                },
                { k -> -silaKarty(k.hodnota, bezTrumfu) }
            )
        )
    }

    private fun celyNazevKarty(karta: Karta): String {
        val barvaSlovo = when (karta.barva) {
            BarvaKarty.CERVENE -> "Červené"
            BarvaKarty.KULE -> "Kulové"
            BarvaKarty.ZELENE -> "Zelené"
            BarvaKarty.ZALUDY -> "Žaludové"
        }
        val hodnotaSlovo = when (karta.hodnota) {
            HodnotaKarty.R7 -> "sedma"
            HodnotaKarty.R8 -> "osma"
            HodnotaKarty.R9 -> "devítka"
            HodnotaKarty.R10 -> "desítka"
            HodnotaKarty.SPODEK -> "spodek"
            HodnotaKarty.SVRSEK -> "svršek"
            HodnotaKarty.KRAL -> "král"
            HodnotaKarty.ESO -> "eso"
        }
        return "$barvaSlovo $hodnotaSlovo"
    }

    fun novaHra(
        jmenaHracu: List<String>,
        indexForhonta: Int = 0,
        balicek: List<Karta> = Karta.vytvorZamichanyBalicek()
    ): StavMarias {
        val upravenaJmena = when {
            jmenaHracu.size >= 3 -> jmenaHracu.take(3)
            else -> (jmenaHracu + listOf("Karel", "Pepa", "Franta")).take(3)
        }
        val forhont = indexForhonta.coerceIn(0, 2)
        val obrance1 = (forhont + 1) % 3
        val obrance2 = (forhont + 2) % 3

        val zdroj = if (balicek.size == 32) balicek else Karta.vytvorZamichanyBalicek()
        val prvnich7Forhont = seradRuku(zdroj.subList(0, 7), null, false)
        val zbytek5Forhont = zdroj.subList(7, 12)
        val rukaObr1 = seradRuku(zdroj.subList(12, 22), null, false)
        val rukaObr2 = seradRuku(zdroj.subList(22, 32), null, false)

        val ruce = MutableList(3) { emptyList<Karta>() }
        ruce[forhont] = prvnich7Forhont
        ruce[obrance1] = rukaObr1
        ruce[obrance2] = rukaObr2

        return StavMarias(
            jmenaHracu = upravenaJmena,
            ruceHracu = ruce,
            zbytekProForhonta = zbytek5Forhont,
            talon = emptyList(),
            faze = FazeMariase.VOLBA_TRUMFU,
            indexForhonta = forhont,
            naTahu = forhont,
            trumf = null,
            volenoZTalonu = false,
            zavazek = ZavazekMariase.HRA,
            nasobekFlekHra = 1,
            nasobekFlekSedma = 1,
            aktivniFlekHra = false,
            aktivniFlekSedma = false,
            flekKrok = 0,
            cisloStychu = 1,
            aktualniStych = emptyList(),
            bodyKaretForhont = 0,
            bodyKaretObrana = 0,
            hlaskyForhont = 0,
            hlaskyObrana = 0,
            pocetStychuForhont = 0,
            pocetStychuObrana = 0,
            stych9VitezIndex = -1,
            stych9ViteznaKarta = null,
            stych10VitezIndex = -1,
            stych10ViteznaKarta = null,
            stych10TrumfSedmaOdIndexu = -1,
            posledniHlaska = "Rozdáno! Forhont ${upravenaJmena[forhont]} volí trumf ze 7 karet.",
            cisloTahu = 1
        )
    }

    fun zvolTrumfNeboHruBezTrumfu(
        stav: StavMarias,
        zvolenyTrumf: BarvaKarty?,
        zTalonu: Boolean,
        pocatecniZavazek: ZavazekMariase = ZavazekMariase.HRA
    ): StavMarias {
        if (stav.faze != FazeMariase.VOLBA_TRUMFU) return stav
        val forhont = stav.indexForhonta

        val urcenyZavazek = when {
            zTalonu -> ZavazekMariase.HRA
            pocatecniZavazek.jeBezTrumfu -> pocatecniZavazek
            else -> ZavazekMariase.HRA
        }

        val kartaZTalonu = stav.zbytekProForhonta.lastOrNull()
        val urcenyTrumf = when {
            urcenyZavazek.jeBezTrumfu -> null
            zTalonu -> kartaZTalonu?.barva ?: BarvaKarty.CERVENE
            else -> zvolenyTrumf ?: BarvaKarty.CERVENE
        }

        val dvanactKaret = seradRuku(
            stav.ruceHracu[forhont] + stav.zbytekProForhonta,
            urcenyTrumf,
            urcenyZavazek.jeBezTrumfu
        )
        val noveRuce = stav.ruceHracu.mapIndexed { idx, ruka ->
            if (idx == forhont) dvanactKaret else seradRuku(ruka, urcenyTrumf, urcenyZavazek.jeBezTrumfu)
        }

        val hlaska = when {
            urcenyZavazek.jeBezTrumfu ->
                "${stav.jmenaHracu[forhont]} chystá ${urcenyZavazek.nazev} a odkládá 2 karty do talonu."
            zTalonu && kartaZTalonu != null ->
                "${stav.jmenaHracu[forhont]} volil z talonu (${celyNazevKarty(kartaZTalonu)}) — trumfem jsou ${urcenyTrumf?.ceskyNazev}!"
            else ->
                "${stav.jmenaHracu[forhont]} zvolil trumf ${urcenyTrumf?.ceskyNazev} a odkládá 2 karty do talonu."
        }

        return stav.copy(
            ruceHracu = noveRuce,
            zbytekProForhonta = emptyList(),
            faze = FazeMariase.ODKLADANI_TALONU,
            naTahu = forhont,
            trumf = urcenyTrumf,
            volenoZTalonu = zTalonu && !urcenyZavazek.jeBezTrumfu,
            zavazek = urcenyZavazek,
            posledniHlaska = hlaska,
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun zjistiKartyPovoleneDoTalonu(stav: StavMarias, planovanyZavazek: ZavazekMariase): Set<Karta> {
        val ruka = stav.ruceHracu.getOrNull(stav.indexForhonta) ?: return emptySet()
        if (planovanyZavazek.jeBezTrumfu) {
            return ruka.toSet()
        }
        val nebodovane = ruka.filter { it.hodnota != HodnotaKarty.ESO && it.hodnota != HodnotaKarty.R10 }
        return if (nebodovane.size >= 2) nebodovane.toSet() else ruka.toSet()
    }

    /**
     * V českém Mariáši smí forhont hlásit sedmu (i dvě sedmy) vždy, pokud nevolil z talonu —
     * včetně tzv. falešné sedmy jako blafu! Tyto pomocné funkce informují UI, zda jde o pravou či falešnou sedmu.
     */
    fun muzeHlasitSedmu(stav: StavMarias): Boolean = !stav.volenoZTalonu

    fun maPravouSedmu(stav: StavMarias, vybraneDoTalonu: List<Karta> = emptyList()): Boolean {
        val trumf = stav.trumf ?: return false
        val zbyvajiciRuka = (stav.ruceHracu.getOrNull(stav.indexForhonta) ?: emptyList()) - vybraneDoTalonu.toSet()
        return zbyvajiciRuka.any { it.barva == trumf && it.hodnota == HodnotaKarty.R7 }
    }

    fun maPraveDveSedmy(stav: StavMarias, vybraneDoTalonu: List<Karta> = emptyList()): Boolean {
        val trumf = stav.trumf ?: return false
        val zbyvajiciRuka = (stav.ruceHracu.getOrNull(stav.indexForhonta) ?: emptyList()) - vybraneDoTalonu.toSet()
        val maTrumfovou = zbyvajiciRuka.any { it.barva == trumf && it.hodnota == HodnotaKarty.R7 }
        val maVedlejsi = zbyvajiciRuka.any { it.barva != trumf && it.hodnota == HodnotaKarty.R7 }
        return maTrumfovou && maVedlejsi
    }

    fun potvrdTalonAZavazek(
        stav: StavMarias,
        odlozeneKarty: List<Karta>,
        zvolenyZavazek: ZavazekMariase
    ): StavMarias {
        if (stav.faze != FazeMariase.ODKLADANI_TALONU) return stav
        val forhont = stav.indexForhonta
        val ruka = stav.ruceHracu[forhont]
        if (odlozeneKarty.size != 2 || !ruka.containsAll(odlozeneKarty)) return stav

        val platnyZavazek = if (stav.volenoZTalonu) ZavazekMariase.HRA else zvolenyZavazek
        val povolene = zjistiKartyPovoleneDoTalonu(stav, platnyZavazek)
        if (!odlozeneKarty.all { it in povolene }) return stav

        val platnyTrumf = if (platnyZavazek.jeBezTrumfu) null else (stav.trumf ?: BarvaKarty.CERVENE)
        val novaRukaForhonta = seradRuku(ruka - odlozeneKarty.toSet(), platnyTrumf, platnyZavazek.jeBezTrumfu)
        val noveRuce = stav.ruceHracu.mapIndexed { idx, h ->
            if (idx == forhont) novaRukaForhonta else seradRuku(h, platnyTrumf, platnyZavazek.jeBezTrumfu)
        }

        // Body případně odložené v Betlu/Durchu se nepočítají; v trumfové hře jsou A/10 chráněné
        val bodyTalonuObrana = if (platnyZavazek.jeBezTrumfu) 0 else odlozeneKarty.sumOf { bodyKarty(it.hodnota) }
        val obrance1 = (forhont + 1) % 3
        val popisTrumfu = if (platnyZavazek.jeBezTrumfu) "" else " (${platnyTrumf?.ceskyNazev})"

        return stav.copy(
            ruceHracu = noveRuce,
            talon = odlozeneKarty,
            faze = FazeMariase.FLEKOVANI,
            trumf = platnyTrumf,
            zavazek = platnyZavazek,
            naTahu = obrance1,
            nasobekFlekHra = 1,
            nasobekFlekSedma = 1,
            aktivniFlekHra = true,
            aktivniFlekSedma = platnyZavazek.hlasiSedmu,
            flekKrok = 0,
            bodyKaretObrana = bodyTalonuObrana,
            posledniHlaska = "${stav.jmenaHracu[forhont]} hlásí: ${platnyZavazek.nazev}$popisTrumfu! Obrana se vyjadřuje k fleku.",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun muzeHracFlekHry(stav: StavMarias, indexHrace: Int): Boolean {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace || !stav.aktivniFlekHra) return false
        return if (indexHrace == stav.indexForhonta) {
            stav.nasobekFlekHra == 2 || stav.nasobekFlekHra == 8
        } else {
            stav.nasobekFlekHra == 1 || stav.nasobekFlekHra == 4 || stav.nasobekFlekHra == 16
        }
    }

    fun muzeHracFlekSedmy(stav: StavMarias, indexHrace: Int): Boolean {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace || !stav.aktivniFlekSedma) return false
        if (!stav.zavazek.hlasiSedmu) return false
        return if (indexHrace == stav.indexForhonta) {
            stav.nasobekFlekSedma == 2 || stav.nasobekFlekSedma == 8
        } else {
            stav.nasobekFlekSedma == 1 || stav.nasobekFlekSedma == 4 || stav.nasobekFlekSedma == 16
        }
    }

    fun odpovezNaFlekovani(
        stav: StavMarias,
        indexHrace: Int,
        zvednoutHru: Boolean,
        zvednoutSedmu: Boolean
    ): StavMarias {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace) return stav
        val forhont = stav.indexForhonta
        val obrance1 = (forhont + 1) % 3
        val obrance2 = (forhont + 2) % 3

        val platnyZdvihHry = zvednoutHru && muzeHracFlekHry(stav, indexHrace)
        val platnyZdvihSedmy = zvednoutSedmu && muzeHracFlekSedmy(stav, indexHrace)

        val novyNasHra = if (platnyZdvihHry) (stav.nasobekFlekHra * 2).coerceAtMost(32) else stav.nasobekFlekHra
        val novyNasSedma = if (platnyZdvihSedmy) (stav.nasobekFlekSedma * 2).coerceAtMost(32) else stav.nasobekFlekSedma

        val jmeno = stav.jmenaHracu[indexHrace]
        val reakceText = when {
            platnyZdvihHry && platnyZdvihSedmy ->
                "$jmeno dává ${nazevStupneFleku(novyNasHra)} na hru i ${nazevStupneFleku(novyNasSedma)} na sedmu!"
            platnyZdvihHry ->
                "$jmeno dává ${nazevStupneFleku(novyNasHra)} na hru!"
            platnyZdvihSedmy ->
                "$jmeno dává ${nazevStupneFleku(novyNasSedma)} na ${if (stav.zavazek.hlasiDveSedmy) "dvě sedmy" else "sedmu"}!"
            else ->
                "$jmeno hlásí: Dobrý."
        }

        // Krok 0: vyjádřil se Obránce 1 -> zjistíme, zda má Obránce 2 ještě co zvednout
        if (stav.flekKrok == 0) {
            val obr2MuzeHru = stav.aktivniFlekHra && (novyNasHra == 1 || novyNasHra == 4 || novyNasHra == 16)
            val obr2MuzeSedmu = stav.aktivniFlekSedma && stav.zavazek.hlasiSedmu &&
                (novyNasSedma == 1 || novyNasSedma == 4 || novyNasSedma == 16)

            if (obr2MuzeHru || obr2MuzeSedmu) {
                return stav.copy(
                    nasobekFlekHra = novyNasHra,
                    nasobekFlekSedma = novyNasSedma,
                    flekKrok = 1,
                    naTahu = obrance2,
                    posledniHlaska = reakceText,
                    cisloTahu = stav.cisloTahu + 1
                )
            }
            return ukonciKoloObranyVeFlekovani(stav, novyNasHra, novyNasSedma, reakceText)
        }

        // Krok 1: vyjádřil se Obránce 2 -> konec kola obrany
        if (stav.flekKrok == 1) {
            return ukonciKoloObranyVeFlekovani(stav, novyNasHra, novyNasSedma, reakceText)
        }

        // Krok 2: vyjádřil se Forhont (Re 4× nebo Boty 16×) -> buď má obrana nárok na Tutti/Kalhoty, nebo jdeme hrát
        val daleAktivniHra = stav.aktivniFlekHra && (novyNasHra == 4 || novyNasHra == 16)
        val daleAktivniSedma = stav.aktivniFlekSedma && stav.zavazek.hlasiSedmu && (novyNasSedma == 4 || novyNasSedma == 16)

        return if (daleAktivniHra || daleAktivniSedma) {
            stav.copy(
                nasobekFlekHra = novyNasHra,
                nasobekFlekSedma = novyNasSedma,
                aktivniFlekHra = daleAktivniHra,
                aktivniFlekSedma = daleAktivniSedma,
                flekKrok = 0,
                naTahu = obrance1,
                posledniHlaska = reakceText,
                cisloTahu = stav.cisloTahu + 1
            )
        } else {
            stav.copy(
                nasobekFlekHra = novyNasHra,
                nasobekFlekSedma = novyNasSedma,
                aktivniFlekHra = false,
                aktivniFlekSedma = false,
                faze = FazeMariase.SEHRAVKA,
                naTahu = forhont,
                posledniHlaska = "$reakceText Začíná sehrávka — vynáší ${stav.jmenaHracu[forhont]}.",
                cisloTahu = stav.cisloTahu + 1
            )
        }
    }

    private fun ukonciKoloObranyVeFlekovani(
        stav: StavMarias,
        novyNasHra: Int,
        novyNasSedma: Int,
        reakceText: String
    ): StavMarias {
        val forhont = stav.indexForhonta
        val daleAktivniHra = stav.aktivniFlekHra && (novyNasHra == 2 || novyNasHra == 8)
        val daleAktivniSedma = stav.aktivniFlekSedma && stav.zavazek.hlasiSedmu && (novyNasSedma == 2 || novyNasSedma == 8)

        return if (daleAktivniHra || daleAktivniSedma) {
            stav.copy(
                nasobekFlekHra = novyNasHra,
                nasobekFlekSedma = novyNasSedma,
                aktivniFlekHra = daleAktivniHra,
                aktivniFlekSedma = daleAktivniSedma,
                flekKrok = 2,
                naTahu = forhont,
                posledniHlaska = "$reakceText Na řadě je forhont ${stav.jmenaHracu[forhont]}.",
                cisloTahu = stav.cisloTahu + 1
            )
        } else {
            stav.copy(
                nasobekFlekHra = novyNasHra,
                nasobekFlekSedma = novyNasSedma,
                aktivniFlekHra = false,
                aktivniFlekSedma = false,
                faze = FazeMariase.SEHRAVKA,
                naTahu = forhont,
                posledniHlaska = "$reakceText Začíná sehrávka — vynáší ${stav.jmenaHracu[forhont]}.",
                cisloTahu = stav.cisloTahu + 1
            )
        }
    }

    private fun urciVitezeStychu(
        stych: List<TahStychuMariase>,
        trumf: BarvaKarty?,
        bezTrumfu: Boolean
    ): TahStychuMariase {
        val barvaVynosu = stych.first().karta.barva
        val trumfoveTahy = if (!bezTrumfu && trumf != null) {
            stych.filter { it.karta.barva == trumf }
        } else {
            emptyList()
        }
        return if (trumfoveTahy.isNotEmpty()) {
            trumfoveTahy.maxBy { silaKarty(it.karta.hodnota, bezTrumfu = false) }
        } else {
            stych.filter { it.karta.barva == barvaVynosu }
                .maxBy { silaKarty(it.karta.hodnota, bezTrumfu = bezTrumfu) }
        }
    }

    fun zjistiPovoleneKarty(stav: StavMarias, indexHrace: Int): Set<Karta> {
        if (stav.faze != FazeMariase.SEHRAVKA || stav.naTahu != indexHrace) return emptySet()
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()
        if (ruka.isEmpty()) return emptySet()
        if (stav.aktualniStych.isEmpty()) return ruka.toSet()

        val bezTrumfu = stav.jeBetlNeboDurch
        val trumf = stav.trumf
        val barvaVynosu = stav.aktualniStych.first().karta.barva
        val kartyVBarve = ruka.filter { it.barva == barvaVynosu }

        // 1) Hráč má vynesenou barvu -> povinnost přiznat barvu (a přebít, pokud štych ještě nebyl trumfnut cizím trumfem)
        if (kartyVBarve.isNotEmpty()) {
            val uzTrumfnutoCizimTrumfem = !bezTrumfu && trumf != null &&
                barvaVynosu != trumf && stav.aktualniStych.any { it.karta.barva == trumf }

            if (uzTrumfnutoCizimTrumfem) {
                return kartyVBarve.toSet()
            }
            val nejvyssiSilaVeStychu = stav.aktualniStych
                .filter { it.karta.barva == barvaVynosu }
                .maxOf { silaKarty(it.karta.hodnota, bezTrumfu) }
            val vyssiKarty = kartyVBarve.filter { silaKarty(it.hodnota, bezTrumfu) > nejvyssiSilaVeStychu }
            return if (vyssiKarty.isNotEmpty()) vyssiKarty.toSet() else kartyVBarve.toSet()
        }

        // 2) Hráč nemá vynesenou barvu -> v trumfové hře povinnost trumfovat (a nadtrumfnout, pokud lze)
        if (!bezTrumfu && trumf != null) {
            val trumfyVRuce = ruka.filter { it.barva == trumf }
            if (trumfyVRuce.isNotEmpty()) {
                val trumfyVeStychu = stav.aktualniStych.filter { it.karta.barva == trumf }
                if (trumfyVeStychu.isEmpty()) {
                    return trumfyVRuce.toSet()
                }
                val maxTrumfVeStychu = trumfyVeStychu.maxOf { silaKarty(it.karta.hodnota, false) }
                val vyssiTrumfy = trumfyVRuce.filter { silaKarty(it.hodnota, false) > maxTrumfVeStychu }
                return if (vyssiTrumfy.isNotEmpty()) vyssiTrumfy.toSet() else trumfyVRuce.toSet()
            }
        }

        // 3) Nemá ani barvu, ani trumf -> volný odhoz
        return ruka.toSet()
    }

    fun zahrajKartu(stav: StavMarias, indexHrace: Int, karta: Karta): StavMarias {
        val povolene = zjistiPovoleneKarty(stav, indexHrace)
        if (karta !in povolene) return stav

        val rukaPred = stav.ruceHracu[indexHrace]
        val jmenoHrace = stav.jmenaHracu[indexHrace]

        // Kontrola hlášky (Svršek + Král stejné barvy v ruce v okamžiku vynesení/přiznání)
        var bodyHlaskyTahu = 0
        if (!stav.jeBetlNeboDurch && (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL)) {
            val parovaHodnota = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
            if (rukaPred.any { it.barva == karta.barva && it.hodnota == parovaHodnota }) {
                bodyHlaskyTahu = if (karta.barva == stav.trumf) 40 else 20
            }
        }

        val noveHlaskyForhont = stav.hlaskyForhont + if (indexHrace == stav.indexForhonta) bodyHlaskyTahu else 0
        val noveHlaskyObrana = stav.hlaskyObrana + if (indexHrace != stav.indexForhonta) bodyHlaskyTahu else 0
        val hlaskaPridavek = if (bodyHlaskyTahu > 0) " (Hláška $bodyHlaskyTahu!)" else ""

        val novaRuka = rukaPred - karta
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }
        val novyStych = stav.aktualniStych + TahStychuMariase(indexHrace, jmenoHrace, karta)

        if (novyStych.size < 3) {
            return stav.copy(
                ruceHracu = noveRuce,
                aktualniStych = novyStych,
                naTahu = (indexHrace + 1) % 3,
                hlaskyForhont = noveHlaskyForhont,
                hlaskyObrana = noveHlaskyObrana,
                posledniHlaska = "$jmenoHrace nese ${celyNazevKarty(karta)}$hlaskaPridavek.",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        // Vyhodnocení kompletního 3karetního štychu
        val viteznyTah = urciVitezeStychu(novyStych, stav.trumf, stav.jeBetlNeboDurch)
        val vitezIdx = viteznyTah.indexHrace
        val bereForhont = vitezIdx == stav.indexForhonta

        val jePosledniStych = stav.cisloStychu >= 10 || noveRuce.all { it.isEmpty() }
        val bodyVeStychu = if (stav.jeBetlNeboDurch) {
            0
        } else {
            novyStych.sumOf { bodyKarty(it.karta.hodnota) } + if (jePosledniStych) 10 else 0
        }

        val novyPocetStychuForhont = stav.pocetStychuForhont + if (bereForhont) 1 else 0
        val novyPocetStychuObrana = stav.pocetStychuObrana + if (!bereForhont) 1 else 0
        val noveBodyForhont = stav.bodyKaretForhont + if (bereForhont) bodyVeStychu else 0
        val noveBodyObrana = stav.bodyKaretObrana + if (!bereForhont) bodyVeStychu else 0

        val novyV9 = if (stav.cisloStychu == 9) vitezIdx else stav.stych9VitezIndex
        val novaK9 = if (stav.cisloStychu == 9) viteznyTah.karta else stav.stych9ViteznaKarta
        val novyV10 = if (jePosledniStych) vitezIdx else stav.stych10VitezIndex
        val novaK10 = if (jePosledniStych) viteznyTah.karta else stav.stych10ViteznaKarta
        val novyTrumf7Od = if (jePosledniStych && stav.trumf != null && !stav.jeBetlNeboDurch) {
            novyStych.firstOrNull { it.karta.barva == stav.trumf && it.karta.hodnota == HodnotaKarty.R7 }?.indexHrace ?: -1
        } else {
            stav.stych10TrumfSedmaOdIndexu
        }

        // Okamžité ukončení Betlu / Durchu při porušení závazku
        val selhalBetl = stav.zavazek == ZavazekMariase.BETL && novyPocetStychuForhont > 0
        val selhalDurch = stav.zavazek == ZavazekMariase.DURCH && novyPocetStychuObrana > 0
        val konecHry = jePosledniStych || selhalBetl || selhalDurch

        val souhrnStychu = when {
            selhalBetl -> "${stav.jmenaHracu[stav.indexForhonta]} vzal štych — Betl je prohraný!"
            selhalDurch -> "${viteznyTah.jmenoHrace} vzal štych — Durch je prohraný!"
            jePosledniStych -> "Poslední štych bere ${viteznyTah.jmenoHrace} kartou ${celyNazevKarty(viteznyTah.karta)}$hlaskaPridavek!"
            else -> "${stav.cisloStychu}. štych bere ${viteznyTah.jmenoHrace} (${celyNazevKarty(viteznyTah.karta)})$hlaskaPridavek."
        }

        return stav.copy(
            ruceHracu = noveRuce,
            faze = if (konecHry) FazeMariase.KONEC else FazeMariase.SEHRAVKA,
            naTahu = vitezIdx,
            cisloStychu = if (konecHry) stav.cisloStychu else stav.cisloStychu + 1,
            aktualniStych = emptyList(),
            bodyKaretForhont = noveBodyForhont,
            bodyKaretObrana = noveBodyObrana,
            hlaskyForhont = noveHlaskyForhont,
            hlaskyObrana = noveHlaskyObrana,
            pocetStychuForhont = novyPocetStychuForhont,
            pocetStychuObrana = novyPocetStychuObrana,
            stych9VitezIndex = novyV9,
            stych9ViteznaKarta = novaK9,
            stych10VitezIndex = novyV10,
            stych10ViteznaKarta = novaK10,
            stych10TrumfSedmaOdIndexu = novyTrumf7Od,
            posledniHlaska = souhrnStychu,
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun vyhodnotMariase(stav: StavMarias): VyuctovaniMariase {
        val forhont = stav.indexForhonta
        val jmenoForhonta = stav.jmenaHracu[forhont]
        val polozky = ArrayList<PolozkaVyuctovaniMariase>()

        // 1) Vyhodnocení Betlu a Durchu
        if (stav.jeBetlNeboDurch) {
            val jeDurch = stav.zavazek == ZavazekMariase.DURCH
            val vyhralForhont = if (jeDurch) {
                stav.pocetStychuObrana == 0 && stav.pocetStychuForhont == 10
            } else {
                stav.pocetStychuForhont == 0
            }
            val zaklad = if (jeDurch) 100 else 50
            val sazbaNaObrance = zaklad * stav.nasobekFlekHra
            val znamenkoForhont = if (vyhralForhont) 1 else -1
            val bilanceNaObrance = znamenkoForhont * sazbaNaObrance

            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Závazek ${stav.zavazek.nazev} (${stav.nasobekFlekHra}×)",
                    hodnotaText = if (vyhralForhont) "Uhráno!" else "Prohráno",
                    jeZvyraznena = true
                )
            )
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Sazba na každého obránce",
                    hodnotaText = formatujTereziany(bilanceNaObrance, seZnaminkem = true)
                )
            )

            val hraciVys = stav.jmenaHracu.mapIndexed { idx, jm ->
                val jeF = idx == forhont
                val zmena = if (jeF) bilanceNaObrance * 2 else -bilanceNaObrance
                val vyhra = if (jeF) vyhralForhont else !vyhralForhont
                HracVyuctovaniMariase(
                    indexHrace = idx,
                    jmenoHrace = jm,
                    jeVitez = vyhra,
                    bodyZapasu = zmena,
                    zmenaTerezianu = zmena
                )
            }
            return VyuctovaniMariase(
                nadpis = if (vyhralForhont) "$jmenoForhonta uhrál ${stav.zavazek.nazev}!" else "$jmenoForhonta prohrál ${stav.zavazek.nazev}!",
                podnadpis = "Štychy forhont ${stav.pocetStychuForhont} : obrana ${stav.pocetStychuObrana}",
                polozky = polozky,
                hraci = hraciVys
            )
        }

        // 2) Vyhodnocení trumfové hry (Hra, Stovka, Sedma, Dvě sedmy, Tiché a Zabité sedmy)
        val trumf = stav.trumf ?: BarvaKarty.KULE
        val nasobekCervene = if (trumf == BarvaKarty.CERVENE) 2 else 1

        // Hlášky platí pouze straně, která uhrála alespoň 1 štych!
        val platneHlaskyF = if (stav.pocetStychuForhont > 0) stav.hlaskyForhont else 0
        val platneHlaskyO = if (stav.pocetStychuObrana > 0) stav.hlaskyObrana else 0
        val celkemF = stav.bodyKaretForhont + platneHlaskyF
        val celkemO = stav.bodyKaretObrana + platneHlaskyO

        polozky.add(
            PolozkaVyuctovaniMariase(
                popis = "Body a hlášky ($jmenoForhonta vs. Obrana)",
                hodnotaText = "$celkemF b. : $celkemO b."
            )
        )
        if (trumf == BarvaKarty.CERVENE) {
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Červené trumfy",
                    hodnotaText = "2× sazba"
                )
            )
        }

        // A) Bilance hlavní hry / stovky
        val bilanceHlavniNaObrance: Int
        if (stav.zavazek.hlasiSto) {
            val uhranaStovka = celkemF >= 100
            val nadStoBonus = if (uhranaStovka) ((celkemF - 100) / 10) * 10 else 0
            val sazba = (40 + nadStoBonus) * nasobekCervene * stav.nasobekFlekHra
            bilanceHlavniNaObrance = if (uhranaStovka) sazba else -sazba
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Hlášená Stovka (${stav.nasobekFlekHra}×)",
                    hodnotaText = (if (uhranaStovka) "+" else "-") + "$sazba / obr.",
                    jeZvyraznena = true
                )
            )
        } else {
            val vyhralHru = celkemF > celkemO
            if (vyhralHru) {
                val tichaStovka = celkemF >= 100
                val kanar = stav.pocetStychuObrana == 0
                val zaklad = when {
                    tichaStovka -> 20 + ((celkemF - 100) / 10) * 10
                    kanar -> 20
                    else -> 10
                }
                val sazba = zaklad * nasobekCervene * stav.nasobekFlekHra
                bilanceHlavniNaObrance = sazba
                val nazevPol = when {
                    tichaStovka -> "Uhraná Tichá stovka (${stav.nasobekFlekHra}×)"
                    kanar -> "Uhraná Hra s kanárem (${stav.nasobekFlekHra}×)"
                    else -> "Uhraná Hra (${stav.nasobekFlekHra}×)"
                }
                polozky.add(PolozkaVyuctovaniMariase(nazevPol, "+$sazba / obr.", jeZvyraznena = true))
            } else {
                val tichaStovkaObr = celkemO >= 100
                val kanarObr = stav.pocetStychuForhont == 0
                val zaklad = when {
                    tichaStovkaObr -> 20 + ((celkemO - 100) / 10) * 10
                    kanarObr -> 20
                    else -> 10
                }
                val sazba = zaklad * nasobekCervene * stav.nasobekFlekHra
                bilanceHlavniNaObrance = -sazba
                val nazevPol = when {
                    tichaStovkaObr -> "Tichá stovka obrany (${stav.nasobekFlekHra}×)"
                    else -> "Prohraná Hra (${stav.nasobekFlekHra}×)"
                }
                polozky.add(PolozkaVyuctovaniMariase(nazevPol, "-$sazba / obr.", jeZvyraznena = true))
            }
        }

        // B) Bilance sedmy / dvou sedem (hlášené, falešné, tiché i zabité)
        val uhrana9Netrumf7F = stav.stych9VitezIndex == forhont &&
            stav.stych9ViteznaKarta?.hodnota == HodnotaKarty.R7 &&
            stav.stych9ViteznaKarta.barva != trumf
        val uhrana10Trumf7F = stav.stych10VitezIndex == forhont &&
            stav.stych10ViteznaKarta == Karta(trumf, HodnotaKarty.R7)
        val uhrana10Trumf7Obr = stav.stych10VitezIndex != -1 && stav.stych10VitezIndex != forhont &&
            stav.stych10ViteznaKarta == Karta(trumf, HodnotaKarty.R7)

        var bilanceSedmyNaObrance = 0

        if (stav.zavazek.hlasiDveSedmy) {
            val splneno = uhrana9Netrumf7F && uhrana10Trumf7F
            val sazba = 40 * nasobekCervene * stav.nasobekFlekSedma
            bilanceSedmyNaObrance = if (splneno) sazba else -sazba
            val popisSedem = when {
                splneno -> "Uhrané Hlášené dvě sedmy (${stav.nasobekFlekSedma}×)"
                stav.stych10TrumfSedmaOdIndexu == forhont && !uhrana10Trumf7F ->
                    "Zabitá trumfová sedma (Dvě sedmy ${stav.nasobekFlekSedma}×)"
                else -> "Nesplněné / falešné Dvě sedmy (${stav.nasobekFlekSedma}×)"
            }
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = popisSedem,
                    hodnotaText = (if (splneno) "+" else "-") + "$sazba / obr.",
                    jeZvyraznena = true
                )
            )
        } else if (stav.zavazek.hlasiSedmu) {
            val sazba = 20 * nasobekCervene * stav.nasobekFlekSedma
            if (uhrana10Trumf7F) {
                bilanceSedmyNaObrance += sazba
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Uhraná Hlášená sedma (${stav.nasobekFlekSedma}×)",
                        hodnotaText = "+$sazba / obr.",
                        jeZvyraznena = true
                    )
                )
                if (uhrana9Netrumf7F) {
                    val tichaDruha = 10 * nasobekCervene
                    bilanceSedmyNaObrance += tichaDruha
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Druhá tichá sedma v 9. štychu",
                            hodnotaText = "+$tichaDruha / obr."
                        )
                    )
                }
            } else {
                bilanceSedmyNaObrance -= sazba
                val popisPadu = if (stav.stych10TrumfSedmaOdIndexu == forhont) {
                    "Zabitá hlášená sedma v 10. štychu (${stav.nasobekFlekSedma}×)"
                } else {
                    "Nesplněná / falešná hlášená sedma (${stav.nasobekFlekSedma}×)"
                }
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = popisPadu,
                        hodnotaText = "-$sazba / obr.",
                        jeZvyraznena = true
                    )
                )
            }
        } else {
            // Tichá (utajená) sedma, tiché dvě sedmy a zabitá tichá sedma
            if (uhrana10Trumf7F && uhrana9Netrumf7F) {
                val sazba = 20 * nasobekCervene
                bilanceSedmyNaObrance += sazba
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Tiché dvě sedmy (9. a 10. štych)!",
                        hodnotaText = "+$sazba / obr.",
                        jeZvyraznena = true
                    )
                )
            } else if (uhrana10Trumf7F) {
                val sazba = 10 * nasobekCervene
                bilanceSedmyNaObrance += sazba
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Tichá (utajená) sedma v 10. štychu!",
                        hodnotaText = "+$sazba / obr.",
                        jeZvyraznena = true
                    )
                )
            } else if (uhrana10Trumf7Obr) {
                val sazba = 10 * nasobekCervene
                bilanceSedmyNaObrance -= sazba
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Tichá sedma obrany v 10. štychu!",
                        hodnotaText = "-$sazba / obr.",
                        jeZvyraznena = true
                    )
                )
            } else if (stav.stych10TrumfSedmaOdIndexu != -1) {
                val sazba = 10 * nasobekCervene
                if (stav.stych10TrumfSedmaOdIndexu == forhont && stav.stych10VitezIndex != forhont) {
                    bilanceSedmyNaObrance -= sazba
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Zabitá tichá sedma forhonta v 10. štychu!",
                            hodnotaText = "-$sazba / obr.",
                            jeZvyraznena = true
                        )
                    )
                } else if (stav.stych10TrumfSedmaOdIndexu != forhont && stav.stych10VitezIndex == forhont) {
                    bilanceSedmyNaObrance += sazba
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Forhont zabil tichou sedmu obrany!",
                            hodnotaText = "+$sazba / obr.",
                            jeZvyraznena = true
                        )
                    )
                }
            }
        }

        val soucetNaObrance = bilanceHlavniNaObrance + bilanceSedmyNaObrance
        val celkovyZiskForhonta = soucetNaObrance * 2
        val forhontCelkoveUspel = celkovyZiskForhonta > 0 || (celkovyZiskForhonta == 0 && bilanceHlavniNaObrance > 0)

        val hraciVys = stav.jmenaHracu.mapIndexed { idx, jm ->
            val jeF = idx == forhont
            val zmena = if (jeF) celkovyZiskForhonta else -soucetNaObrance
            val vyhra = if (jeF) forhontCelkoveUspel else !forhontCelkoveUspel
            HracVyuctovaniMariase(
                indexHrace = idx,
                jmenoHrace = jm,
                jeVitez = vyhra,
                bodyZapasu = zmena,
                zmenaTerezianu = zmena
            )
        }

        val nadpis = when {
            celkovyZiskForhonta > 0 -> "$jmenoForhonta vyhrál rozdání!"
            celkovyZiskForhonta < 0 -> "Obrana porazila forhonta ($jmenoForhonta)!"
            else -> "Smírné vyúčtování (hra proti sedmě)"
        }

        return VyuctovaniMariase(
            nadpis = nadpis,
            podnadpis = "${stav.zavazek.nazev} (${trumf.ceskyNazev}) • Body $celkemF : $celkemO",
            polozky = polozky,
            hraci = hraciVys
        )
    }

    /**
     * Inteligentní tah bota pro všechny fáze voleného Mariáše.
     */
    fun provedTahBota(stav: StavMarias): StavMarias {
        val botIdx = stav.naTahu
        val ruka = stav.ruceHracu.getOrNull(botIdx) ?: return stav

        return when (stav.faze) {
            FazeMariase.VOLBA_TRUMFU -> {
                val pocetEsAKralu = ruka.count { it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.KRAL }
                if (pocetEsAKralu >= 6) {
                    return zvolTrumfNeboHruBezTrumfu(stav, null, zTalonu = false, pocatecniZavazek = ZavazekMariase.DURCH)
                }
                val nizkeKarty = ruka.count {
                    it.hodnota == HodnotaKarty.R7 || it.hodnota == HodnotaKarty.R8 || it.hodnota == HodnotaKarty.R9
                }
                if (nizkeKarty >= 6) {
                    return zvolTrumfNeboHruBezTrumfu(stav, null, zTalonu = false, pocatecniZavazek = ZavazekMariase.BETL)
                }

                var nejBarva: BarvaKarty? = null
                var nejSkore = -1
                for (b in BarvaKarty.entries) {
                    val vBarve = ruka.filter { it.barva == b }
                    val maHlasku = vBarve.any { it.hodnota == HodnotaKarty.SVRSEK } &&
                        vBarve.any { it.hodnota == HodnotaKarty.KRAL }
                    val skore = vBarve.size * 12 +
                        vBarve.sumOf { silaKarty(it.hodnota, false) } +
                        if (maHlasku) 18 else 0
                    if (skore > nejSkore) {
                        nejSkore = skore
                        nejBarva = b
                    }
                }
                if (nejSkore < 32) {
                    zvolTrumfNeboHruBezTrumfu(stav, null, zTalonu = true)
                } else {
                    zvolTrumfNeboHruBezTrumfu(stav, nejBarva, zTalonu = false)
                }
            }

            FazeMariase.ODKLADANI_TALONU -> {
                val trumf = stav.trumf
                val povolene = zjistiKartyPovoleneDoTalonu(stav, stav.zavazek).toList()
                val serazeneProOdhoz = povolene.sortedBy { k ->
                    val jeTrumf = trumf != null && k.barva == trumf
                    val tvoriHlasku = (k.hodnota == HodnotaKarty.SVRSEK || k.hodnota == HodnotaKarty.KRAL) &&
                        ruka.any {
                            it.barva == k.barva &&
                                it.hodnota == (if (k.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK)
                        }
                    (if (jeTrumf) 100 else 0) +
                        (if (tvoriHlasku) 60 else 0) +
                        silaKarty(k.hodnota, stav.jeBetlNeboDurch)
                }
                val odhoz = serazeneProOdhoz.take(2)
                val zbytek = ruka - odhoz.toSet()

                val cilZavazek = if (stav.volenoZTalonu || stav.zavazek.jeBezTrumfu) {
                    stav.zavazek
                } else {
                    val pocetTrumfu = zbytek.count { it.barva == trumf }
                    val maTrumf7 = trumf != null && zbytek.any { it.barva == trumf && it.hodnota == HodnotaKarty.R7 }
                    val maVedlejsi7SEsem = zbytek.any { k ->
                        k.barva != trumf && k.hodnota == HodnotaKarty.R7 &&
                            zbytek.any { it.barva == k.barva && it.hodnota == HodnotaKarty.ESO }
                    }
                    val ma40Hlasku = trumf != null &&
                        zbytek.any { it.barva == trumf && it.hodnota == HodnotaKarty.SVRSEK } &&
                        zbytek.any { it.barva == trumf && it.hodnota == HodnotaKarty.KRAL }
                    val pocetEsADesitek = zbytek.count { it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.R10 }

                    when {
                        ma40Hlasku && pocetTrumfu >= 5 && pocetEsADesitek >= 4 && maTrumf7 -> ZavazekMariase.STO_A_SEDMA
                        ma40Hlasku && pocetTrumfu >= 5 && pocetEsADesitek >= 4 -> ZavazekMariase.STO
                        pocetTrumfu >= 6 && maTrumf7 && maVedlejsi7SEsem -> ZavazekMariase.HRA_A_DVE_SEDMY
                        pocetTrumfu >= 5 && maTrumf7 -> ZavazekMariase.HRA_A_SEDMA
                        else -> ZavazekMariase.HRA
                    }
                }
                potvrdTalonAZavazek(stav, odhoz, cilZavazek)
            }

            FazeMariase.FLEKOVANI -> {
                val trumf = stav.trumf
                val pocetEsADesitek = ruka.count { it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.R10 }
                val pocetTrumfu = if (trumf != null) ruka.count { it.barva == trumf } else 0
                val maTrumf7 = trumf != null && ruka.any { it.barva == trumf && it.hodnota == HodnotaKarty.R7 }

                val chceZvednoutHru = if (muzeHracFlekHry(stav, botIdx)) {
                    when {
                        stav.zavazek == ZavazekMariase.BETL ->
                            ruka.count { it.hodnota == HodnotaKarty.R7 || it.hodnota == HodnotaKarty.R8 } >= 4 && stav.nasobekFlekHra <= 2
                        stav.zavazek == ZavazekMariase.DURCH ->
                            ruka.count { it.hodnota == HodnotaKarty.ESO } >= 2 && stav.nasobekFlekHra <= 2
                        botIdx == stav.indexForhonta ->
                            pocetTrumfu >= 5 && pocetEsADesitek >= 4 && stav.nasobekFlekHra <= 2
                        else ->
                            (pocetTrumfu >= 4 && pocetEsADesitek >= 3 && stav.nasobekFlekHra == 1) ||
                                (pocetTrumfu >= 5 && pocetEsADesitek >= 4 && stav.nasobekFlekHra <= 4)
                    }
                } else false

                val chceZvednoutSedmu = if (muzeHracFlekSedmy(stav, botIdx)) {
                    if (botIdx != stav.indexForhonta && maTrumf7) {
                        // Obránce drží trumfovou sedmu proti falešné sedmě forhonta -> vždy flekuje!
                        true
                    } else if (botIdx != stav.indexForhonta) {
                        pocetTrumfu >= 4 && stav.nasobekFlekSedma == 1
                    } else {
                        maTrumf7 && pocetTrumfu >= 6 && stav.nasobekFlekSedma == 2
                    }
                } else false

                odpovezNaFlekovani(stav, botIdx, chceZvednoutHru, chceZvednoutSedmu)
            }

            FazeMariase.SEHRAVKA -> {
                val povolene = zjistiPovoleneKarty(stav, botIdx).toList()
                if (povolene.isEmpty()) return stav
                if (povolene.size == 1) return zahrajKartu(stav, botIdx, povolene.first())

                val trumf = stav.trumf
                val jeForhont = botIdx == stav.indexForhonta

                // Chránění trumfové (případně i vedlejší) sedmy při hlášené sedmě / dvou sedmách
                val kandidati = if (jeForhont && stav.zavazek.hlasiSedmu && stav.cisloStychu < 10 && trumf != null) {
                    val bezTrumf7 = povolene.filterNot { it.barva == trumf && it.hodnota == HodnotaKarty.R7 }
                    val poOchraneTrumf7 = bezTrumf7.ifEmpty { povolene }
                    if (stav.zavazek.hlasiDveSedmy && stav.cisloStychu < 9) {
                        val bezSedem = poOchraneTrumf7.filterNot { it.hodnota == HodnotaKarty.R7 }
                        bezSedem.ifEmpty { poOchraneTrumf7 }
                    } else if (stav.zavazek.hlasiDveSedmy && stav.cisloStychu == 9 && stav.aktualniStych.isEmpty()) {
                        val vedlejsi7 = poOchraneTrumf7.filter { it.barva != trumf && it.hodnota == HodnotaKarty.R7 }
                        vedlejsi7.ifEmpty { poOchraneTrumf7 }
                    } else {
                        poOchraneTrumf7
                    }
                } else {
                    povolene
                }

                val vybrana = if (stav.zavazek == ZavazekMariase.BETL) {
                    kandidati.minBy { silaKarty(it.hodnota, true) }
                } else if (stav.aktualniStych.isEmpty()) {
                    // Výnos: eso nebo nejvyšší karta
                    kandidati.maxBy { bodyKarty(it.hodnota) * 2 + silaKarty(it.hodnota, stav.jeBetlNeboDurch) }
                } else if (stav.aktualniStych.size == 2 && !jeForhont) {
                    // Druhý obránce: pokud štych už drží parťák z obrany, namaže mu desítku nebo eso!
                    val prozatimVitez = urciVitezeStychu(stav.aktualniStych, trumf, stav.jeBetlNeboDurch)
                    if (prozatimVitez.indexHrace != stav.indexForhonta) {
                        kandidati.maxBy { bodyKarty(it.hodnota) * 10 - silaKarty(it.hodnota, stav.jeBetlNeboDurch) }
                    } else {
                        // Jinak zkusí štych přebít, nebo zahodí nejlevnější kartu
                        val vitezneKarty = kandidati.filter { k ->
                            val zkusebni = stav.aktualniStych + TahStychuMariase(botIdx, "", k)
                            urciVitezeStychu(zkusebni, trumf, stav.jeBetlNeboDurch).indexHrace == botIdx
                        }
                        if (vitezneKarty.isNotEmpty()) {
                            vitezneKarty.maxBy { bodyKarty(it.hodnota) }
                        } else {
                            kandidati.minBy { bodyKarty(it.hodnota) * 10 + silaKarty(it.hodnota, stav.jeBetlNeboDurch) }
                        }
                    }
                } else {
                    val vitezneKarty = kandidati.filter { k ->
                        val zkusebni = stav.aktualniStych + TahStychuMariase(botIdx, "", k)
                        urciVitezeStychu(zkusebni, trumf, stav.jeBetlNeboDurch).indexHrace == botIdx
                    }
                    if (vitezneKarty.isNotEmpty()) {
                        vitezneKarty.minBy { silaKarty(it.hodnota, stav.jeBetlNeboDurch) }
                    } else {
                        kandidati.minBy { bodyKarty(it.hodnota) * 10 + silaKarty(it.hodnota, stav.jeBetlNeboDurch) }
                    }
                }

                zahrajKartu(stav, botIdx, vybrana)
            }

            FazeMariase.KONEC -> stav
        }
    }
}
