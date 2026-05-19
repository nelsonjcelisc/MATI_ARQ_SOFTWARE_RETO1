import exchangeRepository from '../../repositories/exchange.repository.js';
import { publishAlbumRecalculate } from '../../messaging/publisher.js';

export async function execute(exchange) {
  await exchangeRepository.updateStatus(exchange.id, 'COMPLETED');

  try {
    await publishAlbumRecalculate(exchange.id, [
      { collectorId: exchange.hostCollectorId,  collectionId: exchange.hostStickerId },
      { collectorId: exchange.guestCollectorId, collectionId: exchange.guestStickerId },
    ]);
  } catch (publishErr) {
    console.error('⚠ Failed to publish album.recalculate — exchange still COMPLETED:', publishErr.message);
  }
}
