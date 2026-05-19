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

  // Reverts a completed transfer: EXCHANGED → RESERVED with original owner.
  // Called by exchange-ms compensation when Promise.all partially fails.
  async revertTransferOwnership(req, res, next) {
    try {
      const { id } = req.params;
      const { newOwnerId } = req.body;
      const sticker = await stickerService.revertTransfer(id, newOwnerId);
      res.status(200).json({ success: true, data: sticker });
    } catch (err) { next(err); }
  },

  // Chaos endpoint — always returns 500. Used by Artillery stress tests to
  // guarantee transfer B fails so compensation triggers every run.
  failTransferOwnership(req, res) {
    res.status(500).json({
      success: false,
      message: 'Chaos: simulated transfer failure',
    });
  },
};
export default stickerController;