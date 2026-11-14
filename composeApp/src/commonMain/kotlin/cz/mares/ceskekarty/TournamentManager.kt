package cz.mares.ceskekarty

import kotlin.math.abs

object TournamentManager {

    private const val POZDNI_REGISTRACE_OKNO_MS = 10 * 60_000L
    private const val ARCHIV_TURNAJU_MS = 36 * 3600_000L
    private const val DOBA_SIMULACE_BOT_ZAPASU_MS = 28_000L

    private suspend fun ulozPlanovanyTurnaj(turnaj: PlanovanyTurnaj): PlanovanyTurnaj? {
        val payload = serializujPlanovanyTurnaj(turnaj)
        val nyni = OnlineManager.odhadniAktualniServerovyCasMs()
        val telo = """{"payload":${jsonEscape(payload)},"updatedAt":$nyni}"""
        val res = OnlineManager.dotazSOpakovanim(
            metoda = "PUT",
            url = "$FIREBASE_DB_ZAKLAD/card_tournaments/${turnaj.turnajId}.json",
            jsonTelo = telo,
            pokusy = 2
        )
        return if (res.uspesne) turnaj else null
    }

    suspend fun nactiKonkretniTurnaj(turnajId: String): PlanovanyTurnaj? {
        if (turnajId.isBlank()) return null
        val res = OnlineManager.dotazSOpakovanim(
            metoda = "GET",
            url = "$FIREBASE_DB_ZAKLAD/card_tournaments/$turnajId.json",
            pokusy = 2
        )
        if (!res.uspesne || res.telo.isBlank() || res.telo.trim() == "null") return null
        val obj = parsujJsonText(res.telo) as? JsonPrvek.Objekt ?: return null
        return deserializujPlanovanyTurnaj(obj.retezec("payload", ""))
    }

    suspend fun synchronizujANactiRozvrhTurnaju(): List<PlanovanyTurnaj> {
        val nyniMs = OnlineManager.ziskejOverenyCasServeruMs()
        val res = OnlineManager.dotazSOpakovanim(
            metoda = "GET",
            url = "$FIREBASE_DB_ZAKLAD/card_tournaments.json",
            pokusy = 2
        )

        val mapaExistujicich = LinkedHashMap<String, PlanovanyTurnaj>()
        if (res.uspesne && res.telo.isNotBlank() && res.telo.trim() != "null") {
            val koren = parsujJsonText(res.telo) as? JsonPrvek.Objekt
            if (koren != null) {
                for ((id, prvek) in koren.polozky) {
                    val obj = prvek as? JsonPrvek.Objekt ?: continue
                    val surovyPayload = obj.retezec("payload", "")
                    val turnaj = deserializujPlanovanyTurnaj(surovyPayload)
                    if (turnaj == null) {
                        provedSitovyDotaz("DELETE", "$FIREBASE_DB_ZAKLAD/card_tournaments/$id.json")
                        continue
                    }
                    if (nyniMs - turnaj.casStartuMs > ARCHIV_TURNAJU_MS) {
                        provedSitovyDotaz("DELETE", "$FIREBASE_DB_ZAKLAD/card_tournaments/$id.json")
                        continue
                    }
                    if (surovyPayload.contains("Večerní pohár:") || surovyPayload.contains("Královský pohár:")) {
                        ulozPlanovanyTurnaj(turnaj)
                    }
                    mapaExistujicich[turnaj.turnajId] = turnaj
                }
            }
        }

        val dnesPraha = TurnajKalendar.utcMsNaPrahaCas(nyniMs)
        for (posunDne in 0..1) {
            val cilovyEpochDen = dnesPraha.epochDen + posunDne
            val (rok, mesic, den) = TurnajKalendar.epochDenNaDatum(cilovyEpochDen)
            val datumKlic = "${rok}${mesic.toString().padStart(2, '0')}${den.toString().padStart(2, '0')}"

            DENNI_ROZVRH_TURNAJU.forEachIndexed { idxSlotu, sablona ->
                val tid = "auto_${datumKlic}_${sablona.kodSlotu}"
                if (!mapaExistujicich.containsKey(tid)) {
                    val startMs = TurnajKalendar.prahaCasNaUtcMs(
                        rok = rok,
                        mesic = mesic,
                        den = den,
                        hodina = sablona.hodina,
                        minuta = sablona.minuta
                    )
                    if (nyniMs <= startMs + POZDNI_REGISTRACE_OKNO_MS) {
                        val varianta = TurnajKalendar.variantaProEpochDen(cilovyEpochDen, idxSlotu)
                        val novy = PlanovanyTurnaj(
                            turnajId = tid,
                            nazev = sablona.nazev,
                            typHry = sablona.typHry,
                            variantaPravidel = varianta,
                            kapacita = sablona.kapacita,
                            casStartuMs = startMs,
                            vkladTereziany = sablona.vkladTereziany,
                            dotaceSine = sablona.dotaceSine,
                            stav = "REGISTRACE",
                            ucastnici = emptyList(),
                            uzlyPavouka = emptyList(),
                            celkovyVitezId = "",
                            celkovyVitezJmeno = "",
                            druheMistoId = "",
                            druheMistoJmeno = "",
                            vyplaceneIdHracu = emptyList(),
                            vytvorenoMs = nyniMs
                        )
                        val ulozeny = ulozPlanovanyTurnaj(novy) ?: novy
                        mapaExistujicich[tid] = ulozeny
                    }
                }
            }
        }

        val aktualizovane = ArrayList<PlanovanyTurnaj>(mapaExistujicich.size)
        for (turnaj in mapaExistujicich.values) {
            aktualizovane.add(zkontrolujAPosunStavTurnaje(turnaj, nyniMs))
        }

        return aktualizovane.sortedWith(
            compareBy<PlanovanyTurnaj> {
                when (it.stav) {
                    "PROBIHA" -> 0
                    "REGISTRACE" -> 1
                    else -> 2
                }
            }.thenBy {
                if (it.stav == "DOKONCENO") -it.casStartuMs else it.casStartuMs
            }
        )
    }

    suspend fun prihlasSeDoTurnaje(turnajId: String): Result<PlanovanyTurnaj> {
        val nick = nactiUlozenouPrezdivku().trim()
        if (nick.isEmpty()) {
            return Result.failure(Exception("Pro vstup do turnaje si nejprve ulož přezdívku na úvodní obrazovce."))
        }

        val nyniMs = OnlineManager.ziskejOverenyCasServeruMs()
        val cerstvy = nactiKonkretniTurnaj(turnajId)
            ?: return Result.failure(Exception("Turnaj nebyl nalezen — obnov prosím přehled."))

        val mojeId = ziskejUnikatniIdZarizeni()
        if (cerstvy.jeHracPrihlasen(mojeId)) {
            return Result.success(zkontrolujAPosunStavTurnaje(cerstvy, nyniMs))
        }
        if (cerstvy.stav != "REGISTRACE") {
            return Result.failure(Exception("Registrace do tohoto turnaje je již uzavřena."))
        }
        if (nyniMs > cerstvy.casStartuMs + POZDNI_REGISTRACE_OKNO_MS) {
            return Result.failure(Exception("Termín pro přihlášení do tohoto turnaje již vypršel."))
        }
        val ziviUcastnici = cerstvy.ucastnici.filterNot { jeBotDeviceId(it.deviceId) }
        if (ziviUcastnici.size >= cerstvy.kapacita) {
            return Result.failure(Exception("Turnajový pavouk je již plně obsazen (${cerstvy.kapacita}/${cerstvy.kapacita})."))
        }

        val aktualniTereziany = nactiLokalniTereziany()
        if (aktualniTereziany < cerstvy.vkladTereziany) {
            return Result.failure(
                Exception("Potřebuješ startovné ${formatujTereziany(cerstvy.vkladTereziany)} — vyzvedni si odměnu nebo Císařskou podporu.")
            )
        }

        val novyZustatek = aktualniTereziany - cerstvy.vkladTereziany
        OnlineManager.odesliAktualniTerezianyDoZebricku(novyZustatek)

        val upraveny = cerstvy.copy(
            ucastnici = ziviUcastnici + HracUStolu(mojeId, nick, novyZustatek)
        )
        val ulozeny = ulozPlanovanyTurnaj(upraveny)
        if (ulozeny == null) {
            OnlineManager.odesliAktualniTerezianyDoZebricku(aktualniTereziany)
            return Result.failure(Exception("Nepodařilo se zapsat přihlášku na server."))
        }

        val poKontroleStartu = zkontrolujAPosunStavTurnaje(ulozeny, nyniMs)
        return Result.success(poKontroleStartu)
    }

    suspend fun odhlasSeZTurnaje(turnajId: String): Result<PlanovanyTurnaj> {
        val nyniMs = OnlineManager.ziskejOverenyCasServeruMs()
        val cerstvy = nactiKonkretniTurnaj(turnajId)
            ?: return Result.failure(Exception("Turnaj nebyl nalezen."))

        val mojeId = ziskejUnikatniIdZarizeni()
        if (!cerstvy.jeHracPrihlasen(mojeId)) {
            return Result.success(cerstvy)
        }
        if (cerstvy.stav != "REGISTRACE" || nyniMs >= cerstvy.casStartuMs - 60_000L) {
            return Result.failure(Exception("Těsně před výkopem nebo po startu se již nelze odhlásit."))
        }

        val zbyli = cerstvy.ucastnici.filterNot { it.deviceId == mojeId }
        val upraveny = cerstvy.copy(ucastnici = zbyli)
        val ulozeny = ulozPlanovanyTurnaj(upraveny)
            ?: return Result.failure(Exception("Odhlášení se nepodařilo uložit."))

        val vraceno = (nactiLokalniTereziany() + cerstvy.vkladTereziany).coerceIn(0, 999_999)
        OnlineManager.odesliAktualniTerezianyDoZebricku(vraceno)
        return Result.success(ulozeny)
    }

    suspend fun zkontrolujVyplatuOdmenyLokalnimuHraci(turnaje: List<PlanovanyTurnaj>): String? {
        val mojeId = ziskejUnikatniIdZarizeni()
        if (mojeId.isBlank() || jeBotDeviceId(mojeId)) return null

        for (t in turnaje) {
            if (t.stav != "DOKONCENO") continue
            if (mojeId in t.vyplaceneIdHracu) continue

            val (odmena, poradiText) = when (mojeId) {
                t.celkovyVitezId -> t.odmenaPrvniMisto to "1. místo"
                t.druheMistoId -> t.odmenaDruheMisto to "2. místo"
                else -> 0 to ""
            }
            if (odmena <= 0) continue

            val cerstvy = nactiKonkretniTurnaj(t.turnajId) ?: t
            if (mojeId in cerstvy.vyplaceneIdHracu) continue

            val oznaceny = cerstvy.copy(vyplaceneIdHracu = cerstvy.vyplaceneIdHracu + mojeId)
            val ulozeno = ulozPlanovanyTurnaj(oznaceny)
            if (ulozeno != null) {
                val novyMesec = (nactiLokalniTereziany() + odmena).coerceIn(0, 999_999)
                OnlineManager.odesliAktualniTerezianyDoZebricku(novyMesec)
                return "Gratulace! Za $poradiText v turnaji „${t.nazev}“ získáváš +${formatujTereziany(odmena)}!"
            }
        }
        return null
    }

    private fun deterministickyHashZapasu(turnajId: String, slot: Int): Int {
        var h = 17
        val klic = "$turnajId#$slot"
        for (ch in klic) {
            h = h * 31 + ch.code
        }
        return abs(h)
    }

    private fun nastavPocatecniSkoreUzlu(typHry: TypHry, hraci: List<HracVeSlotuPavouka>): List<HracVeSlotuPavouka> {
        val vychozi = formatujSkoreDoPavouka(typHry, 0)
        return hraci.map { h ->
            if (h.jeVolneMisto) h else h.copy(skoreText = h.skoreText.ifEmpty { vychozi })
        }
    }

    private fun vygenerujSimulovanyVysledekBotu(
        turnaj: PlanovanyTurnaj,
        uzel: UzelZapasuPavouka
    ): UzelZapasuPavouka {
        val h = deterministickyHashZapasu(turnaj.turnajId, uzel.slot)
        val idxVit = h % uzel.hraci.size.coerceAtLeast(1)
        val vit = uzel.hraci[idxVit]

        val hraciSeSkore = if (turnaj.typHry == TypHry.MARIAS) {
            val ziskViteze = listOf(50, 60, 80, 90)[h % 4]
            val ztrataPrvniho = -(ziskViteze / 3 / 10 * 10).coerceAtLeast(20)
            val ztrataDruheho = -(ziskViteze + ztrataPrvniho)
            var poradiPorazenych = 0
            uzel.hraci.mapIndexed { idx, hrac ->
                val body = if (idx == idxVit) {
                    ziskViteze
                } else {
                    poradiPorazenych++
                    if (poradiPorazenych == 1) ztrataPrvniho else ztrataDruheho
                }
                hrac.copy(skoreText = formatujSkoreDoPavouka(TypHry.MARIAS, body))
            }
        } else {
            val prohraneBitvy = (h / 7) % CIL_VITEZNYCH_BITEV_DUELU
            uzel.hraci.mapIndexed { idx, hrac ->
                val vyhry = if (idx == idxVit) CIL_VITEZNYCH_BITEV_DUELU else prohraneBitvy
                hrac.copy(skoreText = vyhry.toString())
            }
        }

        return uzel.copy(
            hraci = hraciSeSkore,
            vitezId = vit.deviceId,
            vitezJmeno = vit.prezdivka,
            stavZapasu = "DOHRANO"
        )
    }

    private suspend fun zajistiStulProZapasUzlu(
        turnaj: PlanovanyTurnaj,
        uzel: UzelZapasuPavouka
    ): String {
        if (uzel.roomId.isNotEmpty()) {
            val existujici = OnlineManager.nactiKonkretniStul(uzel.roomId)
            if (existujici != null) return existujici.roomId
        }
        val hraciStolu = uzel.hraci.map { hs ->
            val nalezeny = turnaj.ucastnici.firstOrNull { it.deviceId == hs.deviceId }
            nalezeny ?: HracUStolu(hs.deviceId, hs.prezdivka, STARTOVNI_TEREZIANY)
        }
        val maxRozdaniSerie = if (turnaj.typHry == TypHry.MARIAS) {
            POCET_ROZDANI_MARIAS_TURNAJ
        } else {
            MAX_HER_DUEL_SERIE
        }
        val stul = OnlineManager.vytvorStul(
            nazevStolu = "${turnaj.nazev} • ${uzel.nazevZapasu}",
            typHry = turnaj.typHry,
            pocetHracu = hraciStolu.size,
            turnajId = turnaj.turnajId,
            slotZapasu = uzel.slot,
            celkemRozdani = maxRozdaniSerie,
            predvyplneniHraci = hraciStolu,
            variantaPravidel = turnaj.variantaPravidel
        )
        return stul?.roomId ?: ""
    }

    suspend fun zkontrolujAPosunStavTurnaje(turnaj: PlanovanyTurnaj, nyniMs: Long): PlanovanyTurnaj {
        var aktualni = turnaj

        if (aktualni.stav == "REGISTRACE") {
            val zivi = aktualni.ucastnici.filterNot { jeBotDeviceId(it.deviceId) }
            if (nyniMs >= aktualni.casStartuMs && zivi.isNotEmpty()) {
                val (plniUcastnici, startUzly) = sestavStartovniUzlySHraciABoty(aktualni)
                val pripraveneUzly = startUzly.map { uzel ->
                    if (uzel.kolo == 0) {
                        val maZiveho = uzel.hraci.any { !it.jeBot && !it.jeVolneMisto }
                        val seSkore = nastavPocatecniSkoreUzlu(aktualni.typHry, uzel.hraci)
                        if (maZiveho) {
                            val docasny = aktualni.copy(ucastnici = plniUcastnici)
                            val rid = zajistiStulProZapasUzlu(docasny, uzel)
                            uzel.copy(hraci = seSkore, roomId = rid, stavZapasu = "HRAJE_SE")
                        } else {
                            uzel.copy(hraci = seSkore, planovanyCasMs = nyniMs, stavZapasu = "HRAJE_SE")
                        }
                    } else {
                        uzel
                    }
                }
                val odstartovany = aktualni.copy(
                    stav = "PROBIHA",
                    ucastnici = plniUcastnici,
                    uzlyPavouka = pripraveneUzly
                )
                aktualni = ulozPlanovanyTurnaj(odstartovany) ?: odstartovany
            } else if (nyniMs > aktualni.casStartuMs + POZDNI_REGISTRACE_OKNO_MS && zivi.isEmpty()) {
                val uzavreny = aktualni.copy(
                    stav = "DOKONCENO",
                    celkovyVitezJmeno = "Turnaj bez přihlášených hráčů"
                )
                return ulozPlanovanyTurnaj(uzavreny) ?: uzavreny
            } else {
                return aktualni
            }
        }

        if (aktualni.stav != "PROBIHA") return aktualni

        var dosloKeZmene = false
        val uzly = aktualni.uzlyPavouka.toMutableList()

        for (i in uzly.indices) {
            val u = uzly[i]
            val vsichniObsazeni = u.hraci.isNotEmpty() && u.hraci.none { it.jeVolneMisto }
            if (!vsichniObsazeni || u.vitezId.isNotEmpty()) continue

            val vsichniBoti = u.hraci.all { it.jeBot }
            if (vsichniBoti) {
                if (u.stavZapasu == "CEKA") {
                    uzly[i] = u.copy(
                        hraci = nastavPocatecniSkoreUzlu(aktualni.typHry, u.hraci),
                        planovanyCasMs = maxOf(u.planovanyCasMs, nyniMs),
                        stavZapasu = "HRAJE_SE"
                    )
                    dosloKeZmene = true
                } else if (nyniMs >= u.planovanyCasMs + DOBA_SIMULACE_BOT_ZAPASU_MS) {
                    uzly[i] = vygenerujSimulovanyVysledekBotu(aktualni, u)
                    dosloKeZmene = true
                }
            } else if (u.roomId.isEmpty()) {
                val rid = zajistiStulProZapasUzlu(aktualni, u)
                if (rid.isNotEmpty()) {
                    uzly[i] = u.copy(
                        hraci = nastavPocatecniSkoreUzlu(aktualni.typHry, u.hraci),
                        roomId = rid,
                        stavZapasu = "HRAJE_SE"
                    )
                    dosloKeZmene = true
                }
            }
        }

        val prepocteny = propisPostupujiciDoDalsichKol(aktualni.copy(uzlyPavouka = uzly), nyniMs)
        if (prepocteny != aktualni) {
            dosloKeZmene = true
            aktualni = prepocteny
        } else if (dosloKeZmene) {
            aktualni = aktualni.copy(uzlyPavouka = uzly)
        }

        return if (dosloKeZmene) {
            ulozPlanovanyTurnaj(aktualni) ?: aktualni
        } else {
            aktualni
        }
    }

    private suspend fun propisPostupujiciDoDalsichKol(
        turnaj: PlanovanyTurnaj,
        nyniMs: Long
    ): PlanovanyTurnaj {
        val uzly = turnaj.uzlyPavouka.toMutableList()
        if (uzly.isEmpty()) return turnaj

        if (turnaj.typHry == TypHry.MARIAS && uzly.size >= 4) {
            val finale = uzly[3]
            val noviFinaliste = (0..2).map { sfSlot ->
                val sf = uzly[sfSlot]
                val existujiciSlot = finale.hraci.getOrNull(sfSlot)
                if (sf.vitezId.isNotEmpty()) {
                    val zachovaneSkore = if (existujiciSlot?.deviceId == sf.vitezId) existujiciSlot.skoreText else ""
                    HracVeSlotuPavouka(
                        deviceId = sf.vitezId,
                        prezdivka = sf.vitezJmeno,
                        jeBot = jeBotDeviceId(sf.vitezId),
                        jeVolneMisto = false,
                        skoreText = zachovaneSkore
                    )
                } else {
                    existujiciSlot ?: HracVeSlotuPavouka(
                        deviceId = "",
                        prezdivka = "Vítěz SF ${('A' + sfSlot)}",
                        jeBot = false,
                        jeVolneMisto = true
                    )
                }
            }
            var noveFinale = finale.copy(hraci = noviFinaliste)
            val finaleKompletni = noviFinaliste.none { it.jeVolneMisto }
            if (finaleKompletni && noveFinale.vitezId.isEmpty()) {
                val seSkore = nastavPocatecniSkoreUzlu(turnaj.typHry, noveFinale.hraci)
                val maZiveho = noviFinaliste.any { !it.jeBot }
                if (maZiveho && noveFinale.roomId.isEmpty()) {
                    val rid = zajistiStulProZapasUzlu(turnaj, noveFinale)
                    noveFinale = noveFinale.copy(hraci = seSkore, roomId = rid, stavZapasu = "HRAJE_SE")
                } else if (!maZiveho && noveFinale.stavZapasu == "CEKA") {
                    noveFinale = noveFinale.copy(hraci = seSkore, planovanyCasMs = nyniMs, stavZapasu = "HRAJE_SE")
                }
            }
            uzly[3] = noveFinale

            if (noveFinale.vitezId.isNotEmpty()) {
                val druhy = turnaj.druheMistoId.ifEmpty {
                    noveFinale.hraci.firstOrNull { it.deviceId != noveFinale.vitezId }?.deviceId ?: ""
                }
                val druhyJmeno = turnaj.druheMistoJmeno.ifEmpty {
                    noveFinale.hraci.firstOrNull { it.deviceId != noveFinale.vitezId }?.prezdivka ?: ""
                }
                return turnaj.copy(
                    stav = "DOKONCENO",
                    uzlyPavouka = uzly,
                    celkovyVitezId = noveFinale.vitezId,
                    celkovyVitezJmeno = noveFinale.vitezJmeno,
                    druheMistoId = druhy,
                    druheMistoJmeno = druhyJmeno
                )
            }
            return turnaj.copy(uzlyPavouka = uzly)
        }

        if (uzly.size >= 7) {
            for (sfIdx in 0..1) {
                val cilSlot = 4 + sfIdx
                val zdrojA = uzly[sfIdx * 2]
                val zdrojB = uzly[sfIdx * 2 + 1]
                val aktualniSf = uzly[cilSlot]
                val staryH1 = aktualniSf.hraci[0]
                val staryH2 = aktualniSf.hraci[1]

                val novyHrac1 = if (zdrojA.vitezId.isNotEmpty()) {
                    val sk1 = if (staryH1.deviceId == zdrojA.vitezId) staryH1.skoreText else ""
                    HracVeSlotuPavouka(zdrojA.vitezId, zdrojA.vitezJmeno, jeBotDeviceId(zdrojA.vitezId), false, sk1)
                } else {
                    staryH1
                }
                val novyHrac2 = if (zdrojB.vitezId.isNotEmpty()) {
                    val sk2 = if (staryH2.deviceId == zdrojB.vitezId) staryH2.skoreText else ""
                    HracVeSlotuPavouka(zdrojB.vitezId, zdrojB.vitezJmeno, jeBotDeviceId(zdrojB.vitezId), false, sk2)
                } else {
                    staryH2
                }
                var noveSf = aktualniSf.copy(hraci = listOf(novyHrac1, novyHrac2))
                if (!novyHrac1.jeVolneMisto && !novyHrac2.jeVolneMisto && noveSf.vitezId.isEmpty()) {
                    val seSkore = nastavPocatecniSkoreUzlu(turnaj.typHry, noveSf.hraci)
                    val maZiveho = !novyHrac1.jeBot || !novyHrac2.jeBot
                    if (maZiveho && noveSf.roomId.isEmpty()) {
                        val rid = zajistiStulProZapasUzlu(turnaj, noveSf)
                        noveSf = noveSf.copy(hraci = seSkore, roomId = rid, stavZapasu = "HRAJE_SE")
                    } else if (!maZiveho && noveSf.stavZapasu == "CEKA") {
                        noveSf = noveSf.copy(hraci = seSkore, planovanyCasMs = nyniMs, stavZapasu = "HRAJE_SE")
                    }
                }
                uzly[cilSlot] = noveSf
            }

            val sf1 = uzly[4]
            val sf2 = uzly[5]
            val aktFin = uzly[6]
            val staryFin1 = aktFin.hraci[0]
            val staryFin2 = aktFin.hraci[1]

            val finH1 = if (sf1.vitezId.isNotEmpty()) {
                val sk1 = if (staryFin1.deviceId == sf1.vitezId) staryFin1.skoreText else ""
                HracVeSlotuPavouka(sf1.vitezId, sf1.vitezJmeno, jeBotDeviceId(sf1.vitezId), false, sk1)
            } else {
                staryFin1
            }
            val finH2 = if (sf2.vitezId.isNotEmpty()) {
                val sk2 = if (staryFin2.deviceId == sf2.vitezId) staryFin2.skoreText else ""
                HracVeSlotuPavouka(sf2.vitezId, sf2.vitezJmeno, jeBotDeviceId(sf2.vitezId), false, sk2)
            } else {
                staryFin2
            }
            var noveFin = aktFin.copy(hraci = listOf(finH1, finH2))
            if (!finH1.jeVolneMisto && !finH2.jeVolneMisto && noveFin.vitezId.isEmpty()) {
                val seSkore = nastavPocatecniSkoreUzlu(turnaj.typHry, noveFin.hraci)
                val maZiveho = !finH1.jeBot || !finH2.jeBot
                if (maZiveho && noveFin.roomId.isEmpty()) {
                    val rid = zajistiStulProZapasUzlu(turnaj, noveFin)
                    noveFin = noveFin.copy(hraci = seSkore, roomId = rid, stavZapasu = "HRAJE_SE")
                } else if (!maZiveho && noveFin.stavZapasu == "CEKA") {
                    noveFin = noveFin.copy(hraci = seSkore, planovanyCasMs = nyniMs, stavZapasu = "HRAJE_SE")
                }
            }
            uzly[6] = noveFin

            if (noveFin.vitezId.isNotEmpty()) {
                val porazenyFinalista = noveFin.hraci.firstOrNull { it.deviceId != noveFin.vitezId }
                return turnaj.copy(
                    stav = "DOKONCENO",
                    uzlyPavouka = uzly,
                    celkovyVitezId = noveFin.vitezId,
                    celkovyVitezJmeno = noveFin.vitezJmeno,
                    druheMistoId = porazenyFinalista?.deviceId ?: turnaj.druheMistoId,
                    druheMistoJmeno = porazenyFinalista?.prezdivka ?: turnaj.druheMistoJmeno
                )
            }
        }

        return turnaj.copy(uzlyPavouka = uzly)
    }

    suspend fun zapisPrubezneSkoreZapasu(
        turnajId: String,
        slotZapasu: Int,
        skoreHracu: List<Pair<String, String>>
    ) {
        if (turnajId.isBlank() || skoreHracu.isEmpty()) return
        val cerstvy = nactiKonkretniTurnaj(turnajId) ?: return
        if (cerstvy.stav != "PROBIHA") return

        var zmena = false
        val upraveneUzly = cerstvy.uzlyPavouka.map { uzel ->
            if (uzel.slot == slotZapasu && uzel.vitezId.isEmpty()) {
                val noviHraci = uzel.hraci.map { h ->
                    val noveSkore = skoreHracu.firstOrNull { it.first == h.deviceId }?.second
                    if (noveSkore != null && noveSkore != h.skoreText) {
                        zmena = true
                        h.copy(skoreText = noveSkore)
                    } else {
                        h
                    }
                }
                uzel.copy(hraci = noviHraci)
            } else {
                uzel
            }
        }
        if (zmena) {
            ulozPlanovanyTurnaj(cerstvy.copy(uzlyPavouka = upraveneUzly))
        }
    }

    suspend fun zapisVysledekZapasu(
        turnajId: String,
        slotZapasu: Int,
        vitezDeviceId: String,
        vitezJmeno: String,
        druhyDeviceId: String = "",
        druhyJmeno: String = "",
        skoreHracu: List<Pair<String, String>> = emptyList()
    ) {
        if (turnajId.isBlank() || vitezDeviceId.isBlank()) return
        val nyniMs = OnlineManager.ziskejOverenyCasServeruMs()
        val cerstvy = nactiKonkretniTurnaj(turnajId) ?: return
        if (cerstvy.stav == "DOKONCENO") return

        val upraveneUzly = cerstvy.uzlyPavouka.map { uzel ->
            if (uzel.slot == slotZapasu && uzel.vitezId.isEmpty()) {
                val upraveniHraci = uzel.hraci.map { h ->
                    val predaneSkore = skoreHracu.firstOrNull { it.first == h.deviceId }?.second
                    val vysledneSkore = when {
                        !predaneSkore.isNullOrEmpty() -> predaneSkore
                        cerstvy.typHry != TypHry.MARIAS && h.deviceId == vitezDeviceId ->
                            CIL_VITEZNYCH_BITEV_DUELU.toString()
                        h.skoreText.isNotEmpty() -> h.skoreText
                        else -> formatujSkoreDoPavouka(cerstvy.typHry, 0)
                    }
                    h.copy(skoreText = vysledneSkore)
                }
                uzel.copy(
                    hraci = upraveniHraci,
                    vitezId = vitezDeviceId,
                    vitezJmeno = vitezJmeno,
                    stavZapasu = "DOHRANO"
                )
            } else {
                uzel
            }
        }

        val jeFinalovySlot = (cerstvy.typHry == TypHry.MARIAS && slotZapasu == 3) ||
            (cerstvy.typHry != TypHry.MARIAS && slotZapasu == 6)

        val mezistav = cerstvy.copy(
            uzlyPavouka = upraveneUzly,
            druheMistoId = if (jeFinalovySlot && druhyDeviceId.isNotEmpty()) druhyDeviceId else cerstvy.druheMistoId,
            druheMistoJmeno = if (jeFinalovySlot && druhyJmeno.isNotEmpty()) druhyJmeno else cerstvy.druheMistoJmeno
        )
        val posunuty = propisPostupujiciDoDalsichKol(mezistav, nyniMs)
        ulozPlanovanyTurnaj(posunuty)
    }

    suspend fun vstupDoAktivnihoZapasuHrace(turnaj: PlanovanyTurnaj): Result<OnlineStul> {
        val mojeId = ziskejUnikatniIdZarizeni()
        val uzel = turnaj.aktivniZapasHrace(mojeId)
            ?: return Result.failure(Exception("Momentálně nemáš v tomto turnaji aktivní rozehraný stůl."))

        var stul = OnlineManager.nactiKonkretniStul(uzel.roomId)
        if (stul == null) {
            val rid = zajistiStulProZapasUzlu(turnaj, uzel)
            if (rid.isNotEmpty()) {
                stul = OnlineManager.nactiKonkretniStul(rid)
            }
        }
        return if (stul != null) {
            OnlineManager.odesliTepSpojeni(stul.roomId)
            Result.success(stul)
        } else {
            Result.failure(Exception("Nepodařilo se otevřít turnajový stůl — zkus to za okamžik znovu."))
        }
    }
}
