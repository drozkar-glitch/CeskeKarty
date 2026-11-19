package cz.mares.ceskekarty

import kotlin.random.Random

/**
 * Hospodský komentátor a zvuková kulisa pro Mariáš, Prší a Sedmu.
 * Přednostně pouští šťavnaté AI hlášky z HospodaAudioData.kt, u chybějících klipů
 * zaskočí českou hlasovou syntézou a hlídá, aby podružné hlášky neusekávaly důležitá zvolání.
 */
object HospodaZvuk {

    private data class ProfilHlasu(
        val vyska: Float,
        val rychlost: Float
    )

    private data class KandidatHlasky(
        val idKlipu: String?,
        val zalozniText: String
    )

    private var dulezitaHlaskaChranenaDoMs: Long = 0L

    private fun profilProHrace(jmeno: String, indexHrace: Int): ProfilHlasu {
        val male = jmeno.lowercase()
        return when {
            male.contains("karel") -> ProfilHlasu(vyska = 0.80f, rychlost = 0.94f)
            male.contains("pepa") -> ProfilHlasu(vyska = 1.13f, rychlost = 1.07f)
            male.contains("franta") -> ProfilHlasu(vyska = 0.90f, rychlost = 1.02f)
            male.contains("vašek") || male.contains("vasek") -> ProfilHlasu(vyska = 0.86f, rychlost = 0.98f)
            male.contains("tonda") -> ProfilHlasu(vyska = 0.78f, rychlost = 0.92f)
            male.contains("honza") -> ProfilHlasu(vyska = 1.05f, rychlost = 1.04f)
            male.contains("lojza") -> ProfilHlasu(vyska = 1.16f, rychlost = 1.00f)
            male.contains("standa") -> ProfilHlasu(vyska = 0.95f, rychlost = 0.95f)
            male.contains("venca") -> ProfilHlasu(vyska = 1.08f, rychlost = 1.09f)
            indexHrace == 0 -> ProfilHlasu(vyska = 1.00f, rychlost = 1.02f)
            indexHrace == 1 -> ProfilHlasu(vyska = 0.84f, rychlost = 0.96f)
            indexHrace == 2 -> ProfilHlasu(vyska = 1.10f, rychlost = 1.05f)
            else -> ProfilHlasu(vyska = 0.94f, rychlost = 1.00f)
        }
    }

    private fun <T> vyberNahodne(moznosti: List<T>): T {
        return moznosti[Random.nextInt(moznosti.size)]
    }

    fun pleskniKartou() {
        if (!jeZvukAHlaskyZapnuty()) return
        prehrajZvukovyEfektPlatforma(EfektStolu.KARTA_NA_STUL)
    }

    fun cinkniVyuctovani() {
        if (!jeZvukAHlaskyZapnuty()) return
        prehrajZvukovyEfektPlatforma(EfektStolu.SLAVNOSTNI_CINK)
    }

    fun rekniHlasku(
        text: String,
        jmenoMluvciho: String = "",
        indexHrace: Int = 0,
        jeDulezita: Boolean = true
    ) {
        if (!jeZvukAHlaskyZapnuty() || text.isBlank()) return
        val nyni = ziskejAktualniCasMs()
        if (!jeDulezita && (probihaMluveniPlatforma() || nyni < dulezitaHlaskaChranenaDoMs)) {
            return
        }
        if (jeDulezita) {
            dulezitaHlaskaChranenaDoMs = nyni + 1200L
        }
        val profil = profilProHrace(jmenoMluvciho, indexHrace)
        vyslovCeskyTextPlatforma(text, profil.vyska, profil.rychlost)
    }

    private fun prehrajKlipNeboTts(
        idKlipu: String?,
        zalozniText: String,
        jmenoMluvciho: String = "",
        indexHrace: Int = 0,
        jeDulezita: Boolean = true
    ) {
        if (!jeZvukAHlaskyZapnuty()) return
        val nyni = ziskejAktualniCasMs()
        if (!jeDulezita && (probihaMluveniPlatforma() || nyni < dulezitaHlaskaChranenaDoMs)) {
            return
        }
        if (jeDulezita) {
            dulezitaHlaskaChranenaDoMs = nyni + 1250L
        }
        if (!idKlipu.isNullOrEmpty() && prehrajHospodaKlipPlatforma(idKlipu)) {
            return
        }
        val profil = profilProHrace(jmenoMluvciho, indexHrace)
        vyslovCeskyTextPlatforma(zalozniText, profil.vyska, profil.rychlost)
    }

    private fun prehrajVybranouHlasku(
        kandidati: List<KandidatHlasky>,
        jmenoMluvciho: String = "",
        indexHrace: Int = 0,
        jeDulezita: Boolean = true
    ) {
        if (!jeZvukAHlaskyZapnuty() || kandidati.isEmpty()) return
        val dostupneAi = kandidati.filter { !it.idKlipu.isNullOrEmpty() && ziskejHospodaKlipB64(it.idKlipu) != null }
        val vybrany = if (dostupneAi.isNotEmpty()) {
            vyberNahodne(dostupneAi)
        } else {
            vyberNahodne(kandidati)
        }
        prehrajKlipNeboTts(vybrany.idKlipu, vybrany.zalozniText, jmenoMluvciho, indexHrace, jeDulezita)
    }

    fun priPrepnutiZvuku(zapnuto: Boolean) {
        dulezitaHlaskaChranenaDoMs = 0L
        nastavZvukAHlasky(zapnuto)
        if (zapnuto) {
            prehrajZvukovyEfektPlatforma(EfektStolu.KARTA_NA_STUL)
            prehrajKlipNeboTts("rozdej", "Tak rozdávej, pane vrchní!", "", 0, jeDulezita = true)
        }
    }

    // -------------------------------------------------------------------------
    // PRŠÍ — vyhodnocení tahu a výběr hospodské hlášky
    // -------------------------------------------------------------------------
    fun zpracujZmenuPrsi(predchozi: StavPrsi?, novy: StavPrsi) {
        if (predchozi == null) return
        if (novy.cisloTahu == predchozi.cisloTahu && novy.cekaNaVolbuBarvy == predchozi.cekaNaVolbuBarvy) return

        val mluvciIdx = predchozi.naTahu
        val mluvciJmeno = novy.jmenaHracu.getOrElse(mluvciIdx) { "" }

        if (!predchozi.cekaNaVolbuBarvy && novy.cekaNaVolbuBarvy) {
            pleskniKartou()
            return
        }

        val polozenaNovaKarta = novy.odhazovaciBalicek.size > predchozi.odhazovaciBalicek.size
        val dokoncenaVolbaSvrska = predchozi.cekaNaVolbuBarvy && !novy.cekaNaVolbuBarvy
        val odhozenaKarta = polozenaNovaKarta || dokoncenaVolbaSvrska
        if (polozenaNovaKarta) {
            pleskniKartou()
        }

        if (!predchozi.jeKonec && novy.jeKonec) {
            silnaHaptika()
            cinkniVyuctovani()
            val vitezIdx = novy.indexViteze.coerceAtLeast(0)
            val vitezJm = novy.jmenaHracu.getOrElse(vitezIdx) { "" }
            prehrajKlipNeboTts("p_vyhra", "Čistá ruka, dopršelo!", vitezJm, vitezIdx, jeDulezita = true)
            return
        }

        val rukaMluvcihoPoTahu = novy.ruceHracu.getOrNull(mluvciIdx)?.size ?: 99
        val hlasiPrsi = odhozenaKarta && rukaMluvcihoPoTahu == 1

        if (novy.zvolenaBarvaSvrskem != null &&
            (dokoncenaVolbaSvrska || (polozenaNovaKarta && novy.vrchniKarta.hodnota == HodnotaKarty.SVRSEK))
        ) {
            when (novy.zvolenaBarvaSvrskem) {
                BarvaKarty.CERVENE -> prehrajKlipNeboTts("p_cervene", "Červené poroučím!", mluvciJmeno, mluvciIdx)
                BarvaKarty.KULE -> prehrajKlipNeboTts("p_kule", "Poroučím kule!", mluvciJmeno, mluvciIdx)
                BarvaKarty.ZALUDY -> prehrajKlipNeboTts("m_zaludy", "Budou žaludy!", mluvciJmeno, mluvciIdx)
                BarvaKarty.ZELENE -> prehrajKlipNeboTts("m_zelene", "Zelené na stůl!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        if (novy.aktivniSedmyPocet > predchozi.aktivniSedmyPocet) {
            val karet = novy.aktivniSedmyPocet * 2
            if (karet >= 4 || predchozi.aktivniEsoStoji) silnaHaptika()
            when {
                predchozi.aktivniEsoStoji && karet == 2 ->
                    prehrajKlipNeboTts("p_cervena7", "Červená sedma vrací do hry, lízni si dvě!", mluvciJmeno, mluvciIdx)
                karet == 2 ->
                    prehrajKlipNeboTts("p_ber2", "Sedmička, lízni si dvě!", mluvciJmeno, mluvciIdx)
                karet == 4 ->
                    prehrajKlipNeboTts("p_ber4", "Přitlačím, čtyři kousky do ruky!", mluvciJmeno, mluvciIdx)
                karet == 6 ->
                    prehrajKlipNeboTts("p_ber6", "Šest karet na cestu!", mluvciJmeno, mluvciIdx)
                else ->
                    prehrajKlipNeboTts("p_ber8", "Osm karet, to už neuneseš!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        if (predchozi.aktivniSedmyPocet > 0 && novy.aktivniSedmyPocet == 0) {
            val bral = predchozi.aktivniSedmyPocet * 2
            if (bral >= 4 || rukaMluvcihoPoTahu >= 6) {
                prehrajKlipNeboTts("p_lizu", "No nazdar, plná ruka!", mluvciJmeno, mluvciIdx)
            } else {
                prehrajKlipNeboTts("p_beru2", "Beru dvě, nedá se svítit!", mluvciJmeno, mluvciIdx, jeDulezita = false)
            }
            return
        }

        if (novy.aktivniEsoStoji && odhozenaKarta && novy.vrchniKarta.hodnota == HodnotaKarty.ESO) {
            if (hlasiPrsi) {
                silnaHaptika()
                prehrajKlipNeboTts("p_prsi", "Pozor pánové, prší!", mluvciJmeno, mluvciIdx)
            } else {
                prehrajKlipNeboTts("p_eso", "Eso, stát, nikam nespěchej!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        if (hlasiPrsi) {
            silnaHaptika()
            prehrajKlipNeboTts("p_prsi", "Pozor pánové, prší!", mluvciJmeno, mluvciIdx)
        }
    }

    // -------------------------------------------------------------------------
    // SEDMA — vyhodnocení tahu a výběr hospodské hlášky
    // -------------------------------------------------------------------------
    fun zpracujZmenuSedmy(predchozi: StavSedma?, novy: StavSedma) {
        if (predchozi == null || novy.cisloTahu == predchozi.cisloTahu) return

        val mluvciIdx = predchozi.naTahu
        val mluvciJmeno = novy.jmenaHracu.getOrElse(mluvciIdx) { "" }

        val ukoncilStychTlacitkem = predchozi.muzePustitStych &&
            !novy.muzePustitStych &&
            novy.aktualniStych.isEmpty() &&
            novy.spalenaTym < 0 &&
            novy.posledniUzavrenyStych.size == predchozi.aktualniStych.size

        if (!ukoncilStychTlacitkem) {
            pleskniKartou()
        }

        if (predchozi.spalenaTym < 0 && novy.spalenaTym >= 0) {
            silnaHaptika()
            cinkniVyuctovani()
            if (novy.spalenaZRuky) {
                prehrajKlipNeboTts(
                    "s_spalena_ruka",
                    "Spálená z ruky, všechny čtyři na stůl!",
                    mluvciJmeno,
                    mluvciIdx,
                    jeDulezita = true
                )
            } else {
                prehrajKlipNeboTts(
                    "s_spalena",
                    "Spálená, čtyři stejné na stole!",
                    mluvciJmeno,
                    mluvciIdx,
                    jeDulezita = true
                )
            }
            return
        }

        if (!predchozi.jeKonec && novy.jeKonec) {
            silnaHaptika()
            cinkniVyuctovani()
            val vitezStychu = if (novy.posledniUzavrenyVitez >= 0) novy.posledniUzavrenyVitez else novy.prozatimBereHrac
            val jm = novy.jmenaHracu.getOrElse(vitezStychu) { mluvciJmeno }
            val viteznyTym = novy.tymHrace(vitezStychu)
            val stychyPorazeneho = if (viteznyTym == 0) novy.pocetStychuTym1 else novy.pocetStychuTym0
            if (stychyPorazeneho == 0) {
                prehrajKlipNeboTts("s_holatko", "Ani štych, máme tu holátko!", jm, vitezStychu, jeDulezita = true)
            } else {
                prehrajKlipNeboTts("s_vyhra", "Poslední štych je doma!", jm, vitezStychu, jeDulezita = true)
            }
            return
        }

        if (ukoncilStychTlacitkem) {
            if (predchozi.prozatimBereHrac == mluvciIdx) {
                prehrajKlipNeboTts("s_beru_stych", "Beru štych!", mluvciJmeno, mluvciIdx, jeDulezita = false)
            } else {
                prehrajKlipNeboTts("s_poustim", "Pouštím, vem si to.", mluvciJmeno, mluvciIdx, jeDulezita = false)
            }
            return
        }

        val hranyStych = when {
            novy.aktualniStych.size > predchozi.aktualniStych.size -> novy.aktualniStych
            novy.aktualniStych.isEmpty() && novy.posledniUzavrenyStych.size == predchozi.aktualniStych.size + 1 ->
                novy.posledniUzavrenyStych
            else -> emptyList()
        }

        if (hranyStych.size >= 2) {
            val posledni = hranyStych.last()
            val predchoziDrzitel = predchozi.prozatimBereHrac
            val novyDrzitel = if (novy.aktualniStych.isNotEmpty()) novy.prozatimBereHrac else novy.posledniUzavrenyVitez
            val predchoziTym = novy.tymHrace(predchoziDrzitel)
            val novyTym = novy.tymHrace(novyDrzitel)

            val toceniVlastnihoStychu = novy.pocetHracu == 2 &&
                hranyStych.size >= 3 &&
                hranyStych.size % 2 == 1 &&
                predchoziDrzitel == posledni.indexHrace &&
                novyDrzitel == posledni.indexHrace

            if (novyDrzitel == posledni.indexHrace && novyTym != predchoziTym) {
                if (posledni.karta.hodnota == HodnotaKarty.R7) {
                    prehrajKlipNeboTts("s_sedma_bere", "Šup tam sedmu, ten je můj!", posledni.jmenoHrace, posledni.indexHrace)
                } else if (novy.pocetHracu == 2 && hranyStych.size >= 3) {
                    prehrajKlipNeboTts("s_pritlacim", "Ještě jednu stejnou, přitlačím!", posledni.jmenoHrace, posledni.indexHrace, jeDulezita = false)
                } else {
                    prehrajKlipNeboTts("s_stejna", "Stejná, beru zatím!", posledni.jmenoHrace, posledni.indexHrace, jeDulezita = false)
                }
            } else if (toceniVlastnihoStychu) {
                prehrajKlipNeboTts("s_pritlacim", "Ještě jednu stejnou, přitlačím!", posledni.jmenoHrace, posledni.indexHrace, jeDulezita = false)
            } else if (novy.pocetHracu == 4 &&
                posledni.karta.hodnota.bodyMariasSedma == 10 &&
                novy.tymHrace(posledni.indexHrace) == novyTym &&
                posledni.indexHrace != predchoziDrzitel
            ) {
                prehrajKlipNeboTts("s_namaz", "Tady máš desítku, parťáku!", posledni.jmenoHrace, posledni.indexHrace)
            }
        }
    }

    // -------------------------------------------------------------------------
    // MARIÁŠ (Volený, Licitovaný i Lízací) — vyhodnocení tahu a výběr hlášky
    // -------------------------------------------------------------------------
    fun zpracujZmenuMariase(predchozi: StavMarias?, novy: StavMarias) {
        if (predchozi == null || novy.cisloTahu == predchozi.cisloTahu) return

        val mluvciIdx = predchozi.naTahu
        val mluvciJmeno = novy.jmenaHracu.getOrElse(mluvciIdx) { "" }

        // 0) Dražba v Licitovaném mariáši
        if (predchozi.faze == FazeMariase.DRAZBA) {
            if (novy.slozenoBezHry && novy.faze == FazeMariase.KONEC) {
                cinkniVyuctovani()
                rekniHlasku("Všichni dál, skládáme karty!", mluvciJmeno, mluvciIdx, jeDulezita = true)
                return
            }
            val novaNabidka = novy.nejvyssiNabidka
            if (novaNabidka != null && novaNabidka != predchozi.nejvyssiNabidka) {
                pleskniKartou()
                when (novaNabidka) {
                    ZavazekMariase.HRA ->
                        rekniHlasku("Hra!", mluvciJmeno, mluvciIdx, jeDulezita = false)
                    ZavazekMariase.HRA_A_SEDMA ->
                        prehrajKlipNeboTts("m_sedma_hl", "Hlásím sedmu!", mluvciJmeno, mluvciIdx)
                    ZavazekMariase.HRA_A_DVE_SEDMY, ZavazekMariase.STO_A_DVE_SEDMY ->
                        prehrajKlipNeboTts("m_dve_sedmy", "Dvě sedmy na stůl!", mluvciJmeno, mluvciIdx)
                    ZavazekMariase.STO ->
                        prehrajKlipNeboTts("m_stovka", "Hlásím stovku, pánové!", mluvciJmeno, mluvciIdx)
                    ZavazekMariase.STO_A_SEDMA ->
                        prehrajKlipNeboTts("m_sto_sedma", "Sto a sedma, pánové!", mluvciJmeno, mluvciIdx)
                    ZavazekMariase.BETL ->
                        prehrajKlipNeboTts("m_betl", "Hraju betla, ani štych nevezmu!", mluvciJmeno, mluvciIdx)
                    ZavazekMariase.DURCH ->
                        prehrajKlipNeboTts("m_durch", "Durch, beru všechno!", mluvciJmeno, mluvciIdx)
                }
            } else {
                rekniHlasku("Dál.", mluvciJmeno, mluvciIdx, jeDulezita = false)
            }
            return
        }

        // 1) Volba trumfu
        if (predchozi.faze == FazeMariase.VOLBA_TRUMFU && novy.faze == FazeMariase.ODKLADANI_TALONU) {
            pleskniKartou()
            when {
                novy.zavazek == ZavazekMariase.BETL ->
                    prehrajKlipNeboTts("m_betl", "Hraju betla, ani štych nevezmu!", mluvciJmeno, mluvciIdx)
                novy.zavazek == ZavazekMariase.DURCH ->
                    prehrajKlipNeboTts("m_durch", "Durch, beru všechno!", mluvciJmeno, mluvciIdx)
                novy.volenoZTalonu ->
                    prehrajKlipNeboTts("m_talon", "Z talonu, děj se vůle boží!", mluvciJmeno, mluvciIdx)
                novy.trumf == BarvaKarty.CERVENE ->
                    prehrajKlipNeboTts("m_cervene", "Červené, a hned za dvojnásobek!", mluvciJmeno, mluvciIdx)
                novy.trumf == BarvaKarty.KULE ->
                    prehrajKlipNeboTts("m_kule", "Volím kule, pánové!", mluvciJmeno, mluvciIdx)
                novy.trumf == BarvaKarty.ZALUDY ->
                    prehrajKlipNeboTts("m_zaludy", "Budou žaludy!", mluvciJmeno, mluvciIdx)
                else ->
                    prehrajKlipNeboTts("m_zelene", "Zelené na stůl!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        // 2) Odložení talonu a hlášení závazku
        if (predchozi.faze == FazeMariase.ODKLADANI_TALONU && novy.faze == FazeMariase.FLEKOVANI) {
            pleskniKartou()
            if (novy.zavazek != ZavazekMariase.HRA) silnaHaptika()
            when (novy.zavazek) {
                ZavazekMariase.HRA ->
                    prehrajKlipNeboTts("m_hra", "Talon leží, hraju, ukažte se!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.HRA_A_SEDMA ->
                    prehrajKlipNeboTts("m_sedma_hl", "Hlásím sedmu, tu mi nevezmete!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.HRA_A_DVE_SEDMY ->
                    prehrajKlipNeboTts("m_dve_sedmy", "Dvě sedmy na stůl, držte si klobouky!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.STO ->
                    prehrajKlipNeboTts("m_stovka", "Hlásím stovku, pánové!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.STO_A_SEDMA ->
                    prehrajKlipNeboTts("m_sto_sedma", "Sto a sedma, pánové!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.STO_A_DVE_SEDMY ->
                    prehrajKlipNeboTts("m_dve_sedmy", "Dvě sedmy na stůl, držte si klobouky!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.BETL ->
                    prehrajKlipNeboTts("m_betl", "Hraju betla, ani štych nevezmu!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.DURCH ->
                    prehrajKlipNeboTts("m_durch", "Durch, beru všechno!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        // 3) Flekování (Dobrý / Flek / Re / Tutti / Boty / Kalhoty)
        if (predchozi.faze == FazeMariase.FLEKOVANI) {
            val zvedlHru = novy.nasobekFlekHra > predchozi.nasobekFlekHra
            val zvedlSedmu = novy.nasobekFlekSedma > predchozi.nasobekFlekSedma
            if (zvedlHru || zvedlSedmu) {
                silnaHaptika()
                val vyssi = maxOf(
                    if (zvedlHru) novy.nasobekFlekHra else 1,
                    if (zvedlSedmu) novy.nasobekFlekSedma else 1
                )
                when {
                    vyssi == 2 && zvedlSedmu && !zvedlHru ->
                        prehrajKlipNeboTts("m_flek_sedma", "Flek na sedmu, tu neuhraješ!", mluvciJmeno, mluvciIdx)
                    vyssi == 2 ->
                        prehrajKlipNeboTts("m_flek", "Flek jak Brno!", mluvciJmeno, mluvciIdx)
                    vyssi == 4 ->
                        prehrajKlipNeboTts("m_re", "Re! Přitopíme pod kotlem!", mluvciJmeno, mluvciIdx)
                    vyssi == 8 ->
                        prehrajKlipNeboTts("m_tutti", "Tutti, osmkrát, teď jde do tuhého!", mluvciJmeno, mluvciIdx)
                    vyssi == 16 ->
                        prehrajKlipNeboTts("m_boty", "Boty na stůl!", mluvciJmeno, mluvciIdx)
                    vyssi >= 32 ->
                        prehrajKlipNeboTts("m_kalhoty", "Kalhoty! Domů v trenkách!", mluvciJmeno, mluvciIdx)
                    else ->
                        prehrajKlipNeboTts("m_flek", "Flek jak Brno!", mluvciJmeno, mluvciIdx)
                }
            } else {
                prehrajVybranouHlasku(
                    listOf(
                        KandidatHlasky("m_dobry_1", "Dobrý, nes!"),
                        KandidatHlasky("m_dobry_2", "Nechat, dobrý!")
                    ),
                    mluvciJmeno,
                    mluvciIdx,
                    jeDulezita = false
                )
            }
            return
        }

        // 4) Sehrávka a konec rozdání
        if (predchozi.faze == FazeMariase.SEHRAVKA) {
            pleskniKartou()

            // Výměna trumfové sedmy za otočený trumf v Lízacím mariáši
            if (novy.rezimMariase == RezimMariase.LIZACI &&
                predchozi.otocenyTrumf != novy.otocenyTrumf &&
                novy.otocenyTrumf?.hodnota == HodnotaKarty.R7 &&
                novy.aktualniStych.isEmpty() &&
                novy.faze == FazeMariase.SEHRAVKA
            ) {
                silnaHaptika()
                rekniHlasku("Měním trumfovou sedmu!", mluvciJmeno, mluvciIdx, jeDulezita = true)
                return
            }

            val prirustekHlasek = (novy.hlaskyForhont - predchozi.hlaskyForhont) +
                (novy.hlaskyObrana - predchozi.hlaskyObrana)

            if (novy.faze == FazeMariase.KONEC) {
                silnaHaptika()
                cinkniVyuctovani()
                val vyuctovani = MariasEngine.vyhodnotMariase(novy)

                val maZabitouSedmu = vyuctovani.polozky.any { pol ->
                    pol.popis.contains("Zabitá", ignoreCase = true) ||
                        pol.popis.contains("zabil tichou sedmu", ignoreCase = true)
                }
                val maTichouSedmu = !maZabitouSedmu && vyuctovani.polozky.any { pol ->
                    (pol.popis.contains("sedma", ignoreCase = true) && pol.popis.contains("tich", ignoreCase = true)) ||
                        pol.popis.contains("Tiché dvě sedmy", ignoreCase = true)
                }

                if (novy.rezimMariase == RezimMariase.LIZACI) {
                    val vitezHrac = vyuctovani.hraci.firstOrNull { it.jeVitez }
                    val vitIdx = vitezHrac?.indexHrace ?: novy.indexForhonta
                    val vitJm = vitezHrac?.jmenoHrace ?: novy.jmenaHracu.getOrElse(vitIdx) { "" }
                    when {
                        maZabitouSedmu ->
                            prehrajKlipNeboTts("m_zabita7", "Zabitá sedma, to bolelo!", vitJm, vitIdx)
                        maTichouSedmu ->
                            prehrajKlipNeboTts("m_ticha7", "Tichá sedma doma!", vitJm, vitIdx)
                        else ->
                            prehrajKlipNeboTts("m_vyhra_f", "Je to doma, vysázejte tereziány!", vitJm, vitIdx)
                    }
                    return
                }

                val ziskForhonta = vyuctovani.hraci.firstOrNull { it.indexHrace == novy.indexForhonta }?.zmenaTerezianu ?: 0
                val forhontJm = novy.jmenaHracu.getOrElse(novy.indexForhonta) { "" }
                val obranceIdx = (novy.indexForhonta + 1) % 3
                val obranceJm = novy.jmenaHracu.getOrElse(obranceIdx) { "" }

                when {
                    maZabitouSedmu ->
                        prehrajKlipNeboTts("m_zabita7", "Zabitá sedma, to bolelo!", obranceJm, obranceIdx)
                    maTichouSedmu ->
                        prehrajKlipNeboTts("m_ticha7", "Tichá sedma doma!", forhontJm, novy.indexForhonta)
                    ziskForhonta > 0 ->
                        prehrajKlipNeboTts("m_vyhra_f", "Je to doma, vysázejte tereziány!", forhontJm, novy.indexForhonta)
                    ziskForhonta < 0 ->
                        prehrajKlipNeboTts("m_pad_f", "Forhont padl, zaplať obraně!", obranceJm, obranceIdx)
                    else ->
                        prehrajKlipNeboTts("m_smir", "Smír, nikdo nikomu nic nedluží!", forhontJm, novy.indexForhonta)
                }
                return
            }

            if (prirustekHlasek >= 40) {
                silnaHaptika()
                prehrajKlipNeboTts("m_hl40", "Čtyřicet, trumfová hláška!", mluvciJmeno, mluvciIdx)
            } else if (prirustekHlasek == 20) {
                silnaHaptika()
                prehrajKlipNeboTts("m_hl20", "Dvacet, krásnej párek!", mluvciJmeno, mluvciIdx)
            }
        }
    }
}
