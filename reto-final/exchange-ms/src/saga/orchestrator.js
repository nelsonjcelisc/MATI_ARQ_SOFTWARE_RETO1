import * as reserveStickerStep    from './steps/reserveSticker.step.js';
import * as lockExchangeStep      from './steps/lockExchange.step.js';
import * as transferOwnershipStep from './steps/transferOwnership.step.js';
import * as completeExchangeStep  from './steps/completeExchange.step.js';
import * as releaseStickersComp   from './compensation/releaseStickers.comp.js';
import * as deadLetterComp        from './compensation/deadLetter.comp.js';
import exchangeRepository         from '../repositories/exchange.repository.js';
import {
  sagaTotal,
  sagaDuration,
  compensationDuration,
  compensationTotal,
} from '../metrics.js';

async function logStep(exchange, step, status, error = null) {
  return exchangeRepository.appendLog(exchange, step, status, error);
}

export async function createExchange(data) {
  return exchangeRepository.create({ ...data, status: 'PENDING' });
}

export async function offerSticker(exchangeId, stickerId, collectorRole) {
  const exchange = await exchangeRepository.findById(exchangeId);
  if (!exchange) throw new Error(`Exchange ${exchangeId} not found`);

  await reserveStickerStep.execute(stickerId, exchangeId);

  const field = collectorRole === 'host' ? 'hostStickerId' : 'guestStickerId';
  await exchange.update({ [field]: stickerId });
  await logStep(exchange, `reserveSticker:${collectorRole}`, 'ok');

  return exchange.reload();
}

export async function executeExchange(exchangeId, chaosMode = false) {
  const exchange = await exchangeRepository.findById(exchangeId);
  if (!exchange) throw new Error(`Exchange ${exchangeId} not found`);
  if (exchange.status !== 'PENDING') {
    throw new Error(`Exchange ${exchangeId} is not in PENDING state`);
  }

  const sagaStart = Date.now();

  try {
    await lockExchangeStep.execute(exchange);
    await logStep(exchange, 'lockExchange', 'ok');

    await transferOwnershipStep.execute(exchange, chaosMode);
    await logStep(exchange, 'transferOwnership', 'ok');

    await completeExchangeStep.execute(exchange);
    await logStep(exchange, 'completeExchange', 'ok');

    const elapsed = (Date.now() - sagaStart) / 1000;
    sagaDuration.observe({ status: 'completed' }, elapsed);
    sagaTotal.inc({ status: 'completed' });

    return exchange.reload();

  } catch (err) {
    await logStep(exchange, err.failedStep || 'unknown', 'failed', err);

    const compensationStart = Date.now();
    try {
      await releaseStickersComp.execute(exchange);
      await logStep(exchange, 'compensation:releaseStickers', 'ok');
      compensationTotal.inc({ result: 'success' });
    } catch (compErr) {
      await deadLetterComp.execute(exchange, compErr);
      await logStep(exchange, 'compensation:deadLetter', 'sent');
      compensationTotal.inc({ result: 'failed' });
    }
    compensationDuration.observe((Date.now() - compensationStart) / 1000);

    const elapsed = (Date.now() - sagaStart) / 1000;
    sagaDuration.observe({ status: 'failed' }, elapsed);
    sagaTotal.inc({ status: 'failed' });

    await exchangeRepository.updateStatus(exchange.id, 'FAILED', {
      failureReason: err.message,
    });

    throw err;
  }
}
