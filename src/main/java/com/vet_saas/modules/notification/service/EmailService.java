package com.vet_saas.modules.notification.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Attachment;
import com.vet_saas.config.AppProperties;
import com.vet_saas.modules.sales.model.Orden;
import com.vet_saas.modules.sales.repository.OrdenRepository;
import com.vet_saas.modules.user.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final TemplateEngine templateEngine;
    private final OrdenRepository ordenRepository;
    private final AppProperties appProperties;

    private Resend getResendClient() {
        String apiKey = appProperties.getExternal().getResend().getApiKey();
        LOGGER.info("Initializing Resend client with API Key (length: {})", apiKey != null ? apiKey.length() : "null");
        return new Resend(apiKey);
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            Resend resend = getResendClient();
            String from = appProperties.getExternal().getResend().getFromEmail();

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            LOGGER.info("Email sent successfully via Resend. ID: {}", response.getId());
        } catch (ResendException ex) {
            LOGGER.error("Resend API error sending email to {}: {}", to, ex.getMessage(), ex);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error sending email to {}: {}", to, ex.getMessage(), ex);
        }
    }

    private void sendEmailWithAttachment(String to, String subject, String htmlContent, String attachmentName, byte[] attachmentContent) {
        try {
            Resend resend = getResendClient();
            String from = appProperties.getExternal().getResend().getFromEmail();

            Attachment attachment = Attachment.builder()
                    .fileName(attachmentName)
                    .content(Base64.getEncoder().encodeToString(attachmentContent))
                    .build();

            com.resend.services.emails.model.CreateEmailOptions params = com.resend.services.emails.model.CreateEmailOptions.builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .attachments(attachment)
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            LOGGER.info("Email with attachment sent successfully via Resend. ID: {}", response.getId());
        } catch (ResendException ex) {
            LOGGER.error("Resend API error sending email with attachment to {}: {}", to, ex.getMessage(), ex);
        } catch (Exception ex) {
            LOGGER.error("Unexpected error sending email with attachment to {}: {}", to, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendReclamoEmail(String emailDestino, String nombreCliente, String numeroReclamo, byte[] pdfContent) {
        try {
            Context context = new Context();
            context.setVariable("nombreUsuario", nombreCliente);
            context.setVariable("numeroReclamo", numeroReclamo);

            // Podemos usar una plantilla genérica o crear un mensaje simple en HTML
            String htmlContent = "<h2>Libro de Reclamaciones - HUELLA360</h2>" +
                    "<p>Hola " + nombreCliente + ",</p>" +
                    "<p>Hemos recibido tu " + numeroReclamo + ".</p>" +
                    "<p>Adjunto encontrarás una copia en PDF de tu solicitud. Te contactaremos dentro del plazo legal establecido.</p>";

            // Enviar al cliente
            sendEmailWithAttachment(emailDestino, "Copia de tu Reclamo/Queja - HUELLA360", htmlContent, "Hoja_Reclamacion.pdf", pdfContent);
            
            // Enviar copia a administracion
            String adminEmail = appProperties.getNotification().getAdminEmail();
            sendEmailWithAttachment(adminEmail, "NUEVO " + numeroReclamo + " - " + nombreCliente, htmlContent, "Hoja_Reclamacion.pdf", pdfContent);

        } catch (Exception ex) {
            LOGGER.error("Error preparing reclamo email: {}", ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendOrderConfirmation(Long ordenId) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for email: " + ordenId));

            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            Context context = new Context();
            context.setVariable("nombreCliente", emailDestino);
            context.setVariable("codigoOrden", orden.getCodigoOrden());
            context.setVariable("nombreEmpresa", orden.getEmpresa().getNombreComercial());
            context.setVariable("items", orden.getDetalles());
            context.setVariable("total", orden.getTotal());

            String htmlContent = templateEngine.process("email/order-confirmation", context);
            sendEmail(emailDestino, "Confirmación de compra - " + orden.getCodigoOrden(), htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error preparing order confirmation email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendDeliveryOtpEmail(Long ordenId, String otpCode) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for OTP email: " + ordenId));

            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            Context context = new Context();
            context.setVariable("nombreCliente", emailDestino);
            context.setVariable("codigoOrden", orden.getCodigoOrden());
            context.setVariable("nombreEmpresa", orden.getEmpresa().getNombreComercial());
            context.setVariable("items", orden.getDetalles());
            context.setVariable("total", orden.getTotal());
            context.setVariable("otpCode", otpCode);
            context.setVariable("costoEnvio", orden.getCostoEnvio() != null ? orden.getCostoEnvio() : BigDecimal.ZERO);

            String htmlContent = templateEngine.process("email/order-confirmation", context); // Usando el mismo template por ahora
            sendEmail(emailDestino, "Tu código de entrega - " + orden.getCodigoOrden(), htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error preparing delivery OTP email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendWelcomeEmail(Usuario usuario) {
        try {
            if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) return;

            Context context = new Context();
            context.setVariable("nombreUsuario", usuario.getCorreo());

            String htmlContent = templateEngine.process("email/welcome-email", context);
            sendEmail(usuario.getCorreo(), "¡Bienvenido a VetSaaS!", htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error preparing welcome email to {}: {}", usuario.getCorreo(), ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendVerificationEmail(Usuario usuario, String token) {
        try {
            String frontendUrl = appProperties.getExternal().getFrontendUrl();
            Context context = new Context();
            context.setVariable("nombreUsuario", usuario.getCorreo());
            context.setVariable("token", token);
            String verificationUrl = frontendUrl + "/auth/verify-email?token=" + token;
            context.setVariable("verificationUrl", verificationUrl);

            String htmlContent = templateEngine.process("email/verify-email", context);
            sendEmail(usuario.getCorreo(), "Verifica tu correo - VetSaaS", htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error preparing verification email to {}: {}", usuario.getCorreo(), ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendPasswordResetEmail(Usuario usuario, String token) {
        try {
            String frontendUrl = appProperties.getExternal().getFrontendUrl();
            Context context = new Context();
            context.setVariable("nombreUsuario", usuario.getCorreo());
            context.setVariable("token", token);
            String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("email/password-reset", context);
            sendEmail(usuario.getCorreo(), "Restablece tu contraseña - VetSaaS", htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error preparing reset email to {}: {}", usuario.getCorreo(), ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendReclamoEmailConLink(String emailDestino, String nombreCliente, String numeroReclamo, String pdfUrl) {
        try {
            // Construimos un diseño HTML limpio y profesional con el botón dinámico hacia Cloudinary
            String htmlContent = "<h2>Libro de Reclamaciones Virtual</h2>" +
                    "<p>Estimado(a) <strong>" + nombreCliente + "</strong>,</p>" +
                    "<p>Le informamos que su solicitud en nuestro Libro de Reclamaciones ha sido registrada correctamente bajo el identificador: <strong>" + numeroReclamo + "</strong>.</p>" +
                    "<p>Conforme a la normativa de protección al consumidor, adjuntamos el acceso directo para visualizar, guardar o descargar la copia electrónica oficial de su hoja de reclamación:</p>" +
                    "<div style='margin: 25px 0;'>" +
                    "  <a href='" + pdfUrl + "' target='_blank' style='background-color: #1ea59c; color: white; padding: 12px 20px; text-decoration: none; font-weight: bold; border-radius: 8px; display: inline-block;'>" +
                    "    Ver Hoja de Reclamación (PDF)" +
                    "  </a>" +
                    "</div>" +
                    "<p style='font-size: 12px; color: #666;'>De acuerdo a Ley, daremos respuesta formal a su requerimiento dentro del plazo establecido de quince (15) días hábiles.</p>" +
                    "<hr style='border: 0; border-top: 1px solid #eee; margin-top: 30px;'>" +
                    "<p style='font-size: 11px; color: #999;'>Este es un correo automático de notificación enviado por el sistema, por favor no responda a esta dirección.</p>";

            // 1. Envío directo al cliente afectado
            sendEmail(emailDestino, "Copia de tu Reclamo/Queja - HUELLA360", htmlContent);

            // 2. Envio de copia administrativa a tu cuenta Sandbox autorizada de Resend
            String adminEmail = appProperties.getNotification().getAdminEmail();
            sendEmail(adminEmail, "Copia Administrativa: " + numeroReclamo + " - " + nombreCliente, htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error al preparar el correo electrónico del reclamo dinámico: {}", ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendNewsletterConfirmation(String email) {
        try {
            Context context = new Context();
            context.setVariable("subscriberEmail", email);

            String htmlContent = templateEngine.process("email/newsletter-confirmacion", context);
            sendEmail(email, "¡Gracias por suscribirte a Huella360!", htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error sending newsletter confirmation email to {}: {}", email, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    public void sendNewsletterAdminNotification(String subscriberEmail) {
        try {
            String adminEmail = appProperties.getNotification().getAdminEmail();
            Context context = new Context();
            context.setVariable("subscriberEmail", subscriberEmail);
            context.setVariable("subscriptionDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("email/newsletter-admin-notification", context);
            sendEmail(adminEmail, "Nueva suscripción al newsletter - " + subscriberEmail, htmlContent);

        } catch (Exception ex) {
            LOGGER.error("Error sending newsletter admin notification for {}: {}", subscriberEmail, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendDeliveryAsignadoEmail(Long ordenId, String nombreRepartidor) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for email: " + ordenId));
            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#1ea59c'>Tu pedido tiene repartidor asignado</h2>"
                + "<p>Hola <strong>" + emailDestino + "</strong>,</p>"
                + "<p>Tu pedido <strong>" + orden.getCodigoOrden() + "</strong> fue asignado a <strong>" + nombreRepartidor + "</strong>.</p>"
                + "<p>El repartidor pasará a recoger tu pedido y te lo entregará pronto.</p>"
                + "<hr style='border:0;border-top:1px solid #eee;margin-top:30px'>"
                + "<p style='font-size:11px;color:#999'>Notificación automática de Huella360.</p></div>";
            sendEmail(emailDestino, "Tu pedido fue asignado - " + orden.getCodigoOrden(), htmlContent);
        } catch (Exception ex) {
            LOGGER.error("Error sending delivery asignado email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendDeliveryEnCaminoEmail(Long ordenId) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for email: " + ordenId));
            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#1ea59c'>Tu pedido va en camino</h2>"
                + "<p>Hola <strong>" + emailDestino + "</strong>,</p>"
                + "<p>Tu pedido <strong>" + orden.getCodigoOrden() + "</strong> ya va camino a tu dirección.</p>"
                + "<p>Puedes seguir el estado en tiempo real desde la plataforma.</p>"
                + "<hr style='border:0;border-top:1px solid #eee;margin-top:30px'>"
                + "<p style='font-size:11px;color:#999'>Notificación automática de Huella360.</p></div>";
            sendEmail(emailDestino, "Tu pedido va en camino - " + orden.getCodigoOrden(), htmlContent);
        } catch (Exception ex) {
            LOGGER.error("Error sending delivery en camino email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendDeliveryEntregadoEmail(Long ordenId) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for email: " + ordenId));
            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#1ea59c'>Tu pedido fue entregado</h2>"
                + "<p>Hola <strong>" + emailDestino + "</strong>,</p>"
                + "<p>Tu pedido <strong>" + orden.getCodigoOrden() + "</strong> fue entregado exitosamente.</p>"
                + "<p>Te invitamos a calificar tu experiencia de compra.</p>"
                + "<hr style='border:0;border-top:1px solid #eee;margin-top:30px'>"
                + "<p style='font-size:11px;color:#999'>Notificación automática de Huella360.</p></div>";
            sendEmail(emailDestino, "Pedido entregado - " + orden.getCodigoOrden(), htmlContent);
        } catch (Exception ex) {
            LOGGER.error("Error sending delivery entregado email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendDeliveryFallidoEmail(Long ordenId, String motivo) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for email: " + ordenId));
            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#e74c3c'>Problema con tu entrega</h2>"
                + "<p>Hola <strong>" + emailDestino + "</strong>,</p>"
                + "<p>Lamentablemente hubo un problema con la entrega de tu pedido <strong>" + orden.getCodigoOrden() + "</strong>.</p>"
                + (motivo != null ? "<p><strong>Motivo:</strong> " + motivo + "</p>" : "")
                + "<p>El equipo de la tienda está trabajando para resolverlo.</p>"
                + "<hr style='border:0;border-top:1px solid #eee;margin-top:30px'>"
                + "<p style='font-size:11px;color:#999'>Notificación automática de Huella360.</p></div>";
            sendEmail(emailDestino, "Problema con tu entrega - " + orden.getCodigoOrden(), htmlContent);
        } catch (Exception ex) {
            LOGGER.error("Error sending delivery fallido email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }

    @Async("mailExecutor")
    @Transactional(readOnly = true)
    public void sendDeliveryCanceladoEmail(Long ordenId, String motivo) {
        try {
            Orden orden = ordenRepository.findByIdForEmail(ordenId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for email: " + ordenId));
            String emailDestino = orden.getUsuarioCliente().getCorreo();
            if (emailDestino == null || emailDestino.isBlank()) return;

            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#e67e22'>Tu entrega fue cancelada</h2>"
                + "<p>Hola <strong>" + emailDestino + "</strong>,</p>"
                + "<p>Tu pedido <strong>" + orden.getCodigoOrden() + "</strong> fue cancelado.</p>"
                + (motivo != null ? "<p><strong>Motivo:</strong> " + motivo + "</p>" : "")
                + "<p>Si tienes dudas, contacta a la tienda.</p>"
                + "<hr style='border:0;border-top:1px solid #eee;margin-top:30px'>"
                + "<p style='font-size:11px;color:#999'>Notificación automática de Huella360.</p></div>";
            sendEmail(emailDestino, "Tu entrega fue cancelada - " + orden.getCodigoOrden(), htmlContent);
        } catch (Exception ex) {
            LOGGER.error("Error sending delivery cancelado email orderId={}: {}", ordenId, ex.getMessage(), ex);
        }
    }
}
