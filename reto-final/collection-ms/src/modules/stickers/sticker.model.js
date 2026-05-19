// src/modules/stickers/sticker.model.js
import { DataTypes, Model } from 'sequelize';
import sequelize from '../../config/database.config.js';
import env from '../../config/env.config.js';

class Sticker extends Model {}

Sticker.init({
  id: {
    type:         DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey:   true,
  },
  name: {
    type:      DataTypes.STRING(100),
    allowNull: false,
  },
  number: {
    type:      DataTypes.INTEGER,
    allowNull: false,
  },
  collectionId: {
    type:      DataTypes.UUID,
    allowNull: false,
    field:     'collection_id',
  },
  ownerId: {
    type:      DataTypes.UUID,
    allowNull: false,
    field:     'owner_id',
  },
  status: {
    type:         DataTypes.ENUM('AVAILABLE', 'RESERVED', 'EXCHANGED'),
    allowNull:    false,
    defaultValue: 'AVAILABLE',
  },
  reservedForExchangeId: {
    type:      DataTypes.UUID,
    allowNull: true,
    field:     'reserved_for_exchange_id',
  },
}, {
  sequelize,
  modelName:   'Sticker',
  tableName:   'stickers',
  schema:      env.DB_SCHEMA,
  timestamps:  true,
  underscored: true,
});

export default Sticker;