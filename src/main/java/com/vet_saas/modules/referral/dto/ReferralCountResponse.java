package com.vet_saas.modules.referral.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferralCountResponse {
    private long totalReferidos;
    private boolean desbloqueo2daMascota;
    private long referidosNecesarios;
    private long referidosRestantes;
}
