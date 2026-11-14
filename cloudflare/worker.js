/**
 * Cloudflare Worker pro Ceske karty (cz.mares.ceskekarty)
 * Bezpecne michani 32 mariasovych karet mimo webhosting uzivatele,
 * presny serverovy cas pro planovane turnaje a kontrola 60s neaktivity.
 * Nasazeni zdarma na https://workers.cloudflare.com (promenna prostredi FIREBASE_DB_URL).
 */

const SUITS = ["CERVENE", "KULE", "ZALUDY", "ZELENE"];
const RANKS = ["R7", "R8", "R9", "R10", "SPODEK", "SVRSEK", "KRAL", "ESO"];
const DEFAULT_FB_URL = "https://mares-games-default-rtdb.europe-west1.firebasedatabase.app";

function createSecureShuffledDeck() {
  const deck = [];
  for (const s of SUITS) {
    for (const r of RANKS) {
      deck.push(`${s}_${r}`);
    }
  }
  const rnd = new Uint32Array(deck.length);
  crypto.getRandomValues(rnd);
  for (let i = deck.length - 1; i > 0; i--) {
    const j = rnd[i] % (i + 1);
    const tmp = deck[i];
    deck[i] = deck[j];
    deck[j] = tmp;
  }
  return deck;
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type"
  };
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const fbBase = (env.FIREBASE_DB_URL || DEFAULT_FB_URL).replace(/\/$/, "");
    const url = new URL(request.url);
    const nowMs = Date.now();

    try {
      if (url.pathname === "/time") {
        return Response.json({ ok: true, nowMs }, { headers: corsHeaders() });
      }

      if (url.pathname === "/deal" && request.method === "POST") {
        const body = await request.json();
        const roomId = String(body.roomId || "").replace(/[^a-zA-Z0-9_-]/g, "");
        const gameType = String(body.gameType || "PRSI");
        const playerIds = Array.isArray(body.playerIds) ? body.playerIds : [];
        if (!roomId || playerIds.length < 2) {
          return Response.json({ ok: false, error: "Chybi roomId nebo hraci" }, { status: 400, headers: corsHeaders() });
        }

        const deck = createSecureShuffledDeck();
        const hands = {};
        let cursor = 0;

        if (gameType === "MARIAS" && playerIds.length === 3) {
          hands[playerIds[0]] = deck.slice(0, 7).join(",");
          hands[playerIds[1]] = deck.slice(7, 12).join(",");
          hands[playerIds[2]] = deck.slice(12, 17).join(",");
          cursor = 17;
        } else {
          for (const pid of playerIds) {
            hands[pid] = deck.slice(cursor, cursor + 4).join(",");
            cursor += 4;
          }
        }

        let topCard = "";
        if (gameType === "PRSI") {
          topCard = deck[cursor];
          cursor += 1;
        }
        const remainingStock = deck.slice(cursor).join(",");

        await fetch(`${fbBase}/card_rooms/${roomId}/deal.json`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            hands,
            topCard,
            stock: remainingStock,
            dealtAt: nowMs
          })
        });

        return Response.json({ ok: true, topCard, dealtCount: cursor, nowMs }, { headers: corsHeaders() });
      }

      if (url.pathname === "/check-timeout" && request.method === "POST") {
        const body = await request.json();
        const roomId = String(body.roomId || "").replace(/[^a-zA-Z0-9_-]/g, "");
        if (!roomId) {
          return Response.json({ ok: false, error: "Chybi roomId" }, { status: 400, headers: corsHeaders() });
        }

        const roomRes = await fetch(`${fbBase}/card_rooms/${roomId}.json`);
        const room = await roomRes.json();
        const metaStr = room && typeof room.meta === "string" ? room.meta : "";
        const statusVal = room ? String(room.statusOverride || room.status || "") : "";
        const isPlaying =
          statusVal === "PLAYING" ||
          statusVal === "HRAJE_SE" ||
          (!statusVal && metaStr.includes("stav=HRAJE_SE"));

        if (!room || !isPlaying) {
          return Response.json({ ok: true, terminated: false, nowMs }, { headers: corsHeaders() });
        }

        const heartbeats = room.heartbeats || {};
        let timedOutPlayer = "";
        for (const [pid, ts] of Object.entries(heartbeats)) {
          if (String(pid).startsWith("bot_")) continue;
          if (nowMs - Number(ts) > 60000) {
            timedOutPlayer = pid;
            break;
          }
        }

        if (timedOutPlayer) {
          await fetch(`${fbBase}/card_rooms/${roomId}.json`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              status: "ABORTED_TIMEOUT",
              statusOverride: "ABORTED_TIMEOUT",
              abortedBy: timedOutPlayer,
              finishedAt: nowMs
            })
          });
          return Response.json({ ok: true, terminated: true, abortedBy: timedOutPlayer, nowMs }, { headers: corsHeaders() });
        }

        return Response.json({ ok: true, terminated: false, nowMs }, { headers: corsHeaders() });
      }

      return Response.json({ ok: true, service: "CeskeKarty Cloudflare Worker", nowMs }, { headers: corsHeaders() });
    } catch (err) {
      return Response.json({ ok: false, error: String(err) }, { status: 500, headers: corsHeaders() });
    }
  }
};
