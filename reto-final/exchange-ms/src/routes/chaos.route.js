import { Router } from 'express';
import chaosStore from '../chaos/chaos.store.js';

const router = Router();

// POST /chaos/on  { "failTarget": "guest" }  → enable chaos, fail that transfer role
router.post('/on', (req, res) => {
  chaosStore.enabled    = true;
  chaosStore.failTarget = req.body.failTarget ?? 'guest';
  res.json({ chaos: true, failTarget: chaosStore.failTarget });
});

// POST /chaos/off → restore normal operation
router.post('/off', (req, res) => {
  chaosStore.enabled = false;
  res.json({ chaos: false });
});

// GET /chaos → current state
router.get('/', (req, res) => {
  res.json(chaosStore);
});

export default router;
