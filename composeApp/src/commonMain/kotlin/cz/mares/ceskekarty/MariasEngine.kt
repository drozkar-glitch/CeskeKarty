package cz.mares.ceskekarty

import kotlin.math.abs

enum class FazeMariase {
    DRAZBA,
    VOLBA_TRUMFU,
    ODKLADANI_TALONU,
    FLEKOVANI,
    SEHRAVKA,
    KONEC;

    companion object {
        fun dleNazvu(text: String): FazeMariase {
            return entries.firstOrNull { it.name.equals(text.trim(), ignoreCase = true) } ?: VOLBA_TRUMFU
        }
    }
}

enum class ZavazekMariase(
    val nazev: String,
    val hlasiSto: Boolean,
    val hlasiSedmu: Boolean,
    val hlasiDveSedmy: Boolean,
    val jeBezTrumfu: Boolean,
    val vahaDrazby: Int
) {
    HRA("Hra", hlasiSto = false, hlasiSedmu = false, hlasiDveSedmy = false, jeBezTrumfu = false, vahaDrazby = 1),
    HRA_A_SEDMA("Hra a sedma", hlasiSto = false, hlasiSedmu = true, hlasiDveSedmy = false, jeBezTrumfu = false, vahaDrazby = 2),
    STO("Stovka", hlasiSto = true, hlasiSedmu = false, hlasiDveSedmy = false, jeBezTrumfu = false, vahaDrazby = 3),
    BETL("Betl", hlasiSto = false, hlasiSedmu = false, hlasiDveSedmy = false, jeBezTrumfu = true, vahaDrazby = 4),
    STO_A_SEDMA("Sto a sedma", hlasiSto = true, hlasiSedmu = true, hlasiDveSedmy = false, jeBezTrumfu = false, vahaDrazby = 5),
    DURCH("Durch", hlasiSto = false, hlasiSedmu = false, hlasiDveSedmy = false, jeBezTrumfu = true, vahaDrazby = 6),
    HRA_A_DVE_SEDMY("Hra a dvě sedmy", hlasiSto = false, hlasiSedmu = true, hlasiDveSedmy = true, jeBezTrumfu = false, vahaDrazby = 7),
    STO_A_DVE_SEDMY("Sto a dvě sedmy", hlasiSto = true, hlasiSedmu = true, hlasiDveSedmy = true, jeBezTrumfu = false, vahaDrazby = 8);

    companion object {
        val poradiDrazby: List<ZavazekMariase> = entries.sortedBy { it.vahaDrazby }

        fun dleNazvu(text: String): ZavazekMariase? {
            val cisty = text.trim()
            if (cisty.isEmpty()) return null
            return entries.firstOrNull { it.name.equals(cisty, ignoreCase = true) }
        }
    }
}

data class TahStychuMariase(
    val indexHrace: Int,
    val jmenoHrace: String,
    val karta: Karta
)

private fun stychMariaseNaRetezec(stych: List<TahStychuMariase>): String {
    return stych.joinToString(";") { polozka ->
        val bezpecneJmeno = polozka.jmenoHrace.replace(";", " ").replace(":", " ").replace("|", " ")
        "${polozka.indexHrace}:${bezpecneJmeno}:${polozka.karta.kod}"
    }
}

private fun retezecNaStychMariase(retezec: String): List<TahStychuMariase> {
    if (retezec.isBlank()) return emptyList()
    return retezec.split(";").mapNotNull { zaznam ->
        val casti = zaznam.split(":")
        if (casti.size < 3) return@mapNotNull null
        val idx = casti[0].toIntOrNull() ?: return@mapNotNull null
        val karta = Karta.zKodu(casti[2]) ?: return@mapNotNull null
        TahStychuMariase(idx, casti[1], karta)
    }
}

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
    val indexForhonta: Int,
    val naTahu: Int,
    val faze: FazeMariase,
    val trumf: BarvaKarty?,
    val volenoZTalonu: Boolean,
    val zavazek: ZavazekMariase,
    val zbytekProForhonta: List<Karta>,
    val talon: List<Karta>,
    val nasobekFlekHra: Int,
    val nasobekFlekSedma: Int,
    val flekovalHruObrance: Boolean,
    val flekovalSedmuObrance: Boolean,
    val pocetOdpovediFlekuVKole: Int,
    val aktualniStych: List<TahStychuMariase>,
    val cisloStychu: Int,
    val pocetStychuForhont: Int,
    val pocetStychuObrana: Int,
    val bodyKaretForhont: Int,
    val bodyKaretObrana: Int,
    val hlaskyForhont: Int,
    val hlaskyObrana: Int,
    val stavDevatehoStychuSedma: Int,
    val stavPoslednihoStychuSedma: Int,
    val posledniHlaska: String,
    val cisloTahu: Int,
    val variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA,
    val posledniUzavrenyStych: List<TahStychuMariase> = emptyList(),
    val posledniUzavrenyVitez: Int = -1,
    val rezimMariase: RezimMariase = RezimMariase.VOLENY,
    val pocetHracu: Int = 3,
    val puvodniForhont: Int = indexForhonta,
    val nejvyssiNabidka: ZavazekMariase? = null,
    val prozatimniVitezDrazby: Int = -1,
    val pasovaliHraci: List<Boolean> = listOf(false, false, false),
    val slozenoBezHry: Boolean = false,
    val lizaciBalicek: List<Karta> = emptyList(),
    val otocenyTrumf: Karta? = null,
    val odehraneKarty: List<Karta> = emptyList(),
    val chybejiciBarvyMaska: List<Int> = List(pocetHracu) { 0 },
    val nahlaseneHlaskyMaska: List<Int> = List(pocetHracu) { 0 }
) {
    val jeBetlNeboDurch: Boolean
        get() = zavazek.jeBezTrumfu

    fun doKompaktnihoRetezce(): String {
        val pole = ArrayList<Pair<String, String>>()
        pole.add("rm" to rezimMariase.kod)
        pole.add("ph" to pocetHracu.toString())
        pole.add("jm" to jmenaHracu.joinToString(";"))
        for (i in 0 until pocetHracu) {
            pole.add("r$i" to kartyNaRetezec(ruceHracu.getOrElse(i) { emptyList() }))
        }
        pole.add("ifh" to indexForhonta.toString())
        pole.add("pfh" to puvodniForhont.toString())
        pole.add("nt" to naTahu.toString())
        pole.add("fz" to faze.name)
        pole.add("tr" to (trumf?.name ?: ""))
        pole.add("vzt" to if (volenoZTalonu) "1" else "0")
        pole.add("zav" to zavazek.name)
        pole.add("zpf" to kartyNaRetezec(zbytekProForhonta))
        pole.add("tal" to kartyNaRetezec(talon))
        pole.add("nfh" to nasobekFlekHra.toString())
        pole.add("nfs" to nasobekFlekSedma.toString())
        pole.add("fho" to if (flekovalHruObrance) "1" else "0")
        pole.add("fso" to if (flekovalSedmuObrance) "1" else "0")
        pole.add("pof" to pocetOdpovediFlekuVKole.toString())
        pole.add("ast" to stychMariaseNaRetezec(aktualniStych))
        pole.add("cst" to cisloStychu.toString())
        pole.add("psf" to pocetStychuForhont.toString())
        pole.add("pso" to pocetStychuObrana.toString())
        pole.add("bkf" to bodyKaretForhont.toString())
        pole.add("bko" to bodyKaretObrana.toString())
        pole.add("hlf" to hlaskyForhont.toString())
        pole.add("hlo" to hlaskyObrana.toString())
        pole.add("sd9" to stavDevatehoStychuSedma.toString())
        pole.add("sp10" to stavPoslednihoStychuSedma.toString())
        pole.add("hl" to posledniHlaska)
        pole.add("ct" to cisloTahu.toString())
        pole.add("vp" to variantaPravidel.kod)
        pole.add("pus" to stychMariaseNaRetezec(posledniUzavrenyStych))
        pole.add("puv" to posledniUzavrenyVitez.toString())
        pole.add("nnb" to (nejvyssiNabidka?.name ?: ""))
        pole.add("pvd" to prozatimniVitezDrazby.toString())
        pole.add("pas" to pasovaliHraci.joinToString(",") { if (it) "1" else "0" })
        pole.add("sbh" to if (slozenoBezHry) "1" else "0")
        pole.add("liz" to kartyNaRetezec(lizaciBalicek))
        pole.add("otr" to (otocenyTrumf?.kod ?: ""))
        pole.add("ok" to kartyNaRetezec(odehraneKarty))
        pole.add("cbm" to chybejiciBarvyMaska.take(pocetHracu).joinToString(","))
        pole.add("nhm" to nahlaseneHlaskyMaska.take(pocetHracu).joinToString(","))
        return serializujMapuPoli(pole)
    }

    companion object {
        fun zKompaktnihoRetezce(data: String): StavMarias? {
            if (data.isBlank()) return null
            val r = PoleStavuReader(data)
            val rezim = RezimMariase.dleKodu(r.text("rm", RezimMariase.VOLENY.kod))
            val pocet = r.cislo("ph", rezim.pocetHracu).let { if (rezim == RezimMariase.LIZACI) 2 else if (it == 2) 2 else 3 }
            val zalozniJmena = listOf("Ty", "Karel (Bot)", "Pepa (Bot)")
            val jmena = r.text("jm").split(";").let { seznam ->
                List(pocet) { idx ->
                    seznam.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: zalozniJmena.getOrElse(idx) { "Hráč ${idx + 1}" }
                }
            }
            val ruce = List(pocet) { idx -> retezecNaKarty(r.text("r$idx")) }
            val ifh = r.cislo("ifh", 0).mod(pocet)
            val pasText = r.text("pas")
            val pasovali = if (pasText.isBlank()) {
                List(pocet) { false }
            } else {
                val casti = pasText.split(",")
                List(pocet) { idx -> casti.getOrNull(idx) == "1" }
            }
            val cbmText = r.text("cbm")
            val nacteneChybejici = if (cbmText.isBlank()) {
                List(pocet) { 0 }
            } else {
                val casti = cbmText.split(",")
                List(pocet) { idx -> casti.getOrNull(idx)?.toIntOrNull() ?: 0 }
            }
            val nhmText = r.text("nhm")
            val nacteneHlaskyMaska = if (nhmText.isBlank()) {
                List(pocet) { 0 }
            } else {
                val casti = nhmText.split(",")
                List(pocet) { idx -> casti.getOrNull(idx)?.toIntOrNull() ?: 0 }
            }
            return StavMarias(
                jmenaHracu = jmena,
                ruceHracu = ruce,
                indexForhonta = ifh,
                naTahu = r.cislo("nt", ifh).mod(pocet),
                faze = FazeMariase.dleNazvu(r.text("fz", FazeMariase.VOLBA_TRUMFU.name)),
                trumf = BarvaKarty.dleNazvu(r.text("tr")),
                volenoZTalonu = r.pravda("vzt", false),
                zavazek = ZavazekMariase.dleNazvu(r.text("zav")) ?: ZavazekMariase.HRA,
                zbytekProForhonta = retezecNaKarty(r.text("zpf")),
                talon = retezecNaKarty(r.text("tal")),
                nasobekFlekHra = r.cislo("nfh", 1).coerceAtLeast(1),
                nasobekFlekSedma = r.cislo("nfs", 1).coerceAtLeast(1),
                flekovalHruObrance = r.pravda("fho", false),
                flekovalSedmuObrance = r.pravda("fso", false),
                pocetOdpovediFlekuVKole = r.cislo("pof", 0),
                aktualniStych = retezecNaStychMariase(r.text("ast")),
                cisloStychu = r.cislo("cst", 1),
                pocetStychuForhont = r.cislo("psf", 0),
                pocetStychuObrana = r.cislo("pso", 0),
                bodyKaretForhont = r.cislo("bkf", 0),
                bodyKaretObrana = r.cislo("bko", 0),
                hlaskyForhont = r.cislo("hlf", 0),
                hlaskyObrana = r.cislo("hlo", 0),
                stavDevatehoStychuSedma = r.cislo("sd9", 0),
                stavPoslednihoStychuSedma = r.cislo("sp10", 0),
                posledniHlaska = r.text("hl"),
                cisloTahu = r.cislo("ct", 1),
                variantaPravidel = VariantaPravidel.dleKodu(r.text("vp", VariantaPravidel.HOSPODSKA.kod)),
                posledniUzavrenyStych = retezecNaStychMariase(r.text("pus")),
                posledniUzavrenyVitez = r.cislo("puv", -1),
                rezimMariase = rezim,
                pocetHracu = pocet,
                puvodniForhont = r.cislo("pfh", ifh).mod(pocet),
                nejvyssiNabidka = ZavazekMariase.dleNazvu(r.text("nnb")),
                prozatimniVitezDrazby = r.cislo("pvd", -1),
                pasovaliHraci = pasovali,
                slozenoBezHry = r.pravda("sbh", false),
                lizaciBalicek = retezecNaKarty(r.text("liz")),
                otocenyTrumf = Karta.zKodu(r.text("otr")),
                odehraneKarty = retezecNaKarty(r.text("ok")),
                chybejiciBarvyMaska = nacteneChybejici,
                nahlaseneHlaskyMaska = nacteneHlaskyMaska
            )
        }
    }
}

object MariasEngine {

    fun novaHra(
        jmenaHracu: List<String>,
        indexForhonta: Int = 0,
        predmichanyBalicek: List<Karta>? = null,
        variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA,
        rezimMariase: RezimMariase = RezimMariase.VOLENY
    ): StavMarias {
        val balicek = (predmichanyBalicek?.takeIf { it.size == 32 } ?: Karta.vytvorZamichanyBalicek()).toMutableList()

        if (rezimMariase == RezimMariase.LIZACI) {
            val zalozni2 = listOf("Ty", "Karel (Bot)")
            val jmena2 = List(2) { idx -> jmenaHracu.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: zalozni2[idx] }
            val start2 = indexForhonta.mod(2)
            return MariasVariantyEngine.novaHraLizaci(jmena2, start2, balicek, variantaPravidel)
        }

        val zalozni3 = listOf("Ty", "Karel (Bot)", "Pepa (Bot)")
        val jmena3 = List(3) { idx -> jmenaHracu.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: zalozni3[idx] }
        val startForhont = indexForhonta.mod(3)

        if (rezimMariase == RezimMariase.LICITOVANY) {
            return MariasVariantyEngine.novaHraLicitovany(jmena3, startForhont, balicek, variantaPravidel)
        }

        val ruceMut = List(3) { ArrayList<Karta>(10) }
        repeat(7) { if (balicek.isNotEmpty()) ruceMut[startForhont].add(balicek.removeAt(0)) }
        val obrance1 = (startForhont + 1) % 3
        val obrance2 = (startForhont + 2) % 3
        repeat(5) { if (balicek.isNotEmpty()) ruceMut[obrance1].add(balicek.removeAt(0)) }
        repeat(5) { if (balicek.isNotEmpty()) ruceMut[obrance2].add(balicek.removeAt(0)) }
        val zbytek5 = ArrayList<Karta>(5)
        repeat(5) { if (balicek.isNotEmpty()) zbytek5.add(balicek.removeAt(0)) }
        repeat(5) { if (balicek.isNotEmpty()) ruceMut[obrance1].add(balicek.removeAt(0)) }
        repeat(5) { if (balicek.isNotEmpty()) ruceMut[obrance2].add(balicek.removeAt(0)) }

        val serazene = ruceMut.map { Karta.seradRuku(it) }
        val rezimPravidelText = variantaPravidel.kratkyNazev.lowercase()

        return StavMarias(
            jmenaHracu = jmena3,
            ruceHracu = serazene,
            indexForhonta = startForhont,
            naTahu = startForhont,
            faze = FazeMariase.VOLBA_TRUMFU,
            trumf = null,
            volenoZTalonu = false,
            zavazek = ZavazekMariase.HRA,
            zbytekProForhonta = zbytek5,
            talon = emptyList(),
            nasobekFlekHra = 1,
            nasobekFlekSedma = 1,
            flekovalHruObrance = false,
            flekovalSedmuObrance = false,
            pocetOdpovediFlekuVKole = 0,
            aktualniStych = emptyList(),
            cisloStychu = 1,
            pocetStychuForhont = 0,
            pocetStychuObrana = 0,
            bodyKaretForhont = 0,
            bodyKaretObrana = 0,
            hlaskyForhont = 0,
            hlaskyObrana = 0,
            stavDevatehoStychuSedma = 0,
            stavPoslednihoStychuSedma = 0,
            posledniHlaska = "Volený mariáš ($rezimPravidelText) — trumf volí ${jmena3[startForhont]}.",
            cisloTahu = 1,
            variantaPravidel = variantaPravidel,
            posledniUzavrenyStych = emptyList(),
            posledniUzavrenyVitez = -1,
            rezimMariase = RezimMariase.VOLENY,
            pocetHracu = 3,
            puvodniForhont = startForhont,
            odehraneKarty = emptyList(),
            chybejiciBarvyMaska = listOf(0, 0, 0),
            nahlaseneHlaskyMaska = listOf(0, 0, 0)
        )
    }

    fun zjistiPovoleneNabidkyDrazby(stav: StavMarias, indexHrace: Int): List<ZavazekMariase> {
        return MariasVariantyEngine.zjistiPovoleneNabidkyDrazby(stav, indexHrace)
    }

    fun provedNabidkuDrazby(stav: StavMarias, indexHrace: Int, nabidka: ZavazekMariase?): StavMarias {
        return MariasVariantyEngine.provedNabidkuDrazby(stav, indexHrace, nabidka)
    }

    fun muzeVymenitTrumfovouSedmu(stav: StavMarias, indexHrace: Int): Boolean {
        return MariasVariantyEngine.muzeVymenitTrumfovouSedmu(stav, indexHrace)
    }

    fun vymenTrumfovouSedmu(stav: StavMarias, indexHrace: Int): StavMarias {
        return MariasVariantyEngine.vymenTrumfovouSedmu(stav, indexHrace)
    }

    fun nazevStupneFleku(novyNasobek: Int): String {
        return when (novyNasobek) {
            2 -> "Flek"
            4 -> "Re"
            8 -> "Tutti"
            16 -> "Boty"
            32 -> "Kalhoty"
            else -> "${novyNasobek}×"
        }
    }

    fun maPravouSedmu(stav: StavMarias, vybraneDoTalonu: List<Karta> = emptyList()): Boolean {
        val trumf = stav.trumf ?: return false
        val ruka = stav.ruceHracu.getOrNull(stav.indexForhonta) ?: return false
        val poOdlozeni = ruka.filterNot { it in vybraneDoTalonu }
        return Karta(trumf, HodnotaKarty.R7) in poOdlozeni
    }

    fun maPraveDveSedmy(stav: StavMarias, vybraneDoTalonu: List<Karta> = emptyList()): Boolean {
        if (!maPravouSedmu(stav, vybraneDoTalonu)) return false
        val ruka = stav.ruceHracu.getOrNull(stav.indexForhonta) ?: return false
        val poOdlozeni = ruka.filterNot { it in vybraneDoTalonu }
        return poOdlozeni.count { it.hodnota == HodnotaKarty.R7 } >= 2
    }

    fun zvolTrumfNeboHruBezTrumfu(
        stav: StavMarias,
        zvolenyTrumf: BarvaKarty?,
        volitZTalonu: Boolean,
        planovanyZavazek: ZavazekMariase = ZavazekMariase.HRA
    ): StavMarias {
        if (stav.faze != FazeMariase.VOLBA_TRUMFU) return stav
        val fh = stav.indexForhonta
        val jmeno = stav.jmenaHracu.getOrElse(fh) { "Forhont" }
        val spojena12 = stav.ruceHracu[fh] + stav.zbytekProForhonta

        if (planovanyZavazek.jeBezTrumfu) {
            if (stav.rezimMariase == RezimMariase.LICITOVANY) {
                if (planovanyZavazek == ZavazekMariase.BETL && !MariasVariantyEngine.muzeVolitRovnouBetl(stav)) return stav
                if (planovanyZavazek == ZavazekMariase.DURCH && !MariasVariantyEngine.muzeVolitRovnouDurch(stav)) return stav
            }
            val serazena = Karta.seradRuku(spojena12, trumf = null, betlNeboDurch = true)
            val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == fh) serazena else r }
            return stav.copy(
                ruceHracu = noveRuce,
                faze = FazeMariase.ODKLADANI_TALONU,
                trumf = null,
                volenoZTalonu = false,
                zavazek = planovanyZavazek,
                zbytekProForhonta = emptyList(),
                posledniHlaska = "$jmeno volí ${planovanyZavazek.nazev} a odkládá 2 karty do talonu.",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        if (volitZTalonu && stav.rezimMariase == RezimMariase.VOLENY && stav.zbytekProForhonta.isNotEmpty()) {
            val otocena = stav.zbytekProForhonta.last()
            val trumfBarva = otocena.barva
            val serazena = Karta.seradRuku(spojena12, trumf = trumfBarva, betlNeboDurch = false)
            val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == fh) serazena else r }
            return stav.copy(
                ruceHracu = noveRuce,
                faze = FazeMariase.ODKLADANI_TALONU,
                trumf = trumfBarva,
                volenoZTalonu = true,
                zavazek = ZavazekMariase.HRA,
                zbytekProForhonta = emptyList(),
                posledniHlaska = "$jmeno volí z talonu (${otocena.celyNazev()} -> trumf ${trumfBarva.ceskyNazev}) a odkládá 2 karty.",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        val barva = zvolenyTrumf ?: return stav
        if (stav.rezimMariase == RezimMariase.LICITOVANY && barva !in MariasVariantyEngine.zjistiPovoleneTrumfyPoDrazbe(stav)) {
            return stav
        }
        val serazena = Karta.seradRuku(spojena12, trumf = barva, betlNeboDurch = false)
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == fh) serazena else r }
        val vychoziZavazek = if (stav.rezimMariase == RezimMariase.LICITOVANY && stav.nejvyssiNabidka != null) {
            stav.nejvyssiNabidka
        } else {
            ZavazekMariase.HRA
        }
        val predbeznyStav = stav.copy(
            ruceHracu = noveRuce,
            faze = FazeMariase.ODKLADANI_TALONU,
            trumf = barva,
            volenoZTalonu = false,
            zavazek = vychoziZavazek,
            zbytekProForhonta = emptyList()
        )
        val platneZavazky = zjistiPovoleneZavazkyTalonu(predbeznyStav)
        val upravenyZavazek = if (vychoziZavazek in platneZavazky) {
            vychoziZavazek
        } else {
            platneZavazky.firstOrNull() ?: ZavazekMariase.HRA
        }
        return predbeznyStav.copy(
            zavazek = upravenyZavazek,
            posledniHlaska = "$jmeno volí trumf ${barva.ceskyNazev} a odkládá 2 karty do talonu.",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun zjistiPovoleneZavazkyTalonu(stav: StavMarias): List<ZavazekMariase> {
        val minVaha = if (stav.rezimMariase == RezimMariase.LICITOVANY) {
            stav.nejvyssiNabidka?.vahaDrazby ?: 1
        } else {
            1
        }

        if (stav.zavazek.jeBezTrumfu) {
            return listOf(ZavazekMariase.BETL, ZavazekMariase.DURCH)
                .filter { it.vahaDrazby >= minVaha }
                .ifEmpty { listOf(stav.zavazek) }
        }

        if (stav.volenoZTalonu) {
            val zTalonuMoznosti = listOf(
                ZavazekMariase.HRA,
                ZavazekMariase.HRA_A_SEDMA,
                ZavazekMariase.STO,
                ZavazekMariase.STO_A_SEDMA,
                ZavazekMariase.HRA_A_DVE_SEDMY,
                ZavazekMariase.STO_A_DVE_SEDMY
            )
            return zTalonuMoznosti.filter { zav ->
                if (stav.variantaPravidel == VariantaPravidel.SVAZOVA) {
                    when {
                        zav.hlasiDveSedmy -> maPraveDveSedmy(stav)
                        zav.hlasiSedmu -> maPravouSedmu(stav)
                        else -> true
                    }
                } else {
                    true
                }
            }
        }

        val poradiTalonu = listOf(
            ZavazekMariase.HRA,
            ZavazekMariase.HRA_A_SEDMA,
            ZavazekMariase.STO,
            ZavazekMariase.STO_A_SEDMA,
            ZavazekMariase.HRA_A_DVE_SEDMY,
            ZavazekMariase.STO_A_DVE_SEDMY,
            ZavazekMariase.BETL,
            ZavazekMariase.DURCH
        )

        val vysledek = poradiTalonu.filter { zav ->
            if (zav.vahaDrazby < minVaha) return@filter false
            if (stav.variantaPravidel == VariantaPravidel.SVAZOVA) {
                when {
                    zav.hlasiDveSedmy -> maPraveDveSedmy(stav)
                    zav.hlasiSedmu -> maPravouSedmu(stav)
                    else -> true
                }
            } else {
                true
            }
        }
        return vysledek.ifEmpty { listOf(ZavazekMariase.STO) }
    }

    fun zjistiKartyPovoleneDoTalonu(stav: StavMarias, planovanyZavazek: ZavazekMariase): Set<Karta> {
        val ruka = stav.ruceHracu.getOrNull(stav.indexForhonta) ?: return emptySet()
        if (planovanyZavazek.jeBezTrumfu) {
            return ruka.toSet()
        }

        var povolene = ruka.filter {
            it.hodnota != HodnotaKarty.ESO && it.hodnota != HodnotaKarty.R10
        }

        if (stav.variantaPravidel == VariantaPravidel.SVAZOVA && stav.trumf != null) {
            val trumfSedma = Karta(stav.trumf, HodnotaKarty.R7)
            if (planovanyZavazek.hlasiSedmu) {
                povolene = povolene.filterNot { it == trumfSedma }
            }
            if (planovanyZavazek.hlasiDveSedmy) {
                val vsechnySedmy = ruka.filter { it.hodnota == HodnotaKarty.R7 }
                if (vsechnySedmy.size <= 2) {
                    povolene = povolene.filterNot { it.hodnota == HodnotaKarty.R7 }
                }
            }
        }

        return if (povolene.size >= 2) povolene.toSet() else ruka.toSet()
    }

    fun potvrdTalonAZavazek(
        stav: StavMarias,
        odkladaneKarty: List<Karta>,
        zvolenyZavazek: ZavazekMariase
    ): StavMarias {
        if (stav.faze != FazeMariase.ODKLADANI_TALONU || odkladaneKarty.size != 2) return stav
        val povoleneZavazky = zjistiPovoleneZavazkyTalonu(stav)
        val platnyZavazek = if (zvolenyZavazek in povoleneZavazky) {
            zvolenyZavazek
        } else {
            povoleneZavazky.firstOrNull() ?: return stav
        }

        val povoleneKarty = zjistiKartyPovoleneDoTalonu(stav, platnyZavazek)
        if (!odkladaneKarty.all { it in povoleneKarty }) return stav
        if (stav.variantaPravidel == VariantaPravidel.SVAZOVA) {
            if (platnyZavazek.hlasiDveSedmy && !maPraveDveSedmy(stav, odkladaneKarty)) return stav
            if (platnyZavazek.hlasiSedmu && !maPravouSedmu(stav, odkladaneKarty)) return stav
        }

        val fh = stav.indexForhonta
        val novyTrumf = if (platnyZavazek.jeBezTrumfu) null else stav.trumf
        val bezTrumfu = platnyZavazek.jeBezTrumfu

        val zbylaRukaForhonta = stav.ruceHracu[fh].toMutableList()
        for (k in odkladaneKarty) {
            zbylaRukaForhonta.remove(k)
        }

        val noveRuce = stav.ruceHracu.mapIndexed { idx, r ->
            val zdroj = if (idx == fh) zbylaRukaForhonta else r
            Karta.seradRuku(zdroj, trumf = novyTrumf, betlNeboDurch = bezTrumfu)
        }

        val prvniObrance = (fh + 1) % 3
        val jmenoFh = stav.jmenaHracu.getOrElse(fh) { "Forhont" }
        val popisTrumfu = if (bezTrumfu) "bez trumfů" else (novyTrumf?.ceskyNazev ?: "")

        return stav.copy(
            ruceHracu = noveRuce,
            trumf = novyTrumf,
            volenoZTalonu = if (bezTrumfu) false else stav.volenoZTalonu,
            zavazek = platnyZavazek,
            talon = odkladaneKarty,
            faze = FazeMariase.FLEKOVANI,
            naTahu = prvniObrance,
            nasobekFlekHra = 1,
            nasobekFlekSedma = 1,
            flekovalHruObrance = false,
            flekovalSedmuObrance = false,
            pocetOdpovediFlekuVKole = 0,
            posledniHlaska = "$jmenoFh odložil talon a hlásí ${platnyZavazek.nazev} ($popisTrumfu)! Vyjadřuje se ${stav.jmenaHracu[prvniObrance]}.",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun muzeHracFlekHry(stav: StavMarias, indexHrace: Int): Boolean {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace) return false
        if (stav.nasobekFlekHra >= 32) return false
        return if (indexHrace == stav.indexForhonta) {
            stav.flekovalHruObrance && (stav.nasobekFlekHra == 2 || stav.nasobekFlekHra == 8)
        } else {
            !stav.flekovalHruObrance && (stav.nasobekFlekHra == 1 || stav.nasobekFlekHra == 4 || stav.nasobekFlekHra == 16)
        }
    }

    fun muzeHracFlekSedmy(stav: StavMarias, indexHrace: Int): Boolean {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace) return false
        if (!stav.zavazek.hlasiSedmu || stav.nasobekFlekSedma >= 32) return false
        return if (indexHrace == stav.indexForhonta) {
            stav.flekovalSedmuObrance && (stav.nasobekFlekSedma == 2 || stav.nasobekFlekSedma == 8)
        } else {
            !stav.flekovalSedmuObrance && (stav.nasobekFlekSedma == 1 || stav.nasobekFlekSedma == 4 || stav.nasobekFlekSedma == 16)
        }
    }

    internal fun kdoVynasiPrvniStych(stav: StavMarias): Int {
        return if (stav.rezimMariase == RezimMariase.LICITOVANY && !stav.jeBetlNeboDurch) {
            stav.puvodniForhont
        } else {
            stav.indexForhonta
        }
    }

    fun odpovezNaFlekovani(
        stav: StavMarias,
        indexHrace: Int,
        flekHra: Boolean,
        flekSedma: Boolean
    ): StavMarias {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace) return stav

        val opravduHra = flekHra && muzeHracFlekHry(stav, indexHrace)
        val opravduSedma = flekSedma && muzeHracFlekSedmy(stav, indexHrace)
        val novyNasobekHra = if (opravduHra) (stav.nasobekFlekHra * 2).coerceAtMost(32) else stav.nasobekFlekHra
        val novyNasobekSedma = if (opravduSedma) (stav.nasobekFlekSedma * 2).coerceAtMost(32) else stav.nasobekFlekSedma
        val jmeno = stav.jmenaHracu.getOrElse(indexHrace) { "Hráč" }

        val popisAkce = when {
            opravduHra && opravduSedma ->
                "hlásí ${nazevStupneFleku(novyNasobekHra)} na hru i ${nazevStupneFleku(novyNasobekSedma)} na sedmu"
            opravduHra ->
                "hlásí ${nazevStupneFleku(novyNasobekHra)} na hru (${novyNasobekHra}×)"
            opravduSedma ->
                "hlásí ${nazevStupneFleku(novyNasobekSedma)} na sedmu (${novyNasobekSedma}×)"
            else ->
                "říká Dobrý"
        }

        if (indexHrace == stav.indexForhonta) {
            if (!opravduHra && !opravduSedma) {
                val vynasi = kdoVynasiPrvniStych(stav)
                return stav.copy(
                    faze = FazeMariase.SEHRAVKA,
                    naTahu = vynasi,
                    flekovalHruObrance = false,
                    flekovalSedmuObrance = false,
                    pocetOdpovediFlekuVKole = 0,
                    posledniHlaska = "$jmeno $popisAkce — začíná sehrávka, vynáší ${stav.jmenaHracu[vynasi]}!",
                    cisloTahu = stav.cisloTahu + 1
                )
            }

            val prvniObrance = (stav.indexForhonta + 1) % 3
            val docasny = stav.copy(
                nasobekFlekHra = novyNasobekHra,
                nasobekFlekSedma = novyNasobekSedma,
                flekovalHruObrance = false,
                flekovalSedmuObrance = false,
                naTahu = prvniObrance,
                pocetOdpovediFlekuVKole = 0
            )
            val muzeObranaZvednout = muzeHracFlekHry(docasny, prvniObrance) || muzeHracFlekSedmy(docasny, prvniObrance)
            return if (muzeObranaZvednout) {
                docasny.copy(
                    posledniHlaska = "$jmeno $popisAkce! Vyjadřuje se ${stav.jmenaHracu[prvniObrance]}.",
                    cisloTahu = stav.cisloTahu + 1
                )
            } else {
                val vynasi = kdoVynasiPrvniStych(stav)
                docasny.copy(
                    faze = FazeMariase.SEHRAVKA,
                    naTahu = vynasi,
                    posledniHlaska = "$jmeno $popisAkce — začíná sehrávka, vynáší ${stav.jmenaHracu[vynasi]}!",
                    cisloTahu = stav.cisloTahu + 1
                )
            }
        }

        val zvedlaHruObrana = stav.flekovalHruObrance || opravduHra
        val zvedlaSedmuObrana = stav.flekovalSedmuObrance || opravduSedma
        val novyPocetOdpovedi = stav.pocetOdpovediFlekuVKole + 1
        val druhyObrance = (stav.indexForhonta + 2) % 3

        if (novyPocetOdpovedi == 1 && indexHrace != druhyObrance) {
            val docasnyProDruheho = stav.copy(
                nasobekFlekHra = novyNasobekHra,
                nasobekFlekSedma = novyNasobekSedma,
                flekovalHruObrance = zvedlaHruObrana,
                flekovalSedmuObrance = zvedlaSedmuObrana,
                naTahu = druhyObrance,
                pocetOdpovediFlekuVKole = 1
            )
            val muzeDruhyCokoliv = muzeHracFlekHry(docasnyProDruheho, druhyObrance) ||
                muzeHracFlekSedmy(docasnyProDruheho, druhyObrance)
            if (muzeDruhyCokoliv) {
                return docasnyProDruheho.copy(
                    posledniHlaska = "$jmeno $popisAkce — vyjadřuje se ${stav.jmenaHracu[druhyObrance]}.",
                    cisloTahu = stav.cisloTahu + 1
                )
            }
        }

        val docasnyProForhonta = stav.copy(
            nasobekFlekHra = novyNasobekHra,
            nasobekFlekSedma = novyNasobekSedma,
            flekovalHruObrance = zvedlaHruObrana,
            flekovalSedmuObrance = zvedlaSedmuObrana,
            naTahu = stav.indexForhonta,
            pocetOdpovediFlekuVKole = 0
        )
        val muzeForhontZvednout = muzeHracFlekHry(docasnyProForhonta, stav.indexForhonta) ||
            muzeHracFlekSedmy(docasnyProForhonta, stav.indexForhonta)

        return if (muzeForhontZvednout) {
            docasnyProForhonta.copy(
                posledniHlaska = "$jmeno $popisAkce! Reaguje forhont ${stav.jmenaHracu[stav.indexForhonta]}.",
                cisloTahu = stav.cisloTahu + 1
            )
        } else {
            val vynasi = kdoVynasiPrvniStych(stav)
            docasnyProForhonta.copy(
                faze = FazeMariase.SEHRAVKA,
                naTahu = vynasi,
                flekovalHruObrance = false,
                flekovalSedmuObrance = false,
                posledniHlaska = "$jmeno $popisAkce — začíná sehrávka, vynáší ${stav.jmenaHracu[vynasi]}!",
                cisloTahu = stav.cisloTahu + 1
            )
        }
    }

    fun zjistiPovoleneKarty(stav: StavMarias, indexHrace: Int): Set<Karta> {
        if (stav.rezimMariase == RezimMariase.LIZACI) {
            return MariasVariantyEngine.zjistiPovoleneKartyLizaci(stav, indexHrace)
        }
        if (stav.faze != FazeMariase.SEHRAVKA || stav.naTahu != indexHrace) return emptySet()
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()
        if (ruka.isEmpty()) return emptySet()
        if (stav.aktualniStych.isEmpty()) return ruka.toSet()

        val barvaVynosu = stav.aktualniStych.first().karta.barva
        val bezTrumfu = stav.jeBetlNeboDurch
        val trumf = if (bezTrumfu) null else stav.trumf

        val vBarveVynosu = ruka.filter { it.barva == barvaVynosu }
        if (vBarveVynosu.isNotEmpty()) {
            val uzPadlCiziTrumf = trumf != null &&
                barvaVynosu != trumf &&
                stav.aktualniStych.any { it.karta.barva == trumf }
            if (uzPadlCiziTrumf) {
                return vBarveVynosu.toSet()
            }
            val nejvyssiSilaVBarve = stav.aktualniStych
                .filter { it.karta.barva == barvaVynosu }
                .maxOf { it.karta.silaProMarias(bezTrumfu) }
            val vyssi = vBarveVynosu.filter { it.silaProMarias(bezTrumfu) > nejvyssiSilaVBarve }
            return (if (vyssi.isNotEmpty()) vyssi else vBarveVynosu).toSet()
        }

        if (trumf != null) {
            val trumfyVRuce = ruka.filter { it.barva == trumf }
            if (trumfyVRuce.isNotEmpty()) {
                val trumfyVeStychu = stav.aktualniStych.filter { it.karta.barva == trumf }
                if (trumfyVeStychu.isNotEmpty()) {
                    val nejvyssiTrumf = trumfyVeStychu.maxOf { it.karta.silaProMarias(false) }
                    val vyssiTrumfy = trumfyVRuce.filter { it.silaProMarias(false) > nejvyssiTrumf }
                    return (if (vyssiTrumfy.isNotEmpty()) vyssiTrumfy else trumfyVRuce).toSet()
                }
                return trumfyVRuce.toSet()
            }
        }

        return ruka.toSet()
    }

    internal fun urciVitezeStychu(
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
            trumfoveTahy.maxBy { it.karta.silaProMarias(false) }
        } else {
            stych.filter { it.karta.barva == barvaVynosu }
                .maxBy { it.karta.silaProMarias(bezTrumfu) }
        }
    }

    fun zahrajKartu(stav: StavMarias, indexHrace: Int, karta: Karta): StavMarias {
        if (stav.rezimMariase == RezimMariase.LIZACI) {
            val poTahu = MariasVariantyEngine.zahrajKartuLizaci(stav, indexHrace, karta)
            if (poTahu === stav || poTahu.cisloTahu == stav.cisloTahu) {
                return poTahu
            }
            var noveMaskyLiz = List(poTahu.pocetHracu) { idx -> poTahu.chybejiciBarvyMaska.getOrElse(idx) { 0 } }
            if (stav.lizaciBalicek.isEmpty() && stav.aktualniStych.isNotEmpty()) {
                val barvaVynosuLiz = stav.aktualniStych.first().karta.barva
                if (karta.barva != barvaVynosuLiz) {
                    var m = noveMaskyLiz.getOrElse(indexHrace) { 0 } or (1 shl barvaVynosuLiz.ordinal)
                    val tr = stav.trumf
                    if (tr != null && karta.barva != tr) {
                        m = m or (1 shl tr.ordinal)
                    }
                    noveMaskyLiz = noveMaskyLiz.mapIndexed { idx, puv -> if (idx == indexHrace) m else puv }
                }
            }
            var noveHlaskyLiz = List(poTahu.pocetHracu) { idx -> poTahu.nahlaseneHlaskyMaska.getOrElse(idx) { 0 } }
            val padlaHlaskaLiz = poTahu.hlaskyForhont > stav.hlaskyForhont || poTahu.hlaskyObrana > stav.hlaskyObrana
            if (padlaHlaskaLiz) {
                noveHlaskyLiz = noveHlaskyLiz.mapIndexed { idx, puv ->
                    if (idx == indexHrace) puv or (1 shl karta.barva.ordinal) else puv
                }
            }
            return poTahu.copy(
                odehraneKarty = stav.odehraneKarty + karta,
                chybejiciBarvyMaska = noveMaskyLiz,
                nahlaseneHlaskyMaska = noveHlaskyLiz
            )
        }

        val povolene = zjistiPovoleneKarty(stav, indexHrace)
        if (karta !in povolene) return stav

        val jmeno = stav.jmenaHracu.getOrElse(indexHrace) { "Hráč" }
        val zbylaRuka = Karta.seradRuku(
            stav.ruceHracu[indexHrace].filterNot { it == karta },
            trumf = if (stav.jeBetlNeboDurch) null else stav.trumf,
            betlNeboDurch = stav.jeBetlNeboDurch
        )
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) zbylaRuka else r }

        var zakladMasky = List(3) { idx -> stav.chybejiciBarvyMaska.getOrElse(idx) { 0 } }
        if (stav.aktualniStych.isNotEmpty()) {
            val barvaVynosu = stav.aktualniStych.first().karta.barva
            if (karta.barva != barvaVynosu) {
                var novaMaskaHrace = zakladMasky[indexHrace] or (1 shl barvaVynosu.ordinal)
                if (!stav.jeBetlNeboDurch && stav.trumf != null && karta.barva != stav.trumf) {
                    novaMaskaHrace = novaMaskaHrace or (1 shl stav.trumf.ordinal)
                }
                zakladMasky = zakladMasky.mapIndexed { idx, m -> if (idx == indexHrace) novaMaskaHrace else m }
            }
        }
        var zakladHlaskyMaska = List(3) { idx -> stav.nahlaseneHlaskyMaska.getOrElse(idx) { 0 } }
        val noveOdehrane = stav.odehraneKarty + karta

        var noveHlaskyForhont = stav.hlaskyForhont
        var noveHlaskyObrana = stav.hlaskyObrana
        var hlaskaPrirustekText = ""

        if (!stav.jeBetlNeboDurch && (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL)) {
            val parova = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
            if (zbylaRuka.any { it.barva == karta.barva && it.hodnota == parova }) {
                val hodnotaHlasky = if (karta.barva == stav.trumf) 40 else 20
                if (indexHrace == stav.indexForhonta) {
                    noveHlaskyForhont += hodnotaHlasky
                } else {
                    noveHlaskyObrana += hodnotaHlasky
                }
                zakladHlaskyMaska = zakladHlaskyMaska.mapIndexed { idx, m ->
                    if (idx == indexHrace) m or (1 shl karta.barva.ordinal) else m
                }
                hlaskaPrirustekText = " (+hláška $hodnotaHlasky!)"
            }
        }

        val novyStych = stav.aktualniStych + TahStychuMariase(indexHrace, jmeno, karta)

        if (novyStych.size < 3) {
            val dalsi = (indexHrace + 1) % 3
            val akce = if (novyStych.size == 1) "vynáší" else "přikládá"
            return stav.copy(
                ruceHracu = noveRuce,
                aktualniStych = novyStych,
                naTahu = dalsi,
                hlaskyForhont = noveHlaskyForhont,
                hlaskyObrana = noveHlaskyObrana,
                posledniUzavrenyStych = emptyList(),
                posledniUzavrenyVitez = -1,
                posledniHlaska = "$jmeno $akce ${karta.celyNazev()}$hlaskaPrirustekText.",
                cisloTahu = stav.cisloTahu + 1,
                odehraneKarty = noveOdehrane,
                chybejiciBarvyMaska = zakladMasky,
                nahlaseneHlaskyMaska = zakladHlaskyMaska
            )
        }

        val viteznyTah = urciVitezeStychu(novyStych, stav.trumf, stav.jeBetlNeboDurch)
        val vitez = viteznyTah.indexHrace
        val bereForhont = vitez == stav.indexForhonta
        val jeDesatyStych = stav.cisloStychu >= 10 || noveRuce.all { it.isEmpty() }

        val bodyVeStychu = novyStych.sumOf { it.karta.hodnota.bodyMariasSedma } + if (jeDesatyStych) 10 else 0
        val noveBodyForhont = stav.bodyKaretForhont + if (bereForhont) bodyVeStychu else 0
        val noveBodyObrana = stav.bodyKaretObrana + if (!bereForhont) bodyVeStychu else 0
        val noveStychyForhont = stav.pocetStychuForhont + if (bereForhont) 1 else 0
        val noveStychyObrana = stav.pocetStychuObrana + if (!bereForhont) 1 else 0

        var novyStav9Sedma = stav.stavDevatehoStychuSedma
        if (!stav.jeBetlNeboDurch && stav.cisloStychu == 9) {
            val tahFh = novyStych.firstOrNull { it.indexHrace == stav.indexForhonta }
            if (tahFh != null && tahFh.karta.hodnota == HodnotaKarty.R7 && tahFh.karta.barva != stav.trumf) {
                novyStav9Sedma = if (bereForhont) 1 else -1
            }
        }

        var novyStav10Sedma = stav.stavPoslednihoStychuSedma
        if (!stav.jeBetlNeboDurch && jeDesatyStych && stav.trumf != null) {
            val trumfSedma = Karta(stav.trumf, HodnotaKarty.R7)
            val tahTrumf7 = novyStych.firstOrNull { it.karta == trumfSedma }
            if (tahTrumf7 != null) {
                novyStav10Sedma = if (tahTrumf7.indexHrace == stav.indexForhonta) {
                    if (bereForhont) 1 else -1
                } else {
                    if (!bereForhont) 2 else -2
                }
            }
        }

        val padlBetl = stav.zavazek == ZavazekMariase.BETL && bereForhont
        val padlDurch = stav.zavazek == ZavazekMariase.DURCH && !bereForhont
        val jeKonecSehravky = jeDesatyStych || padlBetl || padlDurch

        val jmenoViteze = stav.jmenaHracu.getOrElse(vitez) { "Hráč" }
        val textHlasky = when {
            padlBetl ->
                "$jmenoViteze musel vzít štych — Betl padl!"
            padlDurch ->
                "$jmenoViteze bere štych forhontovi — Durch padl!"
            jeDesatyStych ->
                "$jmenoViteze bere poslední 10. štych (+10 b.)$hlaskaPrirustekText! Konec rozdání."
            else ->
                "$jmenoViteze bere ${stav.cisloStychu}. štych (${bodyVeStychu} b.)$hlaskaPrirustekText a vynáší."
        }

        return stav.copy(
            ruceHracu = noveRuce,
            aktualniStych = emptyList(),
            naTahu = vitez,
            cisloStychu = if (jeKonecSehravky) stav.cisloStychu else stav.cisloStychu + 1,
            pocetStychuForhont = noveStychyForhont,
            pocetStychuObrana = noveStychyObrana,
            bodyKaretForhont = noveBodyForhont,
            bodyKaretObrana = noveBodyObrana,
            hlaskyForhont = noveHlaskyForhont,
            hlaskyObrana = noveHlaskyObrana,
            stavDevatehoStychuSedma = novyStav9Sedma,
            stavPoslednihoStychuSedma = novyStav10Sedma,
            faze = if (jeKonecSehravky) FazeMariase.KONEC else FazeMariase.SEHRAVKA,
            posledniUzavrenyStych = novyStych,
            posledniUzavrenyVitez = vitez,
            posledniHlaska = textHlasky,
            cisloTahu = stav.cisloTahu + 1,
            odehraneKarty = noveOdehrane,
            chybejiciBarvyMaska = zakladMasky,
            nahlaseneHlaskyMaska = zakladHlaskyMaska
        )
    }

    fun provedTahBota(stav: StavMarias): StavMarias {
        return MariasBotAi.provedTah(stav)
    }

    internal fun spoctiJednotkyForhonta(
        zavazek: ZavazekMariase,
        trumf: BarvaKarty?,
        nasobekFlekHra: Int,
        nasobekFlekSedma: Int,
        pocetStychuForhont: Int,
        pocetStychuObrana: Int,
        bodyKaretForhont: Int,
        bodyKaretObrana: Int,
        bodyTalonu: Int,
        hlaskyForhont: Int,
        hlaskyObrana: Int,
        stav9Sedma: Int,
        stav10Sedma: Int
    ): Int {
        if (zavazek == ZavazekMariase.BETL) {
            val s = 5 * nasobekFlekHra
            return if (pocetStychuForhont == 0) s else -s
        }
        if (zavazek == ZavazekMariase.DURCH) {
            val s = 10 * nasobekFlekHra
            return if (pocetStychuObrana == 0) s else -s
        }

        val cervene = if (trumf == BarvaKarty.CERVENE) 2 else 1
        val platneHlF = if (pocetStychuForhont > 0) hlaskyForhont else 0
        val platneHlO = if (pocetStychuObrana > 0) hlaskyObrana else 0
        val celkemF = bodyKaretForhont + platneHlF
        val celkemO = bodyKaretObrana + bodyTalonu + platneHlO
        var jednotky = 0

        if (zavazek.hlasiSto) {
            if (celkemF >= 100) {
                val nadSto = (celkemF - 100) / 10
                jednotky += (4 + nadSto) * cervene * nasobekFlekHra
            } else {
                jednotky -= 4 * cervene * nasobekFlekHra
            }
        } else {
            val vyhraHry = celkemF > celkemO
            if (vyhraHry && celkemF >= 100) {
                jednotky += (2 + (celkemF - 100) / 10) * cervene * nasobekFlekHra
            } else if (!vyhraHry && celkemO >= 100) {
                jednotky -= (2 + (celkemO - 100) / 10) * cervene * nasobekFlekHra
            } else {
                jednotky += (if (vyhraHry) 1 else -1) * cervene * nasobekFlekHra
            }
            if (vyhraHry && pocetStychuObrana == 0) {
                jednotky += 1 * cervene * nasobekFlekHra
            } else if (!vyhraHry && pocetStychuForhont == 0) {
                jednotky -= 1 * cervene * nasobekFlekHra
            }
        }

        if (zavazek.hlasiDveSedmy) {
            val uhralObe = stav9Sedma == 1 && stav10Sedma == 1
            jednotky += (if (uhralObe) 8 else -8) * cervene * nasobekFlekSedma
        } else if (zavazek.hlasiSedmu) {
            val uhral7 = stav10Sedma == 1
            jednotky += (if (uhral7) 4 else -4) * cervene * nasobekFlekSedma
        } else if (stav9Sedma == 1 && stav10Sedma == 1) {
            jednotky += 4 * cervene
        } else {
            when (stav10Sedma) {
                1 -> jednotky += 2 * cervene
                -1 -> jednotky -= 2 * cervene
                2 -> jednotky -= 2 * cervene
                -2 -> jednotky += 2 * cervene
            }
        }
        return jednotky
    }

    fun vyhodnotMariase(stav: StavMarias): VyuctovaniMariase {
        if (stav.slozenoBezHry) {
            return MariasVariantyEngine.vyhodnotSlozenouDrazbu(stav)
        }
        if (stav.rezimMariase == RezimMariase.LIZACI) {
            return MariasVariantyEngine.vyhodnotLizaciMariase(stav)
        }

        val fh = stav.indexForhonta
        val forhontJmeno = stav.jmenaHracu.getOrElse(fh) { "Forhont" }
        val cerveneNasobek = if (!stav.jeBetlNeboDurch && stav.trumf == BarvaKarty.CERVENE) 2 else 1
        val polozky = ArrayList<PolozkaVyuctovaniMariase>()

        var jednotkyProForhontaNaObrance = 0

        if (stav.zavazek == ZavazekMariase.BETL) {
            val uhral = stav.pocetStychuForhont == 0
            val sazba = 5 * stav.nasobekFlekHra
            jednotkyProForhontaNaObrance = if (uhral) sazba else -sazba
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Závazek Betl (${stav.nasobekFlekHra}×)",
                    hodnotaText = if (uhral) "Uhráno (0 štychů)" else "Padl (${sklonujStychyMariase(stav.pocetStychuForhont)})",
                    jeZvyraznena = true
                )
            )
        } else if (stav.zavazek == ZavazekMariase.DURCH) {
            val uhral = stav.pocetStychuObrana == 0
            val sazba = 10 * stav.nasobekFlekHra
            jednotkyProForhontaNaObrance = if (uhral) sazba else -sazba
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Závazek Durch (${stav.nasobekFlekHra}×)",
                    hodnotaText = if (uhral) "Uhráno (všech 10 štychů)" else "Padl (obrana vzala štych)",
                    jeZvyraznena = true
                )
            )
        } else {
            val bodyTalonu = stav.talon.sumOf { it.hodnota.bodyMariasSedma }
            val platneHlaskyF = if (stav.pocetStychuForhont > 0) stav.hlaskyForhont else 0
            val platneHlaskyO = if (stav.pocetStychuObrana > 0) stav.hlaskyObrana else 0
            val kartyF = stav.bodyKaretForhont
            val kartyO = stav.bodyKaretObrana + bodyTalonu
            val celkemF = kartyF + platneHlaskyF
            val celkemO = kartyO + platneHlaskyO

            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Forhont ($forhontJmeno)",
                    hodnotaText = "$celkemF b. (${kartyF}+hl. $platneHlaskyF • ${sklonujStychyMariase(stav.pocetStychuForhont)})",
                    jeZvyraznena = celkemF > celkemO
                )
            )
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Obrana",
                    hodnotaText = "$celkemO b. (${kartyO}+hl. $platneHlaskyO • ${sklonujStychyMariase(stav.pocetStychuObrana)})",
                    jeZvyraznena = celkemO > celkemF
                )
            )

            val trumfPopis = buildString {
                append(stav.trumf?.ceskyNazev ?: "—")
                if (cerveneNasobek == 2) append(" (2×)")
                if (stav.volenoZTalonu) append(" • z talonu")
            }
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Trumfy",
                    hodnotaText = trumfPopis,
                    jeZvyraznena = cerveneNasobek == 2
                )
            )

            if (stav.zavazek.hlasiSto) {
                val uhralSto = celkemF >= 100
                val nadSto = if (uhralSto) (celkemF - 100) / 10 else 0
                val sazbaSto = (4 + nadSto) * cerveneNasobek * stav.nasobekFlekHra
                jednotkyProForhontaNaObrance += if (uhralSto) sazbaSto else -(4 * cerveneNasobek * stav.nasobekFlekHra)
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Hlášená stovka (${stav.nasobekFlekHra}×)",
                        hodnotaText = if (uhralSto) "Uhráno ($celkemF b.)" else "Neuhral ($celkemF b.)",
                        jeZvyraznena = true
                    )
                )
            } else {
                val vyhraHry = celkemF > celkemO
                if (vyhraHry && celkemF >= 100) {
                    val nadSto = (celkemF - 100) / 10
                    val sazbaTichaSto = (2 + nadSto) * cerveneNasobek * stav.nasobekFlekHra
                    jednotkyProForhontaNaObrance += sazbaTichaSto
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Tichá stovka forhonta ($celkemF b.)",
                            hodnotaText = "+${sazbaTichaSto * 10} b. od každého",
                            jeZvyraznena = true
                        )
                    )
                } else if (!vyhraHry && celkemO >= 100) {
                    val nadSto = (celkemO - 100) / 10
                    val sazbaTichaStoObr = (2 + nadSto) * cerveneNasobek * stav.nasobekFlekHra
                    jednotkyProForhontaNaObrance -= sazbaTichaStoObr
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Tichá stovka obrany ($celkemO b.)",
                            hodnotaText = "-${sazbaTichaStoObr * 10} b. každému",
                            jeZvyraznena = true
                        )
                    )
                } else {
                    val sazbaHry = 1 * cerveneNasobek * stav.nasobekFlekHra
                    jednotkyProForhontaNaObrance += if (vyhraHry) sazbaHry else -sazbaHry
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Hra (${stav.nasobekFlekHra}×)",
                            hodnotaText = if (vyhraHry) "Uhráno na body" else "Obrana ubránila",
                            jeZvyraznena = false
                        )
                    )
                }

                if (vyhraHry && stav.pocetStychuObrana == 0) {
                    val kanar = 1 * cerveneNasobek * stav.nasobekFlekHra
                    jednotkyProForhontaNaObrance += kanar
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Kanár (obrana bez štychu)",
                            hodnotaText = "+${kanar * 10} b. od každého",
                            jeZvyraznena = true
                        )
                    )
                } else if (!vyhraHry && stav.pocetStychuForhont == 0) {
                    val kanar = 1 * cerveneNasobek * stav.nasobekFlekHra
                    jednotkyProForhontaNaObrance -= kanar
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Kanár (forhont bez štychu)",
                            hodnotaText = "-${kanar * 10} b. každému",
                            jeZvyraznena = true
                        )
                    )
                }
            }

            if (stav.zavazek.hlasiDveSedmy) {
                val uhralObe = stav.stavDevatehoStychuSedma == 1 && stav.stavPoslednihoStychuSedma == 1
                val zabita = stav.stavDevatehoStychuSedma == -1 || stav.stavPoslednihoStychuSedma == -1
                val sazba2x7 = 8 * cerveneNasobek * stav.nasobekFlekSedma
                jednotkyProForhontaNaObrance += if (uhralObe) sazba2x7 else -sazba2x7
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = if (zabita) "Zabitá hlášená sedma (2×7 • ${stav.nasobekFlekSedma}×)" else "Hlášené dvě sedmy (${stav.nasobekFlekSedma}×)",
                        hodnotaText = if (uhralObe) "Obě sedmy uhrány!" else "Neuhráno",
                        jeZvyraznena = true
                    )
                )
            } else if (stav.zavazek.hlasiSedmu) {
                val uhral7 = stav.stavPoslednihoStychuSedma == 1
                val zabita7 = stav.stavPoslednihoStychuSedma == -1
                val sazba7 = 4 * cerveneNasobek * stav.nasobekFlekSedma
                jednotkyProForhontaNaObrance += if (uhral7) sazba7 else -sazba7
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = if (zabita7) "Zabitá hlášená sedma (${stav.nasobekFlekSedma}×)" else "Hlášená sedma (${stav.nasobekFlekSedma}×)",
                        hodnotaText = if (uhral7) "Sedma doma!" else "Neuhráno",
                        jeZvyraznena = true
                    )
                )
            } else {
                if (stav.stavDevatehoStychuSedma == 1 && stav.stavPoslednihoStychuSedma == 1) {
                    val s2 = 4 * cerveneNasobek
                    jednotkyProForhontaNaObrance += s2
                    polozky.add(
                        PolozkaVyuctovaniMariase(
                            popis = "Tiché dvě sedmy forhonta",
                            hodnotaText = "+${s2 * 10} b. od každého",
                            jeZvyraznena = true
                        )
                    )
                } else {
                    when (stav.stavPoslednihoStychuSedma) {
                        1 -> {
                            val s7 = 2 * cerveneNasobek
                            jednotkyProForhontaNaObrance += s7
                            polozky.add(
                                PolozkaVyuctovaniMariase(
                                    popis = "Tichá sedma forhonta v 10. štychu",
                                    hodnotaText = "+${s7 * 10} b. od každého",
                                    jeZvyraznena = true
                                )
                            )
                        }
                        -1 -> {
                            val z7 = 2 * cerveneNasobek
                            jednotkyProForhontaNaObrance -= z7
                            polozky.add(
                                PolozkaVyuctovaniMariase(
                                    popis = "Zabitá tichá sedma forhonta",
                                    hodnotaText = "-${z7 * 10} b. každému",
                                    jeZvyraznena = true
                                )
                            )
                        }
                        2 -> {
                            val s7 = 2 * cerveneNasobek
                            jednotkyProForhontaNaObrance -= s7
                            polozky.add(
                                PolozkaVyuctovaniMariase(
                                    popis = "Tichá sedma obrany v 10. štychu",
                                    hodnotaText = "-${s7 * 10} b. každému",
                                    jeZvyraznena = true
                                )
                            )
                        }
                        -2 -> {
                            val z7 = 2 * cerveneNasobek
                            jednotkyProForhontaNaObrance += z7
                            polozky.add(
                                PolozkaVyuctovaniMariase(
                                    popis = "Forhont zabil tichou sedmu obrany",
                                    hodnotaText = "+${z7 * 10} b. od každého",
                                    jeZvyraznena = true
                                )
                            )
                        }
                    }
                }
            }
        }

        val naObranceTereziany = jednotkyProForhontaNaObrance * 10
        val ziskForhonta = naObranceTereziany * 2
        val ziskObrance = -naObranceTereziany

        polozky.add(
            PolozkaVyuctovaniMariase(
                popis = "Sazba na obránce",
                hodnotaText = "${abs(naObranceTereziany)} tereziánů (forhont ${if (ziskForhonta >= 0) "+" else ""}$ziskForhonta)",
                jeZvyraznena = true
            )
        )

        val nadpis = when {
            ziskForhonta > 0 -> "Forhont $forhontJmeno vyhrává!"
            ziskForhonta < 0 -> "Obrana porazila forhonta!"
            else -> "Smír u stolu!"
        }

        val hraci = List(3) { idx ->
            val jeFh = idx == fh
            val zmena = if (jeFh) ziskForhonta else ziskObrance
            HracVyuctovaniMariase(
                indexHrace = idx,
                jmenoHrace = stav.jmenaHracu.getOrElse(idx) { "Hráč ${idx + 1}" },
                jeVitez = zmena > 0,
                bodyZapasu = zmena,
                zmenaTerezianu = zmena
            )
        }

        return VyuctovaniMariase(
            typHryPlaceholder = Unit,
            nadpis = nadpis,
            podnadpis = "${stav.rezimMariase.plnyNazev} (${stav.zavazek.nazev}) • ${stav.variantaPravidel.kratkyNazev} pravidla",
            polozky = polozky,
            hraci = hraci
        )
    }

    private fun VyuctovaniMariase(
        typHryPlaceholder: Unit,
        nadpis: String,
        podnadpis: String,
        polozky: List<PolozkaVyuctovaniMariase>,
        hraci: List<HracVyuctovaniMariase>
    ): VyuctovaniMariase = VyuctovaniMariase(nadpis = nadpis, podnadpis = podnadpis, polozky = polozky, hraci = hraci)
}
