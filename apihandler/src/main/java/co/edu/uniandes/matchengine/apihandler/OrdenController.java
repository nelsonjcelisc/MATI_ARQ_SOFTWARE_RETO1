package co.edu.uniandes.matchengine.apihandler;

import co.edu.uniandes.matchengine.apihandler.dto.OrdenDTO;
import org.springframework.cloud.stream.function.StreamBridge; // NUEVO
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrdenController {

    private final StreamBridge streamBridge; // NUEVO: El puente al Bus de Eventos

    // Constructor para inyectar el Bridge
    public OrdenController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PostMapping("/orden-compra")
    public ResponseEntity<OrdenDTO> recibirCompra(@RequestBody Map<String, Object> payload) {
        return procesarOrden(payload, "COMPRA");
    }

    @PostMapping("/orden-venta")
    public ResponseEntity<OrdenDTO> recibirVenta(@RequestBody Map<String, Object> payload) {
        return procesarOrden(payload, "VENTA");
    }

    private ResponseEntity<OrdenDTO> procesarOrden(Map<String, Object> payload, String tipo) {
        OrdenDTO orden = new OrdenDTO(
            payload.get("id").toString(),
            payload.get("producto").toString(),
            (int) payload.get("cantidad"),
            tipo
        );
        
        orden.registrarHito("apihandler_salida");

        // --- PUBLICACIÓN AL BUS ---
        // 'enviarOrden-out-0' es el nombre del canal que usaremos en el application.yml
        streamBridge.send("enviarOrden-out-0", orden); 
        // --------------------------

        System.out.println("Enviado a RabbitMQ: " + tipo + " de " + orden.getProducto());
        
        return ResponseEntity.ok(orden);
    }
}