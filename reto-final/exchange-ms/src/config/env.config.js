// src/config/env.js
import dotenv from 'dotenv';
import Joi from 'joi';

dotenv.config();

const envSchema = Joi.object({
  NODE_ENV:      Joi.string().valid('development', 'production', 'test').default('development'),
  PORT:          Joi.number().default(3001),
  DB_HOST:       Joi.string().required(),
  DB_PORT:       Joi.number().default(5432),
  DB_NAME:       Joi.string().required(),
  DB_USER:       Joi.string().required(),
  DB_PASSWORD:   Joi.string().required(),
  DB_SCHEMA:     Joi.string().default('exchange'),
  RABBITMQ_URL:          Joi.string().required(),
  COLLECTION_MS_URL:     Joi.string().required(),
  INVENTORY_MS_URL:      Joi.string().required(),
  TRANSFER_RETRY_ATTEMPTS: Joi.number().default(1),
  TRANSFER_RETRY_DELAY_MS: Joi.number().default(500),
}).unknown(true);

const { error, value: env } = envSchema.validate(process.env);

if (error) throw new Error(`Config validation error: ${error.message}`);

export default env;