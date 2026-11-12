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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private val BarvaSlonovinySvetla = Color(0xFFFFFEFA)
private val BarvaSlonovinyStred = Color(0xFFF9F1DD)
private val BarvaSlonovinyOkraj = Color(0xFFEDE0C2)

private val BarvaRamuCervena = Color(0xFFA62626)
private val BarvaRamuZlata = Color(0xFFC5962C)

private val BarvaPovoleneZare = Color(0xFF69F0AE)
private val BarvaVybraneZare = Color(0xFFFFD54F)

fun barvaProZnakKarty(barva: BarvaKarty): Color {
    return when (barva) {
        BarvaKarty.CERVENE -> Color(0xFFC62828)
        BarvaKarty.KULE -> Color(0xFFAD6200)
        BarvaKarty.ZALUDY -> Color(0xFF5D3A1A)
        BarvaKarty.ZELENE -> Color(0xFF156E2C)
    }
}

/**
 * Zelený hospodský karetní stůl s mahagonovou dřevěnou obrubou, mosaznou linkou a česaným suknem.
 */
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

            // Jemná struktura karetního plátna
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

            // Vnitřní zlatá linka plátna
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
    sirka: Dp = 78.dp,
    jePovolena: Boolean = true,
    aktivniKontrolaTahu: Boolean = false,
    jeVybrana: Boolean = false,
    stitekNavic: String? = null,
    onClick: (() -> Unit)? = null
) {
    val vyska = sirka * 1.56f
    val tvar = RoundedCornerShape(10.dp)

    val cilovyPosunY = when {
        jeVybrana -> (-16).dp
        aktivniKontrolaTahu && jePovolena -> (-10).dp
        else -> 0.dp
    }
    val animovanyPosunY by animateDpAsState(targetValue = cilovyPosunY)

    val okrajBarva = when {
        jeVybrana -> BarvaVybraneZare
        aktivniKontrolaTahu && jePovolena -> BarvaPovoleneZare
        else -> Color(0xFF5A4529)
    }
    val okrajTloustka = when {
        jeVybrana -> 3.0.dp
        aktivniKontrolaTahu && jePovolena -> 2.5.dp
        else -> 1.3.dp
    }

    val muzeKliknout = onClick != null && (!aktivniKontrolaTahu || jePovolena)

    Box(
        modifier = modifier
            .offset(y = animovanyPosunY)
            .size(width = sirka, height = vyska)
            .shadow(
                elevation = if (jeVybrana || (aktivniKontrolaTahu && jePovolena)) 10.dp else 5.dp,
                shape = tvar
            )
            .clip(tvar)
            .background(
                Brush.verticalGradient(
                    colors = listOf(BarvaSlonovinySvetla, BarvaSlonovinyStred, BarvaSlonovinyOkraj)
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
            val vnejsi = size.width * 0.055f
            val vnitrni = size.width * 0.082f
            drawRoundRect(
                color = BarvaRamuCervena,
                topLeft = Offset(vnejsi, vnejsi),
                size = Size(size.width - vnejsi * 2f, size.height - vnejsi * 2f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 1.4f)
            )
            drawRoundRect(
                color = BarvaRamuZlata,
                topLeft = Offset(vnitrni, vnitrni),
                size = Size(size.width - vnitrni * 2f, size.height - vnitrni * 2f),
                cornerRadius = CornerRadius(9f, 9f),
                style = Stroke(width = 1.1f)
            )

            vykresliDvouhlavyObsahKarty(karta)
        }

        val barvaPisma = barvaProZnakKarty(karta.barva)
        val pismoRohu = when {
            sirka < 60.dp -> 10.sp
            sirka < 72.dp -> 11.sp
            else -> 12.sp
        }
        val ikonaRohu = when {
            sirka < 60.dp -> 10.dp
            sirka < 72.dp -> 12.dp
            else -> 14.dp
        }

        RohovyIndexKarty(
            znakHodnoty = karta.hodnota.kratkyZnak,
            barvaKarty = karta.barva,
            barvaPisma = barvaPisma,
            velikostPisma = pismoRohu,
            velikostIkony = ikonaRohu,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 3.dp)
        )

        RohovyIndexKarty(
            znakHodnoty = karta.hodnota.kratkyZnak,
            barvaKarty = karta.barva,
            barvaPisma = barvaPisma,
            velikostPisma = pismoRohu,
            velikostIkony = ikonaRohu,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 3.dp)
                .rotate(180f)
        )

        if (!stitekNavic.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xEB134E27))
                    .border(0.8.dp, Color(0xFFFFD54F), RoundedCornerShape(5.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = stitekNavic,
                    color = Color(0xFFFFF59D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (aktivniKontrolaTahu && !jePovolena) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x8809140D))
            )
        }
    }
}

@Composable
private fun RohovyIndexKarty(
    znakHodnoty: String,
    barvaKarty: BarvaKarty,
    barvaPisma: Color,
    velikostPisma: TextUnit,
    velikostIkony: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xEBFFFDF7))
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = znakHodnoty,
            color = barvaPisma,
            fontSize = velikostPisma,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = velikostPisma
        )
        IkonaBarvyKarty(barva = barvaKarty, velikost = velikostIkony)
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
    val vyska = sirka * 1.56f
    val tvar = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .size(width = sirka, height = vyska)
            .shadow(6.dp, tvar)
            .clip(tvar)
            .background(BarvaSlonovinySvetla)
            .border(
                width = if (zvyseneZvyrazneni) 2.6.dp else 1.3.dp,
                color = if (zvyseneZvyrazneni) BarvaPovoleneZare else Color(0xFF5A4529),
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
            val pad = size.width * 0.075f
            val w = size.width - pad * 2f
            val h = size.height - pad * 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8E1B22), Color(0xFF580E13), Color(0xFF8E1B22))
                ),
                topLeft = Offset(pad, pad),
                size = Size(w, h),
                cornerRadius = CornerRadius(10f, 10f)
            )

            clipRect(left = pad + 3f, top = pad + 3f, right = pad + w - 3f, bottom = pad + h - 3f) {
                val roztec = size.width * 0.16f
                var posun = -h
                while (posun < w + h) {
                    drawLine(
                        color = Color(0x38F3CF7A),
                        start = Offset(pad + posun, pad),
                        end = Offset(pad + posun + h, pad + h),
                        strokeWidth = 1.1f
                    )
                    drawLine(
                        color = Color(0x38F3CF7A),
                        start = Offset(pad + posun + h, pad),
                        end = Offset(pad + posun, pad + h),
                        strokeWidth = 1.1f
                    )
                    posun += roztec
                }
            }

            drawRoundRect(
                color = Color(0xFFE6C15A),
                topLeft = Offset(pad + 2.5f, pad + 2.5f),
                size = Size(w - 5f, h - 5f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 1.8f)
            )

            val stred = Offset(size.width / 2f, size.height / 2f)
            val rMed = min(w, h) * 0.31f
            drawCircle(color = Color(0xFF470B0F), radius = rMed, center = stred)
            drawCircle(color = Color(0xFFE6C15A), radius = rMed, center = stred, style = Stroke(width = 1.8f))
            drawCircle(color = Color(0xFF9C2229), radius = rMed * 0.66f, center = stred)
            drawCircle(color = Color(0xFFF6DC8B), radius = rMed * 0.66f, center = stred, style = Stroke(width = 1.2f))
            drawCircle(color = Color(0xFFE6C15A), radius = rMed * 0.24f, center = stred)
        }

        if (pocetVBalicku != null || !popisek.isNullOrEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xD90E2918))
                    .border(1.dp, Color(0xFFE6C15A), RoundedCornerShape(7.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
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
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun DrawScope.vykresliDvouhlavyObsahKarty(karta: Karta) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rZaklad = min(size.width, size.height) * 0.145f

    when (karta.hodnota) {
        HodnotaKarty.ESO -> vykresliDvouhlaveEso(karta.barva, cx, cy, rZaklad)
        HodnotaKarty.KRAL -> vykresliDvouhlavouFiguru(karta.barva, cx, cy, typPostavy = 3)
        HodnotaKarty.SVRSEK -> vykresliDvouhlavouFiguru(karta.barva, cx, cy, typPostavy = 2)
        HodnotaKarty.SPODEK -> vykresliDvouhlavouFiguru(karta.barva, cx, cy, typPostavy = 1)
        HodnotaKarty.R10 -> vykresliBodovouKartu(karta.barva, pocetZnaku = 10, cx = cx, cy = cy, rZnak = rZaklad * 0.88f)
        HodnotaKarty.R9 -> vykresliBodovouKartu(karta.barva, pocetZnaku = 9, cx = cx, cy = cy, rZnak = rZaklad * 0.90f)
        HodnotaKarty.R8 -> vykresliBodovouKartu(karta.barva, pocetZnaku = 8, cx = cx, cy = cy, rZnak = rZaklad * 0.94f)
        HodnotaKarty.R7 -> vykresliBodovouKartu(karta.barva, pocetZnaku = 7, cx = cx, cy = cy, rZnak = rZaklad * 0.98f)
    }
}

private fun DrawScope.vykresliDvouhlaveEso(
    barva: BarvaKarty,
    cx: Float,
    cy: Float,
    rZaklad: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x55FFE082), Color(0x14C5962C), Color.Transparent),
            center = Offset(cx, cy),
            radius = rZaklad * 2.3f
        ),
        radius = rZaklad * 2.3f,
        center = Offset(cx, cy)
    )

    for (uhel in listOf(0f, 180f)) {
        withTransform({
            if (uhel != 0f) rotate(uhel, Offset(cx, cy))
        }) {
            val yZnak = cy - size.height * 0.21f
            drawCircle(
                color = Color(0xFFD4A437),
                radius = rZaklad * 1.25f,
                center = Offset(cx, yZnak),
                style = Stroke(width = 1.8f)
            )
            vykresliKralovskouKorunu(Offset(cx, yZnak - rZaklad * 1.32f), rZaklad * 1.18f)
            vykresliZnakBarvy(barva, Offset(cx, yZnak + rZaklad * 0.08f), rZaklad * 1.12f)
        }
    }

    val sirkaStuhy = size.width * 0.68f
    val vyskaStuhy = size.height * 0.072f
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFB3821E), Color(0xFFF6DC8B), Color(0xFFB3821E))
        ),
        topLeft = Offset(cx - sirkaStuhy / 2f, cy - vyskaStuhy / 2f),
        size = Size(sirkaStuhy, vyskaStuhy),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color(0xFF6D4C0D),
        topLeft = Offset(cx - sirkaStuhy / 2f, cy - vyskaStuhy / 2f),
        size = Size(sirkaStuhy, vyskaStuhy),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 1.2f)
    )
    vykresliZnakBarvy(barva, Offset(cx, cy), rZaklad * 0.56f)
}

private fun DrawScope.vykresliDvouhlavouFiguru(
    barva: BarvaKarty,
    cx: Float,
    cy: Float,
    typPostavy: Int
) {
    val okraj = size.width * 0.09f

    clipRect(left = okraj, top = okraj, right = size.width - okraj, bottom = cy) {
        vykresliPolovinuPostavy(barva, cx, cy, typPostavy)
    }

    clipRect(left = okraj, top = cy, right = size.width - okraj, bottom = size.height - okraj) {
        withTransform({
            rotate(180f, Offset(cx, cy))
        }) {
            vykresliPolovinuPostavy(barva, cx, cy, typPostavy)
        }
    }

    val sirkaSerpy = size.width - okraj * 2f
    val vyskaSerpy = size.height * 0.042f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFA6781E),
                Color(0xFFFFE596),
                Color(0xFFD4A234),
                Color(0xFFFFE596),
                Color(0xFFA6781E)
            )
        ),
        topLeft = Offset(okraj, cy - vyskaSerpy / 2f),
        size = Size(sirkaSerpy, vyskaSerpy)
    )
    drawLine(
        color = Color(0xFF5D3A08),
        start = Offset(okraj, cy - vyskaSerpy / 2f),
        end = Offset(size.width - okraj, cy - vyskaSerpy / 2f),
        strokeWidth = 1.3f
    )
    drawLine(
        color = Color(0xFF5D3A08),
        start = Offset(okraj, cy + vyskaSerpy / 2f),
        end = Offset(size.width - okraj, cy + vyskaSerpy / 2f),
        strokeWidth = 1.3f
    )

    val kosoctverec = Path().apply {
        moveTo(cx, cy - vyskaSerpy * 0.85f)
        lineTo(cx + vyskaSerpy * 0.9f, cy)
        lineTo(cx, cy + vyskaSerpy * 0.85f)
        lineTo(cx - vyskaSerpy * 0.9f, cy)
        close()
    }
    drawPath(kosoctverec, color = barvaProZnakKarty(barva))
    drawPath(kosoctverec, color = Color(0xFFFFF3C4), style = Stroke(width = 1.2f))
}

private fun DrawScope.vykresliPolovinuPostavy(
    barva: BarvaKarty,
    cx: Float,
    cy: Float,
    typPostavy: Int
) {
    val u = size.width * 0.11f

    drawRect(
        color = Color(0x15D4AF37),
        topLeft = Offset(size.width * 0.09f, size.width * 0.09f),
        size = Size(size.width * 0.82f, cy - size.width * 0.09f)
    )

    val stredX = cx - u * 0.30f
    val hlavaY = cy - u * 3.05f

    val barvaPlaste1: Color
    val barvaPlaste2: Color
    val barvaTuniky: Color
    when (typPostavy) {
        3 -> {
            barvaPlaste1 = Color(0xFFB71C1C)
            barvaPlaste2 = Color(0xFF6E0E14)
            barvaTuniky = Color(0xFF1E4D8C)
        }
        2 -> {
            barvaPlaste1 = Color(0xFF1E6B3B)
            barvaPlaste2 = Color(0xFF0E3F20)
            barvaTuniky = Color(0xFF9E2A2B)
        }
        else -> {
            barvaPlaste1 = Color(0xFF234E8E)
            barvaPlaste2 = Color(0xFF132C54)
            barvaTuniky = Color(0xFFC57B1E)
        }
    }

    val plast = Path().apply {
        moveTo(stredX - u * 2.55f, cy + 4f)
        cubicTo(
            stredX - u * 2.35f,
            cy - u * 1.55f,
            stredX - u * 1.45f,
            cy - u * 2.25f,
            stredX - u * 0.85f,
            cy - u * 2.20f
        )
        lineTo(stredX + u * 0.95f, cy - u * 2.20f)
        cubicTo(
            stredX + u * 1.65f,
            cy - u * 2.20f,
            stredX + u * 2.35f,
            cy - u * 1.45f,
            stredX + u * 2.55f,
            cy + 4f
        )
        close()
    }
    drawPath(
        path = plast,
        brush = Brush.verticalGradient(
            colors = listOf(barvaPlaste1, barvaPlaste2),
            startY = cy - u * 2.3f,
            endY = cy
        )
    )
    drawPath(plast, color = Color(0xFFD9AE42), style = Stroke(width = 1.8f))

    val tunika = Path().apply {
        moveTo(stredX - u * 0.95f, cy + 4f)
        lineTo(stredX - u * 0.65f, cy - u * 2.15f)
        lineTo(stredX + u * 0.65f, cy - u * 2.15f)
        lineTo(stredX + u * 0.95f, cy + 4f)
        close()
    }
    drawPath(tunika, color = barvaTuniky)
    drawPath(tunika, color = Color(0xFFE6C15A), style = Stroke(width = 1.3f))

    for (k in 0..2) {
        val ky = cy - u * (1.65f - k * 0.58f)
        drawCircle(color = Color(0xFFFFE082), radius = u * 0.14f, center = Offset(stredX, ky))
    }

    drawOval(
        color = if (typPostavy == 3) Color(0xFFFFFBF0) else Color(0xFFF3E2B8),
        topLeft = Offset(stredX - u * 0.95f, cy - u * 2.38f),
        size = Size(u * 1.90f, u * 0.52f)
    )
    drawOval(
        color = Color(0xFF795548),
        topLeft = Offset(stredX - u * 0.95f, cy - u * 2.38f),
        size = Size(u * 1.90f, u * 0.52f),
        style = Stroke(width = 1.0f)
    )

    when (typPostavy) {
        3 -> {
            val xZezlo = stredX - u * 1.65f
            drawLine(
                color = Color(0xFFE6B83A),
                start = Offset(xZezlo + u * 0.35f, cy),
                end = Offset(xZezlo, hlavaY - u * 0.25f),
                strokeWidth = 3.2f
            )
            drawCircle(color = Color(0xFFFFD54F), radius = u * 0.32f, center = Offset(xZezlo, hlavaY - u * 0.38f))
            drawCircle(color = Color(0xFFC62828), radius = u * 0.15f, center = Offset(xZezlo, hlavaY - u * 0.38f))
        }
        2 -> {
            val xMec = stredX - u * 1.62f
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(xMec, cy - u * 0.4f),
                end = Offset(xMec - u * 0.15f, hlavaY - u * 0.8f),
                strokeWidth = 2.8f
            )
            drawLine(
                color = Color(0xFFE6B83A),
                start = Offset(xMec - u * 0.42f, cy - u * 0.72f),
                end = Offset(xMec + u * 0.42f, cy - u * 0.72f),
                strokeWidth = 2.6f
            )
        }
        else -> {
            val xKopi = stredX - u * 1.58f
            drawLine(
                color = Color(0xFF795548),
                start = Offset(xKopi, cy),
                end = Offset(xKopi, hlavaY - u * 0.85f),
                strokeWidth = 2.6f
            )
            val hrot = Path().apply {
                moveTo(xKopi, hlavaY - u * 1.35f)
                lineTo(xKopi + u * 0.32f, hlavaY - u * 0.65f)
                lineTo(xKopi - u * 0.22f, hlavaY - u * 0.65f)
                close()
            }
            drawPath(hrot, color = Color(0xFFB0BEC5))
        }
    }

    drawCircle(
        color = if (typPostavy == 3) Color(0xFF8D6E63) else Color(0xFF5D4037),
        radius = u * 0.92f,
        center = Offset(stredX, hlavaY + u * 0.08f)
    )

    val hlavaStred = Offset(stredX, hlavaY)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFCE2C4), Color(0xFFEAB88F)),
            center = Offset(hlavaStred.x - u * 0.15f, hlavaStred.y - u * 0.15f),
            radius = u * 1.05f
        ),
        topLeft = Offset(hlavaStred.x - u * 0.76f, hlavaStred.y - u * 0.86f),
        size = Size(u * 1.52f, u * 1.72f)
    )
    drawOval(
        color = Color(0xFF6D4C41),
        topLeft = Offset(hlavaStred.x - u * 0.76f, hlavaStred.y - u * 0.86f),
        size = Size(u * 1.52f, u * 1.72f),
        style = Stroke(width = 1.2f)
    )

    val yOci = hlavaStred.y - u * 0.14f
    drawCircle(color = Color(0xFF2E1C14), radius = u * 0.10f, center = Offset(hlavaStred.x - u * 0.28f, yOci))
    drawCircle(color = Color(0xFF2E1C14), radius = u * 0.10f, center = Offset(hlavaStred.x + u * 0.28f, yOci))
    drawLine(
        color = Color(0xFF8D5B3E),
        start = Offset(hlavaStred.x, yOci - u * 0.05f),
        end = Offset(hlavaStred.x + u * 0.08f, hlavaStred.y + u * 0.22f),
        strokeWidth = 1.2f
    )

    when (typPostavy) {
        3 -> {
            val plnovous = Path().apply {
                moveTo(hlavaStred.x - u * 0.68f, hlavaStred.y + u * 0.22f)
                cubicTo(
                    hlavaStred.x - u * 0.65f,
                    hlavaStred.y + u * 1.25f,
                    hlavaStred.x + u * 0.65f,
                    hlavaStred.y + u * 1.25f,
                    hlavaStred.x + u * 0.68f,
                    hlavaStred.y + u * 0.22f
                )
                close()
            }
            drawPath(plnovous, color = Color(0xFF9E8A78))
            drawPath(plnovous, color = Color(0xFF5D4037), style = Stroke(width = 1.0f))
            vykresliKralovskouKorunu(Offset(hlavaStred.x, hlavaStred.y - u * 1.05f), u * 1.85f)
        }
        2 -> {
            drawLine(
                color = Color(0xFF4E342E),
                start = Offset(hlavaStred.x - u * 0.42f, hlavaStred.y + u * 0.36f),
                end = Offset(hlavaStred.x + u * 0.42f, hlavaStred.y + u * 0.36f),
                strokeWidth = 2.2f
            )
            val baret = Path().apply {
                moveTo(hlavaStred.x - u * 1.05f, hlavaStred.y - u * 0.62f)
                cubicTo(
                    hlavaStred.x - u * 0.95f,
                    hlavaStred.y - u * 1.45f,
                    hlavaStred.x + u * 1.05f,
                    hlavaStred.y - u * 1.45f,
                    hlavaStred.x + u * 0.95f,
                    hlavaStred.y - u * 0.58f
                )
                close()
            }
            drawPath(baret, color = Color(0xFF1B5E38))
            drawPath(baret, color = Color(0xFFE6C15A), style = Stroke(width = 1.3f))
            val pero = Path().apply {
                moveTo(hlavaStred.x + u * 0.35f, hlavaStred.y - u * 1.05f)
                cubicTo(
                    hlavaStred.x + u * 1.45f,
                    hlavaStred.y - u * 1.85f,
                    hlavaStred.x + u * 1.65f,
                    hlavaStred.y - u * 0.95f,
                    hlavaStred.x + u * 0.55f,
                    hlavaStred.y - u * 0.78f
                )
                close()
            }
            drawPath(pero, color = Color(0xFFD32F2F))
        }
        else -> {
            drawLine(
                color = Color(0xFF8D4E3C),
                start = Offset(hlavaStred.x - u * 0.24f, hlavaStred.y + u * 0.42f),
                end = Offset(hlavaStred.x + u * 0.24f, hlavaStred.y + u * 0.42f),
                strokeWidth = 1.5f
            )
            drawRoundRect(
                color = Color(0xFF284B8C),
                topLeft = Offset(hlavaStred.x - u * 0.92f, hlavaStred.y - u * 1.18f),
                size = Size(u * 1.84f, u * 0.58f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawLine(
                color = Color(0xFFE6C15A),
                start = Offset(hlavaStred.x - u * 0.92f, hlavaStred.y - u * 0.64f),
                end = Offset(hlavaStred.x + u * 0.92f, hlavaStred.y - u * 0.64f),
                strokeWidth = 2.0f
            )
        }
    }

    // Znak barvy: Král a Svršek nahoře u hlavy, Spodek dole u šerpy
    val poziceZnaku = when (typPostavy) {
        3 -> Offset(cx + u * 1.95f, hlavaY - u * 0.35f)
        2 -> Offset(cx + u * 1.95f, hlavaY - u * 0.45f)
        else -> Offset(cx + u * 1.90f, cy - u * 1.18f)
    }
    drawCircle(color = Color(0xEBFFFDF6), radius = u * 1.05f, center = poziceZnaku)
    drawCircle(color = Color(0xFFC5962C), radius = u * 1.05f, center = poziceZnaku, style = Stroke(width = 1.2f))
    vykresliZnakBarvy(barva, poziceZnaku, u * 0.86f)
}

private fun DrawScope.vykresliBodovouKartu(
    barva: BarvaKarty,
    pocetZnaku: Int,
    cx: Float,
    cy: Float,
    rZnak: Float
) {
    val sloupX = size.width * 0.22f

    drawLine(
        color = Color(0x366D8F5B),
        start = Offset(cx, size.height * 0.20f),
        end = Offset(cx, size.height * 0.80f),
        strokeWidth = 1.6f
    )

    fun kresliZnak(x: Float, y: Float, otocitDolu: Boolean, meritko: Float = 1f) {
        if (otocitDolu) {
            withTransform({
                rotate(180f, Offset(x, y))
            }) {
                vykresliZnakBarvy(barva, Offset(x, y), rZnak * meritko)
            }
        } else {
            vykresliZnakBarvy(barva, Offset(x, y), rZnak * meritko)
        }
    }

    when (pocetZnaku) {
        10 -> {
            val yRady = listOf(0.21f, 0.38f, 0.62f, 0.79f)
            for (r in yRady) {
                val y = size.height * r
                val dolu = r > 0.5f
                kresliZnak(cx - sloupX, y, dolu, 0.86f)
                kresliZnak(cx + sloupX, y, dolu, 0.86f)
            }
            kresliZnak(cx, size.height * 0.29f, false, 0.88f)
            kresliZnak(cx, size.height * 0.71f, true, 0.88f)

            val rStred = rZnak * 0.72f
            drawCircle(color = Color(0xFFFFF8E1), radius = rStred, center = Offset(cx, cy))
            drawCircle(color = Color(0xFFC5962C), radius = rStred, center = Offset(cx, cy), style = Stroke(width = 1.5f))
            val d = rStred * 0.48f
            val b = barvaProZnakKarty(barva)
            drawLine(b, Offset(cx - d, cy - d), Offset(cx + d, cy + d), strokeWidth = 2.5f)
            drawLine(b, Offset(cx + d, cy - d), Offset(cx - d, cy + d), strokeWidth = 2.5f)
        }
        9 -> {
            val yRady = listOf(0.21f, 0.38f, 0.62f, 0.79f)
            for (r in yRady) {
                val y = size.height * r
                val dolu = r > 0.5f
                kresliZnak(cx - sloupX, y, dolu, 0.88f)
                kresliZnak(cx + sloupX, y, dolu, 0.88f)
            }
            kresliZnak(cx, cy, false, 1.02f)
        }
        8 -> {
            val ySloupce = listOf(0.23f, 0.50f, 0.77f)
            for (r in ySloupce) {
                val y = size.height * r
                val dolu = r > 0.55f
                kresliZnak(cx - sloupX, y, dolu, 0.92f)
                kresliZnak(cx + sloupX, y, dolu, 0.92f)
            }
            kresliZnak(cx, size.height * 0.35f, false, 0.92f)
            kresliZnak(cx, size.height * 0.65f, true, 0.92f)
        }
        else -> {
            val ySloupce = listOf(0.23f, 0.50f, 0.77f)
            for (r in ySloupce) {
                val y = size.height * r
                val dolu = r > 0.55f
                kresliZnak(cx - sloupX, y, dolu, 0.94f)
                kresliZnak(cx + sloupX, y, dolu, 0.94f)
            }
            kresliZnak(cx, size.height * 0.36f, false, 1.02f)
        }
    }
}

private fun DrawScope.vykresliKralovskouKorunu(stred: Offset, sirka: Float) {
    val pol = sirka * 0.5f
    val vyska = sirka * 0.54f
    val koruna = Path().apply {
        moveTo(stred.x - pol * 0.88f, stred.y + vyska * 0.46f)
        lineTo(stred.x - pol * 1.05f, stred.y - vyska * 0.34f)
        lineTo(stred.x - pol * 0.52f, stred.y + vyska * 0.08f)
        lineTo(stred.x, stred.y - vyska * 0.58f)
        lineTo(stred.x + pol * 0.52f, stred.y + vyska * 0.08f)
        lineTo(stred.x + pol * 1.05f, stred.y - vyska * 0.34f)
        lineTo(stred.x + pol * 0.88f, stred.y + vyska * 0.46f)
        close()
    }
    drawPath(
        path = koruna,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFD9A01E)),
            startY = stred.y - vyska * 0.6f,
            endY = stred.y + vyska * 0.5f
        )
    )
    drawPath(koruna, color = Color(0xFF6D4C00), style = Stroke(width = 1.3f))
    drawCircle(color = Color(0xFFD32F2F), radius = sirka * 0.07f, center = Offset(stred.x, stred.y + vyska * 0.18f))
}

private fun DrawScope.vykresliZnakBarvy(
    barva: BarvaKarty,
    stred: Offset,
    polomer: Float
) {
    when (barva) {
        BarvaKarty.CERVENE -> {
            val srdce = Path().apply {
                moveTo(stred.x, stred.y + polomer * 0.98f)
                cubicTo(
                    stred.x - polomer * 1.32f,
                    stred.y + polomer * 0.22f,
                    stred.x - polomer * 1.22f,
                    stred.y - polomer * 0.98f,
                    stred.x,
                    stred.y - polomer * 0.28f
                )
                cubicTo(
                    stred.x + polomer * 1.22f,
                    stred.y - polomer * 0.98f,
                    stred.x + polomer * 1.32f,
                    stred.y + polomer * 0.22f,
                    stred.x,
                    stred.y + polomer * 0.98f
                )
                close()
            }
            drawPath(
                path = srdce,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF03E3E), Color(0xFFB71C1C)),
                    startX = stred.x - polomer,
                    endX = stred.x + polomer
                )
            )
            drawCircle(
                color = Color(0x55FFCDD2),
                radius = polomer * 0.23f,
                center = Offset(stred.x - polomer * 0.42f, stred.y - polomer * 0.34f)
            )
            drawLine(
                color = Color(0x486A0B0B),
                start = Offset(stred.x, stred.y - polomer * 0.26f),
                end = Offset(stred.x, stred.y + polomer * 0.92f),
                strokeWidth = 1.1f
            )
            drawPath(srdce, color = Color(0xFF6A0B0B), style = Stroke(width = 1.4f))
        }

        BarvaKarty.KULE -> {
            val rKoule = polomer * 0.90f
            drawCircle(
                color = Color(0xFFE09F1F),
                radius = polomer * 0.19f,
                center = Offset(stred.x, stred.y - rKoule * 0.96f),
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF176), Color(0xFFE09F1F), Color(0xFFB85414)),
                    center = Offset(stred.x - rKoule * 0.30f, stred.y - rKoule * 0.30f),
                    radius = rKoule * 1.35f
                ),
                radius = rKoule,
                center = stred
            )
            drawLine(
                color = Color(0xFFFFE082),
                start = Offset(stred.x - rKoule * 0.96f, stred.y - rKoule * 0.05f),
                end = Offset(stred.x + rKoule * 0.96f, stred.y - rKoule * 0.05f),
                strokeWidth = 2.4f
            )
            drawLine(
                color = Color(0xFF5D3405),
                start = Offset(stred.x - rKoule * 0.98f, stred.y + rKoule * 0.07f),
                end = Offset(stred.x + rKoule * 0.98f, stred.y + rKoule * 0.07f),
                strokeWidth = 1.4f
            )
            drawLine(
                color = Color(0xFF3E1F04),
                start = Offset(stred.x, stred.y + rKoule * 0.14f),
                end = Offset(stred.x, stred.y + rKoule * 0.84f),
                strokeWidth = 1.6f
            )
            drawCircle(
                color = Color(0xFF2B1402),
                radius = rKoule * 0.18f,
                center = Offset(stred.x, stred.y + rKoule * 0.48f)
            )
            drawCircle(
                color = Color(0xFF5D3405),
                radius = rKoule,
                center = stred,
                style = Stroke(width = 1.4f)
            )
        }

        BarvaKarty.ZALUDY -> {
            val plod = Path().apply {
                moveTo(stred.x - polomer * 0.64f, stred.y - polomer * 0.22f)
                cubicTo(
                    stred.x - polomer * 0.74f,
                    stred.y + polomer * 0.62f,
                    stred.x - polomer * 0.22f,
                    stred.y + polomer * 0.92f,
                    stred.x,
                    stred.y + polomer * 0.96f
                )
                cubicTo(
                    stred.x + polomer * 0.22f,
                    stred.y + polomer * 0.92f,
                    stred.x + polomer * 0.74f,
                    stred.y + polomer * 0.62f,
                    stred.x + polomer * 0.64f,
                    stred.y - polomer * 0.22f
                )
                close()
            }
            drawPath(
                path = plod,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFDFA42B), Color(0xFF388E3C)),
                    startX = stred.x - polomer * 0.65f,
                    endX = stred.x + polomer * 0.65f
                )
            )
            drawPath(plod, color = Color(0xFF3E2411), style = Stroke(width = 1.3f))

            val cepickaTop = stred.y - polomer * 0.56f
            val cepickaSirka = polomer * 1.56f
            val cepickaVyska = polomer * 0.44f
            drawRoundRect(
                color = Color(0xFF6D4527),
                topLeft = Offset(stred.x - cepickaSirka / 2f, cepickaTop),
                size = Size(cepickaSirka, cepickaVyska),
                cornerRadius = CornerRadius(polomer * 0.22f, polomer * 0.22f)
            )
            for (s in -2..2) {
                val sx = stred.x + s * polomer * 0.22f
                drawLine(
                    color = Color(0x88D7CCC8),
                    start = Offset(sx - polomer * 0.10f, cepickaTop + 2f),
                    end = Offset(sx + polomer * 0.10f, cepickaTop + cepickaVyska - 2f),
                    strokeWidth = 1.0f
                )
            }
            drawRoundRect(
                color = Color(0xFF3E2411),
                topLeft = Offset(stred.x - cepickaSirka / 2f, cepickaTop),
                size = Size(cepickaSirka, cepickaVyska),
                cornerRadius = CornerRadius(polomer * 0.22f, polomer * 0.22f),
                style = Stroke(width = 1.3f)
            )
            drawLine(
                color = Color(0xFF3E2411),
                start = Offset(stred.x, cepickaTop),
                end = Offset(stred.x + polomer * 0.12f, stred.y - polomer * 0.96f),
                strokeWidth = 2.2f
            )
        }

        BarvaKarty.ZELENE -> {
            val list = Path().apply {
                moveTo(stred.x, stred.y - polomer * 0.98f)
                cubicTo(
                    stred.x + polomer * 1.22f,
                    stred.y - polomer * 0.34f,
                    stred.x + polomer * 1.02f,
                    stred.y + polomer * 0.64f,
                    stred.x,
                    stred.y + polomer * 0.68f
                )
                cubicTo(
                    stred.x - polomer * 1.02f,
                    stred.y + polomer * 0.64f,
                    stred.x - polomer * 1.22f,
                    stred.y - polomer * 0.34f,
                    stred.x,
                    stred.y - polomer * 0.98f
                )
                close()
            }
            drawPath(
                path = list,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF57B85C), Color(0xFF14692B)),
                    startX = stred.x - polomer,
                    endX = stred.x + polomer
                )
            )
            drawLine(
                color = Color(0xFFDCE775),
                start = Offset(stred.x, stred.y - polomer * 0.78f),
                end = Offset(stred.x, stred.y + polomer * 0.98f),
                strokeWidth = 1.8f
            )
            for (v in -1..1) {
                val vy = stred.y + v * polomer * 0.28f
                drawLine(
                    color = Color(0xAAFFF59D),
                    start = Offset(stred.x, vy + polomer * 0.10f),
                    end = Offset(stred.x - polomer * 0.52f, vy - polomer * 0.12f),
                    strokeWidth = 1.1f
                )
                drawLine(
                    color = Color(0x88C8E6C9),
                    start = Offset(stred.x, vy + polomer * 0.10f),
                    end = Offset(stred.x + polomer * 0.52f, vy - polomer * 0.12f),
                    strokeWidth = 1.1f
                )
            }
            drawPath(list, color = Color(0xFF0A3D17), style = Stroke(width = 1.4f))
        }
    }
}
