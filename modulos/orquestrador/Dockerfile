# modulos/orquestrador/Dockerfile
FROM eclipse-temurin:17 AS builder

WORKDIR /build

# Instala Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copia o pom.xml e mensagens do contratos (dependência)
COPY contratos/pom.xml contratos/
COPY contratos/mensagens contratos/mensagens

# Copia o pom.xml e código do orquestrador
COPY orquestrador/pom.xml orquestrador/
COPY orquestrador orquestrador/

# Build do contratos primeiro (dependência)
RUN cd contratos && mvn clean install -DskipTests

# Build do orquestrador
RUN cd orquestrador && mvn clean package -DskipTests

# Stage final
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia o JAR da stage anterior
COPY --from=builder /build/orquestrador/target/orquestrador-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
