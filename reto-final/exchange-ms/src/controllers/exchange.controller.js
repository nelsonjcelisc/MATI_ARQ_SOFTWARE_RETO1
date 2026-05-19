import exchangeService from '../services/exchange.service.js';

const exchangeController = {
  async create(req, res, next) {
    try {
      const exchange = await exchangeService.create(req.body);
      res.status(201).json({ success: true, data: exchange });
    } catch (err) { next(err); }
  },

  async offer(req, res, next) {
    try {
      const { id } = req.params;
      const { stickerId, collectorRole } = req.body;
      const exchange = await exchangeService.offerSticker(id, stickerId, collectorRole);
      res.status(200).json({ success: true, data: exchange });
    } catch (err) { next(err); }
  },

  async execute(req, res, next) {
    try {
      const chaosMode = req.body?.chaosMode === true;
      const exchange  = await exchangeService.execute(req.params.id, chaosMode);
      res.status(200).json({ success: true, data: exchange });
    } catch (err) { next(err); }
  },

  async getById(req, res, next) {
    try {
      const exchange = await exchangeService.getById(req.params.id);
      res.status(200).json({ success: true, data: exchange });
    } catch (err) { next(err); }
  },
};

export default exchangeController;
