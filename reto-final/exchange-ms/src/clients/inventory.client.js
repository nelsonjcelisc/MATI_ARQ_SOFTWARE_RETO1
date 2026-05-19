import axios from 'axios';
import env from '../config/env.config.js';

const http = axios.create({
  baseURL: env.INVENTORY_MS_URL,
  timeout: 5000,
});

export const transferOwnership = (stickerId, fromId, toId) =>
  http.patch(`/api/v1/inventory/sticker/${stickerId}/owner`, { fromId, toId });

export const revertTransfer    = (stickerId, fromId, toId) =>
  http.patch(`/api/v1/inventory/sticker/${stickerId}/owner`, { fromId, toId });
