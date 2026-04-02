FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos todo el contexto (pom.xml, src, etc.)
COPY . .

# Generamos las fuentes explícitamente para asegurar que las clases de Protobuf estén listas
RUN mvn generate-sources

# Empaquetado final
RUN mvn package -DskipTests

# Etapa de ejecución (Runtime)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]