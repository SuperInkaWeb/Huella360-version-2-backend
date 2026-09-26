package com.vet_saas.modules.complaint.service;

import com.cloudinary.Cloudinary;
import com.vet_saas.modules.complaint.dto.ReclamoRequestDto;
import com.vet_saas.modules.complaint.model.Reclamo;
import com.vet_saas.modules.complaint.repository.ReclamoRepository;
import com.vet_saas.modules.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * H360-QA (24/09): el Libro de Reclamaciones no registraba ningun reclamo.
 * - ReclamoService pasaba un InputStream a cloudinary.uploader().upload(), que el SDK no acepta
 *   ("Unrecognized file parameter"): la subida del PDF fallaba siempre.
 * - Todo el metodo era una transaccion: al fallar el PDF se perdia tambien el reclamo.
 */
@ExtendWith(MockitoExtension.class)
class ReclamoServiceTest {

    @Mock private ReclamoRepository reclamoRepository;
    @Mock private EmailService emailService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private Cloudinary cloudinary;
    @Mock private PdfService pdfService;

    private ReclamoService service;
    private ReclamoRequestDto dto;

    @BeforeEach
    void setUp() {
        service = new ReclamoService(reclamoRepository, emailService, cloudinary, pdfService);
        dto = new ReclamoRequestDto();
        dto.setTipoDocumento("DNI");
        dto.setNumeroDocumento("12345678");
        dto.setPrimerNombre("Ana");
        dto.setPrimerApellido("Quispe");
        dto.setDireccion("Av. Siempre Viva 123");
        dto.setDepartamento("Lima");
        dto.setProvincia("Lima");
        dto.setDistrito("Miraflores");
        dto.setCorreo("ana@test.com");
        dto.setTelefono("987654321");
        dto.setEsMenor(false);
        dto.setTipoReclamo("RECLAMO");
        dto.setResumen("Producto defectuoso");
        dto.setDetallePedido("Llego roto");
        // El repositorio asigna el id al guardar
        lenient().when(reclamoRepository.save(any(Reclamo.class))).thenAnswer(inv -> {
            Reclamo r = inv.getArgument(0);
            if (r.getId() == null) r.setId(7L);
            return r;
        });
    }

    @Test
    void registroAnonimo_guardaElReclamo_subeElPdfComoBytes_yEnviaElCorreo() throws Exception {
        byte[] pdf = {1, 2, 3};
        when(pdfService.generateReclamoPdf(dto, 7L)).thenReturn(pdf);
        when(cloudinary.uploader().upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://cdn/reclamo_000007.pdf"));

        ReclamoService.ResultadoReclamo r = service.registrarReclamo(null, dto, null);

        assertThat(r.numero()).isEqualTo("000007");
        assertThat(r.pdfUrl()).isEqualTo("https://cdn/reclamo_000007.pdf");
        ArgumentCaptor<Object> archivo = ArgumentCaptor.forClass(Object.class);
        verify(cloudinary.uploader()).upload(archivo.capture(), anyMap());
        assertThat(archivo.getValue()).isInstanceOf(byte[].class); // nunca InputStream
        ArgumentCaptor<Reclamo> guardado = ArgumentCaptor.forClass(Reclamo.class);
        verify(reclamoRepository, atLeastOnce()).save(guardado.capture());
        assertThat(guardado.getValue().getUsuario()).isNull();
        assertThat(guardado.getValue().getPdfReclamoUrl()).isEqualTo("https://cdn/reclamo_000007.pdf");
        verify(emailService).sendReclamoEmailConLink(eq("ana@test.com"), eq("Ana Quispe"), eq("Reclamo N° 000007"), eq("https://cdn/reclamo_000007.pdf"));
    }

    @Test
    void siFallaLaSubidaDelPdf_elReclamoQuedaGuardado_yElCorreoSaleSinPdf() throws Exception {
        when(pdfService.generateReclamoPdf(dto, 7L)).thenReturn(new byte[]{1});
        when(cloudinary.uploader().upload(any(), anyMap())).thenThrow(new RuntimeException("Unrecognized file parameter"));

        ReclamoService.ResultadoReclamo r = service.registrarReclamo(null, dto, null);

        assertThat(r.id()).isEqualTo(7L);
        assertThat(r.numero()).isEqualTo("000007");
        assertThat(r.pdfUrl()).isNull();
        verify(reclamoRepository, times(1)).save(any(Reclamo.class)); // solo el guardado inicial
        verify(emailService).sendReclamoEmailConLink(eq("ana@test.com"), anyString(), eq("Reclamo N° 000007"), isNull());
    }

    @Test
    void siFallaLaGeneracionDelPdf_elReclamoQuedaGuardado() {
        when(pdfService.generateReclamoPdf(dto, 7L)).thenThrow(new IllegalStateException("Jasper"));

        ReclamoService.ResultadoReclamo r = service.registrarReclamo(null, dto, null);

        assertThat(r.numero()).isEqualTo("000007");
        assertThat(r.pdfUrl()).isNull();
        verify(reclamoRepository, times(1)).save(any(Reclamo.class));
    }

    @Test
    void siFallaElCorreo_elRegistroNoFalla() throws Exception {
        when(pdfService.generateReclamoPdf(dto, 7L)).thenReturn(new byte[]{1});
        when(cloudinary.uploader().upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", "https://cdn/x.pdf"));
        doThrow(new RuntimeException("Resend caido")).when(emailService).sendReclamoEmailConLink(any(), any(), any(), any());

        ReclamoService.ResultadoReclamo r = service.registrarReclamo(null, dto, null);

        assertThat(r.numero()).isEqualTo("000007");
    }

    @Test
    void archivoAdjunto_seSubeComoBytes_yQuedaEnElReclamo() throws Exception {
        MockMultipartFile adjunto = new MockMultipartFile("archivo", "boleta.jpg", "image/jpeg", new byte[]{9, 9});
        when(pdfService.generateReclamoPdf(dto, 7L)).thenReturn(new byte[]{1});
        when(cloudinary.uploader().upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://cdn/boleta.jpg"))
                .thenReturn(Map.of("secure_url", "https://cdn/reclamo.pdf"));

        service.registrarReclamo(null, dto, adjunto);

        ArgumentCaptor<Reclamo> guardado = ArgumentCaptor.forClass(Reclamo.class);
        verify(reclamoRepository, atLeastOnce()).save(guardado.capture());
        assertThat(guardado.getAllValues().get(0).getArchivoAdjuntoUrl()).isEqualTo("https://cdn/boleta.jpg");
    }
}
