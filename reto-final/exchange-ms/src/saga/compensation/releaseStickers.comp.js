import * as collectionClient from '../../clients/collection.client.js';

// Stickers may be RESERVED (never transferred) or EXCHANGED (transfer completed before failure).
// Try release first; if it fails, revert ownership back to the original owner then release.
async function releaseOne(stickerId, originalOwnerId) {
  try {
    await collectionClient.release(stickerId);
  } catch {
    await collectionClient.revertTransfer(stickerId, originalOwnerId);
    await collectionClient.release(stickerId);
  }
}

export async function execute(exchange) {
  const stickers = [
    { id: exchange.hostStickerId,  originalOwnerId: exchange.hostCollectorId },
    { id: exchange.guestStickerId, originalOwnerId: exchange.guestCollectorId },
  ].filter(s => s.id);

  for (const { id, originalOwnerId } of stickers) {
    await releaseOne(id, originalOwnerId);
  }
}
