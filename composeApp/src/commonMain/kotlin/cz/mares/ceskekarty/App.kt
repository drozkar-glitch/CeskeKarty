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
        var zvukZapnut by remember { mutableStateOf(jeZvukAHlaskyZapnuty()) }

        var vybranyOfflineKonfig by remember { mutableStateOf<KonfiguraceOfflineHry?>(null) }
        var aktivniOnlineStul by remember { mutableStateOf<OnlineStul?>(null) }

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
                        zvukZapnut = zvukZapnut,
                        onPrepnoutZvuk = {
                            val novy = !zvukZapnut
                            zvukZapnut = novy
                            HospodaZvuk.priPrepnutiZvuku(novy)
                        },
                        onUcetNeboMesecZmenen = {
                            ulozenaPrezdivka = nactiUlozenouPrezdivku()
                            ulozenyPinOtisk = nactiUlozenyPinOtisk()
                            aktualniTereziany = nactiLokalniTereziany()
                            dostupnaDenniOdmena = muzeVyzvednoutDenniOdmenu()
                        },
                        onOtevritZivouHernu = { aktualniObrazovka = ObrazovkaHerny.ONLINE_LOBBY },
                        onOtevritTurnaje = { aktualniObrazovka = ObrazovkaHerny.TURNAJE },
                        onOtevritTrenink = { aktualniObrazovka = ObrazovkaHerny.TRENINK_VYBER },
                        onOtevritZebricek = { aktualniObrazovka = ObrazovkaHerny.ZEBRICEK },
                        onOtevritPravidla = { aktualniObrazovka = ObrazovkaHerny.PRAVIDLA }
                    )
                }

                ObrazovkaHerny.TRENINK_VYBER -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    VyberTreninkuObrazovka(
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
                                aktivniOnlineStul = null
                                aktualniObrazovka = ObrazovkaHerny.ONLINE_LOBBY
                            }
                        )
                    }
                }

                ObrazovkaHerny.TURNAJE -> ZelenyKaretniStulBox(zobrazitStredovyKruh = false) {
                    TurnajovaSinObrazovka(
                        onZpet = { aktualniObrazovka = ObrazovkaHerny.DOMOV },
                        onSpustitOfflinePohar = { typ ->
                            vybranyOfflineKonfig = KonfiguraceOfflineHry(
                                typHry = typ,
                                pocetHracu = typ.povolenePoctyHracu.first(),
                                jeOfflinePohar = true,
                                celkemRozdani = 3
                            )
                            aktivniOnlineStul = null
                            aktualniObrazovka = ObrazovkaHerny.HERNI_STUL
                        },
                        onVstoupitDoZapasuTurnaje = { stul ->
                            aktivniOnlineStul = stul
                            vybranyOfflineKonfig = null
                            aktualniObrazovka = ObrazovkaHerny.HERNI_STUL
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
                        zvukZapnut = jeZvukAHlaskyZapnuty()
                        aktualniTereziany = nactiLokalniTereziany()
                        aktivniOnlineStul = upravenyStul
                        vybranyOfflineKonfig = null
                        aktualniObrazovka = ObrazovkaHerny.CEKARNA_STOLU
                    },
                    onOpustitStul = {
                        zvukZapnut = jeZvukAHlaskyZapnuty()
                        aktualniTereziany = nactiLokalniTereziany()
                        dostupnaDenniOdmena = muzeVyzvednoutDenniOdmenu()
                        val navratDoTurnaje = aktivniOnlineStul?.turnajId?.isNotEmpty() == true
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DomovskaObrazovka(
    ulozenaPrezdivka: String,
    ulozenyPinOtisk: String,
    aktualniTereziany: Int,
    dostupnaDenniOdmena: Boolean,
    zvukZapnut: Boolean,
    onPrepnoutZvuk: () -> Unit,
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-18).dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 4.dp)
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (zvukZapnut) Color(0xD9164B2C) else Color(0xD93E2723))
                    .border(
                        width = 1.dp,
                        color = if (zvukZapnut) Color(0x8869F0AE) else Color(0x88FF8A80),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        jemnaHaptika()
                        onPrepnoutZvuk()
                    }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (zvukZapnut) "🔊 Hlášky" else "🔇 Ztišeno",
                    color = if (zvukZapnut) Color(0xFFFFF59D) else Color(0xFFFFCCBC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "ČESKÉ KARTY",
            color = Color(0xFFFFD54F),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Mariáš • Prší • Sedma — online herna o tereziány",
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
                        modifier = Modifier.weight(1f)
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
                                    text = "Účet chráněn PINem",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 10.sp
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
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = formatujTereziany(aktualniTereziany),
                            color = Color(0xFFFFD54F),
                            fontSize = 14.sp,
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
            popis = "Veřejné stoly i soukromé stoly na PIN, kontrolka spojení a vyúčtování v tereziánech.",
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
            popis = "Vyřazovací pavouk pro Prší a Sedmu, bodovaná série Mariáše i okamžitý pohár s boty.",
            zvyraznena = false,
            onClick = onOtevritTurnaje
        )
        Spacer(modifier = Modifier.height(10.dp))

        MenuKartaTlacitko(
            nadpis = "Trénink proti botům (Offline)",
            popis = "Okamžitá hra Mariáše, Prší nebo Sedmy s hospodskými hláškami i bez internetu.",
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
                    popis = "Česká pravidla her a obnova tereziánů.",
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
            fontSize = 12.sp
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(typ.kratkyPopis, color = Color(0xFFE0E0E0), fontSize = 12.sp)
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
                                onSpustitOffline(KonfiguraceOfflineHry(typHry = typ, pocetHracu = pocet))
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
    onZpet: () -> Unit,
    onVstoupitDoStolu: (OnlineStul) -> Unit
) {
    val scope = rememberCoroutineScope()
    var stoly by remember { mutableStateOf<List<OnlineStul>>(emptyList()) }
    var nacitaSe by remember { mutableStateOf(true) }
    var zpravaChyby by remember { mutableStateOf("") }

    var vybranaHraZalozeni by remember { mutableStateOf(TypHry.MARIAS) }
    var vybranyPocetZalozeni by remember { mutableStateOf(3) }
    var soukromyPin by remember { mutableStateOf("") }
    var zadavanyPinProVstup by remember { mutableStateOf("") }

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
                                pinKod = soukromyPin
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
                    Text("Otevřít stůl", color = Color(0xFF04210E), fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
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
                            Text(
                                text = (if (stul.jeSoukromy) "🔒 " else "") + stul.nazevStolu,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
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

@Composable
private fun CekarnaStoluObrazovka(
    pocatecniStul: OnlineStul,
    onHraOdstartovana: (OnlineStul) -> Unit,
    onZrusit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var stul by remember { mutableStateOf(pocatecniStul) }
    var nyniMs by remember { mutableStateOf(ziskejAktualniCasMs()) }
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
            if (cerstvy.hraci.size == cerstvy.maxHracu && cerstvy.hostDeviceId == mojeId) {
                val spusteny = OnlineManager.rozdejASpustStul(cerstvy, zacinajiciIndex = 0)
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
            Text("Opustit čekárnu", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TurnajovaSinObrazovka(
    onZpet: () -> Unit,
    onSpustitOfflinePohar: (TypHry) -> Unit,
    onVstoupitDoZapasuTurnaje: (OnlineStul) -> Unit
) {
    val scope = rememberCoroutineScope()
    val mojeId = remember { ziskejUnikatniIdZarizeni() }
    val ulozenyNick = remember { nactiUlozenouPrezdivku() }
    var turnaje by remember { mutableStateOf<List<OnlineTurnaj>>(emptyList()) }
    var hlaska by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            turnaje = OnlineManager.nactiTurnaje()
            delay(4000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ZahlaviPodobrazovky("Turnajová síň", onZpet)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD9082916))
                .border(1.dp, Color(0x88D4AF37), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "Rychlý pohár proti botům (série 3 rozdání)",
                color = Color(0xFFFFD54F),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (typ in TypHry.entries) {
                    Button(
                        onClick = {
                            jemnaHaptika()
                            onSpustitOfflinePohar(typ)
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
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD9082916))
                .border(1.2.dp, Color(0xFFD4AF37), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text("Založit živý online turnaj", color = Color(0xFFFFD54F), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Prší a Sedma běží jako vyřazovací pavouk pro 4 hráče (Semifinále → Finále), Mariáš jako turnajový stůl na 3 rozdání.",
                color = Color(0xFFB9F6CA),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (typ in TypHry.entries) {
                    Button(
                        onClick = {
                            jemnaHaptika()
                            if (ulozenyNick.isEmpty()) {
                                hlaska = "Pro online turnaj si nejprve nastav přezdívku na hlavní obrazovce."
                                return@Button
                            }
                            if (nactiLokalniTereziany() < 20) {
                                hlaska = "Pro online turnaj potřebuješ alespoň 20 tereziánů — vyzvedni si na hlavní obrazovce Císařskou podporu."
                                return@Button
                            }
                            scope.launch {
                                val novy = OnlineManager.vytvorTurnaj("Online Pohár: ${typ.nazev}", typ)
                                if (novy != null) turnaje = OnlineManager.nactiTurnaje()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text(
                            text = "+ " + (if (typ == TypHry.MARIAS) "Mariáš" else typ.nazev),
                            color = Color(0xFF072312),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (hlaska.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(hlaska, color = Color(0xFFFF8A80), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Probíhající a otevřené online turnaje:", color = Color(0xFFFFE082), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (turnaje.isEmpty()) {
            Text("Momentálně neběží žádný online turnaj.", color = Color(0xFFB9F6CA), fontSize = 13.sp)
        } else {
            for (t in turnaje) {
                val jsemPrihlasen = t.ucastnici.any { it.deviceId == mojeId }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xD9082916))
                        .border(1.dp, Color(0x66D4AF37), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "${t.nazev} (${t.ucastnici.size}/${t.kapacita})",
                        color = Color(0xFFFFD54F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Účastníci: " + t.ucastnici.joinToString(", ") { it.prezdivka },
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    if (t.celkovyVitez.isNotEmpty()) {
                        Text(
                            text = "🏆 Vítěz turnaje: ${t.celkovyVitez}",
                            color = Color(0xFF69F0AE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (t.zapasy.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        for (z in t.zapasy) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${z.nazevKola}: ${z.hrac1Jmeno} vs. ${z.hrac2Jmeno}" +
                                        if (z.vitezJmeno.isNotEmpty()) " → ${z.vitezJmeno}" else "",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                val mujZapas = (z.hrac1Id == mojeId || z.hrac2Id == mojeId) &&
                                    z.vitezId.isEmpty() && z.roomId.isNotEmpty()
                                if (mujZapas) {
                                    Button(
                                        onClick = {
                                            jemnaHaptika()
                                            scope.launch {
                                                val stul = OnlineManager.nactiKonkretniStul(z.roomId)
                                                if (stul != null) onVstoupitDoZapasuTurnaje(stul)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                    ) {
                                        Text("Hrát zápas", color = Color(0xFF04210E), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    } else if (t.typHry == TypHry.MARIAS && t.mariasRoomId.isNotEmpty() && jsemPrihlasen && t.stav == "PROBIHA") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                jemnaHaptika()
                                scope.launch {
                                    val stul = OnlineManager.nactiKonkretniStul(t.mariasRoomId)
                                    if (stul != null) onVstoupitDoZapasuTurnaje(stul)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                        ) {
                            Text("Vstoupit ke stolu Mariáše", color = Color(0xFF04210E), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (t.stav == "REGISTRACE" && !jsemPrihlasen && ulozenyNick.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                jemnaHaptika()
                                scope.launch {
                                    OnlineManager.prihlasDoTurnaje(t.turnajId)
                                    turnaje = OnlineManager.nactiTurnaje()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text("Přihlásit se do turnaje", color = Color(0xFF072312), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZebricekObrazovka(onZpet: () -> Unit) {
    var polozky by remember { mutableStateOf<List<ZaznamZebricku>>(emptyList()) }
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
            nadpis = "Měna tereziány a obnova účtu přes PIN",
            text = "Každý nový hráč dostává do začátku měšec s 1000 tereziány. Jednou denně si na hlavní obrazovce můžeš vyzvednout denní odměnu +100 tereziánů. Klesne-li tvůj měšec pod 100 tereziánů, tlačítko Císařská podpora ti ho zdarma dorovná na 200 tereziánů, případně si můžeš kdykoliv přivydělat v tréninku s boty. Díky 4místnému PINu si celý svůj účet i měšec obnovíš po přeinstalaci nebo v novém telefonu."
        )
        BlokPravidel(
            nadpis = "Hospodské hlášky a zvuky stolu",
            text = "Během partie doprovázejí hru české hospodské hlášky s odlišenými hlasy spoluhráčů a zvukem karet na plátně. Ozvučení i hlasové komentáře můžeš kdykoliv jedním klepnutím zapnout nebo ztišit tlačítkem reproduktoru vpravo nahoře na hlavní obrazovce i přímo u karetního stolu."
        )
        BlokPravidel(
            nadpis = "Volený Mariáš — kompletní česká pravidla",
            text = "Forhont volí trumf ze 7 karet (nebo naslepo z talonu) a po dobrání odkládá 2 karty mimo es a desítek. Kromě Hry, Betlu a Durchu lze hlásit Stovku (100), Sedmu i Dvě sedmy (9. štych vedlejší sedmičkou a 10. štych trumfovou sedmičkou) — včetně falešné sedmy jako blafu. Nehlášená trumfová sedma v 10. štychu platí jako tichá sedma (při přebití v posledním štychu jako zabitá tichá sedma) a uhraná stovka z obyčejné hry jako tichá stovka. Červené trumfy platí dvojnásobně, hlášky 20/40 platí straně s alespoň jedním štychem a flekovat lze v celé řadě Flek (2×), Re (4×), Tutti (8×), Boty (16×) až Kalhoty (32×) zvlášť na hru i na sedmu."
        )
        BlokPravidel(
            nadpis = "Prší (2 až 4 hráči)",
            text = "Přikládá se stejná barva nebo hodnota. Sedmička nutí dalšího líznout 2 karty (lze přebíjet další sedmičkou na 4, 6 či 8 karet), eso zastavuje na 1 kolo (lze přebít esem) a svršek mění barvu."
        )
        BlokPravidel(
            nadpis = "Sedma (1 na 1 i čtyřhra 2 na 2 do kříže)",
            text = "Štych bere karta stejné hodnoty jako první vynesená nebo libovolná sedmička. Počítají se desítky, esa a poslední štych po 10 bodech. Čtveřice stejných karet ve štychu znamená spálenou a okamžitou výhru."
        )
        BlokPravidel(
            nadpis = "Živé online stoly bez botů",
            text = "Karty při každém rozdání míchá server a každý hráč může mít otevřený nejvýše jeden vlastní stůl. Zelená ikona u přezdívky hlídá živé spojení — při výpadku delším než 60 sekund se stůl čistě uzavře bez zásahu botů a přítomným hráčům se žádné tereziány neodečtou."
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
        Text(text, color = Color(0xFFE0E0E0), fontSize = 12.sp)
    }
}
