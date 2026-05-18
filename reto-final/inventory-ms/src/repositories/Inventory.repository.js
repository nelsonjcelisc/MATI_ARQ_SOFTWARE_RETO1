import InventoryItem from "../models/Inventory-item.model.js";

export class NotFoundError extends Error {
    constructor(msg) {
        super(msg); 
        this.statusCode = 404;
    }
}

export class ConflictError extends Error {
    constructor(msg) {
        super(msg); 
        this.statusCode = 409;
    }
}

const inventoryRepository = {
    async findAll() {
        return InventoryItem.findAll({ order: [['created_at', 'DESC'], ['transferred_at', 'DESC']]});
    },
    async findBySticker(stickerId) {
        return InventoryItem.findOne({ where: {stickerId}})
    },
    async findByOwner(ownerId) {
        return InventoryItem.findAll({ where: { ownerId } });
    },
    async create(data) {
        return InventoryItem.create(data);
    },
    async transferOwnership(stickerId, fromId, toId) {
        const item = await InventoryItem.findOne({ where: {stickerId, ownerId: fromId}});

        if(!item) throw new ConflictError(
            `Sticker ${stickerId} not owned by ${fromId} in inventory`
        )

        return item.update({
            ownerId: toId,
            previousOwnerId: fromId,
            transferredAt: new Date()
        });
    }
}

export default inventoryRepository;