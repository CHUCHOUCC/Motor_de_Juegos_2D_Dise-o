# Imagen para desplegar PixelForge (por ejemplo en Render). Se construye desde la RAÍZ del repositorio
# porque además del backend copia el frontend:  docker build -f backend/Dockerfile .

# 1) Compilar con Maven y correr las pruebas
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY backend/mvnw backend/pom.xml ./
COPY backend/.mvn .mvn
RUN sh ./mvnw dependency:go-offline -q
COPY backend/src ./src
RUN sh ./mvnw package -q

# 2) Imagen final: solo el JRE, el .jar y la carpeta del frontend
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY frontend ./frontend
ENV PORT=8080 FRONTEND_DIR=file:/app/frontend/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
