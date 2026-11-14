package cz.mares.ceskekarty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun PrsiEngine.zKompaktnihoRetezce(data: String): StavPrsi? =
    StavPrsi.zKompaktnihoRetezce(data)

internal fun SedmaEngine.zKompaktnihoRetezce(data: String): StavSedma? =
    StavSedma.zKompaktnihoRetezce(data)

internal fun MariasEngine.zKompaktnihoRetezce(data: String): StavMarias? =
    StavMarias.zKompaktnihoRetezce(data)

data class KonfiguraceOfflineHry(
    val typHry: TypHry,
    val pocetHracu: Int,
    val variantaPravidel: VariantaPravidel = VariantaPravidel.HOSPODSKA,
    val jeOfflinePohar: Boolean = false,
    val celkemRozdani: Int = 3
)

internal data class UiPolozkaVyuctovani(
    val popis: String,
    val hodnotaText: String,
    val jeZvyraznena: Boolean
)

internal data class UiHracVyuctovani(
    val indexHrace: Int,
    val jmenoHrace: String,
    val jeVitez: Boolean,
    val bodyZapasu: Int,
    val zmenaTerezianu: Int
)

internal data class UiVyuctovani(
    val nadpis: String,
    val podnadpis: String,
    val polozky: List<UiPolozkaVyuctovani>,
    val hraci: List<UiHracVyuctovani>
)

private fun VysledekPartie.naUiVyuctovani(): UiVyuctovani {
    return UiVyuctovani(
        nadpis = nadpis,
        podnadpis = podnadpis,
        polozky = polozky.map { UiPolozkaVyuctovani(it.popis, it.hodnotaText, it.jeZvyraznena) },
        hraci = hraci.map {
            UiHracVyuctovani(
                indexHrace = it.indexHrace,
                jmenoHrace = it.jmenoHrace,
                jeVitez = it.jeVitez,
                bodyZapasu = it.bodyZapasu,
                zmenaTerezianu = it.zmenaElo
            )
        }
    )
}

private fun VyuctovaniMariase.naUiVyuctovani(): UiVyuctovani {
    return UiVyuctovani(
        nadpis = nadpis,
        podnadpis = podnadpis,
        polozky = polozky.map { UiPolozkaVyuctovani(it.popis, it.hodnotaText, it.jeZvyraznena) },
        hraci = hraci.map {
            UiHracVyuctovani(
                indexHrace = it.indexHrace,
                jmenoHrace = it.jmenoHrace,
                jeVitez = it.jeVitez,
                bodyZapasu = it.bodyZapasu,
                zmenaTerezianu = it.zmenaTerezianu
            )
        }
    )
}

internal fun vypoctiNoveSkoreSerieOnline(
    stul: OnlineStul,
    vysledek: UiVyuctovani
): List<Int> {
    val naVitezneBitvy = stul.turnajId.isNotEmpty() && stul.typHry != TypHry.MARIAS
    return List(stul.hraci.size) { idx ->
        val dosavadni = stul.soucetBoduHracu.getOrElse(idx) { 0 }
        val hracVys = vysledek.hraci.firstOrNull { it.indexHrace == idx }
        val prirustek = if (naVitezneBitvy) {
            if (hracVys?.jeVitez == true) 1 else 0
        } else {
            hracVys?.bodyZapasu ?: 0
        }
        dosavadni + prirustek
    }
}

internal fun jeOnlineTurnajovaSerieHotova(
    stul: OnlineStul,
    vysledek: UiVyuctovani
): Boolean {
    if (stul.turnajId.isEmpty()) return false
    val noveSkore = vypoctiNoveSkoreSerieOnline(stul, vysledek)
    return if (stul.typHry != TypHry.MARIAS) {
        noveSkore.any { it >= CIL_VITEZNYCH_BITEV_DUELU } || stul.cisloRozdani >= stul.celkemRozdani
    } else {
        stul.cisloRozdani >= stul.celkemRozdani
    }
}

internal fun jeOfflinePoharHotovy(
    offlineKonfig: KonfiguraceOfflineHry?,
    cisloOfflineRozdani: Int,
    soucetBoduOffline: List<Int>
): Boolean {
    if (offlineKonfig?.jeOfflinePohar != true) return false
    return if (offlineKonfig.typHry != TypHry.MARIAS) {
        soucetBoduOffline.any { it >= CIL_VITEZNYCH_BITEV_DUELU } ||
            cisloOfflineRozdani >= MAX_HER_DUEL_SERIE
    } else {
        cisloOfflineRozdani >= offlineKonfig.celkemRozdani
    }
}

internal fun sklonujListy(pocet: Int): String {
    return when {
        pocet == 1 -> "1 list"
        pocet in 2..4 -> "$pocet listy"
        else -> "$pocet listů"
    }
}

internal fun sklonujKartyAkuzativ(pocet: Int): String {
    return when {
        pocet == 1 -> "1 kartu"
        pocet in 2..4 -> "$pocet karty"
        else -> "$pocet karet"
    }
}

@Composable
fun GameTableScreen(
    offlineKonfig: KonfiguraceOfflineHry?,
    pocatecniOnlineStul: OnlineStul?,
    onVratitDoCekarny: (OnlineStul) -> Unit = {},
    onOpustitStul: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val mojeDeviceId = remember { ziskejUnikatniIdZarizeni() }
    val mojePrezdivka = remember { nactiUlozenouPrezdivku().ifBlank { "Ty" } }
    val casVstupuKeStoluMs = remember { ziskejAktualniCasMs() }

    val jeOnlineRezim = pocatecniOnlineStul != null
    var onlineStul by remember { mutableStateOf(pocatecniOnlineStul) }
    var tikSekundyMs by remember { mutableStateOf(ziskejAktualniCasMs()) }
    var potvrditOdchod by remember { mutableStateOf(false) }
    var aktualniMesecTerezianu by remember { mutableStateOf(nactiLokalniTereziany()) }
    var zvukZapnut by remember { mutableStateOf(jeZvukAHlaskyZapnuty()) }

    val aktivniTypHry = onlineStul?.typHry ?: offlineKonfig?.typHry ?: TypHry.PRSI
    val pocetHracu = onlineStul?.maxHracu ?: offlineKonfig?.pocetHracu ?: 3
    val pocatecniPravidla = onlineStul?.variantaPravidel
        ?: offlineKonfig?.variantaPravidel
        ?: nactiVychoziVariantuPravidel()

    val vychoziJmenaOffline = remember(aktivniTypHry, pocetHracu, mojePrezdivka) {
        val botJmena = listOf("Karel (Bot)", "Pepa (Bot)", "Franta (Bot)")
        listOf(mojePrezdivka) + botJmena.take((pocetHracu - 1).coerceAtLeast(1))
    }

    var cisloOfflineRozdani by remember { mutableStateOf(1) }
    var soucetBoduOffline by remember { mutableStateOf(List(pocetHracu) { 0 }) }

    var stavPrsi by remember {
        mutableStateOf(
            if (aktivniTypHry == TypHry.PRSI) {
                onlineStul?.serializovanyStavHry?.let { StavPrsi.zKompaktnihoRetezce(it) }
                    ?: PrsiEngine.novaHra(
                        jmenaHracu = vychoziJmenaOffline,
                        variantaPravidel = pocatecniPravidla
                    )
            } else null
        )
    }
    var stavSedma by remember {
        mutableStateOf(
            if (aktivniTypHry == TypHry.SEDMA) {
                onlineStul?.serializovanyStavHry?.let { StavSedma.zKompaktnihoRetezce(it) }
                    ?: SedmaEngine.novaHra(
                        jmenaHracu = vychoziJmenaOffline,
                        variantaPravidel = pocatecniPravidla
                    )
            } else null
        )
    }
    var stavMarias by remember {
        mutableStateOf(
            if (aktivniTypHry == TypHry.MARIAS) {
                onlineStul?.serializovanyStavHry?.let { StavMarias.zKompaktnihoRetezce(it) }
                    ?: MariasEngine.novaHra(
                        jmenaHracu = vychoziJmenaOffline,
                        indexForhonta = 0,
                        variantaPravidel = pocatecniPravidla
                    )
            } else null
        )
    }

    val aktivniPravidla: VariantaPravidel = when (aktivniTypHry) {
        TypHry.PRSI -> stavPrsi?.variantaPravidel
        TypHry.SEDMA -> stavSedma?.variantaPravidel
        TypHry.MARIAS -> stavMarias?.variantaPravidel
    } ?: onlineStul?.variantaPravidel ?: pocatecniPravidla

    var vybraneKartyDoTalonu by remember { mutableStateOf(emptyList<Karta>()) }
    var planovanyZavazekMariase by remember { mutableStateOf(ZavazekMariase.HRA) }
    var zapoctenaVerzeVysledku by remember { mutableStateOf(-1) }

    fun aplikujSerializovanyStavStolu(stul: OnlineStul) {
        if (stul.serializovanyStavHry.isBlank()) return
        when (stul.typHry) {
            TypHry.PRSI -> StavPrsi.zKompaktnihoRetezce(stul.serializovanyStavHry)?.let { stavPrsi = it }
            TypHry.SEDMA -> StavSedma.zKompaktnihoRetezce(stul.serializovanyStavHry)?.let { stavSedma = it }
            TypHry.MARIAS -> StavMarias.zKompaktnihoRetezce(stul.serializovanyStavHry)?.let {
                stavMarias = it
                if (it.faze != FazeMariase.ODKLADANI_TALONU) {
                    vybraneKartyDoTalonu = emptyList()
                } else {
                    planovanyZavazekMariase = it.zavazek
                }
            }
        }
    }

    val lokalniIndex = remember(onlineStul, mojeDeviceId) {
        if (onlineStul != null) {
            onlineStul!!.indexLokalnihoHrace(mojeDeviceId).coerceAtLeast(0)
        } else {
            0
        }
    }

    DisposableEffect(Unit) {
        HospodaZvuk.pleskniKartou()
        onDispose {
            zastavMluveniPlatforma()
        }
    }

    var sledovanyPrsi by remember { mutableStateOf(stavPrsi) }
    LaunchedEffect(stavPrsi) {
        val novy = stavPrsi ?: return@LaunchedEffect
        val stary = sledovanyPrsi
        if (stary != null && novy !== stary) {
            if (novy.cisloTahu <= 1 && stary.cisloTahu > 1) {
                HospodaZvuk.pleskniKartou()
            } else {
                HospodaZvuk.zpracujZmenuPrsi(stary, novy)
            }
        }
        sledovanyPrsi = novy
    }

    var sledovanaSedma by remember { mutableStateOf(stavSedma) }
    LaunchedEffect(stavSedma) {
        val novy = stavSedma ?: return@LaunchedEffect
        val stary = sledovanaSedma
        if (stary != null && novy !== stary) {
            if (novy.cisloTahu <= 1 && stary.cisloTahu > 1) {
                HospodaZvuk.pleskniKartou()
            } else {
                HospodaZvuk.zpracujZmenuSedmy(stary, novy)
            }
        }
        sledovanaSedma = novy
    }

    var sledovanyMarias by remember { mutableStateOf(stavMarias) }
    LaunchedEffect(stavMarias) {
        val novy = stavMarias ?: return@LaunchedEffect
        val stary = sledovanyMarias
        if (stary != null && novy !== stary) {
            if (novy.cisloTahu <= 1 && stary.cisloTahu > 1) {
                HospodaZvuk.pleskniKartou()
            } else {
                HospodaZvuk.zpracujZmenuMariase(stary, novy)
            }
        }
        sledovanyMarias = novy
    }

    val aktualniRozdaniCislo = onlineStul?.cisloRozdani ?: cisloOfflineRozdani
    val klicUzavrenehoStychuSedma = aktualniRozdaniCislo * 100 +
        ((stavSedma?.pocetStychuTym0 ?: 0) + (stavSedma?.pocetStychuTym1 ?: 0))
    var skrytyStychKlicSedma by remember { mutableStateOf(klicUzavrenehoStychuSedma) }
    LaunchedEffect(klicUzavrenehoStychuSedma) {
        val s = stavSedma
        if (s != null && (s.pocetStychuTym0 + s.pocetStychuTym1) > 0 &&
            s.posledniUzavrenyStych.isNotEmpty() && !s.jeKonec
        ) {
            delay(1450L)
            skrytyStychKlicSedma = klicUzavrenehoStychuSedma
        } else {
            skrytyStychKlicSedma = klicUzavrenehoStychuSedma
        }
    }
    val zobrazitUzavrenyStychSedma = stavSedma != null &&
        stavSedma!!.aktualniStych.isEmpty() &&
        stavSedma!!.posledniUzavrenyStych.isNotEmpty() &&
        (stavSedma!!.jeKonec || klicUzavrenehoStychuSedma != skrytyStychKlicSedma)

    val klicUzavrenehoStychuMarias = aktualniRozdaniCislo * 100 +
        ((stavMarias?.pocetStychuForhont ?: 0) + (stavMarias?.pocetStychuObrana ?: 0))
    var skrytyStychKlicMarias by remember { mutableStateOf(klicUzavrenehoStychuMarias) }
    LaunchedEffect(klicUzavrenehoStychuMarias) {
        val s = stavMarias
        if (s != null && (s.pocetStychuForhont + s.pocetStychuObrana) > 0 &&
            s.posledniUzavrenyStych.isNotEmpty() && s.faze != FazeMariase.KONEC
        ) {
            delay(1450L)
            skrytyStychKlicMarias = klicUzavrenehoStychuMarias
        } else {
            skrytyStychKlicMarias = klicUzavrenehoStychuMarias
        }
    }
    val zobrazitUzavrenyStychMarias = stavMarias != null &&
        stavMarias!!.aktualniStych.isEmpty() &&
        stavMarias!!.posledniUzavrenyStych.isNotEmpty() &&
        (stavMarias!!.faze == FazeMariase.KONEC || klicUzavrenehoStychuMarias != skrytyStychKlicMarias)

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tikSekundyMs = ziskejAktualniCasMs()
        }
    }

    LaunchedEffect(jeOnlineRezim, onlineStul?.roomId) {
        val rid = onlineStul?.roomId ?: return@LaunchedEffect
        while (jeOnlineRezim) {
            OnlineManager.odesliTepSpojeni(rid)
            delay(4000L)
        }
    }

    LaunchedEffect(jeOnlineRezim, onlineStul?.roomId) {
        val rid = onlineStul?.roomId ?: return@LaunchedEffect
        while (jeOnlineRezim) {
            val cerstvy = OnlineManager.nactiKonkretniStul(rid)
            if (cerstvy == null) {
                onOpustitStul()
                break
            }
            if (cerstvy.indexLokalnihoHrace(mojeDeviceId) < 0) {
                onOpustitStul()
                break
            }
            if (cerstvy.stavMistnosti == StavMistnosti.CEKANI && cerstvy.hraci.size < cerstvy.maxHracu) {
                onVratitDoCekarny(cerstvy)
                break
            }
            val zmenaStavu = cerstvy.verzeStavu != onlineStul?.verzeStavu ||
                cerstvy.serializovanyStavHry != onlineStul?.serializovanyStavHry ||
                cerstvy.cisloRozdani != onlineStul?.cisloRozdani ||
                cerstvy.hraci != onlineStul?.hraci
            onlineStul = cerstvy

            if (zmenaStavu && cerstvy.serializovanyStavHry.isNotBlank()) {
                aplikujSerializovanyStavStolu(cerstvy)
            }

            if (cerstvy.stavMistnosti == StavMistnosti.HRAJE_SE) {
                val nyni = ziskejAktualniCasMs()
                val jeHraHotova = when (cerstvy.typHry) {
                    TypHry.PRSI -> stavPrsi?.jeKonec == true
                    TypHry.SEDMA -> stavSedma?.jeKonec == true
                    TypHry.MARIAS -> stavMarias?.faze == FazeMariase.KONEC
                }
                if (!jeHraHotova) {
                    val maJinehoZivehoHrace = cerstvy.hraci.any {
                        it.deviceId != mojeDeviceId && !jeBotDeviceId(it.deviceId)
                    }
                    val odpojenyHrac = cerstvy.hraci.firstOrNull { hrac ->
                        !jeBotDeviceId(hrac.deviceId) &&
                            cerstvy.infoPripojeni(hrac.deviceId, nyni).stav == StavPripojeniHrace.ODPOJEN_CERVENA
                    }
                    val efektivniStartTahu = if (cerstvy.verzeStavu <= 1) {
                        max(cerstvy.zacatekTahuMs, casVstupuKeStoluMs)
                    } else {
                        cerstvy.zacatekTahuMs
                    }
                    val kdoIndex = when (cerstvy.typHry) {
                        TypHry.PRSI -> stavPrsi?.naTahu ?: 0
                        TypHry.SEDMA -> stavSedma?.naTahu ?: 0
                        TypHry.MARIAS -> stavMarias?.naTahu ?: 0
                    }
                    val hracNaTahuObj = cerstvy.hraci.getOrNull(kdoIndex)
                    val prekrocenTah = maJinehoZivehoHrace &&
                        hracNaTahuObj != null &&
                        !jeBotDeviceId(hracNaTahuObj.deviceId) &&
                        nyni - efektivniStartTahu >= LIMIT_ODPOJENI_MS

                    if (odpojenyHrac != null || prekrocenTah) {
                        val vinik = odpojenyHrac ?: hracNaTahuObj
                        if (cerstvy.turnajId.isNotEmpty() && vinik != null && !jeBotDeviceId(vinik.deviceId)) {
                            if (vinik.deviceId != mojeDeviceId) {
                                val seZaskokem = OnlineManager.nahradNepritomnehoHraceBotemUStolu(cerstvy, vinik.deviceId)
                                if (seZaskokem != null) {
                                    onlineStul = seZaskokem
                                    aplikujSerializovanyStavStolu(seZaskokem)
                                }
                            }
                        } else if (cerstvy.turnajId.isEmpty()) {
                            val jmenoVinika = vinik?.prezdivka ?: "Hráč"
                            val ukonceny = OnlineManager.ukonciStulProVypadek(cerstvy, jmenoVinika)
                            onlineStul = ukonceny
                        }
                    }
                }
            }
            delay(1500L)
        }
    }

    val aktualniCisloTahu = when (aktivniTypHry) {
        TypHry.PRSI -> stavPrsi?.cisloTahu ?: 0
        TypHry.SEDMA -> stavSedma?.cisloTahu ?: 0
        TypHry.MARIAS -> stavMarias?.cisloTahu ?: 0
    }

    LaunchedEffect(
        jeOnlineRezim,
        onlineStul?.roomId,
        onlineStul?.verzeStavu,
        aktualniRozdaniCislo,
        aktualniCisloTahu,
        stavPrsi?.cekaNaVolbuBarvy
    ) {
        val indexNaTahu = when (aktivniTypHry) {
            TypHry.PRSI -> stavPrsi?.naTahu ?: 0
            TypHry.SEDMA -> stavSedma?.naTahu ?: 0
            TypHry.MARIAS -> stavMarias?.naTahu ?: 0
        }

        if (jeOnlineRezim) {
            val stul = onlineStul ?: return@LaunchedEffect
            if (stul.stavMistnosti != StavMistnosti.HRAJE_SE) return@LaunchedEffect
            val hracNaTahu = stul.hraci.getOrNull(indexNaTahu) ?: return@LaunchedEffect
            if (!jeBotDeviceId(hracNaTahu.deviceId)) return@LaunchedEffect

            val nyni = ziskejAktualniCasMs()
            val hostZivy = stul.hraci.firstOrNull {
                it.deviceId == stul.hostDeviceId &&
                    !jeBotDeviceId(it.deviceId) &&
                    stul.infoPripojeni(it.deviceId, nyni).stav != StavPripojeniHrace.ODPOJEN_CERVENA
            }?.deviceId
            val prvniZivy = hostZivy ?: stul.hraci.firstOrNull {
                !jeBotDeviceId(it.deviceId) &&
                    stul.infoPripojeni(it.deviceId, nyni).stav != StavPripojeniHrace.ODPOJEN_CERVENA
            }?.deviceId
            if (prvniZivy != mojeDeviceId) return@LaunchedEffect
        } else if (indexNaTahu == 0) {
            return@LaunchedEffect
        }

        suspend fun pockejNaStulAHlasku(praveUzavrenStych: Boolean) {
            val startMs = ziskejAktualniCasMs()
            val zakladniPauzaMs = when {
                praveUzavrenStych -> 1580L
                zvukZapnut -> 920L
                else -> 760L
            }
            delay(zakladniPauzaMs)
            if (zvukZapnut) {
                var mluviloSe = false
                while (probihaMluveniPlatforma() && (ziskejAktualniCasMs() - startMs) < 2750L) {
                    mluviloSe = true
                    delay(80L)
                }
                if (mluviloSe) {
                    delay(120L)
                }
            }
        }

        when (aktivniTypHry) {
            TypHry.PRSI -> {
                val s = stavPrsi ?: return@LaunchedEffect
                if (!s.jeKonec && s.naTahu == indexNaTahu) {
                    pockejNaStulAHlasku(praveUzavrenStych = false)
                    val novy = PrsiEngine.provedTahBota(s)
                    stavPrsi = novy
                    if (jeOnlineRezim) {
                        onlineStul?.let { st ->
                            OnlineManager.odesliTahHry(st, novy.doKompaktnihoRetezce())?.let { onlineStul = it }
                        }
                    }
                }
            }
            TypHry.SEDMA -> {
                val s = stavSedma ?: return@LaunchedEffect
                if (!s.jeKonec && s.naTahu == indexNaTahu) {
                    val uzavrenStych = s.aktualniStych.isEmpty() && s.posledniUzavrenyStych.isNotEmpty()
                    pockejNaStulAHlasku(praveUzavrenStych = uzavrenStych)
                    val novy = SedmaEngine.provedTahBota(s)
                    stavSedma = novy
                    if (jeOnlineRezim) {
                        onlineStul?.let { st ->
                            OnlineManager.odesliTahHry(st, novy.doKompaktnihoRetezce())?.let { onlineStul = it }
                        }
                    }
                }
            }
            TypHry.MARIAS -> {
                val s = stavMarias ?: return@LaunchedEffect
                if (s.faze != FazeMariase.KONEC && s.naTahu == indexNaTahu) {
                    val uzavrenStych = s.faze == FazeMariase.SEHRAVKA &&
                        s.aktualniStych.isEmpty() &&
                        s.posledniUzavrenyStych.isNotEmpty()
                    pockejNaStulAHlasku(praveUzavrenStych = uzavrenStych)
                    val novy = MariasEngine.provedTahBota(s)
                    stavMarias = novy
                    if (jeOnlineRezim) {
                        onlineStul?.let { st ->
                            OnlineManager.odesliTahHry(st, novy.doKompaktnihoRetezce())?.let { onlineStul = it }
                        }
                    }
                }
            }
        }
    }

    fun publikujZmenuOnline(novyRetezec: String) {
        val stul = onlineStul ?: return
        scope.launch {
            val aktualizovany = OnlineManager.odesliTahHry(stul, novyRetezec)
            if (aktualizovany != null) {
                onlineStul = aktualizovany
            }
        }
    }

    val vysledekPartie: UiVyuctovani? = when (aktivniTypHry) {
        TypHry.PRSI -> stavPrsi?.takeIf { it.jeKonec }?.let { PrsiEngine.vyhodnotVysledek(it).naUiVyuctovani() }
        TypHry.SEDMA -> stavSedma?.takeIf { it.jeKonec }?.let { SedmaEngine.vyhodnotVysledek(it).naUiVyuctovani() }
        TypHry.MARIAS -> stavMarias?.takeIf { it.faze == FazeMariase.KONEC }?.let { MariasEngine.vyhodnotMariase(it).naUiVyuctovani() }
    }

    val klicDokonceneHry = if (vysledekPartie != null) {
        aktualniRozdaniCislo * 10000 + aktualniCisloTahu
    } else {
        -1
    }

    var zobrazenyKlicVyuctovani by remember { mutableStateOf(-1) }
    LaunchedEffect(klicDokonceneHry) {
        if (klicDokonceneHry > 0) {
            delay(1300L)
            zobrazenyKlicVyuctovani = klicDokonceneHry
        } else {
            zobrazenyKlicVyuctovani = -1
        }
    }

    LaunchedEffect(klicDokonceneHry) {
        if (klicDokonceneHry > 0 && zapoctenaVerzeVysledku != klicDokonceneHry && vysledekPartie != null) {
            zapoctenaVerzeVysledku = klicDokonceneHry
            val mojeZmena = vysledekPartie.hraci.firstOrNull { it.indexHrace == lokalniIndex }
            if (mojeZmena != null) {
                val novyZustatek = (nactiLokalniTereziany() + mojeZmena.zmenaTerezianu).coerceIn(0, 999_999)
                aktualniMesecTerezianu = novyZustatek
                OnlineManager.odesliAktualniTerezianyDoZebricku(novyZustatek)
            }
            if (!jeOnlineRezim) {
                val offlineNaVitezneBitvy = offlineKonfig?.jeOfflinePohar == true && aktivniTypHry != TypHry.MARIAS
                soucetBoduOffline = soucetBoduOffline.mapIndexed { idx, stary ->
                    val hracVys = vysledekPartie.hraci.firstOrNull { it.indexHrace == idx }
                    val prirustek = if (offlineNaVitezneBitvy) {
                        if (hracVys?.jeVitez == true) 1 else 0
                    } else {
                        hracVys?.bodyZapasu ?: 0
                    }
                    stary + prirustek
                }
            } else {
                val stul = onlineStul
                if (stul != null && stul.turnajId.isNotEmpty()) {
                    val noveSkore = vypoctiNoveSkoreSerieOnline(stul, vysledekPartie)
                    val serieHotova = jeOnlineTurnajovaSerieHotova(stul, vysledekPartie)
                    val skoreDoPavouka = stul.hraci.mapIndexed { idx, h ->
                        h.deviceId to formatujSkoreDoPavouka(stul.typHry, noveSkore.getOrElse(idx) { 0 })
                    }
                    val prvniZivy = stul.hraci.firstOrNull { !jeBotDeviceId(it.deviceId) }?.deviceId

                    if (serieHotova) {
                        val serazeneIndexy = stul.hraci.indices.sortedWith(
                            compareByDescending<Int> { noveSkore.getOrElse(it) { 0 } }
                                .thenByDescending { idx ->
                                    vysledekPartie.hraci.firstOrNull { it.indexHrace == idx }?.jeVitez == true
                                }
                                .thenByDescending { idx ->
                                    !jeBotDeviceId(stul.hraci[idx].deviceId)
                                }
                        )
                        val vitezHrac = serazeneIndexy.getOrNull(0)?.let { stul.hraci.getOrNull(it) }
                        val druhyHrac = serazeneIndexy.getOrNull(1)?.let { stul.hraci.getOrNull(it) }
                        val mamZapsatVysledek = stul.hostDeviceId == mojeDeviceId ||
                            prvniZivy == mojeDeviceId ||
                            vitezHrac?.deviceId == mojeDeviceId

                        if (vitezHrac != null && mamZapsatVysledek) {
                            TournamentManager.zapisVysledekZapasu(
                                turnajId = stul.turnajId,
                                slotZapasu = stul.slotZapasu,
                                vitezDeviceId = vitezHrac.deviceId,
                                vitezJmeno = vitezHrac.prezdivka,
                                druhyDeviceId = druhyHrac?.deviceId ?: "",
                                druhyJmeno = druhyHrac?.prezdivka ?: "",
                                skoreHracu = skoreDoPavouka
                            )
                        }
                    } else if (stul.hostDeviceId == mojeDeviceId || prvniZivy == mojeDeviceId) {
                        TournamentManager.zapisPrubezneSkoreZapasu(
                            turnajId = stul.turnajId,
                            slotZapasu = stul.slotZapasu,
                            skoreHracu = skoreDoPavouka
                        )
                    }
                }
            }
        }
    }

    ZelenyKaretniStulBox(zobrazitStredovyKruh = true) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val celkovaSirka = maxWidth
            val celkovaVyska = maxHeight
            val naSirku = celkovaSirka > celkovaVyska
            val naVyskuDostMista = !naSirku && celkovaVyska >= 680.dp

            val sirkaKartyRuky: Dp = if (naSirku) {
                (celkovaVyska.value * 0.17f).coerceIn(50f, 64f).dp
            } else {
                (celkovaSirka.value * 0.205f).coerceIn(66f, 79f).dp
            }
            val sirkaBalickuPrsi: Dp = if (naSirku) {
                (celkovaVyska.value * 0.22f).coerceIn(62f, 84f).dp
            } else {
                min(celkovaSirka.value * 0.29f, celkovaVyska.value * 0.15f).coerceIn(86f, 110f).dp
            }
            val sirkaKartyStolu: Dp = if (naSirku) {
                (celkovaVyska.value * 0.195f).coerceIn(56f, 74f).dp
            } else {
                min(celkovaSirka.value * 0.23f, celkovaVyska.value * 0.128f).coerceIn(76f, 94f).dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 8.dp,
                        vertical = if (naSirku) 4.dp else 6.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorniListaStolu(
                        typHry = aktivniTypHry,
                        variantaPravidel = aktivniPravidla,
                        jeOnline = jeOnlineRezim,
                        onlineStul = onlineStul,
                        offlineKonfig = offlineKonfig,
                        cisloOfflineRozdani = cisloOfflineRozdani,
                        soucetBoduOffline = soucetBoduOffline,
                        stavPrsi = stavPrsi,
                        stavSedma = stavSedma,
                        stavMarias = stavMarias,
                        tikSekundyMs = tikSekundyMs,
                        mojeDeviceId = mojeDeviceId,
                        casVstupuKeStoluMs = casVstupuKeStoluMs,
                        zvukZapnut = zvukZapnut,
                        onPrepnoutZvuk = {
                            val novyStav = !zvukZapnut
                            zvukZapnut = novyStav
                            HospodaZvuk.priPrepnutiZvuku(novyStav)
                        },
                        onKlikZpet = {
                            if (jeOnlineRezim && onlineStul?.stavMistnosti == StavMistnosti.HRAJE_SE && vysledekPartie == null) {
                                potvrditOdchod = true
                            } else {
                                val stul = onlineStul
                                if (stul != null) {
                                    val serieHotova = vysledekPartie != null &&
                                        jeOnlineTurnajovaSerieHotova(stul, vysledekPartie)
                                    scope.launch {
                                        if (stul.turnajId.isNotEmpty() && serieHotova) {
                                            OnlineManager.smazStul(stul.roomId)
                                        } else {
                                            OnlineManager.opustStul(stul.roomId)
                                        }
                                        onOpustitStul()
                                    }
                                } else {
                                    onOpustitStul()
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(if (naSirku) 3.dp else 5.dp))

                    PruhySouperu(
                        typHry = aktivniTypHry,
                        lokalniIndex = lokalniIndex,
                        onlineStul = onlineStul,
                        stavPrsi = stavPrsi,
                        stavSedma = stavSedma,
                        stavMarias = stavMarias,
                        nyniMs = tikSekundyMs
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (aktivniTypHry) {
                        TypHry.PRSI -> stavPrsi?.let { s ->
                            StredStoluPrsi(
                                stav = s,
                                lokalniIndex = lokalniIndex,
                                sirkaKarty = sirkaBalickuPrsi,
                                naSirku = naSirku,
                                onLiznoutNeboStat = {
                                    val novy = PrsiEngine.lizniNeboStuj(s, lokalniIndex)
                                    stavPrsi = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                },
                                onZvolitBarvuSvrskem = { barva ->
                                    val novy = PrsiEngine.potvrdVolbuBarvySvrskem(s, barva)
                                    stavPrsi = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                }
                            )
                        }

                        TypHry.SEDMA -> stavSedma?.let { s ->
                            StredStoluSedma(
                                stav = s,
                                lokalniIndex = lokalniIndex,
                                sirkaKarty = sirkaKartyStolu,
                                dostupnaSirkaDp = (celkovaSirka.value - 16f).coerceAtLeast(240f),
                                naSirku = naSirku,
                                talonNadStychem = naVyskuDostMista,
                                zobrazitUzavrenyStych = zobrazitUzavrenyStychSedma,
                                onPustitNeboVzitStych = {
                                    val novy = SedmaEngine.pustStych(s, lokalniIndex)
                                    stavSedma = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                },
                                onNahlasitSpalenouZRuky = {
                                    val novy = SedmaEngine.nahlasSpalenouZRuky(s, lokalniIndex)
                                    stavSedma = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                }
                            )
                        }

                        TypHry.MARIAS -> stavMarias?.let { s ->
                            StredStoluMarias(
                                stav = s,
                                lokalniIndex = lokalniIndex,
                                sirkaKarty = sirkaKartyStolu,
                                talonNadStychem = naVyskuDostMista,
                                zobrazitUzavrenyStych = zobrazitUzavrenyStychMarias,
                                vybraneDoTalonu = vybraneKartyDoTalonu,
                                planovanyZavazek = planovanyZavazekMariase,
                                onZmenaPlanovanehoZavazku = { novyZav ->
                                    planovanyZavazekMariase = novyZav
                                    val povolene = MariasEngine.zjistiKartyPovoleneDoTalonu(s, novyZav)
                                    vybraneKartyDoTalonu = vybraneKartyDoTalonu.filter { it in povolene }
                                },
                                onVolbaTrumfu = { trumf, zTalonu, zavazek ->
                                    planovanyZavazekMariase = zavazek
                                    vybraneKartyDoTalonu = emptyList()
                                    val novy = MariasEngine.zvolTrumfNeboHruBezTrumfu(s, trumf, zTalonu, zavazek)
                                    stavMarias = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                },
                                onPotvrditTalon = {
                                    val novy = MariasEngine.potvrdTalonAZavazek(
                                        s,
                                        vybraneKartyDoTalonu,
                                        planovanyZavazekMariase
                                    )
                                    vybraneKartyDoTalonu = emptyList()
                                    stavMarias = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                },
                                onOdpovedFleku = { flekHra, flekSedma ->
                                    val novy = MariasEngine.odpovezNaFlekovani(s, lokalniIndex, flekHra, flekSedma)
                                    stavMarias = novy
                                    if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                                }
                            )
                        }
                    }
                }

                SpodniRukaHrace(
                    typHry = aktivniTypHry,
                    lokalniIndex = lokalniIndex,
                    sirkaKarty = sirkaKartyRuky,
                    naSirku = naSirku,
                    stavPrsi = stavPrsi,
                    stavSedma = stavSedma,
                    stavMarias = stavMarias,
                    onlineStul = onlineStul,
                    nyniMs = tikSekundyMs,
                    vybraneDoTalonu = vybraneKartyDoTalonu,
                    planovanyZavazek = planovanyZavazekMariase,
                    onKlikKartaPrsi = { karta ->
                        val s = stavPrsi ?: return@SpodniRukaHrace
                        val novy = PrsiEngine.zahrajKartu(s, lokalniIndex, karta)
                        stavPrsi = novy
                        if (jeOnlineRezim && !novy.cekaNaVolbuBarvy) {
                            publikujZmenuOnline(novy.doKompaktnihoRetezce())
                        }
                    },
                    onKlikKartaSedma = { karta ->
                        val s = stavSedma ?: return@SpodniRukaHrace
                        val novy = SedmaEngine.zahrajKartu(s, lokalniIndex, karta)
                        stavSedma = novy
                        if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                    },
                    onKlikKartaMarias = { karta ->
                        val s = stavMarias ?: return@SpodniRukaHrace
                        if (s.faze == FazeMariase.ODKLADANI_TALONU && s.naTahu == lokalniIndex) {
                            vybraneKartyDoTalonu = if (karta in vybraneKartyDoTalonu) {
                                vybraneKartyDoTalonu - karta
                            } else if (vybraneKartyDoTalonu.size < 2) {
                                vybraneKartyDoTalonu + karta
                            } else {
                                listOf(vybraneKartyDoTalonu[1], karta)
                            }
                        } else if (s.faze == FazeMariase.SEHRAVKA && s.naTahu == lokalniIndex) {
                            val novy = MariasEngine.zahrajKartu(s, lokalniIndex, karta)
                            stavMarias = novy
                            if (jeOnlineRezim) publikujZmenuOnline(novy.doKompaktnihoRetezce())
                        }
                    }
                )
            }

            if (vysledekPartie != null &&
                zobrazenyKlicVyuctovani == klicDokonceneHry &&
                onlineStul?.stavMistnosti != StavMistnosti.UKONCENO_VYPADKEM
            ) {
                PanelVyuctovaniPartie(
                    vysledek = vysledekPartie,
                    jeOnline = jeOnlineRezim,
                    onlineStul = onlineStul,
                    offlineKonfig = offlineKonfig,
                    cisloOfflineRozdani = cisloOfflineRozdani,
                    soucetBoduOffline = soucetBoduOffline,
                    aktualniMesecTerezianu = aktualniMesecTerezianu,
                    mojeDeviceId = mojeDeviceId,
                    onDalsiRozdani = {
                        zobrazenyKlicVyuctovani = -1
                        zapoctenaVerzeVysledku = -1
                        vybraneKartyDoTalonu = emptyList()
                        planovanyZavazekMariase = ZavazekMariase.HRA
                        if (!jeOnlineRezim) {
                            val dalsiKolo = cisloOfflineRozdani + 1
                            cisloOfflineRozdani = dalsiKolo
                            val zacinajici = (dalsiKolo - 1) % pocetHracu
                            when (aktivniTypHry) {
                                TypHry.PRSI -> stavPrsi = PrsiEngine.novaHra(
                                    jmenaHracu = vychoziJmenaOffline,
                                    zacinajiciHrac = zacinajici,
                                    variantaPravidel = aktivniPravidla
                                )
                                TypHry.SEDMA -> stavSedma = SedmaEngine.novaHra(
                                    jmenaHracu = vychoziJmenaOffline,
                                    zacinajiciHrac = zacinajici,
                                    variantaPravidel = aktivniPravidla
                                )
                                TypHry.MARIAS -> stavMarias = MariasEngine.novaHra(
                                    jmenaHracu = vychoziJmenaOffline,
                                    indexForhonta = zacinajici,
                                    variantaPravidel = aktivniPravidla
                                )
                            }
                        } else {
                            val stul = onlineStul ?: return@PanelVyuctovaniPartie
                            val noveSoucty = vypoctiNoveSkoreSerieOnline(stul, vysledekPartie)
                            val dalsiRozdani = stul.cisloRozdani + 1
                            val zacinajici = (dalsiRozdani - 1) % stul.maxHracu
                            scope.launch {
                                val novyStul = OnlineManager.rozdejASpustStul(
                                    stul = stul,
                                    zacinajiciIndex = zacinajici,
                                    noveCisloRozdani = dalsiRozdani,
                                    noveSouctyBodu = noveSoucty
                                )
                                if (novyStul != null) {
                                    onlineStul = novyStul
                                    aplikujSerializovanyStavStolu(novyStul)
                                }
                            }
                        }
                    },
                    onZpetDoHerny = {
                        val stul = onlineStul
                        if (stul != null) {
                            val serieHotova = jeOnlineTurnajovaSerieHotova(stul, vysledekPartie)
                            scope.launch {
                                if (stul.turnajId.isNotEmpty() && serieHotova) {
                                    OnlineManager.smazStul(stul.roomId)
                                } else {
                                    OnlineManager.opustStul(stul.roomId)
                                }
                                onOpustitStul()
                            }
                        } else {
                            onOpustitStul()
                        }
                    }
                )
            }

            if (onlineStul?.stavMistnosti == StavMistnosti.UKONCENO_VYPADKEM) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xD905140C))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF123C24))
                            .border(2.dp, Color(0xFFFFB74D), RoundedCornerShape(18.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Živá hra byla ukončena",
                            color = Color(0xFFFFD54F),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Hráč ${onlineStul?.ukoncenoKym ?: ""} ztratil spojení nebo neodehrál tah do 60 sekund. Do živé hry boti nezasahují — stůl byl čistě uzavřen bez odečtení tereziánů přítomným hráčům.",
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                jemnaHaptika()
                                onOpustitStul()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text("Zpět do herny", color = Color(0xFF0A2916), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (potvrditOdchod) {
                val jeTurnajovyStul = onlineStul?.turnajId?.isNotEmpty() == true
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC000000))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF143D25))
                            .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (jeTurnajovyStul) "Opustit turnajovou sérii?" else "Opustit rozehraný stůl?",
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (jeTurnajovyStul) {
                                "Pokud odejdeš z rozehraného turnajového zápasu, u stolu s dalšími živými hráči tě nahradí bot, případně v duelu s botem postoupí soupeř."
                            } else {
                                "Opuštěním živého stolu se partie ostatním spoluhráčům ukončí."
                            },
                            color = Color(0xFFE0E0E0),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    potvrditOdchod = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E6B47))
                            ) {
                                Text("Zůstat u stolu", color = Color.White, maxLines = 1, softWrap = false, fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    potvrditOdchod = false
                                    val stul = onlineStul
                                    if (stul != null) {
                                        scope.launch {
                                            if (stul.turnajId.isNotEmpty()) {
                                                val jiniZivi = stul.hraci.any {
                                                    it.deviceId != mojeDeviceId && !jeBotDeviceId(it.deviceId)
                                                }
                                                if (jiniZivi) {
                                                    OnlineManager.nahradNepritomnehoHraceBotemUStolu(stul, mojeDeviceId)
                                                } else {
                                                    val souperBot = stul.hraci.firstOrNull { it.deviceId != mojeDeviceId }
                                                    if (souperBot != null) {
                                                        val skoreVzdaneho = stul.hraci.mapIndexed { idx, h ->
                                                            val txt = if (h.deviceId == souperBot.deviceId) {
                                                                if (stul.typHry == TypHry.MARIAS) "+60b"
                                                                else CIL_VITEZNYCH_BITEV_DUELU.toString()
                                                            } else {
                                                                formatujSkoreDoPavouka(stul.typHry, stul.soucetBoduHracu.getOrElse(idx) { 0 })
                                                            }
                                                            h.deviceId to txt
                                                        }
                                                        TournamentManager.zapisVysledekZapasu(
                                                            turnajId = stul.turnajId,
                                                            slotZapasu = stul.slotZapasu,
                                                            vitezDeviceId = souperBot.deviceId,
                                                            vitezJmeno = souperBot.prezdivka,
                                                            druhyDeviceId = mojeDeviceId,
                                                            druhyJmeno = mojePrezdivka,
                                                            skoreHracu = skoreVzdaneho
                                                        )
                                                    }
                                                    OnlineManager.smazStul(stul.roomId)
                                                }
                                            } else {
                                                OnlineManager.ukonciStulProVypadek(stul, mojePrezdivka)
                                            }
                                            onOpustitStul()
                                        }
                                    } else {
                                        onOpustitStul()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                            ) {
                                Text("Ukončit a odejít", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorniListaStolu(
    typHry: TypHry,
    variantaPravidel: VariantaPravidel,
    jeOnline: Boolean,
    onlineStul: OnlineStul?,
    offlineKonfig: KonfiguraceOfflineHry?,
    cisloOfflineRozdani: Int,
    soucetBoduOffline: List<Int>,
    stavPrsi: StavPrsi?,
    stavSedma: StavSedma?,
    stavMarias: StavMarias?,
    tikSekundyMs: Long,
    mojeDeviceId: String,
    casVstupuKeStoluMs: Long,
    zvukZapnut: Boolean,
    onPrepnoutZvuk: () -> Unit,
    onKlikZpet: () -> Unit
) {
    val maJinehoZivehoSouper = onlineStul?.hraci?.any {
        it.deviceId != mojeDeviceId && !jeBotDeviceId(it.deviceId)
    } == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(Color(0xB8062011))
            .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(11.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D5836))
                    .clickable {
                        jemnaHaptika()
                        onKlikZpet()
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "← Odchod",
                    color = Color(0xFFFFE082),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val stitekRezimu = when {
                onlineStul != null && onlineStul.turnajId.isNotEmpty() -> {
                    if (onlineStul.typHry != TypHry.MARIAS) {
                        val s0 = onlineStul.soucetBoduHracu.getOrElse(0) { 0 }
                        val s1 = onlineStul.soucetBoduHracu.getOrElse(1) { 0 }
                        "Bitva ${onlineStul.cisloRozdani} ($s0:$s1)"
                    } else {
                        "TURNAJ ${onlineStul.cisloRozdani}/${onlineStul.celkemRozdani}"
                    }
                }
                jeOnline -> variantaPravidel.stitekUStolu
                offlineKonfig?.jeOfflinePohar == true -> {
                    if (offlineKonfig.typHry != TypHry.MARIAS) {
                        val v0 = soucetBoduOffline.getOrElse(0) { 0 }
                        val v1 = soucetBoduOffline.getOrElse(1) { 0 }
                        "POHÁR • Bitva $cisloOfflineRozdani ($v0:$v1)"
                    } else {
                        "POHÁR $cisloOfflineRozdani/${offlineKonfig.celkemRozdani}"
                    }
                }
                else -> variantaPravidel.stitekUStolu
            }

            Text(
                text = typHry.nazev,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )

            if (jeOnline && onlineStul != null && onlineStul.stavMistnosti == StavMistnosti.HRAJE_SE && maJinehoZivehoSouper) {
                val zakladTahu = if (onlineStul.verzeStavu <= 1) {
                    max(onlineStul.zacatekTahuMs, casVstupuKeStoluMs)
                } else {
                    onlineStul.zacatekTahuMs
                }
                val zbyvaTahu = max(0L, (LIMIT_ODPOJENI_MS - (tikSekundyMs - zakladTahu)) / 1000L)
                Text(
                    text = "$stitekRezimu • ${zbyvaTahu}s",
                    color = if (zbyvaTahu <= 15L) Color(0xFFFF8A65) else Color(0xFFA5D6A7),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stitekRezimu,
                    color = Color(0xFFA5D6A7),
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF144227))
                        .border(0.8.dp, Color(0x55E6C15A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    when (typHry) {
                        TypHry.PRSI -> if (stavPrsi != null) {
                            IkonaBarvyKarty(stavPrsi.aktualniPlatnaBarva, velikost = 12.dp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stavPrsi.aktualniPlatnaBarva.ceskyNazev,
                                color = Color(0xFFFFF59D),
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TypHry.SEDMA -> if (stavSedma != null) {
                            Text(
                                text = "${stavSedma.uhraneBodyTym0} : ${stavSedma.uhraneBodyTym1} b.",
                                color = Color(0xFFFFF59D),
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        TypHry.MARIAS -> if (stavMarias != null) {
                            if (stavMarias.trumf != null && !stavMarias.jeBetlNeboDurch) {
                                IkonaBarvyKarty(stavMarias.trumf, velikost = 12.dp)
                                Spacer(modifier = Modifier.width(3.dp))
                                val nasobkyText = if (stavMarias.zavazek.hlasiSedmu && stavMarias.nasobekFlekSedma > 1) {
                                    "${stavMarias.nasobekFlekHra}×/7:${stavMarias.nasobekFlekSedma}×"
                                } else {
                                    "${stavMarias.nasobekFlekHra}×"
                                }
                                Text(
                                    text = "${stavMarias.trumf.ceskyNazev} ($nasobkyText)",
                                    color = Color(0xFFFFF59D),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            } else {
                                Text(
                                    text = when (stavMarias.zavazek) {
                                        ZavazekMariase.BETL -> "Betl (${stavMarias.nasobekFlekHra}×)"
                                        ZavazekMariase.DURCH -> "Durch (${stavMarias.nasobekFlekHra}×)"
                                        else -> "Volba"
                                    },
                                    color = Color(0xFFFFF59D),
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (zvukZapnut) Color(0xFF1D5836) else Color(0xFF3E2723))
                        .border(
                            width = 0.9.dp,
                            color = if (zvukZapnut) Color(0x8869F0AE) else Color(0x88FF8A80),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            jemnaHaptika()
                            onPrepnoutZvuk()
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (zvukZapnut) "🔊" else "🔇",
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PruhySouperu(
    typHry: TypHry,
    lokalniIndex: Int,
    onlineStul: OnlineStul?,
    stavPrsi: StavPrsi?,
    stavSedma: StavSedma?,
    stavMarias: StavMarias?,
    nyniMs: Long
) {
    val pocet = when (typHry) {
        TypHry.PRSI -> stavPrsi?.pocetHracu ?: 2
        TypHry.SEDMA -> stavSedma?.pocetHracu ?: 2
        TypHry.MARIAS -> 3
    }
    val naTahu = when (typHry) {
        TypHry.PRSI -> stavPrsi?.naTahu ?: 0
        TypHry.SEDMA -> stavSedma?.naTahu ?: 0
        TypHry.MARIAS -> stavMarias?.naTahu ?: 0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        for (posun in 1 until pocet) {
            val idx = (lokalniIndex + posun) % pocet
            val jmeno = when (typHry) {
                TypHry.PRSI -> stavPrsi?.jmenaHracu?.getOrNull(idx) ?: "Soupeř"
                TypHry.SEDMA -> stavSedma?.jmenaHracu?.getOrNull(idx) ?: "Soupeř"
                TypHry.MARIAS -> stavMarias?.jmenaHracu?.getOrNull(idx) ?: "Soupeř"
            }
            val pocetKaret = when (typHry) {
                TypHry.PRSI -> stavPrsi?.ruceHracu?.getOrNull(idx)?.size ?: 0
                TypHry.SEDMA -> stavSedma?.ruceHracu?.getOrNull(idx)?.size ?: 0
                TypHry.MARIAS -> stavMarias?.ruceHracu?.getOrNull(idx)?.size ?: 0
            }
            val rolePopisek = when (typHry) {
                TypHry.MARIAS -> if (stavMarias?.indexForhonta == idx) "Forhont" else "Obrana"
                TypHry.SEDMA -> if (pocet == 4 && idx % 2 == lokalniIndex % 2) "Parťák" else "Soupeř"
                TypHry.PRSI -> ""
            }
            val hracNaTahu = naTahu == idx
            val onlineHrac = onlineStul?.hraci?.getOrNull(idx)
            val jeOnlineBot = onlineHrac != null && jeBotDeviceId(onlineHrac.deviceId)
            val infoSpojeni = if (onlineStul != null && onlineHrac != null) {
                onlineStul.infoPripojeni(onlineHrac.deviceId, nyniMs)
            } else null

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (hracNaTahu) Color(0xD91A5E36) else Color(0xB3072313))
                    .border(
                        width = if (hracNaTahu) 1.8.dp else 1.dp,
                        color = if (hracNaTahu) Color(0xFF69F0AE) else Color(0x55E6C15A),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                val barvaTecky = when {
                    jeOnlineBot -> Color(0xFF90CAF9)
                    infoSpojeni?.stav == StavPripojeniHrace.VYPADEK_ZLUTA -> Color(0xFFFFCA28)
                    infoSpojeni?.stav == StavPripojeniHrace.ODPOJEN_CERVENA -> Color(0xFFFF5252)
                    else -> Color(0xFF00E676)
                }
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(barvaTecky)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = jmeno,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val podtext = when {
                        infoSpojeni?.stav == StavPripojeniHrace.VYPADEK_ZLUTA ->
                            "Výpadek (${infoSpojeni.zbyvaSekundDoUkonceni}s)"
                        rolePopisek.isNotEmpty() -> "$rolePopisek • ${sklonujListy(pocetKaret)}"
                        else -> "${sklonujListy(pocetKaret)} v ruce"
                    }
                    Text(
                        text = podtext,
                        color = if (infoSpojeni?.stav == StavPripojeniHrace.VYPADEK_ZLUTA) {
                            Color(0xFFFFCC80)
                        } else {
                            Color(0xFFB9F6CA)
                        },
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (pocetKaret > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    KartaRubComposable(sirka = 22.dp)
                }
            }
        }
    }
}
