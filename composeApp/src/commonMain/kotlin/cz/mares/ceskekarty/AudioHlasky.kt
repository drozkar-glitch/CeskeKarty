package cz.mares.ceskekarty

import kotlin.random.Random

/**
 * Hospodský komentátor a zvuková kulisa pro Mariáš, Prší a Sedmu.
 * Přednostně přehrává šťavnaté AI hlášky z HospodaAudioData.kt, u chybějících klipů
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

        val odhozenaKarta = novy.odhazovaciBalicek.size > predchozi.odhazovaciBalicek.size ||
            (predchozi.cekaNaVolbuBarvy && !novy.cekaNaVolbuBarvy)

        if (odhozenaKarta) {
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

        // Svršek a změna barvy
        if (novy.zvolenaBarvaSvrskem != null &&
            (predchozi.cekaNaVolbuBarvy || (novy.vrchniKarta.hodnota == HodnotaKarty.SVRSEK && predchozi.zvolenaBarvaSvrskem != novy.zvolenaBarvaSvrskem))
        ) {
            when (novy.zvolenaBarvaSvrskem) {
                BarvaKarty.CERVENE -> prehrajKlipNeboTts("p_cervene", "Červené poroučí!", mluvciJmeno, mluvciIdx)
                BarvaKarty.KULE -> prehrajKlipNeboTts("p_kule", "Poroučím kule!", mluvciJmeno, mluvciIdx)
                BarvaKarty.ZALUDY -> prehrajKlipNeboTts("m_zaludy", "Budou žaludy!", mluvciJmeno, mluvciIdx)
                BarvaKarty.ZELENE -> prehrajKlipNeboTts("m_zelene", "Zelené na stůl!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        // Vynesení nebo přebití sedmičky (včetně hospodské červené sedmy na eso)
        if (novy.aktivniSedmyPocet > predchozi.aktivniSedmyPocet) {
            val karet = novy.aktivniSedmyPocet * 2
            if (karet >= 4 || predchozi.aktivniEsoStoji) silnaHaptika()
            when {
                predchozi.aktivniEsoStoji && karet == 2 ->
                    prehrajKlipNeboTts("p_ber2", "Červená sedma přebíjí eso, lízni si dvě!", mluvciJmeno, mluvciIdx)
                karet == 2 ->
                    prehrajKlipNeboTts("p_ber2", "Sedmička, lízni si dvě!", mluvciJmeno, mluvciIdx)
                karet == 4 ->
                    prehrajKlipNeboTts("p_ber4", "Přitlačím, čtyři kousky do ruky!", mluvciJmeno, mluvciIdx)
                karet == 6 ->
                    rekniHlasku("Šest karet na cestu!", mluvciJmeno, mluvciIdx)
                else ->
                    rekniHlasku("Osm karet, to už neuneseš!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        // Líznutí trestu za sedmu
        if (predchozi.aktivniSedmyPocet > 0 && novy.aktivniSedmyPocet == 0) {
            val bral = predchozi.aktivniSedmyPocet * 2
            if (bral >= 4 || rukaMluvcihoPoTahu >= 6) {
                prehrajKlipNeboTts("p_lizu", "No nazdar, plná ruka!", mluvciJmeno, mluvciIdx)
            } else {
                rekniHlasku("Beru dvě.", mluvciJmeno, mluvciIdx, jeDulezita = false)
            }
            return
        }

        // Eso — další stojí
        if (novy.aktivniEsoStoji && odhozenaKarta && novy.vrchniKarta.hodnota == HodnotaKarty.ESO) {
            if (hlasiPrsi) {
                silnaHaptika()
                prehrajKlipNeboTts("p_prsi", "Eso! A pozor pánové, prší!", mluvciJmeno, mluvciIdx)
            } else {
                prehrajKlipNeboTts("p_eso", "Eso, stát, nikam nespěchej!", mluvciJmeno, mluvciIdx)
            }
            return
        }

        // Poslední karta v ruce — Prší!
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
        val ukoncilStychTlacitkem = predchozi.muzePustitStych && !novy.muzePustitStych &&
            novy.aktualniStych.isEmpty() &&
            novy.spalenaTym < 0 &&
            novy.ruceHracu[mluvciIdx].size >= predchozi.ruceHracu[mluvciIdx].size

        if (!ukoncilStychTlacitkem) {
            pleskniKartou()
        }

        // Spálená (ve štychu i z ruky v hospodských pravidlech)
        if (predchozi.spalenaTym < 0 && novy.spalenaTym >= 0) {
            silnaHaptika()
            cinkniVyuctovani()
            prehrajKlipNeboTts("s_spalena", "Spálená! Čtyři stejné na stole!", mluvciJmeno, mluvciIdx, jeDulezita = true)
            return
        }

        // Konec rozdání Sedmy
        if (!predchozi.jeKonec && novy.jeKonec) {
            silnaHaptika()
            cinkniVyuctovani()
            val vitezStychu = novy.prozatimBereHrac
            val jm = novy.jmenaHracu.getOrElse(vitezStychu) { mluvciJmeno }
            prehrajKlipNeboTts("s_vyhra", "Poslední štych je doma!", jm, vitezStychu, jeDulezita = true)
            return
        }

        if (ukoncilStychTlacitkem) {
            if (predchozi.prozatimBereHrac == mluvciIdx) {
                rekniHlasku("Beru štych!", mluvciJmeno, mluvciIdx, jeDulezita = false)
            } else {
                rekniHlasku("Pouštím, vem si to.", mluvciJmeno, mluvciIdx, jeDulezita = false)
            }
            return
        }

        // Právě přidaná karta ve štychu
        if (novy.aktualniStych.size > predchozi.aktualniStych.size) {
            val posledni = novy.aktualniStych.last()
            if (novy.aktualniStych.size > 1 && novy.prozatimBereHrac == posledni.indexHrace &&
                predchozi.prozatimBereHrac != posledni.indexHrace
            ) {
                if (posledni.karta.hodnota == HodnotaKarty.R7) {
                    prehrajKlipNeboTts("s_sedma_bere", "Šup tam sedmu, ten je můj!", posledni.jmenoHrace, posledni.indexHrace)
                } else {
                    rekniHlasku("Stejná, beru zatím já!", posledni.jmenoHrace, posledni.indexHrace, jeDulezita = false)
                }
            } else if (novy.pocetHracu == 2 && novy.aktualniStych.size >= 3 && novy.aktualniStych.size % 2 == 1 &&
                predchozi.prozatimBereHrac == posledni.indexHrace
            ) {
                rekniHlasku("Ještě jednu stejnou, přitlač!", posledni.jmenoHrace, posledni.indexHrace, jeDulezita = false)
            } else if (novy.pocetHracu == 4 && novy.aktualniStych.size > 1 &&
                posledni.karta.hodnota.bodyMariasSedma == 10 &&
                novy.tymHrace(posledni.indexHrace) == novy.tymHrace(novy.prozatimBereHrac) &&
                posledni.indexHrace != novy.prozatimBereHrac
            ) {
                prehrajKlipNeboTts("s_namaz", "Tady máš desítku, parťáku!", posledni.jmenoHrace, posledni.indexHrace)
            }
        }
    }

    // -------------------------------------------------------------------------
    // VOLENÝ MARIÁŠ — vyhodnocení tahu a výběr hospodské hlášky
    // -------------------------------------------------------------------------
    fun zpracujZmenuMariase(predchozi: StavMarias?, novy: StavMarias) {
        if (predchozi == null || novy.cisloTahu == predchozi.cisloTahu) return

        val mluvciIdx = predchozi.naTahu
        val mluvciJmeno = novy.jmenaHracu.getOrElse(mluvciIdx) { "" }

        // 1) Volba trumfu
        if (predchozi.faze == FazeMariase.VOLBA_TRUMFU && novy.faze == FazeMariase.ODKLADANI_TALONU) {
            pleskniKartou()
            when {
                novy.zavazek == ZavazekMariase.BETL ->
                    prehrajKlipNeboTts("m_betl", "Hraju betla, ani štych nevezmu!", mluvciJmeno, mluvciIdx)
                novy.zavazek == ZavazekMariase.DURCH ->
                    prehrajKlipNeboTts("m_durch", "Durch, beru úplně všechno!", mluvciJmeno, mluvciIdx)
                novy.volenoZTalonu ->
                    prehrajKlipNeboTts("m_talon", "Z talonu, děj se vůle boží!", mluvciJmeno, mluvciIdx)
                novy.trumf == BarvaKarty.CERVENE ->
                    prehrajKlipNeboTts("m_cervene", "Červený, a hned za dvojnásobek!", mluvciJmeno, mluvciIdx)
                novy.trumf == BarvaKarty.KULE ->
                    prehrajKlipNeboTts("m_kule", "Volím kule, pánové!", mluvciJmeno, mluvciIdx)
                novy.trumf == BarvaKarty.ZALUDY ->
                    prehrajKlipNeboTts("m_zaludy", "Budou žaludy!", mluvciJmeno, mluvciIdx)
                else ->
                    prehrajKlipNeboTts("m_zelene", "Zelený na stůl!", mluvciJmeno, mluvciIdx)
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
                    prehrajVybranouHlasku(
                        listOf(
                            KandidatHlasky("m_stovka", "Sto a sedma, pánové!"),
                            KandidatHlasky("m_sedma_hl", "Sto a sedma, pánové!")
                        ),
                        mluvciJmeno,
                        mluvciIdx
                    )
                ZavazekMariase.STO_A_DVE_SEDMY ->
                    prehrajKlipNeboTts("m_dve_sedmy", "Sto a dvě sedmy, držte si klobouky!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.BETL ->
                    prehrajKlipNeboTts("m_betl", "Hraju betla, ani štych nevezmu!", mluvciJmeno, mluvciIdx)
                ZavazekMariase.DURCH ->
                    prehrajKlipNeboTts("m_durch", "Durch, beru úplně všechno!", mluvciJmeno, mluvciIdx)
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
                        prehrajKlipNeboTts("m_re", "Re! Přitápíme pod kotlem!", mluvciJmeno, mluvciIdx)
                    vyssi == 8 ->
                        prehrajKlipNeboTts("m_tutti", "Tutti, osmkrát, teď půjde do tuhýho!", mluvciJmeno, mluvciIdx)
                    vyssi == 16 ->
                        prehrajKlipNeboTts("m_boty", "Boty na stůl!", mluvciJmeno, mluvciIdx)
                    vyssi >= 32 ->
                        prehrajKlipNeboTts("m_kalhoty", "Kalhoty! Domů půjdeš v trenkách!", mluvciJmeno, mluvciIdx)
                    else ->
                        prehrajKlipNeboTts("m_flek", "Flek!", mluvciJmeno, mluvciIdx)
                }
            } else {
                // "Dobrý" má nižší prioritu — pokud ještě doznívá hlášení závazku forhonta, neskočí mu do řeči
                prehrajVybranouHlasku(
                    listOf(
                        KandidatHlasky("m_dobry_1", "Dobrý, nes!"),
                        KandidatHlasky("m_dobry_2", "Nechám tě hrát, dobrý!")
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

            val prirustekHlasek = (novy.hlaskyForhont - predchozi.hlaskyForhont) +
                (novy.hlaskyObrana - predchozi.hlaskyObrana)

            if (novy.faze == FazeMariase.KONEC) {
                silnaHaptika()
                cinkniVyuctovani()
                val vyuctovani = MariasEngine.vyhodnotMariase(novy)
                val ziskForhonta = vyuctovani.hraci.firstOrNull { it.indexHrace == novy.indexForhonta }?.zmenaTerezianu ?: 0
                val forhontJm = novy.jmenaHracu.getOrElse(novy.indexForhonta) { "" }
                val obranceIdx = (novy.indexForhonta + 1) % 3
                val obranceJm = novy.jmenaHracu.getOrElse(obranceIdx) { "" }

                val tichaSedmaText = vyuctovani.polozky.firstOrNull {
                    it.popis.contains("Tichá", ignoreCase = true) || it.popis.contains("Zabitá", ignoreCase = true)
                }?.popis

                if (tichaSedmaText != null && tichaSedmaText.contains("Tichá (utajená) sedma")) {
                    prehrajKlipNeboTts("m_ticha7", "Tichá sedma doma!", forhontJm, novy.indexForhonta)
                } else if (tichaSedmaText != null && tichaSedmaText.contains("Zabitá")) {
                    prehrajKlipNeboTts("m_zabita7", "Zabitá sedma, to bolelo!", obranceJm, obranceIdx)
                } else if (ziskForhonta > 0) {
                    prehrajKlipNeboTts("m_vyhra_f", "Je to doma, vysázejte tereziány!", forhontJm, novy.indexForhonta)
                } else if (ziskForhonta < 0) {
                    prehrajKlipNeboTts("m_pad_f", "Forhont padl, zaplať obraně!", obranceJm, obranceIdx)
                } else {
                    rekniHlasku("Smír, nikdo nikomu nic nedluží!", forhontJm, novy.indexForhonta)
                }
                return
            }

            // Hláška 20 nebo 40 v průběhu štychu
            if (prirustekHlasek >= 40) {
                silnaHaptika()
                prehrajKlipNeboTts("m_hl40", "Čtyřicet, trumfová hláška!", mluvciJmeno, mluvciIdx)
            } else if (prirustekHlasek == 20) {
                silnaHaptika()
                prehrajKlipNeboTts("m_hl20", "Dvacet, krásnej páreček!", mluvciJmeno, mluvciIdx)
            }
        }
    }
}
