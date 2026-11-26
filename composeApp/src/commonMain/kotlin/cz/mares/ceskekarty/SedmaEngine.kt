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

private class SimKoncovkaSedma(
    val pocetHracu: Int,
    val variantaPravidel: VariantaPravidel,
    val ruce: List<List<Karta>>,
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

    // =========================================================================
    // FÁZE 1 (DOKUD JE TALON): Čistá karbanická taktika s počítáním odehraných karet
    // =========================================================================

    private fun najdiKartuProSpalenouVeStychu(stav: StavSedma, kandidati: Collection<Karta>): Karta? {
        if (stav.aktualniStych.size < 3) return null
        val posledniTri = stav.aktualniStych.takeLast(3)
        val hledana = posledniTri.first().karta.hodnota
        if (hledana != stav.vedouciHodnota && hledana != HodnotaKarty.R7) return null
        if (!posledniTri.all { it.karta.hodnota == hledana }) return null
        return kandidati.firstOrNull { it.hodnota == hledana }
    }

    /**
     * Hlídá, abychom nepřiložili 3. stejnou kartu v řadě, když 4. kus je pořád u soupeřů/v talonu
     * a soupeř má ještě příležitost štych zavřít na Spálenou!
     */
    private fun hroziNahravkaNaSpalenou(
        stav: StavSedma,
        hodnotaKarty: HodnotaKarty,
        zbyvaCizichTetoHodnoty: Int
    ): Boolean {
        if (zbyvaCizichTetoHodnoty <= 0) return false
        if (stav.pocetHracu == 4 && stav.aktualniStych.size == 3) return false
        if (stav.aktualniStych.size < 2) return false
        val vedouci = stav.vedouciHodnota ?: return false
        if (hodnotaKarty != vedouci && hodnotaKarty != HodnotaKarty.R7) return false
        val n = stav.aktualniStych.size
        return stav.aktualniStych[n - 1].karta.hodnota == hodnotaKarty &&
            stav.aktualniStych[n - 2].karta.hodnota == hodnotaKarty
    }

    private fun provedTahSFaziTalonu(stav: StavSedma): StavSedma {
        val idx = stav.naTahu
        val ruka = stav.ruceHracu[idx]

        // Férová tabulka neviděných karet (nikdy nečte karty soupeřů ani obsah talonu!)
        val videnePocty = IntArray(HodnotaKarty.entries.size)
        for (k in stav.odehraneKarty) videnePocty[k.hodnota.ordinal]++
        for (hk in stav.aktualniStych) videnePocty[hk.karta.hodnota.ordinal]++
        for (k in ruka) videnePocty[k.hodnota.ordinal]++

        fun zbyvaCizich(hodnota: HodnotaKarty): Int = (4 - videnePocty[hodnota.ordinal]).coerceAtLeast(0)

        val zbyvaCizichSedem = zbyvaCizich(HodnotaKarty.R7)
        val pocetVlastnichSedem = ruka.count { it.hodnota == HodnotaKarty.R7 }
        val bodyVeStychu = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma }
        val mujTym = stav.tymHrace(idx)
        val mojeBody = if (mujTym == 0) stav.uhraneBodyTym0 else stav.uhraneBodyTym1
        val souperBody = if (mujTym == 0) stav.uhraneBodyTym1 else stav.uhraneBodyTym0

        // 1) Návrat tahu v 1 na 1 (rozhodnutí točit / přebít zpět / pustit štych)
        if (stav.muzePustitStych) {
            val povolene = zjistiPovoleneKarty(stav, idx)
            val spalena = najdiKartuProSpalenouVeStychu(stav, povolene)
            if (spalena != null) return zahrajKartu(stav, idx, spalena)

            if (stav.prozatimBereHrac == idx) {
                // Štych už držíme my (hospodské točení):
                val dalsiStejna = povolene.firstOrNull {
                    it.hodnota == stav.vedouciHodnota && it.hodnota != HodnotaKarty.R7
                } ?: return pustStych(stav, idx)

                // Bodovanou kartu ani štych, ve kterém už leží 10+ bodů, zbytečně neriskuj — ber domů!
                if (dalsiStejna.hodnota.bodyMariasSedma >= 10) return pustStych(stav, idx)
                if (bodyVeStychu >= 10) {
                    val neprustrelne = zbyvaCizichSedem == 0 && zbyvaCizich(dalsiStejna.hodnota) == 0
                    return if (neprustrelne) zahrajKartu(stav, idx, dalsiStejna) else pustStych(stav, idx)
                }

                // V nulovém štychu toč jen tehdy, když nenahraješ na Spálenou a máš jištění
                if (hroziNahravkaNaSpalenou(stav, dalsiStejna.hodnota, zbyvaCizich(dalsiStejna.hodnota))) {
                    return pustStych(stav, idx)
                }
                val maJisteni = pocetVlastnichSedem >= 1 ||
                    ruka.count { it.hodnota == dalsiStejna.hodnota } >= 2 ||
                    zbyvaCizichSedem == 0
                return if (maJisteni) zahrajKartu(stav, idx, dalsiStejna) else pustStych(stav, idx)
            } else {
                // Štych přebil soupeř:
                val vedouci = stav.vedouciHodnota
                val bezSedmy = povolene.firstOrNull { it.hodnota != HodnotaKarty.R7 }

                if (bodyVeStychu == 0) {
                    // Na nulový štych NIKDY nepálíme sedmičku! Přebij jen stejnou plívou bez rizika Spálené.
                    if (bezSedmy != null && !hroziNahravkaNaSpalenou(stav, bezSedmy.hodnota, zbyvaCizich(bezSedmy.hodnota))) {
                        return zahrajKartu(stav, idx, bezSedmy)
                    }
                    return pustStych(stav, idx)
                }

                // Ve štychu leží 10+ bodů:
                if (bezSedmy != null && !hroziNahravkaNaSpalenou(stav, bezSedmy.hodnota, zbyvaCizich(bezSedmy.hodnota))) {
                    return zahrajKartu(stav, idx, bezSedmy)
                }
                val sedmicka = povolene.firstOrNull { it.hodnota == HodnotaKarty.R7 }
                val zbyvaVedouci = if (vedouci != null) zbyvaCizich(vedouci) else 0
                val vyplatiseSedma = bodyVeStychu >= 20 ||
                    pocetVlastnichSedem >= 2 ||
                    zbyvaCizichSedem == 0 ||
                    zbyvaVedouci <= 1
                return if (sedmicka != null && vyplatiseSedma) {
                    zahrajKartu(stav, idx, sedmicka)
                } else {
                    pustStych(stav, idx)
                }
            }
        }

        // 2) Výnos do prázdného štychu
        if (stav.aktualniStych.isEmpty()) {
            val plivy = ruka.filter { it.hodnota != HodnotaKarty.R7 && it.hodnota.bodyMariasSedma == 0 }
            val bodovane = ruka.filter { it.hodnota.bodyMariasSedma >= 10 }

            // A) 100% neporazitelná desítka nebo eso (venku už není žádná sedma ani stejná karta)
            if (zbyvaCizichSedem == 0) {
                val jistaBodovana = bodovane.firstOrNull { zbyvaCizich(it.hodnota) == 0 }
                if (jistaBodovana != null) return zahrajKartu(stav, idx, jistaBodovana)
            }

            // B) Trojice plív (držíme 3 ze 4 kusů -> soupeř nemůže udělat Spálenou a my ho vyždímáme)
            val trojicePliv = plivy.firstOrNull { p -> plivy.count { it.hodnota == p.hodnota } >= 3 }
            if (trojicePliv != null) return zahrajKartu(stav, idx, trojicePliv)

            // C) Karbanická past na sedmu: dvojice (nebo trojice) desítek/es krytá sedmičkou (nebo do vybitých sedem)
            val pastBodovana = bodovane.firstOrNull { k ->
                val pocetStejnych = bodovane.count { it.hodnota == k.hodnota }
                pocetStejnych >= 3 ||
                    (pocetStejnych >= 2 && (pocetVlastnichSedem >= 1 || zbyvaCizichSedem <= 1))
            }
            if (pastBodovana != null) return zahrajKartu(stav, idx, pastBodovana)

            // D) Mrtvá 4. plíva (zbylé 3 kusy už padly -> soupeř ji nemůže vzít bez sedmičky!)
            val mrtvaPliva = plivy.firstOrNull { zbyvaCizich(it.hodnota) == 0 }
            if (mrtvaPliva != null) return zahrajKartu(stav, idx, mrtvaPliva)

            // E) Nejlepší plíva (přednostně z páru a s nejmenším počtem kusů venku — NIKDY holá 10/Eso/7!)
            if (plivy.isNotEmpty()) {
                val nejPliva = plivy.maxBy { k ->
                    plivy.count { it.hodnota == k.hodnota } * 12 - zbyvaCizich(k.hodnota) * 5
                }
                return zahrajKartu(stav, idx, nejPliva)
            }

            // F) Nouzový výnos (v ruce jsou výhradně 10/Esa a sedmičky — šetři sedmičky!)
            if (bodovane.isNotEmpty()) {
                val nejlepeKryta = bodovane.minBy { zbyvaCizich(it.hodnota) }
                return zahrajKartu(stav, idx, nejlepeKryta)
            }
            return zahrajKartu(stav, idx, ruka.first())
        }

        // 3) Odpověď do rozehraného štychu
        val spalenaStych = najdiKartuProSpalenouVeStychu(stav, ruka)
        if (spalenaStych != null) return zahrajKartu(stav, idx, spalenaStych)

        val tymDrzitele = stav.tymHrace(stav.prozatimBereHrac)
        val plivy = ruka.filter { it.hodnota != HodnotaKarty.R7 && it.hodnota.bodyMariasSedma == 0 }
        // Na odhoz vybíráme osamocenou plívu, které je venku nejvíc (párové a mrtvé 4. plívy šetříme na výnos!)
        val nejlepsiPlivaOdhoz = plivy.maxByOrNull { k ->
            zbyvaCizich(k.hodnota) * 10 - plivy.count { it.hodnota == k.hodnota } * 8
        }

        // Čtyřhra: štych prozatím drží náš parťák
        if (stav.pocetHracu == 4 && tymDrzitele == mujTym) {
            val vedouci = stav.vedouciHodnota
            val muzuBezpecneNamazat = stav.aktualniStych.size == 3 ||
                (stav.aktualniStych.size == 2 && zbyvaCizichSedem == 0 && vedouci != null && zbyvaCizich(vedouci) == 0)

            if (muzuBezpecneNamazat || nejlepsiPlivaOdhoz == null) {
                val namaz = ruka
                    .filter { it.hodnota.bodyMariasSedma >= 10 && it.hodnota != vedouci }
                    .maxByOrNull { zbyvaCizich(it.hodnota) }
                    ?: ruka.firstOrNull { it.hodnota.bodyMariasSedma >= 10 }
                if (namaz != null) return zahrajKartu(stav, idx, namaz)
            }
            return zahrajKartu(stav, idx, nejlepsiPlivaOdhoz ?: ruka.firstOrNull { it.hodnota != HodnotaKarty.R7 } ?: ruka.first())
        }

        // Štych prozatím drží soupeř:
        val vedouci = stav.vedouciHodnota
        val stejnaBezSedmy = ruka.firstOrNull { it.hodnota == vedouci && it.hodnota != HodnotaKarty.R7 }

        if (bodyVeStychu == 0) {
            // A) Soupeř nese nulovou plívu -> přebij stejnou plívou, jinak odhoď plívu (NIKDY sedmu, pokud máš plívu!)
            if (stejnaBezSedmy != null && !hroziNahravkaNaSpalenou(stav, stejnaBezSedmy.hodnota, zbyvaCizich(stejnaBezSedmy.hodnota))) {
                return zahrajKartu(stav, idx, stejnaBezSedmy)
            }
            if (nejlepsiPlivaOdhoz != null) {
                return zahrajKartu(stav, idx, nejlepsiPlivaOdhoz)
            }
            // V ruce není ani jedna plíva (jen sedmy a 10/Esa) -> zachraň vlastní body sedmičkou
            val nouzovaSedma = ruka.firstOrNull { it.hodnota == HodnotaKarty.R7 }
            return zahrajKartu(stav, idx, nouzovaSedma ?: ruka.first())
        }

        // B) Soupeř nese bodovaný štych (10+ bodů):
        if (stejnaBezSedmy != null && !hroziNahravkaNaSpalenou(stav, stejnaBezSedmy.hodnota, zbyvaCizich(stejnaBezSedmy.hodnota))) {
            return zahrajKartu(stav, idx, stejnaBezSedmy)
        }

        val sedmicka = ruka.firstOrNull { it.hodnota == HodnotaKarty.R7 }
        if (sedmicka != null && !hroziNahravkaNaSpalenou(stav, HodnotaKarty.R7, zbyvaCizichSedem)) {
            val zbyvaVedouci = if (vedouci != null) zbyvaCizich(vedouci) else 0
            val jsemPosledniVeStychu4 = stav.pocetHracu == 4 && stav.aktualniStych.size == 3
            val vzitSedmou = bodyVeStychu >= 20 ||
                jsemPosledniVeStychu4 ||
                pocetVlastnichSedem >= 2 ||
                zbyvaVedouci <= 1 ||
                nejlepsiPlivaOdhoz == null ||
                mojeBody < souperBody ||
                ((stav.cisloTahu + stav.odehraneKarty.size) % 3 != 0)
            if (vzitSedmou) {
                return zahrajKartu(stav, idx, sedmicka)
            }
        }

        return zahrajKartu(
            stav,
            idx,
            nejlepsiPlivaOdhoz ?: ruka.firstOrNull { it.hodnota != HodnotaKarty.R7 } ?: ruka.first()
        )
    }

    // =========================================================================
    // FÁZE 2 (KONCOVKA PO DOBRÁNÍ TALONU): 100% přesný Alpha-Beta Minimax do konce
    // =========================================================================

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

    private fun maSpalenouZRukySim(sim: SimKoncovkaSedma, indexHrace: Int): Boolean {
        if (sim.jeKonec || sim.variantaPravidel != VariantaPravidel.HOSPODSKA) return false
        if (sim.naTahu != indexHrace && sim.stychHodnoty.isNotEmpty()) return false
        val ruka = sim.ruce[indexHrace]
        if (ruka.size < 4) return false
        val h0 = ruka[0].hodnota
        return ruka[1].hodnota == h0 && ruka[2].hodnota == h0 && ruka[3].hodnota == h0
    }

    private fun simUzavriKoncovku(sim: SimKoncovkaSedma): SimKoncovkaSedma {
        val vitez = sim.prozatimBereHrac
        val viteznyTym = vitez % 2
        val vsechnyPrazdne = sim.ruce.all { it.isEmpty() }
        val prirustek = sim.bodyAktualnihoStychu + if (vsechnyPrazdne) 10 else 0

        return SimKoncovkaSedma(
            pocetHracu = sim.pocetHracu,
            variantaPravidel = sim.variantaPravidel,
            ruce = sim.ruce,
            stychHodnoty = emptyList(),
            vynasejiciStychu = vitez,
            vedouciHodnota = null,
            prozatimBereHrac = vitez,
            bodyAktualnihoStychu = 0,
            naTahu = vitez,
            muzePustitStych = false,
            bodyTym0 = sim.bodyTym0 + if (viteznyTym == 0) prirustek else 0,
            bodyTym1 = sim.bodyTym1 + if (viteznyTym == 1) prirustek else 0,
            stychyTym0 = sim.stychyTym0 + if (viteznyTym == 0) 1 else 0,
            stychyTym1 = sim.stychyTym1 + if (viteznyTym == 1) 1 else 0,
            spalenaTym = -1,
            jeKonec = vsechnyPrazdne
        )
    }

    private fun simZahrajKoncovku(sim: SimKoncovkaSedma, karta: Karta): SimKoncovkaSedma {
        val hrac = sim.naTahu
        val puvRuka = sim.ruce[hrac]
        val novaRuka = ArrayList<Karta>(puvRuka.size - 1)
        var odebrano = false
        for (k in puvRuka) {
            if (!odebrano && k == karta) odebrano = true else novaRuka.add(k)
        }
        val noveRuce = sim.ruce.mapIndexed { i, r -> if (i == hrac) novaRuka else r }

        val prvni = sim.stychHodnoty.isEmpty()
        val vynasejici = if (prvni) hrac else sim.vynasejiciStychu
        val vedouci = sim.vedouciHodnota ?: karta.hodnota
        val prebiji = prvni || karta.hodnota == vedouci || karta.hodnota == HodnotaKarty.R7
        val novyDrzitel = if (prebiji) hrac else sim.prozatimBereHrac
        val novyStych = sim.stychHodnoty + karta.hodnota
        val noveBody = sim.bodyAktualnihoStychu + karta.hodnota.bodyMariasSedma

        val n = novyStych.size
        if (n >= 4) {
            val hPosl = novyStych[n - 1]
            if ((hPosl == vedouci || hPosl == HodnotaKarty.R7) &&
                novyStych[n - 2] == hPosl &&
                novyStych[n - 3] == hPosl &&
                novyStych[n - 4] == hPosl
            ) {
                return SimKoncovkaSedma(
                    pocetHracu = sim.pocetHracu,
                    variantaPravidel = sim.variantaPravidel,
                    ruce = noveRuce,
                    stychHodnoty = novyStych,
                    vynasejiciStychu = vynasejici,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    bodyAktualnihoStychu = noveBody,
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
            val dalsiSim = SimKoncovkaSedma(
                pocetHracu = 4,
                variantaPravidel = sim.variantaPravidel,
                ruce = noveRuce,
                stychHodnoty = novyStych,
                vynasejiciStychu = vynasejici,
                vedouciHodnota = vedouci,
                prozatimBereHrac = novyDrzitel,
                bodyAktualnihoStychu = noveBody,
                naTahu = dalsi,
                muzePustitStych = false,
                bodyTym0 = sim.bodyTym0,
                bodyTym1 = sim.bodyTym1,
                stychyTym0 = sim.stychyTym0,
                stychyTym1 = sim.stychyTym1,
                spalenaTym = -1,
                jeKonec = false
            )
            return if (novyStych.size < 4) dalsiSim else simUzavriKoncovku(dalsiSim)
        }

        if (novyStych.size % 2 == 1) {
            return SimKoncovkaSedma(
                pocetHracu = 2,
                variantaPravidel = sim.variantaPravidel,
                ruce = noveRuce,
                stychHodnoty = novyStych,
                vynasejiciStychu = vynasejici,
                vedouciHodnota = vedouci,
                prozatimBereHrac = novyDrzitel,
                bodyAktualnihoStychu = noveBody,
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

        val zakladni = SimKoncovkaSedma(
            pocetHracu = 2,
            variantaPravidel = sim.variantaPravidel,
            ruce = noveRuce,
            stychHodnoty = novyStych,
            vynasejiciStychu = vynasejici,
            vedouciHodnota = vedouci,
            prozatimBereHrac = novyDrzitel,
            bodyAktualnihoStychu = noveBody,
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
            simUzavriKoncovku(zakladni)
        }
    }

    private fun ohodnotKonec(sim: SimKoncovkaSedma, mujTym: Int): Int {
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

    private fun generujAkceKoncovky(sim: SimKoncovkaSedma): List<AkceBotaSedma> {
        val hrac = sim.naTahu
        val ruka = sim.ruce[hrac]
        if (ruka.isEmpty()) return emptyList()

        if (sim.muzePustitStych) {
            val vedouci = sim.vedouciHodnota ?: return listOf(AkceBotaSedma.PustitStych)
            val karty = if (sim.prozatimBereHrac == hrac) {
                vyberUnikatniPodleHodnoty(ruka.filter { it.hodnota == vedouci })
            } else {
                vyberUnikatniPodleHodnoty(ruka.filter { it.hodnota == vedouci || it.hodnota == HodnotaKarty.R7 })
            }
            val akce = ArrayList<AkceBotaSedma>(karty.size + 1)
            for (k in karty) akce.add(AkceBotaSedma.ZahratKartu(k))
            akce.add(AkceBotaSedma.PustitStych)
            return akce
        }
        return vyberUnikatniPodleHodnoty(ruka).map { AkceBotaSedma.ZahratKartu(it) }
    }

    private fun alphaBetaKoncovka(
        sim: SimKoncovkaSedma,
        alfaPuv: Int,
        betaPuv: Int,
        mujTym: Int,
        citac: CitacUzluSedma
    ): Int {
        if (sim.jeKonec || citac.zbyva <= 0) {
            return ohodnotKonec(sim, mujTym)
        }
        if (maSpalenouZRukySim(sim, sim.naTahu)) {
            return if (sim.naTahu % 2 == mujTym) 4000 else -4000
        }
        citac.zbyva--

        val akceSeznam = generujAkceKoncovky(sim)
        if (akceSeznam.isEmpty()) return ohodnotKonec(sim, mujTym)

        var alfa = alfaPuv
        var beta = betaPuv
        if ((sim.naTahu % 2) == mujTym) {
            var nej = Int.MIN_VALUE / 2
            for (akce in akceSeznam) {
                val dalsi = when (akce) {
                    is AkceBotaSedma.PustitStych -> simUzavriKoncovku(sim)
                    is AkceBotaSedma.ZahratKartu -> simZahrajKoncovku(sim, akce.karta)
                }
                val h = alphaBetaKoncovka(dalsi, alfa, beta, mujTym, citac)
                if (h > nej) nej = h
                if (nej > alfa) alfa = nej
                if (alfa >= beta) break
            }
            return nej
        } else {
            var nej = Int.MAX_VALUE / 2
            for (akce in akceSeznam) {
                val dalsi = when (akce) {
                    is AkceBotaSedma.PustitStych -> simUzavriKoncovku(sim)
                    is AkceBotaSedma.ZahratKartu -> simZahrajKoncovku(sim, akce.karta)
                }
                val h = alphaBetaKoncovka(dalsi, alfa, beta, mujTym, citac)
                if (h < nej) nej = h
                if (nej < beta) beta = nej
                if (alfa >= beta) break
            }
            return nej
        }
    }

    private fun sestavSimKoncovky(
        stav: StavSedma,
        idxBota: Int,
        mojeRuka: List<Karta>,
        zamichaneNevidene: List<Karta>
    ): SimKoncovkaSedma {
        var pos = 0
        val ruce = List(stav.pocetHracu) { p ->
            if (p == idxBota) {
                mojeRuka
            } else {
                val pocet = stav.ruceHracu[p].size
                val vyrez = ArrayList<Karta>(pocet)
                repeat(pocet) {
                    if (pos < zamichaneNevidene.size) vyrez.add(zamichaneNevidene[pos++])
                }
                vyrez
            }
        }
        return SimKoncovkaSedma(
            pocetHracu = stav.pocetHracu,
            variantaPravidel = stav.variantaPravidel,
            ruce = ruce,
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

        // Dokud jsou v talonu karty, hraj čistou karbanickou taktiku s počítáním odehraných karet
        if (stav.lizaciBalicek.isNotEmpty()) {
            return provedTahSFaziTalonu(stav)
        }

        // Po dobrání talonu (poslední 4 štychy) dopočítej přesný Alpha-Beta Minimax až na konec hry
        val povolene = zjistiPovoleneKarty(stav, idx)
        val unikatni = vyberUnikatniPodleHodnoty(povolene)
        val korenoveAkce = ArrayList<AkceBotaSedma>(unikatni.size + 1)
        for (k in unikatni) korenoveAkce.add(AkceBotaSedma.ZahratKartu(k))
        if (stav.muzePustitStych) korenoveAkce.add(AkceBotaSedma.PustitStych)

        if (korenoveAkce.isEmpty()) return stav
        if (korenoveAkce.size == 1) {
            return when (val jedina = korenoveAkce.first()) {
                is AkceBotaSedma.PustitStych -> pustStych(stav, idx)
                is AkceBotaSedma.ZahratKartu -> zahrajKartu(stav, idx, jedina.karta)
            }
        }

        val zname = HashSet<Karta>(32)
        zname.addAll(mojeRuka)
        zname.addAll(stav.odehraneKarty)
        for (hk in stav.aktualniStych) zname.add(hk.karta)
        val nevidene = VSECHNY_KARTY_BALICKU.filterNot { it in zname }
        val mujTym = stav.tymHrace(idx)
        val soucty = DoubleArray(korenoveAkce.size)

        if (stav.pocetHracu == 2) {
            // V duelu 1 na 1 po dobrání talonu jsou karty soupeře z počítání 100% jisté -> jediný přesný svět
            val presny = sestavSimKoncovky(stav, idx, mojeRuka, nevidene)
            for ((aIdx, akce) in korenoveAkce.withIndex()) {
                val poAkci = when (akce) {
                    is AkceBotaSedma.PustitStych -> simUzavriKoncovku(presny)
                    is AkceBotaSedma.ZahratKartu -> simZahrajKoncovku(presny, akce.karta)
                }
                val citac = CitacUzluSedma(100_000)
                soucty[aIdx] = alphaBetaKoncovka(poAkci, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, mujTym, citac).toDouble()
            }
        } else {
            val pocetSvetu = 24
            val nahoda = Random(ziskejAktualniCasMs() xor (stav.cisloTahu.toLong() * 2654435761L))
            val michaci = nevidene.toMutableList()
            repeat(pocetSvetu) {
                michaci.shuffle(nahoda)
                val svet = sestavSimKoncovky(stav, idx, mojeRuka, michaci)
                for ((aIdx, akce) in korenoveAkce.withIndex()) {
                    val poAkci = when (akce) {
                        is AkceBotaSedma.PustitStych -> simUzavriKoncovku(svet)
                        is AkceBotaSedma.ZahratKartu -> simZahrajKoncovku(svet, akce.karta)
                    }
                    val citac = CitacUzluSedma(8_000)
                    soucty[aIdx] += alphaBetaKoncovka(poAkci, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, mujTym, citac).toDouble()
                }
            }
            for (i in soucty.indices) soucty[i] /= pocetSvetu.toDouble()
        }

        var nejIdx = 0
        var nejSkore = Double.NEGATIVE_INFINITY
        for ((aIdx, akce) in korenoveAkce.withIndex()) {
            val korekce = when (akce) {
                is AkceBotaSedma.PustitStych -> 0.05
                is AkceBotaSedma.ZahratKartu -> when {
                    akce.karta.hodnota == HodnotaKarty.R7 -> -0.15
                    akce.karta.hodnota.bodyMariasSedma >= 10 -> -0.05
                    else -> 0.0
                }
            }
            val celkem = soucty[aIdx] + korekce
            if (celkem > nejSkore) {
                nejSkore = celkem
                nejIdx = aIdx
            }
        }

        return when (val vitezna = korenoveAkce[nejIdx]) {
            is AkceBotaSedma.PustitStych -> pustStych(stav, idx)
            is AkceBotaSedma.ZahratKartu -> zahrajKartu(stav, idx, vitezna.karta)
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
