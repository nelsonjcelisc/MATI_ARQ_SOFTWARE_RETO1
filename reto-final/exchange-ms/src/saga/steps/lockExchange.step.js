import exchangeRepository from '../../repositories/exchange.repository.js';

export async function execute(exchange) {
  return exchangeRepository.updateStatus(exchange.id, 'LOCKED');
}
