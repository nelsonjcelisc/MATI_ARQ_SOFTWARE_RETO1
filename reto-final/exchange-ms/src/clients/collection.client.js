import axios from 'axios';
import env from '../config/env.config.js';

const http = axios.create({
  baseURL: env.COLLECTION_MS_URL,
  timeout: 5000,
});

export const reserve              = (stickerId, exchangeId)      =>
  http.post(`/api/v1/stickers/${stickerId}/reserve`, { exchangeId });

export const release              = (stickerId)                  =>
  http.post(`/api/v1/stickers/${stickerId}/release`);

export const transferOwnership    = (stickerId, newOwnerId)      =>
  http.patch(`/api/v1/stickers/${stickerId}/owner`, { newOwnerId });

// Hits the dedicated revert route (EXCHANGED → RESERVED) — different from transferOwnership
// which requires RESERVED status and would fail on an already-exchanged sticker.
export const revertTransfer       = (stickerId, originalOwnerId) =>
  http.patch(`/api/v1/stickers/${stickerId}/owner/revert`, { newOwnerId: originalOwnerId });

// Chaos endpoint — collection-ms always returns 500. Used to guarantee
// transfer B fails so compensation triggers deterministically in stress tests.
export const failTransferOwnership = (stickerId) =>
  http.patch(`/api/v1/stickers/${stickerId}/owner/fail`);
