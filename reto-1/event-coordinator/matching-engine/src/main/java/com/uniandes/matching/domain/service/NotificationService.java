package com.uniandes.matching.domain.service;

import com.uniandes.matching.domain.model.Match;
import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.grpc.MatchRequest;
import com.uniandes.matching.grpc.NotificadorGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class NotificationService {

    @GrpcClient("notificaciones")
    private NotificadorGrpc.NotificadorBlockingStub notificadorStub;

    public void broadcastNewOrder(Order order) {
        log.info("Broadcasting new {} order: {} - Symbol: {} - Price: {} - Qty: {}",
                order.getType(),
                order.getId(),
                order.getSymbol(),
                order.getPrice(),
                order.getQuantity());

        // TODO: Implementar WebSocket broadcast cuando esté listo
    }

    public void sendMatchNotification(Match match) {
        try {
            log.info("Enviando notificación de match vía gRPC: {}", match.getId());

            Instant now = Instant.now();
            long timestampNs = now.getEpochSecond() * 1_000_000_000L + now.getNano();

            MatchRequest request = MatchRequest.newBuilder()
                    .setOrdenId(match.getId().hashCode()) // Go usa int64, hashcode temporal
                    .setTsEngineMatch(timestampNs)
                    .setTsApiSalida(timestampNs)    // Simulado por ahora
                    .build();

            var response = notificadorStub.enviarNotificacion(request);
            log.info("Respuesta del notificador: {}", response.getMensaje());
        } catch (Exception e) {
            log.error("Error enviando notificación gRPC", e);
        }
    }
}