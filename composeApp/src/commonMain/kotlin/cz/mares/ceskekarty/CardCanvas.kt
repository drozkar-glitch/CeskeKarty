package cz.mares.ceskekarty

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val BarvaPapiruKarty = Color(0xFFFAF7EF)
private val BarvaInkoustu = Color(0xFF231C18)
private val BarvaRumelka = Color(0xFFD61F2C)
private val BarvaRumelkaTmava = Color(0xFF9E111B)
private val BarvaChromovaZluta = Color(0xFFF4C41C)
private val BarvaZlutaSvetla = Color(0xFFFFE47A)
private val BarvaSmaragdZelena = Color(0xFF167E39)
private val BarvaZelenaSvetla = Color(0xFF34A853)
private val BarvaKmene = Color(0xFF7D4422)

private val BarvaPovoleneZare = Color(0xFF69F0AE)
private val BarvaVybraneZare = Color(0xFFFFD54F)

fun barvaProZnakKarty(barva: BarvaKarty): Color {
    return when (barva) {
        BarvaKarty.CERVENE -> BarvaRumelka
        BarvaKarty.KULE -> Color(0xFFC85A12)
        BarvaKarty.ZALUDY -> Color(0xFF5D3A1A)
        BarvaKarty.ZELENE -> BarvaSmaragdZelena
    }
}

@Composable
fun ZelenyKaretniStulBox(
    modifier: Modifier = Modifier,
    zobrazitStredovyKruh: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4E2C14), Color(0xFF2B1608), Color(0xFF5A3418))
                )
            )
            .padding(5.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.8.dp, Color(0xFFD1A94B), RoundedCornerShape(18.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val polomer = max(size.width, size.height) * 0.74f
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF268E50),
                        Color(0xFF196E3B),
                        Color(0xFF0F4C28),
                        Color(0xFF082E17)
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.46f),
                    radius = polomer
                )
            )

            val krok = 20f
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0x09FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += krok
            }

            val odsazeni = 10f
            drawRoundRect(
                color = Color(0x40E6C365),
                topLeft = Offset(odsazeni, odsazeni),
                size = Size(size.width - odsazeni * 2f, size.height - odsazeni * 2f),
                cornerRadius = CornerRadius(22f, 22f),
                style = Stroke(width = 1.5f)
            )

            if (zobrazitStredovyKruh) {
                val rKruh = min(size.width, size.height) * 0.27f
                val stred = Offset(size.width * 0.5f, size.height * 0.46f)
                drawCircle(
                    color = Color(0x22E6C365),
                    radius = rKruh,
                    center = stred,
                    style = Stroke(width = 1.8f)
                )
                drawCircle(
                    color = Color(0x14E6C365),
                    radius = rKruh * 0.88f,
                    center = stred,
                    style = Stroke(width = 1.0f)
                )
            }
        }

        content()
    }
}

@Composable
fun IkonaBarvyKarty(
    barva: BarvaKarty,
    velikost: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(velikost)) {
        val r = min(size.width, size.height) * 0.44f
        vykresliZnakBarvy(
            barva = barva,
            stred = Offset(size.width / 2f, size.height / 2f),
            polomer = r
        )
    }
}

private fun radekArchuProBarvu(barva: BarvaKarty): Int = when (barva) {
    BarvaKarty.KULE -> 0
    BarvaKarty.CERVENE -> 1
    BarvaKarty.ZELENE -> 2
    BarvaKarty.ZALUDY -> 3
}

private fun sloupecArchuProHodnotu(hodnota: HodnotaKarty): Int = when (hodnota) {
    HodnotaKarty.R7 -> 0
    HodnotaKarty.R8 -> 1
    HodnotaKarty.R9 -> 2
    HodnotaKarty.R10 -> 3
    HodnotaKarty.SPODEK -> 4
    HodnotaKarty.SVRSEK -> 5
    HodnotaKarty.KRAL -> 6
    HodnotaKarty.ESO -> 7
}

@Composable
fun KartaLiceComposable(
    karta: Karta,
    modifier: Modifier = Modifier,
    sirka: Dp = 74.dp,
    jePovolena: Boolean = true,
    aktivniKontrolaTahu: Boolean = false,
    jeVybrana: Boolean = false,
    stitekNavic: String? = null,
    onClick: (() -> Unit)? = null
) {
    val archBitmap = remember { ziskejArchJednohlavychKaret() }
    val vyska = sirka * 1.58f
    val tvar = RoundedCornerShape(7.dp)

    val cilovyPosunY = when {
        jeVybrana -> (-14).dp
        aktivniKontrolaTahu && jePovolena -> (-9).dp
        else -> 0.dp
    }
    val animovanyPosunY by animateDpAsState(targetValue = cilovyPosunY)

    val okrajBarva = when {
        jeVybrana -> BarvaVybraneZare
        aktivniKontrolaTahu && jePovolena -> BarvaPovoleneZare
        else -> Color(0xFFB8B09E)
    }
    val okrajTloustka = when {
        jeVybrana -> 2.8.dp
        aktivniKontrolaTahu && jePovolena -> 2.4.dp
        else -> 1.0.dp
    }

    val muzeKliknout = onClick != null && (!aktivniKontrolaTahu || jePovolena)

    Box(
        modifier = modifier
            .offset(y = animovanyPosunY)
            .widthIn(max = sirka)
            .heightIn(max = vyska)
            .aspectRatio(1f / 1.58f)
            .shadow(
                elevation = if (jeVybrana || (aktivniKontrolaTahu && jePovolena)) 10.dp else 5.dp,
                shape = tvar
            )
            .clip(tvar)
            .background(BarvaPapiruKarty)
            .border(okrajTloustka, okrajBarva, tvar)
            .then(
                if (muzeKliknout && onClick != null) {
                    Modifier.clickable {
                        jemnaHaptika()
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (archBitmap != null && archBitmap.width >= 80 && archBitmap.height >= 40) {
                val radek = radekArchuProBarvu(karta.barva)
                val sloupec = sloupecArchuProHodnotu(karta.hodnota)
                val sirkaBunky = archBitmap.width / 8
                val vyskaBunky = archBitmap.height / 4

                drawImage(
                    image = archBitmap,
                    srcOffset = IntOffset(
                        x = sloupec * sirkaBunky + 1,
                        y = radek * vyskaBunky + 1
                    ),
                    srcSize = IntSize(
                        width = (sirkaBunky - 2).coerceAtLeast(1),
                        height = (vyskaBunky - 2).coerceAtLeast(1)
                    ),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(
                        width = size.width.roundToInt().coerceAtLeast(1),
                        height = size.height.roundToInt().coerceAtLeast(1)
                    ),
                    filterQuality = FilterQuality.High
                )
            } else {
                val pad = size.width * 0.07f
                drawRect(
                    color = BarvaInkoustu,
                    topLeft = Offset(pad, pad),
                    size = Size(size.width - pad * 2f, size.height - pad * 2f),
                    style = Stroke(width = 1.2f)
                )
                vykresliZnakBarvy(
                    barva = karta.barva,
                    stred = Offset(size.width * 0.5f, size.height * 0.5f),
                    polomer = min(size.width, size.height) * 0.24f
                )
            }
        }

        if (!stitekNavic.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xEB104422))
                    .border(0.8.dp, Color(0xFFFFD54F), RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text(
                    text = stitekNavic,
                    color = Color(0xFFFFF59D),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (aktivniKontrolaTahu && !jePovolena) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x5C06120B))
            )
        }
    }
}

@Composable
fun KartaRubComposable(
    modifier: Modifier = Modifier,
    sirka: Dp = 62.dp,
    pocetVBalicku: Int? = null,
    popisek: String? = null,
    zvyseneZvyrazneni: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val vyska = sirka * 1.58f
    val tvar = RoundedCornerShape(7.dp)

    Box(
        modifier = modifier
            .widthIn(max = sirka)
            .heightIn(max = vyska)
            .aspectRatio(1f / 1.58f)
            .shadow(5.dp, tvar)
            .clip(tvar)
            .background(BarvaPapiruKarty)
            .border(
                width = if (zvyseneZvyrazneni) 2.5.dp else 1.1.dp,
                color = if (zvyseneZvyrazneni) BarvaPovoleneZare else Color(0xFFB8B09E),
                shape = tvar
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        jemnaHaptika()
                        onClick()
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad = size.width * 0.068f
            val w = size.width - pad * 2f
            val h = size.height - pad * 2f

            drawRect(
                color = Color(0xFF981D26),
                topLeft = Offset(pad, pad),
                size = Size(w, h)
            )

            clipRect(left = pad + 2f, top = pad + 2f, right = pad + w - 2f, bottom = pad + h - 2f) {
                val roztec = size.width * 0.14f
                var posun = -h
                while (posun < w + h) {
                    drawLine(
                        color = Color(0x44F7DF94),
                        start = Offset(pad + posun, pad),
                        end = Offset(pad + posun + h, pad + h),
                        strokeWidth = 1.1f
                    )
                    drawLine(
                        color = Color(0x44F7DF94),
                        start = Offset(pad + posun + h, pad),
                        end = Offset(pad + posun, pad + h),
                        strokeWidth = 1.1f
                    )
                    posun += roztec
                }
            }

            drawRect(
                color = BarvaInkoustu,
                topLeft = Offset(pad, pad),
                size = Size(w, h),
                style = Stroke(width = 1.3f)
            )
            drawRect(
                color = Color(0xFFF5D77F),
                topLeft = Offset(pad + 3f, pad + 3f),
                size = Size(w - 6f, h - 6f),
                style = Stroke(width = 1.2f)
            )

            val stred = Offset(size.width / 2f, size.height / 2f)
            val rMed = min(w, h) * 0.29f
            drawCircle(color = Color(0xFF560E14), radius = rMed, center = stred)
            drawCircle(color = Color(0xFFF5D77F), radius = rMed, center = stred, style = Stroke(width = 1.5f))
            drawCircle(color = Color(0xFFF5D77F), radius = rMed * 0.34f, center = stred)
        }

        if (pocetVBalicku != null || !popisek.isNullOrEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xD90E2918))
                    .border(1.dp, Color(0xFFE6C15A), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                if (pocetVBalicku != null) {
                    Text(
                        text = pocetVBalicku.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (!popisek.isNullOrEmpty()) {
                    Text(
                        text = popisek,
                        color = Color(0xFFFFE082),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun DrawScope.vykresliZnakBarvy(
    barva: BarvaKarty,
    stred: Offset,
    polomer: Float
) {
    when (barva) {
        BarvaKarty.KULE -> {
            val r = polomer * 0.90f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BarvaChromovaZluta, BarvaRumelka, BarvaRumelkaTmava),
                    center = Offset(stred.x - r * 0.28f, stred.y - r * 0.32f),
                    radius = r * 1.28f
                ),
                radius = r,
                center = stred
            )
            clipRect(
                left = stred.x - r,
                top = stred.y - r * 0.28f,
                right = stred.x + r,
                bottom = stred.y + r * 0.28f
            ) {
                drawCircle(color = BarvaZlutaSvetla, radius = r, center = stred)
                val klikatka = Path().apply {
                    val krokX = r * 0.42f
                    var x = stred.x - r
                    moveTo(x, stred.y + r * 0.16f)
                    var nahoru = true
                    while (x <= stred.x + r + krokX) {
                        lineTo(x, if (nahoru) stred.y - r * 0.18f else stred.y + r * 0.18f)
                        x += krokX * 0.5f
                        nahoru = !nahoru
                    }
                }
                drawPath(klikatka, color = BarvaInkoustu, style = Stroke(width = 1.1f, join = StrokeJoin.Round))
            }
            drawCircle(BarvaInkoustu, radius = r * 0.16f, center = Offset(stred.x, stred.y + r * 0.60f))
            drawCircle(BarvaInkoustu, radius = r, center = stred, style = Stroke(width = 1.2f))
        }

        BarvaKarty.CERVENE -> {
            val srdce = Path().apply {
                moveTo(stred.x, stred.y + polomer * 0.96f)
                cubicTo(
                    stred.x - polomer * 1.34f,
                    stred.y + polomer * 0.20f,
                    stred.x - polomer * 1.20f,
                    stred.y - polomer * 0.98f,
                    stred.x,
                    stred.y - polomer * 0.30f
                )
                cubicTo(
                    stred.x + polomer * 1.20f,
                    stred.y - polomer * 0.98f,
                    stred.x + polomer * 1.34f,
                    stred.y + polomer * 0.20f,
                    stred.x,
                    stred.y + polomer * 0.96f
                )
                close()
            }
            drawPath(
                path = srdce,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFEB2836), BarvaRumelka, BarvaRumelkaTmava),
                    startX = stred.x - polomer,
                    endX = stred.x + polomer
                )
            )
            drawPath(srdce, color = BarvaInkoustu, style = Stroke(width = 1.2f))
        }

        BarvaKarty.ZELENE -> {
            val list = Path().apply {
                moveTo(stred.x, stred.y - polomer * 0.98f)
                cubicTo(
                    stred.x + polomer * 1.22f,
                    stred.y - polomer * 0.30f,
                    stred.x + polomer * 0.98f,
                    stred.y + polomer * 0.62f,
                    stred.x,
                    stred.y + polomer * 0.62f
                )
                cubicTo(
                    stred.x - polomer * 0.98f,
                    stred.y + polomer * 0.62f,
                    stred.x - polomer * 1.22f,
                    stred.y - polomer * 0.30f,
                    stred.x,
                    stred.y - polomer * 0.98f
                )
                close()
            }
            drawPath(
                path = list,
                brush = Brush.horizontalGradient(
                    colors = listOf(BarvaZelenaSvetla, BarvaSmaragdZelena),
                    startX = stred.x - polomer,
                    endX = stred.x + polomer
                )
            )
            drawLine(BarvaZlutaSvetla, Offset(stred.x, stred.y - polomer * 0.80f), Offset(stred.x, stred.y + polomer * 0.60f), strokeWidth = 1.2f)
            drawLine(BarvaKmene, Offset(stred.x, stred.y + polomer * 0.60f), Offset(stred.x + polomer * 0.18f, stred.y + polomer * 1.02f), strokeWidth = 1.6f)
            drawPath(list, color = BarvaInkoustu, style = Stroke(width = 1.2f))
        }

        BarvaKarty.ZALUDY -> {
            val plodW = polomer * 0.96f
            val plodH = polomer * 1.08f
            val plodTop = stred.y - polomer * 0.92f

            drawRoundRect(
                color = BarvaChromovaZluta,
                topLeft = Offset(stred.x - plodW * 0.5f, plodTop),
                size = Size(plodW * 0.52f, plodH),
                cornerRadius = CornerRadius(plodW * 0.45f, plodW * 0.45f)
            )
            drawRoundRect(
                color = BarvaRumelka,
                topLeft = Offset(stred.x, plodTop),
                size = Size(plodW * 0.50f, plodH),
                cornerRadius = CornerRadius(plodW * 0.45f, plodW * 0.45f)
            )
            drawRoundRect(
                color = BarvaInkoustu,
                topLeft = Offset(stred.x - plodW * 0.5f, plodTop),
                size = Size(plodW, plodH),
                cornerRadius = CornerRadius(plodW * 0.45f, plodW * 0.45f),
                style = Stroke(width = 1.1f)
            )

            val ciskaW = polomer * 1.30f
            val ciskaH = polomer * 0.46f
            val ciskaTop = stred.y + polomer * 0.02f
            drawRoundRect(
                color = BarvaSmaragdZelena,
                topLeft = Offset(stred.x - ciskaW * 0.5f, ciskaTop),
                size = Size(ciskaW, ciskaH),
                cornerRadius = CornerRadius(polomer * 0.22f, polomer * 0.22f)
            )
            drawRoundRect(
                color = BarvaInkoustu,
                topLeft = Offset(stred.x - ciskaW * 0.5f, ciskaTop),
                size = Size(ciskaW, ciskaH),
                cornerRadius = CornerRadius(polomer * 0.22f, polomer * 0.22f),
                style = Stroke(width = 1.1f)
            )
            drawLine(BarvaKmene, Offset(stred.x, ciskaTop + ciskaH), Offset(stred.x, stred.y + polomer * 0.98f), strokeWidth = 1.6f)
        }
    }
}
