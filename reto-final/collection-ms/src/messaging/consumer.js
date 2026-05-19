// src/messaging/consumer.js
import { getChannel } from '../config/rabbitmq.config.js';
import * as albumRepository   from '../modules/album/album.repository.js';
import * as stickerRepository from '../modules/stickers/sticker.repository.js';

// Handles both RESERVED (release) and EXCHANGED (revert then release) sticker states.
async function releaseOrRevert(stickerId, originalOwnerId) {
  try {
    await stickerRepository.release(stickerId);
  } catch {
    await stickerRepository.revertTransfer(stickerId, originalOwnerId);
    await stickerRepository.release(stickerId);
  }
}

export async function startConsumers() {
  const channel = getChannel();
  channel.prefetch(1);

  // ── album.recalculate ────────────────────────────────────────────────────
  const aq = await channel.assertQueue('collection.album-recalculate', { durable: true });
  await channel.bindQueue(aq.queue, 'album.recalculate', '');
  channel.consume(aq.queue, async (msg) => {
    if (!msg) return;
    try {
      const { exchangeId, collectorIds } = JSON.parse(msg.content.toString());
      console.log(`Processing album recalculate for exchange ${exchangeId}`);
      await Promise.all(
        collectorIds.map(({ collectorId, collectionId }) =>
          albumRepository.recalculateProgress(collectorId, collectionId)
        )
      );
      channel.ack(msg);
      console.log(`✓ Album progress recalculated for exchange ${exchangeId}`);
    } catch (err) {
      console.error('✗ Album recalculate failed:', err.message);
      channel.nack(msg, false, false);
    }
  });
  console.log('✓ Album recalculate consumer started');

  // ── saga.compensation.dead-letter ────────────────────────────────────────
  // Last-resort: releases stickers that the orchestrator's releaseStickers step could not free.
  // Stickers may be RESERVED or EXCHANGED depending on how far the SAGA got before failing.
  const dlq = await channel.assertQueue('collection.saga-dead-letter', { durable: true });
  await channel.bindQueue(dlq.queue, 'saga.compensation.dead-letter', '');
  channel.consume(dlq.queue, async (msg) => {
    if (!msg) return;
    try {
      const {
        exchangeId,
        hostStickerId,  hostCollectorId,
        guestStickerId, guestCollectorId,
      } = JSON.parse(msg.content.toString());
      console.log(`Dead letter: releasing stickers for exchange ${exchangeId}`);

      const stickers = [
        { id: hostStickerId,  originalOwnerId: hostCollectorId },
        { id: guestStickerId, originalOwnerId: guestCollectorId },
      ].filter(s => s.id);

      await Promise.all(stickers.map(({ id, originalOwnerId }) =>
        releaseOrRevert(id, originalOwnerId)
      ));

      channel.ack(msg);
      console.log(`✓ Dead letter: stickers released for exchange ${exchangeId}`);
    } catch (err) {
      console.error('✗ Dead letter release failed:', err.message);
      channel.nack(msg, false, false);
    }
  });
  console.log('✓ Dead letter compensation consumer started');
}