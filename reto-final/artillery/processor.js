"use strict";

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const DUPLICATE_RATE = parseInt(process.env.DUPLICATE_RATE || "30", 10);
const LOG_FILE = path.resolve(__dirname, "run-log.jsonl");

const usedKeys = [];
const keyPayloads = {};
const counters = { total: 0, new: 0, duplicate: 0 };

// Limpiar el log viejo al iniciar
fs.writeFileSync(LOG_FILE, "", "utf-8");

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomChoice(arr) {
  return arr[randomInt(0, arr.length - 1)];
}

function generateStochasticDelay(userContext, events, done) {
  const lambda = 1.5;
  const raw = -Math.log(Math.random()) / lambda;
  const delay = Math.min(Math.max(raw, 0.1), 2.5);
  userContext.vars.stochasticDelay = parseFloat(delay.toFixed(2));
  return done();
}

function prepareRequest(userContext, events, done) {
  const isDuplicate = usedKeys.length > 0 && Math.random() * 100 < DUPLICATE_RATE;

  let idempotenciaKey;
  let payload;

  if (isDuplicate) {
    idempotenciaKey = randomChoice(usedKeys);
    payload = keyPayloads[idempotenciaKey];
  } else {
    // Para que coincida con el formato del manual, usamos un prefijo tipo LAMINA-
    idempotenciaKey = `LAMINA-${crypto.randomUUID().substring(0, 8).toUpperCase()}`;

    payload = {
      idUsuario: randomInt(100, 999),
      idLamina: idempotenciaKey
    };

    usedKeys.push(idempotenciaKey);
    keyPayloads[idempotenciaKey] = payload;
  }

  userContext.vars.idempotenciaKey = idempotenciaKey;
  userContext.vars.idUsuario = payload.idUsuario;
  userContext.vars.idLamina = payload.idLamina;

  userContext.vars._isDuplicate = isDuplicate;
  userContext.vars._requestTimestamp = Date.now();

  counters.total++;
  if (isDuplicate) {
    counters.duplicate++;
  } else {
    counters.new++;
  }

  return done();
}

function logResponse(req, res, userContext, events, done) {
  const entry = {
    timestamp: userContext.vars._requestTimestamp,
    idempotenciaKey: userContext.vars.idempotenciaKey,
    isDuplicate: userContext.vars._isDuplicate,
    statusCode: res.statusCode,
    idUsuario: userContext.vars.idUsuario
  };

  fs.appendFile(LOG_FILE, JSON.stringify(entry) + "\n", "utf-8", (err) => {
    if (err) console.error("[processor] Error escribiendo log:", err.message);
  });

  return done();
}

process.on("exit", () => {
  console.log("\n╔══════════════════════════════════════════════════╗");
  console.log("║           RESUMEN PLANIFICADO DE TRÁFICO         ║");
  console.log("╠══════════════════════════════════════════════════╣");
  console.log(`║  Total peticiones:      ${String(counters.total).padStart(6)}                  ║`);
  console.log(`║  Nuevas (key única):    ${String(counters.new).padStart(6)}                  ║`);
  console.log(`║  Duplicadas (reenvío):  ${String(counters.duplicate).padStart(6)}                  ║`);
  console.log(`║  Tasa duplicación real: ${String(
    counters.total > 0 ? ((counters.duplicate / counters.total) * 100).toFixed(1) : "0.0"
  ).padStart(5)}%               ║`);
  console.log("╚══════════════════════════════════════════════════╝\n");
});

module.exports = {
  generateStochasticDelay,
  prepareRequest,
  logResponse
};