package com.uniandes.matching.domain.service;

import com.uniandes.matching.domain.model.Match;
import com.uniandes.matching.grpc.matching.MatchRequest;
import com.uniandes.matching.grpc.matching.MatchResponse;
import com.uniandes.matching.grpc.matching.MatchingServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchingServiceClient {

    @GrpcClient("matching-engine-node")
    private MatchingServiceGrpc.MatchingServiceBlockingStub matchingStub;

    /**
     * Llama al servicio de matching de Node.js y convierte los resultados
     * al modelo de dominio Match
     */
    public List<Match> performMatching(String requester) {
        try {
            log.info("Llamando al servicio de matching de Node.js con requester: {}", requester);
            
            MatchRequest request = MatchRequest.newBuilder()
                    .setRequester(requester)
                    .build();

            MatchResponse response = matchingStub.match(request);
            
            log.info("Respuesta recibida del servicio Node.js - RequestId: {}, Matches: {}", 
                    response.getRequestId(), response.getMatchesCount());

            // Convertir los matches del proto al modelo de dominio
            return response.getMatchesList().stream()
                    .map(this::convertToDomainMatch)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error al llamar al servicio de matching de Node.js", e);
            throw new RuntimeException("Error al realizar matching: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte un Match del proto (Node.js) al modelo de dominio Match
     * Usa el nombre completo del paquete para evitar conflicto con el modelo de dominio
     */
    private Match convertToDomainMatch(com.uniandes.matching.grpc.matching.Match protoMatch) {
        Match domainMatch = Match.builder()
                .buyOrderId(protoMatch.getBidId())      // bidId -> buyOrderId
                .saleOrderId(protoMatch.getOfferId())   // offerId -> saleOrderId
                .symbol(protoMatch.getSymbol())
                .quantity((int) protoMatch.getQty())    // double -> int
                .price(BigDecimal.valueOf(protoMatch.getPrice()))
                .build();

        domainMatch.initialize(); // Genera ID y timestamp
        return domainMatch;
    }
}

