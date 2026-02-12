package co.edu.uniandes.matchengine.apihandler;

import co.edu.uniandes.matchengine.apihandler.dto.OrdenDTO;
import org.springframework.cloud.stream.function.StreamBridge; // IMPORTANTE: Faltaba este
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrdenController {

    private final StreamBridge streamBridge; // Declaración de la variable

    // Constructor para que Spring inyecte el StreamBridge
    public OrdenController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    // Endpoint para COMPRADOR
    @PostMapping("/orden-compra")
    public ResponseEntity<OrdenDTO> recibirCompra(@RequestBody Map<String, Object> payload) {
        return procesarOrden(payload, "COMPRA");
    }

    // Endpoint para VENDEDOR
    @PostMapping("/orden-venta")
    public ResponseEntity<OrdenDTO> recibirVenta(@RequestBody Map<String, Object> payload) {
        return procesarOrden(payload, "VENTA");
    }

    // Método de procesamiento con la lógica optimizada
    private ResponseEntity<OrdenDTO> procesarOrden(Map<String, Object> payload, String tipoTexto) {
        // 1. Conversión de ID a long
        long id = Long.parseLong(payload.get("id").toString());
        
        // 2. Conversión de producto a long (max 8 chars)
        long productoId = stringToLong(payload.get("producto").toString());
        
        // 3. Conversión de tipo a byte (1=COMPRA, 2=VENTA)
        byte tipoByte = (byte) (tipoTexto.equals("COMPRA") ? 1 : 2);

        // Creación del DTO optimizado (el constructor ya marca ts_api_recepcion)
        OrdenDTO orden = new OrdenDTO(
            id, 
            productoId, 
            (int) payload.get("cantidad"), 
            tipoByte
        );
        
        // Marcamos el tiempo de salida hacia el Bus de Eventos
        orden.setTs_api_salida(System.nanoTime());

        // Envío a RabbitMQ a través del bridge
        streamBridge.send("enviarOrden-out-0", orden); 
        
        return ResponseEntity.ok(orden);
    }

    // Método utilitario para empaquetar 8 caracteres en un solo long de 64 bits
    private long stringToLong(String str) {
        long result = 0;
        for (int i = 0; i < Math.min(str.length(), 8); i++) {
            result = (result << 8) | (str.charAt(i) & 0xff);
        }
        return result;
    }
}