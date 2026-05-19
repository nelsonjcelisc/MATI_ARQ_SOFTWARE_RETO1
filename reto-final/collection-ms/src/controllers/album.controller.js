import * as albumRepository from '../modules/album/album.repository.js';

const albumController = {
  async getAlbum(req, res, next) {
    try {
      const { collectorId, collectionId } = req.params;
      const album = await albumRepository.getAlbumWithStickers(collectorId, collectionId);
      if (!album) return res.status(404).json({ success: false, message: 'Album not found' });
      res.status(200).json({ success: true, data: album });
    } catch (err) { next(err); }
  },

  async createAlbum(req, res, next) {
    try {
      const album = await albumRepository.createAlbum(req.body);
      res.status(201).json({ success: true, data: album });
    } catch (err) { next(err); }
  },
};

export default albumController;
