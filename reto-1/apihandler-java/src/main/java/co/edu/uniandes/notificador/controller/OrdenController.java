package co.edu.uniandes.notificador.controller;

import co.edu.uniandes.notificador.grpc.NotificadorClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrdenController {

    @Autowired
    private NotificadorClient notificadorClient;

    @PostMapping("/orden")
    public String crearOrden(@RequestBody Map<String, Object> payload) {
        long id = Long.parseLong(payload.get("id").toString());
        long productoId = Long.parseLong(payload.get("productoId").toString());
        int cantidad = Integer.parseInt(payload.get("cantidad").toString());

        boolean ok = notificadorClient.enviarNotificacion(id, productoId, cantidad);

        return ok ? "Orden enviada a Go correctamente" : "Error al notificar a Go";
    }
}