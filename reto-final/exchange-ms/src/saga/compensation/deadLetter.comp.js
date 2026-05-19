import { publishDeadLetter } from '../../messaging/publisher.js';

export async function execute(exchange, error) {
  await publishDeadLetter(exchange, error);
  console.error(`✗ Exchange ${exchange.id} sent to dead letter queue:`, error.message);
}
