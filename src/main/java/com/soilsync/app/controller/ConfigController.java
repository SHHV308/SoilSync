package com.soilsync.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Currency;
import java.util.Map;

@RestController
public class ConfigController {

    private final String catalogCurrency;

    public ConfigController(@Value("${soilsync.catalog.currency:INR}") String catalogCurrency) {
        // Fails fast at startup on an invalid ISO 4217 code.
        this.catalogCurrency = Currency.getInstance(catalogCurrency.trim().toUpperCase()).getCurrencyCode();
    }

    @GetMapping("/api/config")
    public Map<String, String> config() {
        return Map.of("catalogCurrency", catalogCurrency);
    }
}
