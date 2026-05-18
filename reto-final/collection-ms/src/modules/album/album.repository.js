// src/modules/album/album.repository.js
import { Album, Sticker } from './album.model.js';
import sequelize from '../../config/database.config.js';

export class AlbumNotFoundError extends Error {
  constructor(id) { super(`Album not found for collector ${id}`); this.statusCode = 404; }
}

// Recalculate album progress after an exchange completes.
//
// Generated SQL (single query):
//   SELECT album.*, COUNT(sticker.id) as sticker_count
//   FROM collection.albums album
//   LEFT JOIN collection.stickers sticker
//     ON sticker.collection_id = album.collection_id
//     AND sticker.owner_id = album.collector_id
//     AND sticker.status = 'EXCHANGED'
//   WHERE album.collector_id = ? AND album.collection_id = ?
//
// attributes: ['id'] on include → SELECT only id from stickers (we only need count)
// required: false → LEFT JOIN — album exists even if collector has 0 exchanged stickers
export async function recalculateProgress(collectorId, collectionId) {
  return sequelize.transaction(async (t) => {
    const album = await Album.findOne({
      where: { collectorId, collectionId },
      include: [{
        model:      Sticker,
        as:         'stickers',
        where:      { ownerId: collectorId, status: 'EXCHANGED' },
        required:   false,
        attributes: ['id'],     // minimal data — we only need .length
      }],
      transaction: t,
    });
    
    if (!album) throw new AlbumNotFoundError(collectorId);
    
    const completedSlots = album.stickers.length;
    
    const progressPct = parseFloat(
      ((completedSlots / album.totalSlots) * 100).toFixed(2)
    );
    
    return album.update({ completedSlots, progressPct }, { transaction: t });
  });
}
// Full album read — no transaction needed (read path)
export async function getAlbumWithStickers(collectorId, collectionId) {
  return Album.findOne({
    where: { collectorId, collectionId },
    include: [{
      model:      Sticker,
      as:         'stickers',
      where:      { ownerId: collectorId },
      required:   false,
      attributes: ['id', 'name', 'number', 'status'],
      order:      [['number', 'ASC']],
    }],
  });
}

export const createAlbum = (data) => Album.create(data);