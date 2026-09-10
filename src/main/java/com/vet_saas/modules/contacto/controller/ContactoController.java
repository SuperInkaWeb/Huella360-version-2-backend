package com.vet_saas.modules.contacto.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.notification.service.EmailService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/contacto")
@RequiredArgsConstructor
public class ContactoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactoController.class);

    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> sendContactMessage(
            @RequestBody ContactRequest request) {
        try {
            String html = buildEmailHtml(request);
            emailService.sendEmail("hola@huella360.com", "Nuevo mensaje de contacto: " + request.asunto(), html);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Mensaje enviado correctamente")));
        } catch (Exception e) {
            LOGGER.error("Error sending contact form email", e);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Mensaje enviado correctamente")));
        }
    }

    private String buildEmailHtml(ContactRequest request) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                  <h2 style="color: #2D3E82;">Nuevo mensaje de contacto</h2>
                  <table style="width: 100%; border-collapse: collapse; margin-top: 16px;">
                    <tr>
                      <td style="padding: 8px; font-weight: bold; color: #555;">Nombre:</td>
                      <td style="padding: 8px;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 8px; font-weight: bold; color: #555;">Correo:</td>
                      <td style="padding: 8px;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 8px; font-weight: bold; color: #555;">Perfil:</td>
                      <td style="padding: 8px;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding: 8px; font-weight: bold; color: #555;">Mensaje:</td>
                      <td style="padding: 8px;">%s</td>
                    </tr>
                  </table>
                </div>
                """.formatted(
                escapeHtml(request.nombre()),
                escapeHtml(request.email()),
                escapeHtml(request.asunto()),
                escapeHtml(request.mensaje())
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record ContactRequest(
            @NotBlank String nombre,
            @NotBlank @Email String email,
            @NotBlank String asunto,
            @NotBlank String mensaje
    ) {}
}
