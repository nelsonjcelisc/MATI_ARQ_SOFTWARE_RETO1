import { getChannel } from '../config/rabbitmq.config.js';

export async function publishAlbumRecalculate(exchangeId, collectorIds) {
  const channel = getChannel();
  const payload = JSON.stringify({ exchangeId, collectorIds, timestamp: new Date() });
  channel.publish('album.recalculate', '', Buffer.from(payload), {
    persistent:  true,
    contentType: 'application/json',
  });
}

export async function publishDeadLetter(exchange, error) {
  const channel = getChannel();
  const payload = JSON.stringify({
    exchangeId:       exchange.id,
    hostStickerId:    exchange.hostStickerId,
    guestStickerId:   exchange.guestStickerId,
    hostCollectorId:  exchange.hostCollectorId,
    guestCollectorId: exchange.guestCollectorId,
    sagaLog:          exchange.sagaLog,
    error:            error.message,
    timestamp:        new Date(),
  });
  channel.publish('saga.compensation.dead-letter', '', Buffer.from(payload), {
    persistent:  true,
    contentType: 'application/json',
  });
}
