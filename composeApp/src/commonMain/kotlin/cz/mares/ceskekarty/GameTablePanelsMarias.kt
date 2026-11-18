package cz.mares.ceskekarty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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

@Composable
private fun VykresliStychMariase(
    kartyNaPlose: List<TahStychuMariase>,
    jeNahledSebraneho: Boolean,
    vitezUzavreneho: Int,
    hracNaVynosu: String,
    sirkaKarty: Dp
) {
    if (kartyNaPlose.isEmpty()) {
        Box(
            modifier = Modifier
                .size(width = sirkaKarty * 1.85f, height = sirkaKarty * 1.54f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .border(1.2.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Vynáší $hracNaVynosu",
                color = Color(0xDDFFF8E1),
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            for (polozka in kartyNaPlose) {
                val bereStych = jeNahledSebraneho && polozka.indexHrace == vitezUzavreneho
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = polozka.jmenoHrace,
                        color = if (bereStych) Color(0xFF69F0AE) else Color(0xFFFFF59D),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    KartaLiceComposable(
                        karta = polozka.karta,
                        sirka = sirkaKarty,
                        stitekNavic = if (bereStych) "BERE ŠTYCH" else null
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StredStoluMarias(
    stav: StavMarias,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    talonNadStychem: Boolean = true,
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
                            for (barva in BarvaKarty.entries) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1D5836))
                                        .border(0.8.dp, Color(0x44FFD54F), RoundedCornerShape(8.dp))
                                        .clickable {
                                            jemnaHaptika()
                                            onVolbaTrumfu(barva, false, ZavazekMariase.HRA)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 7.dp)
                                ) {
                                    IkonaBarvyKarty(barva, velikost = 19.dp)
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
                    val karetTalonu = stav.zbytekProForhonta.size.takeIf { it > 0 } ?: 5
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        KartaRubComposable(
                            sirka = sirkaKarty,
                            pocetVBalicku = karetTalonu,
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
                    val povoleneZavazky = remember(stav) { MariasEngine.zjistiPovoleneZavazkyTalonu(stav) }
                    val zvolenyPlan = if (planovanyZavazek in povoleneZavazky) {
                        planovanyZavazek
                    } else {
                        povoleneZavazky.firstOrNull() ?: ZavazekMariase.HRA
                    }
                    LaunchedEffect(zvolenyPlan) {
                        if (zvolenyPlan != planovanyZavazek) {
                            onZmenaPlanovanehoZavazku(zvolenyPlan)
                        }
                    }
                    val drziPravou7 = MariasEngine.maPravouSedmu(stav, vybraneDoTalonu)
                    val drziPrave2Sedmy = MariasEngine.maPraveDveSedmy(stav, vybraneDoTalonu)
                    val hospodskaPravidla = stav.variantaPravidel == VariantaPravidel.HOSPODSKA

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
                        if (!zvolenyPlan.jeBezTrumfu) {
                            Text(
                                text = if (hospodskaPravidla) {
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
                            for (polozkaZavazku in povoleneZavazky) {
                                val jeZvoleny = zvolenyPlan == polozkaZavazku
                                val stitekZavazku = when (polozkaZavazku) {
                                    ZavazekMariase.HRA_A_SEDMA ->
                                        if (drziPravou7 || !hospodskaPravidla) "Hra + 7" else "Hra + fal. 7"
                                    ZavazekMariase.HRA_A_DVE_SEDMY ->
                                        if (drziPrave2Sedmy || !hospodskaPravidla) "Hra + 2×7" else "Hra + fal. 2×7"
                                    ZavazekMariase.STO -> "Stovka"
                                    ZavazekMariase.STO_A_SEDMA ->
                                        if (drziPravou7 || !hospodskaPravidla) "100 + 7" else "100 + fal. 7"
                                    ZavazekMariase.STO_A_DVE_SEDMY ->
                                        if (drziPrave2Sedmy || !hospodskaPravidla) "100 + 2×7" else "100 + fal. 2×7"
                                    else -> polozkaZavazku.nazev
                                }
                                MaleAkcniTlacitko(
                                    text = stitekZavazku,
                                    barva = if (jeZvoleny) Color(0xFFD4AF37) else Color(0xFF205536),
                                    barvaTextu = if (jeZvoleny) Color(0xFF072312) else Color.White
                                ) {
                                    onZmenaPlanovanehoZavazku(polozkaZavazku)
                                }
                            }
                        }

                        if (hospodskaPravidla && zvolenyPlan.hlasiDveSedmy && !drziPrave2Sedmy) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pozor: nemáš obě sedmy — hlásíš falešné dvě sedmy (blaf)!",
                                color = Color(0xFFFFB74D),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else if (hospodskaPravidla && zvolenyPlan.hlasiSedmu && !drziPravou7) {
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
                                text = "Odložit talon a hlásit ${zvolenyPlan.nazev}",
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
                    val lzeFlekHru = MariasEngine.muzeHracFlekHry(stav, lokalniIndex)
                    val lzeFlekSedmu = MariasEngine.muzeHracFlekSedmy(stav, lokalniIndex)
                    val nazevHra = MariasEngine.nazevStupneFleku(stav.nasobekFlekHra * 2)
                    val nazevSedma = MariasEngine.nazevStupneFleku(stav.nasobekFlekSedma * 2)
                    val cilSedmy = if (stav.zavazek.hlasiDveSedmy) "2×7" else "sedmu"

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
                        if (lzeFlekHru) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    onOdpovedFleku(true, false)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                            ) {
                                Text("$nazevHra na hru!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
                            }
                        }
                        if (lzeFlekSedmu) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    onOdpovedFleku(false, true)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAD1457))
                            ) {
                                Text("$nazevSedma na $cilSedmy!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
                            }
                        }
                        if (lzeFlekHru && lzeFlekSedmu) {
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
                val stychKarty = when {
                    stav.aktualniStych.isNotEmpty() -> stav.aktualniStych
                    zobrazitUzavrenyStych && stav.posledniUzavrenyStych.isNotEmpty() -> stav.posledniUzavrenyStych
                    else -> emptyList()
                }
                val nahledSebraneho = stav.aktualniStych.isEmpty() && stychKarty.isNotEmpty()
                val sirkaTalonu = (sirkaKarty.value * 0.84f).dp
                val karetVTalonu = stav.talon.size.takeIf { it > 0 } ?: 2
                val vynasejiciJmeno = stav.jmenaHracu.getOrElse(stav.naTahu) { "Hráč" }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${stav.cisloStychu}. štych • Forhont ${stav.bodyKaretForhont + stav.hlaskyForhont} : Obrana ${stav.bodyKaretObrana + stav.hlaskyObrana}",
                        color = Color(0xFFFFF59D),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    if (talonNadStychem) {
                        KartaRubComposable(
                            sirka = sirkaTalonu,
                            pocetVBalicku = karetVTalonu,
                            popisek = "TALON"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        VykresliStychMariase(
                            kartyNaPlose = stychKarty,
                            jeNahledSebraneho = nahledSebraneho,
                            vitezUzavreneho = stav.posledniUzavrenyVitez,
                            hracNaVynosu = vynasejiciJmeno,
                            sirkaKarty = sirkaKarty
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            KartaRubComposable(
                                sirka = sirkaTalonu,
                                pocetVBalicku = karetVTalonu,
                                popisek = "TALON"
                            )
                            VykresliStychMariase(
                                kartyNaPlose = stychKarty,
                                jeNahledSebraneho = nahledSebraneho,
                                vitezUzavreneho = stav.posledniUzavrenyVitez,
                                hracNaVynosu = vynasejiciJmeno,
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
    val kartyVRuce: List<Karta>
    val hratelneKarty: Set<Karta>
    val aktivniVyberKarty: Boolean
    val svitiRamecekRuky: Boolean
    val stavovaNapoveda: String

    when (typHry) {
        TypHry.PRSI -> {
            val s = stavPrsi
            kartyVRuce = s?.ruceHracu?.getOrNull(lokalniIndex) ?: emptyList()
            val vybiraBarvu = s != null && !s.jeKonec && s.naTahu == lokalniIndex && s.cekaNaVolbuBarvy
            val hrajeTah = s != null && !s.jeKonec && s.naTahu == lokalniIndex && !s.cekaNaVolbuBarvy
            hratelneKarty = if (hrajeTah && s != null) PrsiEngine.zjistiPovoleneKarty(s, lokalniIndex) else emptySet()
            aktivniVyberKarty = hrajeTah
            svitiRamecekRuky = hrajeTah || vybiraBarvu
            val pocetAktivnichSedem = s?.aktivniSedmyPocet ?: 0
            stavovaNapoveda = when {
                vybiraBarvu -> "Vynesl jsi svrška — zvol novou barvu!"
                !hrajeTah -> "Čekání na tah soupeře…"
                pocetAktivnichSedem > 0 ->
                    "Přebij sedmičkou, nebo lízni ${sklonujKartyAkuzativ(pocetAktivnichSedem * 2)}"
                s?.aktivniEsoStoji == true && s.variantaPravidel == VariantaPravidel.HOSPODSKA ->
                    "Přebij esem či Červenou 7, nebo stůj 1 kolo"
                s?.aktivniEsoStoji == true ->
                    "Aktivní eso — přebij esem, nebo stůj 1 kolo"
                hratelneKarty.isEmpty() -> "Nemáš kartu — klepni na balíček pro líznutí"
                else -> "Jsi na tahu — zahraj zvýrazněnou kartu"
            }
        }
        TypHry.SEDMA -> {
            val s = stavSedma
            kartyVRuce = s?.ruceHracu?.getOrNull(lokalniIndex)?.let { Karta.seradRukuSedma(it) } ?: emptyList()
            val hrajeTah = s != null && !s.jeKonec && s.naTahu == lokalniIndex
            hratelneKarty = if (hrajeTah && s != null) SedmaEngine.zjistiPovoleneKarty(s, lokalniIndex) else emptySet()
            aktivniVyberKarty = hrajeTah
            svitiRamecekRuky = hrajeTah
            stavovaNapoveda = when {
                !hrajeTah -> "Čekání na tah u stolu…"
                s?.muzePustitStych == true && s.prozatimBereHrac == lokalniIndex ->
                    "Držíš štych — přilož další, nebo štych vezmi"
                s?.muzePustitStych == true ->
                    "Soupeř přebil — přebij zpět, nebo štych pusť"
                else -> "Jsi na tahu • štítek BERE přebíjí štych"
            }
        }
        TypHry.MARIAS -> {
            val s = stavMarias
            kartyVRuce = s?.ruceHracu?.getOrNull(lokalniIndex) ?: emptyList()
            val fazeTrumfu = s != null && s.faze == FazeMariase.VOLBA_TRUMFU && s.naTahu == lokalniIndex
            val fazeTalonu = s != null && s.faze == FazeMariase.ODKLADANI_TALONU && s.naTahu == lokalniIndex
            val fazeFleku = s != null && s.faze == FazeMariase.FLEKOVANI && s.naTahu == lokalniIndex
            val fazeSehravky = s != null && s.faze == FazeMariase.SEHRAVKA && s.naTahu == lokalniIndex
            hratelneKarty = when {
                fazeTalonu && s != null -> MariasEngine.zjistiKartyPovoleneDoTalonu(s, planovanyZavazek)
                fazeSehravky && s != null -> MariasEngine.zjistiPovoleneKarty(s, lokalniIndex)
                else -> emptySet()
            }
            aktivniVyberKarty = fazeTalonu || fazeSehravky
            svitiRamecekRuky = fazeTrumfu || fazeTalonu || fazeFleku || fazeSehravky
            stavovaNapoveda = when {
                fazeTrumfu -> "Prohlédni si 7 karet a nahoře zvol trumf"
                fazeTalonu -> "Vyber 2 povolené karty do talonu"
                fazeFleku -> "Zvol nahoře Dobrý nebo Flek"
                !fazeSehravky -> "Sleduj stůl"
                s?.aktualniStych?.isEmpty() == true -> "Vynášíš do štychu"
                else -> {
                    val barvaVynosu = s?.aktualniStych?.firstOrNull()?.karta?.barva
                    val drziBarvu = barvaVynosu != null && kartyVRuce.any { it.barva == barvaVynosu }
                    val drziTrumf = s?.trumf != null && !s.jeBetlNeboDurch && kartyVRuce.any { it.barva == s.trumf }
                    when {
                        drziBarvu -> "Povinnost přiznat (a přebít) ${barvaVynosu?.ceskyNazev}"
                        drziTrumf -> "Nemáš barvu — trumfuj (${s?.trumf?.ceskyNazev})!"
                        else -> "Nemáš barvu ani trumf — volný odhoz"
                    }
                }
            }
        }
    }

    val spojeniLokalniho = onlineStul?.hraci?.getOrNull(lokalniIndex)?.let {
        onlineStul.infoPripojeni(it.deviceId, nyniMs)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xB8062011))
            .border(
                width = if (svitiRamecekRuky) 1.6.dp else 1.dp,
                color = if (svitiRamecekRuky) Color(0xFF69F0AE) else Color(0x55E6C15A),
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
            if (spojeniLokalniho != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (spojeniLokalniho.stav == StavPripojeniHrace.ONLINE_ZELENA) {
                                Color(0xFF00E676)
                            } else {
                                Color(0xFFFFCA28)
                            }
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = stavovaNapoveda,
                color = if (svitiRamecekRuky) Color(0xFF69F0AE) else Color(0xFFFFF8E1),
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
            val celkovaSirkaPanelu = maxWidth
            val pocetKaret = kartyVRuce.size
            val stredVejire = (pocetKaret - 1) / 2f
            val vnitrniProstorDp = (celkovaSirkaPanelu.value - 24f).coerceAtLeast(180f)
            val sirkaJedneDp = if (pocetKaret in 1..4) {
                minOf(sirkaKarty.value, (vnitrniProstorDp - 12f) / 4f).coerceAtLeast(52f)
            } else {
                sirkaKarty.value
            }
            val nejmensiMezeraDp = -(sirkaJedneDp - 30f)
            val nejvetsiMezeraDp = 7f
            val mezeraDp = if (pocetKaret <= 1) {
                0f
            } else {
                ((vnitrniProstorDp - sirkaJedneDp * pocetKaret) / (pocetKaret - 1).toFloat())
                    .coerceIn(nejmensiMezeraDp, nejvetsiMezeraDp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = celkovaSirkaPanelu)
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = if (naSirku) 12.dp else 16.dp,
                        bottom = if (naSirku) 4.dp else 6.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(mezeraDp.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                kartyVRuce.forEachIndexed { poradi, karta ->
                    val posunOdStredu = poradi - stredVejire
                    val natoceni = if (pocetKaret >= 4) (posunOdStredu * 1.4f).coerceIn(-8f, 8f) else 0f
                    val poklesY = if (pocetKaret >= 4) abs(posunOdStredu) * 1.8f else 0f
                    val vybranaDoTalonu = karta in vybraneDoTalonu
                    val vrstvaZ = if (vybranaDoTalonu) 40f else poradi.toFloat()

                    val horniStitek = when (typHry) {
                        TypHry.SEDMA -> {
                            val s = stavSedma
                            if (aktivniVyberKarty && s != null && s.aktualniStych.isNotEmpty() && SedmaEngine.kartaPrebijiStych(s, karta)) {
                                "BERE"
                            } else null
                        }
                        TypHry.MARIAS -> {
                            val s = stavMarias
                            if (s != null && !s.jeBetlNeboDurch && (karta.hodnota == HodnotaKarty.SVRSEK || karta.hodnota == HodnotaKarty.KRAL)) {
                                val parovaHodnota = if (karta.hodnota == HodnotaKarty.SVRSEK) HodnotaKarty.KRAL else HodnotaKarty.SVRSEK
                                if (kartyVRuce.any { it.barva == karta.barva && it.hodnota == parovaHodnota }) {
                                    if (karta.barva == s.trumf) "HL 40" else "HL 20"
                                } else null
                            } else null
                        }
                        TypHry.PRSI -> null
                    }

                    KartaLiceComposable(
                        karta = karta,
                        modifier = Modifier
                            .zIndex(vrstvaZ)
                            .graphicsLayer {
                                rotationZ = natoceni
                                translationY = poklesY
                            },
                        sirka = sirkaJedneDp.dp,
                        jePovolena = karta in hratelneKarty,
                        aktivniKontrolaTahu = aktivniVyberKarty,
                        jeVybrana = vybranaDoTalonu,
                        stitekNavic = horniStitek,
                        onClick = if (aktivniVyberKarty && karta in hratelneKarty) {
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
private fun RadekPolozkyVyuctovani(polozka: UiPolozkaVyuctovani) {
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp)
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
    val jeDuelSerie = (jeOnlineTurnaj && onlineStul != null && onlineStul.typHry != TypHry.MARIAS) ||
        (jeOfflinePohar && offlineKonfig?.typHry != TypHry.MARIAS)
    val jeTurnajHotovy = when {
        jeOnlineTurnaj && onlineStul != null -> jeOnlineTurnajovaSerieHotova(onlineStul, vysledek)
        jeOfflinePohar -> jeOfflinePoharHotovy(offlineKonfig, cisloOfflineRozdani, soucetBoduOffline)
        else -> false
    }
    val plnyStul = !jeOnline || (onlineStul != null && onlineStul.hraci.size >= onlineStul.maxHracu)
    val spravceStoluId = onlineStul?.hraci?.firstOrNull {
        it.deviceId == onlineStul.hostDeviceId && !jeBotDeviceId(it.deviceId)
    }?.deviceId ?: onlineStul?.hraci?.firstOrNull { !jeBotDeviceId(it.deviceId) }?.deviceId
    val muzeSpustitDalsi = !jeTurnajHotovy && plnyStul && (!jeOnline || spravceStoluId == mojeDeviceId)

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
                RadekPolozkyVyuctovani(polozka)
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
            for (hrac in vysledek.hraci) {
                Text(
                    text = "${hrac.jmenoHrace}: ${formatujTereziany(hrac.zmenaTerezianu, seZnaminkem = true)}",
                    color = if (hrac.zmenaTerezianu >= 0) Color(0xFF69F0AE) else Color(0xFFFF8A80),
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
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tvůj měšec celkem: ${formatujTereziany(aktualniMesecTerezianu)}",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val souhrnSerie: String? = when {
                jeOnlineTurnaj && onlineStul != null -> {
                    val noveSkore = vypoctiNoveSkoreSerieOnline(onlineStul, vysledek)
                    if (onlineStul.typHry != TypHry.MARIAS) {
                        val jm0 = onlineStul.hraci.getOrNull(0)?.prezdivka
                            ?: vysledek.hraci.firstOrNull { it.indexHrace == 0 }?.jmenoHrace ?: "Hráč 1"
                        val jm1 = onlineStul.hraci.getOrNull(1)?.prezdivka
                            ?: vysledek.hraci.firstOrNull { it.indexHrace == 1 }?.jmenoHrace ?: "Hráč 2"
                        val s0 = noveSkore.getOrElse(0) { 0 }
                        val s1 = noveSkore.getOrElse(1) { 0 }
                        if (jeTurnajHotovy) {
                            val vitez = if (s0 >= s1) jm0 else jm1
                            "🏆 Vítěz série a postupující: $vitez ($s0 : $s1 na bitvy)"
                        } else {
                            "⚔️ Stav série (na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy): $jm0 $s0 : $s1 $jm1"
                        }
                    } else {
                        val poradi = onlineStul.hraci.mapIndexed { idx, h ->
                            h.prezdivka to noveSkore.getOrElse(idx) { 0 }
                        }.sortedByDescending { it.second }
                        val uvod = if (jeTurnajHotovy) {
                            "🏆 Konečné pořadí stolu (${onlineStul.cisloRozdani}/${onlineStul.celkemRozdani}): "
                        } else {
                            "Pořadí u stolu (${onlineStul.cisloRozdani}/${onlineStul.celkemRozdani}): "
                        }
                        uvod + poradi.joinToString(", ") { "${it.first} (${it.second} b.)" }
                    }
                }
                jeOfflinePohar && offlineKonfig != null -> {
                    if (offlineKonfig.typHry != TypHry.MARIAS) {
                        val jm0 = vysledek.hraci.firstOrNull { it.indexHrace == 0 }?.jmenoHrace ?: "Ty"
                        val jm1 = vysledek.hraci.firstOrNull { it.indexHrace == 1 }?.jmenoHrace ?: "Soupeř"
                        val s0 = soucetBoduOffline.getOrElse(0) { 0 }
                        val s1 = soucetBoduOffline.getOrElse(1) { 0 }
                        if (jeTurnajHotovy) {
                            val vitez = if (s0 >= s1) jm0 else jm1
                            "🏆 Vítěz poháru: $vitez ($s0 : $s1 na bitvy)"
                        } else {
                            "⚔️ Stav poháru (na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy): $jm0 $s0 : $s1 $jm1"
                        }
                    } else {
                        val poradi = vysledek.hraci.map { hr ->
                            hr.jmenoHrace to soucetBoduOffline.getOrElse(hr.indexHrace) { 0 }
                        }.sortedByDescending { it.second }
                        val uvod = if (jeTurnajHotovy) {
                            "🏆 Konečné pořadí poháru (${cisloOfflineRozdani}/${offlineKonfig.celkemRozdani}): "
                        } else {
                            "Průběžné pořadí poháru (${cisloOfflineRozdani}/${offlineKonfig.celkemRozdani}): "
                        }
                        uvod + poradi.joinToString(", ") { "${it.first} (${it.second} b.)" }
                    }
                }
                else -> null
            }

            if (souhrnSerie != null) {
                Spacer(modifier = Modifier.height(8.dp))
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
                        text = souhrnSerie,
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
