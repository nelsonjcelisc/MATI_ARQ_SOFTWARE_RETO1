// src/repositories/exchange.repository.js
import Exchange from '../models/exchange.model.js';

export class ExchangeNotFoundError extends Error {
  constructor(id) { super(`Exchange ${id} not found`); this.statusCode = 404; }
}

const exchangeRepository = {
  async create(data) {
    return Exchange.create(data);
  },
  async findById(id) {
    return Exchange.findByPk(id);
  },
  async updateStatus(id, status, extra = {}) {
    const exchange = await Exchange.findByPk(id);
    
    if (!exchange) throw new ExchangeNotFoundError(id);
    
    return exchange.update({ status, ...extra });
  },
  async appendLog(exchange, step, status, error = null) {
    const entry = { step, status, timestamp: new Date().toISOString() };
    if (error) entry.error = error.message;
    return exchange.update({ sagaLog: [...exchange.sagaLog, entry] });
  },
};

export default exchangeRepository;