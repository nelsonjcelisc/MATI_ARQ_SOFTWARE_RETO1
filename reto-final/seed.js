// seed.js — populates collection-ms and inventory-ms with test data for stress testing
// Outputs: seed-output.json + seed-output.csv (for Artillery payload)
//
// Usage:  node seed.js [PAIRS=50]
// Prereq: inventory-ms (:3003) and collection-ms (:3002) must be running

import { randomUUID }    from 'crypto';
import { writeFileSync } from 'fs';

const COLLECTION_MS = process.env.COLLECTION_MS_URL ?? 'http://localhost:3002';
const INVENTORY_MS  = process.env.INVENTORY_MS_URL  ?? 'http://localhost:3003';
const PAIRS_COUNT   = parseInt(process.argv[2] ?? '50', 10);
const TOTAL_SLOTS   = 200;

// One shared collection for all stickers so album recalculation is realistic
const COLLECTION_ID = randomUUID();

async function post(url, body) {
  const res = await fetch(url, {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify(body),
  });
  const json = await res.json();
  if (!json.success) {
    throw new Error(`POST ${url} failed [${res.status}]: ${JSON.stringify(json)}`);
  }
  return json.data;
}

async function seedPair(index) {
  const hostCollectorId  = randomUUID();
  const guestCollectorId = randomUUID();

  // 1. Create stickers in collection-ms
  const [stickerA, stickerB] = await Promise.all([
    post(`${COLLECTION_MS}/api/v1/stickers`, {
      name:         `Sticker-A-${index}`,
      number:       index * 2 + 1,
      collectionId: COLLECTION_ID,
      ownerId:      hostCollectorId,
    }),
    post(`${COLLECTION_MS}/api/v1/stickers`, {
      name:         `Sticker-B-${index}`,
      number:       index * 2 + 2,
      collectionId: COLLECTION_ID,
      ownerId:      guestCollectorId,
    }),
  ]);

  // 2. Register both stickers in inventory-ms
  await Promise.all([
    post(`${INVENTORY_MS}/api/v1/inventory`, {
      stickerId: stickerA.id,
      ownerId:   hostCollectorId,
    }),
    post(`${INVENTORY_MS}/api/v1/inventory`, {
      stickerId: stickerB.id,
      ownerId:   guestCollectorId,
    }),
  ]);

  // 3. Create albums for each collector
  await Promise.all([
    post(`${COLLECTION_MS}/api/v1/albums`, {
      collectorId:  hostCollectorId,
      collectionId: COLLECTION_ID,
      totalSlots:   TOTAL_SLOTS,
    }),
    post(`${COLLECTION_MS}/api/v1/albums`, {
      collectorId:  guestCollectorId,
      collectionId: COLLECTION_ID,
      totalSlots:   TOTAL_SLOTS,
    }),
  ]);

  return {
    hostCollectorId,
    guestCollectorId,
    hostStickerId:  stickerA.id,
    guestStickerId: stickerB.id,
  };
}

async function main() {
  console.log(`\nSeeding ${PAIRS_COUNT} exchange pairs`);
  console.log(`  collection-ms → ${COLLECTION_MS}`);
  console.log(`  inventory-ms  → ${INVENTORY_MS}`);
  console.log(`  collectionId  → ${COLLECTION_ID}\n`);

  const pairs  = [];
  const failed = [];

  for (let i = 0; i < PAIRS_COUNT; i++) {
    process.stdout.write(`  [${String(i + 1).padStart(3)}/${PAIRS_COUNT}] seeding pair...`);
    try {
      const pair = await seedPair(i);
      pairs.push(pair);
      process.stdout.write(' ✓\n');
    } catch (err) {
      process.stdout.write(` ✗ ${err.message}\n`);
      failed.push(i);
    }
  }

  // ── JSON output (Artillery payload / general use) ─────────────────────────
  const jsonOutput = { collectionId: COLLECTION_ID, total: pairs.length, pairs };
  writeFileSync('./seed-output.json', JSON.stringify(jsonOutput, null, 2));

  // ── CSV output (Artillery payload with csvfile) ───────────────────────────
  const csvHeader = 'hostCollectorId,guestCollectorId,hostStickerId,guestStickerId';
  const csvRows   = pairs.map(p =>
    `${p.hostCollectorId},${p.guestCollectorId},${p.hostStickerId},${p.guestStickerId}`
  );
  writeFileSync('./seed-output.csv', [csvHeader, ...csvRows].join('\n'));

  console.log(`\n──────────────────────────────────────────`);
  console.log(`  Seeded:  ${pairs.length} pairs`);
  if (failed.length) console.log(`  Failed:  ${failed.length} pairs (indices: ${failed.join(', ')})`);
  console.log(`  Output:  seed-output.json`);
  console.log(`  Output:  seed-output.csv`);
  console.log(`──────────────────────────────────────────\n`);
}

main().catch(err => {
  console.error('\n✗ Seed script crashed:', err.message);
  process.exit(1);
});
