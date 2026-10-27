package com.soilsync.app.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class RegionalKnowledgeBaseTest {

    @Test
    void placeholderCountryAndGlobalDocumentsAreNotUsedAsAdvice() {
        RegionalKnowledgeBase knowledgeBase = new RegionalKnowledgeBase(new DefaultResourceLoader());

        assertThat(knowledgeBase.findVettedDocument(
            "KE", "Kenya", "Nairobi County", "", "maize"
        )).isEmpty();
    }

    @Test
    void countryDocumentMustCoverSelectedRegionAndGlobalDocumentMustBeMarkedGlobal() {
        assertThat(RegionalKnowledgeBase.coversRegion(
            "Nairobi County|Kiambu County", false, "Kenya", "Nairobi County", null
        )).isTrue();
        assertThat(RegionalKnowledgeBase.coversRegion(
            "Nairobi County|Kiambu County", false, "Kenya", "Mombasa County", null
        )).isFalse();
        assertThat(RegionalKnowledgeBase.coversRegion(
            "Kenya", true, "Kenya", "Nairobi County", null
        )).isFalse();
        assertThat(RegionalKnowledgeBase.coversRegion(
            "Worldwide", true, "Kenya", "Nairobi County", null
        )).isTrue();
    }
}
