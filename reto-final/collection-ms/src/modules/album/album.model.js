// src/modules/album/album.model.js
import { DataTypes, Model } from 'sequelize';
import sequelize from '../../config/database.config.js';
import Sticker from '../stickers/sticker.model.js';
import env from '../../config/env.config.js';

class Album extends Model {}

Album.init({
  id: {
    type:         DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey:   true,
  },
  collectorId: {
    type:      DataTypes.UUID,
    allowNull: false,
    field:     'collector_id',
  },
  collectionId: {
    type:      DataTypes.UUID,
    allowNull: false,
    field:     'collection_id',
  },
  totalSlots: {
    type:      DataTypes.INTEGER,
    allowNull: false,
    field:     'total_slots',
  },
  completedSlots: {
    type:         DataTypes.INTEGER,
    allowNull:    false,
    defaultValue: 0,
    field:        'completed_slots',
  },
  progressPct: {
    type:         DataTypes.DECIMAL(5, 2),
    allowNull:    false,
    defaultValue: 0,
    field:        'progress_pct',
  },
}, {
  sequelize,
  modelName:   'Album',
  tableName:   'albums',
  schema:      env.DB_SCHEMA,
  timestamps:  true,
  underscored: true,
});
// Association: an album tracks stickers by collectionId
// scope: status = 'EXCHANGED' — only stickers that completed a transfer count
Album.hasMany(Sticker, {
  foreignKey:  'collection_id',
  sourceKey:   'collectionId',
  as:          'stickers',
  scope:       { status: 'EXCHANGED' },
  constraints: false,             // no FK constraint — cross-schema join
});

export { Album, Sticker };