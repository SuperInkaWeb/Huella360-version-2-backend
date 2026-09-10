package com.vet_saas.modules.payment.gateway;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import com.vet_saas.core.exceptions.types.BusinessException;
import com.vet_saas.modules.payment.dto.PaymentPreferenceResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MercadoPagoGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercadoPagoGateway.class);
    private final PreferenceClient preferenceClient = new PreferenceClient();
    private final PaymentClient paymentClient = new PaymentClient();

    public PaymentPreferenceResponse createPreference(
            String accessToken,
            String externalReference,
            List<PreferenceItemRequest> items,
            PreferencePayerRequest payer,
            Map<String, Object> metadata,
            String successUrl,
            String notificationUrl) {

        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .accessToken(accessToken)
                .build();

        try {
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .externalReference(externalReference)
                    .metadata(metadata)
                    .backUrls(PreferenceBackUrlsRequest.builder()
                            .success(successUrl)
                            .failure(successUrl) // Redigir al mismo lugar para que el frontend maneje el estado
                            .pending(successUrl)
                            .build())
                    .autoReturn("approved")
                    .notificationUrl(notificationUrl)
                    .statementDescriptor("VETSAAS")
                    .build();

            Preference preference = preferenceClient.create(preferenceRequest, requestOptions);

            return new PaymentPreferenceResponse(
                    preference.getId(),
                    preference.getInitPoint(),
                    preference.getSandboxInitPoint());

        } catch (Exception e) {
            LOGGER.error("Error al crear preferencia en Mercado Pago", e);
            throw new BusinessException("Error al comunicarse con la pasarela de pagos: " + e.getMessage());
        }
    }

    public Payment getPaymentDetails(String paymentId, String accessToken) {
        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .accessToken(accessToken)
                .build();

        try {
            return paymentClient.get(Long.parseLong(paymentId), requestOptions);
        } catch (Exception e) {
            LOGGER.error("Error al obtener detalles del pago {} en Mercado Pago", paymentId, e);
            throw new BusinessException("No se pudo verificar el estado del pago en Mercado Pago.");
        }
    }
}
