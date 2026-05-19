import { Router } from 'express';
import albumController from '../controllers/album.controller.js';

const router = Router();

router.get('/:collectorId/:collectionId', albumController.getAlbum);
router.post('/',                          albumController.createAlbum);

export default router;
