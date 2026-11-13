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
    val posledniUzavrenyVitez: Int = -1
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
            val ruce = List(pocet) { idx -> retezecNaKarty(r.text("r$idx")) }
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
                posledniUzavrenyVitez = r.cislo("puv", -1)
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
            Karta.seradRuku(ruka)
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
            posledniUzavrenyVitez = -1
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
            if (idx == indexHrace) r.filterNot { it.hodnota == hodnotaCtyrky } else r
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
        // Když se ve 2 hráčích vrací tah na vynášejícího (přebití zpět nebo hospodské natahování štychu),
        // smí pokračovat pouze kartou vedoucí hodnoty nebo sedmičkou.
        if (stav.muzePustitStych) {
            return ruka.filter { kartaPrebijiStych(stav, it) }.toSet()
        }
        return ruka.toSet()
    }

    fun zahrajKartu(stav: StavSedma, indexHrace: Int, karta: Karta): StavSedma {
        val povolene = zjistiPovoleneKarty(stav, indexHrace)
        if (karta !in povolene) return stav

        val jmeno = stav.jmenaHracu[indexHrace]
        val novaRuka = Karta.seradRuku(stav.ruceHracu[indexHrace].filterNot { it == karta })
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }

        val vedouci = stav.vedouciHodnota ?: karta.hodnota
        val prebiji = stav.aktualniStych.isEmpty() ||
            karta.hodnota == vedouci ||
            karta.hodnota == HodnotaKarty.R7
        val novyDrzitel = if (prebiji) indexHrace else stav.prozatimBereHrac
        val novyStych = stav.aktualniStych + HranaKartaStolu(indexHrace, jmeno, karta)

        // Kontrola Spálené ve štychu (4 karty stejné hodnoty po sobě na stole)
        if (novyStych.size >= 4) {
            val posledniCtyri = novyStych.takeLast(4)
            val prvniHod = posledniCtyri.first().karta.hodnota
            if (posledniCtyri.all { it.karta.hodnota == prvniHod }) {
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

        // Ve čtyřhře má štych přesně 4 karty
        if (stav.pocetHracu == 4) {
            if (novyStych.size < 4) {
                val popisAkce = if (prebiji && novyStych.size > 1) "přebíjí kartou" else "přikládá"
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

        // Hra 2 hráčů (1 na 1)
        if (novyStych.size % 2 == 1) {
            val sloveso = if (novyStych.size == 1) "vynáší" else "natahuje štych kartou"
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
            val maCimPokracovat = rukaVynasejiciho.any {
                it.hodnota == vedouci || it.hodnota == HodnotaKarty.R7
            } && noveRuce[odpovidajici].isNotEmpty()

            return if (souperPrebil && maCimPokracovat) {
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
            } else if (!souperPrebil && stav.variantaPravidel == VariantaPravidel.HOSPODSKA && maCimPokracovat) {
                // Hospodská pravidla: vynášející štych drží a má v ruce další stejnou kartu (nebo sedmu),
                // takže může štych natáhnout a přinutit soupeře odhodit další kartu, nebo štych vzít.
                stav.copy(
                    ruceHracu = noveRuce,
                    aktualniStych = novyStych,
                    vedouciHodnota = vedouci,
                    prozatimBereHrac = novyDrzitel,
                    naTahu = vynasejici,
                    muzePustitStych = true,
                    posledniUzavrenyStych = emptyList(),
                    posledniUzavrenyVitez = -1,
                    posledniHlaska = "$jmeno odhodil ${karta.celyNazev()} — ${stav.jmenaHracu[vynasejici]} může vynést další stejnou kartu, nebo štych vzít!",
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

        val lizaci = stav.lizaciBalicek.toMutableList()
        val ruceMut = stav.ruceHracu.map { it.toMutableList() }

        while (lizaci.size >= stav.pocetHracu && ruceMut[vitezStychu].size < 4) {
            for (posun in 0 until stav.pocetHracu) {
                val idx = (vitezStychu + posun) % stav.pocetHracu
                ruceMut[idx].add(lizaci.removeAt(0))
            }
        }

        val serazeneRuce = ruceMut.map { Karta.seradRuku(it) }
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
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun provedTahBota(stav: StavSedma): StavSedma {
        if (stav.jeKonec) return stav
        val idx = stav.naTahu

        // V hospodských pravidlech bot okamžitě ohlásí Spálenou, pokud drží 4 stejné karty na ruce
        if (zjistiSpalenouVRuce(stav, idx) != null) {
            return nahlasSpalenouZRuky(stav, idx)
        }

        val ruka = stav.ruceHracu[idx]
        if (ruka.isEmpty()) return stav

        if (stav.muzePustitStych) {
            val povolene = zjistiPovoleneKarty(stav, idx)
            if (stav.prozatimBereHrac == idx) {
                // Hospodská pravidla: bot už štych drží -> natáhne ho další kartou stejné hodnoty (neplýtvá sedmou)
                val dalsiStejnaBezSedmy = povolene.firstOrNull { it.hodnota != HodnotaKarty.R7 }
                return if (dalsiStejnaBezSedmy != null) {
                    zahrajKartu(stav, idx, dalsiStejnaBezSedmy)
                } else {
                    pustStych(stav, idx)
                }
            } else {
                // Soupeř štych přebil -> bot zváží, zda vrátit přebití nebo štych pustit
                val bodyNaStole = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma }
                val bezSedmy = povolene.firstOrNull { it.hodnota != HodnotaKarty.R7 }
                return if (bezSedmy != null) {
                    zahrajKartu(stav, idx, bezSedmy)
                } else if (bodyNaStole >= 10 && povolene.isNotEmpty()) {
                    zahrajKartu(stav, idx, povolene.first())
                } else {
                    pustStych(stav, idx)
                }
            }
        }

        if (stav.aktualniStych.isEmpty()) {
            // V hospodských pravidlech bot rád vynese hodnotu, od které drží více kusů v ruce (mimo sedmiček)
            val bezSedmy = ruka.filter { it.hodnota != HodnotaKarty.R7 }
            val kandidat = if (stav.variantaPravidel == VariantaPravidel.HOSPODSKA && stav.pocetHracu == 2 && bezSedmy.isNotEmpty()) {
                bezSedmy.maxByOrNull { k ->
                    val pocetStejnych = bezSedmy.count { it.hodnota == k.hodnota }
                    val penalizaceNechranene10 = if (k.hodnota.bodyMariasSedma == 10 && pocetStejnych == 1) -3 else 0
                    pocetStejnych * 4 + penalizaceNechranene10
                } ?: bezSedmy.first()
            } else {
                ruka.firstOrNull {
                    it.hodnota != HodnotaKarty.R7 && it.hodnota.bodyMariasSedma == 0
                } ?: ruka.firstOrNull { it.hodnota != HodnotaKarty.R7 } ?: ruka.first()
            }
            return zahrajKartu(stav, idx, kandidat)
        }

        val mujTym = stav.tymHrace(idx)
        val tymDrzitele = stav.tymHrace(stav.prozatimBereHrac)
        val bodyVeStychu = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma }

        if (mujTym == tymDrzitele && stav.pocetHracu == 4) {
            // Parťák zatím bere štych -> namaž mu desítku nebo eso!
            val namazat = ruka.firstOrNull {
                it.hodnota.bodyMariasSedma == 10 && it.hodnota != stav.vedouciHodnota
            } ?: ruka.firstOrNull { it.hodnota != HodnotaKarty.R7 && it.hodnota.bodyMariasSedma == 0 }
                ?: ruka.first()
            return zahrajKartu(stav, idx, namazat)
        }

        // Soupeř bere štych -> zkus přebít stejnou hodnotou, případně sedmou, pokud jsou ve štychu body
        val stejnaHodnota = ruka.firstOrNull { it.hodnota == stav.vedouciHodnota }
        if (stejnaHodnota != null) {
            return zahrajKartu(stav, idx, stejnaHodnota)
        }
        val sedmicka = ruka.firstOrNull { it.hodnota == HodnotaKarty.R7 }
        if (sedmicka != null && bodyVeStychu >= 10) {
            return zahrajKartu(stav, idx, sedmicka)
        }

        val odhozBezBodu = ruka.firstOrNull {
            it.hodnota.bodyMariasSedma == 0 && it.hodnota != HodnotaKarty.R7
        } ?: ruka.first()
        return zahrajKartu(stav, idx, odhozBezBodu)
    }

    fun vyhodnotVysledek(stav: StavSedma): VysledekPartie {
        val spalena = stav.spalenaTym
        val viteznyTym: Int
        val nasobekZapasu: Int
        val duvodNasobku: String

        if (spalena >= 0) {
            viteznyTym = spalena
            nasobekZapasu = 4
            duvodNasobku = if (stav.spalenaZRuky) {
                "Spálená z ruky (4 stejné karty v ruce) — 4× násobek"
            } else {
                "Spálená (4 stejné karty ve štychu) — 4× násobek"
            }
        } else {
            viteznyTym = if (stav.uhraneBodyTym0 > stav.uhraneBodyTym1) 0 else 1
            val bodyViteze = if (viteznyTym == 0) stav.uhraneBodyTym0 else stav.uhraneBodyTym1
            val stychyPorazeneho = if (viteznyTym == 0) stav.pocetStychuTym1 else stav.pocetStychuTym0
            when {
                stychyPorazeneho == 0 -> {
                    nasobekZapasu = 3
                    duvodNasobku = "Čistá devadesátka bez jediného štychu soupeře — 3× násobek"
                }
                bodyViteze == 90 -> {
                    nasobekZapasu = 2
                    duvodNasobku = "Uhraných všech 90 bodů — 2× násobek"
                }
                else -> {
                    nasobekZapasu = 1
                    duvodNasobku = "Běžná výhra na body (1×)"
                }
            }
        }

        val zapasoveBody = nasobekZapasu * 10
        val polozky = listOf(
            PolozkaVyuctovani(
                stav.nazevTymu(0),
                "${stav.uhraneBodyTym0} b. (${sklonujStychy(stav.pocetStychuTym0)})",
                viteznyTym == 0
            ),
            PolozkaVyuctovani(
                stav.nazevTymu(1),
                "${stav.uhraneBodyTym1} b. (${sklonujStychy(stav.pocetStychuTym1)})",
                viteznyTym == 1
            ),
            PolozkaVyuctovani("Hodnocení hry", duvodNasobku, jeZvyraznena = true),
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
