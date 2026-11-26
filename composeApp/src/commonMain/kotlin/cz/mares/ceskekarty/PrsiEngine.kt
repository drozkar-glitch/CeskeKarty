package cz.mares.ceskekarty

internal const val PRSI_BIT_BEZ_SVRSKU = 1 shl 4
internal const val PRSI_BIT_BEZ_ESA = 1 shl 6

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
    val cisloTahu: Int,
    val variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA,
    val poradiDohrani: List<Int> = emptyList(),
    val ziviHraci: List<Boolean> = List(pocetHracu) { it == 0 },
    val chybejiciBarvyMaska: List<Int> = List(pocetHracu) { 0 }
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
        pole.add("vp" to variantaPravidel.kod)
        pole.add("pd" to poradiDohrani.joinToString(","))
        pole.add("zh" to ziviHraci.take(pocetHracu).joinToString(",") { if (it) "1" else "0" })
        pole.add("cbm" to chybejiciBarvyMaska.take(pocetHracu).joinToString(","))
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
            val pdText = r.text("pd")
            val nactenePoradi = if (pdText.isBlank()) {
                emptyList()
            } else {
                pdText.split(",").mapNotNull { it.toIntOrNull()?.takeIf { idx -> idx in 0 until pocet } }.distinct()
            }
            val zhText = r.text("zh")
            val nacteniZivi = if (zhText.isBlank()) {
                List(pocet) { idx -> idx == 0 }
            } else {
                val casti = zhText.split(",")
                List(pocet) { idx -> casti.getOrNull(idx) == "1" }
            }
            val cbmText = r.text("cbm")
            val nacteneChybejici = if (cbmText.isBlank()) {
                List(pocet) { 0 }
            } else {
                val casti = cbmText.split(",")
                List(pocet) { idx -> casti.getOrNull(idx)?.toIntOrNull() ?: 0 }
            }
            return StavPrsi(
                pocetHracu = pocet,
                jmenaHracu = jmena,
                ruceHracu = ruce,
                lizaciBalicek = retezecNaKarty(r.text("liz")),
                odhazovaciBalicek = odh,
                naTahu = r.cislo("nt", 0).mod(pocet),
                zvolenaBarvaSvrskem = BarvaKarty.dleNazvu(r.text("zbs")),
                aktivniSedmyPocet = r.cislo("sed", 0),
                aktivniEsoStoji = r.pravda("eso", false),
                cekaNaVolbuBarvy = r.pravda("cvb", false),
                posledniHlaska = r.text("hl"),
                jeKonec = r.pravda("kon", false),
                indexViteze = r.cislo("vit", -1),
                cisloTahu = r.cislo("ct", 1),
                variantaPravidel = VariantaPravidel.dleKodu(r.text("vp", VariantaPravidel.HOSPODSKA.kod)),
                poradiDohrani = nactenePoradi,
                ziviHraci = nacteniZivi,
                chybejiciBarvyMaska = nacteneChybejici
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

    internal fun trestnaHodnotaKartyPrsi(hodnota: HodnotaKarty): Int {
        return when (hodnota) {
            HodnotaKarty.SVRSEK -> 20
            HodnotaKarty.R7 -> 15
            HodnotaKarty.ESO -> 11
            HodnotaKarty.R10, HodnotaKarty.KRAL -> 10
            else -> 5
        }
    }

    internal fun najdiDalsihoAktivnihoHrace(
        odIndexu: Int,
        pocetHracu: Int,
        ruce: List<List<Karta>>,
        poradiDohrani: List<Int>
    ): Int {
        for (krok in 1..pocetHracu) {
            val idx = (odIndexu + krok) % pocetHracu
            if (idx !in poradiDohrani && ruce.getOrNull(idx)?.isNotEmpty() == true) {
                return idx
            }
        }
        return odIndexu
    }

    internal fun maPokracovatDohravani(
        variantaPravidel: VariantaPravidel,
        pocetHracu: Int,
        ruce: List<List<Karta>>,
        poradiDohrani: List<Int>,
        ziviHraci: List<Boolean>
    ): Boolean {
        if (variantaPravidel != VariantaPravidel.HOSPODSKA || pocetHracu <= 2) {
            return false
        }
        val zbyliHraci = (0 until pocetHracu).filter { idx ->
            idx !in poradiDohrani && ruce.getOrNull(idx)?.isNotEmpty() == true
        }
        if (zbyliHraci.size < 2) {
            return false
        }
        return zbyliHraci.any { idx -> ziviHraci.getOrElse(idx) { false } }
    }

    fun novaHra(
        jmenaHracu: List<String>,
        predmichanyBalicek: List<Karta>? = null,
        zacinajiciHrac: Int = 0,
        variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA,
        ziviHraci: List<Boolean>? = null
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
        val seznamZivych = List(pocet) { idx -> ziviHraci?.getOrNull(idx) ?: (idx == 0) }

        val uvodniSedmy = if (variantaPravidel == VariantaPravidel.HOSPODSKA && prvniNaStole.hodnota == HodnotaKarty.R7) {
            1
        } else {
            0
        }
        val uvodniEso = variantaPravidel == VariantaPravidel.HOSPODSKA && prvniNaStole.hodnota == HodnotaKarty.ESO

        val uvodniHlaska = when {
            uvodniSedmy > 0 ->
                "Rozdáno (hospodská pravidla)! Na stole leží ${prvniNaStole.celyNazev()} — začíná ${jmenaHracu[startIndex]}, berou se 2 karty!"
            uvodniEso ->
                "Rozdáno (hospodská pravidla)! Na stole leží ${prvniNaStole.celyNazev()} — začíná ${jmenaHracu[startIndex]}, stojí 1 kolo!"
            else ->
                "Rozdáno (${variantaPravidel.kratkyNazev.lowercase()} pravidla)! Začíná ${jmenaHracu[startIndex]} na kartu ${prvniNaStole.celyNazev()}."
        }

        return StavPrsi(
            pocetHracu = pocet,
            jmenaHracu = jmenaHracu.take(pocet),
            ruceHracu = ruce,
            lizaciBalicek = balicek,
            odhazovaciBalicek = listOf(prvniNaStole),
            naTahu = startIndex,
            zvolenaBarvaSvrskem = null,
            aktivniSedmyPocet = uvodniSedmy,
            aktivniEsoStoji = uvodniEso,
            cekaNaVolbuBarvy = false,
            posledniHlaska = uvodniHlaska,
            jeKonec = false,
            indexViteze = -1,
            cisloTahu = 1,
            variantaPravidel = variantaPravidel,
            poradiDohrani = emptyList(),
            ziviHraci = seznamZivych,
            chybejiciBarvyMaska = List(pocet) { 0 }
        )
    }

    fun zjistiPovoleneKarty(stav: StavPrsi, indexHrace: Int): Set<Karta> {
        if (stav.jeKonec || stav.naTahu != indexHrace || stav.cekaNaVolbuBarvy || indexHrace in stav.poradiDohrani) {
            return emptySet()
        }
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()
        if (ruka.isEmpty()) return emptySet()

        if (stav.aktivniSedmyPocet > 0) {
            return ruka.filter { it.hodnota == HodnotaKarty.R7 }.toSet()
        }
        if (stav.aktivniEsoStoji) {
            return ruka.filter { karta ->
                karta.hodnota == HodnotaKarty.ESO ||
                    (stav.variantaPravidel == VariantaPravidel.HOSPODSKA &&
                        karta.barva == BarvaKarty.CERVENE &&
                        karta.hodnota == HodnotaKarty.R7)
            }.toSet()
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

        val hranaBarvaProMasku = if (karta.hodnota == HodnotaKarty.SVRSEK) zvolenaBarva else karta.barva
        val upraveneMasky = stav.chybejiciBarvyMaska.mapIndexed { idx, maska ->
            if (idx != indexHrace) {
                maska
            } else {
                var m = maska
                if (hranaBarvaProMasku != null) m = m and (1 shl hranaBarvaProMasku.ordinal).inv()
                if (karta.hodnota == HodnotaKarty.SVRSEK) m = m and PRSI_BIT_BEZ_SVRSKU.inv()
                if (karta.hodnota == HodnotaKarty.ESO) m = m and PRSI_BIT_BEZ_ESA.inv()
                m
            }
        }

        if (novaRuka.isEmpty()) {
            val novePoradi = if (indexHrace in stav.poradiDohrani) {
                stav.poradiDohrani
            } else {
                stav.poradiDohrani + indexHrace
            }
            val prvniVitez = novePoradi.first()
            val umisteni = novePoradi.size
            val pokracovat = maPokracovatDohravani(
                variantaPravidel = stav.variantaPravidel,
                pocetHracu = stav.pocetHracu,
                ruce = noveRuce,
                poradiDohrani = novePoradi,
                ziviHraci = stav.ziviHraci
            )

            if (!pokracovat) {
                val zaverecnaBarva = if (karta.hodnota == HodnotaKarty.SVRSEK) {
                    zvolenaBarva ?: karta.barva
                } else {
                    null
                }
                val textKonce = if (umisteni == 1) {
                    "$jmeno odhazuje poslední kartu (${karta.celyNazev()}) a vyhrává!"
                } else {
                    "$jmeno dohrává na $umisteni. místě (${karta.celyNazev()}) — konec partie!"
                }
                return stav.copy(
                    ruceHracu = noveRuce,
                    odhazovaciBalicek = novyOdhazovaci,
                    zvolenaBarvaSvrskem = zaverecnaBarva,
                    aktivniSedmyPocet = 0,
                    aktivniEsoStoji = false,
                    cekaNaVolbuBarvy = false,
                    posledniHlaska = textKonce,
                    jeKonec = true,
                    indexViteze = prvniVitez,
                    cisloTahu = stav.cisloTahu + 1,
                    poradiDohrani = novePoradi,
                    chybejiciBarvyMaska = upraveneMasky
                )
            }

            if (karta.hodnota == HodnotaKarty.SVRSEK && zvolenaBarva == null) {
                return stav.copy(
                    ruceHracu = noveRuce,
                    odhazovaciBalicek = novyOdhazovaci,
                    aktivniSedmyPocet = 0,
                    aktivniEsoStoji = false,
                    cekaNaVolbuBarvy = true,
                    indexViteze = prvniVitez,
                    poradiDohrani = novePoradi,
                    chybejiciBarvyMaska = upraveneMasky,
                    posledniHlaska = "$jmeno bere $umisteni. místo svrškem a volí barvu pro dohrávku…"
                )
            }

            val dalsiAktivni = najdiDalsihoAktivnihoHrace(indexHrace, stav.pocetHracu, noveRuce, novePoradi)
            val noveSedmyDohrani = if (karta.hodnota == HodnotaKarty.R7) stav.aktivniSedmyPocet + 1 else 0
            val noveEsoDohrani = karta.hodnota == HodnotaKarty.ESO
            val barvaSvrskemDohrani = if (karta.hodnota == HodnotaKarty.SVRSEK) zvolenaBarva else null

            val dovetekDohrani = when {
                noveSedmyDohrani > 0 -> " — ${sklonujBereSeKaret(noveSedmyDohrani * 2)}, dohrává ${stav.jmenaHracu[dalsiAktivni]}!"
                noveEsoDohrani -> " — ${stav.jmenaHracu[dalsiAktivni]} stojí, dohrává se dál!"
                barvaSvrskemDohrani != null -> " (barva ${barvaSvrskemDohrani.ceskyNazev}) — dohrává ${stav.jmenaHracu[dalsiAktivni]}!"
                else -> " — o další pořadí pokračuje ${stav.jmenaHracu[dalsiAktivni]}!"
            }

            return stav.copy(
                ruceHracu = noveRuce,
                odhazovaciBalicek = novyOdhazovaci,
                naTahu = dalsiAktivni,
                zvolenaBarvaSvrskem = barvaSvrskemDohrani,
                aktivniSedmyPocet = noveSedmyDohrani,
                aktivniEsoStoji = noveEsoDohrani,
                cekaNaVolbuBarvy = false,
                posledniHlaska = "$jmeno bere $umisteni. místo (${karta.celyNazev()})$dovetekDohrani",
                jeKonec = false,
                indexViteze = prvniVitez,
                cisloTahu = stav.cisloTahu + 1,
                poradiDohrani = novePoradi,
                chybejiciBarvyMaska = upraveneMasky
            )
        }

        if (karta.hodnota == HodnotaKarty.SVRSEK && zvolenaBarva == null) {
            return stav.copy(
                ruceHracu = noveRuce,
                odhazovaciBalicek = novyOdhazovaci,
                cekaNaVolbuBarvy = true,
                chybejiciBarvyMaska = upraveneMasky,
                posledniHlaska = "$jmeno vynesl svrška a volí novou barvu…"
            )
        }

        val dalsiHrac = najdiDalsihoAktivnihoHrace(indexHrace, stav.pocetHracu, noveRuce, stav.poradiDohrani)
        val prebilEsoCervenouSedmou = stav.aktivniEsoStoji && karta.hodnota == HodnotaKarty.R7
        val noveSedmy = if (karta.hodnota == HodnotaKarty.R7) stav.aktivniSedmyPocet + 1 else 0
        val noveEso = karta.hodnota == HodnotaKarty.ESO
        val barvaSvrskem = if (karta.hodnota == HodnotaKarty.SVRSEK) zvolenaBarva else null

        val hlaska = when {
            prebilEsoCervenouSedmou ->
                "$jmeno přebíjí stojící eso Červenou sedmou — ${sklonujBereSeKaret(noveSedmy * 2)}!"
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
            cisloTahu = stav.cisloTahu + 1,
            chybejiciBarvyMaska = upraveneMasky
        )
    }

    fun potvrdVolbuBarvySvrskem(stav: StavPrsi, barva: BarvaKarty): StavPrsi {
        if (!stav.cekaNaVolbuBarvy || stav.jeKonec) return stav
        val hracIdx = stav.naTahu
        val jmeno = stav.jmenaHracu[hracIdx]
        val dalsiHrac = najdiDalsihoAktivnihoHrace(hracIdx, stav.pocetHracu, stav.ruceHracu, stav.poradiDohrani)
        val upraveneMasky = stav.chybejiciBarvyMaska.mapIndexed { idx, m ->
            if (idx == hracIdx) m and (1 shl barva.ordinal).inv() and PRSI_BIT_BEZ_SVRSKU.inv() else m
        }
        return stav.copy(
            naTahu = dalsiHrac,
            zvolenaBarvaSvrskem = barva,
            cekaNaVolbuBarvy = false,
            posledniHlaska = "$jmeno mění barvu na ${barva.ceskyNazev}!",
            cisloTahu = stav.cisloTahu + 1,
            chybejiciBarvyMaska = upraveneMasky
        )
    }

    fun lizniNeboStuj(stav: StavPrsi, indexHrace: Int): StavPrsi {
        if (stav.jeKonec || stav.naTahu != indexHrace || stav.cekaNaVolbuBarvy || indexHrace in stav.poradiDohrani) {
            return stav
        }
        val jmeno = stav.jmenaHracu[indexHrace]
        val dalsiHrac = najdiDalsihoAktivnihoHrace(indexHrace, stav.pocetHracu, stav.ruceHracu, stav.poradiDohrani)

        if (stav.aktivniEsoStoji) {
            // Hráč stál na esu a nelízal -> veřejně víme, že nedrží Eso (ani Červenou sedmu v hospodských pravidlech)
            val maskyPoStani = stav.chybejiciBarvyMaska.mapIndexed { idx, m ->
                if (idx == indexHrace) m or PRSI_BIT_BEZ_ESA else m
            }
            return stav.copy(
                naTahu = dalsiHrac,
                aktivniEsoStoji = false,
                posledniHlaska = "$jmeno stojí jedno kolo.",
                cisloTahu = stav.cisloTahu + 1,
                chybejiciBarvyMaska = maskyPoStani
            )
        }

        val platnaBarvaPredLiznutim = stav.aktualniPlatnaBarva
        val bylUtokSedmou = stav.aktivniSedmyPocet > 0
        val kolikLiznout = if (bylUtokSedmou) stav.aktivniSedmyPocet * 2 else 1
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

        val upraveneMasky = stav.chybejiciBarvyMaska.mapIndexed { idx, maska ->
            if (idx != indexHrace) {
                maska
            } else if (bylUtokSedmou && dobrane.size >= 2) {
                0
            } else {
                maska or (1 shl platnaBarvaPredLiznutim.ordinal) or PRSI_BIT_BEZ_SVRSKU
            }
        }

        val novaRuka = Karta.seradRuku(stav.ruceHracu[indexHrace] + dobrane)
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == indexHrace) novaRuka else r }
        val hlaska = if (bylUtokSedmou) {
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
            cisloTahu = stav.cisloTahu + 1,
            chybejiciBarvyMaska = upraveneMasky
        )
    }

    fun provedTahBota(stav: StavPrsi): StavPrsi {
        return PrsiBotAi.provedTah(stav)
    }

    fun vyhodnotVysledek(stav: StavPrsi): VysledekPartie {
        val trestneBody: List<Int> = stav.ruceHracu.map { ruka ->
            var soucetRuky = 0
            for (karta in ruka) {
                soucetRuky += trestnaHodnotaKartyPrsi(karta.hodnota)
            }
            soucetRuky
        }

        val jizDohrani = if (stav.poradiDohrani.isNotEmpty()) {
            stav.poradiDohrani
        } else {
            listOf(stav.indexViteze.coerceIn(0, stav.pocetHracu - 1))
        }
        val nedohrani = (0 until stav.pocetHracu)
            .filter { it !in jizDohrani }
            .sortedWith(
                compareBy<Int>(
                    { stav.ruceHracu[it].size },
                    { trestneBody[it] },
                    { it }
                )
            )
        val konecnePoradi = jizDohrani + nedohrani
        val vitez = konecnePoradi.first()
        val jmenoViteze = stav.jmenaHracu.getOrElse(vitez) { "Vítěz" }
        val celkemOdSouperu: Int = trestneBody.fold(0) { acc, hodnota -> acc + hodnota }

        val polozky = ArrayList<PolozkaVyuctovani>()
        polozky.add(PolozkaVyuctovani("Vítěz partie", jmenoViteze, jeZvyraznena = true))
        for ((poradiIdx, hracIdx) in konecnePoradi.withIndex()) {
            val umisteni = poradiIdx + 1
            val pocetKaret = stav.ruceHracu[hracIdx].size
            val popisKaret = if (pocetKaret == 0) "dohráno" else sklonujKartyVRuce(pocetKaret)
            val hodnotaRadku = when {
                hracIdx == vitez -> "+$celkemOdSouperu b."
                pocetKaret == 0 -> "0 b."
                else -> "-${trestneBody[hracIdx]} b."
            }
            polozky.add(
                PolozkaVyuctovani(
                    popis = "$umisteni. ${stav.jmenaHracu[hracIdx]} ($popisKaret)",
                    hodnotaText = hodnotaRadku,
                    jeZvyraznena = umisteni == 1
                )
            )
        }

        val hospodskaDohravka = stav.variantaPravidel == VariantaPravidel.HOSPODSKA && stav.pocetHracu >= 3
        val zmenyHracu = List(stav.pocetHracu) { i ->
            val umisteni = konecnePoradi.indexOf(i) + 1
            val jeVit = i == vitez
            val terezianyZmena = if (hospodskaDohravka) {
                if (stav.pocetHracu == 3) {
                    when (umisteni) {
                        1 -> 21
                        2 -> 0
                        else -> -10
                    }
                } else {
                    when (umisteni) {
                        1 -> 26
                        2 -> 8
                        3 -> -4
                        else -> -12
                    }
                }
            } else {
                if (jeVit) 16 + (stav.pocetHracu - 2) * 5 else -10
            }

            ZmenaBoduHrace(
                indexHrace = i,
                jmenoHrace = stav.jmenaHracu[i],
                bodyZapasu = if (jeVit) celkemOdSouperu else -trestneBody[i],
                zmenaElo = terezianyZmena,
                jeVitez = jeVit
            )
        }

        return VysledekPartie(
            typHry = TypHry.PRSI,
            nadpis = "Vítězí $jmenoViteze!",
            podnadpis = "Konečné pořadí u stolu (${stav.variantaPravidel.kratkyNazev.lowercase()} pravidla)",
            polozky = polozky,
            hraci = zmenyHracu
        )
    }
}
