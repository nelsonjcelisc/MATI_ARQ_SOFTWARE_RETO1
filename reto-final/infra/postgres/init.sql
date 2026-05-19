-- Creates schemas for each microservice
-- Runs once on postgres container first start
CREATE SCHEMA IF NOT EXISTS exchange;
CREATE SCHEMA IF NOT EXISTS collection;
CREATE SCHEMA IF NOT EXISTS inventory;
GRANT ALL PRIVILEGES ON SCHEMA exchange   TO appuser;
GRANT ALL PRIVILEGES ON SCHEMA collection TO appuser;
GRANT ALL PRIVILEGES ON SCHEMA inventory  TO appuser;