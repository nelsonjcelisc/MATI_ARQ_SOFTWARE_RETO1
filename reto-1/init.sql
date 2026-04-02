CREATE TABLE IF NOT EXISTS journal_ordenes (
    id BIGINT PRIMARY KEY,
    producto_id BIGINT,
    cantidad INT,
    tipo SMALLINT,
    ts_api_recepcion BIGINT,
    ts_api_salida BIGINT,
    ts_journal_registro BIGINT
);