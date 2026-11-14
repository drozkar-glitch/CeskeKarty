package cz.mares.ceskekarty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ObrazovkaHerny {
    DOMOV,
    TRENINK_VYBER,
    ONLINE_LOBBY,
    CEKARNA_STOLU,
    TURNAJE,
    ZEBRICEK,
    PRAVIDLA,
    HERNI_STUL
}

@Composable
fun App() {
    MaterialTheme {
        var aktualniObrazovka by remember { mutableStateOf(ObrazovkaHerny.DOMOV) }
        var ulozenaPrezdivka by remember { mutableStateOf(nactiUlozenouPrezdivku()) }
        var ulozenyPinOtisk by remember { mutableStateOf(nactiUlozenyPinOtisk()) }
        var aktualniTereziany by remember { mutableStateOf(nactiLokalniTereziany()) }
        var dostupnaDenniOdmena by remember { mutableStateOf(muzeVyzvednoutDenniOdmenu()) }
        var vybranaPravidla by remember { mutableStateOf(nactiVychoziVariantuPravidel()) }

        var vybranyOfflineKonfig by remember { mutableStateOf<KonfiguraceOfflineHry?>(null) }
        var aktivniOnlineStul by remember { mutableStateOf<OnlineStul?>(null) }

        fun nastavVariantuPravidel(nova: VariantaPravidel) {
            vybranaPravidla = nova
            ulozVychoziVariantuPravidel(nova)
        }

        fun obnovStavUctuAMesce() {
            ulozenaPrezdivka = nactiUlozenouPrezdivku()
            ulozenyPinOtisk = nactiUlozenyPinOtisk()
            aktualniTereziany = nactiLokalniTereziany()
            dostupnaDenniOdmena = muzeVyzvednoutDenniOdmenu()
        }

        LaunchedEffect(ulozenaPrezdivka, ulozenyPinOtisk) {
            if (ulozenaPrezdivka.isNotEmpty() && ulozenyPinOtisk.isNotEmpty()) {
                aktualniTereziany = OnlineManager.synchronizujUcetPoStartu()
                dostupnaDenniOdmena = muzeVyzvednoutDenniOdmenu()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1F1006))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            when (aktualniObrazovka) {
                ObrazovkaHerny.DOMOV -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    DomovskaObrazovka(
                        ulozenaPrezdivka = ulozenaPrezdivka,
                        ulozenyPinOtisk = ulozenyPinOtisk,
                        aktualniTereziany = aktualniTereziany,
                        dostupnaDenniOdmena = dostupnaDenniOdmena,
                        onUcetNeboMesecZmenen = { obnovStavUctuAMesce() },
                        onOtevritZivouHernu = { aktualniObrazovka = ObrazovkaHerny.ONLINE_LOBBY },
                        onOtevritTurnaje = { aktualniObrazovka = ObrazovkaHerny.TURNAJE },
                        onOtevritTrenink = { aktualniObrazovka = ObrazovkaHerny.TRENINK_VYBER },
                        onOtevritZebricek = { aktualniObrazovka = ObrazovkaHerny.ZEBRICEK },
                        onOtevritPravidla = { aktualniObrazovka = ObrazovkaHerny.PRAVIDLA }
                    )
                }

                ObrazovkaHerny.TRENINK_VYBER -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    VyberTreninkuObrazovka(
                        vybranaVarianta = vybranaPravidla,
                        onZmenaVarianty = { nastavVariantuPravidel(it) },
                        onZpet = { aktualniObrazovka = ObrazovkaHerny.DOMOV },
                        onSpustitOffline = { konfig ->
                            vybranyOfflineKonfig = konfig
                            aktivniOnlineStul = null
                            aktualniObrazovka = ObrazovkaHerny.HERNI_STUL
                        }
                    )
                }

                ObrazovkaHerny.ONLINE_LOBBY -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    OnlineLobbyObrazovka(
                        vybranaVarianta = vybranaPravidla,
                        onZmenaVarianty = { nastavVariantuPravidel(it) },
                        onZpet = { aktualniObrazovka = ObrazovkaHerny.DOMOV },
                        onVstoupitDoStolu = { stul ->
                            aktivniOnlineStul = stul
                            vybranyOfflineKonfig = null
                            aktualniObrazovka = if (stul.stavMistnosti == StavMistnosti.HRAJE_SE) {
                                ObrazovkaHerny.HERNI_STUL
                            } else {
                                ObrazovkaHerny.CEKARNA_STOLU
                            }
                        }
                    )
                }

                ObrazovkaHerny.CEKARNA_STOLU -> ZelenyKaretniStulBox(zobrazitStredovyKruh = true) {
                    val stul = aktivniOnlineStul
                    if (stul == null) {
                        aktualniObrazovka = ObrazovkaHerny.ONLINE_LOBBY
                    } else {
                        CekarnaStoluObrazovka(
                            pocatecniStul = stul,
                            onHraOdstartovana = { spusteny ->
                                aktivniOnlineStul = spusteny
                                vybranyOfflineKonfig = null
                                aktualniObrazovka = ObrazovkaHerny.HERNI_STUL
                            },
                            onZrusit = {
                                val bylTurnaj = aktivniOnlineStul?.turnajId?.isNotEmpty() == true
                                aktivniOnlineStul = null
                                aktualniObrazovka = if (bylTurnaj) {
                                    ObrazovkaHerny.TURNAJE
                                } else {
                                    ObrazovkaHerny.ONLINE_LOBBY
                                }
                            }
                        )
                    }
                }

                ObrazovkaHerny.TURNAJE -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    TournamentHallScreen(
                        vybranaVarianta = vybranaPravidla,
                        onZmenaVarianty = { nastavVariantuPravidel(it) },
                        onZpet = {
                            obnovStavUctuAMesce()
                            aktualniObrazovka = ObrazovkaHerny.DOMOV
                        },
                        onUcetNeboMesecZmenen = { obnovStavUctuAMesce() },
                        onSpustitOfflinePohar = { typ, varianta ->
                            vybranyOfflineKonfig = KonfiguraceOfflineHry(
                                typHry = typ,
                                pocetHracu = typ.povolenePoctyHracu.first(),
                                variantaPravidel = varianta,
                                jeOfflinePohar = true,
                                celkemRozdani = 3
                            )
                            aktivniOnlineStul = null
                            aktualniObrazovka = ObrazovkaHerny.HERNI_STUL
                        },
                        onVstoupitDoZapasuTurnaje = { stul ->
                            aktivniOnlineStul = stul
                            vybranyOfflineKonfig = null
                            aktualniObrazovka = if (stul.stavMistnosti == StavMistnosti.HRAJE_SE) {
                                ObrazovkaHerny.HERNI_STUL
                            } else {
                                ObrazovkaHerny.CEKARNA_STOLU
                            }
                        }
                    )
                }

                ObrazovkaHerny.ZEBRICEK -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    ZebricekObrazovka(
                        onZpet = { aktualniObrazovka = ObrazovkaHerny.DOMOV }
                    )
                }

                ObrazovkaHerny.PRAVIDLA -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    PravidlaObrazovka(
                        onZpet = { aktualniObrazovka = ObrazovkaHerny.DOMOV }
                    )
                }

                ObrazovkaHerny.HERNI_STUL -> GameTableScreen(
                    offlineKonfig = vybranyOfflineKonfig,
                    pocatecniOnlineStul = aktivniOnlineStul,
                    onVratitDoCekarny = { upravenyStul ->
                        obnovStavUctuAMesce()
                        aktivniOnlineStul = upravenyStul
                        vybranyOfflineKonfig = null
                        aktualniObrazovka = ObrazovkaHerny.CEKARNA_STOLU
                    },
                    onOpustitStul = {
                        obnovStavUctuAMesce()
                        val navratDoTurnaje = aktivniOnlineStul?.turnajId?.isNotEmpty() == true ||
                            vybranyOfflineKonfig?.jeOfflinePohar == true
                        aktivniOnlineStul = null
                        vybranyOfflineKonfig = null
                        aktualniObrazovka = if (navratDoTurnaje) {
                            ObrazovkaHerny.TURNAJE
                        } else {
                            ObrazovkaHerny.DOMOV
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PrepinacPravidelPanel(
    vybrana: VariantaPravidel,
    onZmena: (VariantaPravidel) -> Unit,
    zobrazitPodrobnyPopis: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xD9082916))
            .border(1.2.dp, Color(0x99D4AF37), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Varianta pravidel hry:",
            color = Color(0xFFFFD54F),
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (varianta in VariantaPravidel.entries) {
                val aktivni = vybrana == varianta
                val ikona = if (varianta == VariantaPravidel.HOSPODSKA) "🍺" else "🏛️"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (aktivni) Color(0xFFD4AF37) else Color(0xFF194D2F))
                        .border(
                            width = if (aktivni) 1.5.dp else 1.dp,
                            color = if (aktivni) Color(0xFFFFF59D) else Color(0x55D4AF37),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            jemnaHaptika()
                            onZmena(varianta)
                        }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$ikona ${varianta.kratkyNazev}",
                        color = if (aktivni) Color(0xFF072312) else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (zobrazitPodrobnyPopis) {
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = vybrana.popisVyberu,
                color = Color(0xFFB9F6CA),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun StitekPravidelBadge(varianta: VariantaPravidel) {
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
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DomovskaObrazovka(
    ulozenaPrezdivka: String,
    ulozenyPinOtisk: String,
    aktualniTereziany: Int,
    dostupnaDenniOdmena: Boolean,
    onUcetNeboMesecZmenen: () -> Unit,
    onOtevritZivouHernu: () -> Unit,
    onOtevritTurnaje: () -> Unit,
    onOtevritTrenink: () -> Unit,
    onOtevritZebricek: () -> Unit,
    onOtevritPravidla: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var vstupNick by remember { mutableStateOf("") }
    var vstupPin by remember { mutableStateOf("") }
    var stavHlaska by remember { mutableStateOf("") }
    var jeUspechHlaska by remember { mutableStateOf(false) }
    var probihaAkce by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((-18).dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            KartaLiceComposable(
                karta = Karta(BarvaKarty.ZALUDY, HodnotaKarty.KRAL),
                sirka = 62.dp,
                modifier = Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        rotationZ = -13f
                        translationY = 8f
                    }
            )
            KartaLiceComposable(
                karta = Karta(BarvaKarty.CERVENE, HodnotaKarty.ESO),
                sirka = 68.dp,
                modifier = Modifier.zIndex(3f)
            )
            KartaLiceComposable(
                karta = Karta(BarvaKarty.KULE, HodnotaKarty.SVRSEK),
                sirka = 62.dp,
                modifier = Modifier
                    .zIndex(2f)
                    .graphicsLayer {
                        rotationZ = 13f
                        translationY = 8f
                    }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "ČESKÉ KARTY",
            color = Color(0xFFFFD54F),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Mariáš • Prší • Sedma — hospodská i svazová pravidla",
            color = Color(0xFFDCE775),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (ulozenaPrezdivka.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD9082916))
                    .border(1.5.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Registrace přezdívky nebo obnova účtu",
                    color = Color(0xFFFFE082),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Nový účet získá do začátku 1000 tereziánů. Pokud už účet máš, zadej svou přezdívku a 4místný PIN pro obnovu celého měšce.",
                    color = Color(0xFFB9F6CA),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = vstupNick,
                        onValueChange = { vstupNick = it.take(18) },
                        singleLine = true,
                        placeholder = { Text("Přezdívka", color = Color(0x99FFFFFF), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            cursorColor = Color(0xFFFFD54F)
                        ),
                        modifier = Modifier.weight(1.7f)
                    )
                    OutlinedTextField(
                        value = vstupPin,
                        onValueChange = { vstupPin = it.filter { ch -> ch.isDigit() }.take(4) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = { Text("PIN (4 čís.)", color = Color(0x99FFFFFF), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color(0x66FFFFFF),
                            cursorColor = Color(0xFFFFD54F)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (stavHlaska.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stavHlaska,
                        color = if (jeUspechHlaska) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        jemnaHaptika()
                        if (probihaAkce) return@Button
                        probihaAkce = true
                        stavHlaska = ""
                        scope.launch {
                            when (val vys = OnlineManager.overAUlozPrezdivku(vstupNick, vstupPin)) {
                                is VysledekRegistraceNicku.Uspech -> {
                                    silnaHaptika()
                                    jeUspechHlaska = true
                                    stavHlaska = vys.zprava
                                    onUcetNeboMesecZmenen()
                                }
                                is VysledekRegistraceNicku.Chyba -> {
                                    jeUspechHlaska = false
                                    stavHlaska = vys.zprava
                                }
                            }
                            probihaAkce = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text(
                        text = if (probihaAkce) "Ověřuji na serveru…" else "Uložit / Obnovit účet",
                        color = Color(0xFF072312),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD9082916))
                    .border(1.2.dp, Color(0x99D4AF37), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = ulozenaPrezdivka,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (ulozenyPinOtisk.isNotEmpty()) {
                                Text(
                                    text = "Chráněno PINem",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF15492B))
                            .border(1.dp, Color(0xAAFFD54F), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = formatujTereziany(aktualniTereziany),
                            color = Color(0xFFFFD54F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val narokNaPodporu = aktualniTereziany < HRANICE_PODPORY_TEREZIANY
                if (dostupnaDenniOdmena || narokNaPodporu) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (dostupnaDenniOdmena) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    if (probihaAkce) return@Button
                                    probihaAkce = true
                                    scope.launch {
                                        val vys = OnlineManager.vyzvedniDenniOdmenu()
                                        vys.onSuccess { novyStav ->
                                            silnaHaptika()
                                            HospodaZvuk.cinkniVyuctovani()
                                            jeUspechHlaska = true
                                            stavHlaska = "Vyzvednuta denní odměna +$DENNI_ODMENA_TEREZIANY tereziánů! Celkem máš ${formatujTereziany(novyStav)}."
                                            onUcetNeboMesecZmenen()
                                        }.onFailure { err ->
                                            jeUspechHlaska = false
                                            stavHlaska = err.message ?: "Denní odměnu nelze vyzvednout."
                                        }
                                        probihaAkce = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                            ) {
                                Text(
                                    text = "Denní odměna (+$DENNI_ODMENA_TEREZIANY tereziánů)",
                                    color = Color(0xFF04210E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (narokNaPodporu) {
                            Button(
                                onClick = {
                                    jemnaHaptika()
                                    if (probihaAkce) return@Button
                                    probihaAkce = true
                                    scope.launch {
                                        val vys = OnlineManager.aktivujCisarskouPodporu()
                                        vys.onSuccess { dorovnano ->
                                            silnaHaptika()
                                            HospodaZvuk.cinkniVyuctovani()
                                            jeUspechHlaska = true
                                            stavHlaska = "Císařská podpora dorovnala tvůj měšec na ${formatujTereziany(dorovnano)}!"
                                            onUcetNeboMesecZmenen()
                                        }.onFailure { err ->
                                            jeUspechHlaska = false
                                            stavHlaska = err.message ?: "Podporu nelze uplatnit."
                                        }
                                        probihaAkce = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                            ) {
                                Text(
                                    text = "Císařská podpora (dorovnat na $DOROVNANI_PODPORY_TEREZIANY)",
                                    color = Color(0xFF072312),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                if (ulozenyPinOtisk.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nastav si 4místný PIN pro obnovu účtu a tereziánů v jiném telefonu:",
                        color = Color(0xFFFFE082),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = vstupPin,
                            onValueChange = { vstupPin = it.filter { ch -> ch.isDigit() }.take(4) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("4místný PIN", color = Color(0x99FFFFFF), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color(0x66FFFFFF)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                jemnaHaptika()
                                if (probihaAkce) return@Button
                                probihaAkce = true
                                scope.launch {
                                    when (val vys = OnlineManager.doplnPinKeStavajiciPrezdivce(vstupPin)) {
                                        is VysledekRegistraceNicku.Uspech -> {
                                            silnaHaptika()
                                            jeUspechHlaska = true
                                            stavHlaska = vys.zprava
                                            onUcetNeboMesecZmenen()
                                        }
                                        is VysledekRegistraceNicku.Chyba -> {
                                            jeUspechHlaska = false
                                            stavHlaska = vys.zprava
                                        }
                                    }
                                    probihaAkce = false
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text("Uložit PIN", color = Color(0xFF072312), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                if (stavHlaska.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stavHlaska,
                        color = if (jeUspechHlaska) Color(0xFF69F0AE) else Color(0xFFFF8A80),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        MenuKartaTlacitko(
            nadpis = "Živá online herna (proti hráčům)",
            popis = "Veřejné i soukromé stoly s volbou hospodských či svazových pravidel a hrou o tereziány.",
            zvyraznena = true,
            onClick = {
                if (ulozenaPrezdivka.isEmpty()) {
                    jeUspechHlaska = false
                    stavHlaska = "Nejprve si prosím ulož nebo obnov přezdívku výše."
                } else if (aktualniTereziany < 20) {
                    jeUspechHlaska = false
                    stavHlaska = "Pro živý stůl potřebuješ alespoň 20 tereziánů — vyzvedni si nahoře Císařskou podporu zdarma!"
                } else {
                    onOtevritZivouHernu()
                }
            }
        )
        Spacer(modifier = Modifier.height(10.dp))

        MenuKartaTlacitko(
            nadpis = "Turnajová síň a poháry",
            popis = "Denní rozvrh turnajů s pavoukem (19:00 Prší, 20:00 Sedma, 20:30 Mariáš) i rychlý pohár s boty.",
            zvyraznena = false,
            onClick = onOtevritTurnaje
        )
        Spacer(modifier = Modifier.height(10.dp))

        MenuKartaTlacitko(
            nadpis = "Trénink proti botům (Offline)",
            popis = "Okamžitá hra Mariáše, Prší nebo Sedmy podle hospodských či svazových pravidel i bez internetu.",
            zvyraznena = false,
            onClick = onOtevritTrenink
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                MenuKartaTlacitko(
                    nadpis = "Nejbohatší hráči",
                    popis = "Online žebříček podle měšce tereziánů.",
                    zvyraznena = false,
                    onClick = onOtevritZebricek
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                MenuKartaTlacitko(
                    nadpis = "Pravidla a měna",
                    popis = "Rozdíly hospodských a svazových pravidel.",
                    zvyraznena = false,
                    onClick = onOtevritPravidla
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "© 2026 Created by Pavel Mareš",
            color = Color(0xBBFFE082),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MenuKartaTlacitko(
    nadpis: String,
    popis: String,
    zvyraznena: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (zvyraznena) {
                    Brush.horizontalGradient(listOf(Color(0xE6165832), Color(0xE60D3D22)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xCC092D19), Color(0xCC062111)))
                }
            )
            .border(
                width = if (zvyraznena) 1.8.dp else 1.dp,
                color = if (zvyraznena) Color(0xFFD4AF37) else Color(0x66D4AF37),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                jemnaHaptika()
                onClick()
            }
            .padding(14.dp)
    ) {
        Text(
            text = nadpis,
            color = if (zvyraznena) Color(0xFFFFD54F) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = popis,
            color = Color(0xFFC8E6C9),
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ZahlaviPodobrazovky(nadpis: String, onZpet: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
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
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "← Zpět",
                color = Color(0xFFFFE082),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = nadpis,
            color = Color(0xFFFFD54F),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VyberTreninkuObrazovka(
    vybranaVarianta: VariantaPravidel,
    onZmenaVarianty: (VariantaPravidel) -> Unit,
    onZpet: () -> Unit,
    onSpustitOffline: (KonfiguraceOfflineHry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ZahlaviPodobrazovky("Trénink proti botům", onZpet)

        PrepinacPravidelPanel(
            vybrana = vybranaVarianta,
            onZmena = onZmenaVarianty
        )
        Spacer(modifier = Modifier.height(12.dp))

        for (typ in TypHry.entries) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD9082916))
                    .border(1.dp, Color(0x77D4AF37), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        IkonaBarvyKarty(
                            barva = when (typ) {
                                TypHry.MARIAS -> BarvaKarty.CERVENE
                                TypHry.PRSI -> BarvaKarty.KULE
                                TypHry.SEDMA -> BarvaKarty.ZELENE
                            },
                            velikost = 22.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = typ.nazev,
                            color = Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StitekPravidelBadge(vybranaVarianta)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = typ.kratkyPopis,
                    color = Color(0xFFE0E0E0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val moznostiPoctu = typ.povolenePoctyHracu
                    for (pocet in moznostiPoctu) {
                        val popisek = when {
                            typ == TypHry.SEDMA && pocet == 2 -> "1 na 1 (2 hráči)"
                            typ == TypHry.SEDMA && pocet == 4 -> "Čtyřhra 2 na 2"
                            moznostiPoctu.size >= 3 -> "$pocet hráči"
                            else -> "Hrát ($pocet hráči)"
                        }
                        Button(
                            onClick = {
                                jemnaHaptika()
                                onSpustitOffline(
                                    KonfiguraceOfflineHry(
                                        typHry = typ,
                                        pocetHracu = pocet,
                                        variantaPravidel = vybranaVarianta
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text(
                                text = popisek,
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlineLobbyObrazovka(
    vybranaVarianta: VariantaPravidel,
    onZmenaVarianty: (VariantaPravidel) -> Unit,
    onZpet: () -> Unit,
    onVstoupitDoStolu: (OnlineStul) -> Unit
) {
    val scope = rememberCoroutineScope()
    var stoly by remember { mutableStateOf(emptyList<OnlineStul>()) }
    var nacitaSe by remember { mutableStateOf(true) }
    var zpravaChyby by remember { mutableStateOf("") }

    var vybranaHraZalozeni by remember { mutableStateOf(TypHry.MARIAS) }
    var vybranyPocetZalozeni by remember { mutableStateOf(3) }
    var soukromyPin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            stoly = OnlineManager.nactiZiveStoly()
            nacitaSe = false
            delay(3500L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ZahlaviPodobrazovky("Živá online herna", onZpet)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD9082916))
                .border(1.2.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("Založit nový stůl", color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (typ in TypHry.entries) {
                    val aktivni = vybranaHraZalozeni == typ
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (aktivni) Color(0xFFD4AF37) else Color(0xFF1D5636))
                            .clickable {
                                jemnaHaptika()
                                vybranaHraZalozeni = typ
                                vybranyPocetZalozeni = typ.povolenePoctyHracu.first()
                            }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = typ.nazev,
                            color = if (aktivni) Color(0xFF072312) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (vybranaHraZalozeni.povolenePoctyHracu.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in vybranaHraZalozeni.povolenePoctyHracu) {
                        val akt = vybranyPocetZalozeni == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (akt) Color(0xFF69F0AE) else Color(0xFF18492D))
                                .clickable {
                                    jemnaHaptika()
                                    vybranyPocetZalozeni = p
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$p hráči",
                                color = if (akt) Color(0xFF062111) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
                            .background(if (akt) Color(0xFF69F0AE) else Color(0xFF18492D))
                            .clickable {
                                jemnaHaptika()
                                onZmenaVarianty(varianta)
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$ikona ${varianta.kratkyNazev}",
                            color = if (akt) Color(0xFF062111) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = soukromyPin,
                    onValueChange = { soukromyPin = it.filter { ch -> ch.isDigit() }.take(4) },
                    singleLine = true,
                    placeholder = { Text("PIN stolu (volitelně)", color = Color(0x88FFFFFF), fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color(0x55FFFFFF)
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        jemnaHaptika()
                        zpravaChyby = ""
                        scope.launch {
                            val vytvoreny = OnlineManager.vytvorStul(
                                nazevStolu = "${vybranaHraZalozeni.nazev} (${nactiUlozenouPrezdivku()})",
                                typHry = vybranaHraZalozeni,
                                pocetHracu = vybranyPocetZalozeni,
                                pinKod = soukromyPin,
                                variantaPravidel = vybranaVarianta
                            )
                            if (vytvoreny != null) {
                                onVstoupitDoStolu(vytvoreny)
                            } else {
                                zpravaChyby = "Nepodařilo se otevřít stůl — zkontroluj připojení nebo Rules ve Firebase."
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text(
                        text = "Otevřít stůl",
                        color = Color(0xFF04210E),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (zpravaChyby.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(zpravaChyby, color = Color(0xFFFF8A80), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Otevřené stoly v herně:", color = Color(0xFFFFE082), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (nacitaSe) {
            Text("Načítám živé stoly…", color = Color(0xFFB9F6CA), fontSize = 13.sp)
        } else if (stoly.isEmpty()) {
            Text(
                text = "Zatím není otevřený žádný čekající stůl — založ první stůl nahoře a pozvi spoluhráče.",
                color = Color(0xFFB9F6CA),
                fontSize = 13.sp
            )
        } else {
            val mojeId = ziskejUnikatniIdZarizeni()
            for (stul in stoly) {
                key(stul.roomId) {
                    var zadavanyPinProVstup by remember { mutableStateOf("") }
                    val jsemUvnitr = stul.hraci.any { it.deviceId == mojeId }
                    val muzePrisednout = stul.stavMistnosti == StavMistnosti.CEKANI || jsemUvnitr
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xD9082916))
                            .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = (if (stul.jeSoukromy) "🔒 " else "") + stul.nazevStolu,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    StitekPravidelBadge(stul.variantaPravidel)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Hráči (${stul.hraci.size}/${stul.maxHracu}): " +
                                        stul.hraci.joinToString(", ") { it.prezdivka },
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 12.sp
                                )
                            }
                            if (muzePrisednout) {
                                Button(
                                    onClick = {
                                        jemnaHaptika()
                                        scope.launch {
                                            val vys = OnlineManager.prisedniKeStolu(stul.roomId, zadavanyPinProVstup)
                                            vys.onSuccess { onVstoupitDoStolu(it) }
                                                .onFailure { zpravaChyby = it.message ?: "Nelze přisednout." }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                                ) {
                                    Text(
                                        text = if (jsemUvnitr) "Vrátit se" else "Přisednout",
                                        color = Color(0xFF072312),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Text("Hraje se", color = Color(0xFFFFCC80), fontSize = 12.sp)
                            }
                        }

                        if (stul.jeSoukromy && !jsemUvnitr && stul.stavMistnosti == StavMistnosti.CEKANI) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = zadavanyPinProVstup,
                                onValueChange = { zadavanyPinProVstup = it.filter { c -> c.isDigit() }.take(4) },
                                singleLine = true,
                                placeholder = { Text("Zadej 4místný PIN stolu", color = Color(0x88FFFFFF), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CekarnaStoluObrazovka(
    pocatecniStul: OnlineStul,
    onHraOdstartovana: (OnlineStul) -> Unit,
    onZrusit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var stul by remember { mutableStateOf(pocatecniStul) }
    var nyniMs by remember { mutableStateOf(ziskejAktualniCasMs()) }
    var probihaRozdavani by remember { mutableStateOf(false) }
    val mojeId = remember { ziskejUnikatniIdZarizeni() }

    LaunchedEffect(stul.roomId) {
        while (true) {
            OnlineManager.odesliTepSpojeni(stul.roomId)
            val cerstvy = OnlineManager.nactiKonkretniStul(stul.roomId)
            nyniMs = ziskejAktualniCasMs()
            if (cerstvy == null) {
                onZrusit()
                break
            }
            stul = cerstvy
            if (cerstvy.stavMistnosti == StavMistnosti.HRAJE_SE && cerstvy.serializovanyStavHry.isNotBlank()) {
                silnaHaptika()
                onHraOdstartovana(cerstvy)
                break
            }
            val odpovednyHostId = cerstvy.hraci.firstOrNull { it.deviceId == cerstvy.hostDeviceId }?.deviceId
                ?: cerstvy.hraci.firstOrNull { !jeBotDeviceId(it.deviceId) }?.deviceId
            if (!probihaRozdavani &&
                cerstvy.stavMistnosti == StavMistnosti.CEKANI &&
                cerstvy.hraci.size == cerstvy.maxHracu &&
                odpovednyHostId == mojeId
            ) {
                probihaRozdavani = true
                val spusteny = OnlineManager.rozdejASpustStul(cerstvy, zacinajiciIndex = 0)
                probihaRozdavani = false
                if (spusteny != null) {
                    silnaHaptika()
                    onHraOdstartovana(spusteny)
                    break
                }
            }
            delay(1800L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stul.nazevStolu,
            color = Color(0xFFFFD54F),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        StitekPravidelBadge(stul.variantaPravidel)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Čekání na spoluhráče (${stul.hraci.size}/${stul.maxHracu})" +
                if (stul.jeSoukromy) " • PIN: ${stul.pinKod}" else "",
            color = Color(0xFFB9F6CA),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until stul.maxHracu) {
            val hrac = stul.hraci.getOrNull(i)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD9082916))
                    .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                val jeOnline = hrac != null &&
                    stul.infoPripojeni(hrac.deviceId, nyniMs).stav == StavPripojeniHrace.ONLINE_ZELENA
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (hrac == null) Color(0xFF546E7A) else if (jeOnline) Color(0xFF00E676) else Color(0xFFFFCA28))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = hrac?.let { "${it.prezdivka} (${formatujTereziany(it.tereziany)})" } ?: "Volné místo u stolu…",
                    color = if (hrac != null) Color.White else Color(0x99FFFFFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                jemnaHaptika()
                scope.launch {
                    OnlineManager.opustStul(stul.roomId)
                    onZrusit()
                }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
            Text("Opustit čekárnu", color = Color(0xFFFFFFFF), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ZebricekObrazovka(onZpet: () -> Unit) {
    var polozky by remember { mutableStateOf(emptyList<ZaznamZebricku>()) }
    var nacita by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        polozky = OnlineManager.nactiZebricek(includeZeroScores = false)
        nacita = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ZahlaviPodobrazovky("Nejbohatší hráči", onZpet)

        if (nacita) {
            Text("Načítám online pořadí měšců…", color = Color(0xFFB9F6CA), fontSize = 14.sp)
        } else if (polozky.isEmpty()) {
            Text("Žebříček je zatím prázdný — založ si účet a odehraj první partii!", color = Color(0xFFB9F6CA), fontSize = 14.sp)
        } else {
            for (z in polozky) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (z.jeLokalniHrac) Color(0xE6185732) else Color(0xD9082916))
                        .border(
                            width = if (z.jeLokalniHrac) 1.6.dp else 1.dp,
                            color = if (z.jeLokalniHrac) Color(0xFFFFD54F) else Color(0x44D4AF37),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${z.poradi}. ${z.prezdivka}",
                        color = if (z.jeLokalniHrac) Color(0xFFFFD54F) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = formatujTereziany(z.tereziany),
                        color = Color(0xFF69F0AE),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PravidlaObrazovka(onZpet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ZahlaviPodobrazovky("Pravidla a měna", onZpet)

        BlokPravidel(
            nadpis = "Volba pravidel: Hospodská vs. Svazová",
            text = "Před spuštěním tréninku, při zakládání živého online stolu i v turnajích platí Hospodská nebo Svazová (klasická soutěžní) pravidla. Zvolená varianta je vždy viditelná na štítku u každého stolu i turnaje."
        )
        BlokPravidel(
            nadpis = "Turnajová síň a denní rozvrh s pavoukem",
            text = "Každý den se automaticky vypisují večerní turnaje: 19:00 Prší (pavouk 8 hráčů), 20:00 Sedma (pavouk 8 hráčů) a 20:30 Volený Mariáš (3 semifinálové stoly po 3 hráčích na 3 rozdání se postupem vítězů do Finálového stolu). Přihlášené přezdívky se okamžitě zapisují do pavouka; případná volná místa v hodinu startu doplní turnajoví boti, takže se turnaj odehraje v každém případě."
        )
        BlokPravidel(
            nadpis = "Sedma — Hospodská vs. Svazová pravidla",
            text = "Štych bere karta stejné hodnoty jako první vynesená nebo libovolná sedmička; počítají se desítky, esa a poslední štych po 10 bodech.\n• Svazová (klasická) pravidla: V duelu 1 na 1 končí štych po nepřebité dvojici (třetí kartou lze pokračovat jen tehdy, když soupeř štych přebil). Spálená (4×) platí pouze za 4 stejné karty odehrané v jednom štychu na stole.\n• Hospodská pravidla: Spálená (4×) platí jak ve štychu na stole, tak i při 4 stejných kartách na ruce (tlačítko Hlásit spálenou z ruky v 1 na 1 i ve čtyřhře). V duelu 1 na 1 navíc může vynášející natahovat štych dalšími kartami stejné hodnoty nebo sedmičkou i tehdy, když soupeř nepřebil, případně štych kdykoliv sebrat tlačítkem Vzít štych."
        )
        BlokPravidel(
            nadpis = "Volený Mariáš — Hospodská vs. Svazová pravidla",
            text = "Forhont volí trumf ze 7 karet (nebo naslepo z talonu) a po dobrání odkládá 2 karty mimo es a desítek. Kromě Hry, Betlu a Durchu lze hlásit Stovku (100), Sedmu i Dvě sedmy (9. štych vedlejší sedmičkou a 10. štych trumfovou sedmičkou). Nehlášená trumfová sedma v 10. štychu platí jako tichá sedma (při přebití jako zabitá tichá sedma). Červené trumfy platí dvojnásobně, hlášky 20/40 platí straně s alespoň jedním štychem a flekovat lze v řadě Flek (2×), Re (4×), Tutti (8×), Boty (16×) až Kalhoty (32×).\n• Svazová pravidla: Sedmu i Dvě sedmy smí forhont hlásit pouze tehdy, když příslušné sedmičky po odložení talonu skutečně drží v ruce (zákaz blafování).\n• Hospodská pravidla: Forhont smí ohlásit i tzv. falešnou sedmu nebo falešné dvě sedmy jako blaf, i když trumfovou sedmu v ruce nemá."
        )
        BlokPravidel(
            nadpis = "Prší — Hospodská vs. Svazová pravidla",
            text = "Přikládá se stejná barva nebo hodnota. Sedmička nutí dalšího líznout 2 karty (lze přebíjet další sedmičkou na 4, 6 či 8 karet), eso zastavuje na 1 kolo a svršek mění barvu.\n• Svazová (klasická) pravidla: Aktivní stojící eso lze přebít výhradně dalším esem.\n• Hospodská pravidla: Aktivní stojící eso lze přebít nejen dalším esem, ale také Červenou sedmou, která přebije stání a rovnou pošle dalšímu hráči líznutí 2 karet."
        )
        BlokPravidel(
            nadpis = "Měna tereziány a obnova účtu přes PIN",
            text = "Každý nový hráč dostává do začátku měšec s 1000 tereziány. Jednou denně si na hlavní obrazovce můžeš vyzvednout denní odměnu +100 tereziánů. Klesne-li tvůj měšec pod 100 tereziánů, tlačítko Císařská podpora ti ho zdarma dorovná na 200 tereziánů, případně si můžeš kdykoliv přivydělat v tréninku s boty. Díky 4místnému PINu si celý svůj účet i měšec obnovíš po přeinstalaci nebo v novém telefonu."
        )
        BlokPravidel(
            nadpis = "Hospodské hlášky a přehledný stůl",
            text = "Během partie doprovázejí hru české hospodské hlášky a zvuk karet na plátně, přičemž každý dohraný štych zůstane na stole chvíli viditelný před sebráním, abys měl přehled o každé odhozené kartě. Zvuky i hlášky můžeš u stolu kdykoliv zapnout nebo ztišit tlačítkem reproduktoru vpravo nahoře."
        )
        BlokPravidel(
            nadpis = "Živé online stoly bez botů",
            text = "Karty při každém rozdání míchá server a každý hráč může mít otevřený nejvýše jeden vlastní stůl. Zelená ikona u přezdívky hlídá živé spojení — při výpadku delším než 60 sekund se běžný stůl v herně čistě uzavře bez zásahu botů a přítomným hráčům se žádné tereziány neodečtou."
        )
        BlokPravidel(
            nadpis = "Původ a licence karetního obrazu",
            text = "Jednohlavé mariášové karty vycházejí z tradičního českého (pražského) obrazu: dílo „Bohemian pattern“, autor skenu Countakeshi, zdroj Wikimedia Commons, použito a upraveno pod licencí Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0, https://creativecommons.org/licenses/by-sa/4.0/)."
        )
    }
}

@Composable
private fun BlokPravidel(nadpis: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xD9082916))
            .border(1.dp, Color(0x55D4AF37), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(nadpis, color = Color(0xFFFFD54F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, color = Color(0xFFE0E0E0), fontSize = 12.sp, lineHeight = 17.sp)
    }
}
