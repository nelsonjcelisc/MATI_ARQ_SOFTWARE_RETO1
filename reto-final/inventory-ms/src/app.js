import express from 'express';
import cors from 'cors';
import client from 'prom-client';
import inventoryRoutes from './routes/inventory.routes.js';
import { errorHandler } from './middlewares/error-handler.middleware.js';

// Prometheus — default metrics (CPU, memory, event loop lag, etc.)
client.collectDefaultMetrics({ prefix: 'inventory_ms_' });

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.get('/health', (req, res) =>
  res.json({ status: 'ok', service: 'inventory-ms', timestamp: new Date() })
);

// Prometheus metrics endpoint
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', client.register.contentType);
  res.end(await client.register.metrics());
});

app.use('/api/v1/inventory', inventoryRoutes);

app.use('*splat', (req, res) =>
  res.status(404).json({ success: false, message: `${req.method} ${req.originalUrl} not found` })
);

app.use(errorHandler);

export default app;