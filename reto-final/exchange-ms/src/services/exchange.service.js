import * as orchestrator from '../saga/orchestrator.js';
import exchangeRepository, { ExchangeNotFoundError } from '../repositories/exchange.repository.js';

const exchangeService = {
  async create(data) {
    return orchestrator.createExchange(data);
  },

  async offerSticker(exchangeId, stickerId, collectorRole) {
    return orchestrator.offerSticker(exchangeId, stickerId, collectorRole);
  },

  async execute(exchangeId, chaosMode = false) {
    return orchestrator.executeExchange(exchangeId, chaosMode);
  },

  async getById(exchangeId) {
    const exchange = await exchangeRepository.findById(exchangeId);
    if (!exchange) throw new ExchangeNotFoundError(exchangeId);
    return exchange;
  },
};

export default exchangeService;
