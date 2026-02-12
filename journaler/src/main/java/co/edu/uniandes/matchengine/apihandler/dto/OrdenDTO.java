package co.edu.uniandes.matchengine.apihandler.dto;

public class OrdenDTO {
    // Datos de Negocio
    private long id;           // 8 bytes (ID numérico)
    private long productoId;   // 8 bytes (Nombre de 8 chars convertido a long)
    private int cantidad;      // 4 bytes
    private byte tipo;         // 1 byte (1=COMPRA, 2=VENTA)

    // Trazabilidad de Latencia (Nanosegundos para precisión LMAX)
    private long ts_api_recepcion;
    private long ts_api_salida;
    private long ts_rabbit_recepcion;
    private long ts_engine_match;
    private long ts_notificar_orden;
    private long ts_notificar_match;

    public OrdenDTO() {} // Necesario para la serialización de Spring

    public OrdenDTO(long id, long productoId, int cantidad, byte tipo) {
        this.id = id;
        this.productoId = productoId;
        this.cantidad = cantidad;
        this.tipo = tipo;
        this.ts_api_recepcion = System.nanoTime(); // Precisión máxima
    }

    // Getters y Setters
    public long getId() { return id; }
    public long getProductoId() { return productoId; }
    public int getCantidad() { return cantidad; }
    public byte getTipo() { return tipo; }
    
    public long getTs_api_recepcion() { return ts_api_recepcion; }
    public long getTs_api_salida() { return ts_api_salida; }
    public void setTs_api_salida(long ts) { this.ts_api_salida = ts; }
    public void setTs_rabbit_recepcion(long ts) { this.ts_rabbit_recepcion = ts; }
    public void setTs_engine_match(long ts) { this.ts_engine_match = ts; }
    public void setTs_notificar_orden(long ts) { this.ts_notificar_orden = ts; }
    public void setTs_notificar_match(long ts) { this.ts_notificar_match = ts; }
}