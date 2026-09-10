package com.vet_saas.modules.payment.dto;

public record PaymentPreferenceResponse(
                String preferenceId,
                String initPoint,
                String sandboxInitPoint) {
}
