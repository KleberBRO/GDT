# modulos/cruzamento/Dockerfile
FROM eclipse-temurin:17 AS builder

WORKDIR /build

# Instala Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copia tudo de modulos/ para /build/
COPY . .

# Build do contratos primeiro (dependência)
RUN cd contratos && mvn clean install -DskipTests

# Build do cruzamento
RUN cd cruzamento && mvn clean package -DskipTests

# Stage final
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia o JAR da stage anterior
COPY --from=builder /build/cruzamento/target/cruzamento-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
