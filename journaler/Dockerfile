# ETAPA 1: Compilación (Build)
# Usamos una imagen que YA tiene Maven y Java 17 para compilar
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiamos los archivos del proyecto al contenedor
COPY . .

# Ejecutamos la compilación dentro del contenedor (aquí no importa si tu server tiene Java o no)
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# ETAPA 2: Ejecución (Run)
# Usamos una imagen ligera que solo tiene lo necesario para correr Java
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Traemos el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto 8080 que es el de Spring Boot
EXPOSE 8080

# Arrancamos la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]