// src/models/Exchange.js
import { DataTypes, Model } from 'sequelize';
import sequelize from '../config/database.config.js';
import env from '../config/env.config.js';

class Exchange extends Model {}

Exchange.init({
  id: {
    type:         DataTypes.UUID,
    defaultValue: DataTypes.UUIDV4,
    primaryKey:   true,
  },
  hostCollectorId: {
    type:      DataTypes.UUID,
    allowNull: false,
    field:     'host_collector_id',
  },
  guestCollectorId: {
    type:      DataTypes.UUID,
    allowNull: true,
    field:     'guest_collector_id',
  },
  hostStickerId: {
    type:      DataTypes.UUID,
    allowNull: true,
    field:     'host_sticker_id',
  },
  guestStickerId: {
    type:      DataTypes.UUID,
    allowNull: true,
    field:     'guest_sticker_id',
  },
  status: {
    type:         DataTypes.ENUM('PENDING', 'LOCKED', 'COMPLETED', 'FAILED'),
    allowNull:    false,
    defaultValue: 'PENDING',
  },
  failureReason: {
    type:      DataTypes.TEXT,
    allowNull: true,
    field:     'failure_reason',
  },
  // JSONB — cada step del SAGA appenda su resultado aquí
  // Permite observar qué pasó sin tabla de auditoría separada
  // Indexable: WHERE saga_log @> '[{"step":"transferOwnership","status":"failed"}]'
  sagaLog: {
    type:         DataTypes.JSONB,
    allowNull:    false,
    defaultValue: [],
    field:        'saga_log',
  },
}, {
  sequelize,
  modelName:   'Exchange',
  tableName:   'exchange_state',
  schema:      env.DB_SCHEMA,
  timestamps:  true,
  underscored: true,
});

export default Exchange;