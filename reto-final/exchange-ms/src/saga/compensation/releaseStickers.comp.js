import * as collectionClient from '../../clients/collection.client.js';

export async function execute(exchange) {
  const toRelease = [exchange.hostStickerId, exchange.guestStickerId].filter(Boolean);

  for (const stickerId of toRelease) {
    try {
      await collectionClient.release(stickerId);
    } catch (err) {
      console.error(`⚠ Failed to release sticker ${stickerId}:`, err.message);
      throw err;
    }
  }
}
