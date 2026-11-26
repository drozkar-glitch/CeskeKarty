package cz.mares.ceskekarty

import kotlin.random.Random

private class CitacUzluMarias(var zbyva: Int)

private class SimMarias3(
    val indexForhonta: Int,
    val zavazek: ZavazekMariase,
    val trumf: BarvaKarty?,
    val nasobekFlekHra: Int,
    val nasobekFlekSedma: Int,
    val bodyTalonu: Int,
    val ruce: List<List<Karta>>,
    val stych: List<TahStychuMariase>,
    val naTahu: Int,
    val cisloStychu: Int,
    val stychyF: Int,
    val stychyO: Int,
    val bodyF: Int,
    val bodyO: Int,
    val hlaskyF: Int,
    val hlaskyO: Int,
    val stav9Sedma: Int,
    val stav10Sedma: Int,
    val jeKonec: Boolean
)

private class SimLizaciEndgame(
    val trumf: BarvaKarty?,
    val ruce: List<List<Karta>>,
    val stych: List<TahStychuMariase>,
    val naTahu: Int,
    val stychy0: Int,
    val stychy1: Int,
    val body0: Int,
    val body1: Int,
    val hlasky0: Int,
    val hlasky1: Int,
    val tichaSedmaStav: Int,
    val jeKonec: Boolean
)

internal object MariasBotAi {

    private val VSECHNY_KARTY_32: List<Karta> = buildList(32) {
        for (b in BarvaKarty.entries) {
            for (h in HodnotaKarty.entries) {
                add(Karta(b, h))
            }
        }
    }

    private fun soucastHlasky(ruka: List<Karta>, karta: Karta): Boolean {
        if (karta.hodnota != HodnotaKarty.SVRSEK && karta.hodnota != HodnotaKarty.KRAL) return false
        val druha = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
        return ruka.any { it.barva == karta.barva && it.hodnota == druha }
    }

    fun provedTah(stav: StavMarias): StavMarias {
        if (stav.faze == FazeMariase.KONEC) return stav
        if (stav.rezimMariase == RezimMariase.LIZACI) {
            return provedTahLizaci(stav)
        }
        return when (stav.faze) {
            FazeMariase.DRAZBA -> MariasVariantyEngine.provedTahDrazbyBota(stav)
            FazeMariase.VOLBA_TRUMFU -> provedVolbuTrumfuBota(stav)
            FazeMariase.ODKLADANI_TALONU -> provedOdlozeniTalonuBota(stav)
            FazeMariase.FLEKOVANI -> provedFlekovaniBota(stav)
            FazeMariase.SEHRAVKA -> provedSehravku3Hracu(stav)
            FazeMariase.KONEC -> stav
        }
    }

    private fun provedVolbuTrumfuBota(stav: StavMarias): StavMarias {
        if (stav.rezimMariase == RezimMariase.LICITOVANY) {
            return MariasVariantyEngine.zvolTrumfPoDrazbeBot(stav)
        }
        val fh = stav.indexForhonta
        val ruka7 = stav.ruceHracu.getOrNull(fh) ?: return stav

        val pocetEs = ruka7.count { it.hodnota == HodnotaKarty.ESO }
        val pocetKralu = ruka7.count { it.hodnota == HodnotaKarty.KRAL }
        if (pocetEs >= 4 && pocetKralu >= 2) {
            return MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, null, false, ZavazekMariase.DURCH)
        }
        val nizkeBetl = ruka7.count {
            it.hodnota == HodnotaKarty.R7 || it.hodnota == HodnotaKarty.R8 || it.hodnota == HodnotaKarty.R9
        }
        if (nizkeBetl >= 6 && pocetEs == 0) {
            return MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, null, false, ZavazekMariase.BETL)
        }

        var nejlepsiBarva: BarvaKarty? = null
        var nejSkore = -1
        for (barva in BarvaKarty.entries) {
            val vBarve = ruka7.filter { it.barva == barva }
            if (vBarve.isEmpty()) continue
            val hl40 = if (
                Karta(barva, HodnotaKarty.SVRSEK) in ruka7 &&
                Karta(barva, HodnotaKarty.KRAL) in ruka7
            ) 28 else 0
            val maSedmu = Karta(barva, HodnotaKarty.R7) in ruka7
            val sedmaUprava = if (maSedmu && vBarve.size >= 4) 9 else if (maSedmu && vBarve.size <= 2) -6 else 0
            val skore = vBarve.size * 16 + vBarve.sumOf { it.hodnota.silaMarias } + hl40 + sedmaUprava +
                if (barva == BarvaKarty.CERVENE && vBarve.size >= 3) 8 else 0
            if (skore > nejSkore) {
                nejSkore = skore
                nejlepsiBarva = barva
            }
        }

        val nejdelsiBarvaPocet = BarvaKarty.entries.maxOf { b -> ruka7.count { it.barva == b } }
        return if (nejdelsiBarvaPocet <= 2 && nejSkore < 44 && stav.zbytekProForhonta.isNotEmpty()) {
            MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, null, volitZTalonu = true)
        } else {
            MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, nejlepsiBarva ?: BarvaKarty.CERVENE, volitZTalonu = false)
        }
    }

    private fun vyberDveKartyDoTalonuBot(stav: StavMarias, plan: ZavazekMariase): List<Karta> {
        val ruka12 = stav.ruceHracu[stav.indexForhonta]
        val povolene = MariasEngine.zjistiKartyPovoleneDoTalonu(stav, plan).toList()
        if (povolene.size <= 2) return povolene

        if (plan == ZavazekMariase.BETL) {
            return povolene.sortedByDescending { k ->
                val vBarve = ruka12.filter { it.barva == k.barva }
                val maNizkeKryti = vBarve.any { it.hodnota == HodnotaKarty.R7 || it.hodnota == HodnotaKarty.R8 }
                k.hodnota.silaBetlDurch * 10 + (if (!maNizkeKryti) 25 else 0) - vBarve.size * 3
            }.take(2)
        }
        if (plan == ZavazekMariase.DURCH) {
            return povolene.sortedBy { k ->
                val maEsoVBarve = Karta(k.barva, HodnotaKarty.ESO) in ruka12
                k.hodnota.silaBetlDurch * 10 + (if (maEsoVBarve) 8 else 0)
            }.take(2)
        }

        val trumf = stav.trumf
        val trumfSedma = trumf?.let { Karta(it, HodnotaKarty.R7) }
        var nejDvojice = listOf(povolene[0], povolene[1])
        var nejHodnoceni = Int.MIN_VALUE

        for (i in 0 until povolene.size - 1) {
            for (j in i + 1 until povolene.size) {
                val k1 = povolene[i]
                val k2 = povolene[j]
                val zbytek10 = ruka12.filterNot { it == k1 || it == k2 }

                var skore = zbytek10.count { it.barva == trumf } * 36
                if (trumfSedma != null && trumfSedma in zbytek10) {
                    skore += if (plan.hlasiSedmu || zbytek10.count { it.barva == trumf } >= 4) 55 else 14
                }
                if (plan.hlasiDveSedmy) {
                    skore += zbytek10.count { it.hodnota == HodnotaKarty.R7 } * 45
                }
                for (b in BarvaKarty.entries) {
                    val vBarve = zbytek10.filter { it.barva == b }
                    if (Karta(b, HodnotaKarty.SVRSEK) in vBarve && Karta(b, HodnotaKarty.KRAL) in vBarve) {
                        skore += if (b == trumf) 72 else 46
                    }
                    if (b != trumf) {
                        if (vBarve.isEmpty()) skore += 28
                        else if (vBarve.size == 1 && vBarve.first().hodnota == HodnotaKarty.R10) skore -= 18
                    }
                }
                skore -= (k1.hodnota.silaMarias + k2.hodnota.silaMarias) * 2
                if (skore > nejHodnoceni) {
                    nejHodnoceni = skore
                    nejDvojice = listOf(k1, k2)
                }
            }
        }
        return nejDvojice
    }

    private fun provedOdlozeniTalonuBota(stav: StavMarias): StavMarias {
        val povoleneZavazky = MariasEngine.zjistiPovoleneZavazkyTalonu(stav)
        if (stav.zavazek.jeBezTrumfu) {
            val odhoz = vyberDveKartyDoTalonuBot(stav, stav.zavazek)
            return MariasEngine.potvrdTalonAZavazek(stav, odhoz, stav.zavazek)
        }

        val ruka12 = stav.ruceHracu[stav.indexForhonta]
        val trumf = stav.trumf
        val zakladniOdhoz = vyberDveKartyDoTalonuBot(stav, povoleneZavazky.first())
        val ruka10 = ruka12.filterNot { it in zakladniOdhoz }

        val trumfy = ruka10.filter { it.barva == trumf }
        val maTrumf7 = trumf != null && Karta(trumf, HodnotaKarty.R7) in ruka10
        val maTrumfEso = trumf != null && Karta(trumf, HodnotaKarty.ESO) in ruka10
        val pocetVsechSedem = ruka10.count { it.hodnota == HodnotaKarty.R7 }
        val vedlejsiEsa = ruka10.count { it.barva != trumf && it.hodnota == HodnotaKarty.ESO }
        val vedlejsiRenonsy = BarvaKarty.entries.count { b -> b != trumf && ruka10.none { it.barva == b } }
        val trumfVysoke = trumfy.count {
            it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.R10 || it.hodnota == HodnotaKarty.KRAL
        }
        var hlaskyVlastni = 0
        for (b in BarvaKarty.entries) {
            if (Karta(b, HodnotaKarty.SVRSEK) in ruka10 && Karta(b, HodnotaKarty.KRAL) in ruka10) {
                hlaskyVlastni += if (b == trumf) 40 else 20
            }
        }
        val odhadBodu = trumfy.size * 8 + trumfVysoke * 9 + vedlejsiEsa * 12 + vedlejsiRenonsy * 5 + hlaskyVlastni + 10

        val chteny = when {
            ZavazekMariase.STO_A_DVE_SEDMY in povoleneZavazky &&
                maTrumf7 && maTrumfEso && pocetVsechSedem >= 2 && trumfy.size >= 6 && odhadBodu >= 110 ->
                ZavazekMariase.STO_A_DVE_SEDMY
            ZavazekMariase.STO_A_SEDMA in povoleneZavazky && maTrumf7 && trumfy.size >= 5 && odhadBodu >= 105 ->
                ZavazekMariase.STO_A_SEDMA
            ZavazekMariase.STO in povoleneZavazky && trumfy.size >= 4 && odhadBodu >= 102 ->
                ZavazekMariase.STO
            ZavazekMariase.HRA_A_SEDMA in povoleneZavazky && maTrumf7 && trumfy.size >= 5 && trumfVysoke >= 2 ->
                ZavazekMariase.HRA_A_SEDMA
            else ->
                povoleneZavazky.first()
        }

        val definitivniZavazek = if (chteny.vahaDrazby >= (stav.nejvyssiNabidka?.vahaDrazby ?: 1) && chteny in povoleneZavazky) {
            chteny
        } else {
            povoleneZavazky.first()
        }
        val definitivniOdhoz = vyberDveKartyDoTalonuBot(stav, definitivniZavazek)
        return MariasEngine.potvrdTalonAZavazek(stav, definitivniOdhoz, definitivniZavazek)
    }

    private fun maKrytehoStoperaDurch(ruka: List<Karta>, barva: BarvaKarty): Boolean {
        val vBarve = ruka.filter { it.barva == barva }
        return (vBarve.size >= 3 && Karta(barva, HodnotaKarty.R10) in vBarve) ||
            (vBarve.size >= 4 && Karta(barva, HodnotaKarty.KRAL) in vBarve)
    }

    private fun provedFlekovaniBota(stav: StavMarias): StavMarias {
        val idx = stav.naTahu
        val ruka = stav.ruceHracu.getOrNull(idx) ?: return MariasEngine.odpovezNaFlekovani(stav, idx, false, false)
        val lzeHra = MariasEngine.muzeHracFlekHry(stav, idx)
        val lzeSedma = MariasEngine.muzeHracFlekSedmy(stav, idx)
        if (!lzeHra && !lzeSedma) return MariasEngine.odpovezNaFlekovani(stav, idx, false, false)

        val trumf = stav.trumf
        if (idx == stav.indexForhonta) {
            val trumfy = ruka.count { it.barva == trumf }
            val esa = ruka.count { it.hodnota == HodnotaKarty.ESO }
            val ma40 = trumf != null && Karta(trumf, HodnotaKarty.SVRSEK) in ruka && Karta(trumf, HodnotaKarty.KRAL) in ruka
            val daReHra = lzeHra && stav.nasobekFlekHra <= 2 && (trumfy >= 6 || (trumfy >= 5 && (esa >= 3 || ma40)))
            val daReSedma = lzeSedma && stav.nasobekFlekSedma <= 2 && trumfy >= 6 &&
                trumf != null && Karta(trumf, HodnotaKarty.ESO) in ruka && Karta(trumf, HodnotaKarty.R7) in ruka
            return MariasEngine.odpovezNaFlekovani(stav, idx, daReHra, daReSedma)
        } else {
            val trumfy = ruka.filter { it.barva == trumf }
            val esa = ruka.count { it.hodnota == HodnotaKarty.ESO }
            val ma40 = trumf != null && Karta(trumf, HodnotaKarty.SVRSEK) in ruka && Karta(trumf, HodnotaKarty.KRAL) in ruka
            val vynasimPrvni = MariasEngine.kdoVynasiPrvniStych(stav) == idx

            val daFlekHra = lzeHra && stav.nasobekFlekHra == 1 && when (stav.zavazek) {
                ZavazekMariase.DURCH -> (vynasimPrvni && esa >= 1) || esa >= 2 || BarvaKarty.entries.any { maKrytehoStoperaDurch(ruka, it) }
                ZavazekMariase.BETL -> ruka.count { it.hodnota == HodnotaKarty.R7 || it.hodnota == HodnotaKarty.R8 } >= 4 &&
                    BarvaKarty.entries.any { b -> ruka.none { it.barva == b } }
                else -> trumfy.size >= 4 || (trumfy.size >= 3 && (esa >= 2 || ma40))
            }
            val daFlekSedma = lzeSedma && stav.nasobekFlekSedma == 1 && trumf != null && (
                Karta(trumf, HodnotaKarty.R7) in ruka ||
                    (trumfy.size >= 4 && trumfy.any { it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.R10 }) ||
                    (trumfy.size >= 3 && Karta(trumf, HodnotaKarty.ESO) in ruka && Karta(trumf, HodnotaKarty.R10) in ruka)
                )
            return MariasEngine.odpovezNaFlekovani(stav, idx, daFlekHra, daFlekSedma)
        }
    }

    // =========================================================================
    // 3HRÁČOVÁ SEHRÁVKA: Férové Monte Carlo (PIMC) + Alpha-Beta Minimax
    // =========================================================================

    private fun povoleneSim3(sim: SimMarias3, hrac: Int): List<Karta> {
        val ruka = sim.ruce[hrac]
        if (ruka.isEmpty()) return emptyList()
        if (sim.stych.isEmpty()) return ruka

        val barvaVynosu = sim.stych.first().karta.barva
        val bezTrumfu = sim.zavazek.jeBezTrumfu
        val trumf = if (bezTrumfu) null else sim.trumf

        val vBarve = ruka.filter { it.barva == barvaVynosu }
        if (vBarve.isNotEmpty()) {
            val uzPadlCiziTrumf = trumf != null && barvaVynosu != trumf && sim.stych.any { it.karta.barva == trumf }
            if (uzPadlCiziTrumf) return vBarve
            val maxSila = sim.stych.filter { it.karta.barva == barvaVynosu }.maxOf { it.karta.silaProMarias(bezTrumfu) }
            val vyssi = vBarve.filter { it.silaProMarias(bezTrumfu) > maxSila }
            return vyssi.ifEmpty { vBarve }
        }

        if (trumf != null) {
            val trumfy = ruka.filter { it.barva == trumf }
            if (trumfy.isNotEmpty()) {
                val trumfyNaStole = sim.stych.filter { it.karta.barva == trumf }
                if (trumfyNaStole.isNotEmpty()) {
                    val maxTrumf = trumfyNaStole.maxOf { it.karta.silaProMarias(false) }
                    val vyssiTrumfy = trumfy.filter { it.silaProMarias(false) > maxTrumf }
                    return vyssiTrumfy.ifEmpty { trumfy }
                }
                return trumfy
            }
        }
        return ruka
    }

    private fun simZahraj3(sim: SimMarias3, karta: Karta): SimMarias3 {
        val hrac = sim.naTahu
        val puvRuka = sim.ruce[hrac]
        val zbylaRuka = ArrayList<Karta>(puvRuka.size - 1)
        var odebrano = false
        for (k in puvRuka) {
            if (!odebrano && k == karta) odebrano = true else zbylaRuka.add(k)
        }
        val noveRuce = sim.ruce.mapIndexed { i, r -> if (i == hrac) zbylaRuka else r }

        var noveHlF = sim.hlaskyF
        var noveHlO = sim.hlaskyO
        val bezTrumfu = sim.zavazek.jeBezTrumfu
        if (!bezTrumfu && (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL)) {
            val parova = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
            if (zbylaRuka.any { it.barva == karta.barva && it.hodnota == parova }) {
                val bodyHl = if (karta.barva == sim.trumf) 40 else 20
                if (hrac == sim.indexForhonta) noveHlF += bodyHl else noveHlO += bodyHl
            }
        }

        val novyStych = sim.stych + TahStychuMariase(hrac, "", karta)
        if (novyStych.size < 3) {
            return SimMarias3(
                indexForhonta = sim.indexForhonta,
                zavazek = sim.zavazek,
                trumf = sim.trumf,
                nasobekFlekHra = sim.nasobekFlekHra,
                nasobekFlekSedma = sim.nasobekFlekSedma,
                bodyTalonu = sim.bodyTalonu,
                ruce = noveRuce,
                stych = novyStych,
                naTahu = (hrac + 1) % 3,
                cisloStychu = sim.cisloStychu,
                stychyF = sim.stychyF,
                stychyO = sim.stychyO,
                bodyF = sim.bodyF,
                bodyO = sim.bodyO,
                hlaskyF = noveHlF,
                hlaskyO = noveHlO,
                stav9Sedma = sim.stav9Sedma,
                stav10Sedma = sim.stav10Sedma,
                jeKonec = false
            )
        }

        val vitez = MariasEngine.urciVitezeStychu(novyStych, sim.trumf, bezTrumfu).indexHrace
        val bereForhont = vitez == sim.indexForhonta
        val jeDesaty = sim.cisloStychu >= 10 || noveRuce.all { it.isEmpty() }
        val bodyStychu = novyStych.sumOf { it.karta.hodnota.bodyMariasSedma } + if (jeDesaty) 10 else 0

        var novy9 = sim.stav9Sedma
        if (!bezTrumfu && sim.cisloStychu == 9) {
            val tahFh = novyStych.firstOrNull { it.indexHrace == sim.indexForhonta }
            if (tahFh != null && tahFh.karta.hodnota == HodnotaKarty.R7 && tahFh.karta.barva != sim.trumf) {
                novy9 = if (bereForhont) 1 else -1
            }
        }
        var novy10 = sim.stav10Sedma
        if (!bezTrumfu && jeDesaty && sim.trumf != null) {
            val trumf7 = Karta(sim.trumf, HodnotaKarty.R7)
            val tah7 = novyStych.firstOrNull { it.karta == trumf7 }
            if (tah7 != null) {
                novy10 = if (tah7.indexHrace == sim.indexForhonta) {
                    if (bereForhont) 1 else -1
                } else {
                    if (!bereForhont) 2 else -2
                }
            }
        }

        val padlBetl = sim.zavazek == ZavazekMariase.BETL && bereForhont
        val padlDurch = sim.zavazek == ZavazekMariase.DURCH && !bereForhont
        val hotovo = jeDesaty || padlBetl || padlDurch

        return SimMarias3(
            indexForhonta = sim.indexForhonta,
            zavazek = sim.zavazek,
            trumf = sim.trumf,
            nasobekFlekHra = sim.nasobekFlekHra,
            nasobekFlekSedma = sim.nasobekFlekSedma,
            bodyTalonu = sim.bodyTalonu,
            ruce = noveRuce,
            stych = emptyList(),
            naTahu = vitez,
            cisloStychu = if (hotovo) sim.cisloStychu else sim.cisloStychu + 1,
            stychyF = sim.stychyF + if (bereForhont) 1 else 0,
            stychyO = sim.stychyO + if (!bereForhont) 1 else 0,
            bodyF = sim.bodyF + if (bereForhont) bodyStychu else 0,
            bodyO = sim.bodyO + if (!bereForhont) bodyStychu else 0,
            hlaskyF = noveHlF,
            hlaskyO = noveHlO,
            stav9Sedma = novy9,
            stav10Sedma = novy10,
            jeKonec = hotovo
        )
    }

    /**
     * Hodnotí stav z pohledu FORHONTA (Forhont maximalizuje, oba Obránci minimalizují).
     */
    private fun ohodnotSim3ProForhonta(sim: SimMarias3): Int {
        if (sim.jeKonec) {
            val jednotky = MariasEngine.spoctiJednotkyForhonta(
                zavazek = sim.zavazek,
                trumf = sim.trumf,
                nasobekFlekHra = sim.nasobekFlekHra,
                nasobekFlekSedma = sim.nasobekFlekSedma,
                pocetStychuForhont = sim.stychyF,
                pocetStychuObrana = sim.stychyO,
                bodyKaretForhont = sim.bodyF,
                bodyKaretObrana = sim.bodyO,
                bodyTalonu = sim.bodyTalonu,
                hlaskyForhont = sim.hlaskyF,
                hlaskyObrana = sim.hlaskyO,
                stav9Sedma = sim.stav9Sedma,
                stav10Sedma = sim.stav10Sedma
            )
            val celkemF = sim.bodyF + if (sim.stychyF > 0) sim.hlaskyF else 0
            val celkemO = sim.bodyO + sim.bodyTalonu + if (sim.stychyO > 0) sim.hlaskyO else 0
            return jednotky * 150 + (celkemF - celkemO)
        }

        val fh = sim.indexForhonta
        val rukaF = sim.ruce[fh]

        if (sim.zavazek == ZavazekMariase.BETL) {
            if (sim.stychyF > 0) return -4000
            var riziko = 0
            for (k in rukaF) {
                riziko += k.hodnota.silaBetlDurch * 8
            }
            return 1200 - riziko
        }
        if (sim.zavazek == ZavazekMariase.DURCH) {
            if (sim.stychyO > 0) return -6000
            var sila = sim.stychyF * 90
            for (k in rukaF) {
                sila += k.hodnota.silaBetlDurch * 6
            }
            return 1500 + sila
        }

        val trumf = sim.trumf
        val celkemF = sim.bodyF + sim.hlaskyF
        val celkemO = sim.bodyO + sim.bodyTalonu + sim.hlaskyO
        var skore = (celkemF - celkemO) * 5

        val obr1 = (fh + 1) % 3
        val obr2 = (fh + 2) % 3
        val trumfyF = rukaF.count { it.barva == trumf }
        val trumfyO1 = sim.ruce[obr1].count { it.barva == trumf }
        val trumfyO2 = sim.ruce[obr2].count { it.barva == trumf }
        val maxTrumfyO = maxOf(trumfyO1, trumfyO2)

        skore += (trumfyF - maxTrumfyO) * 26
        skore += rukaF.count { it.hodnota == HodnotaKarty.ESO } * 18

        for (p in 0..2) {
            val r = sim.ruce[p]
            var hlVRuce = 0
            for (b in BarvaKarty.entries) {
                if (Karta(b, HodnotaKarty.SVRSEK) in r && Karta(b, HodnotaKarty.KRAL) in r) {
                    hlVRuce += if (b == trumf) 34 else 17
                }
            }
            if (p == fh) skore += hlVRuce else skore -= hlVRuce
        }

        if (trumf != null) {
            val trumf7 = Karta(trumf, HodnotaKarty.R7)
            if (trumf7 in rukaF) {
                skore += if (trumfyF > maxTrumfyO) {
                    if (sim.zavazek.hlasiSedmu) 240 else 95
                } else if (trumfyF == 1 && (trumfyO1 + trumfyO2) > 0) {
                    if (sim.zavazek.hlasiSedmu) -280 else -110
                } else {
                    30
                }
            }
        }

        skore += if (sim.naTahu == fh) 16 else -10
        return skore
    }

    private fun alphaBeta3(
        sim: SimMarias3,
        zbyvaStychu: Int,
        alfaPuv: Int,
        betaPuv: Int,
        citac: CitacUzluMarias
    ): Int {
        if (sim.jeKonec) {
            return ohodnotSim3ProForhonta(sim)
        }
        citac.zbyva--
        if ((zbyvaStychu <= 0 && sim.stych.isEmpty()) || citac.zbyva <= 0) {
            return ohodnotSim3ProForhonta(sim)
        }

        val povolene = povoleneSim3(sim, sim.naTahu)
        if (povolene.isEmpty()) return ohodnotSim3ProForhonta(sim)

        val jeTahForhonta = sim.naTahu == sim.indexForhonta
        var alfa = alfaPuv
        var beta = betaPuv

        if (jeTahForhonta) {
            var nej = Int.MIN_VALUE / 2
            for (k in povolene) {
                val dalsi = simZahraj3(sim, k)
                val uzavrel = dalsi.stych.isEmpty() && !dalsi.jeKonec
                val h = alphaBeta3(dalsi, if (uzavrel) zbyvaStychu - 1 else zbyvaStychu, alfa, beta, citac)
                if (h > nej) nej = h
                if (nej > alfa) alfa = nej
                if (alfa >= beta) break
            }
            return nej
        } else {
            var nej = Int.MAX_VALUE / 2
            for (k in povolene) {
                val dalsi = simZahraj3(sim, k)
                val uzavrel = dalsi.stych.isEmpty() && !dalsi.jeKonec
                val h = alphaBeta3(dalsi, if (uzavrel) zbyvaStychu - 1 else zbyvaStychu, alfa, beta, citac)
                if (h < nej) nej = h
                if (nej < beta) beta = nej
                if (alfa >= beta) break
            }
            return nej
        }
    }

    private fun sestavFerovySvet3(
        stav: StavMarias,
        idxBota: Int,
        nevideneZaklad: List<Karta>,
        nahoda: Random
    ): SimMarias3 {
        val fh = stav.indexForhonta
        val mojeRuka = stav.ruceHracu[idxBota]
        val ciziHraci = (0..2).filter { it != idxBota }
        val pA = ciziHraci[0]
        val pB = ciziHraci[1]
        val cilA = stav.ruceHracu[pA].size
        val cilB = stav.ruceHracu[pB].size

        val rukaA = ArrayList<Karta>(cilA)
        val rukaB = ArrayList<Karta>(cilB)
        val volne = nevideneZaklad.shuffled(nahoda).toMutableList()

        // 1) Připni známou druhou kartu z veřejně nahlášené hlášky 20/40
        for (p in ciziHraci) {
            val hlMaska = stav.nahlaseneHlaskyMaska.getOrElse(p) { 0 }
            if (hlMaska == 0) continue
            val cilRuka = if (p == pA) rukaA else rukaB
            val maxVel = if (p == pA) cilA else cilB
            for (b in BarvaKarty.entries) {
                if ((hlMaska and (1 shl b.ordinal)) != 0) {
                    for (hod in listOf(HodnotaKarty.SVRSEK, HodnotaKarty.KRAL)) {
                        val parKarta = Karta(b, hod)
                        if (cilRuka.size < maxVel && volne.remove(parKarta)) {
                            cilRuka.add(parKarta)
                        }
                    }
                }
            }
        }

        // 2) Pokud bot není forhont, vylosuj 2 karty simulovaného talonu (v trumfové hře nikdy Eso ani 10!)
        var simBodyTalonu = 0
        if (idxBota == fh) {
            simBodyTalonu = stav.talon.sumOf { it.hodnota.bodyMariasSedma }
        } else {
            val talonKarty = ArrayList<Karta>(2)
            val trumf7 = stav.trumf?.let { Karta(it, HodnotaKarty.R7) }
            val iter = volne.iterator()
            while (iter.hasNext() && talonKarty.size < 2) {
                val k = iter.next()
                val zakazanaVTalonu = (!stav.jeBetlNeboDurch && (k.hodnota == HodnotaKarty.ESO || k.hodnota == HodnotaKarty.R10)) ||
                    (stav.variantaPravidel == VariantaPravidel.SVAZOVA && stav.zavazek.hlasiSedmu && k == trumf7)
                if (!zakazanaVTalonu) {
                    talonKarty.add(k)
                    iter.remove()
                }
            }
            while (talonKarty.size < 2 && volne.isNotEmpty()) {
                talonKarty.add(volne.removeAt(0))
            }
            simBodyTalonu = talonKarty.sumOf { it.hodnota.bodyMariasSedma }
        }

        // 3) Rozděl zbylé karty mezi pA a pB s respektem ke zjištěným renonsům (chybejiciBarvyMaska)
        val maskaA = stav.chybejiciBarvyMaska.getOrElse(pA) { 0 }
        val maskaB = stav.chybejiciBarvyMaska.getOrElse(pB) { 0 }
        volne.sortByDescending { k ->
            val bit = 1 shl k.barva.ordinal
            val zakazA = (maskaA and bit) != 0
            val zakazB = (maskaB and bit) != 0
            if (zakazA xor zakazB) 1 else 0
        }

        for (k in volne) {
            val bit = 1 shl k.barva.ordinal
            val zakazA = (maskaA and bit) != 0
            val zakazB = (maskaB and bit) != 0
            when {
                zakazA && !zakazB && rukaB.size < cilB -> rukaB.add(k)
                zakazB && !zakazA && rukaA.size < cilA -> rukaA.add(k)
                rukaA.size < cilA && (rukaB.size >= cilB || nahoda.nextBoolean()) -> rukaA.add(k)
                rukaB.size < cilB -> rukaB.add(k)
                rukaA.size < cilA -> rukaA.add(k)
            }
        }

        val ruce = List(3) { p ->
            when (p) {
                idxBota -> mojeRuka
                pA -> rukaA
                else -> rukaB
            }
        }

        return SimMarias3(
            indexForhonta = fh,
            zavazek = stav.zavazek,
            trumf = stav.trumf,
            nasobekFlekHra = stav.nasobekFlekHra,
            nasobekFlekSedma = stav.nasobekFlekSedma,
            bodyTalonu = simBodyTalonu,
            ruce = ruce,
            stych = stav.aktualniStych,
            naTahu = stav.naTahu,
            cisloStychu = stav.cisloStychu,
            stychyF = stav.pocetStychuForhont,
            stychyO = stav.pocetStychuObrana,
            bodyF = stav.bodyKaretForhont,
            bodyO = stav.bodyKaretObrana,
            hlaskyF = stav.hlaskyForhont,
            hlaskyO = stav.hlaskyObrana,
            stav9Sedma = stav.stavDevatehoStychuSedma,
            stav10Sedma = stav.stavPoslednihoStychuSedma,
            jeKonec = false
        )
    }

    private fun provedSehravku3Hracu(stav: StavMarias): StavMarias {
        val idx = stav.naTahu
        val povolene = MariasEngine.zjistiPovoleneKarty(stav, idx).toList()
        if (povolene.isEmpty()) return stav
        if (povolene.size == 1) return MariasEngine.zahrajKartu(stav, idx, povolene.first())

        val mojeRuka = stav.ruceHracu[idx]
        val zname = HashSet<Karta>(32)
        zname.addAll(stav.odehraneKarty)
        for (t in stav.aktualniStych) zname.add(t.karta)
        zname.addAll(mojeRuka)
        if (idx == stav.indexForhonta) {
            zname.addAll(stav.talon)
        }
        val nevidene = VSECHNY_KARTY_32.filterNot { it in zname }

        val jeKoncovka = stav.cisloStychu >= 7
        val pocetSvetu = if (jeKoncovka) 20 else 18
        val hloubkaStychu = if (jeKoncovka) 5 else 2
        val rozpocetNaAkci = if (jeKoncovka) 850 else 500

        val nahoda = Random(ziskejAktualniCasMs() xor (stav.cisloTahu.toLong() * 2654435761L))
        val souctyProForhonta = DoubleArray(povolene.size)

        repeat(pocetSvetu) {
            val svet = sestavFerovySvet3(stav, idx, nevidene, nahoda)
            for ((kIdx, karta) in povolene.withIndex()) {
                val poTahu = simZahraj3(svet, karta)
                val uzavrel = poTahu.stych.isEmpty() && !poTahu.jeKonec
                val startHloubka = if (uzavrel) hloubkaStychu - 1 else hloubkaStychu
                val citac = CitacUzluMarias(rozpocetNaAkci)
                val hodnota = alphaBeta3(
                    sim = poTahu,
                    zbyvaStychu = startHloubka,
                    alfaPuv = Int.MIN_VALUE / 2,
                    betaPuv = Int.MAX_VALUE / 2,
                    citac = citac
                )
                souctyProForhonta[kIdx] += hodnota.toDouble()
            }
        }

        val jsemForhont = idx == stav.indexForhonta
        val trumfSedma = if (!stav.jeBetlNeboDurch && stav.trumf != null) Karta(stav.trumf, HodnotaKarty.R7) else null
        var nejIdx = 0
        var nejSkore = Double.NEGATIVE_INFINITY

        for ((kIdx, karta) in povolene.withIndex()) {
            val prumerForhonta = souctyProForhonta[kIdx] / pocetSvetu.toDouble()
            val mojePerspektiva = if (jsemForhont) prumerForhonta else -prumerForhonta
            val jemnaKorekce = when {
                trumfSedma != null && karta == trumfSedma && stav.cisloStychu < 10 -> -0.15
                soucastHlasky(mojeRuka, karta) && stav.aktualniStych.isEmpty() -> 0.08
                else -> -karta.hodnota.silaMarias * 0.005
            }
            val celkem = mojePerspektiva + jemnaKorekce
            if (celkem > nejSkore) {
                nejSkore = celkem
                nejIdx = kIdx
            }
        }

        return MariasEngine.zahrajKartu(stav, idx, povolene[nejIdx])
    }

    // =========================================================================
    // LÍZACÍ MARIÁŠ (2 hráči): Výměna 7, chytrá fáze s talonem a přesný Minimax v koncovce
    // =========================================================================

    private fun povoleneLizaciEndgame(sim: SimLizaciEndgame, hrac: Int): List<Karta> {
        val ruka = sim.ruce[hrac]
        if (ruka.isEmpty()) return emptyList()
        if (sim.stych.isEmpty()) return ruka
        val barvaVynosu = sim.stych.first().karta.barva
        val vBarve = ruka.filter { it.barva == barvaVynosu }
        if (vBarve.isNotEmpty()) {
            val maxSila = sim.stych.first().karta.silaProMarias(false)
            val vyssi = vBarve.filter { it.silaProMarias(false) > maxSila }
            return vyssi.ifEmpty { vBarve }
        }
        val trumf = sim.trumf
        if (trumf != null) {
            val trumfy = ruka.filter { it.barva == trumf }
            if (trumfy.isNotEmpty()) return trumfy
        }
        return ruka
    }

    private fun simZahrajLizaci(sim: SimLizaciEndgame, karta: Karta): SimLizaciEndgame {
        val hrac = sim.naTahu
        val zbyla = sim.ruce[hrac].filterNot { it == karta }
        val noveRuce = sim.ruce.mapIndexed { i, r -> if (i == hrac) zbyla else r }

        var hl0 = sim.hlasky0
        var hl1 = sim.hlasky1
        if (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL) {
            val parova = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
            if (zbyla.any { it.barva == karta.barva && it.hodnota == parova }) {
                val bHl = if (karta.barva == sim.trumf) 40 else 20
                if (hrac == 0) hl0 += bHl else hl1 += bHl
            }
        }

        val novyStych = sim.stych + TahStychuMariase(hrac, "", karta)
        if (novyStych.size < 2) {
            return SimLizaciEndgame(
                trumf = sim.trumf,
                ruce = noveRuce,
                stych = novyStych,
                naTahu = 1 - hrac,
                stychy0 = sim.stychy0,
                stychy1 = sim.stychy1,
                body0 = sim.body0,
                body1 = sim.body1,
                hlasky0 = hl0,
                hlasky1 = hl1,
                tichaSedmaStav = sim.tichaSedmaStav,
                jeKonec = false
            )
        }

        val vitez = MariasEngine.urciVitezeStychu(novyStych, sim.trumf, false).indexHrace
        val hotovo = noveRuce.all { it.isEmpty() }
        val bodyStychu = novyStych.sumOf { it.karta.hodnota.bodyMariasSedma } + if (hotovo) 10 else 0

        var novyStav7 = sim.tichaSedmaStav
        if (hotovo && sim.trumf != null) {
            val trumf7 = Karta(sim.trumf, HodnotaKarty.R7)
            val tah7 = novyStych.firstOrNull { it.karta == trumf7 }
            if (tah7 != null) {
                novyStav7 = if (tah7.indexHrace == 0) {
                    if (vitez == 0) 1 else -1
                } else {
                    if (vitez == 1) 2 else -2
                }
            }
        }

        return SimLizaciEndgame(
            trumf = sim.trumf,
            ruce = noveRuce,
            stych = emptyList(),
            naTahu = vitez,
            stychy0 = sim.stychy0 + if (vitez == 0) 1 else 0,
            stychy1 = sim.stychy1 + if (vitez == 1) 1 else 0,
            body0 = sim.body0 + if (vitez == 0) bodyStychu else 0,
            body1 = sim.body1 + if (vitez == 1) bodyStychu else 0,
            hlasky0 = hl0,
            hlasky1 = hl1,
            tichaSedmaStav = novyStav7,
            jeKonec = hotovo
        )
    }

    private fun ohodnotLizaciEndgame(sim: SimLizaciEndgame, idxBota: Int): Int {
        val c0 = sim.body0 + if (sim.stychy0 > 0) sim.hlasky0 else 0
        val c1 = sim.body1 + if (sim.stychy1 > 0) sim.hlasky1 else 0
        val moje = if (idxBota == 0) c0 else c1
        val souper = if (idxBota == 0) c1 else c0

        var skore = (moje - souper) * 4
        if (moje > souper) {
            skore += 400
            if (moje >= 100) skore += 300
        } else if (moje < souper) {
            skore -= 400
            if (souper >= 100) skore -= 300
        }

        val bonusSedmyPro0 = when (sim.tichaSedmaStav) {
            1, -2 -> 500
            -1, 2 -> -500
            else -> 0
        }
        skore += if (idxBota == 0) bonusSedmyPro0 else -bonusSedmyPro0
        return skore
    }

    private fun alphaBetaLizaci(
        sim: SimLizaciEndgame,
        alfaPuv: Int,
        betaPuv: Int,
        idxBota: Int,
        citac: CitacUzluMarias
    ): Int {
        if (sim.jeKonec || citac.zbyva <= 0) {
            return ohodnotLizaciEndgame(sim, idxBota)
        }
        citac.zbyva--
        val povolene = povoleneLizaciEndgame(sim, sim.naTahu)
        if (povolene.isEmpty()) return ohodnotLizaciEndgame(sim, idxBota)

        var alfa = alfaPuv
        var beta = betaPuv
        if (sim.naTahu == idxBota) {
            var nej = Int.MIN_VALUE / 2
            for (k in povolene) {
                val h = alphaBetaLizaci(simZahrajLizaci(sim, k), alfa, beta, idxBota, citac)
                if (h > nej) nej = h
                if (nej > alfa) alfa = nej
                if (alfa >= beta) break
            }
            return nej
        } else {
            var nej = Int.MAX_VALUE / 2
            for (k in povolene) {
                val h = alphaBetaLizaci(simZahrajLizaci(sim, k), alfa, beta, idxBota, citac)
                if (h < nej) nej = h
                if (nej < beta) beta = nej
                if (alfa >= beta) break
            }
            return nej
        }
    }

    private fun provedTahLizaci(stav: StavMarias): StavMarias {
        val idx = stav.naTahu
        if (MariasEngine.muzeVymenitTrumfovouSedmu(stav, idx)) {
            val otoceny = stav.otocenyTrumf
            if (otoceny != null && otoceny.hodnota != HodnotaKarty.R7 && otoceny.hodnota != HodnotaKarty.R8) {
                return MariasEngine.vymenTrumfovouSedmu(stav, idx)
            }
        }

        val povolene = MariasEngine.zjistiPovoleneKarty(stav, idx).toList()
        if (povolene.isEmpty()) return stav
        if (povolene.size == 1) return MariasEngine.zahrajKartu(stav, idx, povolene.first())

        val ruka = stav.ruceHracu[idx]
        val trumf = stav.trumf
        val trumfSedma = trumf?.let { Karta(it, HodnotaKarty.R7) }

        // Koncovka Lízacího mariáše bez talonu (posledních 5 štychů): karty soupeře jsou z počítání 100% známé -> přesný Minimax!
        if (stav.lizaciBalicek.isEmpty()) {
            val zname = HashSet<Karta>(32)
            zname.addAll(stav.odehraneKarty)
            for (t in stav.aktualniStych) zname.add(t.karta)
            zname.addAll(ruka)
            val souperRuka = VSECHNY_KARTY_32.filterNot { it in zname }
            val ruceSim = if (idx == 0) listOf(ruka, souperRuka) else listOf(souperRuka, ruka)

            val fh = stav.indexForhonta
            val st0 = if (fh == 0) stav.pocetStychuForhont else stav.pocetStychuObrana
            val st1 = if (fh == 1) stav.pocetStychuForhont else stav.pocetStychuObrana
            val b0 = if (fh == 0) stav.bodyKaretForhont else stav.bodyKaretObrana
            val b1 = if (fh == 1) stav.bodyKaretForhont else stav.bodyKaretObrana
            val h0 = if (fh == 0) stav.hlaskyForhont else stav.hlaskyObrana
            val h1 = if (fh == 1) stav.hlaskyForhont else stav.hlaskyObrana

            val korenSim = SimLizaciEndgame(
                trumf = trumf,
                ruce = ruceSim,
                stych = stav.aktualniStych,
                naTahu = idx,
                stychy0 = st0,
                stychy1 = st1,
                body0 = b0,
                body1 = b1,
                hlasky0 = h0,
                hlasky1 = h1,
                tichaSedmaStav = 0,
                jeKonec = false
            )

            var nejKarta = povolene.first()
            var nejHodnota = Int.MIN_VALUE
            for (k in povolene) {
                val poAkci = simZahrajLizaci(korenSim, k)
                val citac = CitacUzluMarias(15_000)
                val hod = alphaBetaLizaci(poAkci, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, idx, citac)
                if (hod > nejHodnota) {
                    nejHodnota = hod
                    nejKarta = k
                }
            }
            return MariasEngine.zahrajKartu(stav, idx, nejKarta)
        }

        // První fáze Lízacího mariáše (s talonem): šetři trumfy a bodované karty, ber soupeřovy desítky a esa
        if (stav.aktualniStych.isEmpty()) {
            val bezpecnaHlaska = povolene.firstOrNull {
                soucastHlasky(ruka, it) && (it.barva == trumf || it.hodnota == HodnotaKarty.SVRSEK)
            }
            if (bezpecnaHlaska != null) return MariasEngine.zahrajKartu(stav, idx, bezpecnaHlaska)

            val levnaVedlejsi = povolene
                .filter { it.barva != trumf && it.hodnota.bodyMariasSedma == 0 && !soucastHlasky(ruka, it) }
                .minByOrNull { it.hodnota.silaMarias }
            if (levnaVedlejsi != null) return MariasEngine.zahrajKartu(stav, idx, levnaVedlejsi)

            val nulova = povolene
                .filter { it.hodnota.bodyMariasSedma == 0 && it != trumfSedma }
                .minByOrNull { (if (it.barva == trumf) 20 else 0) + it.hodnota.silaMarias }
            return MariasEngine.zahrajKartu(stav, idx, nulova ?: povolene.minBy { it.hodnota.silaMarias })
        } else {
            val vynesena = stav.aktualniStych.first().karta
            val vitezne = povolene.filter { k ->
                val test = stav.aktualniStych + TahStychuMariase(idx, "", k)
                MariasEngine.urciVitezeStychu(test, trumf, false).indexHrace == idx
            }
            if (vynesena.hodnota.bodyMariasSedma >= 10 && vitezne.isNotEmpty()) {
                val vBarveVyssi = vitezne.filter { it.barva == vynesena.barva }
                    .maxByOrNull { it.hodnota.bodyMariasSedma * 10 - it.hodnota.silaMarias }
                if (vBarveVyssi != null) return MariasEngine.zahrajKartu(stav, idx, vBarveVyssi)

                val trumfSeber = vitezne
                    .filter { it != trumfSedma && !soucastHlasky(ruka, it) }
                    .minByOrNull { it.hodnota.silaMarias }
                    ?: vitezne.filterNot { it == trumfSedma }.minByOrNull { it.hodnota.silaMarias }
                    ?: vitezne.first()
                return MariasEngine.zahrajKartu(stav, idx, trumfSeber)
            }

            val ziskBezTrumfu = vitezne.firstOrNull {
                it.barva == vynesena.barva && it.barva != trumf && it.hodnota.bodyMariasSedma >= 10
            }
            if (ziskBezTrumfu != null) return MariasEngine.zahrajKartu(stav, idx, ziskBezTrumfu)

            val odhoz = povolene
                .filter { it.barva != trumf && it.hodnota.bodyMariasSedma == 0 && !soucastHlasky(ruka, it) }
                .minByOrNull { it.hodnota.silaMarias }
                ?: povolene.minWithOrNull(
                    compareBy<Karta>(
                        { if (it.barva == trumf) 1 else 0 },
                        { it.hodnota.bodyMariasSedma },
                        { if (soucastHlasky(ruka, it)) 1 else 0 },
                        { it.hodnota.silaMarias }
                    )
                ) ?: povolene.first()
            return MariasEngine.zahrajKartu(stav, idx, odhoz)
        }
    }
}
