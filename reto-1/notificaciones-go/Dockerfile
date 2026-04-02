# ETAPA 1: Generación de Protos y Compilación
FROM golang:1.23-alpine AS builder

# 1. Instalar dependencias del sistema
RUN apk add --no-cache protobuf protobuf-dev git

# 2. Instalar los plugins de Go para protoc
RUN go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.34.2
RUN go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.5.1

WORKDIR /app

# 3. Copiamos los archivos base
COPY go.mod ./
COPY notificaciones.proto ./
COPY main.go ./

# 4. GENERAR EL CÓDIGO PROTO PRIMERO
# Esto crea los archivos .go necesarios para que 'go mod' vea las dependencias
RUN mkdir -p proto && \
    protoc --go_out=. --go-grpc_out=. notificaciones.proto

# 5. AHORA SÍ, DESCARGAR DEPENDENCIAS
# Forzamos las versiones compatibles con Go 1.23
RUN go mod edit -require google.golang.org/grpc@v1.70.0
RUN go mod edit -require google.golang.org/protobuf@v1.34.2
RUN go mod tidy

# 6. Compilamos
RUN go build -o notificador-go main.go

# ETAPA 2: Imagen de ejecución (ligera)
FROM alpine:latest
WORKDIR /root/
# Ojo aquí: el binario se llama notificador-go según tu comando de build
COPY --from=builder /app/notificador-go .
EXPOSE 50051
CMD ["./notificador-go"]