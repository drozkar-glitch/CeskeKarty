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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private val BarvaOkrajePapiru = Color(0xFFFCFBF7)
private val BarvaVnitrkuPapiru = Color(0xFFF6F2E6)
private val BarvaInkoustu = Color(0xFF231C18)

private val BarvaRumelka = Color(0xFFD61F2C)
private val BarvaRumelkaTmava = Color(0xFF9E111B)
private val BarvaChromovaZluta = Color(0xFFF4C41C)
private val BarvaZlutaSvetla = Color(0xFFFFE47A)
private val BarvaSmaragdZelena = Color(0xFF167E39)
private val BarvaZelenaSvetla = Color(0xFF34A853)
private val BarvaKobaltModra = Color(0xFF1E5AA8)
private val BarvaPurpurRuzova = Color(0xFFD6386E)
private val BarvaPleti = Color(0xFFFAD7BE)
private val BarvaTvari = Color(0xFFEC9680)
private val BarvaKmene = Color(0xFF7D4422)
private val BarvaVlasu = Color(0xFF5D3A24)

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
    val vyska = sirka * 1.58f
    val tvar = RoundedCornerShape(8.dp)

    val cilovyPosunY = when {
        jeVybrana -> (-14).dp
        aktivniKontrolaTahu && jePovolena -> (-9).dp
        else -> 0.dp
    }
    val animovanyPosunY by animateDpAsState(targetValue = cilovyPosunY)

    val okrajBarva = when {
        jeVybrana -> BarvaVybraneZare
        aktivniKontrolaTahu && jePovolena -> BarvaPovoleneZare
        else -> Color(0xFFC8C0AE)
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
                elevation = if (jeVybrana || (aktivniKontrolaTahu && jePovolena)) 9.dp else 4.dp,
                shape = tvar
            )
            .clip(tvar)
            .background(BarvaOkrajePapiru)
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
            vykresliJednohlavouKartuCeskyObraz(karta)
        }

        if (!stitekNavic.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xE6134E27))
                    .border(0.8.dp, Color(0xFFFFD54F), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = stitekNavic,
                    color = Color(0xFFFFF59D),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (aktivniKontrolaTahu && !jePovolena) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x5A07140D))
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
    val tvar = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .widthIn(max = sirka)
            .heightIn(max = vyska)
            .aspectRatio(1f / 1.58f)
            .shadow(5.dp, tvar)
            .clip(tvar)
            .background(BarvaOkrajePapiru)
            .border(
                width = if (zvyseneZvyrazneni) 2.5.dp else 1.1.dp,
                color = if (zvyseneZvyrazneni) BarvaPovoleneZare else Color(0xFFC8C0AE),
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

/**
 * Hlavní kresba tradiční české jednohlavé karty (Pražský / Český obraz):
 * čistý bílý okraj, tenká litografická linka rámečku, žádné indexy v rozích.
 */
private fun DrawScope.vykresliJednohlavouKartuCeskyObraz(karta: Karta) {
    val fx = size.width * 0.065f
    val fy = size.height * 0.045f
    val fw = size.width - fx * 2f
    val fh = size.height - fy * 2f

    drawRect(
        color = BarvaVnitrkuPapiru,
        topLeft = Offset(fx, fy),
        size = Size(fw, fh)
    )

    clipRect(left = fx, top = fy, right = fx + fw, bottom = fy + fh) {
        when (karta.hodnota) {
            HodnotaKarty.R7 -> vykresliCiselnouKartu(karta.barva, 7, fx, fy, fw, fh)
            HodnotaKarty.R8 -> vykresliCiselnouKartu(karta.barva, 8, fx, fy, fw, fh)
            HodnotaKarty.R9 -> vykresliCiselnouKartu(karta.barva, 9, fx, fy, fw, fh)
            HodnotaKarty.R10 -> vykresliCiselnouKartu(karta.barva, 10, fx, fy, fw, fh)
            HodnotaKarty.SPODEK -> vykresliSpodka(karta.barva, fx, fy, fw, fh)
            HodnotaKarty.SVRSEK -> vykresliSvrska(karta.barva, fx, fy, fw, fh)
            HodnotaKarty.KRAL -> vykresliKrale(karta.barva, fx, fy, fw, fh)
            HodnotaKarty.ESO -> vykresliEso(karta.barva, fx, fy, fw, fh)
        }
    }

    drawRect(
        color = BarvaInkoustu,
        topLeft = Offset(fx, fy),
        size = Size(fw, fh),
        style = Stroke(width = max(1.1f, size.width * 0.008f))
    )
}

private fun DrawScope.vykresliZelenoZlutyPazit(
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float,
    vyskaPodilu: Float = 0.14f,
    cerveneDlazdice: Boolean = false
) {
    val topY = fy + fh * (1f - vyskaPodilu)
    val dnoY = fy + fh

    val zlutySvah = Path().apply {
        moveTo(fx, topY + fh * 0.03f)
        cubicTo(
            fx + fw * 0.32f,
            topY - fh * 0.02f,
            fx + fw * 0.68f,
            topY + fh * 0.01f,
            fx + fw,
            topY + fh * 0.035f
        )
        lineTo(fx + fw, dnoY)
        lineTo(fx, dnoY)
        close()
    }
    drawPath(zlutySvah, color = BarvaChromovaZluta)

    val zelenyKopecekLevy = Path().apply {
        moveTo(fx, topY + fh * 0.04f)
        cubicTo(fx + fw * 0.22f, topY + fh * 0.01f, fx + fw * 0.42f, topY + fh * 0.06f, fx + fw * 0.52f, dnoY)
        lineTo(fx, dnoY)
        close()
    }
    drawPath(zelenyKopecekLevy, color = BarvaSmaragdZelena)

    val pravBarva = if (cerveneDlazdice) BarvaRumelka else BarvaSmaragdZelena
    val kopecekPravy = Path().apply {
        moveTo(fx + fw * 0.56f, dnoY)
        cubicTo(fx + fw * 0.68f, topY + fh * 0.03f, fx + fw * 0.86f, topY + fh * 0.02f, fx + fw, topY + fh * 0.045f)
        lineTo(fx + fw, dnoY)
        close()
    }
    drawPath(kopecekPravy, color = pravBarva)

    drawLine(
        color = BarvaInkoustu,
        start = Offset(fx, topY + fh * 0.035f),
        end = Offset(fx + fw, topY + fh * 0.04f),
        strokeWidth = 0.9f
    )
}

private fun DrawScope.vykresliRimskeDeset(stredX: Float, stredY: Float, vyska: Float) {
    val polV = vyska * 0.5f
    val polS = vyska * 0.46f
    val tl = max(2.0f, vyska * 0.20f)
    drawLine(
        color = BarvaInkoustu,
        start = Offset(stredX - polS, stredY - polV),
        end = Offset(stredX + polS, stredY + polV),
        strokeWidth = tl
    )
    drawLine(
        color = BarvaInkoustu,
        start = Offset(stredX + polS, stredY - polV),
        end = Offset(stredX - polS, stredY + polV),
        strokeWidth = tl
    )
    val patka = polS * 0.42f
    drawLine(BarvaInkoustu, Offset(stredX - polS - patka, stredY - polV), Offset(stredX - polS + patka, stredY - polV), strokeWidth = 1.3f)
    drawLine(BarvaInkoustu, Offset(stredX + polS - patka, stredY - polV), Offset(stredX + polS + patka, stredY - polV), strokeWidth = 1.3f)
    drawLine(BarvaInkoustu, Offset(stredX - polS - patka, stredY + polV), Offset(stredX - polS + patka, stredY + polV), strokeWidth = 1.3f)
    drawLine(BarvaInkoustu, Offset(stredX + polS - patka, stredY + polV), Offset(stredX + polS + patka, stredY + polV), strokeWidth = 1.3f)
}

/**
 * Číselné karty 7, 8, 9, 10 s přesným rozložením znaků, kmenem pro zelené a žaludy,
 * římským X na desítkách a spodní ilustrací na pažitu.
 */
private fun DrawScope.vykresliCiselnouKartu(
    barva: BarvaKarty,
    pocet: Int,
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float
) {
    val cx = fx + fw * 0.5f
    val rZnak = fw * 0.132f

    if (barva != BarvaKarty.ZALUDY || pocet != 10) {
        vykresliZelenoZlutyPazit(fx, fy, fw, fh, vyskaPodilu = 0.15f)
    }

    if (pocet == 10) {
        vykresliRimskeDeset(cx, fy + fh * 0.055f, fh * 0.062f)
    }

    when (barva) {
        BarvaKarty.KULE, BarvaKarty.CERVENE -> {
            val lx = fx + fw * 0.21f
            val rx = fx + fw * 0.79f
            when (pocet) {
                7 -> {
                    vykresliZnakBarvy(barva, Offset(cx, fy + fh * 0.11f), rZnak)
                    for (rf in listOf(0.19f, 0.34f, 0.49f)) {
                        vykresliZnakBarvy(barva, Offset(lx, fy + fh * rf), rZnak * 0.96f)
                        vykresliZnakBarvy(barva, Offset(rx, fy + fh * rf), rZnak * 0.96f)
                    }
                }
                8 -> {
                    for (rf in listOf(0.11f, 0.25f, 0.39f, 0.53f)) {
                        vykresliZnakBarvy(barva, Offset(lx, fy + fh * rf), rZnak * 0.93f)
                        vykresliZnakBarvy(barva, Offset(rx, fy + fh * rf), rZnak * 0.93f)
                    }
                }
                9 -> {
                    for (rf in listOf(0.14f, 0.29f, 0.44f)) {
                        vykresliZnakBarvy(barva, Offset(lx, fy + fh * rf), rZnak * 0.88f)
                        vykresliZnakBarvy(barva, Offset(cx, fy + fh * (rf - 0.03f)), rZnak * 0.88f)
                        vykresliZnakBarvy(barva, Offset(rx, fy + fh * rf), rZnak * 0.88f)
                    }
                }
                else -> {
                    for (rf in listOf(0.18f, 0.33f, 0.48f)) {
                        vykresliZnakBarvy(barva, Offset(lx, fy + fh * rf), rZnak * 0.85f)
                        vykresliZnakBarvy(barva, Offset(rx, fy + fh * rf), rZnak * 0.85f)
                    }
                    for (rf in listOf(0.15f, 0.28f, 0.41f, 0.54f)) {
                        vykresliZnakBarvy(barva, Offset(cx, fy + fh * rf), rZnak * 0.83f)
                    }
                }
            }
        }

        BarvaKarty.ZELENE -> {
            val kmenTop = if (pocet == 10) fy + fh * 0.11f else fy + fh * 0.15f
            val kmenDno = fy + fh * 0.88f
            drawLine(
                color = BarvaKmene,
                start = Offset(cx, kmenTop),
                end = Offset(cx, kmenDno),
                strokeWidth = max(2.2f, fw * 0.028f)
            )

            val rady = when (pocet) {
                7 -> listOf(0.25f, 0.40f, 0.55f)
                8 -> listOf(0.17f, 0.30f, 0.43f, 0.56f)
                9 -> listOf(0.22f, 0.34f, 0.46f, 0.58f)
                else -> listOf(0.17f, 0.28f, 0.39f, 0.50f, 0.61f)
            }

            if (pocet == 7 || pocet == 9) {
                vykresliZnakBarvy(BarvaKarty.ZELENE, Offset(cx, fy + fh * 0.10f), rZnak * 0.92f)
            } else {
                drawCircle(BarvaRumelka, radius = fw * 0.022f, center = Offset(cx - fw * 0.04f, kmenTop))
                drawCircle(BarvaRumelka, radius = fw * 0.022f, center = Offset(cx + fw * 0.04f, kmenTop))
            }

            val rList = if (pocet == 10) rZnak * 0.78f else rZnak * 0.86f
            for (rf in rady) {
                val y = fy + fh * rf
                drawLine(BarvaKmene, Offset(cx, y + rList * 0.5f), Offset(fx + fw * 0.24f, y), strokeWidth = 1.3f)
                drawLine(BarvaKmene, Offset(cx, y + rList * 0.5f), Offset(fx + fw * 0.76f, y), strokeWidth = 1.3f)
                vykresliZnakBarvy(BarvaKarty.ZELENE, Offset(fx + fw * 0.23f, y), rList)
                vykresliZnakBarvy(BarvaKarty.ZELENE, Offset(fx + fw * 0.77f, y), rList)
            }
        }

        BarvaKarty.ZALUDY -> {
            val kmenTop = if (pocet == 10) fy + fh * 0.11f else fy + fh * 0.16f
            val kmenDno = fy + fh * 0.88f
            drawLine(
                color = BarvaKmene,
                start = Offset(cx, kmenTop),
                end = Offset(cx, kmenDno),
                strokeWidth = max(2.2f, fw * 0.028f)
            )

            if (pocet == 7 || pocet == 9) {
                vykresliZnakBarvy(BarvaKarty.ZALUDY, Offset(cx, fy + fh * 0.10f), rZnak * 0.88f)
            } else {
                drawCircle(BarvaRumelka, radius = fw * 0.022f, center = Offset(cx - fw * 0.04f, kmenTop))
                drawCircle(BarvaRumelka, radius = fw * 0.022f, center = Offset(cx + fw * 0.04f, kmenTop))
            }

            val rady = when (pocet) {
                7 -> listOf(0.24f, 0.38f, 0.52f)
                8 -> listOf(0.17f, 0.30f, 0.43f, 0.56f)
                9 -> listOf(0.22f, 0.34f, 0.46f, 0.58f)
                else -> listOf(0.17f, 0.29f, 0.41f, 0.53f, 0.65f)
            }
            val delkaZaludu = if (pocet == 10) fw * 0.17f else fw * 0.19f
            for (rf in rady) {
                val y = fy + fh * rf
                vykresliVodorovnyZalud(Offset(cx - fw * 0.04f, y), delkaZaludu, smerDoprava = false)
                vykresliVodorovnyZalud(Offset(cx + fw * 0.04f, y), delkaZaludu, smerDoprava = true)
            }
        }
    }

    vykresliSpodniVyjevCiselneKarty(barva, pocet, fx, fy, fw, fh)
}

/**
 * Tradiční spodní miniatury českého obrazu na kartách 7 až 10.
 */
private fun DrawScope.vykresliSpodniVyjevCiselneKarty(
    barva: BarvaKarty,
    pocet: Int,
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float
) {
    val cx = fx + fw * 0.5f
    val zemY = fy + fh * 0.89f
    val u = fw * 0.08f

    when (barva) {
        BarvaKarty.KULE -> when (pocet) {
            7 -> {
                // Květináč s kvetoucí rostlinou
                val kvetinac = Path().apply {
                    moveTo(cx - u * 1.4f, zemY - u * 2.1f)
                    lineTo(cx + u * 1.4f, zemY - u * 2.1f)
                    lineTo(cx + u * 1.0f, zemY)
                    lineTo(cx - u * 1.0f, zemY)
                    close()
                }
                drawPath(kvetinac, color = Color(0xFFC86428))
                drawPath(kvetinac, color = BarvaInkoustu, style = Stroke(width = 1.1f))
                drawLine(BarvaSmaragdZelena, Offset(cx, zemY - u * 2.1f), Offset(cx, zemY - u * 4.2f), strokeWidth = 1.8f)
                drawCircle(BarvaChromovaZluta, radius = u * 0.85f, center = Offset(cx, zemY - u * 4.3f))
                drawCircle(BarvaRumelka, radius = u * 0.38f, center = Offset(cx, zemY - u * 4.3f))
            }
            8 -> {
                // Běžící lovecký pes
                drawOval(Color(0xFF8D5A38), Offset(cx - u * 2.1f, zemY - u * 2.0f), Size(u * 3.6f, u * 1.2f))
                drawCircle(Color(0xFF8D5A38), radius = u * 0.65f, center = Offset(cx + u * 1.6f, zemY - u * 2.1f))
                drawLine(BarvaInkoustu, Offset(cx - u * 1.8f, zemY - u * 0.9f), Offset(cx - u * 2.5f, zemY), strokeWidth = 1.6f)
                drawLine(BarvaInkoustu, Offset(cx + u * 1.2f, zemY - u * 0.9f), Offset(cx + u * 2.1f, zemY), strokeWidth = 1.6f)
            }
            9 -> {
                // Vzpínající se kůň u ohrady
                drawLine(BarvaKmene, Offset(fx + fw * 0.14f, zemY), Offset(fx + fw * 0.14f, zemY - u * 2.4f), strokeWidth = 2f)
                drawLine(BarvaKmene, Offset(fx + fw * 0.28f, zemY), Offset(fx + fw * 0.28f, zemY - u * 2.4f), strokeWidth = 2f)
                drawLine(BarvaKmene, Offset(fx + fw * 0.10f, zemY - u * 1.6f), Offset(fx + fw * 0.32f, zemY - u * 1.6f), strokeWidth = 1.4f)
                drawOval(Color(0xFF7D4E32), Offset(cx - u * 0.8f, zemY - u * 2.6f), Size(u * 3.2f, u * 1.4f))
                drawCircle(Color(0xFF7D4E32), radius = u * 0.68f, center = Offset(cx - u * 0.9f, zemY - u * 3.2f))
                drawLine(BarvaInkoustu, Offset(cx - u * 0.2f, zemY - u * 1.3f), Offset(cx - u * 0.7f, zemY), strokeWidth = 1.6f)
                drawLine(BarvaInkoustu, Offset(cx + u * 1.8f, zemY - u * 1.3f), Offset(cx + u * 2.2f, zemY), strokeWidth = 1.6f)
            }
            else -> {
                // Dvě vesnické postavičky na pažitu
                drawCircle(BarvaPleti, radius = u * 0.5f, center = Offset(cx - u * 2.2f, zemY - u * 2.5f))
                drawRect(BarvaSmaragdZelena, Offset(cx - u * 2.6f, zemY - u * 2.0f), Size(u * 0.8f, u * 1.4f))
                drawCircle(BarvaPleti, radius = u * 0.5f, center = Offset(cx + u * 2.2f, zemY - u * 2.5f))
                drawRect(BarvaRumelka, Offset(cx + u * 1.8f, zemY - u * 2.0f), Size(u * 0.8f, u * 1.4f))
            }
        }

        BarvaKarty.CERVENE -> when (pocet) {
            7 -> {
                // Dřevěný škopek s růžovým keříkem
                drawRect(Color(0xFFA84B24), Offset(cx - u * 1.5f, zemY - u * 2.0f), Size(u * 3.0f, u * 2.0f))
                drawRect(BarvaInkoustu, Offset(cx - u * 1.5f, zemY - u * 2.0f), Size(u * 3.0f, u * 2.0f), style = Stroke(width = 1.1f))
                drawLine(BarvaSmaragdZelena, Offset(cx, zemY - u * 2.0f), Offset(cx, zemY - u * 4.4f), strokeWidth = 1.8f)
                drawCircle(BarvaRumelka, radius = u * 0.55f, center = Offset(cx, zemY - u * 4.5f))
                drawCircle(BarvaRumelka, radius = u * 0.42f, center = Offset(cx - u * 0.9f, zemY - u * 3.4f))
                drawCircle(BarvaRumelka, radius = u * 0.42f, center = Offset(cx + u * 0.9f, zemY - u * 3.4f))
            }
            8 -> {
                // Červeno-zlatá zahradní kašna s tryskající vodou
                drawOval(BarvaRumelka, Offset(cx - u * 2.0f, zemY - u * 2.2f), Size(u * 4.0f, u * 1.2f))
                drawRect(BarvaKmene, Offset(cx - u * 0.6f, zemY - u * 3.4f), Size(u * 1.2f, u * 3.4f))
                drawLine(BarvaChromovaZluta, Offset(cx, zemY - u * 3.4f), Offset(cx - u * 2.2f, zemY), strokeWidth = 2.0f)
                drawLine(BarvaChromovaZluta, Offset(cx, zemY - u * 3.4f), Offset(cx + u * 2.2f, zemY), strokeWidth = 2.0f)
            }
            9 -> {
                // Květinový zlatý obelisk
                val pyr = Path().apply {
                    moveTo(cx, zemY - u * 4.6f)
                    lineTo(cx + u * 1.8f, zemY)
                    lineTo(cx - u * 1.8f, zemY)
                    close()
                }
                drawPath(pyr, color = BarvaChromovaZluta)
                drawPath(pyr, color = BarvaRumelka, style = Stroke(width = 1.4f))
                drawCircle(BarvaSmaragdZelena, radius = u * 0.45f, center = Offset(cx, zemY - u * 2.0f))
            }
            else -> {
                // Amor s lukem a šípem
                val ax = cx + u * 1.8f
                drawCircle(BarvaPleti, radius = u * 0.62f, center = Offset(ax, zemY - u * 3.0f))
                drawOval(BarvaPleti, Offset(ax - u * 0.5f, zemY - u * 2.4f), Size(u * 1.0f, u * 1.6f))
                drawLine(BarvaInkoustu, Offset(ax - u * 1.1f, zemY - u * 3.2f), Offset(ax - u * 1.1f, zemY - u * 1.4f), strokeWidth = 1.4f)
                drawLine(BarvaInkoustu, Offset(ax - u * 1.6f, zemY - u * 2.3f), Offset(ax - u * 0.4f, zemY - u * 2.3f), strokeWidth = 1.1f)
                drawLine(BarvaKmene, Offset(cx - u * 2.2f, zemY), Offset(cx - u * 2.2f, zemY - u * 2.8f), strokeWidth = 2.2f)
                drawCircle(BarvaRumelka, radius = u * 0.45f, center = Offset(cx - u * 2.2f, zemY - u * 3.0f))
            }
        }

        BarvaKarty.ZELENE -> when (pocet) {
            7 -> {
                // Poutník v červeném kabátci s holí u kmene
                val px = cx - u * 1.8f
                drawCircle(BarvaPleti, radius = u * 0.52f, center = Offset(px, zemY - u * 2.7f))
                drawRect(BarvaRumelka, Offset(px - u * 0.5f, zemY - u * 2.1f), Size(u * 1.0f, u * 1.3f))
                drawLine(BarvaInkoustu, Offset(px - u * 1.0f, zemY - u * 2.2f), Offset(px - u * 1.2f, zemY), strokeWidth = 1.2f)
            }
            8 -> {
                // Šedý slon s chobotem
                drawOval(Color(0xFF75706B), Offset(cx - u * 2.2f, zemY - u * 2.4f), Size(u * 3.6f, u * 2.0f))
                drawCircle(Color(0xFF75706B), radius = u * 0.9f, center = Offset(cx - u * 2.2f, zemY - u * 1.8f))
                drawLine(Color(0xFF75706B), Offset(cx - u * 2.9f, zemY - u * 1.6f), Offset(cx - u * 3.4f, zemY - u * 0.4f), strokeWidth = 2.4f)
            }
            9 -> {
                // Skákající jelen s parožím
                drawOval(Color(0xFF8A5636), Offset(cx - u * 1.8f, zemY - u * 2.0f), Size(u * 2.8f, u * 1.1f))
                drawCircle(Color(0xFF8A5636), radius = u * 0.5f, center = Offset(cx - u * 1.9f, zemY - u * 2.5f))
                drawLine(BarvaInkoustu, Offset(cx - u * 1.9f, zemY - u * 2.9f), Offset(cx - u * 2.5f, zemY - u * 3.6f), strokeWidth = 1.2f)
            }
            else -> {
                // Ležící ohař pod kmenem
                drawOval(Color(0xFF6D584B), Offset(cx - u * 2.2f, zemY - u * 1.3f), Size(u * 4.2f, u * 1.2f))
                drawCircle(Color(0xFF6D584B), radius = u * 0.65f, center = Offset(cx - u * 2.3f, zemY - u * 1.4f))
            }
        }

        BarvaKarty.ZALUDY -> when (pocet) {
            7 -> {
                // Venkovan v zeleném kabátci a chaloupka vpravo
                val px = cx - u * 0.8f
                drawCircle(BarvaPleti, radius = u * 0.55f, center = Offset(px, zemY - u * 2.8f))
                drawRect(BarvaSmaragdZelena, Offset(px - u * 0.6f, zemY - u * 2.2f), Size(u * 1.2f, u * 1.4f))
                drawRect(BarvaRumelka, Offset(fx + fw * 0.78f, zemY - u * 2.2f), Size(u * 1.4f, u * 1.8f))
            }
            8 -> {
                // Trubač v červeném kabátci se zlatou trubkou a psem
                val px = cx - u * 1.8f
                drawCircle(BarvaPleti, radius = u * 0.58f, center = Offset(px, zemY - u * 2.5f))
                drawRect(BarvaRumelka, Offset(px - u * 0.7f, zemY - u * 1.9f), Size(u * 1.4f, u * 1.6f))
                drawLine(BarvaChromovaZluta, Offset(px + u * 0.4f, zemY - u * 2.3f), Offset(cx + u * 1.4f, zemY - u * 2.5f), strokeWidth = 2.2f)
                drawOval(Color(0xFF8D7B6F), Offset(cx + u * 0.9f, zemY - u * 1.4f), Size(u * 1.8f, u * 0.9f))
            }
            9 -> {
                // Bažant / dravec sedící na kmeni
                drawOval(BarvaChromovaZluta, Offset(cx - u * 1.6f, zemY - u * 2.3f), Size(u * 3.0f, u * 1.3f))
                drawOval(BarvaSmaragdZelena, Offset(cx - u * 1.2f, zemY - u * 2.2f), Size(u * 2.0f, u * 0.9f))
                drawLine(BarvaRumelka, Offset(cx - u * 1.6f, zemY - u * 1.6f), Offset(cx - u * 3.0f, zemY - u * 0.6f), strokeWidth = 2.2f)
            }
            else -> {
                // Symetrické barokní rozviliny pod kmenem desítky žaludů
                drawCircle(BarvaInkoustu, radius = u * 1.2f, center = Offset(cx - u * 1.8f, zemY - u * 0.8f), style = Stroke(width = 1.2f))
                drawCircle(BarvaInkoustu, radius = u * 1.2f, center = Offset(cx + u * 1.8f, zemY - u * 0.8f), style = Stroke(width = 1.2f))
            }
        }
    }
}

/**
 * Jednohlavý SPODEK: celá stojící postava, BEZ znaku nahoře — 1 velký znak barvy dole u nohou.
 */
private fun DrawScope.vykresliSpodka(
    barva: BarvaKarty,
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float
) {
    vykresliZelenoZlutyPazit(fx, fy, fw, fh, vyskaPodilu = 0.13f)
    val cx = fx + fw * 0.52f
    val u = fw * 0.10f
    val hlavaY = fy + fh * 0.16f
    val ramenaY = fy + fh * 0.25f
    val pasY = fy + fh * 0.49f
    val kolenaY = fy + fh * 0.69f
    val chodidlaY = fy + fh * 0.89f

    val barvaKabatu: Color
    val barvaNohavic: Color
    val barvaPuncoch: Color
    when (barva) {
        BarvaKarty.KULE -> {
            barvaKabatu = BarvaRumelka
            barvaNohavic = BarvaChromovaZluta
            barvaPuncoch = BarvaChromovaZluta
        }
        BarvaKarty.CERVENE -> {
            barvaKabatu = BarvaChromovaZluta
            barvaNohavic = BarvaChromovaZluta
            barvaPuncoch = BarvaKobaltModra
        }
        BarvaKarty.ZELENE -> {
            barvaKabatu = BarvaRumelka
            barvaNohavic = BarvaSmaragdZelena
            barvaPuncoch = BarvaKobaltModra
        }
        BarvaKarty.ZALUDY -> {
            barvaKabatu = BarvaRumelka
            barvaNohavic = BarvaRumelka
            barvaPuncoch = BarvaChromovaZluta
        }
    }

    // Rozkročené nohy s punčochami a střevíci
    val levaNoha = Path().apply {
        moveTo(cx - u * 0.9f, pasY)
        lineTo(cx - u * 2.1f, kolenaY)
        lineTo(cx - u * 2.5f, chodidlaY)
        lineTo(cx - u * 1.7f, chodidlaY)
        lineTo(cx - u * 1.1f, kolenaY)
        lineTo(cx - u * 0.1f, pasY + u * 0.5f)
        close()
    }
    val pravaNoha = Path().apply {
        moveTo(cx + u * 0.9f, pasY)
        lineTo(cx + u * 1.9f, kolenaY)
        lineTo(cx + u * 2.1f, chodidlaY)
        lineTo(cx + u * 1.3f, chodidlaY)
        lineTo(cx + u * 0.9f, kolenaY)
        lineTo(cx + u * 0.1f, pasY + u * 0.5f)
        close()
    }
    drawPath(levaNoha, color = barvaNohavic)
    drawPath(pravaNoha, color = barvaNohavic)
    drawLine(barvaPuncoch, Offset(cx - u * 1.6f, kolenaY), Offset(cx - u * 2.1f, chodidlaY), strokeWidth = u * 0.72f)
    drawLine(barvaPuncoch, Offset(cx + u * 1.4f, kolenaY), Offset(cx + u * 1.7f, chodidlaY), strokeWidth = u * 0.72f)
    drawPath(levaNoha, color = BarvaInkoustu, style = Stroke(width = 1.2f))
    drawPath(pravaNoha, color = BarvaInkoustu, style = Stroke(width = 1.2f))
    drawOval(BarvaInkoustu, Offset(cx - u * 2.9f, chodidlaY - u * 0.35f), Size(u * 1.3f, u * 0.55f))
    drawOval(BarvaInkoustu, Offset(cx + u * 1.2f, chodidlaY - u * 0.35f), Size(u * 1.3f, u * 0.55f))

    // Kabátec / halena se šosy
    val kabat = Path().apply {
        moveTo(cx - u * 1.35f, ramenaY)
        lineTo(cx + u * 1.35f, ramenaY)
        lineTo(cx + u * 1.05f, pasY - u * 0.2f)
        lineTo(cx + u * 1.85f, pasY + u * 0.9f)
        lineTo(cx - u * 1.85f, pasY + u * 0.9f)
        lineTo(cx - u * 1.05f, pasY - u * 0.2f)
        close()
    }
    drawPath(kabat, color = barvaKabatu)
    if (barva == BarvaKarty.ZALUDY || barva == BarvaKarty.ZELENE) {
        drawRect(
            color = if (barva == BarvaKarty.ZELENE) BarvaSmaragdZelena else BarvaChromovaZluta,
            topLeft = Offset(cx - u * 0.55f, ramenaY),
            size = Size(u * 1.1f, pasY - ramenaY + u * 0.6f)
        )
    }
    drawPath(kabat, color = BarvaInkoustu, style = Stroke(width = 1.3f))
    drawLine(BarvaChromovaZluta, Offset(cx - u * 1.1f, pasY - u * 0.1f), Offset(cx + u * 1.1f, pasY - u * 0.1f), strokeWidth = 2.4f)

    // Ruce a typická rekvizita spodka dle barvy (včetně flétnisty na zeleném spodkovi)
    when (barva) {
        BarvaKarty.ZELENE -> {
            // Zelený spodek hrající na příčnou flétnu v růžovém klobouku
            drawLine(BarvaSmaragdZelena, Offset(cx - u * 1.2f, ramenaY + u * 0.3f), Offset(cx - u * 2.2f, hlavaY + u * 0.5f), strokeWidth = u * 0.7f, cap = StrokeCap.Round)
            drawLine(BarvaKmene, Offset(cx + u * 0.3f, hlavaY + u * 0.35f), Offset(cx - u * 3.8f, hlavaY + u * 0.65f), strokeWidth = 2.5f)
        }
        BarvaKarty.CERVENE -> {
            drawLine(BarvaRumelka, Offset(cx - u * 1.3f, ramenaY + u * 0.2f), Offset(cx - u * 2.5f, pasY - u * 0.2f), strokeWidth = u * 0.75f, cap = StrokeCap.Round)
            drawLine(BarvaRumelka, Offset(cx + u * 1.3f, ramenaY + u * 0.2f), Offset(cx + u * 2.2f, ramenaY - u * 0.5f), strokeWidth = u * 0.75f, cap = StrokeCap.Round)
            drawLine(BarvaKmene, Offset(cx - u * 3.2f, pasY + u * 1.2f), Offset(cx + u * 3.0f, hlavaY - u * 0.5f), strokeWidth = 2.6f)
        }
        else -> {
            // Tasený kord nad hlavou
            val rukaBarva = if (barva == BarvaKarty.ZALUDY) BarvaKobaltModra else BarvaRumelka
            drawLine(rukaBarva, Offset(cx - u * 1.2f, ramenaY + u * 0.2f), Offset(cx - u * 2.2f, hlavaY - u * 0.3f), strokeWidth = u * 0.75f, cap = StrokeCap.Round)
            drawLine(Color(0xFFCFD8DC), Offset(cx - u * 2.0f, hlavaY - u * 0.4f), Offset(cx + u * 2.8f, pasY - u * 0.2f), strokeWidth = 2.2f)
        }
    }

    // Bílý okružní límec a hlava spodka
    drawOval(Color.White, Offset(cx - u * 1.0f, ramenaY - u * 0.32f), Size(u * 2.0f, u * 0.55f))
    drawOval(BarvaInkoustu, Offset(cx - u * 1.0f, ramenaY - u * 0.32f), Size(u * 2.0f, u * 0.55f), style = Stroke(width = 1.0f))
    vykresliHlavuPostavy(
        stred = Offset(cx, hlavaY),
        u = u,
        sPlnovousem = false,
        typPokryvky = if (barva == BarvaKarty.ZELENE) 1 else 0
    )

    // 1 znak barvy DOLE u nohou spodka!
    val znakStred = if (barva == BarvaKarty.ZELENE) {
        Offset(fx + fw * 0.22f, fy + fh * 0.78f)
    } else {
        Offset(cx, fy + fh * 0.80f)
    }
    vykresliZnakBarvy(barva, znakStred, fw * 0.135f)
}

/**
 * Jednohlavý SVRŠEK: celá stojící postava a 1 velký znak barvy V HORNÍM LEVÉM ROHU.
 */
private fun DrawScope.vykresliSvrska(
    barva: BarvaKarty,
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float
) {
    vykresliZelenoZlutyPazit(fx, fy, fw, fh, vyskaPodilu = 0.13f, cerveneDlazdice = barva == BarvaKarty.ZELENE)

    // 1 znak barvy vlevo nahoře (hlavní poznávací znak svrška!)
    vykresliZnakBarvy(barva, Offset(fx + fw * 0.20f, fy + fh * 0.11f), fw * 0.14f)

    val cx = fx + fw * 0.56f
    val u = fw * 0.10f
    val hlavaY = fy + fh * 0.16f
    val ramenaY = fy + fh * 0.25f
    val pasY = fy + fh * 0.49f
    val kolenaY = fy + fh * 0.69f
    val chodidlaY = fy + fh * 0.89f

    val barvaKabatu = when (barva) {
        BarvaKarty.KULE -> BarvaRumelka
        BarvaKarty.CERVENE -> BarvaPurpurRuzova
        BarvaKarty.ZELENE -> BarvaSmaragdZelena
        BarvaKarty.ZALUDY -> BarvaRumelka
    }
    val barvaNohavic = when (barva) {
        BarvaKarty.KULE -> BarvaChromovaZluta
        BarvaKarty.CERVENE -> BarvaSmaragdZelena
        BarvaKarty.ZELENE -> BarvaSmaragdZelena
        BarvaKarty.ZALUDY -> BarvaSmaragdZelena
    }
    val barvaPuncoch = when (barva) {
        BarvaKarty.ZELENE -> BarvaKobaltModra
        BarvaKarty.CERVENE -> BarvaRumelka
        else -> BarvaChromovaZluta
    }

    val levaNoha = Path().apply {
        moveTo(cx - u * 0.9f, pasY)
        lineTo(cx - u * 1.9f, kolenaY)
        lineTo(cx - u * 2.2f, chodidlaY)
        lineTo(cx - u * 1.4f, chodidlaY)
        lineTo(cx - u * 0.9f, kolenaY)
        lineTo(cx - u * 0.1f, pasY + u * 0.5f)
        close()
    }
    val pravaNoha = Path().apply {
        moveTo(cx + u * 0.9f, pasY)
        lineTo(cx + u * 1.8f, kolenaY)
        lineTo(cx + u * 2.0f, chodidlaY)
        lineTo(cx + u * 1.2f, chodidlaY)
        lineTo(cx + u * 0.8f, kolenaY)
        lineTo(cx + u * 0.1f, pasY + u * 0.5f)
        close()
    }
    drawPath(levaNoha, color = barvaNohavic)
    drawPath(pravaNoha, color = barvaNohavic)
    drawLine(barvaPuncoch, Offset(cx - u * 1.4f, kolenaY), Offset(cx - u * 1.8f, chodidlaY), strokeWidth = u * 0.72f)
    drawLine(barvaPuncoch, Offset(cx + u * 1.3f, kolenaY), Offset(cx + u * 1.6f, chodidlaY), strokeWidth = u * 0.72f)
    drawPath(levaNoha, color = BarvaInkoustu, style = Stroke(width = 1.2f))
    drawPath(pravaNoha, color = BarvaInkoustu, style = Stroke(width = 1.2f))
    drawOval(BarvaInkoustu, Offset(cx - u * 2.6f, chodidlaY - u * 0.35f), Size(u * 1.3f, u * 0.55f))
    drawOval(BarvaInkoustu, Offset(cx + u * 1.1f, chodidlaY - u * 0.35f), Size(u * 1.3f, u * 0.55f))

    val kabat = Path().apply {
        moveTo(cx - u * 1.4f, ramenaY)
        lineTo(cx + u * 1.4f, ramenaY)
        lineTo(cx + u * 1.1f, pasY - u * 0.15f)
        lineTo(cx + u * 2.0f, pasY + u * 1.0f)
        lineTo(cx - u * 2.0f, pasY + u * 1.0f)
        lineTo(cx - u * 1.1f, pasY - u * 0.15f)
        close()
    }
    drawPath(kabat, color = barvaKabatu)
    drawPath(kabat, color = BarvaInkoustu, style = Stroke(width = 1.3f))
    drawLine(BarvaChromovaZluta, Offset(cx, ramenaY), Offset(cx, pasY + u * 0.9f), strokeWidth = 2.0f)
    drawLine(BarvaChromovaZluta, Offset(cx - u * 1.15f, pasY - u * 0.1f), Offset(cx + u * 1.15f, pasY - u * 0.1f), strokeWidth = 2.4f)

    // Výzbroj svrška (včetně bubeníka na zeleném svrškovi a dvou mečů na žaludovém)
    when (barva) {
        BarvaKarty.ZELENE -> {
            // Bubeník s velkým válcovým bubnem u boku a paličkami
            val bx = cx + u * 0.9f
            val by = pasY + u * 0.4f
            drawRoundRect(
                color = Color(0xFFD7CCC8),
                topLeft = Offset(bx - u * 1.3f, by - u * 1.1f),
                size = Size(u * 2.6f, u * 2.2f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawLine(BarvaChromovaZluta, Offset(bx - u * 1.3f, by - u * 0.9f), Offset(bx + u * 1.3f, by - u * 0.9f), strokeWidth = 2.6f)
            drawLine(BarvaChromovaZluta, Offset(bx - u * 1.3f, by + u * 0.9f), Offset(bx + u * 1.3f, by + u * 0.9f), strokeWidth = 2.6f)
            drawRoundRect(
                color = BarvaInkoustu,
                topLeft = Offset(bx - u * 1.3f, by - u * 1.1f),
                size = Size(u * 2.6f, u * 2.2f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.3f)
            )
            drawLine(BarvaKmene, Offset(cx - u * 2.0f, pasY - u * 0.6f), Offset(bx - u * 0.2f, by - u * 0.5f), strokeWidth = 1.8f)
        }
        BarvaKarty.ZALUDY -> {
            // Dva meče žaludového svrška
            drawLine(Color(0xFFCFD8DC), Offset(cx - u * 2.4f, pasY - u * 0.4f), Offset(cx + u * 2.4f, ramenaY - u * 0.6f), strokeWidth = 2.3f)
            drawLine(Color(0xFFCFD8DC), Offset(cx - u * 2.6f, kolenaY - u * 0.5f), Offset(cx + u * 2.5f, kolenaY - u * 0.5f), strokeWidth = 2.3f)
        }
        BarvaKarty.CERVENE -> {
            drawLine(BarvaKmene, Offset(cx - u * 3.0f, kolenaY), Offset(cx + u * 2.6f, hlavaY - u * 0.6f), strokeWidth = 2.8f)
        }
        BarvaKarty.KULE -> {
            drawLine(Color(0xFFCFD8DC), Offset(cx - u * 0.8f, pasY - u * 0.2f), Offset(cx + u * 2.8f, kolenaY + u * 0.8f), strokeWidth = 2.5f)
        }
    }

    drawOval(Color.White, Offset(cx - u * 1.0f, ramenaY - u * 0.30f), Size(u * 2.0f, u * 0.50f))
    vykresliHlavuPostavy(
        stred = Offset(cx, hlavaY),
        u = u,
        sPlnovousem = false,
        typPokryvky = if (barva == BarvaKarty.ZELENE) 2 else 0
    )
}

/**
 * Jednohlavý KRÁL: 2 znaky barvy nahoře po stranách koruny, sedící panovník na zlatém trůnu se žezlem.
 */
private fun DrawScope.vykresliKrale(
    barva: BarvaKarty,
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float
) {
    // Šrafovaná modro-bílá podlaha pod královským trůnem
    val podlahaTop = fy + fh * 0.82f
    drawRect(Color(0xFFDCE6F2), Offset(fx, podlahaTop), Size(fw, fy + fh - podlahaTop))
    var sx = fx - fh * 0.2f
    while (sx < fx + fw + fh * 0.2f) {
        drawLine(
            color = BarvaKobaltModra,
            start = Offset(sx, fy + fh),
            end = Offset(sx + fh * 0.16f, podlahaTop),
            strokeWidth = 1.4f
        )
        sx += fw * 0.06f
    }
    drawLine(BarvaInkoustu, Offset(fx, podlahaTop), Offset(fx + fw, podlahaTop), strokeWidth = 1.2f)

    // 2 znaky barvy v horních rozích po stranách krále
    val rZnak = fw * 0.132f
    vykresliZnakBarvy(barva, Offset(fx + fw * 0.18f, fy + fh * 0.105f), rZnak)
    vykresliZnakBarvy(barva, Offset(fx + fw * 0.82f, fy + fh * 0.105f), rZnak)

    val cx = fx + fw * 0.48f
    val u = fw * 0.10f

    // Zlatý barokní trůn s opěradlem a volutovými nohami
    val trun = Path().apply {
        moveTo(cx - u * 2.8f, fy + fh * 0.88f)
        lineTo(cx - u * 2.2f, fy + fh * 0.46f)
        lineTo(cx + u * 2.8f, fy + fh * 0.46f)
        lineTo(cx + u * 3.4f, fy + fh * 0.88f)
        close()
    }
    drawPath(trun, color = BarvaChromovaZluta)
    drawPath(trun, color = BarvaInkoustu, style = Stroke(width = 1.4f))

    // Sedící postava krále v bohatém rouchu a plášti
    val barvaRoucha = when (barva) {
        BarvaKarty.KULE -> BarvaRumelka
        BarvaKarty.CERVENE -> BarvaPurpurRuzova
        BarvaKarty.ZELENE -> BarvaSmaragdZelena
        BarvaKarty.ZALUDY -> BarvaRumelka
    }
    val barvaKabatu = when (barva) {
        BarvaKarty.KULE -> BarvaSmaragdZelena
        BarvaKarty.CERVENE -> BarvaSmaragdZelena
        BarvaKarty.ZELENE -> BarvaRumelka
        BarvaKarty.ZALUDY -> BarvaKobaltModra
    }

    val roucho = Path().apply {
        moveTo(cx - u * 1.6f, fy + fh * 0.28f)
        lineTo(cx + u * 1.6f, fy + fh * 0.28f)
        lineTo(cx + u * 2.7f, fy + fh * 0.62f)
        lineTo(cx + u * 2.2f, fy + fh * 0.84f)
        lineTo(cx - u * 2.0f, fy + fh * 0.84f)
        lineTo(cx - u * 2.3f, fy + fh * 0.56f)
        close()
    }
    drawPath(roucho, color = barvaRoucha)
    drawPath(roucho, color = BarvaInkoustu, style = Stroke(width = 1.4f))

    // Horní kabátec a zlatý lem na klíně
    drawRoundRect(
        color = barvaKabatu,
        topLeft = Offset(cx - u * 1.4f, fy + fh * 0.28f),
        size = Size(u * 2.8f, fh * 0.22f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawLine(
        color = BarvaChromovaZluta,
        start = Offset(cx - u * 2.0f, fy + fh * 0.68f),
        end = Offset(cx + u * 2.2f, fy + fh * 0.68f),
        strokeWidth = 3.2f
    )

    // Střevíce vykukující pod rouchem
    drawOval(BarvaKmene, Offset(cx - u * 1.4f, fy + fh * 0.83f), Size(u * 1.3f, u * 0.6f))
    drawOval(BarvaKmene, Offset(cx + u * 0.4f, fy + fh * 0.83f), Size(u * 1.3f, u * 0.6f))

    // Zlaté královské žezlo v ruce
    val zezloX = if (barva == BarvaKarty.CERVENE) cx - u * 2.3f else cx + u * 2.3f
    drawLine(
        color = BarvaChromovaZluta,
        start = Offset(zezloX - u * 0.3f, fy + fh * 0.54f),
        end = Offset(zezloX + u * 0.3f, fy + fh * 0.26f),
        strokeWidth = 3.0f
    )
    drawLine(
        color = BarvaInkoustu,
        start = Offset(zezloX - u * 0.3f, fy + fh * 0.54f),
        end = Offset(zezloX + u * 0.3f, fy + fh * 0.26f),
        strokeWidth = 0.9f
    )
    drawCircle(BarvaChromovaZluta, radius = u * 0.35f, center = Offset(zezloX + u * 0.34f, fy + fh * 0.24f))
    drawCircle(BarvaRumelka, radius = u * 0.16f, center = Offset(zezloX + u * 0.34f, fy + fh * 0.24f))

    // Hermelínový / zlatý límec, vousatá hlava a vysoká královská koruna
    drawOval(BarvaChromovaZluta, Offset(cx - u * 1.25f, fy + fh * 0.26f), Size(u * 2.5f, u * 0.55f))
    vykresliHlavuPostavy(
        stred = Offset(cx, fy + fh * 0.18f),
        u = u * 1.05f,
        sPlnovousem = true,
        typPokryvky = 3
    )
}

/**
 * Heraldická jednohlavá ESA českého obrazu: 2 velké znaky nahoře a bohatý erbovní motiv dole.
 */
private fun DrawScope.vykresliEso(
    barva: BarvaKarty,
    fx: Float,
    fy: Float,
    fw: Float,
    fh: Float
) {
    val cx = fx + fw * 0.5f
    val rZnak = fw * 0.145f

    // 2 znaky barvy nahoře vlevo a vpravo
    vykresliZnakBarvy(barva, Offset(fx + fw * 0.20f, fy + fh * 0.115f), rZnak)
    vykresliZnakBarvy(barva, Offset(fx + fw * 0.80f, fy + fh * 0.115f), rZnak)

    when (barva) {
        BarvaKarty.KULE -> {
            // Kulové eso: velká císařská koruna nad čtvrceným červeno-zlatým erbem se lvy
            vykresliZelenoZlutyPazit(fx, fy, fw, fh, vyskaPodilu = 0.08f)
            vykresliCeskouKorunu(Offset(cx, fy + fh * 0.27f), fw * 0.46f, fh * 0.15f)

            val erbTop = fy + fh * 0.36f
            val erbW = fw * 0.68f
            val erbH = fh * 0.50f
            val erb = Path().apply {
                moveTo(cx - erbW * 0.5f, erbTop)
                lineTo(cx + erbW * 0.5f, erbTop)
                lineTo(cx + erbW * 0.5f, erbTop + erbH * 0.68f)
                cubicTo(cx + erbW * 0.45f, erbTop + erbH * 0.92f, cx + erbW * 0.15f, erbTop + erbH, cx, erbTop + erbH * 1.05f)
                cubicTo(cx - erbW * 0.15f, erbTop + erbH, cx - erbW * 0.45f, erbTop + erbH * 0.92f, cx - erbW * 0.5f, erbTop + erbH * 0.68f)
                close()
            }
            drawPath(erb, color = BarvaChromovaZluta)
            // Čtvrcení štítu (vpravo nahoře a vlevo dole červené pole)
            drawRect(BarvaRumelka, Offset(cx, erbTop), Size(erbW * 0.5f, erbH * 0.48f))
            drawRect(BarvaRumelka, Offset(cx - erbW * 0.5f, erbTop + erbH * 0.48f), Size(erbW * 0.5f, erbH * 0.36f))
            drawLine(BarvaInkoustu, Offset(cx, erbTop), Offset(cx, erbTop + erbH), strokeWidth = 1.4f)
            drawLine(BarvaInkoustu, Offset(cx - erbW * 0.5f, erbTop + erbH * 0.48f), Offset(cx + erbW * 0.5f, erbTop + erbH * 0.48f), strokeWidth = 1.4f)
            drawCircle(BarvaKobaltModra, radius = fw * 0.07f, center = Offset(cx, erbTop + erbH * 0.48f))
            drawCircle(BarvaChromovaZluta, radius = fw * 0.07f, center = Offset(cx, erbTop + erbH * 0.48f), style = Stroke(width = 1.5f))
            drawPath(erb, color = BarvaChromovaZluta, style = Stroke(width = 3.0f))
            drawPath(erb, color = BarvaInkoustu, style = Stroke(width = 1.4f))
        }

        BarvaKarty.CERVENE -> {
            // Červené eso: zlatá stuha a oválný vavřínový věnec se zlatým lvem a ptáčkem
            vykresliZelenoZlutyPazit(fx, fy, fw, fh, vyskaPodilu = 0.07f)
            val venecStred = Offset(cx, fy + fh * 0.60f)
            val rx = fw * 0.35f
            val ry = fh * 0.28f

            drawOval(
                color = BarvaChromovaZluta,
                topLeft = Offset(cx - rx * 0.9f, fy + fh * 0.25f),
                size = Size(rx * 1.8f, fh * 0.08f)
            )
            drawOval(
                color = BarvaSmaragdZelena,
                topLeft = Offset(venecStred.x - rx, venecStred.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = fw * 0.075f)
            )
            drawOval(
                color = BarvaInkoustu,
                topLeft = Offset(venecStred.x - rx, venecStred.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1.2f)
            )
            // Zlatý štítek se lvem a ptáčkem uvnitř věnce
            drawOval(
                color = BarvaChromovaZluta,
                topLeft = Offset(cx - fw * 0.16f, fy + fh * 0.56f),
                size = Size(fw * 0.32f, fh * 0.22f)
            )
            drawOval(
                color = BarvaInkoustu,
                topLeft = Offset(cx - fw * 0.16f, fy + fh * 0.56f),
                size = Size(fw * 0.32f, fh * 0.22f),
                style = Stroke(width = 1.2f)
            )
            drawCircle(BarvaRumelka, radius = fw * 0.055f, center = Offset(cx, fy + fh * 0.66f))
            drawCircle(BarvaSmaragdZelena, radius = fw * 0.04f, center = Offset(cx, fy + fh * 0.44f))
        }

        BarvaKarty.ZELENE -> {
            // Zelené eso: žlutý květ mezi listy nahoře a purpurovo-zlatá barokní kartuš
            drawCircle(BarvaChromovaZluta, radius = fw * 0.065f, center = Offset(cx, fy + fh * 0.10f))
            drawCircle(BarvaRumelka, radius = fw * 0.028f, center = Offset(cx, fy + fh * 0.10f))
            drawLine(BarvaKmene, Offset(cx, fy + fh * 0.16f), Offset(cx, fy + fh * 0.36f), strokeWidth = 2.4f)

            val kartus = Path().apply {
                moveTo(cx, fy + fh * 0.34f)
                cubicTo(cx + fw * 0.42f, fy + fh * 0.34f, cx + fw * 0.44f, fy + fh * 0.72f, cx, fy + fh * 0.88f)
                cubicTo(cx - fw * 0.44f, fy + fh * 0.72f, cx - fw * 0.42f, fy + fh * 0.34f, cx, fy + fh * 0.34f)
                close()
            }
            drawPath(kartus, color = BarvaRumelka)
            drawPath(kartus, color = BarvaChromovaZluta, style = Stroke(width = 3.2f))
            drawPath(kartus, color = BarvaInkoustu, style = Stroke(width = 1.3f))

            drawOval(
                color = BarvaChromovaZluta,
                topLeft = Offset(cx - fw * 0.20f, fy + fh * 0.44f),
                size = Size(fw * 0.40f, fh * 0.32f)
            )
            drawOval(
                color = BarvaInkoustu,
                topLeft = Offset(cx - fw * 0.20f, fy + fh * 0.44f),
                size = Size(fw * 0.40f, fh * 0.32f),
                style = Stroke(width = 1.2f)
            )
            drawRect(BarvaSmaragdZelena, Offset(cx - fw * 0.04f, fy + fh * 0.54f), Size(fw * 0.08f, fh * 0.14f))
        }

        BarvaKarty.ZALUDY -> {
            // Žaludské eso: klenutá žlutá stuha a sedící strážný lev držící dva zlaté štíty v tlapách
            drawCircle(BarvaRumelka, radius = fw * 0.045f, center = Offset(cx, fy + fh * 0.10f))
            drawOval(
                color = BarvaChromovaZluta,
                topLeft = Offset(fx + fw * 0.10f, fy + fh * 0.24f),
                size = Size(fw * 0.80f, fh * 0.09f)
            )
            drawOval(
                color = BarvaInkoustu,
                topLeft = Offset(fx + fw * 0.10f, fy + fh * 0.24f),
                size = Size(fw * 0.80f, fh * 0.09f),
                style = Stroke(width = 1.1f)
            )

            // Hříva a tělo sedícího lva
            val teloLva = Path().apply {
                moveTo(cx - fw * 0.34f, fy + fh * 0.88f)
                cubicTo(cx - fw * 0.38f, fy + fh * 0.56f, cx - fw * 0.24f, fy + fh * 0.42f, cx, fy + fh * 0.42f)
                cubicTo(cx + fw * 0.24f, fy + fh * 0.42f, cx + fw * 0.38f, fy + fh * 0.56f, cx + fw * 0.34f, fy + fh * 0.88f)
                close()
            }
            drawPath(teloLva, color = Color(0xFF9C3622))
            drawPath(teloLva, color = BarvaInkoustu, style = Stroke(width = 1.4f))
            drawCircle(Color(0xFFB73225), radius = fw * 0.16f, center = Offset(cx, fy + fh * 0.48f))
            drawCircle(BarvaInkoustu, radius = fw * 0.16f, center = Offset(cx, fy + fh * 0.48f), style = Stroke(width = 1.2f))
            drawCircle(BarvaChromovaZluta, radius = fw * 0.022f, center = Offset(cx - fw * 0.055f, fy + fh * 0.46f))
            drawCircle(BarvaChromovaZluta, radius = fw * 0.022f, center = Offset(cx + fw * 0.055f, fy + fh * 0.46f))

            // Dva zlaté erbovní štíty v levé a pravé tlapě
            for (posunX in listOf(-fw * 0.23f, fw * 0.23f)) {
                drawOval(
                    color = BarvaChromovaZluta,
                    topLeft = Offset(cx + posunX - fw * 0.12f, fy + fh * 0.68f),
                    size = Size(fw * 0.24f, fh * 0.22f)
                )
                drawOval(
                    color = BarvaInkoustu,
                    topLeft = Offset(cx + posunX - fw * 0.12f, fy + fh * 0.68f),
                    size = Size(fw * 0.24f, fh * 0.22f),
                    style = Stroke(width = 1.3f)
                )
            }
        }
    }
}

private fun DrawScope.vykresliHlavuPostavy(
    stred: Offset,
    u: Float,
    sPlnovousem: Boolean,
    typPokryvky: Int
) {
    // Polodlouhé hnědé vlasy po stranách hlavy
    drawCircle(BarvaVlasu, radius = u * 0.96f, center = Offset(stred.x, stred.y + u * 0.10f))

    // Obličej s ruměncem a perokresebnou konturou
    drawOval(
        color = BarvaPleti,
        topLeft = Offset(stred.x - u * 0.72f, stred.y - u * 0.82f),
        size = Size(u * 1.44f, u * 1.64f)
    )
    drawCircle(BarvaTvari, radius = u * 0.22f, center = Offset(stred.x - u * 0.34f, stred.y + u * 0.12f))
    drawCircle(BarvaTvari, radius = u * 0.22f, center = Offset(stred.x + u * 0.34f, stred.y + u * 0.12f))
    drawOval(
        color = BarvaInkoustu,
        topLeft = Offset(stred.x - u * 0.72f, stred.y - u * 0.82f),
        size = Size(u * 1.44f, u * 1.64f),
        style = Stroke(width = 1.1f)
    )

    // Oči, nos a ústa / plnovous
    drawCircle(BarvaInkoustu, radius = u * 0.09f, center = Offset(stred.x - u * 0.26f, stred.y - u * 0.14f))
    drawCircle(BarvaInkoustu, radius = u * 0.09f, center = Offset(stred.x + u * 0.26f, stred.y - u * 0.14f))
    drawLine(BarvaInkoustu, Offset(stred.x, stred.y - u * 0.16f), Offset(stred.x - u * 0.06f, stred.y + u * 0.18f), strokeWidth = 1.0f)

    if (sPlnovousem) {
        val vous = Path().apply {
            moveTo(stred.x - u * 0.66f, stred.y + u * 0.20f)
            cubicTo(stred.x - u * 0.62f, stred.y + u * 1.25f, stred.x + u * 0.62f, stred.y + u * 1.25f, stred.x + u * 0.66f, stred.y + u * 0.20f)
            close()
        }
        drawPath(vous, color = BarvaVlasu)
        drawPath(vous, color = BarvaInkoustu, style = Stroke(width = 1.0f))
    } else {
        drawLine(BarvaRumelkaTmava, Offset(stred.x - u * 0.20f, stred.y + u * 0.40f), Offset(stred.x + u * 0.20f, stred.y + u * 0.40f), strokeWidth = 1.3f)
    }

    when (typPokryvky) {
        1 -> {
            // Růžovo-červený klobouk flétnisty
            drawOval(BarvaPurpurRuzova, Offset(stred.x - u * 1.25f, stred.y - u * 1.15f), Size(u * 2.5f, u * 0.72f))
            drawOval(BarvaInkoustu, Offset(stred.x - u * 1.25f, stred.y - u * 1.15f), Size(u * 2.5f, u * 0.72f), style = Stroke(width = 1.1f))
        }
        2 -> {
            // Červený baret bubeníka
            drawOval(BarvaRumelka, Offset(stred.x - u * 1.15f, stred.y - u * 1.12f), Size(u * 2.3f, u * 0.68f))
            drawOval(BarvaInkoustu, Offset(stred.x - u * 1.15f, stred.y - u * 1.12f), Size(u * 2.3f, u * 0.68f), style = Stroke(width = 1.1f))
        }
        3 -> {
            // Vysoká červeno-zlatá koruna českých králů
            vykresliCeskouKorunu(Offset(stred.x, stred.y - u * 1.10f), u * 2.0f, u * 1.15f)
        }
    }
}

private fun DrawScope.vykresliCeskouKorunu(stred: Offset, sirka: Float, vyska: Float) {
    val polS = sirka * 0.5f
    val polV = vyska * 0.5f
    // Červená mitrová čapka uvnitř zlaté koruny
    drawOval(
        color = BarvaRumelka,
        topLeft = Offset(stred.x - polS * 0.95f, stred.y - polV * 0.85f),
        size = Size(sirka * 0.95f, vyska * 1.35f)
    )
    drawOval(
        color = BarvaInkoustu,
        topLeft = Offset(stred.x - polS * 0.95f, stred.y - polV * 0.85f),
        size = Size(sirka * 0.95f, vyska * 1.35f),
        style = Stroke(width = 1.1f)
    )
    // Zlatá obroučka a středový oblouk s jablkem
    drawRoundRect(
        color = BarvaChromovaZluta,
        topLeft = Offset(stred.x - polS, stred.y + polV * 0.15f),
        size = Size(sirka, vyska * 0.36f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = BarvaInkoustu,
        topLeft = Offset(stred.x - polS, stred.y + polV * 0.15f),
        size = Size(sirka, vyska * 0.36f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1.1f)
    )
    drawLine(BarvaChromovaZluta, Offset(stred.x, stred.y - polV * 0.95f), Offset(stred.x, stred.y + polV * 0.20f), strokeWidth = 2.4f)
    drawCircle(BarvaChromovaZluta, radius = sirka * 0.09f, center = Offset(stred.x, stred.y - polV * 1.02f))
}

/**
 * Vodorovný žalud vyrůstající z kmene na číselných kartách 7, 8, 9, 10 (zelená číška u kmene,
 * červeno-žlutý plod směřující ven).
 */
private fun DrawScope.vykresliVodorovnyZalud(
    uchytKmen: Offset,
    delka: Float,
    smerDoprava: Boolean
) {
    val zn = if (smerDoprava) 1f else -1f
    val vyskaPlodu = delka * 0.46f
    val sirkaCisky = delka * 0.30f
    val sirkaPlodu = delka * 0.68f

    val ciskaX = if (smerDoprava) uchytKmen.x else uchytKmen.x - sirkaCisky
    val plodX = if (smerDoprava) uchytKmen.x + sirkaCisky * 0.85f else uchytKmen.x - sirkaCisky * 0.85f - sirkaPlodu

    // Horní červená a spodní žlutá polovina plodu žaludu
    drawRoundRect(
        color = BarvaRumelka,
        topLeft = Offset(plodX, uchytKmen.y - vyskaPlodu * 0.5f),
        size = Size(sirkaPlodu, vyskaPlodu * 0.52f),
        cornerRadius = CornerRadius(vyskaPlodu * 0.45f, vyskaPlodu * 0.45f)
    )
    drawRoundRect(
        color = BarvaChromovaZluta,
        topLeft = Offset(plodX, uchytKmen.y),
        size = Size(sirkaPlodu, vyskaPlodu * 0.50f),
        cornerRadius = CornerRadius(vyskaPlodu * 0.45f, vyskaPlodu * 0.45f)
    )
    drawRoundRect(
        color = BarvaInkoustu,
        topLeft = Offset(plodX, uchytKmen.y - vyskaPlodu * 0.5f),
        size = Size(sirkaPlodu, vyskaPlodu),
        cornerRadius = CornerRadius(vyskaPlodu * 0.45f, vyskaPlodu * 0.45f),
        style = Stroke(width = 1.1f)
    )

    // Zelená číška u kmene
    drawRoundRect(
        color = BarvaSmaragdZelena,
        topLeft = Offset(ciskaX, uchytKmen.y - vyskaPlodu * 0.58f),
        size = Size(sirkaCisky, vyskaPlodu * 1.16f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = BarvaInkoustu,
        topLeft = Offset(ciskaX, uchytKmen.y - vyskaPlodu * 0.58f),
        size = Size(sirkaCisky, vyskaPlodu * 1.16f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1.1f)
    )
    drawCircle(
        color = BarvaInkoustu,
        radius = 1.1f,
        center = Offset(uchytKmen.x + zn * (sirkaCisky * 0.85f + sirkaPlodu), uchytKmen.y)
    )
}

/**
 * Svislé znaky barev podle českého jednohlavého obrazu:
 * - Kule: rumělkově červená rolnička se zlatým odleskem a klikatým žluto-bílým pásem.
 * - Červené: plné rumělkové srdce s klenutým výkrojem a tmavou linkou.
 * - Zelené: smaragdový lipový list se špičkou nahoru, světlou žilkou a červenohnědou stopkou.
 * - Žaludy: svislý žluto-červený plod v zelené šrafované číšce se stopkou a lístkem.
 */
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

            // Charakteristický žlutý středový pás s klikatou kresbou
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
                drawPath(
                    path = klikatka,
                    color = BarvaInkoustu,
                    style = Stroke(width = 1.2f, join = StrokeJoin.Round)
                )
            }
            drawLine(BarvaInkoustu, Offset(stred.x - r * 0.96f, stred.y - r * 0.26f), Offset(stred.x + r * 0.96f, stred.y - r * 0.26f), strokeWidth = 1.1f)
            drawLine(BarvaInkoustu, Offset(stred.x - r * 0.96f, stred.y + r * 0.26f), Offset(stred.x + r * 0.96f, stred.y + r * 0.26f), strokeWidth = 1.1f)

            // Spodní rezonanční otvor a ouško kule
            drawCircle(BarvaInkoustu, radius = r * 0.16f, center = Offset(stred.x, stred.y + r * 0.60f))
            drawCircle(BarvaChromovaZluta, radius = r * 0.18f, center = Offset(stred.x, stred.y + r * 1.02f))
            drawCircle(BarvaInkoustu, radius = r * 0.18f, center = Offset(stred.x, stred.y + r * 1.02f), style = Stroke(width = 1.0f))
            drawCircle(BarvaInkoustu, radius = r, center = stred, style = Stroke(width = 1.3f))
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
            drawPath(srdce, color = BarvaInkoustu, style = Stroke(width = 1.3f))
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
            drawLine(BarvaZlutaSvetla, Offset(stred.x, stred.y - polomer * 0.80f), Offset(stred.x, stred.y + polomer * 0.60f), strokeWidth = 1.3f)
            for (v in -1..1) {
                val vy = stred.y + v * polomer * 0.25f
                drawLine(Color(0xBBCEE8B4), Offset(stred.x, vy + polomer * 0.08f), Offset(stred.x - polomer * 0.50f, vy - polomer * 0.12f), strokeWidth = 1.0f)
                drawLine(Color(0x99CEE8B4), Offset(stred.x, vy + polomer * 0.08f), Offset(stred.x + polomer * 0.50f, vy - polomer * 0.12f), strokeWidth = 1.0f)
            }
            drawLine(BarvaKmene, Offset(stred.x, stred.y + polomer * 0.60f), Offset(stred.x + polomer * 0.18f, stred.y + polomer * 1.02f), strokeWidth = 1.8f)
            drawPath(list, color = BarvaInkoustu, style = Stroke(width = 1.3f))
        }

        BarvaKarty.ZALUDY -> {
            // Svislý český žalud: nahoře žluto-červený plod, dole zelená číška se stopkou a lístkem
            val plodW = polomer * 0.96f
            val plodH = polomer * 1.08f
            val plodTop = stred.y - polomer * 0.92f

            // Levá žlutá a pravá červená polovina plodu
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
                style = Stroke(width = 1.2f)
            )

            // Zelená číška dole
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
                style = Stroke(width = 1.2f)
            )

            // Stopka a drobný zelený lístek dole
            drawLine(
                color = BarvaKmene,
                start = Offset(stred.x, ciskaTop + ciskaH),
                end = Offset(stred.x, stred.y + polomer * 0.98f),
                strokeWidth = 1.8f
            )
            drawOval(
                color = BarvaSmaragdZelena,
                topLeft = Offset(stred.x - polomer * 0.52f, stred.y + polomer * 0.62f),
                size = Size(polomer * 0.46f, polomer * 0.24f)
            )
        }
    }
}
