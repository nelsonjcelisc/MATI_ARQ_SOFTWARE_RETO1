# ETAPA 1: Compilación (Build)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 1. Copiamos e instalamos la librería común primero
# Esto la deja en el repositorio local de Maven del contenedor (/root/.m2)
COPY common-lib /app/common-lib
RUN mvn -f /app/common-lib/pom.xml clean install

# 2. Copiamos el código del apihandler
COPY apihandler /app/apihandler

# 3. Compilamos el apihandler
# Usamos el pom.xml específico de la carpeta apihandler
RUN mvn -f /app/apihandler/pom.xml clean package -DskipTests

# ETAPA 2: Ejecución (Run)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Traemos el jar desde la ruta donde quedó en la etapa de build
COPY --from=build /app/apihandler/target/*.jar app.jar

# Puerto de Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]