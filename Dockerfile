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
# - MaxRAMPercentage=50: Usa max 256MB para heap (deja ~256MB para OS, metaspace, stack, etc)
# - MaxMetaspaceSize=128m: Limita metaspace para evitar crecimiento indefinido
# - ZGC: Garbage collector de baja latencia y bajo overhead de memoria
# - TrimNativeHeap: Libera memoria nativa al GC
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=50.0", \
    "-XX:InitialRAMPercentage=40.0", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
