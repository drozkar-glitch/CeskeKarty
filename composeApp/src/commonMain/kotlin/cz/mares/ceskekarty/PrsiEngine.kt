package cz.mares.ceskekarty

data class StavPrsi(
    val pocetHracu: Int,
    val jmenaHracu: List<String>,
    val ruceHracu: List<List<Karta>>,
    val lizaciBalicek: List<Karta>,
    val odhazovaciBalicek: List<Karta>,
    val naTahu: Int,
    val zvolenaBarvaSvrskem: BarvaKarty?,
    val aktivniSedmyPocet: Int,
    val aktivniEsoStoji: Boolean,
    val cekaNaVolbuBarvy: Boolean,
    val posledniHlaska: String,
    val jeKonec: Boolean,
    val indexViteze: Int,
    val cisloTahu: Int
) {
    val vrchniKarta: Karta
        get() = odhazovaciBalicek.last()

    val aktualniPlatnaBarva: BarvaKarty
        get() = zvolenaBarvaSvrskem ?: vrchniKarta.barva

    fun doKompaktnihoRetezce(): String {
        val pole = ArrayList<Pair<String, String>>()
        pole.add("ph" to pocetHracu.toString())
        pole.add("jm" to jmenaHracu.joinToString(";"))
        for (i in 0 until pocetHracu) {
            pole.add("r$i" to kartyNaRetezec(ruceHracu.getOrElse(i) { emptyList() }))
        }
        pole.add("liz" to kartyNaRetezec(lizaciBalicek))
        pole.add("odh" to kartyNaRetezec(odhazovaciBalicek))
        pole.add("nt" to naTahu.toString())
        pole.add("zbs" to (zvolenaBarvaSvrskem?.name ?: ""))
        pole.add("sed" to aktivniSedmyPocet.toString())
        pole.add("eso" to if (aktivniEsoStoji) "1" else "0")
        pole.add("cvb" to if (cekaNaVolbuBarvy) "1" else "0")
        pole.add("hl" to posledniHlaska)
        pole.add("kon" to if (jeKonec) "1" else "0")
        pole.add("vit" to indexViteze.toString())
        pole.add("ct" to cisloTahu.toString())
        return serializujMapuPoli(pole)
    }

    companion object {
        fun zKompaktnihoRetezce(data: String): StavPrsi? {
            if (data.isBlank()) return null
            val r = PoleStavuReader(data)
            val pocet = r.cislo("ph", 0)
            if (pocet !in 2..4) return null
            val jmena = r.text("jm").split(";").let { seznam ->
                List(pocet) { idx -> seznam.getOrNull(idx) ?: "Hráč ${idx + 1}" }
            }
            val ruce = List(pocet) { idx ->
                retezecNaKarty(r.text("r$idx"))
            }
            val odh = retezecNaKarty(r.text("odh"))
            if (odh.isEmpty()) return null
            return StavPrsi(
                pocetHracu = pocet,
                jmenaHracu = jmena,
                ruceHracu = ruce,
                lizaciBalicek = retezecNaKarty(r.text("liz")),
                odhazovaciBalicek = odh,
                naTahu = r.cislo("nt", 0),
                zvolenaBarvaSvrskem = BarvaKarty.dleNazvu(r.text("zbs")),
                aktivniSedmyPocet = r.cislo("sed", 0),
                aktivniEsoStoji = r.pravda("eso", false),
                cekaNaVolbuBarvy = r.pravda("cvb", false),
                posledniHlaska = r.text("hl"),
                jeKonec = r.pravda("kon", false),
                indexViteze = r.cislo("vit", -1),
                cisloTahu = r.cislo("ct", 1)
            )
        }
    }
}

object PrsiEngine {

    private fun sklonujKartyVRuce(pocet: Int): String {
        return when {
            pocet == 1 -> "1 karta v ruce"
            pocet in 2..4 -> "$pocet karty v ruce"
            else -> "$pocet karet v ruce"
        }
    }

    private fun sklonujBereSeKaret(pocet: Int): String {
        return when {
            pocet in 2..4 -> "berou se $pocet karty"
            else -> "bere se $pocet karet"
        }
    }

    private fun sklonujKartyAkuzativ(pocet: Int): String {
        return when {
            pocet == 1 -> "1 kartu"
            pocet in 2..4 -> "$pocet karty"
            else -> "$pocet karet"
        }
    }

    fun novaHra(
        jmenaHracu: List<String>,
        predmichanyBalicek: List<Karta>? = null,
        zacinajiciHrac: Int = 0
    ): StavPrsi {
        val pocet = jmenaHracu.size.coerceIn(2, 4)
        val balicek = (predmichanyBalicek ?: Karta.vytvorZamichanyBalicek()).toMutableList()
        val ruce = List(pocet) {
            val ruka = ArrayList<Karta>(4)
            repeat(4) {
                if (balicek.isNotEmpty()) ruka.add(balicek.removeAt(0))
            }
            Karta.seradRuku(ruka)
        }
        val prvniNaStole = balicek.removeAt(0)
        val startIndex = zacinajiciHrac.mod(pocet)
        return StavPrsi(
            pocetHracu = pocet,
            jmenaHracu = jmenaHracu.take(pocet),
            ruceHracu = ruce,
            lizaciBalicek = balicek,
            odhazovaciBalicek = listOf(prvniNaStole),
            naTahu = startIndex,
            zvolenaBarvaSvrskem = null,
            aktivniSedmyPocet = 0,
            aktivniEsoStoji = false,
            cekaNaVolbuBarvy = false,
            posledniHlaska = "Rozdáno! Začíná ${jmenaHracu[startIndex]} na kartu ${prvniNaStole.celyNazev()}.",
            jeKonec = false,
            indexViteze = -1,
            cisloTahu = 1
        )
    }

    fun zjistiPovoleneKarty(stav: StavPrsi, indexHrace: Int): Set<Karta> {
        if (stav.jeKonec || stav.naTahu != indexHrace || stav.cekaNaVolbuBarvy) {
            return emptySet()
        }
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()

        if (stav.aktivniSedmyPocet > 0) {
            return ruka.filter { it.hodnota == HodnotaKarty.R7 }.toSet()
        }
        if (stav.aktivniEsoStoji) {
            return ruka.filter { it.hodnota == HodnotaKarty.ESO }.toSet()
        }

        val platnaBarva = stav.aktualniPlatnaBarva
        val vrchniHodnota = stav.vrchniKarta.hodnota
        return ruka.filter { karta ->
            karta.hodnota == HodnotaKarty.SVRSEK ||
                karta.barva == platnaBarva ||
                karta.hodnota == vrchniHodnota
        }.toSet()
    }

    fun zahrajKartu(
        stav: StavPrsi,
        indexHrace: Int,
        karta: Karta,
        zvolenaBarva: BarvaKarty? = null
    ): StavPrsi {
        val povolene = zjistiPovoleneKarty(stav, indexHrace)
        if (karta !in povolene) return stav

        val jmeno = stav.jmenaHracu[indexHrace]
        val novaRuka = Karta.seradRuku(stav.ruceHracu[indexHrace].filterNot { it == karta })
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }
        val novyOdhazovaci = stav.odhazovaciBalicek + karta

        if (novaRuka.isEmpty()) {
            return stav.copy(
                ruceHracu = noveRuce,
                odhazovaciBalicek = novyOdhazovaci,
                zvolenaBarvaSvrskem = zvolenaBarva,
                aktivniSedmyPocet = 0,
                aktivniEsoStoji = false,
                cekaNaVolbuBarvy = false,
                posledniHlaska = "$jmeno odhazuje poslední kartu (${karta.celyNazev()}) a vyhrává!",
                jeKonec = true,
                indexViteze = indexHrace,
                cisloTahu = stav.cisloTahu + 1
            )
        }

        if (karta.hodnota == HodnotaKarty.SVRSEK && zvolenaBarva == null) {
            return stav.copy(
                ruceHracu = noveRuce,
                odhazovaciBalicek = novyOdhazovaci,
                cekaNaVolbuBarvy = true,
                posledniHlaska = "$jmeno vynesl svrška a volí novou barvu…"
            )
        }

        val dalsiHrac = (indexHrace + 1) % stav.pocetHracu
        val noveSedmy = if (karta.hodnota == HodnotaKarty.R7) stav.aktivniSedmyPocet + 1 else 0
        val noveEso = karta.hodnota == HodnotaKarty.ESO
        val barvaSvrskem = if (karta.hodnota == HodnotaKarty.SVRSEK) zvolenaBarva else null

        val hlaska = when {
            karta.hodnota == HodnotaKarty.R7 ->
                "$jmeno hraje ${karta.celyNazev()} — ${sklonujBereSeKaret(noveSedmy * 2)}!"
            karta.hodnota == HodnotaKarty.ESO ->
                "$jmeno hraje ${karta.celyNazev()} — další stojí!"
            karta.hodnota == HodnotaKarty.SVRSEK && barvaSvrskem != null ->
                "$jmeno mění svrškem barvu na ${barvaSvrskem.ceskyNazev}!"
            else ->
                "$jmeno hraje ${karta.celyNazev()}."
        }

        return stav.copy(
            ruceHracu = noveRuce,
            odhazovaciBalicek = novyOdhazovaci,
            naTahu = dalsiHrac,
            zvolenaBarvaSvrskem = barvaSvrskem,
            aktivniSedmyPocet = noveSedmy,
            aktivniEsoStoji = noveEso,
            cekaNaVolbuBarvy = false,
            posledniHlaska = hlaska,
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun potvrdVolbuBarvySvrskem(stav: StavPrsi, barva: BarvaKarty): StavPrsi {
        if (!stav.cekaNaVolbuBarvy || stav.jeKonec) return stav
        val jmeno = stav.jmenaHracu[stav.naTahu]
        val dalsiHrac = (stav.naTahu + 1) % stav.pocetHracu
        return stav.copy(
            naTahu = dalsiHrac,
            zvolenaBarvaSvrskem = barva,
            cekaNaVolbuBarvy = false,
            posledniHlaska = "$jmeno mění barvu na ${barva.ceskyNazev}!",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun lizniNeboStuj(stav: StavPrsi, indexHrace: Int): StavPrsi {
        if (stav.jeKonec || stav.naTahu != indexHrace || stav.cekaNaVolbuBarvy) return stav
        val jmeno = stav.jmenaHracu[indexHrace]
        val dalsiHrac = (indexHrace + 1) % stav.pocetHracu

        if (stav.aktivniEsoStoji) {
            return stav.copy(
                naTahu = dalsiHrac,
                aktivniEsoStoji = false,
                posledniHlaska = "$jmeno stojí jedno kolo.",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        val kolikLiznout = if (stav.aktivniSedmyPocet > 0) stav.aktivniSedmyPocet * 2 else 1
        val lizaci = stav.lizaciBalicek.toMutableList()
        var odhazovaci = stav.odhazovaciBalicek.toMutableList()
        val dobrane = ArrayList<Karta>(kolikLiznout)

        repeat(kolikLiznout) {
            if (lizaci.isEmpty() && odhazovaci.size > 1) {
                val vrchni = odhazovaci.removeAt(odhazovaci.lastIndex)
                val kZamichani = odhazovaci.shuffled()
                lizaci.addAll(kZamichani)
                odhazovaci = mutableListOf(vrchni)
            }
            if (lizaci.isNotEmpty()) {
                dobrane.add(lizaci.removeAt(0))
            }
        }

        val novaRuka = Karta.seradRuku(stav.ruceHracu[indexHrace] + dobrane)
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }
        val hlaska = if (stav.aktivniSedmyPocet > 0) {
            "$jmeno líže ${sklonujKartyAkuzativ(dobrane.size)} za sedmičku."
        } else {
            "$jmeno si lízl kartu z balíčku."
        }

        return stav.copy(
            ruceHracu = noveRuce,
            lizaciBalicek = lizaci,
            odhazovaciBalicek = odhazovaci,
            naTahu = dalsiHrac,
            aktivniSedmyPocet = 0,
            posledniHlaska = hlaska,
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun provedTahBota(stav: StavPrsi): StavPrsi {
        if (stav.jeKonec) return stav
        val idx = stav.naTahu
        val ruka = stav.ruceHracu[idx]

        if (stav.cekaNaVolbuBarvy) {
            val nejcastejsiBarva = BarvaKarty.entries.maxByOrNull { b ->
                ruka.count { it.barva == b && it.hodnota != HodnotaKarty.SVRSEK }
            } ?: BarvaKarty.CERVENE
            return potvrdVolbuBarvySvrskem(stav, nejcastejsiBarva)
        }

        val povolene = zjistiPovoleneKarty(stav, idx)
        if (povolene.isEmpty()) {
            return lizniNeboStuj(stav, idx)
        }

        val bezSvrsku = povolene.filter { it.hodnota != HodnotaKarty.SVRSEK }
        val vybrana = if (bezSvrsku.isNotEmpty()) {
            bezSvrsku.maxByOrNull { karta ->
                val pocetBarvyVRuce = ruka.count { it.barva == karta.barva }
                val bonusUtoku = if (karta.hodnota == HodnotaKarty.R7 || karta.hodnota == HodnotaKarty.ESO) 3 else 0
                pocetBarvyVRuce * 2 + bonusUtoku
            } ?: bezSvrsku.first()
        } else {
            povolene.first()
        }

        return if (vybrana.hodnota == HodnotaKarty.SVRSEK) {
            val zbytek = ruka.filterNot { it == vybrana }
            val cilovaBarva = BarvaKarty.entries.maxByOrNull { b ->
                zbytek.count { it.barva == b }
            } ?: BarvaKarty.CERVENE
            zahrajKartu(stav, idx, vybrana, cilovaBarva)
        } else {
            zahrajKartu(stav, idx, vybrana)
        }
    }

    fun vyhodnotVysledek(stav: StavPrsi): VysledekPartie {
        val vitez = stav.indexViteze.coerceAtLeast(0)
        val jmenoViteze = stav.jmenaHracu.getOrElse(vitez) { "Vítěz" }

        val trestneBody: List<Int> = stav.ruceHracu.map { ruka ->
            var soucetRuky = 0
            for (karta in ruka) {
                val bodyKarty: Int = when (karta.hodnota) {
                    HodnotaKarty.SVRSEK -> 20
                    HodnotaKarty.R7 -> 15
                    HodnotaKarty.ESO -> 11
                    HodnotaKarty.R10, HodnotaKarty.KRAL -> 10
                    else -> 5
                }
                soucetRuky += bodyKarty
            }
            soucetRuky
        }
        val celkemOdSouperu: Int = trestneBody.fold(0) { acc, hodnota -> acc + hodnota }

        val polozky = ArrayList<PolozkaVyuctovani>()
        polozky.add(PolozkaVyuctovani("Vítěz partie", jmenoViteze, jeZvyraznena = true))
        for (i in 0 until stav.pocetHracu) {
            val pocetKaret = stav.ruceHracu[i].size
            polozky.add(
                PolozkaVyuctovani(
                    popis = "${stav.jmenaHracu[i]} (${sklonujKartyVRuce(pocetKaret)})",
                    hodnotaText = if (i == vitez) "+$celkemOdSouperu b." else "-${trestneBody[i]} b.",
                    jeZvyraznena = i == vitez
                )
            )
        }

        val zmenyHracu = List(stav.pocetHracu) { i ->
            val jeVit = i == vitez
            ZmenaBoduHrace(
                indexHrace = i,
                jmenoHrace = stav.jmenaHracu[i],
                bodyZapasu = if (jeVit) celkemOdSouperu else -trestneBody[i],
                zmenaElo = if (jeVit) 16 + (stav.pocetHracu - 2) * 5 else -10,
                jeVitez = jeVit
            )
        }

        return VysledekPartie(
            typHry = TypHry.PRSI,
            nadpis = "Vítězí $jmenoViteze!",
            podnadpis = "Automatické vyúčtování zbylých karet v rukou soupeřů",
            polozky = polozky,
            hraci = zmenyHracu
        )
    }
}
