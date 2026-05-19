import axios from 'axios';
import env from '../config/env.config.js';

const http = axios.create({
  baseURL: env.COLLECTION_MS_URL,
  timeout: 5000,
});

export const reserve           = (stickerId, exchangeId)      =>
  http.post(`/api/v1/stickers/${stickerId}/reserve`, { exchangeId });

export const release           = (stickerId)                  =>
  http.post(`/api/v1/stickers/${stickerId}/release`);

export const transferOwnership = (stickerId, newOwnerId)      =>
  http.patch(`/api/v1/stickers/${stickerId}/owner`, { newOwnerId });

export const revertTransfer    = (stickerId, originalOwnerId) =>
  http.patch(`/api/v1/stickers/${stickerId}/owner`, { newOwnerId: originalOwnerId });
