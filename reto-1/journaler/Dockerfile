# ETAPA 1: Compilación (Build)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# 1. Copiamos la librería común y la instalamos localmente en el contenedor
COPY common-lib /app/common-lib
RUN mvn -f /app/common-lib/pom.xml clean install

# 2. Copiamos el código del journaler
COPY journaler /app/journaler

# 3. Compilamos el journaler usando la librería que instalamos en el paso 1
RUN mvn -f /app/journaler/pom.xml clean package -DskipTests

# ETAPA 2: Ejecución (Run)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# El JAR ahora está dentro de la carpeta del journaler
COPY --from=build /app/journaler/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]