package com.vet_saas.modules.subscription.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionUsageDto {
    private long currentPets;
    private int maxPets;
    private double petPercentage;

    private long currentProducts;
    private int maxProducts;
    private double productPercentage;

    private long currentServices;
    private int maxServices;
    private double servicePercentage;
}
