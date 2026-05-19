import * as collectionClient from '../../clients/collection.client.js';

export async function execute(stickerId, exchangeId) {
  try {
    await collectionClient.reserve(stickerId, exchangeId);
  } catch (err) {
    const e = new Error(`Failed to reserve sticker ${stickerId}: ${err.message}`);
    e.statusCode = err.response?.status || 500;
    e.failedStep = 'reserveSticker';
    throw e;
  }
}
