import inventoryRepository, { NotFoundError } from "../repositories/Inventory.repository.js";

const inventoryService = {
    async getAll() {
        return inventoryRepository.findAll();
    },
    async getBySticker(stickerId) {
        const item = await inventoryRepository.findBySticker(stickerId);

        if (!item) throw new NotFoundError(`Sticker ${stickerId} not found in inventory`);

        return item;
    },
    async getByOwner(ownerId) {
        return inventoryRepository.findByOwner(ownerId);
    },
    async create(data) {
        return inventoryRepository.create(data);
    },
    async transferOwnership(stickerId, fromId, toId) {
        return inventoryRepository.transferOwnership(stickerId, fromId, toId);
    },
}

export default inventoryService;