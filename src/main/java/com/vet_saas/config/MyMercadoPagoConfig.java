package com.vet_saas.config;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MyMercadoPagoConfig {

    private final AppProperties appProperties;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(appProperties.getExternal().getMercadoPago().getAccessToken());
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