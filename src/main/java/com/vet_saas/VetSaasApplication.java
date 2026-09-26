package com.vet_saas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VetSaasApplication {

	/** Zona horaria del negocio si no se define APP_TIMEZONE. */
	static final String ZONA_HORARIA_POR_DEFECTO = "America/Lima";

	public static void main(String[] args) {
		configurarZonaHoraria(System.getenv("APP_TIMEZONE"));
		SpringApplication.run(VetSaasApplication.class, args);
	}

	/**
	 * Las fechas del dominio son LocalDateTime (hora "de pared", sin zona) y el frontend las muestra
	 * como hora local del usuario. Si el servidor corre en UTC (Render, Docker), LocalDateTime.now()
	 * y la lectura de columnas TIMESTAMPTZ dan la hora UTC y todo se ve 5 horas despues
	 * (recordatorio de las 10:30 mostrado a las 15:30, factura con la hora corrida).
	 * Se fija la zona del negocio antes de arrancar Spring (conexiones JDBC, Jackson y @Scheduled
	 * toman la zona por defecto de la JVM).
	 */
	static void configurarZonaHoraria(String zona) {
		String efectiva = (zona == null || zona.isBlank()) ? ZONA_HORARIA_POR_DEFECTO : zona.trim();
		TimeZone.setDefault(TimeZone.getTimeZone(java.time.ZoneId.of(efectiva)));
	}

}
