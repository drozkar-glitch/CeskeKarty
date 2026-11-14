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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlin.math.min

@Composable
internal fun StredStoluPrsi(
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
            lineHeight = 16.sp,
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
        Spacer(modifier = Modifier.height(if (naSirku) 6.dp else 12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (naSirku) 20.dp else 26.dp)
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
            Spacer(modifier = Modifier.height(if (naSirku) 6.dp else 12.dp))
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = textTlacitka,
                    color = if (zvyraznitLiznuti) Color(0xFF072312) else Color.White,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
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
                            lineHeight = 14.sp,
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
private fun StychSedmaBox(
    zobrazenyVyrez: List<HranaKartaStolu>,
    drzitelNaPlose: Int,
    jeNahledSebraneho: Boolean,
    prazdnaSirkaKarty: Dp,
    sirkaKartyVeStychu: Dp
) {
    if (zobrazenyVyrez.isEmpty()) {
        Box(
            modifier = Modifier
                .size(width = prazdnaSirkaKarty * 1.75f, height = prazdnaSirkaKarty * 1.54f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .border(1.2.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Čeká na výnos",
                color = Color(0xCCFFF8E1),
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            for (hrana in zobrazenyVyrez) {
                val bereTato = hrana.indexHrace == drzitelNaPlose &&
                    hrana == zobrazenyVyrez.lastOrNull { it.indexHrace == drzitelNaPlose }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hrana.jmenoHrace,
                        color = if (bereTato) Color(0xFF69F0AE) else Color(0xFFFFF8E1),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    KartaLiceComposable(
                        karta = hrana.karta,
                        sirka = sirkaKartyVeStychu,
                        stitekNavic = when {
                            bereTato && jeNahledSebraneho -> "BERE ŠTYCH"
                            bereTato -> "BERE"
                            else -> null
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun StredStoluSedma(
    stav: StavSedma,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    dostupnaSirkaDp: Float,
    naSirku: Boolean,
    talonNadStychem: Boolean,
    zobrazitUzavrenyStych: Boolean,
    onPustitNeboVzitStych: () -> Unit,
    onNahlasitSpalenouZRuky: () -> Unit
) {
    val stychNaPlose = when {
        stav.aktualniStych.isNotEmpty() -> stav.aktualniStych
        zobrazitUzavrenyStych && stav.posledniUzavrenyStych.isNotEmpty() -> stav.posledniUzavrenyStych
        else -> emptyList()
    }
    val jeNahledSebraneho = stav.aktualniStych.isEmpty() && stychNaPlose.isNotEmpty()
    val drzitelNaPlose = if (jeNahledSebraneho) stav.posledniUzavrenyVitez else stav.prozatimBereHrac
    val zobrazenyVyrez = stychNaPlose.takeLast(4)

    val sirkaKartyVeStychu: Dp = if (talonNadStychem && zobrazenyVyrez.isNotEmpty()) {
        val maxNaJednu = (dostupnaSirkaDp - 24f) / zobrazenyVyrez.size.coerceAtLeast(3).toFloat()
        min(sirkaKarty.value, maxNaJednu).coerceAtLeast(64f).dp
    } else {
        sirkaKarty
    }
    val sirkaTalonu: Dp = (sirkaKarty.value * if (talonNadStychem) 0.94f else 0.92f).dp
    val spalenaVRuceHodnota = remember(stav, lokalniIndex) {
        SedmaEngine.zjistiSpalenouVRuce(stav, lokalniIndex)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stav.posledniHlaska,
            color = Color(0xFFFFF8E1),
            fontSize = 12.sp,
            lineHeight = 16.sp,
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
        Spacer(modifier = Modifier.height(if (naSirku) 5.dp else 8.dp))

        if (talonNadStychem) {
            KartaRubComposable(
                sirka = sirkaTalonu,
                pocetVBalicku = stav.lizaciBalicek.size,
                popisek = "TALON"
            )
            Spacer(modifier = Modifier.height(8.dp))
            StychSedmaBox(
                zobrazenyVyrez = zobrazenyVyrez,
                drzitelNaPlose = drzitelNaPlose,
                jeNahledSebraneho = jeNahledSebraneho,
                prazdnaSirkaKarty = sirkaKarty,
                sirkaKartyVeStychu = sirkaKartyVeStychu
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KartaRubComposable(
                    sirka = sirkaTalonu,
                    pocetVBalicku = stav.lizaciBalicek.size,
                    popisek = "TALON"
                )
                StychSedmaBox(
                    zobrazenyVyrez = zobrazenyVyrez,
                    drzitelNaPlose = drzitelNaPlose,
                    jeNahledSebraneho = jeNahledSebraneho,
                    prazdnaSirkaKarty = sirkaKarty,
                    sirkaKartyVeStychu = sirkaKartyVeStychu
                )
            }
        }

        if ((stav.muzePustitStych && stav.naTahu == lokalniIndex && !stav.jeKonec) || spalenaVRuceHodnota != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (spalenaVRuceHodnota != null && !stav.jeKonec) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFD84315))
                            .border(1.2.dp, Color(0xFFFFE082), RoundedCornerShape(10.dp))
                            .clickable {
                                silnaHaptika()
                                onNahlasitSpalenouZRuky()
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "🔥 Hlásit spálenou z ruky (4× ${spalenaVRuceHodnota.kratkyZnak})!",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (stav.muzePustitStych && stav.naTahu == lokalniIndex && !stav.jeKonec) {
                    val drzimStychJa = stav.prozatimBereHrac == lokalniIndex
                    val bodyVeStychu = stav.aktualniStych.sumOf { it.karta.hodnota.bodyMariasSedma }
                    val popisekTlacitka = if (drzimStychJa) {
                        "Vzít štych (${bodyVeStychu} b.)"
                    } else {
                        "Pustit štych soupeři"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (drzimStychJa) Color(0xFF00C853) else Color(0xFFD4AF37))
                            .clickable {
                                jemnaHaptika()
                                onPustitNeboVzitStych()
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = popisekTlacitka,
                            color = Color(0xFF062111),
                            fontWeight = FontWeight.ExtraBold,
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
}

@Composable
private fun StychMariasBox(
    stychNaPlose: List<TahStychuMariase>,
    jeNahledSebraneho: Boolean,
    posledniUzavrenyVitez: Int,
    jmenoNaTahu: String,
    sirkaKarty: Dp
) {
    if (stychNaPlose.isEmpty()) {
        Box(
            modifier = Modifier
                .size(width = sirkaKarty * 1.85f, height = sirkaKarty * 1.54f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .border(1.2.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Vynáší $jmenoNaTahu",
                color = Color(0xDDFFF8E1),
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (hrana in stychNaPlose) {
                val bereTento = jeNahledSebraneho && hrana.indexHrace == posledniUzavrenyVitez
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = hrana.jmenoHrace,
                        color = if (bereTento) Color(0xFF69F0AE) else Color(0xFFFFF59D),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    KartaLiceComposable(
                        karta = hrana.karta,
                        sirka = sirkaKarty,
                        stitekNavic = if (bereTento) "BERE ŠTYCH" else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StredStoluMarias(
    stav: StavMarias,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    talonNadStychem: Boolean,
    zobrazitUzavrenyStych: Boolean,
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
            lineHeight = 16.sp,
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
                            lineHeight = 16.sp,
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
                                        lineHeight = 14.sp,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KartaRubComposable(
                            sirka = sirkaKarty,
                            pocetVBalicku = stav.zbytekProForhonta.size.takeIf { it > 0 } ?: 5,
                            popisek = "TALON"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Čekání, než ${stav.jmenaHracu[stav.indexForhonta]} zvolí trumf…",
                            color = Color(0xFFB9F6CA),
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            FazeMariase.ODKLADANI_TALONU -> {
                if (stav.naTahu == lokalniIndex) {
                    val moznosti = remember(stav) { MariasEngine.zjistiPovoleneZavazkyTalonu(stav) }
                    val aktualniPlan = if (planovanyZavazek in moznosti) {
                        planovanyZavazek
                    } else {
                        moznosti.firstOrNull() ?: ZavazekMariase.HRA
                    }
                    LaunchedEffect(aktualniPlan) {
                        if (aktualniPlan != planovanyZavazek) {
                            onZmenaPlanovanehoZavazku(aktualniPlan)
                        }
                    }

                    val maPravou7 = MariasEngine.maPravouSedmu(stav, vybraneDoTalonu)
                    val maPrave2Sedmy = MariasEngine.maPraveDveSedmy(stav, vybraneDoTalonu)
                    val jeHospodska = stav.variantaPravidel == VariantaPravidel.HOSPODSKA

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
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!aktualniPlan.jeBezTrumfu) {
                            Text(
                                text = if (jeHospodska) {
                                    "Esa a desítky jsou chráněné • Hospodská: lze hlásit i falešnou 7."
                                } else {
                                    "Esa a desítky jsou chráněné • Svazová: hlásit lze jen pravou 7."
                                },
                                color = Color(0xFFA5D6A7),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (moznost in moznosti) {
                                val aktivni = aktualniPlan == moznost
                                val popisek = when (moznost) {
                                    ZavazekMariase.HRA_A_SEDMA -> if (maPravou7 || !jeHospodska) "Hra + 7" else "Hra + fal. 7"
                                    ZavazekMariase.HRA_A_DVE_SEDMY -> if (maPrave2Sedmy || !jeHospodska) "Hra + 2×7" else "Hra + fal. 2×7"
                                    ZavazekMariase.STO -> "Stovka"
                                    ZavazekMariase.STO_A_SEDMA -> if (maPravou7 || !jeHospodska) "100 + 7" else "100 + fal. 7"
                                    ZavazekMariase.STO_A_DVE_SEDMY -> if (maPrave2Sedmy || !jeHospodska) "100 + 2×7" else "100 + fal. 2×7"
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

                        if (jeHospodska && aktualniPlan.hlasiDveSedmy && !maPrave2Sedmy) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pozor: nemáš obě sedmy — hlásíš falešné dvě sedmy (blaf)!",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else if (jeHospodska && aktualniPlan.hlasiSedmu && !maPravou7) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pozor: nemáš trumfovou sedmu — hlásíš falešnou sedmu (blaf)!",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
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
                                text = "Odložit talon a hlásit ${aktualniPlan.nazev}",
                                color = Color(0xFF05200E),
                                fontSize = 12.sp,
                                lineHeight = 15.sp,
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
                        fontSize = 13.sp,
                        lineHeight = 17.sp
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
                            Text("Dobrý", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
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
                                Text("$stupenHra na hru!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
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
                                Text("$stupenSedma na $slovoSedma!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
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
                                Text("Zvednout obojí!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Vyjadřuje se ${stav.jmenaHracu[stav.naTahu]} (Dobrý / Flek)…",
                        color = Color(0xFFB9F6CA),
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            FazeMariase.SEHRAVKA, FazeMariase.KONEC -> {
                val stychNaPlose = when {
                    stav.aktualniStych.isNotEmpty() -> stav.aktualniStych
                    zobrazitUzavrenyStych && stav.posledniUzavrenyStych.isNotEmpty() -> stav.posledniUzavrenyStych
                    else -> emptyList()
                }
                val jeNahledSebraneho = stav.aktualniStych.isEmpty() && stychNaPlose.isNotEmpty()
                val sirkaTalonuMarias = (sirkaKarty.value * 0.86f).dp
                val pocetVTalonu = stav.talon.size.takeIf { it > 0 } ?: 2
                val jmenoNaTahu = stav.jmenaHracu.getOrElse(stav.naTahu) { "Hráč" }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stav.cisloStychu}. štych • Forhont ${stav.bodyKaretForhont + stav.hlaskyForhont} : Obrana ${stav.bodyKaretObrana + stav.hlaskyObrana}",
                        color = Color(0xFFFFF59D),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (talonNadStychem) {
                        KartaRubComposable(
                            sirka = sirkaTalonuMarias,
                            pocetVBalicku = pocetVTalonu,
                            popisek = "TALON"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StychMariasBox(
                            stychNaPlose = stychNaPlose,
                            jeNahledSebraneho = jeNahledSebraneho,
                            posledniUzavrenyVitez = stav.posledniUzavrenyVitez,
                            jmenoNaTahu = jmenoNaTahu,
                            sirkaKarty = sirkaKarty
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            KartaRubComposable(
                                sirka = sirkaTalonuMarias,
                                pocetVBalicku = pocetVTalonu,
                                popisek = "TALON"
                            )
                            StychMariasBox(
                                stychNaPlose = stychNaPlose,
                                jeNahledSebraneho = jeNahledSebraneho,
                                posledniUzavrenyVitez = stav.posledniUzavrenyVitez,
                                jmenoNaTahu = jmenoNaTahu,
                                sirkaKarty = sirkaKarty
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SpodniRukaHrace(
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
                voliBarvuSvrskem -> "Vynesl jsi svrška — zvol novou barvu!"
                !mujTah -> "Čekáš na tah soupeře"
                s?.aktivniSedmyPocet ?: 0 > 0 ->
                    "Přebij sedmičkou, nebo lízni ${sklonujKartyAkuzativ((s?.aktivniSedmyPocet ?: 1) * 2)}"
                s?.aktivniEsoStoji == true && s.variantaPravidel == VariantaPravidel.HOSPODSKA ->
                    "Přebij esem či Červenou 7, nebo stůj 1 kolo"
                s?.aktivniEsoStoji == true ->
                    "Aktivní eso — přebij esem, nebo stůj 1 kolo"
                povolene.isEmpty() -> "Nemáš kartu — klepni na balíček pro líznutí"
                else -> "Jsi na tahu — zahraj zvýrazněnou kartu"
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
                s?.muzePustitStych == true && s.prozatimBereHrac == lokalniIndex ->
                    "Držíš štych — přilož další, nebo štych vezmi"
                s?.muzePustitStych == true ->
                    "Soupeř přebil — přebij zpět, nebo štych pusť"
                else -> "Jsi na tahu — štítek BERE přebíjí štych"
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
                voliTrumf -> "Prohlédni si 7 karet a nahoře zvol trumf"
                odkladaTalon -> "Vyber 2 povolené karty do talonu"
                flekuje -> "Zvol nahoře Dobrý nebo Flek"
                !hrajeStych -> "Sleduj stůl"
                s?.aktualniStych?.isEmpty() == true -> "Vynášíš do štychu"
                else -> {
                    val vynosBarva = s?.aktualniStych?.firstOrNull()?.karta?.barva
                    val maBarvu = vynosBarva != null && ruka.any { it.barva == vynosBarva }
                    val maTrumf = s?.trumf != null && !s.jeBetlNeboDurch && ruka.any { it.barva == s.trumf }
                    when {
                        maBarvu -> "Povinnost přiznat (a přebít) ${vynosBarva?.ceskyNazev}"
                        maTrumf -> "Nemáš barvu — trumfuj (${s?.trumf?.ceskyNazev})!"
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
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sirkaPanelu = maxWidth
            val pocetVRuce = ruka.size
            val stredIdx = (pocetVRuce - 1) / 2f

            val vnitrniSirkaDp = (sirkaPanelu.value - 24f).coerceAtLeast(180f)
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
                    .widthIn(min = sirkaPanelu)
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
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun PanelVyuctovaniPartie(
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
    val jeOnlineTurnaj = onlineStul != null && onlineStul.turnajId.isNotEmpty()
    val jeOfflinePohar = !jeOnline && offlineKonfig?.jeOfflinePohar == true
    val jeDuelSerie = (jeOnlineTurnaj && onlineStul!!.typHry != TypHry.MARIAS) ||
        (jeOfflinePohar && offlineKonfig?.typHry != TypHry.MARIAS)

    val jeTurnajHotovy = when {
        jeOnlineTurnaj -> jeOnlineTurnajovaSerieHotova(onlineStul!!, vysledek)
        jeOfflinePohar -> jeOfflinePoharHotovy(offlineKonfig, cisloOfflineRozdani, soucetBoduOffline)
        else -> false
    }

    val dostHracuUStolu = !jeOnline || (onlineStul != null && onlineStul.hraci.size >= onlineStul.maxHracu)
    val odpovednyHostId = onlineStul?.hraci?.firstOrNull {
        it.deviceId == onlineStul.hostDeviceId && !jeBotDeviceId(it.deviceId)
    }?.deviceId ?: onlineStul?.hraci?.firstOrNull { !jeBotDeviceId(it.deviceId) }?.deviceId

    val muzeSpustitDalsi = !jeTurnajHotovy && dostHracuUStolu &&
        (!jeOnline || odpovednyHostId == mojeDeviceId)

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
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = vysledek.podnadpis,
                color = Color(0xFFB9F6CA),
                fontSize = 12.sp,
                lineHeight = 16.sp,
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
                        lineHeight = 16.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = polozka.hodnotaText,
                        color = if (polozka.jeZvyraznena) Color(0xFFFFE082) else Color(0xFFE0E0E0),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
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
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            for (h in vysledek.hraci) {
                Text(
                    text = "${h.jmenoHrace}: ${formatujTereziany(h.zmenaTerezianu, seZnaminkem = true)}",
                    color = if (h.zmenaTerezianu >= 0) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
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
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (jeOnlineTurnaj && onlineStul != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val noveSkore = vypoctiNoveSkoreSerieOnline(onlineStul, vysledek)
                val textStavuSerie = if (onlineStul.typHry != TypHry.MARIAS) {
                    val jm0 = onlineStul.hraci.getOrNull(0)?.prezdivka
                        ?: vysledek.hraci.firstOrNull { it.indexHrace == 0 }?.jmenoHrace ?: "Hráč 1"
                    val jm1 = onlineStul.hraci.getOrNull(1)?.prezdivka
                        ?: vysledek.hraci.firstOrNull { it.indexHrace == 1 }?.jmenoHrace ?: "Hráč 2"
                    val s0 = noveSkore.getOrElse(0) { 0 }
                    val s1 = noveSkore.getOrElse(1) { 0 }
                    if (jeTurnajHotovy) {
                        val vitezSerie = if (s0 >= s1) jm0 else jm1
                        "🏆 Vítěz série a postupující: $vitezSerie ($s0 : $s1 na bitvy)"
                    } else {
                        "⚔️ Stav série (na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy): $jm0 $s0 : $s1 $jm1"
                    }
                } else {
                    val poradiTurnaj = onlineStul.hraci.mapIndexed { idx, h ->
                        h.prezdivka to noveSkore.getOrElse(idx) { 0 }
                    }.sortedByDescending { it.second }
                    val prefix = if (jeTurnajHotovy) {
                        "🏆 Konečné pořadí stolu (${onlineStul.cisloRozdani}/${onlineStul.celkemRozdani}): "
                    } else {
                        "Pořadí u stolu (${onlineStul.cisloRozdani}/${onlineStul.celkemRozdani}): "
                    }
                    prefix + poradiTurnaj.joinToString(", ") { "${it.first} (${it.second} b.)" }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF144026))
                        .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = textStavuSerie,
                        color = Color(0xFFFFE082),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (jeOfflinePohar && offlineKonfig != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val textPohar = if (offlineKonfig.typHry != TypHry.MARIAS) {
                    val jm0 = vysledek.hraci.firstOrNull { it.indexHrace == 0 }?.jmenoHrace ?: "Ty"
                    val jm1 = vysledek.hraci.firstOrNull { it.indexHrace == 1 }?.jmenoHrace ?: "Soupeř"
                    val s0 = soucetBoduOffline.getOrElse(0) { 0 }
                    val s1 = soucetBoduOffline.getOrElse(1) { 0 }
                    if (jeTurnajHotovy) {
                        val vitezPohar = if (s0 >= s1) jm0 else jm1
                        "🏆 Vítěz poháru: $vitezPohar ($s0 : $s1 na bitvy)"
                    } else {
                        "⚔️ Stav poháru (na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy): $jm0 $s0 : $s1 $jm1"
                    }
                } else {
                    val poradiPohar = vysledek.hraci.map { hr ->
                        hr.jmenoHrace to (soucetBoduOffline.getOrElse(hr.indexHrace) { 0 })
                    }.sortedByDescending { it.second }
                    val prefix = if (jeTurnajHotovy) {
                        "🏆 Konečné pořadí poháru (${cisloOfflineRozdani}/${offlineKonfig.celkemRozdani}): "
                    } else {
                        "Průběžné pořadí poháru (${cisloOfflineRozdani}/${offlineKonfig.celkemRozdani}): "
                    }
                    prefix + poradiPohar.joinToString(", ") { "${it.first} (${it.second} b.)" }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF144026))
                        .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = textPohar,
                        color = Color(0xFFFFE082),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (jeOnline && !jeTurnajHotovy && !muzeSpustitDalsi) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (jeDuelSerie) {
                        "Čekání, než spoluhráč u stolu spustí další bitvu…"
                    } else {
                        "Čekání, než spoluhráč u stolu spustí další rozdání…"
                    },
                    color = Color(0xFFB9F6CA),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
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
                            text = if (jeDuelSerie) "Další bitva" else "Další rozdání",
                            color = Color(0xFF04210E),
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
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
                    val popisekZpet = when {
                        jeOnlineTurnaj && jeTurnajHotovy -> "Zpět k pavouku"
                        jeTurnajHotovy -> "Zpět k turnaji"
                        else -> "Zpět do herny"
                    }
                    Text(
                        text = popisekZpet,
                        color = Color(0xFF072312),
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
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
