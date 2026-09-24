package com.vet_saas.core.validation;

import com.vet_saas.modules.catalog.dto.CreateServiceDto;
import com.vet_saas.modules.catalog.dto.UpdateServiceDto;
import com.vet_saas.modules.catalog.model.ModalidadServicio;
import com.vet_saas.modules.company.dto.UpdateCompanyDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H360-QA (24/09): en QA el backend acepto un telefono de empresa de 17 digitos y un servicio
 * de 3030 minutos (50 horas). Estas reglas los rechazan con 400 y un mensaje claro.
 */
class DtoValidacionesTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static UpdateCompanyDto empresaConTelefono(String telefono) {
        return new UpdateCompanyDto(null, null, null, telefono, null, null, null, null, null);
    }

    private static Set<String> campos(Set<? extends ConstraintViolation<?>> v) {
        return v.stream().map(c -> c.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void telefonoEmpresa_valido() {
        assertThat(validator.validate(empresaConTelefono("987650003"))).isEmpty();
        assertThat(validator.validate(empresaConTelefono("014567890"))).isEmpty();
        assertThat(validator.validate(empresaConTelefono(null))).isEmpty(); // update parcial
    }

    @Test
    void telefonoEmpresa_rechazaLargoIrrealYFormatoInvalido() {
        assertThat(campos(validator.validate(empresaConTelefono("98765432987650003")))).contains("telefono"); // 17 digitos (caso QA)
        assertThat(campos(validator.validate(empresaConTelefono("12345")))).contains("telefono");
        assertThat(campos(validator.validate(empresaConTelefono("98765-432")))).contains("telefono");
    }

    private static CreateServiceDto servicioConDuracion(Integer minutos) {
        return new CreateServiceDto("Consulta", null, new BigDecimal("50"), minutos, ModalidadServicio.PRESENCIAL, true, true);
    }

    @Test
    void duracionServicio_valida() {
        assertThat(validator.validate(servicioConDuracion(30))).isEmpty();
        assertThat(validator.validate(servicioConDuracion(480))).isEmpty();
    }

    @Test
    void duracionServicio_rechazaMasDe8HorasOCero() {
        assertThat(campos(validator.validate(servicioConDuracion(3030)))).contains("duracionMinutos"); // caso QA
        assertThat(campos(validator.validate(servicioConDuracion(481)))).contains("duracionMinutos");
        assertThat(campos(validator.validate(servicioConDuracion(0)))).contains("duracionMinutos");
    }

    @Test
    void duracionServicio_enEdicionTambienSeValida() {
        UpdateServiceDto invalido = new UpdateServiceDto(null, null, null, 3030, null, null, null);
        UpdateServiceDto valido = new UpdateServiceDto(null, null, null, 20, null, null, null);
        UpdateServiceDto sinCambio = new UpdateServiceDto(null, null, null, null, null, null, null);

        assertThat(campos(validator.validate(invalido))).contains("duracionMinutos");
        assertThat(validator.validate(valido)).isEmpty();
        assertThat(validator.validate(sinCambio)).isEmpty();
    }
}
