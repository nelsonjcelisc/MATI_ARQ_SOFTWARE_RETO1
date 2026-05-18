// src/config/rabbitmq.js
import amqp from 'amqplib';
import env from './env.config.js';

let channel;

export async function connectRabbitMQ() {
  const connection = await amqp.connect(env.RABBITMQ_URL);
  channel = await connection.createChannel();
  // Declara el exchange que exchange-ms publica
  // fanout → todos los consumers vinculados reciben el mensaje
  await channel.assertExchange('album.recalculate', 'fanout', { durable: true });
  console.log('✓ RabbitMQ connected');
}

export function getChannel() {
  if (!channel) throw new Error('RabbitMQ channel not initialized');
  return channel;
}