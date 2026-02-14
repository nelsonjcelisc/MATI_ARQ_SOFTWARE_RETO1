package main

import (
	"context"
	"log"
	"net"
	"time"

	pb "notificaciones-go/proto" // El código que genere el proto

	"google.golang.org/grpc"
)

type server struct {
	pb.UnimplementedNotificadorServer
}

func (s *server) EnviarNotificacion(ctx context.Context, in *pb.MatchRequest) (*pb.NotificacionResponse, error) {

	// Asigna el tiempo actual en nanosegundos al campo 'TsApiRecepcion' del request recibido
	in.TsApiRecepcion = time.Now().UnixNano()

	// Agrega logs usando el paquete 'log' que muestren el ID del Match y la latencia calculada
	log.Printf("Match ID: %d | Match->Salida: %d ms | Salida->Recepcion: %d ms | Datos: %+v", in.OrdenId, (in.TsApiSalida-in.TsEngineMatch)/1000000, (in.TsApiRecepcion-in.TsApiSalida)/1000000, in)

	// TODO: Lógica para publicar el mensaje en RabbitMQ

	return &pb.NotificacionResponse{Exito: true, Mensaje: "Procesado correctamente"}, nil
}

func main() {
	lis, _ := net.Listen("tcp", ":50051")
	// 2. Crea el cerebro del servidor gRPC
	s := grpc.NewServer()
	// 3. Conecta tu código con el servidor gRPC
	pb.RegisterNotificadorServer(s, &server{})
	// 4. confirmar que todo va bien
	log.Println("Servidor de Notificaciones Go escuchando en :50051")
	// 5. Empieza a trabajar de verdad
	s.Serve(lis)
}
