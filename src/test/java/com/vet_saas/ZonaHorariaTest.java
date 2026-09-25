package com.vet_saas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * H360: en Render (UTC) todas las horas se mostraban 5 horas despues (recordatorio 10:30 -> 15:30,
 * factura de las 09:38 -> 02:38 p. m.). La JVM debe correr en la zona del negocio.
 */
class ZonaHorariaTest {

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restaurar() {
        TimeZone.setDefault(original);
    }

    @Test
    void sinVariable_usaLimaAunqueElServidorEsteEnUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        VetSaasApplication.configurarZonaHoraria(null);

        assertEquals("America/Lima", TimeZone.getDefault().getID());
    }

    @Test
    void variableVacia_usaLima() {
        VetSaasApplication.configurarZonaHoraria("  ");

        assertEquals("America/Lima", TimeZone.getDefault().getID());
    }

    @Test
    void respetaAppTimezone() {
        VetSaasApplication.configurarZonaHoraria("America/Bogota");

        assertEquals("America/Bogota", TimeZone.getDefault().getID());
    }
}
