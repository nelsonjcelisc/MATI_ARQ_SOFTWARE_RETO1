"use strict";

const fs = require("fs");
const path = require("path");

const LOG_FILE = path.resolve(__dirname, "run-log.jsonl");

if (!fs.existsSync(LOG_FILE)) {
  console.error("No se encontró run-log.jsonl. Ejecuta primero: artillery run test.yml");
  process.exit(1);
}

const lines = fs
  .readFileSync(LOG_FILE, "utf-8")
  .split("\n")
  .filter((l) => l.trim().length > 0);

if (lines.length === 0) {
  console.error("El archivo run-log.jsonl está vacío.");
  process.exit(1);
}

const entries = lines.map((line, i) => {
  try { return JSON.parse(line); }
  catch (e) { console.warn(`Línea ${i + 1} inválida, ignorada.`); return null; }
}).filter(Boolean);

const stats = {
  total: entries.length,
  new: 0,
  duplicate: 0,
  statusCodes: {},
  duplicateResponses: {},
  newResponses: {},
  keyOccurrences: {}
};

for (const entry of entries) {
  const code = String(entry.statusCode);

  if (entry.isDuplicate) {
    stats.duplicate++;
    stats.duplicateResponses[code] = (stats.duplicateResponses[code] || 0) + 1;
  } else {
    stats.new++;
    stats.newResponses[code] = (stats.newResponses[code] || 0) + 1;
  }

  stats.statusCodes[code] = (stats.statusCodes[code] || 0) + 1;

  if (!stats.keyOccurrences[entry.idempotenciaKey]) {
    stats.keyOccurrences[entry.idempotenciaKey] = [];
  }
  stats.keyOccurrences[entry.idempotenciaKey].push({
    isDuplicate: entry.isDuplicate,
    statusCode: code,
    timestamp: entry.timestamp
  });
}

const keysUsedMultipleTimes = Object.entries(stats.keyOccurrences).filter(
  ([, uses]) => uses.length > 1
);

let expectedDetections = 0;
let actuallyDetected = 0;

for (const [, uses] of keysUsedMultipleTimes) {
  for (let i = 1; i < uses.length; i++) {
    expectedDetections++;
    const reuse = uses[i];
    // 200/201 en caso de cache exitoso, 409 = Conflicto controlado por filtro
    if (["200", "201", "409", "304"].includes(reuse.statusCode)) {
      actuallyDetected++;
    }
  }
}

const sep = "═".repeat(58);
const ln  = "─".repeat(58);

console.log(`\n╔${sep}╗`);
console.log(`║  RESUMEN DE TEST DE IDEMPOTENCIA — RETO FINAL            ║`);
console.log(`╠${sep}╣`);

console.log(`║  1. VOLUMEN DE TRÁFICO                                  ║`);
console.log(`║  ${ln}║`);
console.log(`║  Peticiones totales:               ${String(stats.total).padStart(8)}            ║`);
console.log(`║  Nuevas (key única):               ${String(stats.new).padStart(8)}            ║`);
console.log(`║  Duplicadas (reenvío intencional): ${String(stats.duplicate).padStart(7)}            ║`);
console.log(`║  Tasa de duplicación planificada:  ${
  stats.total > 0 ? String(((stats.duplicate / stats.total) * 100).toFixed(1) + "%").padStart(7) : "   N/A "
}            ║`);
console.log(`║  Keys únicas generadas:           ${String(Object.keys(stats.keyOccurrences).length).padStart(8)}            ║`);
console.log(`║  Keys reutilizadas (>1 uso):       ${String(keysUsedMultipleTimes.length).padStart(7)}            ║`);

console.log(`║${" ".repeat(58)}║`);
console.log(`║  2. CÓDIGOS HTTP GLOBALES                               ║`);
console.log(`║  ${ln}║`);
for (const [code, count] of Object.entries(stats.statusCodes).sort()) {
  console.log(`║  HTTP ${code}: ${String(count).padStart(8)}${" ".repeat(37)}║`);
}

console.log(`║${" ".repeat(58)}║`);
console.log(`║  3. RESPUESTAS — PETICIONES NUEVAS                      ║`);
console.log(`║  ${ln}║`);
for (const [code, count] of Object.entries(stats.newResponses).sort()) {
  console.log(`║  HTTP ${code}: ${String(count).padStart(8)}${" ".repeat(37)}║`);
}

console.log(`║${" ".repeat(58)}║`);
console.log(`║  4. RESPUESTAS — PETICIONES DUPLICADAS                  ║`);
console.log(`║  ${ln}║`);
if (Object.keys(stats.duplicateResponses).length === 0) {
  console.log(`║  (sin datos)${" ".repeat(45)}║`);
} else {
  for (const [code, count] of Object.entries(stats.duplicateResponses).sort()) {
    console.log(`║  HTTP ${code}: ${String(count).padStart(8)}${" ".repeat(37)}║`);
  }
}

console.log(`║${" ".repeat(58)}║`);
console.log(`║  5. EFECTIVIDAD DE IDEMPOTENCIA                         ║`);
console.log(`║  ${ln}║`);
console.log(`║  Reenvíos esperados como detectados: ${String(expectedDetections).padStart(6)}            ║`);
console.log(`║  Reenvíos efectivamente detectados:  ${String(actuallyDetected).padStart(6)}            ║`);
console.log(`║  Tasa de detección:                  ${
  expectedDetections > 0 ? String(((actuallyDetected / expectedDetections) * 100).toFixed(1) + "%").padStart(7) : "   N/A "
}            ║`);
╚${sep}╝`);

console.log("\nINTERPRETACIÓN:");
console.log(ln);

if (expectedDetections === 0) {
  console.log("→ No hubo reenvíos con keys duplicadas. Aumenta DUPLICATE_RATE en el entorno.");
} else if (actuallyDetected === expectedDetections) {
  console.log("→ Todas las peticiones duplicadas fueron interceptadas y detectadas (409 Conflict o Cache).");
  console.log("  El mecanismo de idempotencia cumple el ASR bajo estrés.");
} else if (actuallyDetected > 0) {
  const missed = expectedDetections - actuallyDetected;
  console.log(`→ ${missed} de ${expectedDetections} reenvíos se saltaron el filtro (Generaron falsos positivos en BD).`);
} else {
  console.log("→ Ningún reenvío fue detectado. Revisa si el filtro de Redis está encendido.");
}
console.log(`\nLog completo: ${LOG_FILE}\n`);