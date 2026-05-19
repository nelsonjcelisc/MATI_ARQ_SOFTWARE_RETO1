// 01-seed.js — Populates collection-ms and inventory-ms with test data.
// Outputs seed-output.json + seed-output.csv inside this directory.
//
// Usage:  node 01-seed.js <PAIRS>
// Prereq: collection-ms (:3002) and inventory-ms (:3003) must be running
// Example: node 01-seed.js 2000

import { randomUUID }    from 'crypto';
import { writeFileSync } from 'fs';
import path              from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const COLLECTION_MS = process.env.COLLECTION_MS_URL ?? 'http://localhost:3002';
const INVENTORY_MS  = process.env.INVENTORY_MS_URL  ?? 'http://localhost:3003';
const PAIRS_COUNT   = parseInt(process.argv[2] ?? '50', 10);
const TOTAL_SLOTS   = 200;

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

  const [stickerA, stickerB] = await Promise.all([
    post(`${COLLECTION_MS}/api/v1/stickers`, {
      name: `Sticker-A-${index}`, number: index * 2 + 1,
      collectionId: COLLECTION_ID, ownerId: hostCollectorId,
    }),
    post(`${COLLECTION_MS}/api/v1/stickers`, {
      name: `Sticker-B-${index}`, number: index * 2 + 2,
      collectionId: COLLECTION_ID, ownerId: guestCollectorId,
    }),
  ]);

  await Promise.all([
    post(`${INVENTORY_MS}/api/v1/inventory`, { stickerId: stickerA.id, ownerId: hostCollectorId }),
    post(`${INVENTORY_MS}/api/v1/inventory`, { stickerId: stickerB.id, ownerId: guestCollectorId }),
  ]);

  await Promise.all([
    post(`${COLLECTION_MS}/api/v1/albums`, { collectorId: hostCollectorId,  collectionId: COLLECTION_ID, totalSlots: TOTAL_SLOTS }),
    post(`${COLLECTION_MS}/api/v1/albums`, { collectorId: guestCollectorId, collectionId: COLLECTION_ID, totalSlots: TOTAL_SLOTS }),
  ]);

  return { hostCollectorId, guestCollectorId, hostStickerId: stickerA.id, guestStickerId: stickerB.id };
}

async function main() {
  console.log(`\nSeeding ${PAIRS_COUNT} exchange pairs`);
  console.log(`  collection-ms → ${COLLECTION_MS}`);
  console.log(`  inventory-ms  → ${INVENTORY_MS}`);
  console.log(`  collectionId  → ${COLLECTION_ID}\n`);

  const pairs = [], failed = [];

  for (let i = 0; i < PAIRS_COUNT; i++) {
    process.stdout.write(`  [${String(i + 1).padStart(4)}/${PAIRS_COUNT}] seeding pair...`);
    try {
      pairs.push(await seedPair(i));
      process.stdout.write(' ✓\n');
    } catch (err) {
      process.stdout.write(` ✗ ${err.message}\n`);
      failed.push(i);
    }
  }

  writeFileSync(
    path.join(__dirname, 'seed-output.json'),
    JSON.stringify({ collectionId: COLLECTION_ID, total: pairs.length, pairs }, null, 2)
  );

  const HAPPY_COUNT = 1000;
  const CHAOS_COUNT = 1000;
  const HEADER = 'hostCollectorId,guestCollectorId,hostStickerId,guestStickerId';

  const csvRows = pairs.map(p =>
    `${p.hostCollectorId},${p.guestCollectorId},${p.hostStickerId},${p.guestStickerId}`
  );

  writeFileSync(
    path.join(__dirname, 'seed-output.csv'),
    [HEADER, ...csvRows].join('\n') + '\n'
  );

  writeFileSync(
    path.join(__dirname, 'seed-happy.csv'),
    [HEADER, ...csvRows.slice(0, HAPPY_COUNT)].join('\n') + '\n'
  );

  writeFileSync(
    path.join(__dirname, 'seed-chaos.csv'),
    [HEADER, ...csvRows.slice(HAPPY_COUNT, HAPPY_COUNT + CHAOS_COUNT)].join('\n') + '\n'
  );

  console.log(`\n  Seeded : ${pairs.length} pairs`);
  if (failed.length) console.log(`  Failed : ${failed.length} (indices: ${failed.join(', ')})`);
  console.log(`  Output : seed-output.csv`);
  console.log(`  Output : seed-happy.csv  (rows 0–${HAPPY_COUNT - 1})`);
  console.log(`  Output : seed-chaos.csv  (rows ${HAPPY_COUNT}–${HAPPY_COUNT + CHAOS_COUNT - 1})\n`);
}

main().catch(err => { console.error('\n✗ Seed crashed:', err.message); process.exit(1); });
