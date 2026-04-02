package com.uniandes.matching.web.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;
    private String status;
    private String message;
    private LocalDateTime timestamp;

    public static OrderResponse accepted(String orderId) {
        return OrderResponse.builder()
                .orderId(orderId)
                .status("ACCEPTED")
                .message("Order received and processing")
                .timestamp(LocalDateTime.now())
                .build();
    }
}