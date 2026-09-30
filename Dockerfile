FROM node:22-alpine AS frontend
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY pom.xml ./
COPY src/ ./src/
COPY --from=frontend /web/dist/ ./frontend/dist/
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S techshop && adduser -S techshop -G techshop && mkdir /app/data && chown techshop:techshop /app/data
COPY --from=backend /app/target/techshop.jar /app/techshop.jar
USER techshop
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/techshop.jar"]
