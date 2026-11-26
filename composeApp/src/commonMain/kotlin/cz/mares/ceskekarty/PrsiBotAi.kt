package cz.mares.ceskekarty

import kotlin.random.Random

private sealed class AkceBotaPrsi {
    data object LiznoutNeboStat : AkceBotaPrsi()
    data class ZvolitBarvu(val barva: BarvaKarty) : AkceBotaPrsi()
    data class ZahratKartu(val karta: Karta, val barvaSvrskem: BarvaKarty? = null) : AkceBotaPrsi()
}

private class CitacUzluPrsi(var zbyva: Int)

private class SimStavPrsi(
    val pocetHracu: Int,
    val variantaPravidel: VariantaPravidel,
    val ruce: List<List<Karta>>,
    val lizaci: List<Karta>,
    val lizaciIdx: Int,
    val vrchniKarta: Karta,
    val zvolenaBarvaSvrskem: BarvaKarty?,
    val aktivniSedmyPocet: Int,
    val aktivniEsoStoji: Boolean,
    val naTahu: Int,
    val poradiDohrani: List<Int>,
    val ziviHraci: List<Boolean>,
    val jeKonec: Boolean
) {
    val platnaBarva: BarvaKarty
        get() = zvolenaBarvaSvrskem ?: vrchniKarta.barva
}

internal object PrsiBotAi {

    private val VSECHNY_KARTY_32: List<Karta> = buildList(32) {
        for (b in BarvaKarty.entries) {
            for (h in HodnotaKarty.entries) {
                add(Karta(b, h))
            }
        }
    }

    private fun dalsiAktivniSim(sim: SimStavPrsi, odIndexu: Int, noveRuce: List<List<Karta>>, novePoradi: List<Int>): Int {
        return PrsiEngine.najdiDalsihoAktivnihoHrace(odIndexu, sim.pocetHracu, noveRuce, novePoradi)
    }

    private fun povoleneKartySim(sim: SimStavPrsi, hrac: Int): List<Karta> {
        val ruka = sim.ruce[hrac]
        if (ruka.isEmpty() || hrac in sim.poradiDohrani) return emptyList()
        if (sim.aktivniSedmyPocet > 0) {
            return ruka.filter { it.hodnota == HodnotaKarty.R7 }
        }
        if (sim.aktivniEsoStoji) {
            return ruka.filter { k ->
                k.hodnota == HodnotaKarty.ESO ||
                    (sim.variantaPravidel == VariantaPravidel.HOSPODSKA &&
                        k.barva == BarvaKarty.CERVENE &&
                        k.hodnota == HodnotaKarty.R7)
            }
        }
        val barva = sim.platnaBarva
        val hod = sim.vrchniKarta.hodnota
        return ruka.filter { k ->
            k.hodnota == HodnotaKarty.SVRSEK || k.barva == barva || k.hodnota == hod
        }
    }

    private fun simLizniNeboStuj(sim: SimStavPrsi): SimStavPrsi {
        val hrac = sim.naTahu
        val dalsi = dalsiAktivniSim(sim, hrac, sim.ruce, sim.poradiDohrani)
        if (sim.aktivniEsoStoji) {
            return SimStavPrsi(
                pocetHracu = sim.pocetHracu,
                variantaPravidel = sim.variantaPravidel,
                ruce = sim.ruce,
                lizaci = sim.lizaci,
                lizaciIdx = sim.lizaciIdx,
                vrchniKarta = sim.vrchniKarta,
                zvolenaBarvaSvrskem = sim.zvolenaBarvaSvrskem,
                aktivniSedmyPocet = 0,
                aktivniEsoStoji = false,
                naTahu = dalsi,
                poradiDohrani = sim.poradiDohrani,
                ziviHraci = sim.ziviHraci,
                jeKonec = false
            )
        }

        val kolik = if (sim.aktivniSedmyPocet > 0) sim.aktivniSedmyPocet * 2 else 1
        var lIdx = sim.lizaciIdx
        val novaRuka = ArrayList<Karta>(sim.ruce[hrac].size + kolik)
        novaRuka.addAll(sim.ruce[hrac])
        repeat(kolik) {
            if (lIdx < sim.lizaci.size) {
                novaRuka.add(sim.lizaci[lIdx++])
            }
        }
        val noveRuce = sim.ruce.mapIndexed { i, r -> if (i == hrac) novaRuka else r }
        return SimStavPrsi(
            pocetHracu = sim.pocetHracu,
            variantaPravidel = sim.variantaPravidel,
            ruce = noveRuce,
            lizaci = sim.lizaci,
            lizaciIdx = lIdx,
            vrchniKarta = sim.vrchniKarta,
            zvolenaBarvaSvrskem = sim.zvolenaBarvaSvrskem,
            aktivniSedmyPocet = 0,
            aktivniEsoStoji = false,
            naTahu = dalsi,
            poradiDohrani = sim.poradiDohrani,
            ziviHraci = sim.ziviHraci,
            jeKonec = false
        )
    }

    private fun simZahrajKartu(sim: SimStavPrsi, karta: Karta, barvaSvrskem: BarvaKarty?): SimStavPrsi {
        val hrac = sim.naTahu
        val puvRuka = sim.ruce[hrac]
        val novaRuka = ArrayList<Karta>(puvRuka.size - 1)
        var odebrano = false
        for (k in puvRuka) {
            if (!odebrano && k == karta) {
                odebrano = true
            } else {
                novaRuka.add(k)
            }
        }
        val noveRuce = sim.ruce.mapIndexed { i, r -> if (i == hrac) novaRuka else r }
        val novaZvolenaBarva = if (karta.hodnota == HodnotaKarty.SVRSEK) (barvaSvrskem ?: karta.barva) else null
        val noveSedmy = if (karta.hodnota == HodnotaKarty.R7) sim.aktivniSedmyPocet + 1 else 0
        val noveEso = karta.hodnota == HodnotaKarty.ESO

        if (novaRuka.isEmpty()) {
            val novePoradi = if (hrac in sim.poradiDohrani) sim.poradiDohrani else (sim.poradiDohrani + hrac)
            val pokracovat = PrsiEngine.maPokracovatDohravani(
                variantaPravidel = sim.variantaPravidel,
                pocetHracu = sim.pocetHracu,
                ruce = noveRuce,
                poradiDohrani = novePoradi,
                ziviHraci = sim.ziviHraci
            )
            if (!pokracovat) {
                return SimStavPrsi(
                    pocetHracu = sim.pocetHracu,
                    variantaPravidel = sim.variantaPravidel,
                    ruce = noveRuce,
                    lizaci = sim.lizaci,
                    lizaciIdx = sim.lizaciIdx,
                    vrchniKarta = karta,
                    zvolenaBarvaSvrskem = novaZvolenaBarva,
                    aktivniSedmyPocet = 0,
                    aktivniEsoStoji = false,
                    naTahu = hrac,
                    poradiDohrani = novePoradi,
                    ziviHraci = sim.ziviHraci,
                    jeKonec = true
                )
            }
            val dalsi = dalsiAktivniSim(sim, hrac, noveRuce, novePoradi)
            return SimStavPrsi(
                pocetHracu = sim.pocetHracu,
                variantaPravidel = sim.variantaPravidel,
                ruce = noveRuce,
                lizaci = sim.lizaci,
                lizaciIdx = sim.lizaciIdx,
                vrchniKarta = karta,
                zvolenaBarvaSvrskem = novaZvolenaBarva,
                aktivniSedmyPocet = noveSedmy,
                aktivniEsoStoji = noveEso,
                naTahu = dalsi,
                poradiDohrani = novePoradi,
                ziviHraci = sim.ziviHraci,
                jeKonec = false
            )
        }

        val dalsi = dalsiAktivniSim(sim, hrac, noveRuce, sim.poradiDohrani)
        return SimStavPrsi(
            pocetHracu = sim.pocetHracu,
            variantaPravidel = sim.variantaPravidel,
            ruce = noveRuce,
            lizaci = sim.lizaci,
            lizaciIdx = sim.lizaciIdx,
            vrchniKarta = karta,
            zvolenaBarvaSvrskem = novaZvolenaBarva,
            aktivniSedmyPocet = noveSedmy,
            aktivniEsoStoji = noveEso,
            naTahu = dalsi,
            poradiDohrani = sim.poradiDohrani,
            ziviHraci = sim.ziviHraci,
            jeKonec = false
        )
    }

    /**
     * Spočítá počet nespojených ostrovů v ruce mimo svršky — bot díky tomu neničí mosty stejných hodnot mezi barvami!
     */
    private fun spoctiOstrovyVRuce(rukaBezSvrsku: List<Karta>): Int {
        val n = rukaBezSvrsku.size
        if (n <= 1) return n
        var navstiveno = 0
        var ostrovy = 0
        val fronta = IntArray(n)
        for (start in 0 until n) {
            if ((navstiveno and (1 shl start)) != 0) continue
            ostrovy++
            var hlava = 0
            var ocas = 0
            fronta[ocas++] = start
            navstiveno = navstiveno or (1 shl start)
            while (hlava < ocas) {
                val u = fronta[hlava++]
                val ku = rukaBezSvrsku[u]
                for (v in 0 until n) {
                    if ((navstiveno and (1 shl v)) == 0) {
                        val kv = rukaBezSvrsku[v]
                        if (ku.barva == kv.barva || ku.hodnota == kv.hodnota) {
                            navstiveno = navstiveno or (1 shl v)
                            fronta[ocas++] = v
                        }
                    }
                }
            }
        }
        return ostrovy
    }

    private fun ohodnotSimStav(sim: SimStavPrsi, idxBota: Int, zbyvaHloubka: Int): Int {
        val poradiIdx = sim.poradiDohrani.indexOf(idxBota)
        if (poradiIdx == 0) {
            return 5000 + zbyvaHloubka * 90
        }
        if (poradiIdx == 1) {
            return if (sim.pocetHracu == 4) 1400 + zbyvaHloubka * 30 else 500 + zbyvaHloubka * 20
        }

        val mojeRuka = sim.ruce[idxBota]
        var mojeTrestne = 0
        for (k in mojeRuka) {
            mojeTrestne += PrsiEngine.trestnaHodnotaKartyPrsi(k.hodnota)
        }

        if (sim.jeKonec) {
            return -3500 - mojeTrestne * 8 - mojeRuka.size * 60
        }

        var minSouperKaret = 99
        var souperJednaKartaNaTahu = false
        for (p in 0 until sim.pocetHracu) {
            if (p != idxBota && p !in sim.poradiDohrani && sim.ruce[p].isNotEmpty()) {
                val sz = sim.ruce[p].size
                if (sz < minSouperKaret) minSouperKaret = sz
                if (sz == 1 && sim.naTahu == p && sim.aktivniSedmyPocet == 0 && !sim.aktivniEsoStoji) {
                    souperJednaKartaNaTahu = true
                }
            }
        }
        if (minSouperKaret == 99) minSouperKaret = 0

        val svrsky = mojeRuka.count { it.hodnota == HodnotaKarty.SVRSEK }
        val bezSvrsku = mojeRuka.filter { it.hodnota != HodnotaKarty.SVRSEK }
        val ostrovy = spoctiOstrovyVRuce(bezSvrsku)
        val volneNepropojeneOstrovy = (ostrovy - 1 - svrsky).coerceAtLeast(0)

        var skore = (minSouperKaret - mojeRuka.size) * 145 - mojeRuka.size * 55 - mojeTrestne * 2
        skore -= volneNepropojeneOstrovy * 72
        skore += svrsky * 82

        for (k in bezSvrsku) {
            if (k.hodnota == HodnotaKarty.R7) {
                skore += if (sim.variantaPravidel == VariantaPravidel.HOSPODSKA && k.barva == BarvaKarty.CERVENE) 54 else 38
            } else if (k.hodnota == HodnotaKarty.ESO) {
                skore += 32
            }
        }

        val navazujeNaStul = mojeRuka.any {
            it.hodnota == HodnotaKarty.SVRSEK ||
                it.barva == sim.platnaBarva ||
                it.hodnota == sim.vrchniKarta.hodnota
        }
        if (navazujeNaStul) skore += 36

        if (minSouperKaret == 1) skore -= 380
        if (souperJednaKartaNaTahu) skore -= 680

        if (sim.naTahu == idxBota) {
            if (sim.aktivniSedmyPocet > 0 && mojeRuka.none { it.hodnota == HodnotaKarty.R7 }) {
                skore -= sim.aktivniSedmyPocet * 210
            } else if (sim.aktivniEsoStoji && povoleneKartySim(sim, idxBota).isEmpty()) {
                skore -= 85
            } else {
                skore += 45
            }
        } else {
            if (sim.aktivniSedmyPocet > 0) skore += sim.aktivniSedmyPocet * 115
            if (sim.aktivniEsoStoji) skore += 70
        }

        return skore
    }

    private fun generujAkceSim(sim: SimStavPrsi): List<AkceBotaPrsi> {
        val hrac = sim.naTahu
        val povolene = povoleneKartySim(sim, hrac)
        if (povolene.isEmpty()) {
            return listOf(AkceBotaPrsi.LiznoutNeboStat)
        }

        val akce = ArrayList<AkceBotaPrsi>(povolene.size + 3)
        var svrsekPridan = false
        for (k in povolene) {
            if (k.hodnota == HodnotaKarty.SVRSEK) {
                if (!svrsekPridan) {
                    svrsekPridan = true
                    val zbytek = sim.ruce[hrac].filterNot { it == k }
                    if (zbytek.isEmpty()) {
                        akce.add(AkceBotaPrsi.ZahratKartu(k, k.barva))
                    } else {
                        for (b in BarvaKarty.entries) {
                            akce.add(AkceBotaPrsi.ZahratKartu(k, b))
                        }
                    }
                }
            } else {
                akce.add(AkceBotaPrsi.ZahratKartu(k, null))
            }
        }
        return akce
    }

    private fun alphaBetaPrsi(
        sim: SimStavPrsi,
        hloubka: Int,
        alfaPuv: Int,
        betaPuv: Int,
        idxBota: Int,
        citac: CitacUzluPrsi
    ): Int {
        if (sim.jeKonec || idxBota in sim.poradiDohrani || hloubka <= 0 || citac.zbyva <= 0) {
            return ohodnotSimStav(sim, idxBota, hloubka)
        }
        citac.zbyva--

        val akceSeznam = generujAkceSim(sim)
        val jeTahBota = sim.naTahu == idxBota
        var alfa = alfaPuv
        var beta = betaPuv

        if (jeTahBota) {
            var nej = Int.MIN_VALUE / 2
            for (akce in akceSeznam) {
                val dalsi = when (akce) {
                    is AkceBotaPrsi.LiznoutNeboStat -> simLizniNeboStuj(sim)
                    is AkceBotaPrsi.ZahratKartu -> simZahrajKartu(sim, akce.karta, akce.barvaSvrskem)
                    is AkceBotaPrsi.ZvolitBarvu -> sim
                }
                val h = alphaBetaPrsi(dalsi, hloubka - 1, alfa, beta, idxBota, citac)
                if (h > nej) nej = h
                if (nej > alfa) alfa = nej
                if (alfa >= beta) break
            }
            return nej
        } else {
            var nej = Int.MAX_VALUE / 2
            for (akce in akceSeznam) {
                val dalsi = when (akce) {
                    is AkceBotaPrsi.LiznoutNeboStat -> simLizniNeboStuj(sim)
                    is AkceBotaPrsi.ZahratKartu -> simZahrajKartu(sim, akce.karta, akce.barvaSvrskem)
                    is AkceBotaPrsi.ZvolitBarvu -> sim
                }
                val h = alphaBetaPrsi(dalsi, hloubka - 1, alfa, beta, idxBota, citac)
                if (h < nej) nej = h
                if (nej < beta) beta = nej
                if (alfa >= beta) break
            }
            return nej
        }
    }

    private fun sestavSimSvet(
        stav: StavPrsi,
        idxBota: Int,
        zamichaneNevidene: List<Karta>
    ): SimStavPrsi {
        var pos = 0
        val ruce = List(stav.pocetHracu) { p ->
            if (p == idxBota || p in stav.poradiDohrani) {
                stav.ruceHracu[p]
            } else {
                val pocet = stav.ruceHracu[p].size
                val vyrez = ArrayList<Karta>(pocet)
                repeat(pocet) {
                    if (pos < zamichaneNevidene.size) {
                        vyrez.add(zamichaneNevidene[pos++])
                    }
                }
                vyrez
            }
        }
        val lizaci = if (pos < zamichaneNevidene.size) {
            zamichaneNevidene.subList(pos, zamichaneNevidene.size)
        } else {
            emptyList()
        }
        return SimStavPrsi(
            pocetHracu = stav.pocetHracu,
            variantaPravidel = stav.variantaPravidel,
            ruce = ruce,
            lizaci = lizaci,
            lizaciIdx = 0,
            vrchniKarta = stav.vrchniKarta,
            zvolenaBarvaSvrskem = stav.zvolenaBarvaSvrskem,
            aktivniSedmyPocet = stav.aktivniSedmyPocet,
            aktivniEsoStoji = stav.aktivniEsoStoji,
            naTahu = stav.naTahu,
            poradiDohrani = stav.poradiDohrani,
            ziviHraci = stav.ziviHraci,
            jeKonec = stav.jeKonec
        )
    }

    fun provedTah(stav: StavPrsi): StavPrsi {
        if (stav.jeKonec) return stav
        val idx = stav.naTahu
        val mojeRuka = stav.ruceHracu[idx]

        // 1) Férový výpočet neviděných karet (nikdy nečte karty soupeřů ani lízací balíček!)
        val zname = HashSet<Karta>(32)
        zname.addAll(mojeRuka)
        zname.addAll(stav.odhazovaciBalicek)
        val nevideneKarty = VSECHNY_KARTY_32.filterNot { it in zname }

        // Počet neviděných karet v jednotlivých barvách (pro bonus vyhladovění barvy)
        val nevideneVBarve = IntArray(BarvaKarty.entries.size)
        for (k in nevideneKarty) {
            if (k.hodnota != HodnotaKarty.SVRSEK) {
                nevideneVBarve[k.barva.ordinal]++
            }
        }

        val korenoveAkce = ArrayList<AkceBotaPrsi>()
        if (stav.cekaNaVolbuBarvy) {
            for (b in BarvaKarty.entries) {
                korenoveAkce.add(AkceBotaPrsi.ZvolitBarvu(b))
            }
        } else {
            val povolene = PrsiEngine.zjistiPovoleneKarty(stav, idx)
            if (povolene.isEmpty()) {
                return PrsiEngine.lizniNeboStuj(stav, idx)
            }
            var svrsekPridan = false
            for (k in povolene) {
                if (k.hodnota == HodnotaKarty.SVRSEK) {
                    if (!svrsekPridan) {
                        svrsekPridan = true
                        if (mojeRuka.size == 1) {
                            korenoveAkce.add(AkceBotaPrsi.ZahratKartu(k, k.barva))
                        } else {
                            for (b in BarvaKarty.entries) {
                                korenoveAkce.add(AkceBotaPrsi.ZahratKartu(k, b))
                            }
                        }
                    }
                } else {
                    korenoveAkce.add(AkceBotaPrsi.ZahratKartu(k, null))
                }
            }
        }

        if (korenoveAkce.size == 1) {
            return aplikujKorenovouAkci(stav, idx, korenoveAkce.first())
        }

        val pocetSvetu = 22
        val hloubka = if (stav.pocetHracu == 2 && mojeRuka.size <= 4) 6 else 5
        val rozpocetNaAkci = 420
        val nahoda = Random(ziskejAktualniCasMs() xor (stav.cisloTahu.toLong() * 2654435761L))
        val michaci = nevideneKarty.toMutableList()
        val souctySkore = DoubleArray(korenoveAkce.size)

        val dalsiIdx = PrsiEngine.najdiDalsihoAktivnihoHrace(idx, stav.pocetHracu, stav.ruceHracu, stav.poradiDohrani)
        val maskaDalsiho = stav.chybejiciBarvyMaska.getOrElse(dalsiIdx) { 0 }

        repeat(pocetSvetu) {
            var vybranySim: SimStavPrsi? = null
            for (pokus in 0 until 3) {
                michaci.shuffle(nahoda)
                val simKandidat = sestavSimSvet(stav, idx, michaci)
                if (pokus == 2) {
                    vybranySim = simKandidat
                    break
                }
                var porusujeMasku = false
                for (p in 0 until stav.pocetHracu) {
                    if (p == idx || p in stav.poradiDohrani) continue
                    val m = stav.chybejiciBarvyMaska.getOrElse(p) { 0 }
                    if (m == 0) continue
                    val rukaP = simKandidat.ruce[p]
                    if ((m and PRSI_BIT_BEZ_SVRSKU) != 0 && rukaP.any { it.hodnota == HodnotaKarty.SVRSEK }) {
                        porusujeMasku = true
                        break
                    }
                    if ((m and PRSI_BIT_BEZ_ESA) != 0 && rukaP.any { it.hodnota == HodnotaKarty.ESO }) {
                        porusujeMasku = true
                        break
                    }
                    for (b in BarvaKarty.entries) {
                        if ((m and (1 shl b.ordinal)) != 0 && rukaP.count { it.barva == b } >= 2) {
                            porusujeMasku = true
                            break
                        }
                    }
                    if (porusujeMasku) break
                }
                if (!porusujeMasku) {
                    vybranySim = simKandidat
                    break
                }
            }

            val zakladSim = vybranySim ?: sestavSimSvet(stav, idx, michaci)
            for ((aIdx, akce) in korenoveAkce.withIndex()) {
                val poAkci = when (akce) {
                    is AkceBotaPrsi.ZvolitBarvu -> {
                        val d = dalsiAktivniSim(zakladSim, idx, zakladSim.ruce, zakladSim.poradiDohrani)
                        SimStavPrsi(
                            pocetHracu = zakladSim.pocetHracu,
                            variantaPravidel = zakladSim.variantaPravidel,
                            ruce = zakladSim.ruce,
                            lizaci = zakladSim.lizaci,
                            lizaciIdx = zakladSim.lizaciIdx,
                            vrchniKarta = zakladSim.vrchniKarta,
                            zvolenaBarvaSvrskem = akce.barva,
                            aktivniSedmyPocet = 0,
                            aktivniEsoStoji = false,
                            naTahu = d,
                            poradiDohrani = zakladSim.poradiDohrani,
                            ziviHraci = zakladSim.ziviHraci,
                            jeKonec = false
                        )
                    }
                    is AkceBotaPrsi.ZahratKartu -> simZahrajKartu(zakladSim, akce.karta, akce.barvaSvrskem)
                    is AkceBotaPrsi.LiznoutNeboStat -> simLizniNeboStuj(zakladSim)
                }
                val citac = CitacUzluPrsi(rozpocetNaAkci)
                val hodnota = alphaBetaPrsi(
                    sim = poAkci,
                    hloubka = hloubka - 1,
                    alfaPuv = Int.MIN_VALUE / 2,
                    betaPuv = Int.MAX_VALUE / 2,
                    idxBota = idx,
                    citac = citac
                )
                souctySkore[aIdx] += hodnota.toDouble()
            }
        }

        var nejIdx = 0
        var nejCelkem = Double.NEGATIVE_INFINITY
        for ((aIdx, akce) in korenoveAkce.withIndex()) {
            val prumer = souctySkore[aIdx] / pocetSvetu.toDouble()
            val cilovaBarva = when (akce) {
                is AkceBotaPrsi.ZvolitBarvu -> akce.barva
                is AkceBotaPrsi.ZahratKartu -> akce.barvaSvrskem ?: akce.karta.barva
                is AkceBotaPrsi.LiznoutNeboStat -> stav.aktualniPlatnaBarva
            }
            // Bonus za vyhladovění barvy (když je venku minimum karet této barvy nebo na ní soupeř lízal)
            val vyhladoveniBonus = (6 - nevideneVBarve[cilovaBarva.ordinal]).coerceAtLeast(0) * 8.0 +
                if ((maskaDalsiho and (1 shl cilovaBarva.ordinal)) != 0) 18.0 else 0.0
            val celkem = prumer + vyhladoveniBonus
            if (celkem > nejCelkem) {
                nejCelkem = celkem
                nejIdx = aIdx
            }
        }

        return aplikujKorenovouAkci(stav, idx, korenoveAkce[nejIdx])
    }

    private fun aplikujKorenovouAkci(stav: StavPrsi, idx: Int, akce: AkceBotaPrsi): StavPrsi {
        return when (akce) {
            is AkceBotaPrsi.LiznoutNeboStat -> PrsiEngine.lizniNeboStuj(stav, idx)
            is AkceBotaPrsi.ZvolitBarvu -> PrsiEngine.potvrdVolbuBarvySvrskem(stav, akce.barva)
            is AkceBotaPrsi.ZahratKartu -> PrsiEngine.zahrajKartu(stav, idx, akce.karta, akce.barvaSvrskem)
        }
    }
}
