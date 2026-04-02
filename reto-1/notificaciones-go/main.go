package main

import (
	"context"
	"log"
	"net"
	"net/http"
	"time"

	pb "notificaciones-go/proto"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

// Definición de métricas (TODAS JUNTAS)
var (
	// Métricas Originales
	opsProcessed = prometheus.NewCounter(prometheus.CounterOpts{
		Name: "notificaciones_procesadas_total",
		Help: "Número total de notificaciones procesadas",
	})

	latencyHistogram = prometheus.NewHistogram(prometheus.HistogramOpts{
		Name:    "notificaciones_latencia_segundos",
		Help:    "Distribución de latencia en el procesamiento de notificaciones",
		Buckets: prometheus.DefBuckets,
	})

	// Métricas Nuevas de Arquitectura
	latenciaJavaHistogram = prometheus.NewHistogram(prometheus.HistogramOpts{
		Name:    "latencia_java_match_a_salida_segundos",
		Help:    "Tiempo desde que ocurre el match hasta que Java envía la notificación",
		Buckets: []float64{0.001, 0.005, 0.01, 0.025, 0.05, 0.1},
	})

	latenciaRedHistogram = prometheus.NewHistogram(prometheus.HistogramOpts{
		Name:    "latencia_red_salida_a_recepcion_segundos",
		Help:    "Tiempo de tránsito del mensaje por la red (gRPC)",
		Buckets: []float64{0.0005, 0.001, 0.002, 0.005, 0.01, 0.025},
	})

	latenciaTotalHistogram = prometheus.NewHistogram(prometheus.HistogramOpts{
		Name:    "latencia_total_match_a_recepcion_segundos",
		Help:    "Tiempo total desde el match en Java hasta la recepción en Go",
		Buckets: []float64{0.001, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25},
	})
)

func init() {
	// Registrar todas las métricas en Prometheus
	prometheus.MustRegister(opsProcessed)
	prometheus.MustRegister(latencyHistogram)
	prometheus.MustRegister(latenciaJavaHistogram)
	prometheus.MustRegister(latenciaRedHistogram)
	prometheus.MustRegister(latenciaTotalHistogram)
}

type server struct {
	pb.UnimplementedNotificadorServer
}

// ÚNICA declaración de EnviarNotificacion
func (s *server) EnviarNotificacion(ctx context.Context, in *pb.MatchRequest) (*pb.NotificacionResponse, error) {
	startTime := time.Now()
	ahora := time.Now().UnixNano()
	in.TsApiRecepcion = ahora

	// 1. Cálculos de Latencia
	diffJava := float64(in.TsApiSalida-in.TsEngineMatch) / 1e9     //envio de java a go - recepción de match
	diffRed := float64(in.TsApiRecepcion-in.TsApiSalida) / 1e9     //recepción en go - envío de java
	diffTotal := float64(in.TsApiRecepcion-in.TsEngineMatch) / 1e9 //recepción en go - match en java (total)

	// 2. Observar en Histogramas
	latenciaJavaHistogram.Observe(diffJava)
	latenciaRedHistogram.Observe(diffRed)
	latenciaTotalHistogram.Observe(diffTotal)

	// 3. Métricas base
	opsProcessed.Inc()
	duration := time.Since(startTime).Seconds()
	latencyHistogram.Observe(duration)

	log.Printf("Match ID: %d | Java: %.2fms | Red: %.2fms | Total: %.2fms",
		in.OrdenId, diffJava*1000, diffRed*1000, diffTotal*1000)

	return &pb.NotificacionResponse{Exito: true, Mensaje: "Procesado correctamente"}, nil
}

func main() {
	// Servidor de Métricas (Puerto 2112)
	go func() {
		http.Handle("/metrics", promhttp.Handler())
		log.Println("Servidor de métricas Prometheus listo en :2112")
		log.Fatal(http.ListenAndServe(":2112", nil))
	}()

	// Servidor gRPC (Puerto 50051)
	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("Fallo al escuchar: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterNotificadorServer(s, &server{})
	reflection.Register(s)

	log.Println("Servidor gRPC escuchando en :50051")
	if err := s.Serve(lis); err != nil {
		log.Fatalf("Fallo al servir gRPC: %v", err)
	}
}
