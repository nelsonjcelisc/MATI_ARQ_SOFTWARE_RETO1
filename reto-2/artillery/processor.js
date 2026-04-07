"use strict";

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const DUPLICATE_RATE = parseInt(process.env.DUPLICATE_RATE || "30", 10);
const LOG_FILE = path.resolve(__dirname, "run-log.jsonl");


const usedKeys = [];

const keyPayloads = {};
const counters = { total: 0, new: 0, duplicate: 0 };

fs.writeFileSync(LOG_FILE, "", "utf-8");

const ESTADOS = ["PENDIENTE", "CONFIRMADA", "EN_PROCESO"];

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomChoice(arr) {
  return arr[randomInt(0, arr.length - 1)];
}

function uuid() {
  return crypto.randomUUID();
}

function generateIdFactura() {
  const year = new Date().getFullYear();
  const seq = String(randomInt(10000, 99999));
  return `FAC-${year}-${seq}`;
}

function generateFechaCompra() {
  const now = Date.now();
  const offset = randomInt(0, 48 * 60 * 60 * 1000);
  return new Date(now - offset).toISOString();
}


function generateStochasticDelay(userContext, events, done) {
  const lambda = 1.5;
  const raw = -Math.log(Math.random()) / lambda;
  const delay = Math.min(Math.max(raw, 0.1), 2.5);
  userContext.vars.stochasticDelay = parseFloat(delay.toFixed(2));
  return done();
}

function prepareRequest(userContext, events, done) {
  const isDuplicate =
    usedKeys.length > 0 && Math.random() * 100 < DUPLICATE_RATE;

  let idempotenciaKey;
  let payload;

  if (isDuplicate) {
    // Reutilizar key 
    idempotenciaKey = randomChoice(usedKeys);
    payload = keyPayloads[idempotenciaKey];
  } else {
    idempotenciaKey = uuid();

    payload = {
      idFactura: generateIdFactura(),
      idCliente: randomInt(1, 500),
      fechaCompra: generateFechaCompra(),
      estado: randomChoice(ESTADOS),
      total: parseFloat((randomInt(10000, 5000000) / 100).toFixed(2))
    };

    usedKeys.push(idempotenciaKey);
    keyPayloads[idempotenciaKey] = payload;
  }


  userContext.vars.idempotenciaKey = idempotenciaKey;
  userContext.vars.idFactura = payload.idFactura;
  userContext.vars.idCliente = payload.idCliente;
  userContext.vars.fechaCompra = payload.fechaCompra;
  userContext.vars.estado = payload.estado;
  userContext.vars.total = payload.total;

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
    idFactura: userContext.vars.idFactura,
    idCliente: userContext.vars.idCliente,
    total: userContext.vars.total
  };

  fs.appendFile(LOG_FILE, JSON.stringify(entry) + "\n", "utf-8", (err) => {
    if (err) console.error("[processor] Error escribiendo log:", err.message);
  });

  return done();
}

process.on("exit", () => {
  console.log("\n╔══════════════════════════════════════════════════╗");
  console.log("║   RESUMEN                                        ║");
  console.log("╠══════════════════════════════════════════════════╣");
  console.log(`║  Total peticiones:      ${String(counters.total).padStart(6)}                ║`);
  console.log(`║  Nuevas (key única):    ${String(counters.new).padStart(6)}                ║`);
  console.log(`║  Duplicadas (reenvío):  ${String(counters.duplicate).padStart(6)}                ║`);
  console.log(`║  Tasa duplicación real: ${String(
    counters.total > 0
      ? ((counters.duplicate / counters.total) * 100).toFixed(1)
      : "0.0"
  ).padStart(5)}%               ║`);
  console.log("╚══════════════════════════════════════════════════╝\n");
});

module.exports = {
  generateStochasticDelay,
  prepareRequest,
  logResponse
};
