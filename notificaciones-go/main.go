package main

import (
	"context"
	"log"
	"net"
	"net/http"
	"time"

	pb "notificaciones-go/proto" // El código que genere el proto

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

// Definición de métricas
var (
	opsProcessed = prometheus.NewCounter(prometheus.CounterOpts{
		Name: "notificaciones_procesadas_total",
		Help: "Número total de notificaciones procesadas",
	})

	latencyHistogram = prometheus.NewHistogram(prometheus.HistogramOpts{
		Name:    "notificaciones_latencia_segundos",
		Help:    "Distribución de latencia en el procesamiento de notificaciones",
		Buckets: prometheus.DefBuckets, // Buckets por defecto (.005, .01, .025, .05, .1, .25, .5, 1, 2.5, 5, 10)
	})
)

func init() {
	// Registrar métricas en Prometheus
	prometheus.MustRegister(opsProcessed)
	prometheus.MustRegister(latencyHistogram)
}

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
	// Exponer métricas en un servidor HTTP separado (puerto 2112)
	go func() {
		http.Handle("/metrics", promhttp.Handler())
		log.Println("Servidor de métricas Prometheus escuchando en :2112")
		log.Fatal(http.ListenAndServe(":2112", nil))
	}()

	lis, _ := net.Listen("tcp", ":50051")
	// 2. Crea el cerebro del servidor gRPC
	s := grpc.NewServer()
	// 3. Conecta tu código con el servidor gRPC
	pb.RegisterNotificadorServer(s, &server{})
	// Habilitar reflection para depuración (grpcurl)
	reflection.Register(s)
	// 4. confirmar que todo va bien
	log.Println("Servidor de Notificaciones Go escuchando en :50051")
	// 5. Empieza a trabajar de verdad
	s.Serve(lis)
}
