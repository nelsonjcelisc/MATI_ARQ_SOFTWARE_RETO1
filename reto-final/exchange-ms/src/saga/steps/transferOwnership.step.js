import * as collectionClient from '../../clients/collection.client.js';
import * as inventoryClient  from '../../clients/inventory.client.js';
import env                   from '../../config/env.config.js';

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function withRetry(fn, retries = env.TRANSFER_RETRY_ATTEMPTS) {
  try {
    return await fn();
  } catch (err) {
    if (retries <= 0) throw err;
    await sleep(env.TRANSFER_RETRY_DELAY_MS);
    return withRetry(fn, retries - 1);
  }
}

async function singleTransfer(stickerId, fromId, toId) {
  await collectionClient.transferOwnership(stickerId, toId);
  await inventoryClient.transferOwnership(stickerId, fromId, toId);
}

async function singleRevert(stickerId, fromId, toId) {
  await collectionClient.revertTransfer(stickerId, fromId);
  await inventoryClient.revertTransfer(stickerId, toId, fromId);
}

// chaosMode=true → transfer B calls /owner/fail on collection-ms (always 500).
// Transfer A runs normally and succeeds, so succeeded=[A] when B throws.
// The catch block then reverts A, giving us a deterministic compensation cycle.
export async function execute(exchange, chaosMode = false) {
  const { hostCollectorId, guestCollectorId, hostStickerId, guestStickerId } = exchange;

  const succeeded = [];

  const transferA = async () => {
    await withRetry(() => singleTransfer(hostStickerId, hostCollectorId, guestCollectorId));
    succeeded.push({ stickerId: hostStickerId, fromId: hostCollectorId, toId: guestCollectorId });
  };

  const transferB = async () => {
    if (chaosMode) {
      // Hits the dedicated /fail endpoint — always 500, no DB changes on collection-ms side.
      await collectionClient.failTransferOwnership(guestStickerId);
    } else {
      await withRetry(() => singleTransfer(guestStickerId, guestCollectorId, hostCollectorId));
    }
    succeeded.push({ stickerId: guestStickerId, fromId: guestCollectorId, toId: hostCollectorId });
  };

  try {
    await Promise.all([transferA(), transferB()]);
  } catch (err) {
    for (const t of succeeded) {
      await singleRevert(t.stickerId, t.fromId, t.toId);
    }
    err.failedStep = 'transferOwnership';
    throw err;
  }
}
