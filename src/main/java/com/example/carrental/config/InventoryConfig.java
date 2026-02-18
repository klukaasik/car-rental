package com.example.carrental.config;

import com.example.carrental.domain.CarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class InventoryConfig {

    @Bean
    public Map<CarType, Long> inventory() {
        return Map.of(
                CarType.SEDAN, 2L,
                CarType.SUV, 1L,
                CarType.VAN, 3L
        );
    }
}