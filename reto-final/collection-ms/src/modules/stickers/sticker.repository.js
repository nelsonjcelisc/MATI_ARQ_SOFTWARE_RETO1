// src/modules/stickers/sticker.repository.js
import sequelize from '../../config/database.config.js';
import Sticker from './sticker.model.js';

export class StickerNotAvailableError extends Error {
  constructor(id) { super(`Sticker ${id} not available`); this.statusCode = 409; }
}

export class StickerNotFoundError extends Error {
  constructor(id) { super(`Sticker ${id} not found`); this.statusCode = 404; }
}

// Reserve: AVAILABLE → RESERVED
// SELECT FOR UPDATE serializa el acceso — la segunda transacción concurrent
// espera hasta que la primera haga commit o rollback antes de leer el row.
export async function reserve(stickerId, exchangeId) {
  return sequelize.transaction(async (t) => {
    const sticker = await Sticker.findOne({
      where: { id: stickerId, status: 'AVAILABLE' },
      lock:  t.LOCK.UPDATE,
      transaction: t,
    });
    
    if (!sticker) throw new StickerNotAvailableError(stickerId);
    
    return sticker.update({
            status: 'RESERVED',
            reservedForExchangeId: exchangeId,
        }, 
        { transaction: t }
    );
  });
}

// Release: RESERVED → AVAILABLE (used in compensation)
export async function release(stickerId) {
  return sequelize.transaction(async (t) => {
    const sticker = await Sticker.findOne({
      where: { id: stickerId, status: 'RESERVED' },
      lock:  t.LOCK.UPDATE,
      transaction: t,
    });
    if (!sticker) throw new StickerNotFoundError(stickerId);
    return sticker.update({
      status:                'AVAILABLE',
      reservedForExchangeId: null,
    }, { transaction: t });
  });
}

// Transfer ownership: RESERVED → EXCHANGED + new owner
export async function transferOwnership(stickerId, newOwnerId) {
  return sequelize.transaction(async (t) => {
    const sticker = await Sticker.findOne({
      where: { id: stickerId, status: 'RESERVED' },
      lock:  t.LOCK.UPDATE,
      transaction: t,
    });
    if (!sticker) throw new StickerNotFoundError(stickerId);
    return sticker.update({
      ownerId:               newOwnerId,
      status:                'EXCHANGED',
      reservedForExchangeId: null,
    }, { transaction: t });
  });
}

// Revert a transfer (compensation): EXCHANGED → back to RESERVED with original owner
// Called when Promise.all partial failure needs to roll back a succeeded transfer
export async function revertTransfer(stickerId, originalOwnerId) {
  return sequelize.transaction(async (t) => {
    const sticker = await Sticker.findOne({
      where: { id: stickerId, status: 'EXCHANGED' },
      lock:  t.LOCK.UPDATE,
      transaction: t,
    });
    if (!sticker) throw new StickerNotFoundError(stickerId);
    return sticker.update({
      ownerId: originalOwnerId,
      status:  'RESERVED',
    }, { transaction: t });
  });
}

// Standard CRUD
export const findById = (id)  => Sticker.findByPk(id);
export const create = (data) => Sticker.create(data);