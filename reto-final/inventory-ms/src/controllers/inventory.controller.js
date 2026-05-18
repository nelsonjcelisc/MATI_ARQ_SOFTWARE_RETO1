import inventoryService from "../services/inventory.service.js";

const inventoryController = {
    async getAll(req, res, next) {
        try {
            const items = await inventoryService.getAll();

            res.status(200).json({
                success: true,
                data: items,
                count: items.length 
            });
        } catch (err) {
            next(err);
        }
    },
    async getBySticker(req, res, next) {
        try {
            const item = await inventoryService.getBySticker(req.params.stickerId);

            res.status(200).json({
                success: true,
                data: item,
            });
        } catch (err) {
            next(err);
        }
    },
    async getByOwner(req, res, next) {
        try {
            const item = await inventoryService.getByOwner(req.params.ownerId);

            res.status(200).json({
                success: true,
                data: item,
            });
        } catch (err) {
            next(err);
        }
    },
    async create(req, res, next) {
        try {
            const item = await inventoryService.create(req.body);

            res.status(201).json({
                success: true,
                data: item,
            });
        } catch (err) {
            next(err);
        }
    },
    async transferOwnership(req, res, next) {
        try {
            const { stickerId } = req.params;
            const { fromId, toId } = req.body;
            
            const item = await inventoryService.transferOwnership(stickerId, fromId, toId);

            res.status(200).json({
                success: true,
                data: item,
            });
        } catch (err) {
            next(err);
        }
    }
}

export default inventoryController;