package cz.mares.ceskekarty

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

object SedmaEngine {

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

        // Kontrola Spálené ve štychu (4 přebíjející karty stejné hodnoty po sobě na stole)
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

        // Ve čtyřhře má štych pevně 4 karty (po jedné od každého hráče u stolu)
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

        // Hra 2 hráčů (1 na 1):
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

    private fun najdiKartuProSpalenouVeStychu(stav: StavSedma, kandidati: Collection<Karta>): Karta? {
        if (stav.aktualniStych.size < 3) return null
        val posledniTri = stav.aktualniStych.takeLast(3)
        val hledanaHodnota = posledniTri.first().karta.hodnota
        if (hledanaHodnota != stav.vedouciHodnota && hledanaHodnota != HodnotaKarty.R7) return null
        if (!posledniTri.all { it.karta.hodnota == hledanaHodnota }) return null
        return kandidati.firstOrNull { it.hodnota == hledanaHodnota }
    }

    fun provedTahBota(stav: StavSedma): StavSedma {
        if (stav.jeKonec) return stav
        val idx = stav.naTahu

        if (zjistiSpalenouVRuce(stav, idx) != null) {
            return nahlasSpalenouZRuky(stav, idx)
        }

        val ruka = stav.ruceHracu[idx]
        if (ruka.isEmpty()) return stav

        // Férová paměť na veřejně odehrané karty + aktuální štych + vlastní ruku (nikdy nenahlíží soupeři do karet)
        val videnePoctyHodnot = IntArray(HodnotaKarty.entries.size)
        for (k in stav.odehraneKarty) {
            videnePoctyHodnot[k.hodnota.ordinal]++
        }
        for (hk in stav.aktualniStych) {
            videnePoctyHodnot[hk.karta.hodnota.ordinal]++
        }
        for (k in ruka) {
            videnePoctyHodnot[k.hodnota.ordinal]++
        }

        fun zbyvaCizichKaret(hodnota: HodnotaKarty): Int {
            return (4 - videnePoctyHodnot[hodnota.ordinal]).coerceAtLeast(0)
        }

        val zbyvaCizichSedem = zbyvaCizichKaret(HodnotaKarty.R7)
        val talonPrazdny = stav.lizaciBalicek.isEmpty()
        val bodyVeStychu = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma }
        val jePosledniStychHry = talonPrazdny && ruka.size == 1

        // 1. Návrat tahu ve dvou hráčích (možnost štych vzít/pustit nebo pokračovat)
        if (stav.muzePustitStych) {
            val povolene = zjistiPovoleneKarty(stav, idx)
            val spalenaKarta = najdiKartuProSpalenouVeStychu(stav, povolene)
            if (spalenaKarta != null) {
                return zahrajKartu(stav, idx, spalenaKarta)
            }

            if (stav.prozatimBereHrac == idx) {
                val dalsiStejnaBezSedmy = povolene.firstOrNull {
                    it.hodnota == stav.vedouciHodnota && it.hodnota != HodnotaKarty.R7
                } ?: return pustStych(stav, idx)

                if (dalsiStejnaBezSedmy.hodnota.bodyMariasSedma >= 10) {
                    return pustStych(stav, idx)
                }

                val zbyvaVenkuVedouci = zbyvaCizichKaret(dalsiStejnaBezSedmy.hodnota)
                if (bodyVeStychu >= 10) {
                    // Ve štychu už leží soupeřovy body -> natahuj jen tehdy, když soupeř nemá sedmu ani stejnou kartu,
                    // nebo máme sedmičku na okamžité zpětné přebití
                    val bezpecneToceni = (zbyvaCizichSedem == 0 && zbyvaVenkuVedouci == 0) ||
                        (zbyvaCizichSedem == 0 && ruka.count { it.hodnota == stav.vedouciHodnota } >= 2) ||
                        (ruka.any { it.hodnota == HodnotaKarty.R7 } && zbyvaCizichSedem <= 1)
                    return if (bezpecneToceni) {
                        zahrajKartu(stav, idx, dalsiStejnaBezSedmy)
                    } else {
                        pustStych(stav, idx)
                    }
                }

                // V nulovém štychu vytahuj ze soupeře karty nebo sedmičku (zvlášť když už nemá další stejnou hodnotu)
                return zahrajKartu(stav, idx, dalsiStejnaBezSedmy)
            } else {
                val vedouciHod = stav.vedouciHodnota
                val zbyvaVenkuVedouci = if (vedouciHod != null) zbyvaCizichKaret(vedouciHod) else 0
                val bezSedmy = povolene.firstOrNull { it.hodnota != HodnotaKarty.R7 }
                if (bezSedmy != null) {
                    // Pokud by šlo o nechráněnou 10/Eso v koncovce, kde má soupeř poslední sedmu a my žádnou, zvaž krytí
                    val hroziZtrataDesitky = bezSedmy.hodnota.bodyMariasSedma >= 10 &&
                        talonPrazdny &&
                        zbyvaCizichSedem > 0 &&
                        ruka.none { it.hodnota == HodnotaKarty.R7 } &&
                        zbyvaVenkuVedouci > 0 &&
                        bodyVeStychu < 20
                    if (!hroziZtrataDesitky) {
                        return zahrajKartu(stav, idx, bezSedmy)
                    }
                }

                val sedmicka = povolene.firstOrNull { it.hodnota == HodnotaKarty.R7 }
                val bojOVynaseniPredPoslednim = talonPrazdny &&
                    ruka.size == 2 &&
                    zbyvaCizichSedem == 0 &&
                    ruka.any { it != sedmicka && zbyvaCizichKaret(it.hodnota) == 0 }
                val maSmyslPrebitSedmou = bodyVeStychu >= 10 ||
                    jePosledniStychHry ||
                    bojOVynaseniPredPoslednim ||
                    (talonPrazdny && ruka.all { it.hodnota == HodnotaKarty.R7 })
                return if (sedmicka != null && maSmyslPrebitSedmou) {
                    zahrajKartu(stav, idx, sedmicka)
                } else {
                    pustStych(stav, idx)
                }
            }
        }

        // 2. Výnos do prázdného štychu s počítáním sedmiček a mrtvých karet
        if (stav.aktualniStych.isEmpty()) {
            val nuloveBezSedmy = ruka.filter {
                it.hodnota != HodnotaKarty.R7 && it.hodnota.bodyMariasSedma == 0
            }
            val bodovane = ruka.filter { it.hodnota.bodyMariasSedma >= 10 }
            val pocetVlastnichSedem = ruka.count { it.hodnota == HodnotaKarty.R7 }

            // A) Všechny soupeřovy sedmičky už padly -> 10 nebo Eso bez zbývajícího dvojčete venku je 100% neporazitelné!
            if (zbyvaCizichSedem == 0) {
                val neprustrelnaBodovana = bodovane.firstOrNull { zbyvaCizichKaret(it.hodnota) == 0 }
                if (neprustrelnaBodovana != null) {
                    return zahrajKartu(stav, idx, neprustrelnaBodovana)
                }
            }

            // B) Mrtvá nebodovaná karta (zbylé 3 kusy této hodnoty už prošly hrou nebo je držíme sami):
            // soupeř ji nemůže přebít stejnou hodnotou a musí buď odevzdat štych, nebo vypotřebovat sedmičku na 0 bodů!
            val mrtvaNulova = nuloveBezSedmy.firstOrNull { zbyvaCizichKaret(it.hodnota) == 0 }
            if (mrtvaNulova != null) {
                return zahrajKartu(stav, idx, mrtvaNulova)
            }

            // C) Silně krytá bodovaná 10 / Eso
            val silnaBodovana = bodovane.firstOrNull { k ->
                val pocetStejnychVRuce = bodovane.count { it.hodnota == k.hodnota }
                val zbyvaVenku = zbyvaCizichKaret(k.hodnota)
                pocetStejnychVRuce >= 3 ||
                    (zbyvaVenku == 0 && pocetVlastnichSedem >= 1 && zbyvaCizichSedem <= 1) ||
                    (stav.pocetHracu == 2 && pocetStejnychVRuce >= 2 && pocetVlastnichSedem >= 2)
            }
            if (silnaBodovana != null) {
                return zahrajKartu(stav, idx, silnaBodovana)
            }

            // D) Běžný nebodovaný výnos — preferuj hodnotu, které venku zbývá nejméně kusů a od které držíme pár
            if (nuloveBezSedmy.isNotEmpty()) {
                val vybranaNulova = nuloveBezSedmy.maxByOrNull { k ->
                    val pocetVRuce = nuloveBezSedmy.count { it.hodnota == k.hodnota }
                    val zbyvaVenku = zbyvaCizichKaret(k.hodnota)
                    pocetVRuce * 10 - zbyvaVenku * 4
                } ?: nuloveBezSedmy.first()
                return zahrajKartu(stav, idx, vybranaNulova)
            }

            // E) Zbývají už jen bodované karty a sedmičky — vynes nejlépe krytou 10/Eso a sedmičky šetři
            if (bodovane.isNotEmpty()) {
                val nejlepeKrytaBodovana = bodovane.minByOrNull { k ->
                    zbyvaCizichKaret(k.hodnota) * 10 - bodovane.count { it.hodnota == k.hodnota }
                } ?: bodovane.first()
                return zahrajKartu(stav, idx, nejlepeKrytaBodovana)
            }

            return zahrajKartu(stav, idx, ruka.first())
        }

        // 3. Odpověď do rozehraného štychu
        val spalenaVeStychu = najdiKartuProSpalenouVeStychu(stav, ruka)
        if (spalenaVeStychu != null) {
            return zahrajKartu(stav, idx, spalenaVeStychu)
        }

        val mujTym = stav.tymHrace(idx)
        val tymDrzitele = stav.tymHrace(stav.prozatimBereHrac)
        val nuloveOdhozy = ruka.filter {
            it.hodnota.bodyMariasSedma == 0 && it.hodnota != HodnotaKarty.R7
        }
        // Jako odhoz volíme kartu, které je venku ještě hodně (mrtvé čtvrté karty si necháváme na vlastní výnos!)
        val nejlepsiNulovyOdhoz = nuloveOdhozy.maxByOrNull { k ->
            zbyvaCizichKaret(k.hodnota) * 10 - nuloveOdhozy.count { it.hodnota == k.hodnota } * 4
        }

        if (mujTym == tymDrzitele && stav.pocetHracu == 4) {
            val jsemPosledniVeStychu = stav.aktualniStych.size == 3
            val vedouciHod = stav.vedouciHodnota
            val tretiPoziceNeprustrelna = stav.aktualniStych.size == 2 &&
                zbyvaCizichSedem == 0 &&
                vedouciHod != null &&
                zbyvaCizichKaret(vedouciHod) == 0

            if (jsemPosledniVeStychu || tretiPoziceNeprustrelna || nejlepsiNulovyOdhoz == null) {
                // Namaž parťákovi 10/Eso (přednostně tu, která je venku nejzranitelnější)
                val namazat = ruka
                    .filter { it.hodnota.bodyMariasSedma >= 10 && it.hodnota != stav.vedouciHodnota }
                    .maxByOrNull { zbyvaCizichKaret(it.hodnota) }
                    ?: ruka.firstOrNull { it.hodnota.bodyMariasSedma >= 10 }
                if (namazat != null) {
                    return zahrajKartu(stav, idx, namazat)
                }
            }
            val odhozPartakovi = nejlepsiNulovyOdhoz
                ?: ruka.firstOrNull { it.hodnota != HodnotaKarty.R7 }
                ?: ruka.first()
            return zahrajKartu(stav, idx, odhozPartakovi)
        }

        // Soupeř prozatím bere štych -> přebij stejnou hodnotou bez plýtvání sedmičkou
        val stejnaBezSedmy = ruka.firstOrNull {
            it.hodnota == stav.vedouciHodnota && it.hodnota != HodnotaKarty.R7
        }
        if (stejnaBezSedmy != null) {
            return zahrajKartu(stav, idx, stejnaBezSedmy)
        }

        val sedmicka = ruka.firstOrNull { it.hodnota == HodnotaKarty.R7 }
        val hroziNucenyOdhozBodu = nejlepsiNulovyOdhoz == null && ruka.any { it.hodnota.bodyMariasSedma >= 10 }
        val prevzitVynaseniDoKoncovky = stav.pocetHracu == 2 &&
            talonPrazdny &&
            ruka.size == 2 &&
            zbyvaCizichSedem == 0 &&
            ruka.any { it != sedmicka && zbyvaCizichKaret(it.hodnota) == 0 }

        if (sedmicka != null && (bodyVeStychu >= 10 || jePosledniStychHry || hroziNucenyOdhozBodu || prevzitVynaseniDoKoncovky)) {
            return zahrajKartu(stav, idx, sedmicka)
        }

        val odhoz = nejlepsiNulovyOdhoz
            ?: ruka.firstOrNull { it.hodnota != HodnotaKarty.R7 }
            ?: ruka.first()
        return zahrajKartu(stav, idx, odhoz)
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
