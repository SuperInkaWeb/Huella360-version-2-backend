FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Usar imagen Debian slim (NO alpine) para soporte de fuentes en JasperReports
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Instalar fontconfig y fuentes necesarias para generacion de PDFs con JasperReports
RUN apt-get update && apt-get install -y --no-install-recommends \
    fontconfig \
    libfreetype6 \
    && fc-cache -fv \
    && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root para seguridad
RUN groupadd -r appuser && useradd -r -g appuser -d /app -s /sbin/nologin appuser

COPY --from=build /app/target/*.jar app.jar

# Propiedad del archivo para el usuario no-root
RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8080

# JVM tuning para contenedores con poca memoria (512MB plan Render, free tier)
# 23-09-2026: el tuning anterior (50% heap + ZGC) seguia siendo OOM-killed por Render
# justo despues del arranque. ZGC tiene overhead fijo de memoria pensado para heaps
# grandes con baja latencia, no es el mas eficiente para un heap chico de un solo
# usuario (prueba QA). Ajustes:
# - MaxRAMPercentage=35 / InitialRAMPercentage=25: heap mas chico, deja mas margen
#   para metaspace, stack de threads, buffers directos y memoria nativa (fontconfig/
#   JasperReports)
# - MaxMetaspaceSize=96m: igual, mas margen
# - UseSerialGC: mucho menor overhead fijo que ZGC/G1 para heaps chicos; el costo
#   (pausas mas largas) es irrelevante para una instancia QA de un solo usuario
# - Xss256k: stack por thread mas chico (default ~1MB); con el pool de Tomcat
#   limitado via SERVER_TOMCAT_THREADS_MAX esto reduce bastante la memoria nativa
#   reservada para stacks
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=35.0", \
    "-XX:InitialRAMPercentage=25.0", \
    "-XX:MaxMetaspaceSize=96m", \
    "-XX:+UseSerialGC", \
    "-Xss256k", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
