import { Router } from 'express'; 
import inventoryController from '../controllers/inventory.controller.js';
import { validateRequest } from '../middlewares/validate-request.middleware.js';
import { createItemSchema, transferOwnershipSchema } from '../validators/inventory.validator.js';

const inventoryRoutes = Router();

inventoryRoutes.get('/', inventoryController.getAll);
inventoryRoutes.get('/sticker/:stickerId', inventoryController.getBySticker);
inventoryRoutes.get('/owner/:ownerId', inventoryController.getByOwner);
inventoryRoutes.post('/', validateRequest(createItemSchema), inventoryController.create);
inventoryRoutes.patch('/sticker/:stickerId/owner', validateRequest(transferOwnershipSchema), inventoryController.transferOwnership);

export default inventoryRoutes;
