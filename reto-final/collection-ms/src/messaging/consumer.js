// src/messaging/consumer.js
import { getChannel } from '../config/rabbitmq.config.js';
import * as albumRepository from '../modules/album/album.repository.js';

export async function startConsumers() {
  const channel = getChannel();
  // Crea una queue durable y la vincula al exchange fanout
  const q = await channel.assertQueue('collection.album-recalculate', { durable: true });
  await channel.bindQueue(q.queue, 'album.recalculate', '');
  // prefetch(1) = procesa un mensaje a la vez por instancia
  // Previene que el consumer se sature si hay muchos mensajes en cola
  channel.prefetch(1);
  channel.consume(q.queue, async (msg) => {
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
      // nack sin requeue — el mensaje no vuelve a la queue
      // Si necesitas retry, configura un DLX a nivel de queue más adelante
      channel.nack(msg, false, false);
    }
  });
  console.log('✓ Album recalculate consumer started');
}