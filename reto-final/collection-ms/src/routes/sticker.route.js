// src/routes/sticker.routes.js
import { Router } from 'express';
import stickerController from '../controllers/sticker.controller.js';

const router = Router();
router.get('/:id',                    stickerController.getById);
router.post('/',                      stickerController.create);
router.post('/:id/reserve',           stickerController.reserve);
router.post('/:id/release',           stickerController.release);
router.patch('/:id/owner',            stickerController.transferOwnership);
export default router;