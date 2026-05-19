"use strict";

const fs   = require("fs");
const path = require("path");
const { randomUUID } = require("crypto");

const LOG_FILE = path.resolve(__dirname, "saga-run-log.jsonl");

// These are reached from inside the Artillery Docker container.
const COLLECTION_MS = "http://host.docker.internal:3002";
const INVENTORY_MS  = "http://host.docker.internal:3003";

// Use a stable collectionId so stickers share a logical collection.
// Any valid UUID is fine — collection-ms has no FK constraint on this field.
const COLLECTION_ID = "00000000-0000-4000-8000-000000000001";

const counters = {
  happyOk:         0,
  happyFailed:     0,
  chaosOk:         0,   // got expected 500 → compensation triggered
  chaosUnexpected: 0,   // got something other than 500
};

fs.writeFileSync(LOG_FILE, "", "utf-8");

async function postJson(url, body) {
  const res  = await fetch(url, {
    method:  "POST",
    headers: { "Content-Type": "application/json" },
    body:    JSON.stringify(body),
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`POST ${url} → ${res.status}: ${JSON.stringify(json)}`);
  return json;
}

// Called before each VU's scenario. Creates a fresh, unique sticker pair so
// concurrent VUs never compete for the same sticker reservation.
function setupPair(userContext, events, done) {
  const hostCollectorId  = randomUUID();
  const guestCollectorId = randomUUID();

  Promise.all([
    postJson(`${COLLECTION_MS}/api/v1/stickers`, {
      name:         `H-${hostCollectorId.slice(0, 8)}`,
      number:       Math.floor(Math.random() * 9_000_000) + 1_000_000,
      collectionId: COLLECTION_ID,
      ownerId:      hostCollectorId,
    }),
    postJson(`${COLLECTION_MS}/api/v1/stickers`, {
      name:         `G-${guestCollectorId.slice(0, 8)}`,
      number:       Math.floor(Math.random() * 9_000_000) + 1_000_000,
      collectionId: COLLECTION_ID,
      ownerId:      guestCollectorId,
    }),
  ])
    .then(([resA, resB]) => {
      const hostStickerId  = resA.data.id;
      const guestStickerId = resB.data.id;

      return Promise.all([
        postJson(`${INVENTORY_MS}/api/v1/inventory`, {
          stickerId: hostStickerId,
          ownerId:   hostCollectorId,
        }),
        postJson(`${INVENTORY_MS}/api/v1/inventory`, {
          stickerId: guestStickerId,
          ownerId:   guestCollectorId,
        }),
      ]).then(() => {
        userContext.vars.hostCollectorId  = hostCollectorId;
        userContext.vars.guestCollectorId = guestCollectorId;
        userContext.vars.hostStickerId    = hostStickerId;
        userContext.vars.guestStickerId   = guestStickerId;
        return done();
      });
    })
    .catch((err) => done(err));
}

function logSagaResult(req, res, userContext, events, done) {
  const ok = res.statusCode === 200;
  if (ok) counters.happyOk++; else counters.happyFailed++;

  fs.appendFile(
    LOG_FILE,
    JSON.stringify({
      ts:         Date.now(),
      scenario:   "happy",
      exchangeId: userContext.vars.exchangeId,
      status:     res.statusCode,
      ok,
    }) + "\n",
    "utf-8",
    () => {}
  );
  return done();
}

function logChaosResult(req, res, userContext, events, done) {
  const compensated = res.statusCode === 500;
  if (compensated) counters.chaosOk++; else counters.chaosUnexpected++;

  fs.appendFile(
    LOG_FILE,
    JSON.stringify({
      ts:          Date.now(),
      scenario:    "chaos",
      exchangeId:  userContext.vars.exchangeId,
      status:      res.statusCode,
      compensated,
    }) + "\n",
    "utf-8",
    () => {}
  );
  return done();
}

process.on("exit", () => {
  const total = Object.values(counters).reduce((a, b) => a + b, 0);
  const line  = (label, val) =>
    `║  ${label.padEnd(32)}${String(val).padStart(5)}                 ║`;

  console.log("\n╔══════════════════════════════════════════════════╗");
  console.log("║          RESUMEN DE EJECUCION SAGA               ║");
  console.log("╠══════════════════════════════════════════════════╣");
  console.log(line("Total SAGAs ejecutadas:", total));
  console.log(line("Happy path completados:", counters.happyOk));
  console.log(line("Happy path fallidos*:", counters.happyFailed));
  console.log(line("Chaos compensados (500):", counters.chaosOk));
  console.log(line("Chaos inesperado (no 500):", counters.chaosUnexpected));
  console.log("╠══════════════════════════════════════════════════╣");
  console.log("║  * error de servicio o compensacion fallida      ║");
  console.log("║  Ver metricas Prometheus en :3001/metrics        ║");
  console.log("╚══════════════════════════════════════════════════╝\n");
});

module.exports = { setupPair, logSagaResult, logChaosResult };
