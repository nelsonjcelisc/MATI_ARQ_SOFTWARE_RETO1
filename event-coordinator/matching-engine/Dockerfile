# ==========================================
# Etapa 1: BUILD (Maven + JDK 17)
# ==========================================
FROM --platform=linux maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos todo el contexto (pom.xml, src, etc.)
COPY . .

# Generamos las fuentes explícitamente para asegurar que las clases de Protobuf estén listas
RUN mvn generate-sources

# Empaquetado final
RUN mvn package -DskipTests

# ==========================================
# Etapa 2: RUNTIME (Amazon Corretto 17 Alpine ARM64)
# ==========================================
FROM --platform=linux amazoncorretto:17-alpine-jdk
WORKDIR /app

# Copia el JAR generado desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Expone el puerto de Spring Boot
EXPOSE 8080

# Configuración JVM optimizada para contenedores ARM64
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Xms256m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]