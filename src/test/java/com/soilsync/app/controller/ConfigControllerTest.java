package com.soilsync.app.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigControllerTest {

    @Test
    void exposesTheConfiguredCatalogCurrencyInUpperCase() {
        assertThat(new ConfigController(" kes ").config()).containsEntry("catalogCurrency", "KES");
    }

    @Test
    void rejectsAnInvalidCurrencyCodeAtStartup() {
        assertThatThrownBy(() -> new ConfigController("RUPEES")).isInstanceOf(IllegalArgumentException.class);
    }
}
