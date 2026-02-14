#!/bin/bash

echo "🔍 Monitoring HikariCP - Press Ctrl+C to stop"
echo ""

while true; do
    TIMESTAMP=$(date '+%H:%M:%S')
    
    RESPONSE=$(curl -s http://localhost:8080/api/v1/monitoring/hikari)
    
    ACTIVE=$(echo $RESPONSE | jq -r '.active')
    IDLE=$(echo $RESPONSE | jq -r '.idle')
    TOTAL=$(echo $RESPONSE | jq -r '.total')
    WAITING=$(echo $RESPONSE | jq -r '.waiting')
    MAX=$(echo $RESPONSE | jq -r '.maxPoolSize')
    
    # Calcular utilización
    if [ "$TOTAL" -gt 0 ]; then
        UTIL=$(echo "scale=1; ($ACTIVE * 100) / $TOTAL" | bc)
    else
        UTIL="0"
    fi
    
    # Color según estado
    if [ "$WAITING" -gt 0 ]; then
        STATUS="⚠️  BLOCKED"
    else
        STATUS="✓ OK"
    fi
    
    # Imprimir línea
    printf "%s | Active: %2d/%2d | Idle: %2d | Wait: %2d | Util: %5s%% | %s\n" \
        "$TIMESTAMP" "$ACTIVE" "$TOTAL" "$IDLE" "$WAITING" "$UTIL" "$STATUS"
    
    # sleep 1 #tiempo de espera entre requests
done