import client from 'prom-client';

// Total SAGA executions labelled by final status
export const sagaTotal = new client.Counter({
  name:       'exchange_saga_total',
  help:       'Total SAGA executions by status',
  labelNames: ['status'],  // completed | failed
});

// Full SAGA wall-clock time (lock → transfer → complete OR lock → transfer → compensate)
export const sagaDuration = new client.Histogram({
  name:       'exchange_saga_duration_seconds',
  help:       'End-to-end SAGA execution duration in seconds',
  labelNames: ['status'],
  buckets:    [0.05, 0.1, 0.25, 0.5, 1, 2, 5, 10],
});

// How long the compensation phase (revert transfers + release reservations) takes
export const compensationDuration = new client.Histogram({
  name:    'exchange_compensation_duration_seconds',
  help:    'Compensation (revert + release) duration in seconds after SAGA failure',
  buckets: [0.02, 0.05, 0.1, 0.25, 0.5, 1, 2],
});

// Total compensation runs and whether they succeeded
export const compensationTotal = new client.Counter({
  name:       'exchange_compensation_total',
  help:       'Total compensation executions by result',
  labelNames: ['result'],  // success | failed
});
