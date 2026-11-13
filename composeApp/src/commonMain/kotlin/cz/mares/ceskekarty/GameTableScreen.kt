package cz.mares.ceskekarty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class KonfiguraceOfflineHry(
    val typHry: TypHry,
    val pocetHracu: Int,
    val jeOfflinePohar: Boolean = false,
    val celkemRozdani: Int = 3
)

private data class UiPolozkaVyuctovani(
    val popis: String,
    val hodnotaText: String,
    val jeZvyraznena: Boolean
)

private data class UiHracVyuctovani(
    val indexHrace: Int,
    val jmenoHrace: String,
    val jeVitez: Boolean,
    val bodyZapasu: Int,
    val zmenaTerezianu: Int
)

private data class UiVyuctovani(
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

private fun sklonujListy(pocet: Int): String {
    return when {
        pocet == 1 -> "1 list"
        pocet in 2..4 -> "$pocet listy"
        else -> "$pocet listů"
    }
}

private fun sklonujKartyAkuzativ(pocet: Int): String {
    return when {
        pocet == 1 -> "1 kartu"
        pocet in 2..4 -> "$pocet karty"
        else -> "$pocet karet"
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    val jeOnlineRezim = pocatecniOnlineStul != null

    var onlineStul by remember { mutableStateOf(pocatecniOnlineStul) }
    var tikSekundyMs by remember { mutableStateOf(ziskejAktualniCasMs()) }
    var potvrditOdchod by remember { mutableStateOf(false) }
    var aktualniMesecTerezianu by remember { mutableStateOf(nactiLokalniTereziany()) }
    var zvukZapnut by remember { mutableStateOf(jeZvukAHlaskyZapnuty()) }

    val aktivniTypHry = onlineStul?.typHry ?: offlineKonfig?.typHry ?: TypHry.PRSI
    val pocetHracu = onlineStul?.maxHracu ?: offlineKonfig?.pocetHracu ?: 3

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
                    ?: PrsiEngine.novaHra(vychoziJmenaOffline)
            } else null
        )
    }
    var stavSedma by remember {
        mutableStateOf(
            if (aktivniTypHry == TypHry.SEDMA) {
                onlineStul?.serializovanyStavHry?.let { StavSedma.zKompaktnihoRetezce(it) }
                    ?: SedmaEngine.novaHra(vychoziJmenaOffline)
            } else null
        )
    }
    var stavMarias by remember {
        mutableStateOf(
            if (aktivniTypHry == TypHry.MARIAS) {
                onlineStul?.serializovanyStavHry?.let { StavMarias.zKompaktnihoRetezce(it) }
                    ?: MariasEngine.novaHra(vychoziJmenaOffline, indexForhonta = 0)
            } else null
        )
    }

    var vybraneKartyDoTalonu by remember { mutableStateOf<List<Karta>>(emptyList()) }
    var planovanyZavazekMariase by remember { mutableStateOf(ZavazekMariase.HRA) }
    var zapoctenaVerzeVysledku by remember { mutableStateOf(-1) }

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
            if (cerstvy.stavMistnosti == StavMistnosti.CEKANI && cerstvy.hraci.size < cerstvy.maxHracu) {
                onVratitDoCekarny(cerstvy)
                break
            }

            val zmenaStavu = cerstvy.verzeStavu != onlineStul?.verzeStavu ||
                cerstvy.serializovanyStavHry != onlineStul?.serializovanyStavHry
            onlineStul = cerstvy
            if (zmenaStavu && cerstvy.serializovanyStavHry.isNotBlank()) {
                when (cerstvy.typHry) {
                    TypHry.PRSI -> StavPrsi.zKompaktnihoRetezce(cerstvy.serializovanyStavHry)?.let { stavPrsi = it }
                    TypHry.SEDMA -> StavSedma.zKompaktnihoRetezce(cerstvy.serializovanyStavHry)?.let { stavSedma = it }
                    TypHry.MARIAS -> StavMarias.zKompaktnihoRetezce(cerstvy.serializovanyStavHry)?.let {
                        stavMarias = it
                        if (it.faze != FazeMariase.ODKLADANI_TALONU) {
                            vybraneKartyDoTalonu = emptyList()
                        } else {
                            planovanyZavazekMariase = it.zavazek
                        }
                    }
                }
            }

            if (cerstvy.stavMistnosti == StavMistnosti.HRAJE_SE) {
                val nyni = ziskejAktualniCasMs()
                val jeHraHotova = when (cerstvy.typHry) {
                    TypHry.PRSI -> stavPrsi?.jeKonec == true
                    TypHry.SEDMA -> stavSedma?.jeKonec == true
                    TypHry.MARIAS -> stavMarias?.faze == FazeMariase.KONEC
                }
                if (!jeHraHotova) {
                    val odpojenyHrac = cerstvy.hraci.firstOrNull { hrac ->
                        cerstvy.infoPripojeni(hrac.deviceId, nyni).stav == StavPripojeniHrace.ODPOJEN_CERVENA
                    }
                    val prekrocenTah = nyni - cerstvy.zacatekTahuMs >= LIMIT_ODPOJENI_MS
                    if (odpojenyHrac != null || prekrocenTah) {
                        val kdoIndex = when (cerstvy.typHry) {
                            TypHry.PRSI -> stavPrsi?.naTahu ?: 0
                            TypHry.SEDMA -> stavSedma?.naTahu ?: 0
                            TypHry.MARIAS -> stavMarias?.naTahu ?: 0
                        }
                        val jmenoVinika = odpojenyHrac?.prezdivka
                            ?: cerstvy.hraci.getOrNull(kdoIndex)?.prezdivka
                            ?: "Hráč"
                        val ukonceny = OnlineManager.ukonciStulProVypadek(cerstvy, jmenoVinika)
                        onlineStul = ukonceny

                        if (cerstvy.turnajId.isNotEmpty() && odpojenyHrac?.deviceId != mojeDeviceId) {
                            val pritomny = cerstvy.hraci.firstOrNull { it.deviceId != odpojenyHrac?.deviceId }
                            if (pritomny != null) {
                                OnlineManager.zapisVysledekTurnajovehoZapasu(
                                    turnajId = cerstvy.turnajId,
                                    slotZapasu = cerstvy.slotZapasu,
                                    vitezDeviceId = pritomny.deviceId,
                                    vitezJmeno = pritomny.prezdivka
                                )
                            }
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

    LaunchedEffect(jeOnlineRezim, aktualniCisloTahu) {
        if (jeOnlineRezim) return@LaunchedEffect
        val prodlevaBotaMs = if (zvukZapnut) 1050L else 700L
        when (aktivniTypHry) {
            TypHry.PRSI -> {
                val s = stavPrsi ?: return@LaunchedEffect
                if (!s.jeKonec && s.naTahu != 0) {
                    delay(prodlevaBotaMs)
                    stavPrsi = PrsiEngine.provedTahBota(s)
                }
            }
            TypHry.SEDMA -> {
                val s = stavSedma ?: return@LaunchedEffect
                if (!s.jeKonec && s.naTahu != 0) {
                    delay(prodlevaBotaMs)
                    stavSedma = SedmaEngine.provedTahBota(s)
                }
            }
            TypHry.MARIAS -> {
                val s = stavMarias ?: return@LaunchedEffect
                if (s.faze != FazeMariase.KONEC && s.naTahu != 0) {
                    delay(prodlevaBotaMs)
                    stavMarias = MariasEngine.provedTahBota(s)
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
        (onlineStul?.cisloRozdani ?: cisloOfflineRozdani) * 10000 + aktualniCisloTahu
    } else {
        -1
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
                soucetBoduOffline = soucetBoduOffline.mapIndexed { idx, stary ->
                    stary + (vysledekPartie.hraci.firstOrNull { it.indexHrace == idx }?.bodyZapasu ?: 0)
                }
            } else {
                val stul = onlineStul
                if (stul != null && stul.turnajId.isNotEmpty()) {
                    val noveSoucty = stul.soucetBoduHracu.mapIndexed { idx, stary ->
                        stary + (vysledekPartie.hraci.firstOrNull { it.indexHrace == idx }?.bodyZapasu ?: 0)
                    }
                    if (stul.cisloRozdani >= stul.celkemRozdani) {
                        val vitezZapasuIdx = if (stul.celkemRozdani > 1) {
                            noveSoucty.indices.maxByOrNull { noveSoucty[it] } ?: 0
                        } else {
                            vysledekPartie.hraci.firstOrNull { it.jeVitez }?.indexHrace ?: 0
                        }
                        val vitezHrac = stul.hraci.getOrNull(vitezZapasuIdx)
                        if (vitezHrac != null && stul.hostDeviceId == mojeDeviceId) {
                            OnlineManager.zapisVysledekTurnajovehoZapasu(
                                turnajId = stul.turnajId,
                                slotZapasu = stul.slotZapasu,
                                vitezDeviceId = vitezHrac.deviceId,
                                vitezJmeno = vitezHrac.prezdivka
                            )
                        }
                    }
                }
            }
        }
    }

    ZelenyKaretniStulBox(zobrazitStredovyKruh = true) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val naSirku = maxWidth > maxHeight
            val sirkaKartyRuky: Dp = if (naSirku) {
                (maxHeight.value * 0.165f).coerceIn(48f, 62f).dp
            } else {
                (maxWidth.value * 0.195f).coerceIn(64f, 76f).dp
            }
            val sirkaKartyStolu: Dp = if (naSirku) {
                (maxHeight.value * 0.155f).coerceIn(46f, 58f).dp
            } else {
                (maxHeight.value * 0.105f).coerceIn(62f, 76f).dp
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
                        jeOnline = jeOnlineRezim,
                        onlineStul = onlineStul,
                        offlineKonfig = offlineKonfig,
                        cisloOfflineRozdani = cisloOfflineRozdani,
                        stavPrsi = stavPrsi,
                        stavSedma = stavSedma,
                        stavMarias = stavMarias,
                        tikSekundyMs = tikSekundyMs,
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
                                    scope.launch {
                                        OnlineManager.opustStul(stul.roomId)
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
                                sirkaKarty = sirkaKartyStolu,
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
                                naSirku = naSirku,
                                onPustitStych = {
                                    val novy = SedmaEngine.pustStych(s, lokalniIndex)
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
                            vybraneKartyDoTalonu = if (karta in vybraneDoTalonu) {
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

            if (vysledekPartie != null && onlineStul?.stavMistnosti != StavMistnosti.UKONCENO_VYPADKEM) {
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
                        zapoctenaVerzeVysledku = -1
                        vybraneKartyDoTalonu = emptyList()
                        planovanyZavazekMariase = ZavazekMariase.HRA
                        if (!jeOnlineRezim) {
                            val dalsiKolo = cisloOfflineRozdani + 1
                            cisloOfflineRozdani = dalsiKolo
                            val zacinajici = (dalsiKolo - 1) % pocetHracu
                            when (aktivniTypHry) {
                                TypHry.PRSI -> stavPrsi = PrsiEngine.novaHra(vychoziJmenaOffline, zacinajiciHrac = zacinajici)
                                TypHry.SEDMA -> stavSedma = SedmaEngine.novaHra(vychoziJmenaOffline, zacinajiciHrac = zacinajici)
                                TypHry.MARIAS -> stavMarias = MariasEngine.novaHra(vychoziJmenaOffline, indexForhonta = zacinajici)
                            }
                        } else {
                            val stul = onlineStul ?: return@PanelVyuctovaniPartie
                            val noveSoucty = stul.soucetBoduHracu.mapIndexed { idx, stary ->
                                stary + (vysledekPartie.hraci.firstOrNull { it.indexHrace == idx }?.bodyZapasu ?: 0)
                            }
                            val dalsiRozdani = stul.cisloRozdani + 1
                            val zacinajici = (dalsiRozdani - 1) % stul.maxHracu
                            scope.launch {
                                val novyStul = OnlineManager.rozdejASpustStul(
                                    stul = stul,
                                    zacinajiciIndex = zacinajici,
                                    noveCisloRozdani = dalsiRozdani,
                                    noveSouctyBodu = noveSoucty
                                )
                                if (novyStul != null) onlineStul = novyStul
                            }
                        }
                    },
                    onZpetDoHerny = {
                        val stul = onlineStul
                        if (stul != null) {
                            scope.launch {
                                OnlineManager.opustStul(stul.roomId)
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
                            text = "Opustit rozehraný stůl?",
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Opuštěním živého stolu se partie ostatním spoluhráčům ukončí.",
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
                                            OnlineManager.ukonciStulProVypadek(stul, mojePrezdivka)
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
    jeOnline: Boolean,
    onlineStul: OnlineStul?,
    offlineKonfig: KonfiguraceOfflineHry?,
    cisloOfflineRozdani: Int,
    stavPrsi: StavPrsi?,
    stavSedma: StavSedma?,
    stavMarias: StavMarias?,
    tikSekundyMs: Long,
    zvukZapnut: Boolean,
    onPrepnoutZvuk: () -> Unit,
    onKlikZpet: () -> Unit
) {
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
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val stitekRezimu = when {
                onlineStul != null && onlineStul.turnajId.isNotEmpty() ->
                    "TURNAJ (${onlineStul.cisloRozdani}/${onlineStul.celkemRozdani})"
                jeOnline -> "ŽIVÝ STŮL"
                offlineKonfig?.jeOfflinePohar == true ->
                    "POHÁR ($cisloOfflineRozdani/${offlineKonfig.celkemRozdani})"
                else -> "TRÉNINK"
            }
            Text(
                text = typHry.nazev,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (jeOnline && onlineStul != null && onlineStul.stavMistnosti == StavMistnosti.HRAJE_SE) {
                val zbyvaTahu = max(0L, (LIMIT_ODPOJENI_MS - (tikSekundyMs - onlineStul.zacatekTahuMs)) / 1000L)
                Text(
                    text = "$stitekRezimu • Tah: ${zbyvaTahu}s",
                    color = if (zbyvaTahu <= 15L) Color(0xFFFF8A65) else Color(0xFFA5D6A7),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                Text(
                    text = stitekRezimu,
                    color = Color(0xFFA5D6A7),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                        IkonaBarvyKarty(stavPrsi.aktualniPlatnaBarva, velikost = 13.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stavPrsi.aktualniPlatnaBarva.ceskyNazev,
                            color = Color(0xFFFFF59D),
                            fontSize = 10.sp,
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    TypHry.MARIAS -> if (stavMarias != null) {
                        if (stavMarias.trumf != null && !stavMarias.jeBetlNeboDurch) {
                            IkonaBarvyKarty(stavMarias.trumf, velikost = 13.dp)
                            Spacer(modifier = Modifier.width(3.dp))
                            val nasobkyText = if (stavMarias.zavazek.hlasiSedmu) {
                                "${stavMarias.nasobekFlekHra}×/7:${stavMarias.nasobekFlekSedma}×"
                            } else {
                                "${stavMarias.nasobekFlekHra}×"
                            }
                            Text(
                                text = "${stavMarias.trumf.ceskyNazev} ($nasobkyText)",
                                color = Color(0xFFFFF59D),
                                fontSize = 10.sp,
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
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (zvukZapnut) "🔊" else "🔇",
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
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
                val barvaTecky = when (infoSpojeni?.stav) {
                    StavPripojeniHrace.ONLINE_ZELENA, null -> Color(0xFF00E676)
                    StavPripojeniHrace.VYPADEK_ZLUTA -> Color(0xFFFFCA28)
                    StavPripojeniHrace.ODPOJEN_CERVENA -> Color(0xFFFF5252)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (pocetKaret > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    KartaRubComposable(sirka = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun StredStoluPrsi(
    stav: StavPrsi,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    naSirku: Boolean,
    onLiznoutNeboStat: () -> Unit,
    onZvolitBarvuSvrskem: (BarvaKarty) -> Unit
) {
    val jsemNaTahu = stav.naTahu == lokalniIndex && !stav.jeKonec
    val povolene = remember(stav, lokalniIndex) { PrsiEngine.zjistiPovoleneKarty(stav, lokalniIndex) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stav.posledniHlaska,
            color = Color(0xFFFFF8E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0x9E061E10))
                .border(1.dp, Color(0x55E6C15A), RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(if (naSirku) 6.dp else 10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            KartaRubComposable(
                sirka = sirkaKarty,
                pocetVBalicku = stav.lizaciBalicek.size,
                popisek = if (stav.aktivniEsoStoji) "STÁT" else "LÍZAT",
                zvyseneZvyrazneni = jsemNaTahu && !stav.cekaNaVolbuBarvy && povolene.isEmpty(),
                onClick = if (jsemNaTahu && !stav.cekaNaVolbuBarvy) onLiznoutNeboStat else null
            )

            KartaLiceComposable(
                karta = stav.vrchniKarta,
                sirka = sirkaKarty,
                stitekNavic = stav.zvolenaBarvaSvrskem?.ceskyNazev
            )
        }

        if (jsemNaTahu && !stav.cekaNaVolbuBarvy) {
            Spacer(modifier = Modifier.height(if (naSirku) 6.dp else 10.dp))
            val textTlacitka = when {
                stav.aktivniEsoStoji -> "Stát 1 kolo (potvrdit eso)"
                stav.aktivniSedmyPocet > 0 -> "Líznout ${sklonujKartyAkuzativ(stav.aktivniSedmyPocet * 2)}"
                else -> "Líznout kartu z balíčku"
            }
            val zvyraznitLiznuti = povolene.isEmpty()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (zvyraznitLiznuti) Color(0xFFD4AF37) else Color(0xFF14492A))
                    .border(
                        width = 1.2.dp,
                        color = if (zvyraznitLiznuti) Color(0xFFFFF59D) else Color(0x66E6C15A),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        jemnaHaptika()
                        onLiznoutNeboStat()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = textTlacitka,
                    color = if (zvyraznitLiznuti) Color(0xFF072312) else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (jsemNaTahu && stav.cekaNaVolbuBarvy) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEB082815))
                    .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                for (barva in BarvaKarty.entries) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1C5435))
                            .border(0.8.dp, Color(0x44FFD54F), RoundedCornerShape(8.dp))
                            .clickable {
                                jemnaHaptika()
                                onZvolitBarvuSvrskem(barva)
                            }
                            .padding(horizontal = 4.dp, vertical = 7.dp)
                    ) {
                        IkonaBarvyKarty(barva = barva, velikost = 20.dp)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = barva.ceskyNazev,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StredStoluSedma(
    stav: StavSedma,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    naSirku: Boolean,
    onPustitStych: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stav.posledniHlaska,
            color = Color(0xFFFFF8E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0x9E061E10))
                .border(1.dp, Color(0x55E6C15A), RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(if (naSirku) 6.dp else 10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KartaRubComposable(
                sirka = sirkaKarty * 0.86f,
                pocetVBalicku = stav.lizaciBalicek.size,
                popisek = "TALON"
            )

            if (stav.aktualniStych.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(width = sirkaKarty * 1.65f, height = sirkaKarty * 1.48f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33000000))
                        .border(1.2.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Čeká na výnos", color = Color(0xCCFFF8E1), fontSize = 12.sp)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    for (hrana in stav.aktualniStych.takeLast(4)) {
                        val bereTato = hrana.indexHrace == stav.prozatimBereHrac &&
                            hrana == stav.aktualniStych.lastOrNull { it.indexHrace == stav.prozatimBereHrac }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = hrana.jmenoHrace,
                                color = if (bereTato) Color(0xFF69F0AE) else Color(0xFFFFF8E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            KartaLiceComposable(
                                karta = hrana.karta,
                                sirka = sirkaKarty,
                                stitekNavic = if (bereTato) "BERE" else null
                            )
                        }
                    }
                }
            }
        }

        if (stav.muzePustitStych && stav.naTahu == lokalniIndex && !stav.jeKonec) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD4AF37))
                    .clickable {
                        jemnaHaptika()
                        onPustitStych()
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Pustit štych soupeři",
                    color = Color(0xFF082413),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StredStoluMarias(
    stav: StavMarias,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    vybraneDoTalonu: List<Karta>,
    planovanyZavazek: ZavazekMariase,
    onZmenaPlanovanehoZavazku: (ZavazekMariase) -> Unit,
    onVolbaTrumfu: (BarvaKarty?, Boolean, ZavazekMariase) -> Unit,
    onPotvrditTalon: () -> Unit,
    onOdpovedFleku: (Boolean, Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stav.posledniHlaska,
            color = Color(0xFFFFF8E1),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0x9E061E10))
                .border(1.dp, Color(0x55E6C15A), RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        when (stav.faze) {
            FazeMariase.VOLBA_TRUMFU -> {
                if (stav.naTahu == lokalniIndex) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xEB082815))
                            .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Jsi na forhontu — zvol trumf ze 7 karet:",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (b in BarvaKarty.entries) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1D5836))
                                        .border(0.8.dp, Color(0x44FFD54F), RoundedCornerShape(8.dp))
                                        .clickable {
                                            jemnaHaptika()
                                            onVolbaTrumfu(b, false, ZavazekMariase.HRA)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 7.dp)
                                ) {
                                    IkonaBarvyKarty(b, velikost = 19.dp)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = b.ceskyNazev,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(7.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MaleAkcniTlacitko(
                                text = "Z talonu",
                                barva = Color(0xFF286040),
                                modifier = Modifier.weight(1f)
                            ) {
                                onVolbaTrumfu(null, true, ZavazekMariase.HRA)
                            }
                            MaleAkcniTlacitko(
                                text = "Rovnou Betl",
                                barva = Color(0xFF5D4037),
                                modifier = Modifier.weight(1f)
                            ) {
                                onVolbaTrumfu(null, false, ZavazekMariase.BETL)
                            }
                            MaleAkcniTlacitko(
                                text = "Rovnou Durch",
                                barva = Color(0xFF6A1B9A),
                                modifier = Modifier.weight(1f)
                            ) {
                                onVolbaTrumfu(null, false, ZavazekMariase.DURCH)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Čekání, než ${stav.jmenaHracu[stav.indexForhonta]} zvolí trumf…",
                        color = Color(0xFFB9F6CA),
                        fontSize = 13.sp
                    )
                }
            }

            FazeMariase.ODKLADANI_TALONU -> {
                if (stav.naTahu == lokalniIndex) {
                    val maPravou7 = MariasEngine.maPravouSedmu(stav, vybraneDoTalonu)
                    val maPrave2Sedmy = MariasEngine.maPraveDveSedmy(stav, vybraneDoTalonu)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xEB082815))
                            .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Klepnutím v ruce vyber 2 karty do talonu (${vybraneDoTalonu.size}/2)",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!planovanyZavazek.jeBezTrumfu) {
                            Text(
                                text = "Esa a desítky jsou chráněné proti odložení.",
                                color = Color(0xFFA5D6A7),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val moznosti = when {
                            stav.volenoZTalonu -> listOf(ZavazekMariase.HRA)
                            stav.trumf == null -> listOf(ZavazekMariase.BETL, ZavazekMariase.DURCH)
                            else -> listOf(
                                ZavazekMariase.HRA,
                                ZavazekMariase.HRA_A_SEDMA,
                                ZavazekMariase.HRA_A_DVE_SEDMY,
                                ZavazekMariase.STO,
                                ZavazekMariase.STO_A_SEDMA,
                                ZavazekMariase.STO_A_DVE_SEDMY,
                                ZavazekMariase.BETL,
                                ZavazekMariase.DURCH
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (moznost in moznosti) {
                                val aktivni = planovanyZavazek == moznost
                                val popisek = when (moznost) {
                                    ZavazekMariase.HRA_A_SEDMA -> if (maPravou7) "Hra + 7" else "Hra + fal. 7"
                                    ZavazekMariase.HRA_A_DVE_SEDMY -> if (maPrave2Sedmy) "Hra + 2×7" else "Hra + fal. 2×7"
                                    ZavazekMariase.STO -> "Stovka"
                                    ZavazekMariase.STO_A_SEDMA -> if (maPravou7) "100 + 7" else "100 + fal. 7"
                                    ZavazekMariase.STO_A_DVE_SEDMY -> if (maPrave2Sedmy) "100 + 2×7" else "100 + fal. 2×7"
                                    else -> moznost.nazev
                                }
                                MaleAkcniTlacitko(
                                    text = popisek,
                                    barva = if (aktivni) Color(0xFFD4AF37) else Color(0xFF205536),
                                    barvaTextu = if (aktivni) Color(0xFF072312) else Color.White
                                ) {
                                    onZmenaPlanovanehoZavazku(moznost)
                                }
                            }
                        }

                        if (planovanyZavazek.hlasiDveSedmy && !maPrave2Sedmy) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pozor: nemáš obě sedmy — hlásíš falešné dvě sedmy (blaf)!",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else if (planovanyZavazek.hlasiSedmu && !maPravou7) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pozor: nemáš trumfovou sedmu — hlásíš falešnou sedmu (blaf)!",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                jemnaHaptika()
                                onPotvrditTalon()
                            },
                            enabled = vybraneDoTalonu.size == 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                        ) {
                            Text(
                                text = "Odložit talon a hlásit ${planovanyZavazek.nazev}",
                                color = Color(0xFF05200E),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${stav.jmenaHracu[stav.indexForhonta]} odkládá 2 karty do talonu…",
                        color = Color(0xFFB9F6CA),
                        fontSize = 13.sp
                    )
                }
            }

            FazeMariase.FLEKOVANI -> {
                if (stav.naTahu == lokalniIndex) {
                    val muzeHru = MariasEngine.muzeHracFlekHry(stav, lokalniIndex)
                    val muzeSedmu = MariasEngine.muzeHracFlekSedmy(stav, lokalniIndex)
                    val stupenHra = MariasEngine.nazevStupneFleku(stav.nasobekFlekHra * 2)
                    val stupenSedma = MariasEngine.nazevStupneFleku(stav.nasobekFlekSedma * 2)
                    val slovoSedma = if (stav.zavazek.hlasiDveSedmy) "2×7" else "sedmu"

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .widthIn(max = 360.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xEB082815))
                            .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                            .padding(10.dp)
                    ) {
                        Button(
                            onClick = {
                                jemnaHaptika()
                                onOdpovedFleku(false, false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Dobrý", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                        if (muzeHru) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    onOdpovedFleku(true, false)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                            ) {
                                Text("$stupenHra na hru!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        if (muzeSedmu) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    onOdpovedFleku(false, true)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAD1457))
                            ) {
                                Text("$stupenSedma na $slovoSedma!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        if (muzeHru && muzeSedmu) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    onOdpovedFleku(true, true)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                            ) {
                                Text("Zvednout obojí!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Vyjadřuje se ${stav.jmenaHracu[stav.naTahu]} (Dobrý / Flek)…",
                        color = Color(0xFFB9F6CA),
                        fontSize = 13.sp
                    )
                }
            }

            FazeMariase.SEHRAVKA, FazeMariase.KONEC -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stav.cisloStychu}. štych • Forhont ${stav.bodyKaretForhont + stav.hlaskyForhont} : Obrana ${stav.bodyKaretObrana + stav.hlaskyObrana}",
                        color = Color(0xFFFFF59D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (stav.aktualniStych.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(width = sirkaKarty * 1.85f, height = sirkaKarty * 1.48f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33000000))
                                .border(1.2.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Vynáší ${stav.jmenaHracu[stav.naTahu]}",
                                color = Color(0xDDFFF8E1),
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (hrana in stav.aktualniStych) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = hrana.jmenoHrace,
                                        color = Color(0xFFFFF59D),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    KartaLiceComposable(karta = hrana.karta, sirka = sirkaKarty)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpodniRukaHrace(
    typHry: TypHry,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    naSirku: Boolean,
    stavPrsi: StavPrsi?,
    stavSedma: StavSedma?,
    stavMarias: StavMarias?,
    onlineStul: OnlineStul?,
    nyniMs: Long,
    vybraneDoTalonu: List<Karta>,
    planovanyZavazek: ZavazekMariase,
    onKlikKartaPrsi: (Karta) -> Unit,
    onKlikKartaSedma: (Karta) -> Unit,
    onKlikKartaMarias: (Karta) -> Unit
) {
    val ruka: List<Karta>
    val povolene: Set<Karta>
    val aktivniKontrola: Boolean
    val zvyraznitPanelRuky: Boolean
    val popisPovinnosti: String

    when (typHry) {
        TypHry.PRSI -> {
            val s = stavPrsi
            ruka = s?.ruceHracu?.getOrNull(lokalniIndex) ?: emptyList()
            val voliBarvuSvrskem = s != null && !s.jeKonec && s.naTahu == lokalniIndex && s.cekaNaVolbuBarvy
            val mujTah = s != null && !s.jeKonec && s.naTahu == lokalniIndex && !s.cekaNaVolbuBarvy
            povolene = if (mujTah && s != null) PrsiEngine.zjistiPovoleneKarty(s, lokalniIndex) else emptySet()
            aktivniKontrola = mujTah
            zvyraznitPanelRuky = mujTah || voliBarvuSvrskem
            popisPovinnosti = when {
                voliBarvuSvrskem -> "Vynesl jsi svrška — zvol uprostřed novou barvu!"
                !mujTah -> "Čekáš na tah soupeře"
                s?.aktivniSedmyPocet ?: 0 > 0 ->
                    "Aktivní sedma: přebij sedmičkou, nebo lízni ${sklonujKartyAkuzativ((s?.aktivniSedmyPocet ?: 1) * 2)}"
                s?.aktivniEsoStoji == true -> "Aktivní eso: přebij esem, nebo stůj 1 kolo"
                povolene.isEmpty() -> "Nemáš vhodnou kartu — klepni na balíček pro líznutí"
                else -> "Jsi na tahu — klepni na zeleně zvýrazněnou kartu"
            }
        }

        TypHry.SEDMA -> {
            val s = stavSedma
            ruka = s?.ruceHracu?.getOrNull(lokalniIndex) ?: emptyList()
            val mujTah = s != null && !s.jeKonec && s.naTahu == lokalniIndex
            povolene = if (mujTah && s != null) SedmaEngine.zjistiPovoleneKarty(s, lokalniIndex) else emptySet()
            aktivniKontrola = mujTah
            zvyraznitPanelRuky = mujTah
            popisPovinnosti = when {
                !mujTah -> "Čekáš na tah u stolu"
                s?.muzePustitStych == true -> "Můžeš vrátit přebití zvýrazněnou kartou, nebo štych pustit"
                else -> "Jsi na tahu — štítek BERE značí kartu, která přebíjí štych"
            }
        }

        TypHry.MARIAS -> {
            val s = stavMarias
            ruka = s?.ruceHracu?.getOrNull(lokalniIndex) ?: emptyList()
            val voliTrumf = s != null && s.faze == FazeMariase.VOLBA_TRUMFU && s.naTahu == lokalniIndex
            val odkladaTalon = s != null && s.faze == FazeMariase.ODKLADANI_TALONU && s.naTahu == lokalniIndex
            val flekuje = s != null && s.faze == FazeMariase.FLEKOVANI && s.naTahu == lokalniIndex
            val hrajeStych = s != null && s.faze == FazeMariase.SEHRAVKA && s.naTahu == lokalniIndex
            povolene = when {
                odkladaTalon && s != null -> MariasEngine.zjistiKartyPovoleneDoTalonu(s, planovanyZavazek)
                hrajeStych && s != null -> MariasEngine.zjistiPovoleneKarty(s, lokalniIndex)
                else -> emptySet()
            }
            aktivniKontrola = odkladaTalon || hrajeStych
            zvyraznitPanelRuky = voliTrumf || odkladaTalon || flekuje || hrajeStych
            popisPovinnosti = when {
                voliTrumf -> "Prohlédni si svých 7 karet a nahoře zvol trumf"
                odkladaTalon -> "Zvol 2 povolené karty do talonu"
                flekuje -> "Prohlédni si list a nahoře zvol Dobrý nebo Flek"
                !hrajeStych -> "Sleduj stůl"
                s?.aktualniStych?.isEmpty() == true -> "Vynášíš do štychu"
                else -> {
                    val vynosBarva = s?.aktualniStych?.firstOrNull()?.karta?.barva
                    val maBarvu = vynosBarva != null && ruka.any { it.barva == vynosBarva }
                    val maTrumf = s?.trumf != null && !s.jeBetlNeboDurch && ruka.any { it.barva == s.trumf }
                    when {
                        maBarvu -> "Povinnost přiznat (a přebít) barvu ${vynosBarva?.ceskyNazev}"
                        maTrumf -> "Nemáš barvu — povinnost trumfovat (${s?.trumf?.ceskyNazev})!"
                        else -> "Nemáš barvu ani trumf — volný odhoz"
                    }
                }
            }
        }
    }

    val mojeInfoSpojeni = onlineStul?.hraci?.getOrNull(lokalniIndex)?.let {
        onlineStul.infoPripojeni(it.deviceId, nyniMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xB8062011))
            .border(
                width = if (zvyraznitPanelRuky) 1.6.dp else 1.dp,
                color = if (zvyraznitPanelRuky) Color(0xFF69F0AE) else Color(0x55E6C15A),
                shape = RoundedCornerShape(15.dp)
            )
            .padding(horizontal = 6.dp, vertical = if (naSirku) 4.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            if (mojeInfoSpojeni != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (mojeInfoSpojeni.stav == StavPripojeniHrace.ONLINE_ZELENA) Color(0xFF00E676)
                            else Color(0xFFFFCA28)
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = popisPovinnosti,
                color = if (zvyraznitPanelRuky) Color(0xFF69F0AE) else Color(0xFFFFF8E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pocetVRuce = ruka.size
            val stredIdx = (pocetVRuce - 1) / 2f

            val vnitrniSirkaDp = (maxWidth.value - 24f).coerceAtLeast(180f)
            val kartaSirkaDp = if (pocetVRuce in 1..4) {
                minOf(sirkaKarty.value, (vnitrniSirkaDp - 12f) / 4f).coerceAtLeast(52f)
            } else {
                sirkaKarty.value
            }
            val minRozestupDp = -(kartaSirkaDp - 30f)
            val maxRozestupDp = 7f
            val rozestupDp = if (pocetVRuce <= 1) {
                0f
            } else {
                ((vnitrniSirkaDp - kartaSirkaDp * pocetVRuce) / (pocetVRuce - 1).toFloat())
                    .coerceIn(minRozestupDp, maxRozestupDp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = maxWidth)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (naSirku) 12.dp else 16.dp,
                        bottom = if (naSirku) 4.dp else 6.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(rozestupDp.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                ruka.forEachIndexed { idx, karta ->
                    val odchylka = idx - stredIdx
                    val uhelVejire = if (pocetVRuce >= 4) (odchylka * 1.4f).coerceIn(-8f, 8f) else 0f
                    val poklesOblouku = if (pocetVRuce >= 4) abs(odchylka) * 1.8f else 0f
                    val jeVybrana = karta in vybraneDoTalonu
                    val zVrstva = if (jeVybrana) 40f else idx.toFloat()

                    val stitek = when (typHry) {
                        TypHry.SEDMA -> {
                            val s = stavSedma
                            if (aktivniKontrola && s != null && s.aktualniStych.isNotEmpty() && SedmaEngine.kartaPrebijiStych(s, karta)) {
                                "BERE"
                            } else null
                        }
                        TypHry.MARIAS -> {
                            val s = stavMarias
                            if (s != null && !s.jeBetlNeboDurch && (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL)) {
                                val druha = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
                                if (ruka.any { it.barva == karta.barva && it.hodnota == druha }) {
                                    if (karta.barva == s.trumf) "HL 40" else "HL 20"
                                } else null
                            } else null
                        }
                        TypHry.PRSI -> null
                    }

                    KartaLiceComposable(
                        karta = karta,
                        modifier = Modifier
                            .zIndex(zVrstva)
                            .graphicsLayer {
                                rotationZ = uhelVejire
                                translationY = poklesOblouku
                            },
                        sirka = kartaSirkaDp.dp,
                        jePovolena = karta in povolene,
                        aktivniKontrolaTahu = aktivniKontrola,
                        jeVybrana = jeVybrana,
                        stitekNavic = stitek,
                        onClick = if (aktivniKontrola && karta in povolene) {
                            {
                                when (typHry) {
                                    TypHry.PRSI -> onKlikKartaPrsi(karta)
                                    TypHry.SEDMA -> onKlikKartaSedma(karta)
                                    TypHry.MARIAS -> onKlikKartaMarias(karta)
                                }
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun MaleAkcniTlacitko(
    text: String,
    barva: Color,
    barvaTextu: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(barva)
            .clickable {
                jemnaHaptika()
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = barvaTextu,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PanelVyuctovaniPartie(
    vysledek: UiVyuctovani,
    jeOnline: Boolean,
    onlineStul: OnlineStul?,
    offlineKonfig: KonfiguraceOfflineHry?,
    cisloOfflineRozdani: Int,
    soucetBoduOffline: List<Int>,
    aktualniMesecTerezianu: Int,
    mojeDeviceId: String,
    onDalsiRozdani: () -> Unit,
    onZpetDoHerny: () -> Unit
) {
    val jeTurnajHotovy = if (onlineStul != null && onlineStul.turnajId.isNotEmpty()) {
        onlineStul.cisloRozdani >= onlineStul.celkemRozdani
    } else if (offlineKonfig?.jeOfflinePohar == true) {
        cisloOfflineRozdani >= offlineKonfig.celkemRozdani
    } else {
        false
    }

    val dostHracuUStolu = !jeOnline || (onlineStul != null && onlineStul.hraci.size >= onlineStul.maxHracu)
    val muzeSpustitDalsi = !jeTurnajHotovy && dostHracuUStolu &&
        (!jeOnline || onlineStul?.hostDeviceId == mojeDeviceId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD904150C))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF103821))
                .border(2.dp, Color(0xFFD4AF37), RoundedCornerShape(18.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = vysledek.nadpis,
                color = Color(0xFFFFD54F),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = vysledek.podnadpis,
                color = Color(0xFFB9F6CA),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            for (polozka in vysledek.polozky) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (polozka.jeZvyraznena) Color(0xFF1A5232) else Color(0xFF0C2B19))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = polozka.popis,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = polozka.hodnotaText,
                        color = if (polozka.jeZvyraznena) Color(0xFFFFE082) else Color(0xFFE0E0E0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Vyúčtování tereziánů:",
                color = Color(0xFFFFF59D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            for (h in vysledek.hraci) {
                Text(
                    text = "${h.jmenoHrace}: ${formatujTereziany(h.zmenaTerezianu, seZnaminkem = true)}",
                    color = if (h.zmenaTerezianu >= 0) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF184A2C))
                    .border(1.dp, Color(0x88FFD54F), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tvůj měšec celkem: ${formatujTereziany(aktualniMesecTerezianu)}",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (offlineKonfig?.jeOfflinePohar == true) {
                Spacer(modifier = Modifier.height(8.dp))
                val poradiPohar = vysledek.hraci.mapIndexed { idx, hr ->
                    hr.jmenoHrace to (soucetBoduOffline.getOrElse(idx) { 0 })
                }.sortedByDescending { it.second }
                Text(
                    text = "Průběžné pořadí poháru (${cisloOfflineRozdani}/${offlineKonfig.celkemRozdani}): " +
                        poradiPohar.joinToString(", ") { "${it.first} (${it.second} b.)" },
                    color = Color(0xFFFFE082),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (jeOnline && !jeTurnajHotovy && !muzeSpustitDalsi) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Čekání, zda zakladatel stolu spustí další rozdání…",
                    color = Color(0xFFB9F6CA),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (muzeSpustitDalsi) {
                    Button(
                        onClick = {
                            jemnaHaptika()
                            onDalsiRozdani()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text(
                            text = "Další rozdání",
                            color = Color(0xFF04210E),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = {
                        jemnaHaptika()
                        onZpetDoHerny()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text(
                        text = if (jeTurnajHotovy) "Zpět k turnaji" else "Zpět do herny",
                        color = Color(0xFF072312),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
