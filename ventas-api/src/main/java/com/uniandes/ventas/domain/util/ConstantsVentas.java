package com.uniandes.ventas.domain.util;

public class ConstantsVentas {
    public static final String REDIS_PREFIX = "idempotency:orden:";
    // Regex para un UUID estándar
    public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
}
