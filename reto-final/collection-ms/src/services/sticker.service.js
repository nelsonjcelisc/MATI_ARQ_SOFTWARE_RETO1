// src/services/sticker.service.js
import * as stickerRepo from '../modules/stickers/sticker.repository.js';

const stickerService = {
  async getById(id) {
    const sticker = await stickerRepo.findById(id);
    if (!sticker) throw new stickerRepo.StickerNotFoundError(id);
    return sticker;
  },
  async create(data) {
    return stickerRepo.create(data);
  },
  async reserve(stickerId, exchangeId) {
    return stickerRepo.reserve(stickerId, exchangeId);
  },
  async release(stickerId) {
    return stickerRepo.release(stickerId);
  },
  async transferOwnership(stickerId, newOwnerId) {
    return stickerRepo.transferOwnership(stickerId, newOwnerId);
  },
  async revertTransfer(stickerId, originalOwnerId) {
    return stickerRepo.revertTransfer(stickerId, originalOwnerId);
  },
};

export default stickerService;