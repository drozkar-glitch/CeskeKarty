package cz.mares.ceskekarty

enum class FazeMariase {
    VOLBA_TRUMFU,
    ODKLADANI_TALONU,
    FLEKOVANI,
    SEHRAVKA,
    KONEC
}

enum class ZavazekMariase(val nazev: String, val jeBezTrumfu: Boolean) {
    HRA("Hra", false),
    HRA_A_SEDMA("Hra + Hlášená sedma", false),
    BETL("Betl (0 štychů)", true),
    DURCH("Durch (všech 10 štychů)", true)
}

data class StavMarias(
    val jmenaHracu: List<String>,
    val indexForhonta: Int,
    val faze: FazeMariase,
    val ruceHracu: List<List<Karta>>,
    val odlozenyZbytekProDobrani: List<Karta>,
    val talon: List<Karta>,
    val trumf: BarvaKarty?,
    val volenoZTalonu: Boolean,
    val zavazek: ZavazekMariase,
    val nasobekFlekHra: Int,
    val nasobekFlekSedma: Int,
    val naTahu: Int,
    val pocetOdpovediFleku: Int,
    val aktualniStych: List<HranaKartaStolu>,
    val cisloStychu: Int,
    val bodyKaretForhont: Int,
    val bodyKaretObrana: Int,
    val hlaskyForhont: Int,
    val hlaskyObrana: Int,
    val stychyForhont: Int,
    val stychyObrana: Int,
    val stavPosledniSedmy: Int,
    val posledniHlaska: String,
    val cisloTahu: Int
) {
    val jeBetlNeboDurch: Boolean
        get() = zavazek.jeBezTrumfu

    fun doKompaktnihoRetezce(): String {
        val pole = ArrayList<Pair<String, String>>()
        pole.add("jm" to jmenaHracu.joinToString(";"))
        pole.add("fh" to indexForhonta.toString())
        pole.add("fz" to faze.name)
        for (i in 0..2) {
            pole.add("r$i" to kartyNaRetezec(ruceHracu.getOrElse(i) { emptyList() }))
        }
        pole.add("zby" to kartyNaRetezec(odlozenyZbytekProDobrani))
        pole.add("tal" to kartyNaRetezec(talon))
        pole.add("tr" to (trumf?.name ?: ""))
        pole.add("vzt" to if (volenoZTalonu) "1" else "0")
        pole.add("zav" to zavazek.name)
        pole.add("nfh" to nasobekFlekHra.toString())
        pole.add("nfs" to nasobekFlekSedma.toString())
        pole.add("nt" to naTahu.toString())
        pole.add("pof" to pocetOdpovediFleku.toString())
        pole.add("st" to stychNaRetezec(aktualniStych))
        pole.add("cs" to cisloStychu.toString())
        pole.add("bkf" to bodyKaretForhont.toString())
        pole.add("bko" to bodyKaretObrana.toString())
        pole.add("hlf" to hlaskyForhont.toString())
        pole.add("hlo" to hlaskyObrana.toString())
        pole.add("sf" to stychyForhont.toString())
        pole.add("so" to stychyObrana.toString())
        pole.add("sps" to stavPosledniSedmy.toString())
        pole.add("hl" to posledniHlaska)
        pole.add("ct" to cisloTahu.toString())
        return serializujMapuPoli(pole)
    }

    companion object {
        fun zKompaktnihoRetezce(data: String): StavMarias? {
            if (data.isBlank()) return null
            val r = PoleStavuReader(data)
            val jmena = r.text("jm").split(";").let { s ->
                List(3) { idx -> s.getOrNull(idx) ?: "Hráč ${idx + 1}" }
            }
            val faze = FazeMariase.entries.firstOrNull { it.name == r.text("fz") } ?: return null
            val zavazek = ZavazekMariase.entries.firstOrNull { it.name == r.text("zav") } ?: ZavazekMariase.HRA
            return StavMarias(
                jmenaHracu = jmena,
                indexForhonta = r.cislo("fh", 0),
                faze = faze,
                ruceHracu = List(3) { idx -> retezecNaKarty(r.text("r$idx")) },
                odlozenyZbytekProDobrani = retezecNaKarty(r.text("zby")),
                talon = retezecNaKarty(r.text("tal")),
                trumf = BarvaKarty.dleNazvu(r.text("tr")),
                volenoZTalonu = r.pravda("vzt", false),
                zavazek = zavazek,
                nasobekFlekHra = r.cislo("nfh", 1),
                nasobekFlekSedma = r.cislo("nfs", 1),
                naTahu = r.cislo("nt", 0),
                pocetOdpovediFleku = r.cislo("pof", 0),
                aktualniStych = retezecNaStych(r.text("st")),
                cisloStychu = r.cislo("cs", 1),
                bodyKaretForhont = r.cislo("bkf", 0),
                bodyKaretObrana = r.cislo("bko", 0),
                hlaskyForhont = r.cislo("hlf", 0),
                hlaskyObrana = r.cislo("hlo", 0),
                stychyForhont = r.cislo("sf", 0),
                stychyObrana = r.cislo("so", 0),
                stavPosledniSedmy = r.cislo("sps", 0),
                posledniHlaska = r.text("hl"),
                cisloTahu = r.cislo("ct", 1)
            )
        }
    }
}

object MariasEngine {

    fun novaHra(
        jmenaHracu: List<String>,
        indexForhonta: Int = 0,
        predmichanyBalicek: List<Karta>? = null
    ): StavMarias {
        val jmena = List(3) { idx -> jmenaHracu.getOrNull(idx) ?: "Hráč ${idx + 1}" }
        val fh = indexForhonta.mod(3)
        val zadak = (fh + 1) % 3
        val prizadak = (fh + 2) % 3
        val balicek = (predmichanyBalicek ?: Karta.vytvorZamichanyBalicek()).toList()

        val ruce = MutableList(3) { emptyList<Karta>() }
        ruce[fh] = Karta.seradRuku(balicek.subList(0, 7))
        ruce[zadak] = Karta.seradRuku(balicek.subList(7, 12))
        ruce[prizadak] = Karta.seradRuku(balicek.subList(12, 17))
        val zbytek = balicek.subList(17, 32)

        return StavMarias(
            jmenaHracu = jmena,
            indexForhonta = fh,
            faze = FazeMariase.VOLBA_TRUMFU,
            ruceHracu = ruce,
            odlozenyZbytekProDobrani = zbytek,
            talon = emptyList(),
            trumf = null,
            volenoZTalonu = false,
            zavazek = ZavazekMariase.HRA,
            nasobekFlekHra = 1,
            nasobekFlekSedma = 1,
            naTahu = fh,
            pocetOdpovediFleku = 0,
            aktualniStych = emptyList(),
            cisloStychu = 1,
            bodyKaretForhont = 0,
            bodyKaretObrana = 0,
            hlaskyForhont = 0,
            hlaskyObrana = 0,
            stychyForhont = 0,
            stychyObrana = 0,
            stavPosledniSedmy = 0,
            posledniHlaska = "${jmena[fh]} je na forhontu a volí trumf ze 7 karet (nebo z talonu).",
            cisloTahu = 1
        )
    }

    fun zvolTrumfNeboHruBezTrumfu(
        stav: StavMarias,
        zvolenyTrumf: BarvaKarty?,
        zTalonu: Boolean = false,
        rovnouZavazek: ZavazekMariase = ZavazekMariase.HRA
    ): StavMarias {
        if (stav.faze != FazeMariase.VOLBA_TRUMFU) return stav
        val fh = stav.indexForhonta
        val zadak = (fh + 1) % 3
        val prizadak = (fh + 2) % 3
        val zbytek = stav.odlozenyZbytekProDobrani
        if (zbytek.size < 15) return stav

        val vyslednyTrumf: BarvaKarty?
        val popisVolby: String
        if (rovnouZavazek.jeBezTrumfu) {
            vyslednyTrumf = null
            popisVolby = "volí ${rovnouZavazek.nazev}"
        } else if (zTalonu) {
            val kartaTalonu = zbytek[0]
            vyslednyTrumf = kartaTalonu.barva
            popisVolby = "volí naslepo z talonu (${kartaTalonu.celyNazev()} -> trumf ${vyslednyTrumf.ceskyNazev})"
        } else {
            vyslednyTrumf = zvolenyTrumf ?: BarvaKarty.CERVENE
            popisVolby = "volí trumf ${vyslednyTrumf.ceskyNazev}"
        }

        val ruce = MutableList(3) { emptyList<Karta>() }
        ruce[fh] = Karta.seradRuku(
            stav.ruceHracu[fh] + zbytek.subList(0, 5),
            trumf = vyslednyTrumf,
            betlNeboDurch = rovnouZavazek.jeBezTrumfu
        )
        ruce[zadak] = Karta.seradRuku(
            stav.ruceHracu[zadak] + zbytek.subList(5, 10),
            trumf = vyslednyTrumf,
            betlNeboDurch = rovnouZavazek.jeBezTrumfu
        )
        ruce[prizadak] = Karta.seradRuku(
            stav.ruceHracu[prizadak] + zbytek.subList(10, 15),
            trumf = vyslednyTrumf,
            betlNeboDurch = rovnouZavazek.jeBezTrumfu
        )

        return stav.copy(
            faze = FazeMariase.ODKLADANI_TALONU,
            ruceHracu = ruce,
            odlozenyZbytekProDobrani = emptyList(),
            trumf = vyslednyTrumf,
            volenoZTalonu = zTalonu && !rovnouZavazek.jeBezTrumfu,
            zavazek = rovnouZavazek,
            naTahu = fh,
            posledniHlaska = "${stav.jmenaHracu[fh]} $popisVolby a odkládá 2 karty do talonu.",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun zjistiKartyPovoleneDoTalonu(stav: StavMarias, planovanyZavazek: ZavazekMariase): Set<Karta> {
        if (stav.faze != FazeMariase.ODKLADANI_TALONU) return emptySet()
        val ruka = stav.ruceHracu[stav.indexForhonta]
        if (planovanyZavazek.jeBezTrumfu) {
            return ruka.toSet()
        }
        // V trumfové Hře platí přísný zákaz odkládat do talonu desítky a esa (a při hlášené sedmě ani trumfovou sedmu)
        return ruka.filter { karta ->
            val jeBodova = karta.hodnota == HodnotaKarty.ESO || karta.hodnota == HodnotaKarty.R10
            val jeHlasenaSedma = planovanyZavazek == ZavazekMariase.HRA_A_SEDMA &&
                karta.barva == stav.trumf &&
                karta.hodnota == HodnotaKarty.R7
            !jeBodova && !jeHlasenaSedma
        }.toSet()
    }

    fun muzeHlasitSedmu(stav: StavMarias): Boolean {
        val tr = stav.trumf ?: return false
        return stav.ruceHracu[stav.indexForhonta].any { it.barva == tr && it.hodnota == HodnotaKarty.R7 }
    }

    fun potvrdTalonAZavazek(
        stav: StavMarias,
        odlozeneDveKarty: List<Karta>,
        zvolenyZavazek: ZavazekMariase
    ): StavMarias {
        if (stav.faze != FazeMariase.ODKLADANI_TALONU || odlozeneDveKarty.size != 2) return stav
        if (stav.volenoZTalonu && zvolenyZavazek.jeBezTrumfu) return stav

        val povolene = zjistiKartyPovoleneDoTalonu(stav, zvolenyZavazek)
        if (!odlozeneDveKarty.all { it in povolene }) return stav

        val fh = stav.indexForhonta
        val konecnyTrumf = if (zvolenyZavazek.jeBezTrumfu) null else stav.trumf
        if (zvolenyZavazek == ZavazekMariase.HRA_A_SEDMA) {
            val zbyvajici = stav.ruceHracu[fh].filterNot { it in odlozeneDveKarty }
            val drziTrumfSedmu = konecnyTrumf != null &&
                zbyvajici.any { it.barva == konecnyTrumf && it.hodnota == HodnotaKarty.R7 }
            if (!drziTrumfSedmu) return stav
        }

        val noveRuce = stav.ruceHracu.mapIndexed { idx, ruka ->
            val upravena = if (idx == fh) ruka.filterNot { it in odlozeneDveKarty } else ruka
            Karta.seradRuku(upravena, trumf = konecnyTrumf, betlNeboDurch = zvolenyZavazek.jeBezTrumfu)
        }

        val zadak = (fh + 1) % 3
        val popisTrumfu = if (konecnyTrumf != null) " (${konecnyTrumf.ceskyNazev})" else ""

        return stav.copy(
            faze = FazeMariase.FLEKOVANI,
            ruceHracu = noveRuce,
            talon = odlozeneDveKarty,
            trumf = konecnyTrumf,
            zavazek = zvolenyZavazek,
            naTahu = zadak,
            pocetOdpovediFleku = 0,
            posledniHlaska = "${stav.jmenaHracu[fh]} hlásí: ${zvolenyZavazek.nazev}$popisTrumfu. Obrana se vyjadřuje (Dobrý / Flek).",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun odpovezNaFlekovani(
        stav: StavMarias,
        indexHrace: Int,
        davaFlekNaHru: Boolean,
        davaFlekNaSedmu: Boolean = false
    ): StavMarias {
        if (stav.faze != FazeMariase.FLEKOVANI || stav.naTahu != indexHrace) return stav
        val fh = stav.indexForhonta
        val jmeno = stav.jmenaHracu[indexHrace]

        var novyFlekHra = stav.nasobekFlekHra
        var novyFlekSedma = stav.nasobekFlekSedma
        val akcePopisy = ArrayList<String>()

        if (davaFlekNaHru && novyFlekHra < 8) {
            novyFlekHra *= 2
            val nazevStupne = when (novyFlekHra) {
                2 -> "Flek!"
                4 -> "Re!"
                else -> "Tutti!"
            }
            akcePopisy.add("$nazevStupne (${novyFlekHra}×)")
        }
        if (davaFlekNaSedmu && stav.zavazek == ZavazekMariase.HRA_A_SEDMA && novyFlekSedma < 8) {
            novyFlekSedma *= 2
            akcePopisy.add("Flek na sedmu (${novyFlekSedma}×)")
        }

        // Obrana: nejprve Zadák (krok 0), pak Přizadák (krok 1), případně Forhont může dát Re (krok 2)
        if (indexHrace != fh) {
            val prizadak = (fh + 2) % 3
            if (stav.pocetOdpovediFleku == 0) {
                val hl = if (akcePopisy.isEmpty()) "$jmeno: Dobrý." else "$jmeno dává ${akcePopisy.joinToString(", ")}"
                return stav.copy(
                    nasobekFlekHra = novyFlekHra,
                    nasobekFlekSedma = novyFlekSedma,
                    naTahu = prizadak,
                    pocetOdpovediFleku = 1,
                    posledniHlaska = hl,
                    cisloTahu = stav.cisloTahu + 1
                )
            } else {
                val nekdoDalFlek = novyFlekHra > 1 || novyFlekSedma > 1
                val hl = if (akcePopisy.isEmpty()) "$jmeno: Dobrý." else "$jmeno dává ${akcePopisy.joinToString(", ")}"
                return if (nekdoDalFlek && novyFlekHra == 2) {
                    stav.copy(
                        nasobekFlekHra = novyFlekHra,
                        nasobekFlekSedma = novyFlekSedma,
                        naTahu = fh,
                        pocetOdpovediFleku = 2,
                        posledniHlaska = "$hl ${stav.jmenaHracu[fh]} může dát Re nebo zahájit hru.",
                        cisloTahu = stav.cisloTahu + 1
                    )
                } else {
                    stav.copy(
                        faze = FazeMariase.SEHRAVKA,
                        nasobekFlekHra = novyFlekHra,
                        nasobekFlekSedma = novyFlekSedma,
                        naTahu = fh,
                        posledniHlaska = "$hl Začíná sehrávka — vynáší ${stav.jmenaHracu[fh]}!",
                        cisloTahu = stav.cisloTahu + 1
                    )
                }
            }
        } else {
            val hl = if (akcePopisy.isEmpty()) {
                "${stav.jmenaHracu[fh]} přijímá flek. Vynáší do 1. štychu!"
            } else {
                "${stav.jmenaHracu[fh]} hlásí ${akcePopisy.joinToString(", ")}! Vynáší do 1. štychu!"
            }
            return stav.copy(
                faze = FazeMariase.SEHRAVKA,
                nasobekFlekHra = novyFlekHra,
                nasobekFlekSedma = novyFlekSedma,
                naTahu = fh,
                posledniHlaska = hl,
                cisloTahu = stav.cisloTahu + 1
            )
        }
    }

    /**
     * Striktní kontrola pravidel českého Mariáše:
     * 1. Povinnost přiznat barvu a přebít nejvyšší kartu nesené barvy (pokud nebyla trumfnuta).
     * 2. Nemá-li hráč nesenou barvu v trumfové hře, má povinnost trumfovat (a je-li ve štychu trumf, přebít ho vyšším trumfem; nemá-li vyšší trumf, musí i tak přiložit nižší trumf).
     * 3. Nemá-li ani barvu, ani trumf, může odhodit libovolnou kartu.
     * 4. V Betlu a Durchu nejsou trumfy a desítka stojí mezi devítkou a spodkem.
     */
    fun zjistiPovoleneKarty(stav: StavMarias, indexHrace: Int): Set<Karta> {
        if (stav.faze != FazeMariase.SEHRAVKA || stav.naTahu != indexHrace) {
            return emptySet()
        }
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()
        if (ruka.isEmpty()) return emptySet()

        // Při výnosu do štychu lze vynést libovolnou kartu (s ochranou hlášené trumfové sedmy před 10. štychem, pokud má jinou kartu)
        if (stav.aktualniStych.isEmpty()) {
            if (indexHrace == stav.indexForhonta &&
                stav.zavazek == ZavazekMariase.HRA_A_SEDMA &&
                stav.cisloStychu < 10 &&
                stav.trumf != null &&
                ruka.size > 1
            ) {
                val bezTrumfSedmy = ruka.filterNot { it.barva == stav.trumf && it.hodnota == HodnotaKarty.R7 }
                if (bezTrumfSedmy.isNotEmpty()) return bezTrumfSedmy.toSet()
            }
            return ruka.toSet()
        }

        val betlDurch = stav.jeBetlNeboDurch
        val trumf = if (betlDurch) null else stav.trumf
        val barvaVynosu = stav.aktualniStych.first().karta.barva

        val kartyStejneBarvy = ruka.filter { it.barva == barvaVynosu }
        val trumfnutyCiziVynos = trumf != null &&
            barvaVynosu != trumf &&
            stav.aktualniStych.any { it.karta.barva == trumf }

        // 1) Hráč má v ruce barvu výnosu -> musí ji přiznat (a pokud nebyl štych trumfnut, musí přebít)
        if (kartyStejneBarvy.isNotEmpty()) {
            if (!trumfnutyCiziVynos) {
                val maxSilaVeStychu = stav.aktualniStych
                    .filter { it.karta.barva == barvaVynosu }
                    .maxOf { it.karta.silaProMarias(betlDurch) }
                val vyssiKartyBarvy = kartyStejneBarvy.filter {
                    it.silaProMarias(betlDurch) > maxSilaVeStychu
                }
                if (vyssiKartyBarvy.isNotEmpty()) {
                    return vyssiKartyBarvy.toSet()
                }
            }
            return kartyStejneBarvy.toSet()
        }

        // 2) Hráč nemá barvu výnosu -> v trumfové hře musí trumfovat (případně přebít trumf)
        if (trumf != null && barvaVynosu != trumf) {
            val trumfyVRuce = ruka.filter { it.barva == trumf }
            if (trumfyVRuce.isNotEmpty()) {
                val trumfyVeStychu = stav.aktualniStych.filter { it.karta.barva == trumf }
                if (trumfyVeStychu.isNotEmpty()) {
                    val maxSilaTrumfu = trumfyVeStychu.maxOf { it.karta.silaProMarias(false) }
                    val vyssiTrumfy = trumfyVRuce.filter { it.silaProMarias(false) > maxSilaTrumfu }
                    if (vyssiTrumfy.isNotEmpty()) {
                        return vyssiTrumfy.toSet()
                    }
                }
                return trumfyVRuce.toSet()
            }
        }

        // 3) Hráč nemá ani barvu výnosu, ani trumf -> volný odhoz
        return ruka.toSet()
    }

    fun zahrajKartu(stav: StavMarias, indexHrace: Int, karta: Karta): StavMarias {
        val povolene = zjistiPovoleneKarty(stav, indexHrace)
        if (karta !in povolene) return stav

        val jmeno = stav.jmenaHracu[indexHrace]
        val rukaPredTahem = stav.ruceHracu[indexHrace]

        // Automatická detekce hlášky (20 nebo 40 v trumfech) při zahrání svrška či krále z páru v ruce
        var bodyHlaskyTahu = 0
        if (!stav.jeBetlNeboDurch && (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL)) {
            val parovaHodnota = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
            val maParVRuce = rukaPredTahem.any { it.barva == karta.barva && it.hodnota == parovaHodnota }
            if (maParVRuce) {
                bodyHlaskyTahu = if (karta.barva == stav.trumf) 40 else 20
            }
        }

        val noveHlaskyForhont = stav.hlaskyForhont + if (indexHrace == stav.indexForhonta) bodyHlaskyTahu else 0
        val noveHlaskyObrana = stav.hlaskyObrana + if (indexHrace != stav.indexForhonta) bodyHlaskyTahu else 0

        val novaRuka = Karta.seradRuku(
            rukaPredTahem.filterNot { it == karta },
            trumf = stav.trumf,
            betlNeboDurch = stav.jeBetlNeboDurch
        )
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }
        val novyStych = stav.aktualniStych + HranaKartaStolu(indexHrace, jmeno, karta)

        val pripisHlasky = if (bodyHlaskyTahu > 0) " (Hláška $bodyHlaskyTahu!)" else ""

        if (novyStych.size < 3) {
            val dalsiHrac = (indexHrace + 1) % 3
            return stav.copy(
                ruceHracu = noveRuce,
                aktualniStych = novyStych,
                naTahu = dalsiHrac,
                hlaskyForhont = noveHlaskyForhont,
                hlaskyObrana = noveHlaskyObrana,
                posledniHlaska = "$jmeno hraje ${karta.celyNazev()}$pripisHlasky.",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        // Vyhodnocení kompletního 3karetního štychu
        val vitezPolozka = urciVitezeStychu(novyStych, stav.trumf, stav.jeBetlNeboDurch)
        val vitezIdx = vitezPolozka.indexHrace
        val vyhravaForhont = vitezIdx == stav.indexForhonta

        val bodyVeStychu = novyStych.sumOf { it.karta.hodnota.bodyMariasSedma } +
            if (stav.cisloStychu == 10 && !stav.jeBetlNeboDurch) 10 else 0

        val noveBodyForhont = stav.bodyKaretForhont + if (vyhravaForhont) bodyVeStychu else 0
        val noveBodyObrana = stav.bodyKaretObrana + if (!vyhravaForhont) bodyVeStychu else 0
        val noveStychyForhont = stav.stychyForhont + if (vyhravaForhont) 1 else 0
        val noveStychyObrana = stav.stychyObrana + if (!vyhravaForhont) 1 else 0

        // Sledování trumfové sedmy v posledním (10.) štychu: 1 = uhrál forhont, -1 = ztratil / uhrála obrana
        var stavSedmy = stav.stavPosledniSedmy
        if (stav.cisloStychu == 10 && stav.trumf != null && !stav.jeBetlNeboDurch) {
            val trumfSedma = Karta(stav.trumf, HodnotaKarty.R7)
            val bylaTrumfSedmaVeStychu = novyStych.any { it.karta == trumfSedma }
            if (vitezPolozka.karta == trumfSedma) {
                stavSedmy = if (vyhravaForhont) 1 else -1
            } else if (bylaTrumfSedmaVeStychu || stav.zavazek == ZavazekMariase.HRA_A_SEDMA) {
                stavSedmy = -1
            }
        }

        // Okamžité ukončení při porušení Betlu nebo Durchu
        if (stav.zavazek == ZavazekMariase.BETL && vyhravaForhont) {
            return stav.copy(
                faze = FazeMariase.KONEC,
                ruceHracu = noveRuce,
                aktualniStych = novyStych,
                stychyForhont = noveStychyForhont,
                stychyObrana = noveStychyObrana,
                posledniHlaska = "${stav.jmenaHracu[stav.indexForhonta]} vzal štych — Betl je prohraný!",
                cisloTahu = stav.cisloTahu + 1
            )
        }
        if (stav.zavazek == ZavazekMariase.DURCH && !vyhravaForhont) {
            return stav.copy(
                faze = FazeMariase.KONEC,
                ruceHracu = noveRuce,
                aktualniStych = novyStych,
                stychyForhont = noveStychyForhont,
                stychyObrana = noveStychyObrana,
                posledniHlaska = "${stav.jmenaHracu[vitezIdx]} sebral štych — Durch je prohraný!",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        val jePosledni = stav.cisloStychu >= 10
        val jmenoViteze = stav.jmenaHracu[vitezIdx]

        return stav.copy(
            faze = if (jePosledni) FazeMariase.KONEC else FazeMariase.SEHRAVKA,
            ruceHracu = noveRuce,
            aktualniStych = if (jePosledni) novyStych else emptyList(),
            cisloStychu = if (jePosledni) 10 else stav.cisloStychu + 1,
            naTahu = vitezIdx,
            bodyKaretForhont = noveBodyForhont,
            bodyKaretObrana = noveBodyObrana,
            hlaskyForhont = noveHlaskyForhont,
            hlaskyObrana = noveHlaskyObrana,
            stychyForhont = noveStychyForhont,
            stychyObrana = noveStychyObrana,
            stavPosledniSedmy = stavSedmy,
            posledniHlaska = if (jePosledni) {
                "$jmenoViteze bere poslední štych! Partie vyhodnocena."
            } else {
                "$jmenoViteze bere ${stav.cisloStychu}. štych ($bodyVeStychu b.)$pripisHlasky a vynáší."
            },
            cisloTahu = stav.cisloTahu + 1
        )
    }

    private fun urciVitezeStychu(
        stych: List<HranaKartaStolu>,
        trumf: BarvaKarty?,
        betlNeboDurch: Boolean
    ): HranaKartaStolu {
        val barvaVynosu = stych.first().karta.barva
        if (!betlNeboDurch && trumf != null) {
            val trumfove = stych.filter { it.karta.barva == trumf }
            if (trumfove.isNotEmpty()) {
                return trumfove.maxBy { it.karta.silaProMarias(false) }
            }
        }
        return stych
            .filter { it.karta.barva == barvaVynosu }
            .maxBy { it.karta.silaProMarias(betlNeboDurch) }
    }

    fun provedTahBota(stav: StavMarias): StavMarias {
        val idx = stav.naTahu
        return when (stav.faze) {
            FazeMariase.VOLBA_TRUMFU -> {
                val ruka = stav.ruceHracu[idx]
                val nejlepsiBarva = BarvaKarty.entries.maxByOrNull { b ->
                    val kartyB = ruka.filter { it.barva == b }
                    val maHlasku = kartyB.any { it.hodnota == HodnotaKarty.SVRSEK } &&
                        kartyB.any { it.hodnota == HodnotaKarty.KRAL }
                    kartyB.size * 12 + kartyB.sumOf { it.silaMarias() } + if (maHlasku) 25 else 0
                } ?: BarvaKarty.CERVENE
                val pocetNejlepsi = ruka.count { it.barva == nejlepsiBarva }
                if (pocetNejlepsi >= 3) {
                    zvolTrumfNeboHruBezTrumfu(stav, nejlepsiBarva, zTalonu = false)
                } else {
                    zvolTrumfNeboHruBezTrumfu(stav, null, zTalonu = true)
                }
            }

            FazeMariase.ODKLADANI_TALONU -> {
                val povolene = zjistiKartyPovoleneDoTalonu(stav, ZavazekMariase.HRA).toList()
                val kOdlozeni = povolene
                    .sortedWith(
                        compareBy<Karta>(
                            { it.barva == stav.trumf },
                            { it.hodnota.silaMarias }
                        )
                    )
                    .take(2)
                if (kOdlozeni.size == 2) {
                    potvrdTalonAZavazek(stav, kOdlozeni, ZavazekMariase.HRA)
                } else {
                    stav
                }
            }

            FazeMariase.FLEKOVANI -> {
                val ruka = stav.ruceHracu[idx]
                val silneTrumfy = stav.trumf != null &&
                    ruka.count { it.barva == stav.trumf } >= 4 &&
                    ruka.count { it.hodnota == HodnotaKarty.ESO } >= 2
                val daFlek = idx != stav.indexForhonta && stav.nasobekFlekHra == 1 && silneTrumfy
                odpovezNaFlekovani(stav, idx, davaFlekNaHru = daFlek, davaFlekNaSedmu = false)
            }

            FazeMariase.SEHRAVKA -> {
                val povolene = zjistiPovoleneKarty(stav, idx).toList()
                if (povolene.isEmpty()) return stav
                val vybrana = if (stav.jeBetlNeboDurch && stav.zavazek == ZavazekMariase.BETL) {
                    povolene.minBy { it.silaProMarias(true) }
                } else if (stav.aktualniStych.isEmpty()) {
                    // Forhont vytahuje trumfy nebo esa, obrana vynáší silné netrumfové karty
                    if (idx == stav.indexForhonta) {
                        povolene.maxBy {
                            (if (it.barva == stav.trumf) 20 else 0) + it.silaProMarias(stav.jeBetlNeboDurch)
                        }
                    } else {
                        povolene.filter { it.barva != stav.trumf }
                            .maxByOrNull { it.silaProMarias(stav.jeBetlNeboDurch) }
                            ?: povolene.first()
                    }
                } else {
                    val prozatimniVitez = urciVitezeStychu(stav.aktualniStych, stav.trumf, stav.jeBetlNeboDurch)
                    val bereSpoluhracObrany = idx != stav.indexForhonta && prozatimniVitez.indexHrace != stav.indexForhonta
                    if (bereSpoluhracObrany) {
                        // Spoluhráč v obraně bere štych -> přilož mu desítku nebo eso!
                        povolene.maxBy { it.hodnota.bodyMariasSedma * 10 - it.silaProMarias(false) }
                    } else {
                        povolene.minBy { it.silaProMarias(stav.jeBetlNeboDurch) }
                    }
                }
                zahrajKartu(stav, idx, vybrana)
            }

            FazeMariase.KONEC -> stav
        }
    }

    private fun Karta.silaMarias(): Int = hodnota.silaMarias

    fun vyhodnotVysledek(stav: StavMarias): VysledekPartie {
        val fh = stav.indexForhonta
        val jmenoFh = stav.jmenaHracu[fh]
        val cerveneDvojnasobek = if (!stav.jeBetlNeboDurch && stav.trumf == BarvaKarty.CERVENE) 2 else 1

        val polozky = ArrayList<PolozkaVyuctovani>()
        val cistyZiskForhontaOdJednohoObrance: Int
        val nadpis: String
        val podnadpis: String

        if (stav.zavazek == ZavazekMariase.BETL) {
            val splneno = stav.stychyForhont == 0
            val sazba = 5 * stav.nasobekFlekHra
            cistyZiskForhontaOdJednohoObrance = if (splneno) sazba else -sazba
            nadpis = if (splneno) "$jmenoFh uhrál čistý Betl!" else "$jmenoFh prohrál Betl!"
            podnadpis = "Závazek Betl (${stav.nasobekFlekHra}×)"
            polozky.add(PolozkaVyuctovani("Závazek", "Betl (${stav.stychyForhont} štychů forhonta)", true))
            polozky.add(PolozkaVyuctovani("Násobek flekování", "${stav.nasobekFlekHra}×", false))
        } else if (stav.zavazek == ZavazekMariase.DURCH) {
            val splneno = stav.stychyObrana == 0 && stav.stychyForhont == 10
            val sazba = 10 * stav.nasobekFlekHra
            cistyZiskForhontaOdJednohoObrance = if (splneno) sazba else -sazba
            nadpis = if (splneno) "$jmenoFh uhrál Durch!" else "$jmenoFh prohrál Durch!"
            podnadpis = "Závazek Durch (${stav.nasobekFlekHra}×)"
            polozky.add(PolozkaVyuctovani("Závazek", "Durch (${stav.stychyForhont}/10 štychů)", true))
            polozky.add(PolozkaVyuctovani("Násobek flekování", "${stav.nasobekFlekHra}×", false))
        } else {
            val celkemForhont = stav.bodyKaretForhont + stav.hlaskyForhont
            val celkemObrana = stav.bodyKaretObrana + stav.hlaskyObrana
            val forhontVyhralHru = celkemForhont > celkemObrana

            val bodyVitezStrany = if (forhontVyhralHru) celkemForhont else celkemObrana
            val tichaStovkaBonus = if (bodyVitezStrany >= 100) {
                2 + ((bodyVitezStrany - 100) / 10)
            } else {
                0
            }

            val zakladHry = (1 + tichaStovkaBonus) * stav.nasobekFlekHra * cerveneDvojnasobek
            val slozkaHry = if (forhontVyhralHru) zakladHry else -zakladHry

            val slozkaSedmy = when {
                stav.zavazek == ZavazekMariase.HRA_A_SEDMA -> {
                    val sazbaHlasene = 4 * stav.nasobekFlekSedma * cerveneDvojnasobek
                    if (stav.stavPosledniSedmy == 1) sazbaHlasene else -sazbaHlasene
                }
                stav.stavPosledniSedmy == 1 -> 2 * cerveneDvojnasobek
                stav.stavPosledniSedmy == -1 -> -2 * cerveneDvojnasobek
                else -> 0
            }

            cistyZiskForhontaOdJednohoObrance = slozkaHry + slozkaSedmy

            nadpis = if (cistyZiskForhontaOdJednohoObrance > 0) {
                "$jmenoFh vyhrává na forhontu!"
            } else if (cistyZiskForhontaOdJednohoObrance < 0) {
                "Obrana porazila forhonta ($jmenoFh)!"
            } else {
                "Smírné vyúčtování hry a sedmy"
            }
            podnadpis = "Trumfy: ${stav.trumf?.ceskyNazev ?: "-"} | Poměr bodů $celkemForhont : $celkemObrana"

            polozky.add(
                PolozkaVyuctovani(
                    "Forhont ($jmenoFh)",
                    "${stav.bodyKaretForhont} v kartách + ${stav.hlaskyForhont} hlášky = $celkemForhont b.",
                    forhontVyhralHru
                )
            )
            polozky.add(
                PolozkaVyuctovani(
                    "Obrana dohromady",
                    "${stav.bodyKaretObrana} v kartách + ${stav.hlaskyObrana} hlášky = $celkemObrana b.",
                    !forhontVyhralHru
                )
            )
            if (cerveneDvojnasobek == 2) {
                polozky.add(PolozkaVyuctovani("Červené trumfy", "2× dvojnásobek sazby", true))
            }
            if (tichaStovkaBonus > 0) {
                polozky.add(PolozkaVyuctovani("Uhraná stovka ($bodyVitezStrany b.)", "+$tichaStovkaBonus body k základu", true))
            }
            if (slozkaSedmy != 0) {
                val popisSedmy = if (stav.zavazek == ZavazekMariase.HRA_A_SEDMA) "Hlášená sedma" else "Tichá sedma"
                polozky.add(
                    PolozkaVyuctovani(
                        popisSedmy,
                        if (slozkaSedmy > 0) "+$slozkaSedmy b. od hráče" else "$slozkaSedmy b. každému obránci",
                        true
                    )
                )
            }
        }

        val celkemForhontZapas = cistyZiskForhontaOdJednohoObrance * 2
        polozky.add(
            PolozkaVyuctovani(
                "Výsledná bilance forhonta",
                if (celkemForhontZapas >= 0) "+$celkemForhontZapas b." else "$celkemForhontZapas b.",
                jeZvyraznena = true
            )
        )

        val zmenyHracu = List(3) { i ->
            if (i == fh) {
                val jeVit = celkemForhontZapas > 0
                ZmenaBoduHrace(
                    indexHrace = i,
                    jmenoHrace = stav.jmenaHracu[i],
                    bodyZapasu = celkemForhontZapas,
                    zmenaElo = (cistyZiskForhontaOdJednohoObrance * 4).coerceIn(-32, 36),
                    jeVitez = jeVit
                )
            } else {
                val bodyObrance = -cistyZiskForhontaOdJednohoObrance
                val jeVit = bodyObrance > 0
                ZmenaBoduHrace(
                    indexHrace = i,
                    jmenoHrace = stav.jmenaHracu[i],
                    bodyZapasu = bodyObrance,
                    zmenaElo = (bodyObrance * 3).coerceIn(-24, 24),
                    jeVitez = jeVit
                )
            }
        }

        return VysledekPartie(
            typHry = TypHry.MARIAS,
            nadpis = nadpis,
            podnadpis = podnadpis,
            polozky = polozky,
            hraci = zmenyHracu
        )
    }
}
