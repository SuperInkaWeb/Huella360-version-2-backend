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
# 23-09-2026, segundo intento: el primer tuning (35% heap + metaspace 96m + SerialGC)
# quedo corto de metaspace y ni siquiera termino de arrancar (OutOfMemoryError:
# Metaspace creando el securityFilterChain de Spring Security, que genera bastantes
# proxies/clases dinamicas). El tuning original (50% heap + metaspace 128m + ZGC) si
# arrancaba pero moria por OOM del contenedor poco despues de terminar de iniciar.
# Ajuste: bajar heap un poco (40% en vez de 50%) y el resto del ahorro sacarlo del
# overhead fijo de ZGC (cambiado a SerialGC) y de los stacks de threads (Xss256k +
# SERVER_TOMCAT_THREADS_MAX bajo via env var), dejando el metaspace generoso (160m)
# ya que ahi fue donde realmente fallo.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=40.0", \
    "-XX:InitialRAMPercentage=25.0", \
    "-XX:MaxMetaspaceSize=160m", \
    "-XX:+UseSerialGC", \
    "-Xss256k", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
