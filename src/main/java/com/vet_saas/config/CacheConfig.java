package com.vet_saas.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES));
        cacheManager.registerCustomCache("categorias",
                Caffeine.newBuilder().maximumSize(100).expireAfterWrite(30, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("productosById",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(15, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("empresasById",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(15, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("empresasByUsername",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(15, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("empresasByPropietario",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(10, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("empresasOptionalByPropietario",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(10, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("empresaIdsByPropietario",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(10, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("dashboardMetrics",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(3, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("dashboardChart",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(5, TimeUnit.MINUTES).build());
        cacheManager.registerCustomCache("dashboardActivity",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(2, TimeUnit.MINUTES).build());
        return cacheManager;
    }
}
