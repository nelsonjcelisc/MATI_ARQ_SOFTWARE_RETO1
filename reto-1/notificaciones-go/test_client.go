package main

import (
	"context"
	"log"
	"time"

	pb "notificaciones-go/proto"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

func main() {
	// 1. Conectamos al servidor que ya tienes corriendo
	conn, err := grpc.NewClient("notificador_mati:50051", grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("No pude conectar: %v", err)
	}
	defer conn.Close()
	c := pb.NewNotificadorServiceClient(conn)

	// 2. Creamos un "Match" de prueba (Tu Hola Mundo)
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()

	r, err := c.ProcesarNotificacion(ctx, &pb.MatchRequest{
		Id:         1001,
		ProductoId: 500,
		Cantidad:   10,
		Tipo:       1, // COMPRA
	})

	if err != nil {
		log.Fatalf("Error al enviar: %v", err)
	}

	log.Printf("Respuesta del servidor: %v", r.GetSuccess())
}
