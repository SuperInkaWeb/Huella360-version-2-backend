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

# JVM tuning para contenedores con poca memoria (512MB plan Render)
# Es la configuracion que corre estable en el ambiente QA con el mismo plan.
# - MaxRAMPercentage=40: Usa max ~205MB para heap (deja ~300MB para metaspace, JIT, stacks y OS)
# - MaxMetaspaceSize=160m: Con 128m o menos Spring Security no termina de arrancar (OOM Metaspace)
# - SerialGC: Menor overhead fijo de memoria que ZGC en heaps chicos
# - Xss256k: Stacks de thread mas livianos
# No definir JAVA_TOOL_OPTIONS en Render: sus -Xmx/-XX pisan o chocan con estos valores
# (un deploy fallo con "Multiple garbage collectors selected").
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=40.0", \
    "-XX:InitialRAMPercentage=25.0", \
    "-XX:MaxMetaspaceSize=160m", \
    "-XX:+UseSerialGC", \
    "-Xss256k", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
