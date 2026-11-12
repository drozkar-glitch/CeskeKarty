package cz.mares.ceskekarty

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val BarvaPapiruKarty = Color(0xFFFFFDF6)
private val BarvaPapiruStin = Color(0xFFF3EAD6)
private val BarvaCervenaSrdce = Color(0xFFC62828)
private val BarvaCervenaTmava = Color(0xFF8E0000)
private val BarvaKuleZlata = Color(0xFFD99B16)
private val BarvaKuleSpodni = Color(0xFFC85A17)
private val BarvaZaludZelena = Color(0xFF388E3C)
private val BarvaZaludHneda = Color(0xFF795548)
private val BarvaZelenaList = Color(0xFF1B7A34)
private val BarvaZlataKoruna = Color(0xFFE5B11E)
private val BarvaPovoleneZare = Color(0xFF69F0AE)
private val BarvaVybraneZare = Color(0xFFFFD54F)

fun barvaProZnakKarty(barva: BarvaKarty): Color {
    return when (barva) {
        BarvaKarty.CERVENE -> BarvaCervenaSrdce
        BarvaKarty.KULE -> Color(0xFFB26A00)
        BarvaKarty.ZALUDY -> Color(0xFF5D4037)
        BarvaKarty.ZELENE -> BarvaZelenaList
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
    sirka: Dp = 72.dp,
    jePovolena: Boolean = true,
    aktivniKontrolaTahu: Boolean = false,
    jeVybrana: Boolean = false,
    stitekNavic: String? = null,
    onClick: (() -> Unit)? = null
) {
    val vyska = sirka * 1.52f
    val tvar = RoundedCornerShape(9.dp)

    val cilovyPosunY = when {
        jeVybrana -> (-14).dp
        aktivniKontrolaTahu && jePovolena -> (-9).dp
        else -> 0.dp
    }
    val animovanyPosunY by animateDpAsState(targetValue = cilovyPosunY)

    val okrajBarva = when {
        jeVybrana -> BarvaVybraneZare
        aktivniKontrolaTahu && jePovolena -> BarvaPovoleneZare
        else -> Color(0xFFB8A98A)
    }
    val okrajTloustka = when {
        jeVybrana -> 2.8.dp
        aktivniKontrolaTahu && jePovolena -> 2.4.dp
        else -> 1.2.dp
    }

    val muzeKliknout = onClick != null && (!aktivniKontrolaTahu || jePovolena)

    Box(
        modifier = modifier
            .offset(y = animovanyPosunY)
            .size(width = sirka, height = vyska)
            .shadow(
                elevation = if (jeVybrana || (aktivniKontrolaTahu && jePovolena)) 8.dp else 3.dp,
                shape = tvar
            )
            .clip(tvar)
            .background(
                Brush.verticalGradient(
                    colors = listOf(BarvaPapiruKarty, BarvaPapiruStin)
                )
            )
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
            val vnitrniOdsazeni = size.width * 0.07f
            drawRoundRect(
                color = Color(0x339E8B66),
                topLeft = Offset(vnitrniOdsazeni, vnitrniOdsazeni),
                size = Size(size.width - vnitrniOdsazeni * 2f, size.height - vnitrniOdsazeni * 2f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 1.4f)
            )
            vykresliStredJednohlaveKarty(karta)
        }

        val barvaPisma = barvaProZnakKarty(karta.barva)
        val velikostPismaRohu = if (sirka < 62.dp) 11.sp else 13.sp
        val velikostIkonyRohu = if (sirka < 62.dp) 12.dp else 15.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 5.dp, top = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = karta.hodnota.kratkyZnak,
                color = barvaPisma,
                fontSize = velikostPismaRohu,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = velikostPismaRohu
            )
            IkonaBarvyKarty(barva = karta.barva, velikost = velikostIkonyRohu)
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 5.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IkonaBarvyKarty(barva = karta.barva, velikost = velikostIkonyRohu * 0.85f)
            Spacer(modifier = Modifier.width(1.dp))
            Text(
                text = karta.hodnota.kratkyZnak,
                color = barvaPisma,
                fontSize = velikostPismaRohu * 0.9f,
                fontWeight = FontWeight.Bold
            )
        }

        if (!stitekNavic.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xDD1B5E20))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = stitekNavic,
                    color = Color(0xFFFFF59D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (aktivniKontrolaTahu && !jePovolena) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x94101613))
            )
        }
    }
}

@Composable
fun KartaRubComposable(
    modifier: Modifier = Modifier,
    sirka: Dp = 58.dp,
    pocetVBalicku: Int? = null,
    popisek: String? = null,
    zvyseneZvyrazneni: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val vyska = sirka * 1.52f
    val tvar = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(width = sirka, height = vyska)
            .shadow(4.dp, tvar)
            .clip(tvar)
            .background(Color(0xFFFFFBF0))
            .border(
                width = if (zvyseneZvyrazneni) 2.4.dp else 1.4.dp,
                color = if (zvyseneZvyrazneni) BarvaPovoleneZare else Color(0xFFC2B292),
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
            val pad = size.width * 0.08f
            val sirkaVnitrni = size.width - pad * 2f
            val vyskaVnitrni = size.height - pad * 2f

            drawRoundRect(
                color = Color(0xFF8B1E24),
                topLeft = Offset(pad, pad),
                size = Size(sirkaVnitrni, vyskaVnitrni),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = Color(0xFFE6C15A),
                topLeft = Offset(pad + 3f, pad + 3f),
                size = Size(sirkaVnitrni - 6f, vyskaVnitrni - 6f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 1.8f)
            )

            val stred = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color(0xFF5D1015),
                radius = min(sirkaVnitrni, vyskaVnitrni) * 0.30f,
                center = stred
            )
            drawCircle(
                color = Color(0xFFE6C15A),
                radius = min(sirkaVnitrni, vyskaVnitrni) * 0.30f,
                center = stred,
                style = Stroke(width = 1.6f)
            )
        }

        if (pocetVBalicku != null || !popisek.isNullOrEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC121212))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                if (pocetVBalicku != null) {
                    Text(
                        text = pocetVBalicku.toString(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (!popisek.isNullOrEmpty()) {
                    Text(
                        text = popisek,
                        color = Color(0xFFFFE082),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun DrawScope.vykresliStredJednohlaveKarty(karta: Karta) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val zakladniR = min(size.width, size.height) * 0.17f

    when (karta.hodnota) {
        HodnotaKarty.ESO -> {
            // Slavnostní věnec a velký středový znak esa
            drawCircle(
                color = Color(0x28D4AF37),
                radius = zakladniR * 1.65f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = BarvaZlataKoruna,
                radius = zakladniR * 1.55f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.4f)
            )
            vykresliKorunku(Offset(cx, cy - zakladniR * 1.55f), zakladniR * 0.9f)
            vykresliZnakBarvy(karta.barva, Offset(cx, cy + zakladniR * 0.1f), zakladniR * 1.25f)
        }

        HodnotaKarty.KRAL -> {
            // Jednohlavá královská figura s korunou a pláštěm
            vykresliFiguruPostavy(
                stred = Offset(cx, cy + zakladniR * 0.35f),
                meritko = zakladniR,
                barvaPlaste = Color(0xFF9C1F26),
                barvaLemovani = BarvaZlataKoruna,
                typHlavy = 3
            )
            vykresliZnakBarvy(
                barva = karta.barva,
                stred = Offset(cx + zakladniR * 0.92f, cy - zakladniR * 1.15f),
                polomer = zakladniR * 0.65f
            )
        }

        HodnotaKarty.SVRSEK -> {
            // Svršek má znak barvy nahoře u hlavy a klobouk s pérem
            vykresliFiguruPostavy(
                stred = Offset(cx, cy + zakladniR * 0.40f),
                meritko = zakladniR,
                barvaPlaste = Color(0xFF1F5B3A),
                barvaLemovani = Color(0xFFD9A738),
                typHlavy = 2
            )
            vykresliZnakBarvy(
                barva = karta.barva,
                stred = Offset(cx + zakladniR * 0.90f, cy - zakladniR * 1.25f),
                polomer = zakladniR * 0.62f
            )
        }

        HodnotaKarty.SPODEK -> {
            // Spodek má znak barvy ve spodní polovině karty
            vykresliFiguruPostavy(
                stred = Offset(cx, cy + zakladniR * 0.15f),
                meritko = zakladniR * 0.95f,
                barvaPlaste = Color(0xFF2B4C7E),
                barvaLemovani = Color(0xFFC59B27),
                typHlavy = 1
            )
            vykresliZnakBarvy(
                barva = karta.barva,
                stred = Offset(cx + zakladniR * 0.85f, cy + zakladniR * 1.35f),
                polomer = zakladniR * 0.62f
            )
        }

        HodnotaKarty.R10 -> {
            // Římské X uprostřed + 4 znaky v rozích středu
            val dx = size.width * 0.21f
            val dy = size.height * 0.21f
            val rMal = zakladniR * 0.62f
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy - dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy - dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy + dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy + dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx, cy), zakladniR * 0.88f)
        }

        HodnotaKarty.R9 -> {
            val dx = size.width * 0.20f
            val dy = size.height * 0.20f
            val rMal = zakladniR * 0.60f
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy - dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy - dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx, cy), zakladniR * 0.72f)
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy + dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy + dy), rMal)
        }

        HodnotaKarty.R8 -> {
            val dx = size.width * 0.20f
            val dy = size.height * 0.18f
            val rMal = zakladniR * 0.64f
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy - dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy - dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy + dy), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy + dy), rMal)
        }

        HodnotaKarty.R7 -> {
            val dx = size.width * 0.19f
            val dy = size.height * 0.19f
            val rMal = zakladniR * 0.66f
            vykresliZnakBarvy(karta.barva, Offset(cx, cy - dy * 0.95f), zakladniR * 0.75f)
            vykresliZnakBarvy(karta.barva, Offset(cx - dx, cy + dy * 0.75f), rMal)
            vykresliZnakBarvy(karta.barva, Offset(cx + dx, cy + dy * 0.75f), rMal)
        }
    }
}

private fun DrawScope.vykresliFiguruPostavy(
    stred: Offset,
    meritko: Float,
    barvaPlaste: Color,
    barvaLemovani: Color,
    typHlavy: Int
) {
    // Tělo / plášť figury
    val plast = Path().apply {
        moveTo(stred.x - meritko * 1.25f, stred.y + meritko * 1.45f)
        lineTo(stred.x - meritko * 0.80f, stred.y - meritko * 0.25f)
        lineTo(stred.x + meritko * 0.80f, stred.y - meritko * 0.25f)
        lineTo(stred.x + meritko * 1.25f, stred.y + meritko * 1.45f)
        close()
    }
    drawPath(plast, color = barvaPlaste)
    drawPath(plast, color = barvaLemovani, style = Stroke(width = 2.2f))

    // Zlatý pás na hrudi
    drawLine(
        color = barvaLemovani,
        start = Offset(stred.x - meritko * 0.65f, stred.y + meritko * 0.45f),
        end = Offset(stred.x + meritko * 0.65f, stred.y + meritko * 0.45f),
        strokeWidth = 3f
    )

    // Hlava figury
    val hlavaStred = Offset(stred.x, stred.y - meritko * 0.75f)
    drawCircle(
        color = Color(0xFFF5D6B8),
        radius = meritko * 0.48f,
        center = hlavaStred
    )
    drawCircle(
        color = Color(0xFF6D4C41),
        radius = meritko * 0.48f,
        center = hlavaStred,
        style = Stroke(width = 1.4f)
    )

    when (typHlavy) {
        3 -> {
            // Královská koruna + žezlo
            vykresliKorunku(Offset(hlavaStred.x, hlavaStred.y - meritko * 0.56f), meritko * 0.85f)
            drawLine(
                color = BarvaZlataKoruna,
                start = Offset(stred.x - meritko * 0.95f, stred.y + meritko * 0.9f),
                end = Offset(stred.x - meritko * 0.55f, stred.y - meritko * 0.55f),
                strokeWidth = 2.8f
            )
            drawCircle(
                color = BarvaZlataKoruna,
                radius = meritko * 0.16f,
                center = Offset(stred.x - meritko * 0.55f, stred.y - meritko * 0.62f)
            )
        }
        2 -> {
            // Klobouk svrška s pérem
            drawRoundRect(
                color = Color(0xFF2E5B3C),
                topLeft = Offset(hlavaStred.x - meritko * 0.65f, hlavaStred.y - meritko * 0.68f),
                size = Size(meritko * 1.3f, meritko * 0.34f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            val pero = Path().apply {
                moveTo(hlavaStred.x + meritko * 0.25f, hlavaStred.y - meritko * 0.62f)
                cubicTo(
                    hlavaStred.x + meritko * 0.85f,
                    hlavaStred.y - meritko * 1.15f,
                    hlavaStred.x + meritko * 0.95f,
                    hlavaStred.y - meritko * 0.70f,
                    hlavaStred.x + meritko * 0.40f,
                    hlavaStred.y - meritko * 0.48f
                )
            }
            drawPath(pero, color = Color(0xFFE53935))
        }
        else -> {
            // Čapka spodka
            drawRoundRect(
                color = Color(0xFF3949AB),
                topLeft = Offset(hlavaStred.x - meritko * 0.56f, hlavaStred.y - meritko * 0.62f),
                size = Size(meritko * 1.12f, meritko * 0.28f),
                cornerRadius = CornerRadius(5f, 5f)
            )
        }
    }
}

private fun DrawScope.vykresliKorunku(stred: Offset, sirka: Float) {
    val polovina = sirka * 0.5f
    val vyska = sirka * 0.52f
    val koruna = Path().apply {
        moveTo(stred.x - polovina, stred.y + vyska * 0.45f)
        lineTo(stred.x - polovina * 1.05f, stred.y - vyska * 0.35f)
        lineTo(stred.x - polovina * 0.45f, stred.y + vyska * 0.05f)
        lineTo(stred.x, stred.y - vyska * 0.55f)
        lineTo(stred.x + polovina * 0.45f, stred.y + vyska * 0.05f)
        lineTo(stred.x + polovina * 1.05f, stred.y - vyska * 0.35f)
        lineTo(stred.x + polovina, stred.y + vyska * 0.45f)
        close()
    }
    drawPath(koruna, color = BarvaZlataKoruna)
    drawPath(koruna, color = Color(0xFF8D6E11), style = Stroke(width = 1.4f))
}

private fun DrawScope.vykresliZnakBarvy(
    barva: BarvaKarty,
    stred: Offset,
    polomer: Float
) {
    when (barva) {
        BarvaKarty.CERVENE -> {
            val srdce = Path().apply {
                moveTo(stred.x, stred.y + polomer * 0.95f)
                cubicTo(
                    stred.x - polomer * 1.25f,
                    stred.y + polomer * 0.20f,
                    stred.x - polomer * 1.15f,
                    stred.y - polomer * 0.95f,
                    stred.x,
                    stred.y - polomer * 0.30f
                )
                cubicTo(
                    stred.x + polomer * 1.15f,
                    stred.y - polomer * 0.95f,
                    stred.x + polomer * 1.25f,
                    stred.y + polomer * 0.20f,
                    stred.x,
                    stred.y + polomer * 0.95f
                )
                close()
            }
            drawPath(srdce, color = BarvaCervenaSrdce)
            drawPath(srdce, color = BarvaCervenaTmava, style = Stroke(width = 1.3f))
        }

        BarvaKarty.KULE -> {
            // Česká rolnička (kule) s prstencem a drážkou
            drawCircle(
                color = BarvaKuleZlata,
                radius = polomer * 0.88f,
                center = stred
            )
            val spodniPolokoule = Path().apply {
                moveTo(stred.x - polomer * 0.85f, stred.y + polomer * 0.08f)
                cubicTo(
                    stred.x - polomer * 0.75f,
                    stred.y + polomer * 0.95f,
                    stred.x + polomer * 0.75f,
                    stred.y + polomer * 0.95f,
                    stred.x + polomer * 0.85f,
                    stred.y + polomer * 0.08f
                )
                close()
            }
            drawPath(spodniPolokoule, color = BarvaKuleSpodni)
            drawLine(
                color = Color(0xFF5D3A00),
                start = Offset(stred.x - polomer * 0.88f, stred.y),
                end = Offset(stred.x + polomer * 0.88f, stred.y),
                strokeWidth = 2.0f
            )
            drawCircle(
                color = Color(0xFF3E2723),
                radius = polomer * 0.16f,
                center = Offset(stred.x, stred.y + polomer * 0.42f)
            )
            drawCircle(
                color = Color(0xFF6D4C00),
                radius = polomer * 0.88f,
                center = stred,
                style = Stroke(width = 1.4f)
            )
        }

        BarvaKarty.ZALUDY -> {
            // Plod žaludu + hnědá čepička se stopkou
            val plod = Path().apply {
                moveTo(stred.x - polomer * 0.62f, stred.y - polomer * 0.15f)
                cubicTo(
                    stred.x - polomer * 0.72f,
                    stred.y + polomer * 0.75f,
                    stred.x + polomer * 0.72f,
                    stred.y + polomer * 0.75f,
                    stred.x + polomer * 0.62f,
                    stred.y - polomer * 0.15f
                )
                close()
            }
            drawPath(plod, color = BarvaZaludZelena)
            drawPath(plod, color = Color(0xFF1B5E20), style = Stroke(width = 1.3f))

            drawRoundRect(
                color = BarvaZaludHneda,
                topLeft = Offset(stred.x - polomer * 0.76f, stred.y - polomer * 0.52f),
                size = Size(polomer * 1.52f, polomer * 0.44f),
                cornerRadius = CornerRadius(polomer * 0.2f, polomer * 0.2f)
            )
            drawLine(
                color = BarvaZaludHneda,
                start = Offset(stred.x, stred.y - polomer * 0.52f),
                end = Offset(stred.x, stred.y - polomer * 0.92f),
                strokeWidth = 2.2f
            )
        }

        BarvaKarty.ZELENE -> {
            // Lipový zelený list s žilkou a stopkou
            val list = Path().apply {
                moveTo(stred.x, stred.y - polomer * 0.95f)
                cubicTo(
                    stred.x + polomer * 1.15f,
                    stred.y - polomer * 0.35f,
                    stred.x + polomer * 0.95f,
                    stred.y + polomer * 0.65f,
                    stred.x,
                    stred.y + polomer * 0.72f
                )
                cubicTo(
                    stred.x - polomer * 0.95f,
                    stred.y + polomer * 0.65f,
                    stred.x - polomer * 1.15f,
                    stred.y - polomer * 0.35f,
                    stred.x,
                    stred.y - polomer * 0.95f
                )
                close()
            }
            drawPath(list, color = BarvaZelenaList)
            drawPath(list, color = Color(0xFF0D471C), style = Stroke(width = 1.3f))
            drawLine(
                color = Color(0xFFA5D6A7),
                start = Offset(stred.x, stred.y - polomer * 0.68f),
                end = Offset(stred.x, stred.y + polomer * 0.96f),
                strokeWidth = 1.8f
            )
        }
    }
}
