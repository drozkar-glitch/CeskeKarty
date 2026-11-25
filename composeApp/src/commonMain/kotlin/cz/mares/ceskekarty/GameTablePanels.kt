package cz.mares.ceskekarty

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun StredStoluPrsi(
    stav: StavPrsi,
    lokalniIndex: Int,
    sirkaKarty: Dp,
    dostupnaSirkaDp: Float,
    naSirku: Boolean,
    onLiznoutNeboStat: () -> Unit,
    onZvolitBarvuSvrskem: (BarvaKarty) -> Unit
) {
    val uzDohralLokalni = lokalniIndex in stav.poradiDohrani && !stav.jeKonec
    val naTahuLokalni = stav.naTahu == lokalniIndex && !stav.jeKonec
    val hratelneKarty = remember(stav, lokalniIndex) {
        PrsiEngine.zjistiPovoleneKarty(stav, lokalniIndex)
    }

    val vynosKarty = stav.odhazovaciBalicek
    val odhozScroll = rememberScrollState()
    LaunchedEffect(vynosKarty.size) {
        if (vynosKarty.size > 1) {
            odhozScroll.animateScrollTo(odhozScroll.maxValue)
        }
    }

    val delitelRady = vynosKarty.size.coerceIn(3, 4).toFloat()
    val maxNaJednu = (dostupnaSirkaDp - 16f - (delitelRady - 1f) * 7f) / delitelRady
    val sirkaVrchni: Dp = min(sirkaKarty.value, maxNaJednu).coerceAtLeast(if (naSirku) 48f else 62f).dp
    val sirkaPredchozi: Dp = (sirkaVrchni.value * 0.90f).dp
    val sirkaLizaciho: Dp = (sirkaKarty.value * if (naSirku) 0.78f else 0.86f).dp

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

        Spacer(modifier = Modifier.height(if (naSirku) 4.dp else 7.dp))

        KartaRubComposable(
            sirka = sirkaLizaciho,
            pocetVBalicku = stav.lizaciBalicek.size,
            popisek = if (stav.aktivniEsoStoji) "STÁT" else "LÍZAT",
            zvyseneZvyrazneni = naTahuLokalni && !stav.cekaNaVolbuBarvy && !uzDohralLokalni && hratelneKarty.isEmpty(),
            onClick = if (naTahuLokalni && !stav.cekaNaVolbuBarvy && !uzDohralLokalni) onLiznoutNeboStat else null
        )

        Spacer(modifier = Modifier.height(if (naSirku) 5.dp else 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(odhozScroll)
                .widthIn(min = dostupnaSirkaDp.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            vynosKarty.forEachIndexed { idx, karta ->
                val jeVrchni = idx == vynosKarty.lastIndex
                val stitek = if (jeVrchni) {
                    stav.zvolenaBarvaSvrskem?.ceskyNazev
                } else {
                    null
                }
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = if (jeVrchni) 1f else 0.78f
                    }
                ) {
                    KartaLiceComposable(
                        karta = karta,
                        sirka = if (jeVrchni) sirkaVrchni else sirkaPredchozi,
                        stitekNavic = stitek
                    )
                }
            }
        }

        if (uzDohralLokalni && !stav.cekaNaVolbuBarvy) {
            val mojeUmisteni = stav.poradiDohrani.indexOf(lokalniIndex) + 1
            Spacer(modifier = Modifier.height(if (naSirku) 5.dp else 8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD915492B))
                    .border(1.dp, Color(0x8869F0AE), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "✓ Máš $mojeUmisteni. místo — ostatní hráči dohrávají o pořadí",
                    color = Color(0xFF69F0AE),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else if (naTahuLokalni && !stav.cekaNaVolbuBarvy) {
            Spacer(modifier = Modifier.height(if (naSirku) 5.dp else 8.dp))
            val popisekAkce = when {
                stav.aktivniEsoStoji -> "Stát 1 kolo (potvrdit eso)"
                stav.aktivniSedmyPocet > 0 -> "Líznout ${sklonujKartyAkuzativ(stav.aktivniSedmyPocet * 2)}"
                else -> "Líznout kartu z balíčku"
            }
            val zvyraznitTlacitko = hratelneKarty.isEmpty()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (zvyraznitTlacitko) Color(0xFFD4AF37) else Color(0xFF14492A))
                    .border(
                        width = 1.2.dp,
                        color = if (zvyraznitTlacitko) Color(0xFFFFF59D) else Color(0x66E6C15A),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        jemnaHaptika()
                        onLiznoutNeboStat()
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = popisekAkce,
                    color = if (zvyraznitTlacitko) Color(0xFF072312) else Color.White,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (naTahuLokalni && stav.cekaNaVolbuBarvy) {
            Spacer(modifier = Modifier.height(6.dp))
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
private fun VrstvaPlamenuKarty(
    poradiKarty: Int,
    stupenOhoreni: Float,
    uhloveKmitani: Float,
    posunJisker: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.graphicsLayer { clip = false }) {
        val sirka = size.width
        val vyska = size.height
        val polomerRohu = (sirka * 0.062f).coerceIn(8f, 16f)
        val saze = stupenOhoreni.coerceIn(0f, 1f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0x24BF360C),
                0.40f to Color.Transparent,
                0.70f to Color(0xAA2D1106).copy(alpha = 0.26f + 0.40f * saze),
                1.0f to Color(0xEE140702).copy(alpha = 0.46f + 0.44f * saze)
            ),
            topLeft = Offset.Zero,
            size = Size(sirka, vyska),
            cornerRadius = CornerRadius(polomerRohu, polomerRohu)
        )

        val zhaveni = 0.72f + 0.28f * sin(uhloveKmitani + poradiKarty * 0.9f)
        drawRoundRect(
            color = Color(0xFFFF6D00).copy(alpha = (0.76f * zhaveni).coerceIn(0.35f, 1f)),
            topLeft = Offset.Zero,
            size = Size(sirka, vyska),
            cornerRadius = CornerRadius(polomerRohu, polomerRohu),
            style = Stroke(width = 2.4.dp.toPx())
        )

        for (jazyk in 0 until 5) {
            val pataX = sirka * (0.12f + jazyk * 0.19f)
            val vlna = sin(uhloveKmitani + poradiKarty * 1.35f + jazyk * 1.15f)
            val vyskaJazyku = vyska * (0.42f + 0.23f * saze + 0.11f * vlna) +
                if (jazyk == 1 || jazyk == 3) vyska * 0.14f else 0f
            val spickaY = (vyska - vyskaJazyku).coerceAtLeast(-vyska * 0.18f)
            val spickaX = pataX + vlna * (sirka * 0.09f)
            val polovinaPaty = sirka * (0.16f + (jazyk % 2) * 0.04f)

            val obrysVnejsi = Path().apply {
                moveTo(pataX - polovinaPaty, vyska * 0.99f)
                quadraticBezierTo(
                    pataX - polovinaPaty * 0.95f + vlna * 4f,
                    vyska - vyskaJazyku * 0.52f,
                    spickaX,
                    spickaY
                )
                quadraticBezierTo(
                    pataX + polovinaPaty * 0.95f + vlna * 4f,
                    vyska - vyskaJazyku * 0.52f,
                    pataX + polovinaPaty,
                    vyska * 0.99f
                )
                close()
            }
            drawPath(
                path = obrysVnejsi,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00FF3D00),
                        Color(0xCCFF3D00),
                        Color(0xEEFF6D00),
                        Color(0xDD3E1305)
                    ),
                    startY = spickaY,
                    endY = vyska
                )
            )

            val jadroVrcholY = spickaY + vyskaJazyku * 0.26f
            val obrysJadra = Path().apply {
                moveTo(pataX - polovinaPaty * 0.56f, vyska * 0.98f)
                quadraticBezierTo(
                    pataX - polovinaPaty * 0.42f + vlna * 3f,
                    vyska - vyskaJazyku * 0.36f,
                    spickaX,
                    jadroVrcholY
                )
                quadraticBezierTo(
                    pataX + polovinaPaty * 0.42f + vlna * 3f,
                    vyska - vyskaJazyku * 0.36f,
                    pataX + polovinaPaty * 0.56f,
                    vyska * 0.98f
                )
                close()
            }
            drawPath(
                path = obrysJadra,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x00FFF59D),
                        Color(0xEEFFD54F),
                        Color(0xFFFFAB00)
                    ),
                    startY = jadroVrcholY,
                    endY = vyska
                )
            )
        }

        for (jiskra in 0 until 6) {
            val draha = (posunJisker + jiskra * 0.17f + poradiKarty * 0.11f) % 1f
            val xJiskry = sirka * (0.16f + ((jiskra * 29 + poradiKarty * 13) % 68) / 100f) +
                sin(draha * 6.28f + jiskra) * (sirka * 0.06f)
            val yJiskry = vyska * (0.85f - draha * 1.02f)
            val viditelnost = (1f - draha).coerceIn(0f, 1f)
            val polomer = (1.5f + (jiskra % 2) * 1.0f).dp.toPx() * (0.5f + 0.5f * viditelnost)
            drawCircle(
                color = if (jiskra % 2 == 0) {
                    Color(0xFFFFF59D).copy(alpha = viditelnost)
                } else {
                    Color(0xFFFF6E40).copy(alpha = viditelnost)
                },
                radius = polomer,
                center = Offset(xJiskry, yJiskry)
            )
        }
    }
}

@Composable
private fun StychSedmaBox(
    vyrezKaret: List<HranaKartaStolu>,
    drzitelStychu: Int,
    jeNahledSebraneho: Boolean,
    jeSpalena: Boolean,
    zakladniSirkaKarty: Dp,
    sirkaKartyVeStychu: Dp,
    mezeraKaret: Dp,
    minSirkaKontejneruDp: Float
) {
    if (vyrezKaret.isEmpty()) {
        Box(
            modifier = Modifier
                .size(width = zakladniSirkaKarty * 1.75f, height = zakladniSirkaKarty * 1.54f)
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
        return
    }

    val stychScroll = rememberScrollState()
    LaunchedEffect(vyrezKaret.size) {
        if (vyrezKaret.size > 4) {
            stychScroll.animateScrollTo(stychScroll.maxValue)
        }
    }

    var kmitaniPlamene = 0f
    var drahaJisker = 0f
    val ohoreni = remember(jeSpalena) { Animatable(if (jeSpalena) 0.18f else 0f) }

    if (jeSpalena) {
        val smycka = rememberInfiniteTransition()
        kmitaniPlamene = smycka.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 580, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        ).value
        drahaJisker = smycka.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 820, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        ).value
        LaunchedEffect(Unit) {
            ohoreni.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1150, easing = LinearEasing)
            )
        }
    }

    val radekModifier = if (minSirkaKontejneruDp > 0f) {
        Modifier
            .fillMaxWidth()
            .horizontalScroll(stychScroll)
            .widthIn(min = minSirkaKontejneruDp.dp)
            .padding(horizontal = 4.dp, vertical = if (jeSpalena) 6.dp else 0.dp)
    } else {
        Modifier
            .horizontalScroll(stychScroll)
            .padding(vertical = if (jeSpalena) 6.dp else 0.dp)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(mezeraKaret, Alignment.CenterHorizontally),
        modifier = radekModifier
    ) {
        vyrezKaret.forEachIndexed { poradi, hrana ->
            val bereTato = hrana.indexHrace == drzitelStychu &&
                hrana == vyrezKaret.lastOrNull { it.indexHrace == drzitelStychu }
            val natoceni = if (jeSpalena) sin(kmitaniPlamene + poradi * 1.4f) * 1.9f else 0f

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hrana.jmenoHrace,
                    color = when {
                        jeSpalena && bereTato -> Color(0xFFFFAB40)
                        bereTato -> Color(0xFF69F0AE)
                        else -> Color(0xFFFFF8E1)
                    },
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (jeSpalena) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Box(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = natoceni
                        clip = false
                    }
                ) {
                    KartaLiceComposable(
                        karta = hrana.karta,
                        sirka = sirkaKartyVeStychu,
                        stitekNavic = when {
                            jeSpalena && bereTato -> "🔥 SPÁLENÁ!"
                            bereTato && jeNahledSebraneho -> "BERE ŠTYCH"
                            bereTato -> "BERE"
                            else -> null
                        }
                    )
                    if (jeSpalena) {
                        VrstvaPlamenuKarty(
                            poradiKarty = poradi,
                            stupenOhoreni = ohoreni.value,
                            uhloveKmitani = kmitaniPlamene,
                            posunJisker = drahaJisker,
                            modifier = Modifier.matchParentSize()
                        )
                    }
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
    talonNadStychem: Boolean = true,
    zobrazitUzavrenyStych: Boolean,
    onPustitNeboVzitStych: () -> Unit,
    onNahlasitSpalenouZRuky: () -> Unit
) {
    val kartyStychu = when {
        stav.aktualniStych.isNotEmpty() -> stav.aktualniStych
        zobrazitUzavrenyStych && stav.posledniUzavrenyStych.isNotEmpty() -> stav.posledniUzavrenyStych
        else -> emptyList()
    }
    val jeSpalena = stav.spalenaTym >= 0
    val jeNahledSebraneho = stav.aktualniStych.isEmpty() && kartyStychu.isNotEmpty()
    val drzitelNaPlose = if (jeNahledSebraneho) stav.posledniUzavrenyVitez else stav.prozatimBereHrac
    val vyrezStychu = kartyStychu

    val pocetVeStychu = vyrezStychu.size
    val mezeraStychuDp = if (pocetVeStychu >= 5) 5f else 7f
    val sirkaVeStychu: Dp = if (talonNadStychem && pocetVeStychu > 0) {
        val delitel = pocetVeStychu.coerceIn(3, 6).toFloat()
        val maxNaJednu = (dostupnaSirkaDp - 16f - (delitel - 1f) * mezeraStychuDp) / delitel
        min(sirkaKarty.value, maxNaJednu).coerceAtLeast(if (naSirku) 46f else 52f).dp
    } else {
        sirkaKarty
    }
    val sirkaTalonu: Dp = (sirkaKarty.value * if (naSirku) 0.78f else 0.88f).dp
    val hodnotaSpaleneZRuky = remember(stav, lokalniIndex) {
        SedmaEngine.zjistiSpalenouVRuce(stav, lokalniIndex)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        if (jeSpalena) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB71C1C), Color(0xFFE65100), Color(0xFFB71C1C))
                        )
                    )
                    .border(1.4.dp, Color(0xFFFFE082), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (stav.spalenaZRuky) {
                        "🔥 SPÁLENÁ Z RUKY — ČTVEŘICE V PLAMENECH! 🔥"
                    } else {
                        "🔥 SPÁLENÁ — ČTYŘI STEJNÉ V PLAMENECH! 🔥"
                    },
                    color = Color(0xFFFFF8E1),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
        }

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
                .background(if (jeSpalena) Color(0xB83E1106) else Color(0x9E061E10))
                .border(
                    width = 1.dp,
                    color = if (jeSpalena) Color(0xAAFF9100) else Color(0x55E6C15A),
                    shape = RoundedCornerShape(9.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(if (naSirku) 4.dp else 7.dp))

        if (talonNadStychem) {
            KartaRubComposable(
                sirka = sirkaTalonu,
                pocetVBalicku = stav.lizaciBalicek.size,
                popisek = "TALON"
            )
            Spacer(modifier = Modifier.height(if (naSirku) 5.dp else 8.dp))
            StychSedmaBox(
                vyrezKaret = vyrezStychu,
                drzitelStychu = drzitelNaPlose,
                jeNahledSebraneho = jeNahledSebraneho,
                jeSpalena = jeSpalena,
                zakladniSirkaKarty = sirkaKarty,
                sirkaKartyVeStychu = sirkaVeStychu,
                mezeraKaret = mezeraStychuDp.dp,
                minSirkaKontejneruDp = dostupnaSirkaDp
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
                    vyrezKaret = vyrezStychu,
                    drzitelStychu = drzitelNaPlose,
                    jeNahledSebraneho = jeNahledSebraneho,
                    jeSpalena = jeSpalena,
                    zakladniSirkaKarty = sirkaKarty,
                    sirkaKartyVeStychu = sirkaVeStychu,
                    mezeraKaret = mezeraStychuDp.dp,
                    minSirkaKontejneruDp = 0f
                )
            }
        }

        if ((stav.muzePustitStych && stav.naTahu == lokalniIndex && !stav.jeKonec) || hodnotaSpaleneZRuky != null) {
            Spacer(modifier = Modifier.height(if (naSirku) 5.dp else 8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hodnotaSpaleneZRuky != null && !stav.jeKonec) {
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
                            text = "🔥 Hlásit spálenou z ruky (4× ${hodnotaSpaleneZRuky.kratkyZnak})!",
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
                    val textTlacitka = if (drzimStychJa) {
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
                            text = textTlacitka,
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
