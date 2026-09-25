# ── Stage 1: Build ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY frontend ./frontend
COPY database ./database
COPY backend ./backend
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create gallery upload directory
RUN mkdir -p /app/static/assets/images/gallery

COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
