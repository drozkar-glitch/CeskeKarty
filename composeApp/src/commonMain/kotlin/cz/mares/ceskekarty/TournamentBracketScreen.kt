package cz.mares.ceskekarty

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TournamentHallScreen(
    vybranaVarianta: VariantaPravidel,
    onZmenaVarianty: (VariantaPravidel) -> Unit,
    onZpet: () -> Unit,
    onUcetNeboMesecZmenen: () -> Unit,
    onSpustitOfflinePohar: (TypHry, VariantaPravidel) -> Unit,
    onVstoupitDoZapasuTurnaje: (OnlineStul) -> Unit
) {
    val scope = rememberCoroutineScope()
    val mojeId = remember { ziskejUnikatniIdZarizeni() }
    val ulozenyNick = remember { nactiUlozenouPrezdivku() }

    var turnaje by remember { mutableStateOf(emptyList<PlanovanyTurnaj>()) }
    var vybranyTurnajId by remember { mutableStateOf("") }
    var nacitaSe by remember { mutableStateOf(true) }
    var probihaAkce by remember { mutableStateOf(false) }
    var stavovaZprava by remember { mutableStateOf("") }
    var jeUspechZpravy by remember { mutableStateOf(false) }
    var nyniServerMs by remember { mutableStateOf(OnlineManager.odhadniAktualniServerovyCasMs()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nyniServerMs = OnlineManager.odhadniAktualniServerovyCasMs()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val nactene = TournamentManager.synchronizujANactiRozvrhTurnaju()
            nyniServerMs = OnlineManager.odhadniAktualniServerovyCasMs()
            turnaje = nactene
            nacitaSe = false

            if (vybranyTurnajId.isEmpty() || nactene.none { it.turnajId == vybranyTurnajId }) {
                val preferovany = nactene.firstOrNull { it.stav == "PROBIHA" && it.jeHracPrihlasen(mojeId) }
                    ?: nactene.firstOrNull { it.stav == "PROBIHA" }
                    ?: nactene.firstOrNull { it.stav == "REGISTRACE" && it.jeHracPrihlasen(mojeId) }
                    ?: nactene.firstOrNull { it.stav == "REGISTRACE" }
                    ?: nactene.firstOrNull()
                if (preferovany != null) {
                    vybranyTurnajId = preferovany.turnajId
                }
            }

            val vyplataText = TournamentManager.zkontrolujVyplatuOdmenyLokalnimuHraci(nactene)
            if (vyplataText != null) {
                silnaHaptika()
                HospodaZvuk.cinkniVyuctovani()
                jeUspechZpravy = true
                stavovaZprava = vyplataText
                onUcetNeboMesecZmenen()
                turnaje = TournamentManager.synchronizujANactiRozvrhTurnaju()
            }

            delay(4500L)
        }
    }

    val prahaHodiny = TurnajKalendar.utcMsNaPrahaCas(nyniServerMs)
    val hodinyText = "${prahaHodiny.hodina.toString().padStart(2, '0')}:${prahaHodiny.minuta.toString().padStart(2, '0')}"
    val aktivniANadchazejici = turnaje.filter { it.stav != "DOKONCENO" }
    val dohraneTurnaje = turnaje.filter { it.stav == "DOKONCENO" }.take(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF184D2E))
                    .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(8.dp))
                    .clickable {
                        jemnaHaptika()
                        onZpet()
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "← Zpět",
                    color = Color(0xFFFFE082),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Turnajová síň",
                color = Color(0xFFFFD54F),
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF103820))
                    .border(1.dp, Color(0x66FFD54F), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🕒 $hodinyText",
                    color = Color(0xFFFFF59D),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD9082916))
                .border(1.2.dp, Color(0xAAFFD54F), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Pravidelný denní rozvrh turnajů",
                color = Color(0xFFFFD54F),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Každý den: 19:00 Prší (8 hráčů, na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy) • 20:00 Sedma (8 hráčů, na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy) • 20:30 Volený Mariáš (9 hráčů, $POCET_ROZDANI_MARIAS_TURNAJ rozdání). Neobsazená místa v čas výkopu doplní boti.",
                color = Color(0xFFB9F6CA),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        if (stavovaZprava.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (jeUspechZpravy) Color(0xFF144A29) else Color(0xFF4A1818))
                    .border(
                        width = 1.dp,
                        color = if (jeUspechZpravy) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stavovaZprava,
                    color = if (jeUspechZpravy) Color(0xFFB9F6CA) else Color(0xFFFFCCBC),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Vypsané a probíhající turnaje:",
            color = Color(0xFFFFE082),
            fontSize = 15.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (nacitaSe) {
            Text(
                text = "Načítám turnajový rozvrh a pavouky…",
                color = Color(0xFFB9F6CA),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        } else if (aktivniANadchazejici.isEmpty()) {
            Text(
                text = "Připravuji další turnajové termíny…",
                color = Color(0xFFB9F6CA),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        } else {
            for (turnaj in aktivniANadchazejici) {
                val jeRozbaleny = turnaj.turnajId == vybranyTurnajId
                KartaPlanovanehoTurnaje(
                    turnaj = turnaj,
                    mojeId = mojeId,
                    ulozenyNick = ulozenyNick,
                    nyniMs = nyniServerMs,
                    jeRozbalenyPavouk = jeRozbaleny,
                    probihaAkce = probihaAkce,
                    onPrepnoutPavouka = {
                        jemnaHaptika()
                        vybranyTurnajId = if (jeRozbaleny) "" else turnaj.turnajId
                    },
                    onPrihlasit = {
                        if (probihaAkce) return@KartaPlanovanehoTurnaje
                        jemnaHaptika()
                        probihaAkce = true
                        stavovaZprava = ""
                        scope.launch {
                            val vys = TournamentManager.prihlasSeDoTurnaje(turnaj.turnajId)
                            vys.onSuccess { aktualizovany ->
                                silnaHaptika()
                                onUcetNeboMesecZmenen()
                                vybranyTurnajId = aktualizovany.turnajId
                                jeUspechZpravy = true
                                stavovaZprava = "Jsi zapsán v pavouku turnaje „${aktualizovany.nazev}“ (${TurnajKalendar.formatujTerminTurnaje(aktualizovany.casStartuMs, nyniServerMs)})!"
                                turnaje = TournamentManager.synchronizujANactiRozvrhTurnaju()
                            }.onFailure { err ->
                                jeUspechZpravy = false
                                stavovaZprava = err.message ?: "Přihlášení se nezdařilo."
                            }
                            probihaAkce = false
                        }
                    },
                    onOdhlasit = {
                        if (probihaAkce) return@KartaPlanovanehoTurnaje
                        jemnaHaptika()
                        probihaAkce = true
                        stavovaZprava = ""
                        scope.launch {
                            val vys = TournamentManager.odhlasSeZTurnaje(turnaj.turnajId)
                            vys.onSuccess {
                                onUcetNeboMesecZmenen()
                                jeUspechZpravy = true
                                stavovaZprava = "Odhlášeno — vklad ${formatujTereziany(turnaj.vkladTereziany)} byl vrácen do měšce."
                                turnaje = TournamentManager.synchronizujANactiRozvrhTurnaju()
                            }.onFailure { err ->
                                jeUspechZpravy = false
                                stavovaZprava = err.message ?: "Odhlášení již není možné."
                            }
                            probihaAkce = false
                        }
                    },
                    onHratAktivniZapas = {
                        if (probihaAkce) return@KartaPlanovanehoTurnaje
                        jemnaHaptika()
                        probihaAkce = true
                        stavovaZprava = ""
                        scope.launch {
                            val vys = TournamentManager.vstupDoAktivnihoZapasuHrace(turnaj)
                            probihaAkce = false
                            vys.onSuccess { stul ->
                                onVstoupitDoZapasuTurnaje(stul)
                            }.onFailure { err ->
                                jeUspechZpravy = false
                                stavovaZprava = err.message ?: "Stůl zápasu zatím není připraven."
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        if (dohraneTurnaje.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nedávno dohrané turnaje:",
                color = Color(0xFFFFE082),
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            for (dohrany in dohraneTurnaje) {
                val rozbaleno = dohrany.turnajId == vybranyTurnajId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC072213))
                        .border(1.dp, Color(0x55D4AF37), RoundedCornerShape(12.dp))
                        .clickable {
                            jemnaHaptika()
                            vybranyTurnajId = if (rozbaleno) "" else dohrany.turnajId
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = dohrany.nazev,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = TurnajKalendar.formatujTerminTurnaje(dohrany.casStartuMs, nyniServerMs),
                                color = Color(0xFFA5D6A7),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                        TurnajPravidlaStitek(dohrany.variantaPravidel)
                    }
                    if (dohrany.celkovyVitezJmeno.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🏆 1. místo: ${dohrany.celkovyVitezJmeno}" +
                                if (dohrany.druheMistoJmeno.isNotEmpty()) "  •  🥈 2. místo: ${dohrany.druheMistoJmeno}" else "",
                            color = Color(0xFF69F0AE),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (rozbaleno && dohrany.uzlyPavouka.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GrafickyPavoukTurnaje(turnaj = dohrany, mojeId = mojeId)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD9082916))
                .border(1.dp, Color(0x77D4AF37), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "Tréninkový pohár proti botům (offline)",
                color = Color(0xFFFFD54F),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Prší a Sedma na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy • Mariáš série 3 rozdání",
                color = Color(0xFFB9F6CA),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (varianta in VariantaPravidel.entries) {
                    val akt = vybranaVarianta == varianta
                    val ikona = if (varianta == VariantaPravidel.HOSPODSKA) "🍺" else "🏛️"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (akt) Color(0xFFD4AF37) else Color(0xFF194D2F))
                            .clickable {
                                jemnaHaptika()
                                onZmenaVarianty(varianta)
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$ikona ${varianta.kratkyNazev}",
                            color = if (akt) Color(0xFF072312) else Color.White,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (typ in TypHry.entries) {
                    Button(
                        onClick = {
                            jemnaHaptika()
                            onSpustitOfflinePohar(typ, vybranaVarianta)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D5A36))
                    ) {
                        Text(
                            text = if (typ == TypHry.MARIAS) "Mariáš" else typ.nazev,
                            color = Color.White,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
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
private fun TurnajPravidlaStitek(varianta: VariantaPravidel) {
    val jeHospodska = varianta == VariantaPravidel.HOSPODSKA
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (jeHospodska) Color(0xFF3E2723) else Color(0xFF1A3E5C))
            .border(
                width = 0.8.dp,
                color = if (jeHospodska) Color(0xFFFFCC80) else Color(0xFF90CAF9),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (jeHospodska) "🍺 Hospodská" else "🏛️ Svazová",
            color = if (jeHospodska) Color(0xFFFFE082) else Color(0xFFE3F2FD),
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun KartaPlanovanehoTurnaje(
    turnaj: PlanovanyTurnaj,
    mojeId: String,
    ulozenyNick: String,
    nyniMs: Long,
    jeRozbalenyPavouk: Boolean,
    probihaAkce: Boolean,
    onPrepnoutPavouka: () -> Unit,
    onPrihlasit: () -> Unit,
    onOdhlasit: () -> Unit,
    onHratAktivniZapas: () -> Unit
) {
    val ziviPrihlaseni = turnaj.ucastnici.filterNot { jeBotDeviceId(it.deviceId) }
    val jsemPrihlasen = turnaj.jeHracPrihlasen(mojeId)
    val mujAktivniUzel = turnaj.aktivniZapasHrace(mojeId)
    val zbyvaMs = turnaj.casStartuMs - nyniMs
    val probihaHra = turnaj.stav == "PROBIHA"

    val formatZapasuText = if (turnaj.typHry == TypHry.MARIAS) {
        "Série na $POCET_ROZDANI_MARIAS_TURNAJ rozdání (součet bodů)"
    } else {
        "Na $CIL_VITEZNYCH_BITEV_DUELU vítězné bitvy (max. $MAX_HER_DUEL_SERIE)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (probihaHra) Color(0xE60D3820) else Color(0xD9082916))
            .border(
                width = if (mujAktivniUzel != null || jsemPrihlasen) 1.6.dp else 1.dp,
                color = when {
                    mujAktivniUzel != null -> Color(0xFF00E676)
                    jsemPrihlasen -> Color(0xFFFFD54F)
                    else -> Color(0x77D4AF37)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                IkonaBarvyKarty(
                    barva = when (turnaj.typHry) {
                        TypHry.MARIAS -> BarvaKarty.CERVENE
                        TypHry.PRSI -> BarvaKarty.KULE
                        TypHry.SEDMA -> BarvaKarty.ZELENE
                    },
                    velikost = 18.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = turnaj.nazev,
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TurnajPravidlaStitek(turnaj.variantaPravidel)
        }

        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅 ${TurnajKalendar.formatujTerminTurnaje(turnaj.casStartuMs, nyniMs)}",
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            )
            Text(
                text = when {
                    probihaHra -> "🟢 PRÁVĚ BĚŽÍ"
                    zbyvaMs > 0L -> "⏳ Za ${TurnajKalendar.formatujOdpocetDoStartu(zbyvaMs)}"
                    else -> "⚡ Startuje!"
                },
                color = if (probihaHra || zbyvaMs <= 300_000L) Color(0xFF69F0AE) else Color(0xFFFFE082),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "⚔️ $formatZapasuText",
            color = Color(0xFFFFF59D),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Vklad: ${turnaj.vkladTereziany} ter.  •  🥇 1. místo: +${turnaj.odmenaPrvniMisto}  •  🥈 2. místo: +${turnaj.odmenaDruheMisto}",
            color = Color(0xFFB9F6CA),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(2.dp))
        val seznamNickuText = if (ziviPrihlaseni.isEmpty()) {
            "Zatím nikdo (volná místa při startu doplní boti)"
        } else {
            ziviPrihlaseni.joinToString(", ") { it.prezdivka }
        }
        Text(
            text = "Hráči (${ziviPrihlaseni.size}/${turnaj.kapacita}): $seznamNickuText",
            color = Color(0xFFE0E0E0),
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mujAktivniUzel != null) {
                Button(
                    onClick = onHratAktivniZapas,
                    enabled = !probihaAkce,
                    modifier = Modifier.weight(1.25f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text(
                        text = "▶ Hrát (${mujAktivniUzel.nazevZapasu})",
                        color = Color(0xFF04210E),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (turnaj.stav == "REGISTRACE") {
                if (!jsemPrihlasen && ziviPrihlaseni.size < turnaj.kapacita) {
                    Button(
                        onClick = onPrihlasit,
                        enabled = !probihaAkce,
                        modifier = Modifier.weight(1.25f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text(
                            text = if (ulozenyNick.isEmpty()) "Nastav nick" else "Přihlásit (${turnaj.vkladTereziany} ter.)",
                            color = Color(0xFF072312),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (jsemPrihlasen && zbyvaMs > 60_000L) {
                    Button(
                        onClick = onOdhlasit,
                        enabled = !probihaAkce,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D2424))
                    ) {
                        Text(
                            text = "Odhlásit",
                            color = Color(0xFFFFCCBC),
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF164A2C))
                    .border(1.dp, Color(0x88D4AF37), RoundedCornerShape(10.dp))
                    .clickable(onClick = onPrepnoutPavouka)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (jeRozbalenyPavouk) "▲ Skrýt" else "🏆 Pavouk",
                    color = Color(0xFFFFF59D),
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        if (jeRozbalenyPavouk) {
            Spacer(modifier = Modifier.height(10.dp))
            GrafickyPavoukTurnaje(turnaj = turnaj, mojeId = mojeId)
        }
    }
}

@Composable
private fun GrafickyPavoukTurnaje(
    turnaj: PlanovanyTurnaj,
    mojeId: String
) {
    val uzly = turnaj.ziskejUzlyProZobrazeni()
    if (uzly.isEmpty()) return

    val mierkaPisma = LocalDensity.current.fontScale.coerceIn(1f, 1.45f)
    val sirkaSloupce = (182f * mierkaPisma.coerceAtMost(1.22f)).dp
    val sirkaSpojky = 22.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC04180D))
            .border(1.dp, Color(0x55E6C15A), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = if (turnaj.typHry == TypHry.MARIAS) {
                "Pavouk — 3 stoly ($POCET_ROZDANI_MARIAS_TURNAJ rozdání) → Finále"
            } else {
                "Pavouk (na $CIL_VITEZNYCH_BITEV_DUELU vítězné) — Čtvrtfinále → Semifinále → Finále"
            },
            color = Color(0xFFFFE082),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Top
        ) {
            if (turnaj.typHry == TypHry.MARIAS && uzly.size >= 4) {
                val kartaVyska = (138f * mierkaPisma).dp
                val mezera = 12.dp
                val krok = kartaVyska + mezera
                val celkovaVyska = kartaVyska * 3 + mezera * 2
                val stredyKolo0 = listOf(
                    kartaVyska * 0.5f,
                    krok + kartaVyska * 0.5f,
                    krok * 2 + kartaVyska * 0.5f
                )
                val stredFinale = stredyKolo0[1]

                Box(modifier = Modifier.width(sirkaSloupce).height(celkovaVyska)) {
                    for (i in 0..2) {
                        KartaUzluPavouka(
                            uzel = uzly[i],
                            mojeId = mojeId,
                            sirka = sirkaSloupce,
                            vyska = kartaVyska,
                            modifier = Modifier.offset(y = krok * i)
                        )
                    }
                }

                SpojniceTrojicePavouka(
                    sirka = sirkaSpojky,
                    vyska = celkovaVyska,
                    vstupniStredy = stredyKolo0,
                    vystupniStred = stredFinale
                )

                Box(modifier = Modifier.width(sirkaSloupce).height(celkovaVyska)) {
                    KartaUzluPavouka(
                        uzel = uzly[3],
                        mojeId = mojeId,
                        sirka = sirkaSloupce,
                        vyska = kartaVyska,
                        modifier = Modifier.offset(y = stredFinale - kartaVyska * 0.5f)
                    )
                }
            } else if (uzly.size >= 7) {
                val kartaVyska = (106f * mierkaPisma).dp
                val mezera = 12.dp
                val krok = kartaVyska + mezera
                val celkovaVyska = kartaVyska * 4 + mezera * 3

                val qfStred0 = kartaVyska * 0.5f
                val qfStred1 = krok + kartaVyska * 0.5f
                val qfStred2 = krok * 2 + kartaVyska * 0.5f
                val qfStred3 = krok * 3 + kartaVyska * 0.5f

                val sfStred0 = (qfStred0 + qfStred1) * 0.5f
                val sfStred1 = (qfStred2 + qfStred3) * 0.5f
                val finStred = (sfStred0 + sfStred1) * 0.5f

                Box(modifier = Modifier.width(sirkaSloupce).height(celkovaVyska)) {
                    for (i in 0..3) {
                        KartaUzluPavouka(
                            uzel = uzly[i],
                            mojeId = mojeId,
                            sirka = sirkaSloupce,
                            vyska = kartaVyska,
                            modifier = Modifier.offset(y = krok * i)
                        )
                    }
                }

                SpojniceDvojicPavouka(
                    sirka = sirkaSpojky,
                    vyska = celkovaVyska,
                    paryVstupu = listOf(
                        Pair(qfStred0, qfStred1) to sfStred0,
                        Pair(qfStred2, qfStred3) to sfStred1
                    )
                )

                Box(modifier = Modifier.width(sirkaSloupce).height(celkovaVyska)) {
                    KartaUzluPavouka(
                        uzel = uzly[4],
                        mojeId = mojeId,
                        sirka = sirkaSloupce,
                        vyska = kartaVyska,
                        modifier = Modifier.offset(y = sfStred0 - kartaVyska * 0.5f)
                    )
                    KartaUzluPavouka(
                        uzel = uzly[5],
                        mojeId = mojeId,
                        sirka = sirkaSloupce,
                        vyska = kartaVyska,
                        modifier = Modifier.offset(y = sfStred1 - kartaVyska * 0.5f)
                    )
                }

                SpojniceDvojicPavouka(
                    sirka = sirkaSpojky,
                    vyska = celkovaVyska,
                    paryVstupu = listOf(
                        Pair(sfStred0, sfStred1) to finStred
                    )
                )

                Box(modifier = Modifier.width(sirkaSloupce).height(celkovaVyska)) {
                    KartaUzluPavouka(
                        uzel = uzly[6],
                        mojeId = mojeId,
                        sirka = sirkaSloupce,
                        vyska = kartaVyska,
                        modifier = Modifier.offset(y = finStred - kartaVyska * 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpojniceDvojicPavouka(
    sirka: Dp,
    vyska: Dp,
    paryVstupu: List<Pair<Pair<Dp, Dp>, Dp>>
) {
    Canvas(modifier = Modifier.width(sirka).height(vyska)) {
        val midX = size.width * 0.5f
        val barva = Color(0x99D4AF37)
        val tloustka = 2.dp.toPx()
        for ((vstupy, vystup) in paryVstupu) {
            val y1 = vstupy.first.toPx()
            val y2 = vstupy.second.toPx()
            val yOut = vystup.toPx()
            drawLine(barva, Offset(0f, y1), Offset(midX, y1), tloustka, StrokeCap.Round)
            drawLine(barva, Offset(0f, y2), Offset(midX, y2), tloustka, StrokeCap.Round)
            drawLine(barva, Offset(midX, y1), Offset(midX, y2), tloustka, StrokeCap.Round)
            drawLine(barva, Offset(midX, yOut), Offset(size.width, yOut), tloustka, StrokeCap.Round)
        }
    }
}

@Composable
private fun SpojniceTrojicePavouka(
    sirka: Dp,
    vyska: Dp,
    vstupniStredy: List<Dp>,
    vystupniStred: Dp
) {
    Canvas(modifier = Modifier.width(sirka).height(vyska)) {
        if (vstupniStredy.isEmpty()) return@Canvas
        val midX = size.width * 0.5f
        val barva = Color(0x99D4AF37)
        val tloustka = 2.dp.toPx()
        val yBody = vstupniStredy.map { it.toPx() }
        for (y in yBody) {
            drawLine(barva, Offset(0f, y), Offset(midX, y), tloustka, StrokeCap.Round)
        }
        drawLine(barva, Offset(midX, yBody.first()), Offset(midX, yBody.last()), tloustka, StrokeCap.Round)
        val yOut = vystupniStred.toPx()
        drawLine(barva, Offset(midX, yOut), Offset(size.width, yOut), tloustka, StrokeCap.Round)
    }
}

@Composable
private fun KartaUzluPavouka(
    uzel: UzelZapasuPavouka,
    mojeId: String,
    sirka: Dp,
    vyska: Dp,
    modifier: Modifier = Modifier
) {
    val mujZapas = uzel.obsahujeHrace(mojeId)
    val casKolText = TurnajKalendar.formatujHodinuMinutu(uzel.planovanyCasMs)

    Column(
        modifier = modifier
            .width(sirka)
            .height(vyska)
            .clip(RoundedCornerShape(10.dp))
            .background(if (mujZapas) Color(0xFF144528) else Color(0xFF0B2E1A))
            .border(
                width = if (mujZapas) 1.5.dp else 1.dp,
                color = when {
                    uzel.stavZapasu == "HRAJE_SE" && uzel.vitezId.isEmpty() -> Color(0xFF00E676)
                    mujZapas -> Color(0xFFFFD54F)
                    else -> Color(0x55D4AF37)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uzel.nazevZapasu,
                color = Color(0xFFFFD54F),
                fontSize = 10.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            Text(
                text = casKolText,
                color = Color(0xFFA5D6A7),
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (hrac in uzel.hraci) {
                val jeVitez = uzel.vitezId.isNotEmpty() && hrac.deviceId == uzel.vitezId
                val jsemToJa = mojeId.isNotEmpty() && hrac.deviceId == mojeId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            when {
                                jeVitez -> Color(0xFF1F6338)
                                jsemToJa -> Color(0xFF1B5230)
                                else -> Color(0x55000000)
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    jeVitez -> Color(0xFFFFD54F)
                                    hrac.jeVolneMisto -> Color(0xFF546E7A)
                                    hrac.jeBot -> Color(0xFF90CAF9)
                                    else -> Color(0xFF00E676)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = (if (jeVitez) "🏆 " else "") + hrac.prezdivka,
                        color = when {
                            jeVitez -> Color(0xFFFFF59D)
                            hrac.jeVolneMisto -> Color(0x88FFFFFF)
                            jsemToJa -> Color(0xFF69F0AE)
                            else -> Color.White
                        },
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontStyle = if (hrac.jeVolneMisto) FontStyle.Italic else FontStyle.Normal,
                        fontWeight = if (jeVitez || jsemToJa) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                    if (!hrac.jeVolneMisto && hrac.skoreText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = hrac.skoreText,
                            color = if (jeVitez) Color(0xFFFFD54F) else Color(0xFFB9F6CA),
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
