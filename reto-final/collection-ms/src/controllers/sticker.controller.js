// src/controllers/sticker.controller.js
import stickerService from '../services/sticker.service.js';

const stickerController = {
  async getById(req, res, next) {
    try {
      const sticker = await stickerService.getById(req.params.id);
      res.status(200).json({ success: true, data: sticker });
    } catch (err) { next(err); }
  },
  async create(req, res, next) {
    try {
      const sticker = await stickerService.create(req.body);
      res.status(201).json({ success: true, data: sticker });
    } catch (err) { next(err); }
  },
  async reserve(req, res, next) {
    try {
      const { id } = req.params;
      const { exchangeId } = req.body;
      const sticker = await stickerService.reserve(id, exchangeId);
      res.status(200).json({ success: true, data: sticker });
    } catch (err) { next(err); }
  },
  async release(req, res, next) {
    try {
      const sticker = await stickerService.release(req.params.id);
      res.status(200).json({ success: true, data: sticker });
    } catch (err) { next(err); }
  },
  async transferOwnership(req, res, next) {
    try {
      const { id } = req.params;
      const { newOwnerId } = req.body;
      const sticker = await stickerService.transferOwnership(id, newOwnerId);
      res.status(200).json({ success: true, data: sticker });
    } catch (err) { next(err); }
  },
};
export default stickerController;