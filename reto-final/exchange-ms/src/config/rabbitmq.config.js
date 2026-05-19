// src/config/rabbitmq.js
import amqp from 'amqplib';
import env from './env.config.js';

let channel;

export async function connectRabbitMQ() {
  const connection = await amqp.connect(env.RABBITMQ_URL);
  channel = await connection.createChannel();
  // exchange-ms declara ambos exchanges — es el publicador
  await channel.assertExchange('album.recalculate', 'fanout', { durable: true });
  await channel.assertExchange('saga.compensation.dead-letter', 'direct', { durable: true });
  // DLQ donde caen las compensaciones fallidas
  await channel.assertQueue('saga.dlq', { durable: true });
  await channel.bindQueue('saga.dlq', 'saga.compensation.dead-letter', '');
  console.log('✓ RabbitMQ connected — exchanges declared');
}

export function getChannel() {
  if (!channel) throw new Error('RabbitMQ channel not initialized');
  return channel;
}