package cz.mares.ceskekarty

import kotlin.math.abs

internal fun sklonujStychyMariase(pocet: Int): String {
    return when {
        pocet == 1 -> "1 štych"
        pocet in 2..4 -> "$pocet štychy"
        else -> "$pocet štychů"
    }
}

object MariasVariantyEngine {

    // =========================================================================
    // 1. LICITOVANÝ MARIÁŠ PRO 3 HRÁČE (dražba o talon a volba trumfu z 12 karet)
    // =========================================================================

    fun novaHraLicitovany(
        jmena: List<String>,
        startForhont: Int,
        balicek: MutableList<Karta>,
        variantaPravidel: VariantaPravidel
    ): StavMarias {
        val ruka0 = ArrayList<Karta>(10)
        val ruka1 = ArrayList<Karta>(10)
        val ruka2 = ArrayList<Karta>(10)
        repeat(10) { if (balicek.isNotEmpty()) ruka0.add(balicek.removeAt(0)) }
        repeat(10) { if (balicek.isNotEmpty()) ruka1.add(balicek.removeAt(0)) }
        repeat(10) { if (balicek.isNotEmpty()) ruka2.add(balicek.removeAt(0)) }

        val skrytyTalon = ArrayList<Karta>(2)
        while (balicek.isNotEmpty() && skrytyTalon.size < 2) {
            skrytyTalon.add(balicek.removeAt(0))
        }

        val ruce = listOf(
            Karta.seradRuku(ruka0),
            Karta.seradRuku(ruka1),
            Karta.seradRuku(ruka2)
        )
        val rezimPravidelText = variantaPravidel.kratkyNazev.lowercase()

        return StavMarias(
            jmenaHracu = jmena,
            ruceHracu = ruce,
            indexForhonta = startForhont,
            naTahu = startForhont,
            faze = FazeMariase.DRAZBA,
            trumf = null,
            volenoZTalonu = false,
            zavazek = ZavazekMariase.HRA,
            zbytekProForhonta = skrytyTalon,
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
            posledniHlaska = "Licitovaný mariáš ($rezimPravidelText) — dražbu zahajuje ${jmena[startForhont]}.",
            cisloTahu = 1,
            variantaPravidel = variantaPravidel,
            posledniUzavrenyStych = emptyList(),
            posledniUzavrenyVitez = -1,
            rezimMariase = RezimMariase.LICITOVANY,
            pocetHracu = 3,
            puvodniForhont = startForhont,
            nejvyssiNabidka = null,
            prozatimniVitezDrazby = -1,
            pasovaliHraci = listOf(false, false, false),
            slozenoBezHry = false,
            lizaciBalicek = emptyList(),
            otocenyTrumf = null
        )
    }

    fun zjistiPovoleneNabidkyDrazby(stav: StavMarias, indexHrace: Int): List<ZavazekMariase> {
        if (stav.faze != FazeMariase.DRAZBA || stav.naTahu != indexHrace) return emptyList()
        if (stav.pasovaliHraci.getOrElse(indexHrace) { false }) return emptyList()

        val minVaha = (stav.nejvyssiNabidka?.vahaDrazby ?: 0) + 1
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptyList()
        val pocetSedem = ruka.count { it.hodnota == HodnotaKarty.R7 }

        return ZavazekMariase.poradiDrazby.filter { zav ->
            if (zav.vahaDrazby < minVaha) return@filter false
            if (stav.variantaPravidel == VariantaPravidel.SVAZOVA) {
                when {
                    zav.hlasiDveSedmy -> pocetSedem >= 2
                    zav.hlasiSedmu -> pocetSedem >= 1
                    else -> true
                }
            } else {
                true
            }
        }
    }

    fun provedNabidkuDrazby(
        stav: StavMarias,
        indexHrace: Int,
        nabidka: ZavazekMariase?
    ): StavMarias {
        if (stav.faze != FazeMariase.DRAZBA || stav.naTahu != indexHrace) return stav
        val jmeno = stav.jmenaHracu.getOrElse(indexHrace) { "Hráč" }

        if (nabidka == null) {
            val novePasy = stav.pasovaliHraci.mapIndexed { idx, pas ->
                if (idx == indexHrace) true else pas
            }
            val pocetPasu = novePasy.count { it }

            if (pocetPasu >= 3 && stav.nejvyssiNabidka == null) {
                return stav.copy(
                    pasovaliHraci = novePasy,
                    slozenoBezHry = true,
                    faze = FazeMariase.KONEC,
                    posledniHlaska = "$jmeno říká Dál — všichni pasovali, rozdání se skládá bez hry.",
                    cisloTahu = stav.cisloTahu + 1
                )
            }

            if (pocetPasu >= 2 && stav.nejvyssiNabidka != null && stav.prozatimniVitezDrazby in 0..2) {
                return ukonciDrazbuAPredejTalon(
                    stav = stav.copy(pasovaliHraci = novePasy),
                    vitezIdx = stav.prozatimniVitezDrazby,
                    vydrazenyZavazek = stav.nejvyssiNabidka,
                    uvodHlasky = "$jmeno říká Dál — "
                )
            }

            val dalsi = najdiDalsihoAktivnihoVDrazbe(indexHrace, novePasy)
            return stav.copy(
                pasovaliHraci = novePasy,
                naTahu = dalsi,
                posledniHlaska = if (stav.nejvyssiNabidka != null) {
                    "$jmeno říká Dál. Nejvyšší nabídka: ${stav.nejvyssiNabidka.nazev} (${stav.jmenaHracu[stav.prozatimniVitezDrazby]})."
                } else {
                    "$jmeno říká Dál — vyjadřuje se ${stav.jmenaHracu[dalsi]}."
                },
                cisloTahu = stav.cisloTahu + 1
            )
        }

        val povolene = zjistiPovoleneNabidkyDrazby(stav, indexHrace)
        if (nabidka !in povolene) return stav

        val pasovaliOstatniDva = stav.pasovaliHraci.filterIndexed { i, _ -> i != indexHrace }.all { it }
        val jeMaximalniNabidka = nabidka == ZavazekMariase.poradiDrazby.last()

        if (pasovaliOstatniDva || jeMaximalniNabidka) {
            return ukonciDrazbuAPredejTalon(
                stav = stav.copy(
                    nejvyssiNabidka = nabidka,
                    prozatimniVitezDrazby = indexHrace
                ),
                vitezIdx = indexHrace,
                vydrazenyZavazek = nabidka,
                uvodHlasky = "$jmeno hlásí ${nabidka.nazev} — "
            )
        }

        val dalsi = najdiDalsihoAktivnihoVDrazbe(indexHrace, stav.pasovaliHraci)
        return stav.copy(
            nejvyssiNabidka = nabidka,
            prozatimniVitezDrazby = indexHrace,
            naTahu = dalsi,
            posledniHlaska = "$jmeno nabízí ${nabidka.nazev}! Vyjadřuje se ${stav.jmenaHracu[dalsi]}.",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    private fun najdiDalsihoAktivnihoVDrazbe(aktualni: Int, pasovali: List<Boolean>): Int {
        for (krok in 1..3) {
            val idx = (aktualni + krok) % 3
            if (!pasovali.getOrElse(idx) { false }) return idx
        }
        return aktualni
    }

    private fun ukonciDrazbuAPredejTalon(
        stav: StavMarias,
        vitezIdx: Int,
        vydrazenyZavazek: ZavazekMariase,
        uvodHlasky: String
    ): StavMarias {
        val vitezJmeno = stav.jmenaHracu.getOrElse(vitezIdx) { "Forhont" }
        val spojenaRuka = stav.ruceHracu[vitezIdx] + stav.zbytekProForhonta

        return if (vydrazenyZavazek.jeBezTrumfu) {
            val serazena = Karta.seradRuku(spojenaRuka, trumf = null, betlNeboDurch = true)
            val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == vitezIdx) serazena else r }
            stav.copy(
                ruceHracu = noveRuce,
                indexForhonta = vitezIdx,
                naTahu = vitezIdx,
                faze = FazeMariase.ODKLADANI_TALONU,
                trumf = null,
                volenoZTalonu = false,
                zavazek = vydrazenyZavazek,
                zbytekProForhonta = emptyList(),
                nejvyssiNabidka = vydrazenyZavazek,
                prozatimniVitezDrazby = vitezIdx,
                posledniHlaska = "${uvodHlasky}$vitezJmeno vyhrál dražbu (${vydrazenyZavazek.nazev}), bere talon a odkládá 2 karty.",
                cisloTahu = stav.cisloTahu + 1
            )
        } else {
            val serazena = Karta.seradRuku(spojenaRuka, trumf = null, betlNeboDurch = false)
            val noveRuce = stav.ruceHracu.mapIndexed { idx, r -> if (idx == vitezIdx) serazena else r }
            stav.copy(
                ruceHracu = noveRuce,
                indexForhonta = vitezIdx,
                naTahu = vitezIdx,
                faze = FazeMariase.VOLBA_TRUMFU,
                trumf = null,
                volenoZTalonu = false,
                zavazek = vydrazenyZavazek,
                zbytekProForhonta = emptyList(),
                nejvyssiNabidka = vydrazenyZavazek,
                prozatimniVitezDrazby = vitezIdx,
                posledniHlaska = "${uvodHlasky}$vitezJmeno vyhrál dražbu (${vydrazenyZavazek.nazev}), bere talon a volí trumf z 12 karet.",
                cisloTahu = stav.cisloTahu + 1
            )
        }
    }

    fun zjistiPovoleneTrumfyPoDrazbe(stav: StavMarias): Set<BarvaKarty> {
        if (stav.rezimMariase != RezimMariase.LICITOVANY || stav.nejvyssiNabidka == null) {
            return BarvaKarty.entries.toSet()
        }
        val minVaha = stav.nejvyssiNabidka.vahaDrazby
        if (stav.variantaPravidel == VariantaPravidel.HOSPODSKA || minVaha <= ZavazekMariase.STO.vahaDrazby) {
            return BarvaKarty.entries.toSet()
        }
        val ruka = stav.ruceHracu.getOrNull(stav.indexForhonta) ?: return BarvaKarty.entries.toSet()
        val pocetSedem = ruka.count { it.hodnota == HodnotaKarty.R7 }
        val vhodne = BarvaKarty.entries.filter { barva ->
            val maTrumf7 = Karta(barva, HodnotaKarty.R7) in ruka
            when {
                minVaha >= ZavazekMariase.HRA_A_DVE_SEDMY.vahaDrazby -> maTrumf7 && pocetSedem >= 2
                else -> maTrumf7
            }
        }.toSet()
        return vhodne.ifEmpty { BarvaKarty.entries.toSet() }
    }

    fun muzeVolitRovnouBetl(stav: StavMarias): Boolean {
        if (stav.rezimMariase != RezimMariase.LICITOVANY) return true
        val minVaha = stav.nejvyssiNabidka?.vahaDrazby ?: 1
        return ZavazekMariase.BETL.vahaDrazby >= minVaha
    }

    fun muzeVolitRovnouDurch(stav: StavMarias): Boolean {
        if (stav.rezimMariase != RezimMariase.LICITOVANY) return true
        val minVaha = stav.nejvyssiNabidka?.vahaDrazby ?: 1
        return ZavazekMariase.DURCH.vahaDrazby >= minVaha
    }

    private fun jeRukaNaDurch(ruka: List<Karta>): Boolean {
        val pocetEs = ruka.count { it.hodnota == HodnotaKarty.ESO }
        for (barva in BarvaKarty.entries) {
            val vBarve = ruka.filter { it.barva == barva }
            val maEso = vBarve.any { it.hodnota == HodnotaKarty.ESO }
            val maKral = vBarve.any { it.hodnota == HodnotaKarty.KRAL }
            val maSvrsek = vBarve.any { it.hodnota == HodnotaKarty.SVRSEK }
            if (vBarve.size >= 6 && maEso && maKral && maSvrsek && pocetEs >= 3) {
                return true
            }
        }
        return pocetEs == 4 && ruka.count { it.hodnota == HodnotaKarty.KRAL } >= 3
    }

    private fun jeRukaNaBetl(ruka: List<Karta>): Boolean {
        val vysoke = ruka.count {
            it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.KRAL
        }
        if (vysoke > 0) return false
        var rizikove = 0
        for (barva in BarvaKarty.entries) {
            val vBarve = ruka.filter { it.barva == barva }
            if (vBarve.isEmpty()) continue
            val ma7 = vBarve.any { it.hodnota == HodnotaKarty.R7 }
            val ma8 = vBarve.any { it.hodnota == HodnotaKarty.R8 }
            val maSvrskaNebo10 = vBarve.count {
                it.hodnota == HodnotaKarty.SVRSEK || it.hodnota == HodnotaKarty.R10
            }
            if (maSvrskaNebo10 > 0 && !(ma7 && ma8)) {
                rizikove += maSvrskaNebo10
            } else if (!ma7 && !ma8 && vBarve.size <= 2) {
                rizikove++
            }
        }
        return rizikove <= 1
    }

    private fun ohodnotStropRukyProDrazbu(
        ruka: List<Karta>,
        variantaPravidel: VariantaPravidel
    ): ZavazekMariase? {
        if (jeRukaNaDurch(ruka)) return ZavazekMariase.DURCH
        if (jeRukaNaBetl(ruka)) return ZavazekMariase.BETL

        var nejlepsi: ZavazekMariase? = null
        val pocetSedemCelkem = ruka.count { it.hodnota == HodnotaKarty.R7 }

        for (barva in BarvaKarty.entries) {
            val trumfy = ruka.filter { it.barva == barva }
            if (trumfy.size < 4) continue

            val trumfBody = trumfy.count {
                it.hodnota == HodnotaKarty.ESO || it.hodnota == HodnotaKarty.R10
            }
            val vedlejsiEsa = ruka.count {
                it.barva != barva && it.hodnota == HodnotaKarty.ESO
            }
            val kryteVedlejsi10 = ruka.count { k ->
                k.barva != barva && k.hodnota == HodnotaKarty.R10 &&
                    ruka.any { it.barva == k.barva && it.hodnota == HodnotaKarty.ESO }
            }
            val hl40 = if (
                Karta(barva, HodnotaKarty.SVRSEK) in ruka &&
                Karta(barva, HodnotaKarty.KRAL) in ruka
            ) 40 else 0
            val hl20 = BarvaKarty.entries.count { vb ->
                vb != barva && Karta(vb, HodnotaKarty.SVRSEK) in ruka && Karta(vb, HodnotaKarty.KRAL) in ruka
            } * 20
            val maTrumf7 = Karta(barva, HodnotaKarty.R7) in ruka

            val odhadSily = trumfy.size * 9 +
                trumfBody * 9 +
                vedlejsiEsa * 12 +
                kryteVedlejsi10 * 9 +
                hl40 + hl20 + 10

            val kandidat = when {
                trumfy.size >= 5 && maTrumf7 && pocetSedemCelkem >= 2 && odhadSily >= 106 ->
                    ZavazekMariase.STO_A_DVE_SEDMY
                trumfy.size >= 5 && maTrumf7 && pocetSedemCelkem >= 2 && odhadSily >= 86 ->
                    ZavazekMariase.HRA_A_DVE_SEDMY
                trumfy.size >= 5 && maTrumf7 && odhadSily >= 102 ->
                    ZavazekMariase.STO_A_SEDMA
                trumfy.size >= 4 && odhadSily >= 98 ->
                    ZavazekMariase.STO
                trumfy.size >= 5 && maTrumf7 && odhadSily >= 76 ->
                    ZavazekMariase.HRA_A_SEDMA
                trumfy.size >= 4 && odhadSily >= 66 ->
                    ZavazekMariase.HRA
                variantaPravidel == VariantaPravidel.HOSPODSKA && trumfy.size >= 4 && odhadSily >= 62 ->
                    ZavazekMariase.HRA
                else -> null
            }

            if (kandidat != null && (nejlepsi == null || kandidat.vahaDrazby > nejlepsi.vahaDrazby)) {
                nejlepsi = kandidat
            }
        }
        return nejlepsi
    }

    fun provedTahDrazbyBota(stav: StavMarias): StavMarias {
        val idx = stav.naTahu
        val povolene = zjistiPovoleneNabidkyDrazby(stav, idx)
        if (povolene.isEmpty()) {
            return provedNabidkuDrazby(stav, idx, null)
        }
        val ruka = stav.ruceHracu.getOrNull(idx) ?: return provedNabidkuDrazby(stav, idx, null)
        val strop = ohodnotStropRukyProDrazbu(ruka, stav.variantaPravidel)
            ?: return provedNabidkuDrazby(stav, idx, null)

        val dosazitelne = povolene.filter { it.vahaDrazby <= strop.vahaDrazby }
        if (dosazitelne.isEmpty()) {
            return provedNabidkuDrazby(stav, idx, null)
        }

        val nabidka = if (strop.jeBezTrumfu && strop in dosazitelne && stav.nejvyssiNabidka != null) {
            dosazitelne.first()
        } else {
            dosazitelne.first()
        }
        return provedNabidkuDrazby(stav, idx, nabidka)
    }

    fun zvolTrumfPoDrazbeBot(stav: StavMarias): StavMarias {
        val idx = stav.indexForhonta
        val ruka12 = stav.ruceHracu.getOrNull(idx) ?: return stav
        val vydrazeno = stav.nejvyssiNabidka ?: ZavazekMariase.HRA

        if (muzeVolitRovnouDurch(stav) && jeRukaNaDurch(ruka12.take(10))) {
            return MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, null, false, ZavazekMariase.DURCH)
        }
        if (muzeVolitRovnouBetl(stav) && vydrazeno.vahaDrazby <= ZavazekMariase.BETL.vahaDrazby && jeRukaNaBetl(ruka12.drop(2))) {
            return MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, null, false, ZavazekMariase.BETL)
        }

        val povoleneBarvy = zjistiPovoleneTrumfyPoDrazbe(stav)
        val nejlepsiBarva = povoleneBarvy.maxByOrNull { b ->
            val vBarve = ruka12.filter { it.barva == b }
            val hl40 = if (
                Karta(b, HodnotaKarty.SVRSEK) in ruka12 &&
                Karta(b, HodnotaKarty.KRAL) in ruka12
            ) 25 else 0
            val ma7 = if (Karta(b, HodnotaKarty.R7) in ruka12) 10 else 0
            val cerveneBonus = if (b == BarvaKarty.CERVENE) 6 else 0
            vBarve.size * 14 + vBarve.sumOf { it.hodnota.silaMarias } + hl40 + ma7 + cerveneBonus
        } ?: povoleneBarvy.first()

        return MariasEngine.zvolTrumfNeboHruBezTrumfu(stav, nejlepsiBarva, false, vydrazeno)
    }

    fun vyhodnotSlozenouDrazbu(stav: StavMarias): VyuctovaniMariase {
        val polozky = listOf(
            PolozkaVyuctovaniMariase(
                popis = "Průběh dražby",
                hodnotaText = "Všichni 3 hráči dali Dál",
                jeZvyraznena = true
            ),
            PolozkaVyuctovaniMariase(
                popis = "Vyúčtování rozdání",
                hodnotaText = "0 tereziánů (smír bez hry)",
                jeZvyraznena = false
            )
        )
        val hraci = stav.jmenaHracu.take(3).mapIndexed { idx, jm ->
            HracVyuctovaniMariase(
                indexHrace = idx,
                jmenoHrace = jm,
                jeVitez = false,
                bodyZapasu = 0,
                zmenaTerezianu = 0
            )
        }
        return VyuctovaniMariase(
            nadpis = "Složeno bez hry",
            podnadpis = "Licitovaný mariáš • ${stav.variantaPravidel.kratkyNazev} pravidla",
            polozky = polozky,
            hraci = hraci
        )
    }

    // =========================================================================
    // 2. LÍZACÍ MARIÁŠ PRO 2 HRÁČE (5 karet, otočený trumf, volné lízání -> přísnost)
    // =========================================================================

    fun novaHraLizaci(
        jmena: List<String>,
        startForhont: Int,
        balicek: MutableList<Karta>,
        variantaPravidel: VariantaPravidel
    ): StavMarias {
        val ruka0 = ArrayList<Karta>(5)
        val ruka1 = ArrayList<Karta>(5)
        repeat(5) { if (balicek.isNotEmpty()) ruka0.add(balicek.removeAt(0)) }
        repeat(5) { if (balicek.isNotEmpty()) ruka1.add(balicek.removeAt(0)) }

        val lizaci = balicek.toList()
        val otoceny = lizaci.lastOrNull()
        val trumfBarva = otoceny?.barva ?: BarvaKarty.CERVENE
        val vynasejici = startForhont.mod(2)

        val ruce = listOf(
            Karta.seradRuku(ruka0, trumf = trumfBarva, betlNeboDurch = false),
            Karta.seradRuku(ruka1, trumf = trumfBarva, betlNeboDurch = false)
        )
        val popisOtocene = otoceny?.celyNazev() ?: trumfBarva.ceskyNazev

        return StavMarias(
            jmenaHracu = jmena.take(2),
            ruceHracu = ruce,
            indexForhonta = vynasejici,
            naTahu = vynasejici,
            faze = FazeMariase.SEHRAVKA,
            trumf = trumfBarva,
            volenoZTalonu = false,
            zavazek = ZavazekMariase.HRA,
            zbytekProForhonta = emptyList(),
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
            posledniHlaska = "Lízací mariáš! Otočený trumf: $popisOtocene — vynáší ${jmena[vynasejici]}.",
            cisloTahu = 1,
            variantaPravidel = variantaPravidel,
            posledniUzavrenyStych = emptyList(),
            posledniUzavrenyVitez = -1,
            rezimMariase = RezimMariase.LIZACI,
            pocetHracu = 2,
            puvodniForhont = vynasejici,
            nejvyssiNabidka = null,
            prozatimniVitezDrazby = -1,
            pasovaliHraci = listOf(false, false),
            slozenoBezHry = false,
            lizaciBalicek = lizaci,
            otocenyTrumf = otoceny
        )
    }

    fun muzeVymenitTrumfovouSedmu(stav: StavMarias, indexHrace: Int): Boolean {
        if (stav.rezimMariase != RezimMariase.LIZACI || stav.faze != FazeMariase.SEHRAVKA) return false
        if (stav.naTahu != indexHrace || stav.aktualniStych.isEmpty().not()) return false
        val minVelikostTalonu = if (stav.variantaPravidel == VariantaPravidel.SVAZOVA) 4 else 2
        if (stav.lizaciBalicek.size < minVelikostTalonu) return false
        val otoceny = stav.otocenyTrumf ?: return false
        if (otoceny.hodnota == HodnotaKarty.R7) return false
        val trumfBarva = stav.trumf ?: return false
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return false
        return Karta(trumfBarva, HodnotaKarty.R7) in ruka
    }

    fun vymenTrumfovouSedmu(stav: StavMarias, indexHrace: Int): StavMarias {
        if (!muzeVymenitTrumfovouSedmu(stav, indexHrace)) return stav
        val trumfBarva = stav.trumf ?: return stav
        val otoceny = stav.otocenyTrumf ?: return stav
        val trumfSedma = Karta(trumfBarva, HodnotaKarty.R7)

        val upravenaRuka = Karta.seradRuku(
            stav.ruceHracu[indexHrace].filterNot { it == trumfSedma } + otoceny,
            trumf = trumfBarva,
            betlNeboDurch = false
        )
        val noveRuce = stav.ruceHracu.mapIndexed { idx, r ->
            if (idx == indexHrace) upravenaRuka else r
        }
        val novyLizaci = stav.lizaciBalicek.dropLast(1) + trumfSedma
        val jmeno = stav.jmenaHracu.getOrElse(indexHrace) { "Hráč" }

        return stav.copy(
            ruceHracu = noveRuce,
            lizaciBalicek = novyLizaci,
            otocenyTrumf = trumfSedma,
            posledniUzavrenyStych = emptyList(),
            posledniUzavrenyVitez = -1,
            posledniHlaska = "$jmeno mění trumfovou sedmu za ${otoceny.celyNazev()}!",
            cisloTahu = stav.cisloTahu + 1
        )
    }

    fun zjistiPovoleneKartyLizaci(stav: StavMarias, indexHrace: Int): Set<Karta> {
        if (stav.faze != FazeMariase.SEHRAVKA || stav.naTahu != indexHrace) return emptySet()
        val ruka = stav.ruceHracu.getOrNull(indexHrace) ?: return emptySet()
        if (ruka.isEmpty()) return emptySet()

        // Dokud v talonu leží karty k lízání, nemusí se přiznávat barva ani trumfovat
        if (stav.lizaciBalicek.isNotEmpty() || stav.aktualniStych.isEmpty()) {
            return ruka.toSet()
        }

        // Posledních 5 štychů po dobrání talonu: přísná povinnost přiznat barvu, přebít či trumfovat
        val vynesena = stav.aktualniStych.first().karta
        val vBarve = ruka.filter { it.barva == vynesena.barva }
        if (vBarve.isNotEmpty()) {
            val vyssi = vBarve.filter { it.hodnota.silaMarias > vynesena.hodnota.silaMarias }
            return (if (vyssi.isNotEmpty()) vyssi else vBarve).toSet()
        }

        val trumf = stav.trumf
        if (trumf != null) {
            val trumfyVRuce = ruka.filter { it.barva == trumf }
            if (trumfyVRuce.isNotEmpty()) {
                return trumfyVRuce.toSet()
            }
        }

        return ruka.toSet()
    }

    private fun urciVitezeDvojStychu(
        prvni: TahStychuMariase,
        druhy: TahStychuMariase,
        trumf: BarvaKarty?
    ): Int {
        return when {
            druhy.karta.barva == prvni.karta.barva -> {
                if (druhy.karta.hodnota.silaMarias > prvni.karta.hodnota.silaMarias) {
                    druhy.indexHrace
                } else {
                    prvni.indexHrace
                }
            }
            trumf != null && druhy.karta.barva == trumf && prvni.karta.barva != trumf ->
                druhy.indexHrace
            else ->
                prvni.indexHrace
        }
    }

    fun zahrajKartuLizaci(stav: StavMarias, indexHrace: Int, karta: Karta): StavMarias {
        val povolene = zjistiPovoleneKartyLizaci(stav, indexHrace)
        if (karta !in povolene) return stav

        val jmeno = stav.jmenaHracu.getOrElse(indexHrace) { "Hráč" }
        val zbylaRuka = Karta.seradRuku(
            stav.ruceHracu[indexHrace].filterNot { it == karta },
            trumf = stav.trumf,
            betlNeboDurch = false
        )
        val rucePoOdehrani = stav.ruceHracu.mapIndexed { idx, r ->
            if (idx == indexHrace) zbylaRuka else r
        }

        var noveHlaskyForhont = stav.hlaskyForhont
        var noveHlaskyObrana = stav.hlaskyObrana
        var hlaskaZprava = ""

        if (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL) {
            val hledana = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
            if (zbylaRuka.any { it.barva == karta.barva && it.hodnota == hledana }) {
                val bodyHlasky = if (karta.barva == stav.trumf) 40 else 20
                if (indexHrace == stav.indexForhonta) {
                    noveHlaskyForhont += bodyHlasky
                } else {
                    noveHlaskyObrana += bodyHlasky
                }
                hlaskaZprava = " (+hláška $bodyHlasky!)"
            }
        }

        val novyStych = stav.aktualniStych + TahStychuMariase(indexHrace, jmeno, karta)

        if (novyStych.size == 1) {
            val druhyHrac = 1 - indexHrace
            return stav.copy(
                ruceHracu = rucePoOdehrani,
                aktualniStych = novyStych,
                naTahu = druhyHrac,
                hlaskyForhont = noveHlaskyForhont,
                hlaskyObrana = noveHlaskyObrana,
                posledniUzavrenyStych = emptyList(),
                posledniUzavrenyVitez = -1,
                posledniHlaska = "$jmeno vynáší ${karta.celyNazev()}$hlaskaZprava.",
                cisloTahu = stav.cisloTahu + 1
            )
        }

        val vitez = urciVitezeDvojStychu(novyStych[0], novyStych[1], stav.trumf)
        val porazeny = 1 - vitez
        val bodyStychu = novyStych.sumOf { it.karta.hodnota.bodyMariasSedma }

        val ruceMut = rucePoOdehrani.map { it.toMutableList() }
        var novyLizaci = stav.lizaciBalicek
        var novyOtoceny = stav.otocenyTrumf
        val bylTalonPredDobranim = novyLizaci.isNotEmpty()

        if (novyLizaci.size >= 2) {
            ruceMut[vitez].add(novyLizaci[0])
            ruceMut[porazeny].add(novyLizaci[1])
            novyLizaci = novyLizaci.drop(2)
            novyOtoceny = novyLizaci.lastOrNull()
        }

        val serazeneRuce = ruceMut.map {
            Karta.seradRuku(it, trumf = stav.trumf, betlNeboDurch = false)
        }
        val jePosledniStych = novyLizaci.isEmpty() && serazeneRuce.all { it.isEmpty() }
        val bonusPosledni = if (jePosledniStych) 10 else 0

        val pripisForhont = vitez == stav.indexForhonta
        val noveBodyForhont = stav.bodyKaretForhont + if (pripisForhont) bodyStychu + bonusPosledni else 0
        val noveBodyObrana = stav.bodyKaretObrana + if (!pripisForhont) bodyStychu + bonusPosledni else 0
        val noveStychyForhont = stav.pocetStychuForhont + if (pripisForhont) 1 else 0
        val noveStychyObrana = stav.pocetStychuObrana + if (!pripisForhont) 1 else 0

        var novyStavPosledniSedmy = stav.stavPoslednihoStychuSedma
        if (jePosledniStych && stav.trumf != null) {
            val trumfSedma = Karta(stav.trumf, HodnotaKarty.R7)
            val tahSedmy = novyStych.firstOrNull { it.karta == trumfSedma }
            if (tahSedmy != null) {
                novyStavPosledniSedmy = if (tahSedmy.indexHrace == vitez) {
                    if (vitez == stav.indexForhonta) 1 else 2
                } else {
                    if (tahSedmy.indexHrace == stav.indexForhonta) -1 else -2
                }
            }
        }

        val jmenoViteze = stav.jmenaHracu.getOrElse(vitez) { "Hráč" }
        val zaverecnaHlaska = when {
            jePosledniStych ->
                "$jmenoViteze bere poslední 16. štych (+10 b.)$hlaskaZprava! Konec rozdání."
            bylTalonPredDobranim && novyLizaci.isEmpty() ->
                "$jmenoViteze bere štych (${bodyStychu} b.)$hlaskaZprava — talon je dobraný, nyní se přiznává barva a trumfuje!"
            else ->
                "$jmenoViteze bere štych (${bodyStychu} b.)$hlaskaZprava a vynáší."
        }

        return stav.copy(
            ruceHracu = serazeneRuce,
            aktualniStych = emptyList(),
            naTahu = vitez,
            cisloStychu = if (jePosledniStych) stav.cisloStychu else stav.cisloStychu + 1,
            pocetStychuForhont = noveStychyForhont,
            pocetStychuObrana = noveStychyObrana,
            bodyKaretForhont = noveBodyForhont,
            bodyKaretObrana = noveBodyObrana,
            hlaskyForhont = noveHlaskyForhont,
            hlaskyObrana = noveHlaskyObrana,
            stavPoslednihoStychuSedma = novyStavPosledniSedmy,
            lizaciBalicek = novyLizaci,
            otocenyTrumf = novyOtoceny,
            faze = if (jePosledniStych) FazeMariase.KONEC else FazeMariase.SEHRAVKA,
            posledniUzavrenyStych = novyStych,
            posledniUzavrenyVitez = vitez,
            posledniHlaska = zaverecnaHlaska,
            cisloTahu = stav.cisloTahu + 1
        )
    }

    private fun soucastHlaskyVRuce(ruka: List<Karta>, karta: Karta): Boolean {
        if (karta.hodnota != HodnotaKarty.SVRSEK && karta.hodnota != HodnotaKarty.KRAL) return false
        val druha = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
        return ruka.any { it.barva == karta.barva && it.hodnota == druha }
    }

    fun provedTahBotaLizaci(stav: StavMarias): StavMarias {
        val idx = stav.naTahu
        if (muzeVymenitTrumfovouSedmu(stav, idx)) {
            return vymenTrumfovouSedmu(stav, idx)
        }

        val povolene = zjistiPovoleneKartyLizaci(stav, idx).toList()
        if (povolene.isEmpty()) return stav
        val ruka = stav.ruceHracu[idx]
        val trumf = stav.trumf

        // 1. Volná fáze s dobíráním z talonu (šetři trumfy a esa/desítky, sbírej soupeřovy body)
        if (stav.lizaciBalicek.isNotEmpty()) {
            if (stav.aktualniStych.isEmpty()) {
                val hlaskovaKarta = povolene.firstOrNull { k ->
                    soucastHlaskyVRuce(ruka, k) && k.barva != trumf
                } ?: povolene.firstOrNull { k ->
                    soucastHlaskyVRuce(ruka, k)
                }
                if (hlaskovaKarta != null) {
                    return zahrajKartuLizaci(stav, idx, hlaskovaKarta)
                }

                val levneNetrumfy = povolene.filter { k ->
                    k.barva != trumf &&
                        k.hodnota.bodyMariasSedma == 0 &&
                        k.hodnota != HodnotaKarty.KRAL &&
                        k.hodnota != HodnotaKarty.SVRSEK
                }
                if (levneNetrumfy.isNotEmpty()) {
                    val vynes = levneNetrumfy.minByOrNull { it.hodnota.silaMarias } ?: levneNetrumfy.first()
                    return zahrajKartuLizaci(stav, idx, vynes)
                }

                val netrumfBezBodu = povolene.filter { it.barva != trumf && it.hodnota.bodyMariasSedma == 0 }
                if (netrumfBezBodu.isNotEmpty()) {
                    return zahrajKartuLizaci(stav, idx, netrumfBezBodu.minByOrNull { it.hodnota.silaMarias }!!)
                }

                val nejlevnejsi = povolene.minWithOrNull(
                    compareBy<Karta>(
                        { if (it.barva == trumf) 1 else 0 },
                        { it.hodnota.bodyMariasSedma },
                        { it.hodnota.silaMarias }
                    )
                ) ?: povolene.first()
                return zahrajKartuLizaci(stav, idx, nejlevnejsi)
            } else {
                val vynesena = stav.aktualniStych.first().karta
                val jeBodovana = vynesena.hodnota.bodyMariasSedma >= 10

                if (jeBodovana) {
                    val vyssiStejneBarvy = povolene
                        .filter { it.barva == vynesena.barva && it.hodnota.silaMarias > vynesena.hodnota.silaMarias }
                        .minByOrNull { it.hodnota.silaMarias }
                    if (vyssiStejneBarvy != null) {
                        return zahrajKartuLizaci(stav, idx, vyssiStejneBarvy)
                    }
                    if (trumf != null && vynesena.barva != trumf) {
                        val levnyTrumf = povolene
                            .filter { it.barva == trumf && it.hodnota != HodnotaKarty.R7 }
                            .minByOrNull { it.hodnota.silaMarias }
                        if (levnyTrumf != null) {
                            return zahrajKartuLizaci(stav, idx, levnyTrumf)
                        }
                    }
                } else {
                    val levnePrebitiVBarve = povolene.filter { k ->
                        k.barva == vynesena.barva &&
                            k.barva != trumf &&
                            k.hodnota.silaMarias > vynesena.hodnota.silaMarias &&
                            k.hodnota.bodyMariasSedma == 0 &&
                            (soucastHlaskyVRuce(ruka, k) || k.hodnota == HodnotaKarty.SPODEK || k.hodnota == HodnotaKarty.R9)
                    }.minByOrNull { it.hodnota.silaMarias }
                    if (levnePrebitiVBarve != null) {
                        return zahrajKartuLizaci(stav, idx, levnePrebitiVBarve)
                    }
                }

                val odhoz = povolene.minWithOrNull(
                    compareBy<Karta>(
                        { if (it.barva == trumf) 1 else 0 },
                        { it.hodnota.bodyMariasSedma },
                        { if (soucastHlaskyVRuce(ruka, it)) 1 else 0 },
                        { if (it.hodnota == HodnotaKarty.KRAL || it.hodnota == HodnotaKarty.SVRSEK) 1 else 0 },
                        { it.hodnota.silaMarias }
                    )
                ) ?: povolene.first()
                return zahrajKartuLizaci(stav, idx, odhoz)
            }
        }

        // 2. Koncovka posledních 5 štychů po dobrání talonu (přísně na barvu a trumf)
        val trumfSedma = trumf?.let { Karta(it, HodnotaKarty.R7) }
        if (stav.aktualniStych.isEmpty()) {
            val hlaskova = povolene.firstOrNull { soucastHlaskyVRuce(ruka, it) }
            if (hlaskova != null) {
                return zahrajKartuLizaci(stav, idx, hlaskova)
            }
            val bezTrumfoveSedmy = if (trumfSedma != null && povolene.size > 1) {
                povolene.filterNot { it == trumfSedma }
            } else {
                povolene
            }
            val eso = bezTrumfoveSedmy.firstOrNull { it.hodnota == HodnotaKarty.ESO && it.barva != trumf }
                ?: bezTrumfoveSedmy.firstOrNull { it.hodnota == HodnotaKarty.ESO }
            if (eso != null) {
                return zahrajKartuLizaci(stav, idx, eso)
            }
            val vybrana = bezTrumfoveSedmy.maxByOrNull { it.hodnota.silaMarias } ?: povolene.first()
            return zahrajKartuLizaci(stav, idx, vybrana)
        } else {
            val vynesena = stav.aktualniStych.first().karta
            val vitezne = povolene.filter { kandidatka ->
                urciVitezeDvojStychu(
                    stav.aktualniStych.first(),
                    TahStychuMariase(idx, "", kandidatka),
                    trumf
                ) == idx
            }
            if (vitezne.isNotEmpty()) {
                val vybranaVitezna = if (vynesena.hodnota.bodyMariasSedma >= 10) {
                    vitezne.maxByOrNull { it.hodnota.bodyMariasSedma * 10 - it.hodnota.silaMarias }!!
                } else {
                    vitezne.filterNot { it == trumfSedma }.minByOrNull { it.hodnota.silaMarias }
                        ?: vitezne.minByOrNull { it.hodnota.silaMarias }!!
                }
                return zahrajKartuLizaci(stav, idx, vybranaVitezna)
            }
            val nejlevnejsiProhrana = povolene.minWithOrNull(
                compareBy<Karta>(
                    { if (it == trumfSedma) 1 else 0 },
                    { it.hodnota.bodyMariasSedma },
                    { it.hodnota.silaMarias }
                )
            ) ?: povolene.first()
            return zahrajKartuLizaci(stav, idx, nejlevnejsiProhrana)
        }
    }

    fun vyhodnotLizaciMariase(stav: StavMarias): VyuctovaniMariase {
        val idxA = stav.indexForhonta
        val idxB = 1 - idxA
        val jmenoA = stav.jmenaHracu.getOrElse(idxA) { "Hráč 1" }
        val jmenoB = stav.jmenaHracu.getOrElse(idxB) { "Hráč 2" }

        val platneHlaskyA = if (stav.pocetStychuForhont > 0) stav.hlaskyForhont else 0
        val platneHlaskyB = if (stav.pocetStychuObrana > 0) stav.hlaskyObrana else 0
        val celkemA = stav.bodyKaretForhont + platneHlaskyA
        val celkemB = stav.bodyKaretObrana + platneHlaskyB

        val cerveneNasobek = if (stav.trumf == BarvaKarty.CERVENE) 2 else 1
        val polozky = ArrayList<PolozkaVyuctovaniMariase>()

        val bodyHrace0 = if (idxA == 0) celkemA else celkemB
        val kartyHrace0 = if (idxA == 0) stav.bodyKaretForhont else stav.bodyKaretObrana
        val hlaskyHrace0 = if (idxA == 0) platneHlaskyA else platneHlaskyB
        val stychyHrace0 = if (idxA == 0) stav.pocetStychuForhont else stav.pocetStychuObrana

        val bodyHrace1 = if (idxA == 1) celkemA else celkemB
        val kartyHrace1 = if (idxA == 1) stav.bodyKaretForhont else stav.bodyKaretObrana
        val hlaskyHrace1 = if (idxA == 1) platneHlaskyA else platneHlaskyB
        val stychyHrace1 = if (idxA == 1) stav.pocetStychuForhont else stav.pocetStychuObrana

        polozky.add(
            PolozkaVyuctovaniMariase(
                popis = stav.jmenaHracu.getOrElse(0) { "Hráč 1" },
                hodnotaText = "$bodyHrace0 b. (${kartyHrace0}+hl. $hlaskyHrace0 • ${sklonujStychyMariase(stychyHrace0)})",
                jeZvyraznena = bodyHrace0 > bodyHrace1
            )
        )
        polozky.add(
            PolozkaVyuctovaniMariase(
                popis = stav.jmenaHracu.getOrElse(1) { "Hráč 2" },
                hodnotaText = "$bodyHrace1 b. (${kartyHrace1}+hl. $hlaskyHrace1 • ${sklonujStychyMariase(stychyHrace1)})",
                jeZvyraznena = bodyHrace1 > bodyHrace0
            )
        )

        val trumfNazev = stav.trumf?.ceskyNazev ?: "—"
        polozky.add(
            PolozkaVyuctovaniMariase(
                popis = "Trumfová barva",
                hodnotaText = if (cerveneNasobek == 2) "$trumfNazev (2× sazba)" else trumfNazev,
                jeZvyraznena = cerveneNasobek == 2
            )
        )

        val vitezBodu = if (celkemA > celkemB) idxA else idxB
        val bodyViteze = maxOf(celkemA, celkemB)
        val stychyPorazeneho = if (vitezBodu == idxA) stav.pocetStychuObrana else stav.pocetStychuForhont
        val smerProA = if (vitezBodu == idxA) 1 else -1

        var jednotkyProA = 0
        if (bodyViteze >= 100) {
            val desitkyNadSto = (bodyViteze - 100) / 10
            val sazbaStovky = (2 + desitkyNadSto) * cerveneNasobek
            jednotkyProA += smerProA * sazbaStovky
            val jmenoVit = stav.jmenaHracu.getOrElse(vitezBodu) { "Vítěz" }
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Tichá stovka ($jmenoVit: $bodyViteze b.)",
                    hodnotaText = "${sazbaStovky * 10} b.",
                    jeZvyraznena = true
                )
            )
        } else {
            val sazbaHry = 1 * cerveneNasobek
            jednotkyProA += smerProA * sazbaHry
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Výhra na body",
                    hodnotaText = "${sazbaHry * 10} b.",
                    jeZvyraznena = false
                )
            )
        }

        if (stychyPorazeneho == 0) {
            val sazbaKanar = 2 * cerveneNasobek
            jednotkyProA += smerProA * sazbaKanar
            polozky.add(
                PolozkaVyuctovaniMariase(
                    popis = "Kanár (soupeř bez štychu)",
                    hodnotaText = "+${sazbaKanar * 10} b.",
                    jeZvyraznena = true
                )
            )
        }

        when (stav.stavPoslednihoStychuSedma) {
            1 -> {
                val s7 = 2 * cerveneNasobek
                jednotkyProA += s7
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Tichá sedma v 16. štychu ($jmenoA)",
                        hodnotaText = "+${s7 * 10} b.",
                        jeZvyraznena = true
                    )
                )
            }
            -1 -> {
                val z7 = 4 * cerveneNasobek
                jednotkyProA -= z7
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Zabitá tichá sedma ($jmenoB zabil tichou sedmu)",
                        hodnotaText = "${z7 * 10} b.",
                        jeZvyraznena = true
                    )
                )
            }
            2 -> {
                val s7 = 2 * cerveneNasobek
                jednotkyProA -= s7
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Tichá sedma v 16. štychu ($jmenoB)",
                        hodnotaText = "+${s7 * 10} b.",
                        jeZvyraznena = true
                    )
                )
            }
            -2 -> {
                val z7 = 4 * cerveneNasobek
                jednotkyProA += z7
                polozky.add(
                    PolozkaVyuctovaniMariase(
                        popis = "Zabitá tichá sedma ($jmenoA zabil tichou sedmu)",
                        hodnotaText = "${z7 * 10} b.",
                        jeZvyraznena = true
                    )
                )
            }
        }

        val ziskA = jednotkyProA * 10
        val zisk0 = if (idxA == 0) ziskA else -ziskA
        val zisk1 = -zisk0

        val nadpis = when {
            zisk0 > 0 -> "Vítězí ${stav.jmenaHracu.getOrElse(0) { "Hráč 1" }}!"
            zisk1 > 0 -> "Vítězí ${stav.jmenaHracu.getOrElse(1) { "Hráč 2" }}!"
            else -> "Smír u stolu!"
        }

        val hraci = listOf(
            HracVyuctovaniMariase(
                indexHrace = 0,
                jmenoHrace = stav.jmenaHracu.getOrElse(0) { "Hráč 1" },
                jeVitez = if (zisk0 != 0) zisk0 > 0 else vitezBodu == 0,
                bodyZapasu = zisk0,
                zmenaTerezianu = zisk0
            ),
            HracVyuctovaniMariase(
                indexHrace = 1,
                jmenoHrace = stav.jmenaHracu.getOrElse(1) { "Hráč 2" },
                jeVitez = if (zisk1 != 0) zisk1 > 0 else vitezBodu == 1,
                bodyZapasu = zisk1,
                zmenaTerezianu = zisk1
            )
        )

        val celkovaSazba = abs(zisk0)
        polozky.add(
            PolozkaVyuctovaniMariase(
                popis = "Celková bilance hry",
                hodnotaText = "$celkovaSazba tereziánů",
                jeZvyraznena = true
            )
        )

        return VyuctovaniMariase(
            nadpis = nadpis,
            podnadpis = "Lízací mariáš • ${stav.variantaPravidel.kratkyNazev} pravidla",
            polozky = polozky,
            hraci = hraci
        )
    }
}
