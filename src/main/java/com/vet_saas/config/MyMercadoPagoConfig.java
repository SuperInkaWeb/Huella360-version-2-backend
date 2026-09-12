package com.vet_saas.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@RequiredArgsConstructor
public class MyMercadoPagoConfig {

    private final AppProperties appProperties;

    @PostConstruct
    public void init() {
        String accessToken = appProperties
                .getExternal()
                .getMercadoPago()
                .getAccessToken();

        if (StringUtils.hasText(accessToken)) {
            MercadoPagoConfig.setAccessToken(accessToken);
        }
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient();
    }

    @Bean
    public PreferenceClient preferenceClient() {
        return new PreferenceClient();
    }
}