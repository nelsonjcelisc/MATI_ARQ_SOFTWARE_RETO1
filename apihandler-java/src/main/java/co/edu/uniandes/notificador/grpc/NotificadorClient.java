package co.edu.uniandes.notificador.grpc;

import co.edu.uniandes.notificador.MatchRequest;
import co.edu.uniandes.notificador.NotificacionResponse;
import co.edu.uniandes.notificador.NotificadorGrpc; // CORREGIDO: Nombre generado por defecto
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class NotificadorClient {

    private final NotificadorGrpc.NotificadorBlockingStub stub; // CORREGIDO: Tipo de stub correcto

    public NotificadorClient(@Value("${grpc.notificador.host:notificaciones}") String host,
                             @Value("${grpc.notificador.port:50051}") int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        this.stub = NotificadorGrpc.newBlockingStub(channel); // CORREGIDO: Método de creación correcto
    }

    public boolean enviarNotificacion(long id, long productoId, int cantidad) {
        // IMPORTANTE: fíjate en los nombres de los setters generados (setProductoId, setTsEngineMatch)
        
        // Generamos timestamp compatible con Go (UnixNano)
        Instant now = Instant.now();
        long timestampNs = now.getEpochSecond() * 1_000_000_000L + now.getNano();

        MatchRequest request = MatchRequest.newBuilder()
                .setOrdenId(id)              // CORREGIDO: En el proto es 'ordenId'
                .setProductoId(productoId) 
                .setCantidad(cantidad)
                .setTsEngineMatch(timestampNs - 100_000_000L) // Simulación de delay de 100ms (100 * 10^6 ns)
                .setTsApiSalida(timestampNs)      // Salida a notificaciones-go
                .build();

        try {
            // El método en el proto se llama EnviarNotificacion -> enviarNotificacion en Java
            NotificacionResponse response = stub.enviarNotificacion(request);
            return response.getExito();      // CORREGIDO: En el proto es 'exito'
        } catch (Exception e) {
            System.err.println("Error en la comunicación gRPC con Go: " + e.getMessage());
            return false;
        }
    }
}