package cz.mares.ceskekarty

import kotlin.random.Random

data class StavSedma(
    val pocetHracu: Int,
    val jmenaHracu: List<String>,
    val ruceHracu: List<List<Karta>>,
    val lizaciBalicek: List<Karta>,
    val aktualniStych: List<HranaKartaStolu>,
    val vedouciHodnota: HodnotaKarty?,
    val prozatimBereHrac: Int,
    val naTahu: Int,
    val muzePustitStych: Boolean,
    val uhraneBodyTym0: Int,
    val uhraneBodyTym1: Int,
    val pocetStychuTym0: Int,
    val pocetStychuTym1: Int,
    val posledniStychTym: Int,
    val spalenaTym: Int,
    val posledniHlaska: String,
    val jeKonec: Boolean,
    val cisloTahu: Int,
    val variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA,
    val spalenaZRuky: Boolean = false,
    val posledniUzavrenyStych: List<HranaKartaStolu> = emptyList(),
    val posledniUzavrenyVitez: Int = -1,
    val odehraneKarty: List<Karta> = emptyList()
) {
    fun tymHrace(indexHrace: Int): Int = indexHrace % 2

    fun nazevTymu(tym: Int): String {
        return if (pocetHracu == 2) {
            jmenaHracu.getOrElse(tym) { "Hráč ${tym + 1}" }
        } else {
            if (tym == 0) "${jmenaHracu[0]} + ${jmenaHracu[2]}" else "${jmenaHracu[1]} + ${jmenaHracu[3]}"
        }
    }

    fun doKompaktnihoRetezce(): String {
        val pole = ArrayList<Pair<String, String>>()
        pole.add("ph" to pocetHracu.toString())
        pole.add("jm" to jmenaHracu.joinToString(";"))
        for (i in 0 until pocetHracu) {
            pole.add("r$i" to kartyNaRetezec(ruceHracu.getOrElse(i) { emptyList() }))
        }
        pole.add("liz" to kartyNaRetezec(lizaciBalicek))
        pole.add("st" to stychNaRetezec(aktualniStych))
        pole.add("vh" to (vedouciHodnota?.name ?: ""))
        pole.add("pbh" to prozatimBereHrac.toString())
        pole.add("nt" to naTahu.toString())
        pole.add("mps" to if (muzePustitStych) "1" else "0")
        pole.add("b0" to uhraneBodyTym0.toString())
        pole.add("b1" to uhraneBodyTym1.toString())
        pole.add("s0" to pocetStychuTym0.toString())
        pole.add("s1" to pocetStychuTym1.toString())
        pole.add("pst" to posledniStychTym.toString())
        pole.add("sp" to spalenaTym.toString())
        pole.add("hl" to posledniHlaska)
        pole.add("kon" to if (jeKonec) "1" else "0")
        pole.add("ct" to cisloTahu.toString())
        pole.add("vp" to variantaPravidel.kod)
        pole.add("szr" to if (spalenaZRuky) "1" else "0")
        pole.add("pus" to stychNaRetezec(posledniUzavrenyStych))
        pole.add("puv" to posledniUzavrenyVitez.toString())
        pole.add("ok" to kartyNaRetezec(odehraneKarty))
        return serializujMapuPoli(pole)
    }

    companion object {
        fun zKompaktnihoRetezce(data: String): StavSedma? {
            if (data.isBlank()) return null
            val r = PoleStavuReader(data)
            val pocet = r.cislo("ph", 0)
            if (pocet != 2 && pocet != 4) return null
            val jmena = r.text("jm").split(";").let { seznam ->
                List(pocet) { idx -> seznam.getOrNull(idx) ?: "Hráč ${idx + 1}" }
            }
            val ruce = List(pocet) { idx -> Karta.seradRukuSedma(retezecNaKarty(r.text("r$idx"))) }
            return StavSedma(
                pocetHracu = pocet,
                jmenaHracu = jmena,
                ruceHracu = ruce,
                lizaciBalicek = retezecNaKarty(r.text("liz")),
                aktualniStych = retezecNaStych(r.text("st")),
                vedouciHodnota = HodnotaKarty.dleNazvu(r.text("vh")),
                prozatimBereHrac = r.cislo("pbh", 0),
                naTahu = r.cislo("nt", 0),
                muzePustitStych = r.pravda("mps", false),
                uhraneBodyTym0 = r.cislo("b0", 0),
                uhraneBodyTym1 = r.cislo("b1", 0),
                pocetStychuTym0 = r.cislo("s0", 0),
                pocetStychuTym1 = r.cislo("s1", 0),
                posledniStychTym = r.cislo("pst", -1),
                spalenaTym = r.cislo("sp", -1),
                posledniHlaska = r.text("hl"),
                jeKonec = r.pravda("kon", false),
                cisloTahu = r.cislo("ct", 1),
                variantaPravidel = VariantaPravidel.dleKodu(r.text("vp", VariantaPravidel.HOSPODSKA.kod)),
                spalenaZRuky = r.pravda("szr", false),
                posledniUzavrenyStych = retezecNaStych(r.text("pus")),
                posledniUzavrenyVitez = r.cislo("puv", -1),
                odehraneKarty = retezecNaKarty(r.text("ok"))
            )
        }
    }
}

private sealed class AkceBotaSedma {
    data object PustitStych : AkceBotaSedma()
    data class ZahratKartu(val karta: Karta) : AkceBotaSedma()
}

private class CitacUzluSedma(var zbyva: Int)

private class SimStavSedma(
    val pocetHracu: Int,
    val variantaPravidel: VariantaPravidel,
    val ruce: List<List<Karta>>,
    val talon: List<Karta>,
    val talonIndex: Int,
    val stychHodnoty: List<HodnotaKarty>,
    val vynasejiciStychu: Int,
    val vedouciHodnota: HodnotaKarty?,
    val prozatimBereHrac: Int,
    val bodyAktualnihoStychu: Int,
    val naTahu: Int,
    val muzePustitStych: Boolean,
    val bodyTym0: Int,
    val bodyTym1: Int,
    val stychyTym0: Int,
    val stychyTym1: Int,
    val spalenaTym: Int,
    val jeKonec: Boolean
)

object SedmaEngine {

    private val VSECHNY_KARTY_BALICKU: List<Karta> = buildList(32) {
        for (barva in BarvaKarty.entries) {
            for (hodnota in HodnotaKarty.entries) {
                add(Karta(barva, hodnota))
            }
        }
    }

    private fun sklonujStychy(pocet: Int): String {
        return when {
            pocet == 1 -> "1 štych"
            pocet in 2..4 -> "$pocet štychy"
            else -> "$pocet štychů"
        }
    }

    fun novaHra(
        jmenaHracu: List<String>,
        predmichanyBalicek: List<Karta>? = null,
        zacinajiciHrac: Int = 0,
        variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA
    ): StavSedma {
        val pocet = if (jmenaHracu.size >= 4) 4 else 2
        val balicek = (predmichanyBalicek ?: Karta.vytvorZamichanyBalicek()).toMutableList()
        val ruce = List(pocet) {
            val ruka = ArrayList<Karta>(4)
            repeat(4) {
                if (balicek.isNotEmpty()) ruka.add(balicek.removeAt(0))
            }
            Karta.seradRukuSedma(ruka)
        }
        val startIndex = zacinajiciHrac.mod(pocet)
        val rezimText = variantaPravidel.kratkyNazev.lowercase()
        return StavSedma(
            pocetHracu = pocet,
            jmenaHracu = jmenaHracu.take(pocet),
            ruceHracu = ruce,
            lizaciBalicek = balicek,
            aktualniStych = emptyList(),
            vedouciHodnota = null,
            prozatimBereHrac = startIndex,
            naTahu = startIndex,
            muzePustitStych = false,
            uhraneBodyTym0 = 0,
            uhraneBodyTym1 = 0,
            pocetStychuTym0 = 0,
            pocetStychuTym1 = 0,
            posledniStychTym = -1,
            spalenaTym = -1,
            posledniHlaska = if (pocet == 4) {
                "Čtyřhra do kříže ($rezimText)! Vynáší ${jmenaHracu[startIndex]}."
            } else {
                "Sedma 1 na 1 ($rezimText)! Vynáší ${jmenaHracu[startIndex]}."
            },
            jeKonec = false,
            cisloTahu = 1,
            variantaPravidel = variantaPravidel,
            spalenaZRuky = false,
            posledniUzavrenyStych = emptyList(),
            posledniUzavrenyVitez = -1,
            odehraneKarty = emptyList()
        )
    }

    /**
     * V hospodských pravidlech lze hlásit Spálenou i při 4 kartách stejné hodnoty na ruce.
     */
    fun zjistiSpalenouVRuce(stav: StavSedma, indexHrace: Int): HodnotaKarty? {
        if (stav.jeKonec || stav.variantaPravidel != VariantaPravidel.HOSPODSKA) return null
        if (stav.naTahu != indexHrace && stav.aktualniStych.isNotEmpty()) return null
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return null
        if (ruka.size < 4) return null
        val prvniHodnota = ruka.first().hodnota
        return if (ruka.count { it.hodnota == prvniHodnota } == 4) prvniHodnota else null
    }

    fun nahlasSpalenouZRuky(stav: StavSedma, indexHrace: Int): StavSedma {
        val hodnotaCtyrky = zjistiSpalenouVRuce(stav, indexHrace) ?: return stav
        val ruka = stav.ruceHracu[indexHrace]
        val ctyriKarty = ruka.filter { it.hodnota == hodnotaCtyrky }
        val jmeno = stav.jmenaHracu[indexHrace]
        val vylozenyStych = ctyriKarty.map { HranaKartaStolu(indexHrace, jmeno, it) }
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r ->
            if (idx == indexHrace) Karta.seradRukuSedma(r.filterNot { it.hodnota == hodnotaCtyrky }) else r
        }
        val viteznyTym = stav.tymHrace(indexHrace)
        return stav.copy(
            ruceHracu = noveRuce,
            aktualniStych = vylozenyStych,
            vedouciHodnota = hodnotaCtyrky,
            prozatimBereHrac = indexHrace,
            naTahu = indexHrace,
            muzePustitStych = false,
            spalenaTym = viteznyTym,
            spalenaZRuky = true,
            posledniStychTym = viteznyTym,
            posledniUzavrenyStych = vylozenyStych,
            posledniUzavrenyVitez = indexHrace,
            posledniHlaska = "SPÁLENÁ Z RUKY! $jmeno vykládá čtveřici (${hodnotaCtyrky.ceskyNazev}) a okamžitě vyhrává!",
            jeKonec = true,
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun kartaPrebijiStych(stav: StavSedma, karta: Karta): Boolean {
        val vedouci = stav.vedouciHodnota ?: return true
        return karta.hodnota == vedouci || karta.hodnota == HodnotaKarty.R7
    }

    fun zjistiPovoleneKarty(stav: StavSedma, indexHrace: Int): Set<Karta> {
        if (stav.jeKonec || stav.naTahu != indexHrace) return emptySet()
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()
        if (stav.muzePustitStych) {
            val vedouci = stav.vedouciHodnota ?: return emptySet()
            return if (stav.prozatimBereHrac == indexHrace) {
                ruka.filter { it.hodnota == vedouci }.toSet()
            } else {
                ruka.filter { it.hodnota == vedouci || it.hodnota == HodnotaKarty.R7 }.toSet()
            }
        }
        return ruka.toSet()
    }

    fun zahrajKartu(stav: StavSedma, indexHrace: Int, karta: Karta): StavSedma {
        val povolene = zjistiPovoleneKarty(stav, indexHrace)
        if (karta !in povolene) return stav

        val jmeno = stav.jmenaHracu[indexHrace]
        val novaRuka = Karta.seradRukuSedma(stav.ruceHracu[indexHrace].filterNot { it == karta })
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }

        val vedouci = stav.vedouciHodnota ?: karta.hodnota
        val prebiji = stav.aktualniStych.isEmpty() ||
            karta.hodnota == vedouci ||
            karta.hodnota == HodnotaKarty.R7
        val predchoziTymDrzitele = if (stav.aktualniStych.isEmpty()) -1 else stav.tymHrace(stav.prozatimBereHrac)
        val novyDrzitel = if (prebiji) indexHrace else stav.prozatimBereHrac
        val novyStych = stav.aktualniStych + HranaKartaStolu(indexHrace, jmeno, karta)

        if (novyStych.size >= 4) {
            val posledniCtyri = novyStych.takeLast(4)
            val prvniHod = posledniCtyri.first().karta.hodnota
            if ((prvniHod == vedouci || prvniHod == HodnotaKarty.R7) &&
                posledniCtyri.all { it.karta.hodnota == prvniHod }
            ) {
                val viteznyTym = stav.tymHrace(novyDrzitel)
                return stav.copy(
                    ruceHracu = noveRuce,
                    aktualniStych = novyStych,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    muzePustitStych = false,
                    spalenaTym = viteznyTym,
                    spalenaZRuky = false,
                    posledniStychTym = viteznyTym,
                    posledniUzavrenyStych = novyStych,
                    posledniUzavrenyVitez = novyDrzitel,
                    posledniHlaska = "SPÁLENÁ! $jmeno zkompletoval čtveřici (${karta.hodnota.ceskyNazev}) a okamžitě vyhrává!",
                    jeKonec = true,
                    cisloTahu = stav.cisloTahu + 1
                )
            }
        }

        val dalsiHrac = (indexHrace + 1) % stav.pocetHracu

        if (stav.pocetHracu == 4) {
            if (novyStych.size < 4) {
                val popisAkce = when {
                    novyStych.size == 1 -> "vynáší"
                    prebiji && stav.tymHrace(indexHrace) != predchoziTymDrzitele ->
                        "přebíjí (${vedouci.ceskyNazev}) kartou"
                    prebiji ->
                        "přebírá vedení týmu kartou"
                    else ->
                        "přikládá"
                }
                return stav.copy(
                    ruceHracu = noveRuce,
                    aktualniStych = novyStych,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    naTahu = dalsiHrac,
                    muzePustitStych = false,
                    posledniUzavrenyStych = emptyList(),
                    posledniUzavrenyVitez = -1,
                    posledniHlaska = "$jmeno $popisAkce ${karta.celyNazev()}.",
                    cisloTahu = stav.cisloTahu + 1
                )
            } else {
                return uzavriStychADober(
                    stav.copy(
                        ruceHracu = noveRuce,
                        aktualniStych = novyStych,
                        vedouciHodnota = vedouci,
                        prozatimBereHrac = novyDrzitel,
                        muzePustitStych = false
                    )
                )
            }
        }

        if (novyStych.size % 2 == 1) {
            val sloveso = when {
                novyStych.size == 1 -> "vynáší"
                stav.prozatimBereHrac == indexHrace -> "natahuje štych kartou"
                else -> "přebíjí zpět kartou"
            }
            return stav.copy(
                ruceHracu = noveRuce,
                aktualniStych = novyStych,
                vedouciHodnota = vedouci,
                prozatimBereHrac = novyDrzitel,
                naTahu = dalsiHrac,
                muzePustitStych = false,
                posledniUzavrenyStych = emptyList(),
                posledniUzavrenyVitez = -1,
                posledniHlaska = "$jmeno $sloveso ${karta.celyNazev()}.",
                cisloTahu = stav.cisloTahu + 1
            )
        } else {
            val vynasejici = novyStych.first().indexHrace
            val odpovidajici = (vynasejici + 1) % 2
            val souperPrebil = novyDrzitel != vynasejici
            val rukaVynasejiciho = noveRuce[vynasejici]
            val souperMaKarty = noveRuce[odpovidajici].isNotEmpty()

            val maCimPrebitZpet = souperMaKarty && rukaVynasejiciho.any {
                it.hodnota == vedouci || it.hodnota == HodnotaKarty.R7
            }
            val maDalsiStejnouProToceni = souperMaKarty && rukaVynasejiciho.any {
                it.hodnota == vedouci
            }

            return if (souperPrebil && maCimPrebitZpet) {
                stav.copy(
                    ruceHracu = noveRuce,
                    aktualniStych = novyStych,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    naTahu = vynasejici,
                    muzePustitStych = true,
                    posledniUzavrenyStych = emptyList(),
                    posledniUzavrenyVitez = -1,
                    posledniHlaska = "$jmeno přebil kartou ${karta.celyNazev()} — ${stav.jmenaHracu[vynasejici]} může přebít zpět nebo štych pustit!",
                    cisloTahu = stav.cisloTahu + 1
                )
            } else if (!souperPrebil && stav.variantaPravidel == VariantaPravidel.HOSPODSKA && maDalsiStejnouProToceni) {
                stav.copy(
                    ruceHracu = noveRuce,
                    aktualniStych = novyStych,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    naTahu = vynasejici,
                    muzePustitStych = true,
                    posledniUzavrenyStych = emptyList(),
                    posledniUzavrenyVitez = -1,
                    posledniHlaska = "$jmeno odhodil ${karta.celyNazev()} — ${stav.jmenaHracu[vynasejici]} může točit další ${vedouci.ceskyNazev}, nebo štych vzít!",
                    cisloTahu = stav.cisloTahu + 1
                )
            } else {
                uzavriStychADober(
                    stav.copy(
                        ruceHracu = noveRuce,
                        aktualniStych = novyStych,
                        vedouciHodnota = vedouci,
                        prozatimBereHrac = novyDrzitel,
                        muzePustitStych = false
                    )
                )
            }
        }
    }

    fun pustStych(stav: StavSedma, indexHrace: Int): StavSedma {
        if (stav.jeKonec || stav.naTahu != indexHrace || !stav.muzePustitStych) return stav
        return uzavriStychADober(stav.copy(muzePustitStych = false))
    }

    private fun uzavriStychADober(stav: StavSedma): StavSedma {
        val vitezStychu = stav.prozatimBereHrac
        val viteznyTym = stav.tymHrace(vitezStychu)
        val ulozenyStych = stav.aktualniStych
        val bodyVeStychu = ulozenyStych.sumOf { it.karta.hodnota.bodyMariasSedma }
        val noveOdehrane = stav.odehraneKarty + ulozenyStych.map { it.karta }

        val lizaci = stav.lizaciBalicek.toMutableList()
        val ruceMut = stav.ruceHracu.map { it.toMutableList() }

        while (lizaci.size >= stav.pocetHracu && ruceMut[vitezStychu].size < 4) {
            for (posun in 0 until stav.pocetHracu) {
                val idx = (vitezStychu + posun) % stav.pocetHracu
                ruceMut[idx].add(lizaci.removeAt(0))
            }
        }

        val serazeneRuce = ruceMut.map { Karta.seradRukuSedma(it) }
        val vsechnyPrazdne = serazeneRuce.all { it.isEmpty() }
        val bonusPosledniho = if (vsechnyPrazdne) 10 else 0

        val noveBody0 = stav.uhraneBodyTym0 + if (viteznyTym == 0) bodyVeStychu + bonusPosledniho else 0
        val noveBody1 = stav.uhraneBodyTym1 + if (viteznyTym == 1) bodyVeStychu + bonusPosledniho else 0
        val noveStychy0 = stav.pocetStychuTym0 + if (viteznyTym == 0) 1 else 0
        val noveStychy1 = stav.pocetStychuTym1 + if (viteznyTym == 1) 1 else 0

        val jmenoVitezeStychu = stav.jmenaHracu[vitezStychu]
        val hlaska = if (vsechnyPrazdne) {
            "$jmenoVitezeStychu bere poslední štych (+10 b.)! Konec rozdání."
        } else {
            "$jmenoVitezeStychu bere štych (${bodyVeStychu} b.) a vynáší."
        }

        return stav.copy(
            ruceHracu = serazeneRuce,
            lizaciBalicek = lizaci,
            aktualniStych = emptyList(),
            vedouciHodnota = null,
            prozatimBereHrac = vitezStychu,
            naTahu = vitezStychu,
            muzePustitStych = false,
            uhraneBodyTym0 = noveBody0,
            uhraneBodyTym1 = noveBody1,
            pocetStychuTym0 = noveStychy0,
            pocetStychuTym1 = noveStychy1,
            posledniStychTym = if (vsechnyPrazdne) viteznyTym else stav.posledniStychTym,
            posledniUzavrenyStych = ulozenyStych,
            posledniUzavrenyVitez = vitezStychu,
            posledniHlaska = hlaska,
            jeKonec = vsechnyPrazdne,
            cisloTahu = stav.cisloTahu + 1,
            odehraneKarty = noveOdehrane
        )
    }

    private fun vyberUnikatniPodleHodnoty(karty: Collection<Karta>): List<Karta> {
        if (karty.size <= 1) return karty.toList()
        var maska = 0
        val vysledek = ArrayList<Karta>(karty.size)
        for (k in karty) {
            val bit = 1 shl k.hodnota.ordinal
            if ((maska and bit) == 0) {
                maska = maska or bit
                vysledek.add(k)
            }
        }
        return vysledek
    }

    private fun zkompletujeSpalenouSedmou(stychHodnoty: List<HodnotaKarty>): Boolean {
        val n = stychHodnoty.size
        return n >= 3 &&
            stychHodnoty[n - 1] == HodnotaKarty.R7 &&
            stychHodnoty[n - 2] == HodnotaKarty.R7 &&
            stychHodnoty[n - 3] == HodnotaKarty.R7
    }

    /**
     * Hospodská disciplína: dokud jsou v talonu karty a nejde o zkompletování Spálené,
     * sedmička se nikdy nevyhazuje na nulovou plívu (0 bodů), pokud má hráč v ruce plívu na odhoz!
     */
    private fun smiZahratSedmuNaStul(
        talonZbylo: Int,
        stychHodnoty: List<HodnotaKarty>,
        bodyVeStychu: Int,
        muzePustitStych: Boolean,
        prozatimBereHrac: Int,
        hrac: Int,
        ruka: List<Karta>
    ): Boolean {
        if (talonZbylo <= 0) return true
        if (zkompletujeSpalenouSedmou(stychHodnoty)) return true

        // 1) Výnos do prázdného štychu: nikdy nevynášej sedmičku, pokud máš jakoukoliv jinou kartu
        if (stychHodnoty.isEmpty()) {
            return ruka.all { it.hodnota == HodnotaKarty.R7 }
        }

        // 2) Rozhodnutí o přebití po soupeřově tahu v 1 na 1 (kdy lze štych bezplatně pustit)
        if (muzePustitStych) {
            return bodyVeStychu >= 10
        }

        // 3) Ve čtyřhře štych už drží náš parťák -> neplýtvej sedmou na vlastní štych
        if ((prozatimBereHrac % 2) == (hrac % 2)) {
            return ruka.all { it.hodnota == HodnotaKarty.R7 }
        }

        // 4) Soupeř drží bodovaný štych (>= 10 b.) -> sedmička je plně povolena k propočtu
        if (bodyVeStychu >= 10) return true

        // 5) Soupeř drží nulový štych (plívu) -> sedmičku smíš použít jen v nouzi, když nemáš žádnou plívu
        // a jinak bys musel soupeři namazat vlastní desítku nebo eso!
        val maPlivuNaOdhoz = ruka.any { it.hodnota != HodnotaKarty.R7 && it.hodnota.bodyMariasSedma == 0 }
        return !maPlivuNaOdhoz
    }

    private fun maSpalenouZRukySim(sim: SimStavSedma, indexHrace: Int): Boolean {
        if (sim.jeKonec || sim.variantaPravidel != VariantaPravidel.HOSPODSKA) return false
        if (sim.naTahu != indexHrace && sim.stychHodnoty.isNotEmpty()) return false
        val ruka = sim.ruce[indexHrace]
        if (ruka.size < 4) return false
        val h0 = ruka[0].hodnota
        return ruka[1].hodnota == h0 && ruka[2].hodnota == h0 && ruka[3].hodnota == h0
    }

    private fun simUzavriStych(sim: SimStavSedma): SimStavSedma {
        val vitez = sim.prozatimBereHrac
        val viteznyTym = vitez % 2
        val bodyStychu = sim.bodyAktualnihoStychu

        var tIdx = sim.talonIndex
        val noveRuce: List<List<Karta>> = if (sim.talon.size - tIdx >= sim.pocetHracu && sim.ruce[vitez].size < 4) {
            val mut = Array(sim.pocetHracu) { p -> ArrayList<Karta>(4).apply { addAll(sim.ruce[p]) } }
            while (sim.talon.size - tIdx >= sim.pocetHracu && mut[vitez].size < 4) {
                for (posun in 0 until sim.pocetHracu) {
                    val p = (vitez + posun) % sim.pocetHracu
                    mut[p].add(sim.talon[tIdx++])
                }
            }
            mut.toList()
        } else {
            sim.ruce
        }

        var vsechnyPrazdne = true
        for (r in noveRuce) {
            if (r.isNotEmpty()) {
                vsechnyPrazdne = false
                break
            }
        }
        val bonusPosledni = if (vsechnyPrazdne) 10 else 0
        val noveB0 = sim.bodyTym0 + if (viteznyTym == 0) bodyStychu + bonusPosledni else 0
        val noveB1 = sim.bodyTym1 + if (viteznyTym == 1) bodyStychu + bonusPosledni else 0
        val noveS0 = sim.stychyTym0 + if (viteznyTym == 0) 1 else 0
        val noveS1 = sim.stychyTym1 + if (viteznyTym == 1) 1 else 0

        return SimStavSedma(
            pocetHracu = sim.pocetHracu,
            variantaPravidel = sim.variantaPravidel,
            ruce = noveRuce,
            talon = sim.talon,
            talonIndex = tIdx,
            stychHodnoty = emptyList(),
            vynasejiciStychu = vitez,
            vedouciHodnota = null,
            prozatimBereHrac = vitez,
            bodyAktualnihoStychu = 0,
            naTahu = vitez,
            muzePustitStych = false,
            bodyTym0 = noveB0,
            bodyTym1 = noveB1,
            stychyTym0 = noveS0,
            stychyTym1 = noveS1,
            spalenaTym = -1,
            jeKonec = vsechnyPrazdne
        )
    }

    private fun simZahrajKartu(sim: SimStavSedma, karta: Karta): SimStavSedma {
        val hrac = sim.naTahu
        val puvodniRuka = sim.ruce[hrac]
        val novaRuka = ArrayList<Karta>(puvodniRuka.size - 1)
        var odebrano = false
        for (k in puvodniRuka) {
            if (!odebrano && k == karta) {
                odebrano = true
            } else {
                novaRuka.add(k)
            }
        }
        val noveRuce = sim.ruce.mapIndexed { i, r -> if (i == hrac) novaRuka else r }

        val jePrvniVeStychu = sim.stychHodnoty.isEmpty()
        val vynasejici = if (jePrvniVeStychu) hrac else sim.vynasejiciStychu
        val vedouci = sim.vedouciHodnota ?: karta.hodnota
        val prebiji = jePrvniVeStychu || karta.hodnota == vedouci || karta.hodnota == HodnotaKarty.R7
        val novyDrzitel = if (prebiji) hrac else sim.prozatimBereHrac
        val novyStychHodnoty = sim.stychHodnoty + karta.hodnota
        val noveBodyStychu = sim.bodyAktualnihoStychu + karta.hodnota.bodyMariasSedma

        val n = novyStychHodnoty.size
        if (n >= 4) {
            val hPosl = novyStychHodnoty[n - 1]
            if ((hPosl == vedouci || hPosl == HodnotaKarty.R7) &&
                novyStychHodnoty[n - 2] == hPosl &&
                novyStychHodnoty[n - 3] == hPosl &&
                novyStychHodnoty[n - 4] == hPosl
            ) {
                return SimStavSedma(
                    pocetHracu = sim.pocetHracu,
                    variantaPravidel = sim.variantaPravidel,
                    ruce = noveRuce,
                    talon = sim.talon,
                    talonIndex = sim.talonIndex,
                    stychHodnoty = novyStychHodnoty,
                    vynasejiciStychu = vynasejici,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    bodyAktualnihoStychu = noveBodyStychu,
                    naTahu = novyDrzitel,
                    muzePustitStych = false,
                    bodyTym0 = sim.bodyTym0,
                    bodyTym1 = sim.bodyTym1,
                    stychyTym0 = sim.stychyTym0,
                    stychyTym1 = sim.stychyTym1,
                    spalenaTym = novyDrzitel % 2,
                    jeKonec = true
                )
            }
        }

        val dalsi = (hrac + 1) % sim.pocetHracu
        if (sim.pocetHracu == 4) {
            val meziprechod = SimStavSedma(
                pocetHracu = 4,
                variantaPravidel = sim.variantaPravidel,
                ruce = noveRuce,
                talon = sim.talon,
                talonIndex = sim.talonIndex,
                stychHodnoty = novyStychHodnoty,
                vynasejiciStychu = vynasejici,
                vedouciHodnota = vedouci,
                prozatimBereHrac = novyDrzitel,
                bodyAktualnihoStychu = noveBodyStychu,
                naTahu = dalsi,
                muzePustitStych = false,
                bodyTym0 = sim.bodyTym0,
                bodyTym1 = sim.bodyTym1,
                stychyTym0 = sim.stychyTym0,
                stychyTym1 = sim.stychyTym1,
                spalenaTym = -1,
                jeKonec = false
            )
            return if (novyStychHodnoty.size < 4) meziprechod else simUzavriStych(meziprechod)
        }

        if (novyStychHodnoty.size % 2 == 1) {
            return SimStavSedma(
                pocetHracu = 2,
                variantaPravidel = sim.variantaPravidel,
                ruce = noveRuce,
                talon = sim.talon,
                talonIndex = sim.talonIndex,
                stychHodnoty = novyStychHodnoty,
                vynasejiciStychu = vynasejici,
                vedouciHodnota = vedouci,
                prozatimBereHrac = novyDrzitel,
                bodyAktualnihoStychu = noveBodyStychu,
                naTahu = dalsi,
                muzePustitStych = false,
                bodyTym0 = sim.bodyTym0,
                bodyTym1 = sim.bodyTym1,
                stychyTym0 = sim.stychyTym0,
                stychyTym1 = sim.stychyTym1,
                spalenaTym = -1,
                jeKonec = false
            )
        }

        val odpovidajici = (vynasejici + 1) % 2
        val souperPrebil = novyDrzitel != vynasejici
        val rukaVyn = noveRuce[vynasejici]
        val souperMaKarty = noveRuce[odpovidajici].isNotEmpty()
        val maCimPrebitZpet = souperMaKarty && rukaVyn.any {
            it.hodnota == vedouci || it.hodnota == HodnotaKarty.R7
        }
        val maDalsiStejnou = souperMaKarty && rukaVyn.any { it.hodnota == vedouci }

        val zakladni = SimStavSedma(
            pocetHracu = 2,
            variantaPravidel = sim.variantaPravidel,
            ruce = noveRuce,
            talon = sim.talon,
            talonIndex = sim.talonIndex,
            stychHodnoty = novyStychHodnoty,
            vynasejiciStychu = vynasejici,
            vedouciHodnota = vedouci,
            prozatimBereHrac = novyDrzitel,
            bodyAktualnihoStychu = noveBodyStychu,
            naTahu = vynasejici,
            muzePustitStych = true,
            bodyTym0 = sim.bodyTym0,
            bodyTym1 = sim.bodyTym1,
            stychyTym0 = sim.stychyTym0,
            stychyTym1 = sim.stychyTym1,
            spalenaTym = -1,
            jeKonec = false
        )

        return if (souperPrebil && maCimPrebitZpet) {
            zakladni
        } else if (!souperPrebil && sim.variantaPravidel == VariantaPravidel.HOSPODSKA && maDalsiStejnou) {
            zakladni
        } else {
            simUzavriStych(zakladni)
        }
    }

    private fun ohodnotKoncovySim(sim: SimStavSedma, mujTym: Int): Int {
        if (sim.spalenaTym >= 0) {
            return if (sim.spalenaTym == mujTym) 4000 else -4000
        }
        val mojeBody = if (mujTym == 0) sim.bodyTym0 else sim.bodyTym1
        val souperBody = if (mujTym == 0) sim.bodyTym1 else sim.bodyTym0
        val mojeStychy = if (mujTym == 0) sim.stychyTym0 else sim.stychyTym1
        val souperStychy = if (mujTym == 0) sim.stychyTym1 else sim.stychyTym0

        return if (mojeBody > souperBody) {
            val zaklad = when {
                souperStychy == 0 -> 3000
                mojeBody >= 90 -> 2000
                else -> 1000
            }
            zaklad + (mojeBody - souperBody) * 4
        } else {
            val zaklad = when {
                mojeStychy == 0 -> -3000
                souperBody >= 90 -> -2000
                else -> -1000
            }
            zaklad + (mojeBody - souperBody) * 4
        }
    }

    private fun ohodnotRozehranySim(sim: SimStavSedma, mujTym: Int): Int {
        if (sim.jeKonec) return ohodnotKoncovySim(sim, mujTym)

        val mojeBody = if (mujTym == 0) sim.bodyTym0 else sim.bodyTym1
        val souperBody = if (mujTym == 0) sim.bodyTym1 else sim.bodyTym0
        val mojeStychy = if (mujTym == 0) sim.stychyTym0 else sim.stychyTym1
        val souperStychy = if (mujTym == 0) sim.stychyTym1 else sim.stychyTym0
        val zbyvaTalon = sim.talon.size - sim.talonIndex

        var skore = (mojeBody - souperBody) * 18
        if (mojeBody >= 50) skore += 240
        if (souperBody >= 50) skore -= 240

        // Holátko řešíme až v závěru hry, nikdy ne po prvních 2 štyších!
        if (zbyvaTalon <= 4) {
            if (mojeStychy == 0 && souperStychy >= 5) skore -= 220
            if (souperStychy == 0 && mojeStychy >= 5) skore += 220
        }

        if (sim.stychHodnoty.isEmpty()) {
            skore += if (sim.naTahu % 2 == mujTym) 14 else -14
        } else {
            val vPotu = sim.bodyAktualnihoStychu * 12 + 6
            skore += if (sim.prozatimBereHrac % 2 == mujTym) vPotu else -vPotu
        }

        val poctyHodnotRuky = IntArray(HodnotaKarty.entries.size)
        for (p in 0 until sim.pocetHracu) {
            val znamenko = if (p % 2 == mujTym) 1 else -1
            val ruka = sim.ruce[p]
            if (ruka.isEmpty()) continue
            poctyHodnotRuky.fill(0)
            for (k in ruka) {
                poctyHodnotRuky[k.hodnota.ordinal]++
            }
            val sedmy = poctyHodnotRuky[HodnotaKarty.R7.ordinal]
            // Vysoká hodnota sedmičky v ruce zabraňuje jejímu plýtvání na bezcenné štychy
            var hodnotaRuky = sedmy * 165 + if (sedmy > 0) 20 else 0

            for (hod in HodnotaKarty.entries) {
                val pocet = poctyHodnotRuky[hod.ordinal]
                if (pocet == 0) continue
                if (pocet == 4 && sim.variantaPravidel == VariantaPravidel.HOSPODSKA) {
                    hodnotaRuky += if (sim.talonIndex == 0) 3200 else 420
                } else if (hod != HodnotaKarty.R7) {
                    if (pocet == 3) hodnotaRuky += 48
                    if (hod.bodyMariasSedma >= 10) {
                        hodnotaRuky += if (pocet >= 2) {
                            42
                        } else if (sedmy > 0) {
                            14
                        } else {
                            -16
                        }
                    } else if (pocet == 2) {
                        hodnotaRuky += 8
                    }
                }
            }
            skore += znamenko * hodnotaRuky
        }
        return skore
    }

    private fun generujAkceSim(sim: SimStavSedma): List<AkceBotaSedma> {
        val hrac = sim.naTahu
        val ruka = sim.ruce[hrac]
        if (ruka.isEmpty()) return emptyList()

        val talonZbylo = sim.talon.size - sim.talonIndex
        val sedmaPovolena = smiZahratSedmuNaStul(
            talonZbylo = talonZbylo,
            stychHodnoty = sim.stychHodnoty,
            bodyVeStychu = sim.bodyAktualnihoStychu,
            muzePustitStych = sim.muzePustitStych,
            prozatimBereHrac = sim.prozatimBereHrac,
            hrac = hrac,
            ruka = ruka
        )

        if (sim.muzePustitStych) {
            val vedouci = sim.vedouciHodnota ?: return listOf(AkceBotaSedma.PustitStych)
            val karty = if (sim.prozatimBereHrac == hrac) {
                vyberUnikatniPodleHodnoty(ruka.filter { it.hodnota == vedouci })
            } else {
                vyberUnikatniPodleHodnoty(
                    ruka.filter {
                        it.hodnota == vedouci || (sedmaPovolena && it.hodnota == HodnotaKarty.R7)
                    }
                )
            }
            val akce = ArrayList<AkceBotaSedma>(karty.size + 1)
            if (sim.prozatimBereHrac == hrac && sim.bodyAktualnihoStychu >= 10) {
                akce.add(AkceBotaSedma.PustitStych)
                for (k in karty) akce.add(AkceBotaSedma.ZahratKartu(k))
            } else {
                for (k in karty) akce.add(AkceBotaSedma.ZahratKartu(k))
                akce.add(AkceBotaSedma.PustitStych)
            }
            return akce
        }

        val filtrovanaRuka = if (sedmaPovolena) {
            ruka
        } else {
            ruka.filterNot { it.hodnota == HodnotaKarty.R7 }.ifEmpty { ruka }
        }
        val unikatni = vyberUnikatniPodleHodnoty(filtrovanaRuka)
        if (unikatni.size <= 1) {
            return unikatni.map { AkceBotaSedma.ZahratKartu(it) }
        }

        val vedouci = sim.vedouciHodnota
        val serazene = unikatni.sortedByDescending { k ->
            when {
                vedouci != null && k.hodnota == vedouci -> 50 + k.hodnota.bodyMariasSedma
                vedouci != null && k.hodnota == HodnotaKarty.R7 && sim.bodyAktualnihoStychu >= 10 -> 45
                vedouci == null && k.hodnota != HodnotaKarty.R7 && k.hodnota.bodyMariasSedma == 0 -> 30
                k.hodnota == HodnotaKarty.R7 -> 5
                else -> 20
            }
        }
        return serazene.map { AkceBotaSedma.ZahratKartu(it) }
    }

    private fun alphaBetaSim(
        sim: SimStavSedma,
        zbyvaStychu: Int,
        alfaPuv: Int,
        betaPuv: Int,
        mujTym: Int,
        citac: CitacUzluSedma
    ): Int {
        if (sim.jeKonec) {
            return ohodnotKoncovySim(sim, mujTym)
        }
        if (maSpalenouZRukySim(sim, sim.naTahu)) {
            return if (sim.naTahu % 2 == mujTym) 4000 else -4000
        }
        citac.zbyva--
        if ((zbyvaStychu <= 0 && sim.stychHodnoty.isEmpty()) || citac.zbyva <= 0) {
            return ohodnotRozehranySim(sim, mujTym)
        }

        val akceSeznam = generujAkceSim(sim)
        if (akceSeznam.isEmpty()) {
            return ohodnotRozehranySim(sim, mujTym)
        }

        val maximalizuje = (sim.naTahu % 2) == mujTym
        var alfa = alfaPuv
        var beta = betaPuv

        if (maximalizuje) {
            var nej = Int.MIN_VALUE / 2
            for (akce in akceSeznam) {
                val dalsiSim = when (akce) {
                    is AkceBotaSedma.PustitStych -> simUzavriStych(sim)
                    is AkceBotaSedma.ZahratKartu -> simZahrajKartu(sim, akce.karta)
                }
                val dokoncilStych = dalsiSim.stychHodnoty.isEmpty() && !dalsiSim.jeKonec
                val dalsiHloubka = if (dokoncilStych) zbyvaStychu - 1 else zbyvaStychu
                val hodnota = alphaBetaSim(dalsiSim, dalsiHloubka, alfa, beta, mujTym, citac)
                if (hodnota > nej) nej = hodnota
                if (nej > alfa) alfa = nej
                if (alfa >= beta) break
            }
            return nej
        } else {
            var nej = Int.MAX_VALUE / 2
            for (akce in akceSeznam) {
                val dalsiSim = when (akce) {
                    is AkceBotaSedma.PustitStych -> simUzavriStych(sim)
                    is AkceBotaSedma.ZahratKartu -> simZahrajKartu(sim, akce.karta)
                }
                val dokoncilStych = dalsiSim.stychHodnoty.isEmpty() && !dalsiSim.jeKonec
                val dalsiHloubka = if (dokoncilStych) zbyvaStychu - 1 else zbyvaStychu
                val hodnota = alphaBetaSim(dalsiSim, dalsiHloubka, alfa, beta, mujTym, citac)
                if (hodnota < nej) nej = hodnota
                if (nej < beta) beta = nej
                if (alfa >= beta) break
            }
            return nej
        }
    }

    private fun sestavSimStavProSvet(
        stav: StavSedma,
        idxBota: Int,
        mojeRuka: List<Karta>,
        poctyKaretHracu: IntArray,
        zamichaneNevidene: List<Karta>
    ): SimStavSedma {
        var pos = 0
        val ruce = List(stav.pocetHracu) { p ->
            if (p == idxBota) {
                mojeRuka
            } else {
                val pocet = poctyKaretHracu[p]
                val vyrez = ArrayList<Karta>(pocet)
                repeat(pocet) {
                    if (pos < zamichaneNevidene.size) {
                        vyrez.add(zamichaneNevidene[pos++])
                    }
                }
                vyrez
            }
        }
        val talon = if (pos < zamichaneNevidene.size) {
            zamichaneNevidene.subList(pos, zamichaneNevidene.size)
        } else {
            emptyList()
        }

        return SimStavSedma(
            pocetHracu = stav.pocetHracu,
            variantaPravidel = stav.variantaPravidel,
            ruce = ruce,
            talon = talon,
            talonIndex = 0,
            stychHodnoty = stav.aktualniStych.map { it.karta.hodnota },
            vynasejiciStychu = stav.aktualniStych.firstOrNull()?.indexHrace ?: stav.naTahu,
            vedouciHodnota = stav.vedouciHodnota,
            prozatimBereHrac = stav.prozatimBereHrac,
            bodyAktualnihoStychu = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma },
            naTahu = stav.naTahu,
            muzePustitStych = stav.muzePustitStych,
            bodyTym0 = stav.uhraneBodyTym0,
            bodyTym1 = stav.uhraneBodyTym1,
            stychyTym0 = stav.pocetStychuTym0,
            stychyTym1 = stav.pocetStychuTym1,
            spalenaTym = stav.spalenaTym,
            jeKonec = stav.jeKonec
        )
    }

    fun provedTahBota(stav: StavSedma): StavSedma {
        if (stav.jeKonec) return stav
        val idx = stav.naTahu

        if (zjistiSpalenouVRuce(stav, idx) != null) {
            return nahlasSpalenouZRuky(stav, idx)
        }

        val mojeRuka = stav.ruceHracu[idx]
        if (mojeRuka.isEmpty()) return stav

        val velikostTalonu = stav.lizaciBalicek.size
        val stychHodnotyRoot = stav.aktualniStych.map { it.karta.hodnota }
        val bodyVeStychuRoot = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma }
        val sedmaPovolenaRoot = smiZahratSedmuNaStul(
            talonZbylo = velikostTalonu,
            stychHodnoty = stychHodnotyRoot,
            bodyVeStychu = bodyVeStychuRoot,
            muzePustitStych = stav.muzePustitStych,
            prozatimBereHrac = stav.prozatimBereHrac,
            hrac = idx,
            ruka = mojeRuka
        )

        val povoleneSurove = zjistiPovoleneKarty(stav, idx)
        val povoleneFiltrovane = if (sedmaPovolenaRoot) {
            povoleneSurove
        } else {
            val bezSedmy = povoleneSurove.filterNot { it.hodnota == HodnotaKarty.R7 }.toSet()
            if (bezSedmy.isNotEmpty() || stav.muzePustitStych) bezSedmy else povoleneSurove
        }

        val unikatniKarty = vyberUnikatniPodleHodnoty(povoleneFiltrovane)
        val korenoveAkce = ArrayList<AkceBotaSedma>(unikatniKarty.size + 1)
        for (k in unikatniKarty) {
            korenoveAkce.add(AkceBotaSedma.ZahratKartu(k))
        }
        if (stav.muzePustitStych) {
            korenoveAkce.add(AkceBotaSedma.PustitStych)
        }

        if (korenoveAkce.isEmpty()) return stav
        if (korenoveAkce.size == 1) {
            return when (val jedina = korenoveAkce.first()) {
                is AkceBotaSedma.PustitStych -> pustStych(stav, idx)
                is AkceBotaSedma.ZahratKartu -> zahrajKartu(stav, idx, jedina.karta)
            }
        }

        val znameKarty = HashSet<Karta>(32)
        znameKarty.addAll(mojeRuka)
        znameKarty.addAll(stav.odehraneKarty)
        for (hk in stav.aktualniStych) {
            znameKarty.add(hk.karta)
        }
        val nevideneKarty = VSECHNY_KARTY_BALICKU.filterNot { it in znameKarty }
        val nevidenePodleHodnoty = IntArray(HodnotaKarty.entries.size)
        for (k in nevideneKarty) {
            nevidenePodleHodnoty[k.hodnota.ordinal]++
        }

        val poctyKaretHracu = IntArray(stav.pocetHracu) { p -> stav.ruceHracu[p].size }
        val mujTym = stav.tymHrace(idx)

        var maskaHracuBezVedouci = 0
        val aktVedouci = stav.vedouciHodnota
        if (aktVedouci != null) {
            var prubezneBody = 0
            var prubeznyDrzitel = stav.aktualniStych.firstOrNull()?.indexHrace ?: 0
            for ((poradi, tah) in stav.aktualniStych.withIndex()) {
                val prebil = poradi == 0 || tah.karta.hodnota == aktVedouci || tah.karta.hodnota == HodnotaKarty.R7
                if (prebil) {
                    prubeznyDrzitel = tah.indexHrace
                } else if (prubezneBody >= 10 && (tah.indexHrace % 2) != (prubeznyDrzitel % 2)) {
                    maskaHracuBezVedouci = maskaHracuBezVedouci or (1 shl tah.indexHrace)
                }
                prubezneBody += tah.karta.hodnota.bodyMariasSedma
            }
        }

        val souctySkore = DoubleArray(korenoveAkce.size)

        if (stav.pocetHracu == 2 && velikostTalonu == 0) {
            val presnySim = sestavSimStavProSvet(stav, idx, mojeRuka, poctyKaretHracu, nevideneKarty)
            for ((aIdx, akce) in korenoveAkce.withIndex()) {
                val poAkci = when (akce) {
                    is AkceBotaSedma.PustitStych -> simUzavriStych(presnySim)
                    is AkceBotaSedma.ZahratKartu -> simZahrajKartu(presnySim, akce.karta)
                }
                val citac = CitacUzluSedma(30_000)
                val hodnota = alphaBetaSim(
                    sim = poAkci,
                    zbyvaStychu = 10,
                    alfaPuv = Int.MIN_VALUE / 2,
                    betaPuv = Int.MAX_VALUE / 2,
                    mujTym = mujTym,
                    citac = citac
                )
                souctySkore[aIdx] = hodnota.toDouble()
            }
        } else {
            val pocetSvetu: Int
            val hloubkaStychu: Int
            val rozpocetNaAkci: Int
            when {
                stav.pocetHracu == 2 && velikostTalonu <= 4 -> {
                    pocetSvetu = 28
                    hloubkaStychu = 8
                    rozpocetNaAkci = 900
                }
                stav.pocetHracu == 2 -> {
                    pocetSvetu = 26
                    hloubkaStychu = 2
                    rozpocetNaAkci = 550
                }
                velikostTalonu == 0 -> {
                    pocetSvetu = 22
                    hloubkaStychu = 6
                    rozpocetNaAkci = 750
                }
                else -> {
                    pocetSvetu = 20
                    hloubkaStychu = 2
                    rozpocetNaAkci = 500
                }
            }

            val nahoda = Random(ziskejAktualniCasMs() xor (stav.cisloTahu.toLong() * 2654435761L))
            val michaciPole = nevideneKarty.toMutableList()

            repeat(pocetSvetu) {
                var vybranySvet: SimStavSedma? = null
                val maxPokusu = if (maskaHracuBezVedouci != 0 && aktVedouci != null) 3 else 1
                for (pokus in 0 until maxPokusu) {
                    michaciPole.shuffle(nahoda)
                    val kandidatSim = sestavSimStavProSvet(stav, idx, mojeRuka, poctyKaretHracu, michaciPole)
                    if (pokus == maxPokusu - 1 || aktVedouci == null) {
                        vybranySvet = kandidatSim
                        break
                    }
                    var odporujeDedukci = false
                    for (p in 0 until stav.pocetHracu) {
                        if (p != idx && (maskaHracuBezVedouci and (1 shl p)) != 0) {
                            if (kandidatSim.ruce[p].any { it.hodnota == aktVedouci }) {
                                odporujeDedukci = true
                                break
                            }
                        }
                    }
                    if (!odporujeDedukci) {
                        vybranySvet = kandidatSim
                        break
                    }
                }

                val zakladSvet = vybranySvet ?: sestavSimStavProSvet(stav, idx, mojeRuka, poctyKaretHracu, michaciPole)
                for ((aIdx, akce) in korenoveAkce.withIndex()) {
                    val poAkci = when (akce) {
                        is AkceBotaSedma.PustitStych -> simUzavriStych(zakladSvet)
                        is AkceBotaSedma.ZahratKartu -> simZahrajKartu(zakladSvet, akce.karta)
                    }
                    val dokoncilStych = poAkci.stychHodnoty.isEmpty() && !poAkci.jeKonec
                    val startHloubka = if (dokoncilStych) hloubkaStychu - 1 else hloubkaStychu
                    val citac = CitacUzluSedma(rozpocetNaAkci)
                    val hodnota = alphaBetaSim(
                        sim = poAkci,
                        zbyvaStychu = startHloubka,
                        alfaPuv = Int.MIN_VALUE / 2,
                        betaPuv = Int.MAX_VALUE / 2,
                        mujTym = mujTym,
                        citac = citac
                    )
                    souctySkore[aIdx] += hodnota.toDouble()
                }
            }

            for (i in souctySkore.indices) {
                souctySkore[i] /= pocetSvetu.toDouble()
            }
        }

        // Jemný tie-breaker: šetři sedmičku, při odhozu zahazuj běžné plívy (šetři mrtvé 4. karty na výnos)
        var nejIndex = 0
        var nejSkore = Double.NEGATIVE_INFINITY
        for ((aIdx, akce) in korenoveAkce.withIndex()) {
            val jemnaKorekce = when (akce) {
                is AkceBotaSedma.PustitStych -> 0.05
                is AkceBotaSedma.ZahratKartu -> {
                    val h = akce.karta.hodnota
                    val zbyvaVenku = nevidenePodleHodnoty[h.ordinal]
                    when {
                        h == HodnotaKarty.R7 -> -0.15
                        h.bodyMariasSedma >= 10 -> -0.04
                        stav.aktualniStych.isEmpty() -> (4 - zbyvaVenku) * 0.02
                        else -> zbyvaVenku * 0.02
                    }
                }
            }
            val celkem = souctySkore[aIdx] + jemnaKorekce
            if (celkem > nejSkore) {
                nejSkore = celkem
                nejIndex = aIdx
            }
        }

        return when (val viteznaAkce = korenoveAkce[nejIndex]) {
            is AkceBotaSedma.PustitStych -> pustStych(stav, idx)
            is AkceBotaSedma.ZahratKartu -> zahrajKartu(stav, idx, viteznaAkce.karta)
        }
    }

    fun vyhodnotVysledek(stav: StavSedma): VysledekPartie {
        val spalena = stav.spalenaTym
        val viteznyTym: Int
        val nasobekZapasu: Int
        val duvodNasobku: String
        val kratkeHodnoceni: String

        if (spalena >= 0) {
            viteznyTym = spalena
            nasobekZapasu = 4
            if (stav.spalenaZRuky) {
                duvodNasobku = "Spálená z ruky (4 stejné karty v ruce) — 4× násobek"
                kratkeHodnoceni = "🔥 Spálená z ruky (4×)"
            } else {
                duvodNasobku = "Spálená (4 stejné karty ve štychu) — 4× násobek"
                kratkeHodnoceni = "🔥 Spálená (4×)"
            }
        } else {
            viteznyTym = if (stav.uhraneBodyTym0 > stav.uhraneBodyTym1) 0 else 1
            val porazenyTym = 1 - viteznyTym
            val bodyViteze = if (viteznyTym == 0) stav.uhraneBodyTym0 else stav.uhraneBodyTym1
            val stychyPorazeneho = if (viteznyTym == 0) stav.pocetStychuTym1 else stav.pocetStychuTym0
            when {
                stychyPorazeneho == 0 -> {
                    nasobekZapasu = 3
                    duvodNasobku = "Čistá devadesátka (${stav.nazevTymu(porazenyTym)} je holátko) — 3× násobek"
                    kratkeHodnoceni = "👶 Holátko! (3×)"
                }
                bodyViteze == 90 -> {
                    nasobekZapasu = 2
                    duvodNasobku = "Uhraných všech 90 bodů — 2× násobek"
                    kratkeHodnoceni = "Devadesátka (2×)"
                }
                else -> {
                    nasobekZapasu = 1
                    duvodNasobku = "Běžná výhra na body (1×)"
                    kratkeHodnoceni = "Na body (1×)"
                }
            }
        }

        val stychyText0 = if (spalena < 0 && stav.pocetStychuTym0 == 0) {
            "0 štychů • Holátko!"
        } else {
            sklonujStychy(stav.pocetStychuTym0)
        }
        val stychyText1 = if (spalena < 0 && stav.pocetStychuTym1 == 0) {
            "0 štychů • Holátko!"
        } else {
            sklonujStychy(stav.pocetStychuTym1)
        }

        val zapasoveBody = nasobekZapasu * 10
        val polozky = listOf(
            PolozkaVyuctovani(
                stav.nazevTymu(0),
                "${stav.uhraneBodyTym0} b. ($stychyText0)",
                viteznyTym == 0
            ),
            PolozkaVyuctovani(
                stav.nazevTymu(1),
                "${stav.uhraneBodyTym1} b. ($stychyText1)",
                viteznyTym == 1
            ),
            PolozkaVyuctovani("Hodnocení hry", kratkeHodnoceni, jeZvyraznena = true),
            PolozkaVyuctovani("Zápasové skóre", "+$zapasoveBody b.", jeZvyraznena = true)
        )

        val hraciZmeny = List(stav.pocetHracu) { i ->
            val jeVit = stav.tymHrace(i) == viteznyTym
            ZmenaBoduHrace(
                indexHrace = i,
                jmenoHrace = stav.jmenaHracu[i],
                bodyZapasu = if (jeVit) zapasoveBody else -zapasoveBody,
                zmenaElo = if (jeVit) 12 + nasobekZapasu * 4 else -(10 + nasobekZapasu * 2),
                jeVitez = jeVit
            )
        }

        return VysledekPartie(
            typHry = TypHry.SEDMA,
            nadpis = "Vítězí ${stav.nazevTymu(viteznyTym)}!",
            podnadpis = "$duvodNasobku • ${stav.variantaPravidel.kratkyNazev} pravidla",
            polozky = polozky,
            hraci = hraciZmeny
        )
    }
}
