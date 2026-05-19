import { Router } from 'express';
import exchangeController from '../controllers/exchange.controller.js';

const router = Router();

router.post('/',            exchangeController.create);
router.post('/:id/offer',   exchangeController.offer);
router.post('/:id/execute', exchangeController.execute);
router.get('/:id',          exchangeController.getById);

export default router;
