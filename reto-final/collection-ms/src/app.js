import express from 'express';
import cors from 'cors';
import client from 'prom-client';
import stickerRoutes from './routes/sticker.route.js';
import albumRoutes from './routes/album.route.js';
import { errorHandler } from './middlewares/error-handler.middleware.js';

client.collectDefaultMetrics({ prefix: 'collection_ms_' });

const app = express();
app.use(cors());
app.use(express.json({ limit: '10mb' }));

app.get('/health', (req, res) =>
  res.json({ status: 'ok', service: 'collection-ms', timestamp: new Date() })
);

app.get('/metrics', async (req, res) => {
  res.set('Content-Type', client.register.contentType);
  res.end(await client.register.metrics());
});

app.use('/api/v1/stickers', stickerRoutes);
app.use('/api/v1/albums',   albumRoutes);

app.use('*splat', (req, res) =>
  res.status(404).json({ success: false, message: `${req.method} ${req.originalUrl} not found` })
);

app.use(errorHandler);

export default app;
