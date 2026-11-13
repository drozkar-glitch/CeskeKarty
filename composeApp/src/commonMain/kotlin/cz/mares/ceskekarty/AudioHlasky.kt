package cz.mares.ceskekarty

import kotlin.random.Random

/**
 * Hospodský komentátor a zvuková kulisa pro Mariáš, Prší a Sedmu.
 * Respektuje přepínač zvuku v úložišti a každému hráči u stolu přiřazuje odlišnou barvu a tempo hlasu.
 */
object HospodaZvuk {

    private data class ProfilHlasu(
        val vyska: Float,
        val rychlost: Float
    )

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

    private fun vyberNahodne(moznosti: List<String>): String {
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

    fun rekniHlasku(text: String, jmenoMluvciho: String = "", indexHrace: Int = 0) {
        if (!jeZvukAHlaskyZapnuty() || text.isBlank()) return
        val profil = profilProHrace(jmenoMluvciho, indexHrace)
        vyslovCeskyTextPlatforma(text, profil.vyska, profil.rychlost)
    }

    fun priPrepnutiZvuku(zapnuto: Boolean) {
        nastavZvukAHlasky(zapnuto)
        if (zapnuto) {
            prehrajZvukovyEfektPlatforma(EfektStolu.KARTA_NA_STUL)
            vyslovCeskyTextPlatforma("Tak rozdávej, pane vrchní!", 0.96f, 1.02f)
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

        // Dočasné vynesení svršku, kdy hráč teprve vybírá barvu
        if (!predchozi.cekaNaVolbuBarvy && novy.cekaNaVolbuBarvy) {
            pleskniKartou()
            return
        }

        val odhozenaKarta = novy.odhazovaciBalicek.size > predchozi.odhazovaciBalicek.size ||
            (predchozi.cekaNaVolbuBarvy && !novy.cekaNaVolbuBarvy)

        if (odhozenaKarta) {
            pleskniKartou()
        }

        // Konec hry v Prší
        if (!predchozi.jeKonec && novy.jeKonec) {
            silnaHaptika()
            cinkniVyuctovani()
            val vitezIdx = novy.indexViteze.coerceAtLeast(0)
            val vitezJm = novy.jmenaHracu.getOrElse(vitezIdx) { "" }
            rekniHlasku(
                vyberNahodne(
                    listOf(
                        "Čistá ruka, dopršelo!",
                        "Mám prázdnou ruku, zaplaťte pánové!",
                        "A je vymalováno, beru bank!"
                    )
                ),
                vitezJm,
                vitezIdx
            )
            return
        }

        val rukaMluvcihoPoTahu = novy.ruceHracu.getOrNull(mluvciIdx)?.size ?: 99
        val hlasiPrsi = odhozenaKarta && rukaMluvcihoPoTahu == 1

        // Svršek a změna barvy
        if (novy.zvolenaBarvaSvrskem != null &&
            (predchozi.cekaNaVolbuBarvy || novy.vrchniKarta.hodnota == HodnotaKarty.SVRSEK && predchozi.zvolenaBarvaSvrskem != novy.zvolenaBarvaSvrskem)
        ) {
            val barvaHlaska = when (novy.zvolenaBarvaSvrskem) {
                BarvaKarty.CERVENE -> vyberNahodne(listOf("Měním na červené!", "Červené poroučí!"))
                BarvaKarty.KULE -> vyberNahodne(listOf("Poroučím kule!", "Měním na kule!"))
                BarvaKarty.ZALUDY -> vyberNahodne(listOf("Budou žaludy!", "Měním na žaludy!"))
                BarvaKarty.ZELENE -> vyberNahodne(listOf("Zelené na stůl!", "Poroučím zelené!"))
            }
            val plna = if (hlasiPrsi) "$barvaHlaska A pozor, prší!" else barvaHlaska
            rekniHlasku(plna, mluvciJmeno, mluvciIdx)
            return
        }

        // Vynesení nebo přebití sedmičky
        if (novy.aktivniSedmyPocet > predchozi.aktivniSedmyPocet) {
            val karet = novy.aktivniSedmyPocet * 2
            if (karet >= 4) silnaHaptika()
            val textSedmy = when (karet) {
                2 -> vyberNahodne(listOf("Sedmička, ber dvě!", "Lízni si dvě!", "Dvě do ruky!"))
                4 -> vyberNahodne(listOf("Přitlačím, ber čtyři!", "Čtyři kousky do ruky!"))
                6 -> "Šest karet na cestu!"
                else -> "Osm karet, to už neuneseš!"
            }
            rekniHlasku(if (hlasiPrsi) "$textSedmy A prší!" else textSedmy, mluvciJmeno, mluvciIdx)
            return
        }

        // Líznutí trestu za sedmu
        if (predchozi.aktivniSedmyPocet > 0 && novy.aktivniSedmyPocet == 0) {
            val bral = predchozi.aktivniSedmyPocet * 2
            val reakce = if (bral >= 4) {
                vyberNahodne(listOf("No nazdar, plná ruka!", "To je nadílka!"))
            } else {
                vyberNahodne(listOf("Beru dvě.", "Dobrý, lížu dvě."))
            }
            rekniHlasku(reakce, mluvciJmeno, mluvciIdx)
            return
        }

        // Eso — stojíš
        if (novy.aktivniEsoStoji && odhozenaKarta && novy.vrchniKarta.hodnota == HodnotaKarty.ESO) {
            val textEsa = if (predchozi.aktivniEsoStoji) {
                vyberNahodne(listOf("Taky eso, pošli to dál!", "Přebíjím esem!"))
            } else {
                vyberNahodne(listOf("Eso, stůj!", "Stát, nikam nespěchej!", "Zastávka na jedno!"))
            }
            rekniHlasku(if (hlasiPrsi) "$textEsa A prší!" else textEsa, mluvciJmeno, mluvciIdx)
            return
        }

        // Potvrzení stání na eso
        if (predchozi.aktivniEsoStoji && !novy.aktivniEsoStoji && !odhozenaKarta) {
            rekniHlasku(vyberNahodne(listOf("Stojím.", "Tak já počkám.")), mluvciJmeno, mluvciIdx)
            return
        }

        // Poslední karta v ruce — Prší!
        if (hlasiPrsi) {
            silnaHaptika()
            rekniHlasku(vyberNahodne(listOf("Pozor, prší!", "Prší, pánové!")), mluvciJmeno, mluvciIdx)
        }
    }

    // -------------------------------------------------------------------------
    // SEDMA — vyhodnocení tahu a výběr hospodské hlášky
    // -------------------------------------------------------------------------
    fun zpracujZmenuSedmy(predchozi: StavSedma?, novy: StavSedma) {
        if (predchozi == null || novy.cisloTahu == predchozi.cisloTahu) return

        val mluvciIdx = predchozi.naTahu
        val mluvciJmeno = novy.jmenaHracu.getOrElse(mluvciIdx) { "" }
        val pustilBezKarty = predchozi.muzePustitStych && !novy.muzePustitStych &&
            novy.aktualniStych.isEmpty() &&
            novy.ruceHracu[mluvciIdx].size >= predchozi.ruceHracu[mluvciIdx].size

        if (!pustilBezKarty) {
            pleskniKartou()
        }

        // Spálená!
        if (predchozi.spalenaTym < 0 && novy.spalenaTym >= 0) {
            silnaHaptika()
            cinkniVyuctovani()
            rekniHlasku(
                vyberNahodne(
                    listOf(
                        "Spálená! Čtyři stejné na stole!",
                        "Spálená, čistá práce!"
                    )
                ),
                mluvciJmeno,
                mluvciIdx
            )
            return
        }

        // Konec rozdání Sedmy
        if (!predchozi.jeKonec && novy.jeKonec) {
            silnaHaptika()
            cinkniVyuctovani()
            val vitezStychu = novy.prozatimBereHrac
            val jm = novy.jmenaHracu.getOrElse(vitezStychu) { mluvciJmeno }
            val celkem0 = novy.uhraneBodyTym0
            val celkem1 = novy.uhraneBodyTym1
            val hlaskaKonec = if (celkem0 == 90 || celkem1 == 90) {
                "Čistá devadesátka, ani štych!"
            } else {
                "Poslední štych je doma!"
            }
            rekniHlasku(hlaskaKonec, jm, vitezStychu)
            return
        }

        if (pustilBezKarty) {
            rekniHlasku(vyberNahodne(listOf("Pouštím, vem si to.", "Nechám ti to.")), mluvciJmeno, mluvciIdx)
            return
        }

        // Právě přidaná karta ve štychu
        if (novy.aktualniStych.size > predchozi.aktualniStych.size) {
            val posledni = novy.aktualniStych.last()
            if (novy.aktualniStych.size > 1 && novy.prozatimBereHrac == posledni.indexHrace &&
                predchozi.prozatimBereHrac != posledni.indexHrace
            ) {
                val textPrebiti = if (posledni.karta.hodnota == HodnotaKarty.R7) {
                    vyberNahodne(listOf("Sedmička bere!", "Šup tam sedmu!"))
                } else {
                    vyberNahodne(listOf("Stejná, beru zatím já!", "Přebíjím!"))
                }
                rekniHlasku(textPrebiti, posledni.jmenoHrace, posledni.indexHrace)
            } else if (novy.pocetHracu == 4 && novy.aktualniStych.size > 1 &&
                posledni.karta.hodnota.bodyMariasSedma == 10 &&
                novy.tymHrace(posledni.indexHrace) == novy.tymHrace(novy.prozatimBereHrac) &&
                posledni.indexHrace != novy.prozatimBereHrac
            ) {
                rekniHlasku(vyberNahodne(listOf("Namažu ti desítku!", "Tady máš body, parťáku!")), posledni.jmenoHrace, posledni.indexHrace)
            }
            return
        }

        // Štych se právě uzavřel s tučným ziskem bodů
        if (predchozi.aktualniStych.isNotEmpty() && novy.aktualniStych.isEmpty()) {
            val prirustek = (novy.uhraneBodyTym0 - predchozi.uhraneBodyTym0) +
                (novy.uhraneBodyTym1 - predchozi.uhraneBodyTym1)
            if (prirustek >= 20) {
                val bereIdx = novy.naTahu
                val bereJmeno = novy.jmenaHracu.getOrElse(bereIdx) { "" }
                rekniHlasku(vyberNahodne(listOf("Pěkný štych do kapsy!", "Tučný štych je doma!")), bereJmeno, bereIdx)
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
            val hlaskaVolby = when {
                novy.zavazek == ZavazekMariase.BETL -> "Chystám betla, pánové!"
                novy.zavazek == ZavazekMariase.DURCH -> "Půjdeme na durch!"
                novy.volenoZTalonu -> "Z talonu, děj se vůle boží! Trumf ${novy.trumf?.ceskyNazev?.lowercase() ?: ""}!"
                novy.trumf == BarvaKarty.CERVENE -> vyberNahodne(
                    listOf("Červený, a hned za dvojnásobek!", "Volím červené!")
                )
                novy.trumf == BarvaKarty.KULE -> "Volím kule!"
                novy.trumf == BarvaKarty.ZALUDY -> "Volím žaludy!"
                else -> "Volím zelené!"
            }
            rekniHlasku(hlaskaVolby, mluvciJmeno, mluvciIdx)
            return
        }

        // 2) Odložení talonu a hlášení závazku
        if (predchozi.faze == FazeMariase.ODKLADANI_TALONU && novy.faze == FazeMariase.FLEKOVANI) {
            pleskniKartou()
            if (novy.zavazek != ZavazekMariase.HRA) silnaHaptika()
            val hlaskaZavazku = when (novy.zavazek) {
                ZavazekMariase.HRA -> vyberNahodne(listOf("Talon leží, hraju hru!", "Obyčejná hra, ukažte se!"))
                ZavazekMariase.HRA_A_SEDMA -> "Hlásím hru a sedmu k tomu!"
                ZavazekMariase.HRA_A_DVE_SEDMY -> "Hra a dvě sedmy na stůl!"
                ZavazekMariase.STO -> "Hlásím stovku!"
                ZavazekMariase.STO_A_SEDMA -> "Sto a sedma, pánové!"
                ZavazekMariase.STO_A_DVE_SEDMY -> "Sto a dvě sedmy, držte si klobouky!"
                ZavazekMariase.BETL -> "Hlásím betl, ani štych nevezmu!"
                ZavazekMariase.DURCH -> "Hlásím durch, beru úplně všechno!"
            }
            rekniHlasku(hlaskaZavazku, mluvciJmeno, mluvciIdx)
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
                val slovoFleku = when {
                    zvedlHru && zvedlSedmu -> "Flek na hru i na sedmu!"
                    vyssi == 2 && zvedlSedmu -> "Flek na sedmu, tu neuhraješ!"
                    vyssi == 2 -> vyberNahodne(listOf("Flek jak Brno!", "Flek na hru!"))
                    vyssi == 4 -> vyberNahodne(listOf("Re! Přitápíme pod kotlem!", "Dávám re!"))
                    vyssi == 8 -> "Tutti! Osmkrát, teď půjde do tuhého!"
                    vyssi == 16 -> "Boty na stůl!"
                    vyssi >= 32 -> "Kalhoty! Domů půjdeš v trenkách!"
                    else -> "Flek!"
                }
                rekniHlasku(slovoFleku, mluvciJmeno, mluvciIdx)
            } else {
                rekniHlasku(
                    vyberNahodne(listOf("Dobrý.", "Dobrý, nes!", "Nechám tě hrát, dobrý.")),
                    mluvciJmeno,
                    mluvciIdx
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
                val obranceJm = novy.jmenaHracu.getOrElse((novy.indexForhonta + 1) % 3) { "" }

                val tichaSedmaText = vyuctovani.polozky.firstOrNull {
                    it.popis.contains("Tichá", ignoreCase = true) || it.popis.contains("Zabitá", ignoreCase = true)
                }?.popis

                if (tichaSedmaText != null && tichaSedmaText.contains("Tichá (utajená) sedma")) {
                    rekniHlasku("Tichá sedma v posledním štychu!", forhontJm, novy.indexForhonta)
                } else if (tichaSedmaText != null && tichaSedmaText.contains("Zabitá")) {
                    rekniHlasku("Zabitá sedma v posledním štychu!", obranceJm, (novy.indexForhonta + 1) % 3)
                } else if (ziskForhonta > 0) {
                    rekniHlasku(
                        vyberNahodne(listOf("Uhráno, vysázejte tereziány!", "Je to doma, pánové!")),
                        forhontJm,
                        novy.indexForhonta
                    )
                } else if (ziskForhonta < 0) {
                    rekniHlasku(
                        vyberNahodne(listOf("Forhont padl, obrana slaví!", "Neušel jsi, zaplať obraně!")),
                        obranceJm,
                        (novy.indexForhonta + 1) % 3
                    )
                } else {
                    rekniHlasku("Smír, nikdo nikomu nic nedluží!", forhontJm, novy.indexForhonta)
                }
                return
            }

            // Hláška 20 nebo 40 v průběhu štychu
            if (prirustekHlasek >= 40) {
                silnaHaptika()
                rekniHlasku(
                    vyberNahodne(listOf("Čtyřicet, trumfová!", "Hlásím čtyřicet!")),
                    mluvciJmeno,
                    mluvciIdx
                )
            } else if (prirustekHlasek == 20) {
                silnaHaptika()
                rekniHlasku(
                    vyberNahodne(listOf("Dvacet!", "Hláška dvacet!")),
                    mluvciJmeno,
                    mluvciIdx
                )
            }
        }
    }
}
