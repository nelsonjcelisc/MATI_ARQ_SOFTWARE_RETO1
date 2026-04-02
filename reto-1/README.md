¡Excelente! Como todo buen arquitecto, sabes que si no está documentado, no existe. Vamos a dejar el README.md impecable para que cuando tus profesores o compañeros lo vean, entiendan que no solo hiciste código, sino que diseñaste una infraestructura resiliente y escalable.

Aquí tienes una estructura profesional que resume todas las batallas que ganamos hoy.

📄 README.md: Reto 1 - Arquitectura de Microservicios
🏗️ Descripción del Sistema
Este componente es el API Handler, el punto de entrada principal para el procesamiento de órdenes de compra y venta. Está diseñado bajo una arquitectura de microservicios, utilizando un Load Balancer (Nginx) para distribuir la carga entre múltiples instancias de la lógica de negocio desarrollada en Java 17.

🛠️ Stack Tecnológico
Lenguaje: Java 17 (OpenJDK Alpine)

Framework: Spring Boot 3.x

Proxy/Load Balancer: Nginx (Alpine)

Orquestación: Docker Compose

Infraestructura: Ubuntu 24.04 LTS (Oso Cloud Infrastructure)

🚀 Cómo Ejecutar el Proyecto
1. Clonar y Preparar el entorno
Asegúrate de estar en la raíz del proyecto donde se encuentra el archivo docker-compose.yml.

2. Levantar la Infraestructura (Escalable)
Para este reto, hemos configurado el sistema para que inicie con 3 instancias del API Handler para demostrar el balanceo de carga:

Bash
docker compose up -d --build --scale apihandler=3
3. Verificar el estado de los contenedores
Bash
docker ps
Deberías ver nginx-balancer en el puerto 80 y tres instancias de apihandler.

🧪 Pruebas de Funcionamiento (CURL)
El sistema está configurado para recibir tráfico a través del puerto 80 (puerto estándar), el cual es gestionado por Nginx.

Orden de Compra
Bash
curl -i -X POST http://localhost/api/orden-compra \
-H "Content-Type: application/json" \
-d '{"id": "C1", "producto": "AAPL", "cantidad": 10}'
Orden de Venta
Bash
curl -i -X POST http://localhost/api/orden-venta \
-H "Content-Type: application/json" \
-d '{"id": "V1", "producto": "MSFT", "cantidad": 50}'
📊 Observabilidad y Monitoreo
Logs en tiempo real
Para observar cómo Nginx distribuye las peticiones entre las 3 instancias (Round Robin), ejecuta:

Bash
docker compose logs -f apihandler
Timestamps de Latencia
Cada respuesta incluye un objeto timestamps que permite medir:

apihandler_recepcion: Momento exacto en que la orden entró al sistema.

apihandler_salida: Momento en que la orden terminó de procesarse (listo para el siguiente microservicio).

📐 Decisiones de Arquitectura
Nginx vs Traefik: Se optó por Nginx para garantizar la máxima compatibilidad con el motor de Docker del host, eliminando dependencias de versiones de API del socket de Docker.

Límites de Recursos: Cada instancia de Java está limitada a 512MB de RAM (-Xmx512m) para asegurar la estabilidad del servidor host.

Escalabilidad Horizontal: El uso de un upstream en Nginx permite que el sistema crezca o decrezca en instancias sin interrumpir el servicio.