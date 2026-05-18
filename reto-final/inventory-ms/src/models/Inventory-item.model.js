import { DataTypes, Model } from "sequelize";
import sequelize from "../config/database.config.js";
import env from "../config/env.config.js";

class InventoryItem extends Model {}

InventoryItem.init({
    id: {
        type: DataTypes.UUID,
        defaultValue: DataTypes.UUIDV4,
        primaryKey: true
    },
    stickerId: {
        type: DataTypes.UUID,
        allowNull: false,
        unique: true,
        field: 'sticker_id'
    },
    ownerId: {
        type: DataTypes.UUID,
        allowNull: false,
        field: 'owner_id'
    },
    previousOwnerId: {
        type: DataTypes.UUID,
        allowNull: true,
        field: 'previous_owner_id'
    },
    transferredAt: {
        type: DataTypes.DATE,
        allowNull: true,
        field: 'transferred_at'
  }
}, {
    sequelize,
    modelName: 'InventoryItem',
    tableName: 'inventory_items',
    schema: env.DB_SCHEMA,
    timestamps: true,
    underscored: true
});

export default InventoryItem;